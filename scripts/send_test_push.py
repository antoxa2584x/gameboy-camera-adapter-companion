#!/usr/bin/env python3
"""Send a test push to GBCam Companion via FCM.

Uses the FCM HTTP v1 API. The old `fcm.googleapis.com/fcm/send` endpoint with an
`Authorization: key=<server key>` header is gone — Google shut the legacy APIs
down in 2024 — so this authenticates with a service-account key instead.

Setup (once):
  1. Firebase Console -> Project settings -> Service accounts
     -> "Generate new private key". Save the JSON somewhere outside the repo,
     or as ./fcm-key.json (gitignored).
  2. export GCAC_FCM_KEY=/path/to/that.json     # or pass --key

Usage:
  # Broadcast to every install, exactly as a real announcement is sent
  ./scripts/send_test_push.py --title "Hello" --body "Testing 1 2 3"

  # Target one device (token is logged at debug-build startup, tag PushClient)
  ./scripts/send_test_push.py --token "fMEr..." --body "Just you"

  # Exercise the data-message path instead of the notification path
  ./scripts/send_test_push.py --type data --body "Data payload"

  # Ask FCM to validate without delivering
  ./scripts/send_test_push.py --body "noop" --dry-run

Requires: cryptography (pip install cryptography)

WHAT TO CHECK ON DEVICE
  The two message types travel different code paths, and only trying both
  exercises the whole feature:
    --type notification : while the app is backgrounded or killed, FCM itself
                          draws the notification from the manifest
                          default_notification_* meta-data. In the foreground it
                          arrives in onMessageReceived instead.
    --type data         : always arrives in onMessageReceived, in every app
                          state, and is drawn by PushNotifier.
  So test each of: app in foreground, app backgrounded, app force-stopped.
"""
from __future__ import annotations

import argparse
import base64
import json
import os
import subprocess
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

TOKEN_URI = "https://oauth2.googleapis.com/token"
SCOPE = "https://www.googleapis.com/auth/firebase.messaging"
FCM_URL = "https://fcm.googleapis.com/v1/projects/{project}/messages:send"
# Must match R.string.push_channel_id.
CHANNEL_ID = "announcements"
DEFAULT_TOPIC = "announcements"


def die(msg: str) -> None:
    sys.exit(f"error: {msg}")


def b64url(raw: bytes) -> str:
    return base64.urlsafe_b64encode(raw).rstrip(b"=").decode()


def find_key(explicit: str | None) -> Path:
    candidates = [explicit, os.environ.get("GCAC_FCM_KEY"), "fcm-key.json"]
    for c in candidates:
        if c and Path(c).is_file():
            return Path(c)
    die(
        "no service-account key found.\n"
        "  Pass --key PATH, set GCAC_FCM_KEY, or place fcm-key.json in the repo root.\n"
        "  Get one from: Firebase Console -> Project settings -> Service accounts."
    )
    raise AssertionError  # unreachable, keeps type checkers happy


def assert_key_untracked(path: Path) -> None:
    """Refuse to use a key that git is tracking — that is a live credential leak."""
    try:
        tracked = subprocess.run(
            ["git", "ls-files", "--error-unmatch", str(path)],
            capture_output=True, text=True,
        ).returncode == 0
    except FileNotFoundError:
        return  # no git available; nothing to check
    if tracked:
        die(
            f"{path} is tracked by git. A service-account key must never be committed.\n"
            f"  Fix: git rm --cached '{path}'  (the file stays on disk)"
        )


