package ua.retrogaming.gcac.data.repository

import com.chibatching.kotpref.bulk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import ua.retrogaming.gcac.core.Version
import ua.retrogaming.gcac.data.prefs.UpdateCheckData
import ua.retrogaming.gcac.model.UpdateCheckResult
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Checks GitHub releases for adapter-firmware and companion-app updates.
 * Results are exposed as [StateFlow]s and persisted via [UpdateCheckData].
 */
class UpdateRepository {

    data class FirmwareUpdate(val version: String, val releaseUrl: String)
    data class AppUpdate(val version: String, val releaseUrl: String, val skipped: Boolean)

    private val _firmwareUpdate = MutableStateFlow(loadFirmwareUpdate())
    val firmwareUpdate: StateFlow<FirmwareUpdate?> = _firmwareUpdate.asStateFlow()

    private val _appUpdate = MutableStateFlow(loadAppUpdate())
    val appUpdate: StateFlow<AppUpdate?> = _appUpdate.asStateFlow()

    /** Compare the connected adapter's firmware [currentVersion] against the latest release. */
    suspend fun checkFirmwareUpdate(currentVersion: String) {
        val result = checkGitHubRelease(FIRMWARE_REPO, currentVersion)
        if (result.isUpdateAvailable && result.latestVersion != null && result.releaseUrl != null) {
            UpdateCheckData.bulk {
                isUpdateAvailable = true
                latestVersion = result.latestVersion
                releaseUrl = result.releaseUrl
            }
            _firmwareUpdate.value = FirmwareUpdate(result.latestVersion, result.releaseUrl)
        }
    }

    /** Compare the installed app [currentVersion] (versionName) against the latest release. */
    suspend fun checkAppUpdate(currentVersion: String) {
        if (currentVersion.isBlank()) return

        val result = checkGitHubRelease(APP_REPO, currentVersion)
        if (result.error != null) return // keep cached state on network errors

        UpdateCheckData.bulk {
            isAppUpdateAvailable = result.isUpdateAvailable &&
                    result.latestVersion != null && result.releaseUrl != null
            appLatestVersion = result.latestVersion ?: ""
            appReleaseUrl = result.releaseUrl ?: ""
        }
        _appUpdate.value = loadAppUpdate()
    }

    /** Don't show the app-update dialog again until a newer version appears. */
    fun skipAppUpdate() {
        val update = _appUpdate.value ?: return
        UpdateCheckData.appUpdateSkippedVersion = update.version
        _appUpdate.value = update.copy(skipped = true)
    }

    private fun loadFirmwareUpdate(): FirmwareUpdate? = with(UpdateCheckData) {
        if (isUpdateAvailable && releaseUrl.isNotEmpty()) FirmwareUpdate(latestVersion, releaseUrl) else null
    }

    private fun loadAppUpdate(): AppUpdate? = with(UpdateCheckData) {
        if (isAppUpdateAvailable && appLatestVersion.isNotEmpty() && appReleaseUrl.isNotEmpty()) {
            AppUpdate(appLatestVersion, appReleaseUrl, skipped = appLatestVersion == appUpdateSkippedVersion)
        } else null
    }

    private suspend fun checkGitHubRelease(
        repo: String,
        currentVersion: String
    ): UpdateCheckResult = withContext(Dispatchers.IO) {
        val url = URL("https://api.github.com/repos/$repo/releases/latest")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "Android-${repo.replace('/', '-')}-UpdateCheck")
        }

        try {
            val code = conn.responseCode
            if (code !in 200..299) {
                return@withContext UpdateCheckResult(false, null, null, RuntimeException("HTTP $code"))
            }

            val body = conn.inputStream.use { `is` ->
                BufferedReader(InputStreamReader(`is`)).readText()
            }

            val json = JSONObject(body)
            val rawName = json.optString("name", "").ifEmpty { json.optString("tag_name", "") }
            val latestVersion = rawName.replace(Regex("^v"), "") // drop leading "v"
            val releaseUrl = json.optString("html_url", "")

            UpdateCheckResult(
                isUpdateAvailable = isNewerVersion(latestVersion, currentVersion),
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

    private fun isNewerVersion(latest: String, current: String): Boolean =
        latest.isNotBlank() && current.isNotBlank() && Version.compare(latest, current) > 0

    companion object {
        private const val FIRMWARE_REPO = "antoxa2584x/gameboy-camera-adapter"
        private const val APP_REPO = "antoxa2584x/gameboy-camera-adapter-companion"
    }
}
