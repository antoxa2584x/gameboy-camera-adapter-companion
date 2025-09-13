package ua.retrogaming.gcac.services

import com.chibatching.kotpref.bulk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import ua.retrogaming.gcac.model.UpdateCheckResult
import ua.retrogaming.gcac.prefs.DeviceData
import ua.retrogaming.gcac.prefs.UpdateCheckData
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL


class UpdateCheckService {

    fun checkUpdate() {
        CoroutineScope(Dispatchers.IO).launch {
            val result = checkGitHubRelease(
                repo = "antoxa2584x/gameboy-camera-adapter",
                currentVersion = DeviceData.deviceVersion ?: "0.0.0"
            )

            if (result.isUpdateAvailable && result.latestVersion != null && result.releaseUrl != null) {
                UpdateCheckData.bulk {
                    isUpdateAvailable = true
                    latestVersion = result.latestVersion
                    releaseUrl = result.releaseUrl
                }
            } else if (result.error != null) {
                // Log or show a non-blocking error
            }
        }
    }

    /**
     * Checks the latest GitHub Release for the given repo and compares it with [currentVersion].
     * Mirrors the behavior of the provided JS function.
     *
     * @param repo "owner/repo", e.g. "antoxa2584x/gameboy-camera-adapter"
     * @param currentVersion your app's current version (e.g. BuildConfig.VERSION_NAME)
     */
   private suspend fun checkGitHubRelease(
        repo: String = "antoxa2584x/gameboy-camera-adapter",
        currentVersion: String
    ): UpdateCheckResult = withContext(Dispatchers.IO) {
        val url = URL("https://api.github.com/repos/$repo/releases/latest")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            // Helpful for GitHub API
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "Android-${repo.replace('/', '-')}-UpdateCheck")
        }

        try {
            val code = conn.responseCode
            if (code !in 200..299) {
                return@withContext UpdateCheckResult(
                    false,
                    null,
                    null,
                    RuntimeException("HTTP $code")
                )
            }

            val body = conn.inputStream.use { `is` ->
                BufferedReader(InputStreamReader(`is`)).readText()
            }

            val json = JSONObject(body)
            val rawName = json.optString("name", "")
            val latestVersion = rawName.replace(Regex("^v"), "") // drop leading "v"
            val releaseUrl = json.optString("html_url", "")

            val isNewer = isNewerVersion(latestVersion, currentVersion)

            UpdateCheckResult(
                isUpdateAvailable = isNewer,
                latestVersion = latestVersion.ifEmpty { null },
                releaseUrl = releaseUrl.ifEmpty { null },
                error = null
            )
        } catch (t: Throwable) {
            UpdateCheckResult(false, null, null, t)
        } finally {
            conn.disconnect()
        }
    }

    /**
     * Simple semver-ish comparison that handles dotted versions like "1.2.10" vs "1.2.9".
     * Non-numeric parts are compared lexicographically and considered after numeric comparison.
     */
   private fun isNewerVersion(latest: String, current: String): Boolean {
        if (latest.isBlank() || current.isBlank()) return false

        // Split on dots and hyphens to support "1.2.3-beta.1"
        val a = latest.split('.', '-')
        val b = current.split('.', '-')
        val max = maxOf(a.size, b.size)

        for (i in 0 until max) {
            val ai = a.getOrNull(i) ?: "0"
            val bi = b.getOrNull(i) ?: "0"

            val aiNum = ai.toIntOrNull()
            val biNum = bi.toIntOrNull()

            val cmp = when {
                aiNum != null && biNum != null -> aiNum.compareTo(biNum)
                aiNum != null && biNum == null -> 1              // numeric > text
                aiNum == null && biNum != null -> -1             // text < numeric
                else -> ai.compareTo(bi)                          // both text (e.g., "beta" vs "rc")
            }

            if (cmp != 0) return cmp > 0
        }
        return false // equal
    }


}