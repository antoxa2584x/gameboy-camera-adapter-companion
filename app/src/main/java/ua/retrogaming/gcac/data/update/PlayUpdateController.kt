package ua.retrogaming.gcac.data.update

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Drives app updates through Google Play's in-app update API.
 *
 * Play is the only permitted update channel for a Play-distributed build, so the
 * app never downloads or installs anything itself — [AppUpdateManager] hands the
 * download to the Play Store and we only prompt for the final restart.
 *
 * A FLEXIBLE update is used deliberately: the download runs in the background so
 * an in-progress photo transfer or print job is never interrupted.
 *
 * Construct this as a field of the activity — [registerForActivityResult] must be
 * called before the activity reaches STARTED.
 */
class PlayUpdateController(private val activity: ComponentActivity) : DefaultLifecycleObserver {

    /**
     * Lazy because the activity has no attached base context while its fields are
     * being initialised; first touched from [onCreate].
     */
    private val manager: AppUpdateManager by lazy { AppUpdateManagerFactory.create(activity) }

    private val _readyToInstall = MutableStateFlow(false)

    /** True once Play has finished downloading an update; applying it restarts the app. */
    val readyToInstall: StateFlow<Boolean> = _readyToInstall.asStateFlow()

    private val launcher: ActivityResultLauncher<IntentSenderRequest> =
        activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
            // Declining just leaves the installed version in place; Play re-offers later.
        }

    private val installListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            _readyToInstall.value = true
        }
    }

    init {
        activity.lifecycle.addObserver(this)
    }

    override fun onCreate(owner: LifecycleOwner) {
        manager.registerListener(installListener)
        checkForUpdate()
    }

    override fun onResume(owner: LifecycleOwner) {
        // A download that completed while the app was backgrounded still needs the
        // restart prompt, since the listener callback was missed.
        manager.appUpdateInfo
            .addOnSuccessListener { info ->
                if (info.installStatus() == InstallStatus.DOWNLOADED) {
                    _readyToInstall.value = true
                }
            }
            .addOnFailureListener(::logUnavailable)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        manager.unregisterListener(installListener)
    }

    private fun checkForUpdate() {
        manager.appUpdateInfo
            .addOnSuccessListener { info ->
                if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                    info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                ) {
                    startFlow(info)
                }
            }
            .addOnFailureListener(::logUnavailable)
    }

    private fun startFlow(info: AppUpdateInfo) {
        try {
            manager.startUpdateFlowForResult(
                info,
                launcher,
                AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
            )
        } catch (e: Exception) {
            Log.w(TAG, "Could not start update flow", e)
        }
    }

    /** Ask Play to install the downloaded update. This restarts the app. */
    fun completeUpdate() {
        manager.completeUpdate()
    }

    private fun logUnavailable(t: Throwable) {
        // Expected on debug and sideloaded installs — Play doesn't own the package.
        Log.d(TAG, "In-app updates unavailable: ${t.message}")
    }

    private companion object {
        const val TAG = "PlayUpdate"
    }
}
