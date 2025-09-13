package ua.retrogaming.gcac.model

data class UpdateCheckResult(
    val isUpdateAvailable: Boolean,
    val latestVersion: String?,
    val releaseUrl: String?,
    val error: Throwable? = null
)