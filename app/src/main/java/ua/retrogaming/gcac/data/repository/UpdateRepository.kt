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
 * Checks GitHub releases for adapter-firmware updates. Results are exposed as a
 * [StateFlow] and persisted via [UpdateCheckData].
 *
 * This covers the *adapter hardware* only. Companion-app updates go through
 * Google Play's in-app update API — see
 * [ua.retrogaming.gcac.data.update.PlayUpdateController] — because a
 * Play-distributed app may not update itself from any other source.
 */
class UpdateRepository {

    data class FirmwareUpdate(val version: String, val releaseUrl: String)

    private val _firmwareUpdate = MutableStateFlow(loadFirmwareUpdate())
    val firmwareUpdate: StateFlow<FirmwareUpdate?> = _firmwareUpdate.asStateFlow()

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

    private fun loadFirmwareUpdate(): FirmwareUpdate? = with(UpdateCheckData) {
        if (isUpdateAvailable && releaseUrl.isNotEmpty()) FirmwareUpdate(latestVersion, releaseUrl) else null
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
    }
}