def access_token(sa: dict) -> str:
    """Mint a service-account JWT and exchange it for an OAuth2 access token."""
    try:
        from cryptography.hazmat.primitives import hashes, serialization
        from cryptography.hazmat.primitives.asymmetric import padding
    except ImportError:
        die("cryptography is required: pip install cryptography")

    for field in ("client_email", "private_key", "project_id"):
        if not sa.get(field):
            die(f"service-account JSON is missing '{field}' — is this the right file?")

    now = int(time.time())
    header = {"alg": "RS256", "typ": "JWT"}
    claims = {
        "iss": sa["client_email"],
        "scope": SCOPE,
        "aud": TOKEN_URI,
        "iat": now,
        "exp": now + 3600,
    }
    signing_input = (
        b64url(json.dumps(header, separators=(",", ":")).encode())
        + "."
        + b64url(json.dumps(claims, separators=(",", ":")).encode())
    ).encode()

    key = serialization.load_pem_private_key(sa["private_key"].encode(), password=None)
    signature = key.sign(signing_input, padding.PKCS1v15(), hashes.SHA256())
    assertion = signing_input.decode() + "." + b64url(signature)

    body = urllib.parse.urlencode({
        "grant_type": "urn:ietf:params:oauth:grant-type:jwt-bearer",
        "assertion": assertion,
    }).encode()
    req = urllib.request.Request(
        TOKEN_URI, data=body,
        headers={"Content-Type": "application/x-www-form-urlencoded"},
    )
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            return json.load(r)["access_token"]
    except urllib.error.HTTPError as e:
        detail = e.read().decode(errors="replace")
        die(f"could not get an access token ({e.code}).\n  {detail}")
    raise AssertionError


def build_message(args: argparse.Namespace) -> dict:
    target = {"token": args.token} if args.token else {"topic": args.topic}
    msg: dict = dict(target)
    # HIGH so the message is not deferred while the device dozes, which otherwise
    # makes a test look like it silently failed.
    android: dict = {"priority": "HIGH"}

    if args.type in ("notification", "both"):
        msg["notification"] = {"title": args.title, "body": args.body}
        android["notification"] = {"channel_id": CHANNEL_ID}
    if args.type in ("data", "both"):
        # Keys must match PushMessagingService.KEY_TITLE / KEY_BODY.
        msg["data"] = {"title": args.title, "body": args.body}

    msg["android"] = android
    return {"message": msg, **({"validate_only": True} if args.dry_run else {})}


def main() -> int:
    p = argparse.ArgumentParser(
        description="Send a test push to GBCam Companion via FCM HTTP v1.",
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    p.add_argument("--key", help="service-account JSON (default: $GCAC_FCM_KEY or ./fcm-key.json)")
    dest = p.add_mutually_exclusive_group()
    dest.add_argument("--token", help="send to one device (see logcat -s PushClient)")
    dest.add_argument("--topic", default=DEFAULT_TOPIC, help=f"topic to broadcast to (default: {DEFAULT_TOPIC})")
    p.add_argument("--title", default="GBCam Companion", help="notification title")
    p.add_argument("--body", default="Test announcement", help="notification body")
    p.add_argument("--type", choices=("notification", "data", "both"), default="notification",
                   help="payload style; see the module docstring for why both matter")
    p.add_argument("--dry-run", action="store_true", help="ask FCM to validate without delivering")
    args = p.parse_args()

    key_path = find_key(args.key)
    assert_key_untracked(key_path)

    try:
        sa = json.loads(key_path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as e:
        die(f"{key_path} is not valid JSON: {e}")

    project = sa["project_id"]
    payload = build_message(args)

    where = f"token {args.token[:12]}…" if args.token else f"topic '{args.topic}'"
    print(f"project : {project}")
    print(f"target  : {where}")
    print(f"type    : {args.type}{'  (validate only)' if args.dry_run else ''}")

    req = urllib.request.Request(
        FCM_URL.format(project=project),
        data=json.dumps(payload).encode(),
        headers={
            "Authorization": f"Bearer {access_token(sa)}",
            "Content-Type": "application/json; charset=UTF-8",
        },
    )
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            name = json.load(r).get("name", "(no name returned)")
        print(f"\nsent ok : {name}")
        if args.dry_run:
            print("note    : validate_only — nothing was delivered")
        return 0
    except urllib.error.HTTPError as e:
        detail = e.read().decode(errors="replace")
        print(f"\nFCM rejected the message ({e.code}):\n{detail}", file=sys.stderr)
        if e.code == 404:
            print("hint: UNREGISTERED usually means a stale device token — "
                  "relaunch the app and re-read it from logcat.", file=sys.stderr)
        if e.code == 403:
            print("hint: check Cloud Messaging is enabled for the project and that "
                  "the service account has the Firebase Messaging role.", file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
