package ua.retrogaming.gcac.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import org.koin.androidx.viewmodel.ext.android.viewModel
import ua.retrogaming.gcac.R
import ua.retrogaming.gcac.data.prefs.DeviceData
import ua.retrogaming.gcac.data.repository.UpdateRepository
import ua.retrogaming.gcac.data.update.PlayUpdateController
import ua.retrogaming.gcac.ui.theme.BackgroundColor
import ua.retrogaming.gcac.ui.theme.CameraAdapterCompanionTheme
import ua.retrogaming.gcac.ui.theme.DarkRed
import ua.retrogaming.gcac.ui.theme.PressStart2P
import ua.retrogaming.gcac.ui.theme.SecondaryBackgroundColor
import ua.retrogaming.gcac.ui.view.GalleryActionButtons
import ua.retrogaming.gcac.ui.view.ImagePopup
import ua.retrogaming.gcac.ui.view.PrintScreen
import ua.retrogaming.gcac.ui.view.PrintingGallery
import ua.retrogaming.gcac.ui.view.SettingsPopup
import java.util.Locale

/**
 * Main-screen logo, fetched at runtime so it can be changed without shipping an
 * app update. Coil disk-caches the result, so this costs one request per cache
 * lifetime rather than one per launch.
 */
private const val LOGO_URL = "https://rgaming.com.ua/camera_adapter/assets/logo.webp"

/** Bundled copy: shown while the remote one loads, and kept if it never arrives. */
private const val LOGO_ASSET = "file:///android_asset/logo.webp"

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModel()

    /** Must stay a field initialiser — it registers an activity-result launcher. */
    private val playUpdate = PlayUpdateController(this)

    private var onPermissionResult: ((Boolean) -> Unit)? = null

    fun checkStoragePermission(onResult: (Boolean) -> Unit) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            onResult(true)
            return
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            onResult(true)
        } else {
            onPermissionResult = onResult
            requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        onPermissionResult?.invoke(isGranted)
        onPermissionResult = null
    }

    private val requestNotificationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        // Declining only means no announcements — nothing in the app depends on it.
    }

    /**
     * Announcements are opt-in from API 33. Asked once per launch at most; the
     * system stops showing the dialog itself after two dismissals, so there is no
     * need to track that here.
     */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) requestNotificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun updateLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        updateLocale(DeviceData.language)
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(
                Color.Transparent.toArgb()
            ), navigationBarStyle = SystemBarStyle.dark(
                Color.Transparent.toArgb(),
            )
        )

        requestNotificationPermissionIfNeeded()

        setContent {
            CameraAdapterCompanionTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()

                MainEffects(state)

                MainScreen(state)
            }
        }
    }

    /** One-shot effects: toasts and locale changes. */
    @Composable
    private fun MainEffects(state: MainUiState) {
        LaunchedEffect(Unit) {
            viewModel.events.collect { event ->
                if (event is MainEvent.Message) {
                    Toast.makeText(this@MainActivity, getString(event.textRes), Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

        LaunchedEffect(state.language) {
            val currentLocale = resources.configuration.locales[0].language
            if (state.language != currentLocale) {
                updateLocale(state.language)
                recreate()
            }
        }
    }

    @Composable
    private fun MainScreen(state: MainUiState) {
        val isLandscape = isLandscape()

        var ledModalOpen by remember { mutableStateOf(false) }
        var printScreenOpen by remember { mutableStateOf(false) }
        var showPrintUpdateDialog by remember { mutableStateOf(false) }
        var showDeleteAllDialog by remember { mutableStateOf(false) }
        var fabExpanded by remember { mutableStateOf(false) }

        if (printScreenOpen) {
            BackHandler { printScreenOpen = false }

            PrintScreen(
                connected = state.connected,
                printSupported = state.printSupported,
                printState = state.printState,
                onPrint = viewModel::printBitmap,
                onClose = { printScreenOpen = false }
            )
            return
        }

        val anyPopupOpen = (ledModalOpen && state.ledStatus != null) || state.currentPhoto != null

        if (anyPopupOpen) {
            BackHandler {
                if (state.currentPhoto != null) {
                    viewModel.closePhoto()
                } else if (ledModalOpen) {
                    ledModalOpen = false
                }
            }
        }

        if (ledModalOpen && state.ledStatus == null) {
            ledModalOpen = false
        }

        if (showPrintUpdateDialog) {
            AlertDialog(
                onDismissRequest = { showPrintUpdateDialog = false },
                title = {
                    Text(stringResource(R.string.print_update_required_title), color = Color.White)
                },
                text = {
                    Text(
                        stringResource(
                            R.string.print_update_required_msg,
                            MainViewModel.MIN_PRINT_FIRMWARE
                        ),
                        color = Color.White
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showPrintUpdateDialog = false }) {
                        Text(stringResource(R.string.confirm), color = Color.Yellow)
                    }
                },
                containerColor = BackgroundColor
            )
        }

        if (showDeleteAllDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteAllDialog = false },
                title = { Text(stringResource(R.string.remove_confirm_title), color = Color.White) },
                text = { Text(stringResource(R.string.remove_all_confirm_msg), color = Color.White) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.removeAll()
                        showDeleteAllDialog = false
                    }) {
                        Text(stringResource(R.string.confirm), color = DarkRed)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteAllDialog = false }) {
                        Text(stringResource(R.string.cancel), color = Color.White)
                    }
                },
                containerColor = BackgroundColor
            )
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            floatingActionButton = {
                if (!anyPopupOpen) {
                    Column(
                        modifier = Modifier.padding(bottom = 32.dp, end = 16.dp),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AnimatedVisibility(
                            visible = fabExpanded,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            GalleryActionButtons(
                                showGalleryActions = state.photos.isNotEmpty(),
                                onPrintRequest = {
                                    // Printing needs firmware > MIN_PRINT_FIRMWARE; with no
                                    // adapter attached the screen itself shows the connect hint
                                    if (state.connected && !state.printSupported) {
                                        showPrintUpdateDialog = true
                                    } else {
                                        printScreenOpen = true
                                    }
                                    fabExpanded = false
                                },
                                onSaveAll = {
                                    checkStoragePermission { isGranted ->
                                        if (isGranted) viewModel.saveAll()
                                    }
                                },
                                onDeleteAllRequest = {
                                    showDeleteAllDialog = true
                                    fabExpanded = false
                                }
                            )
                        }

                        FloatingActionButton(
                            onClick = { fabExpanded = !fabExpanded },
                            containerColor = SecondaryBackgroundColor,
                            contentColor = Color.White,
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = if (fabExpanded) Icons.Default.Close else Icons.Default.Menu,
                                contentDescription = "Menu"
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            val bottomPadding = paddingValues.calculateBottomPadding()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier.fillMaxWidth(), contentAlignment = Alignment.TopEnd
                ) {
                    if (state.ledStatus != null) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 36.dp, end = 10.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            LedStatus(state.ledStatus.hex) {
                                ledModalOpen = true
                            }
                        }
                    }

                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        // Used for all three states, so the logo is drawn from the
                        // bundle immediately and simply stays if the fetch fails.
                        // Photo transfer works with no connectivity at all, so this
                        // must never degrade to a blank gap or a layout jump.
                        val bundledLogo = rememberAsyncImagePainter(LOGO_ASSET)

                        AsyncImage(
                            modifier = Modifier
                                .fillMaxWidth(if (!isLandscape) 0.7f else 0.3f)
                                .padding(
                                    top = if (!isLandscape) 48.dp else 20.dp, bottom = 10.dp
                                ),
                            model = LOGO_URL,
                            contentDescription = stringResource(R.string.app_name),
                            contentScale = ContentScale.Fit,
                            placeholder = bundledLogo,
                            error = bundledLogo,
                            fallback = bundledLogo
                        )
                    }
                }

                ConnectDevice(state.connected, state.firmwareUpdate)

                PrintingGallery(
                    photos = state.photos,
                    colorScheme = state.colorScheme,
                    isLandscape = isLandscape,
                    onSchemeSelected = viewModel::selectColorScheme,
                    onPhotoClick = viewModel::openPhoto,
                    bottomPadding = bottomPadding
                )
            }

            if (ledModalOpen && state.ledStatus != null) {
                SettingsPopup(
                    ledStatus = state.ledStatus,
                    firmwareVersion = state.firmwareVersion,
                    language = state.language,
                    onSetLedColor = viewModel::setLedColor,
                    onSetLanguage = viewModel::setLanguage,
                    onSetMobileMode = viewModel::setMobileMode,
                    onCloseClick = { ledModalOpen = false }
                )
            }

            if (state.currentPhoto != null) {
                ImagePopup(
                    photo = state.currentPhoto,
                    colorScheme = state.colorScheme,
                    printState = state.printState,
                    showPrint = state.printSupported,
                    events = viewModel.events,
                    onSave = { photo ->
                        checkStoragePermission { isGranted ->
                            if (isGranted) viewModel.savePhoto(photo)
                        }
                    },
                    onPrint = viewModel::printPhoto,
                    onRemove = viewModel::removePhoto,
                    onClose = viewModel::closePhoto
                )
            }

            ProgressIndicator(state.isBusy)

            UpdateReadyDialog()
        }
    }

    /**
     * Play downloads updates in the background; the only thing left for us is to
     * ask for the restart that applies one. Play itself owns the "update available"
     * prompt, so there is no version-check dialog here.
     */
    @Composable
    private fun UpdateReadyDialog() {
        val readyToInstall by playUpdate.readyToInstall.collectAsStateWithLifecycle()
        var dismissed by remember { mutableStateOf(false) }

        if (!readyToInstall || dismissed) return

        AlertDialog(
            onDismissRequest = { dismissed = true },
            title = { Text(stringResource(R.string.update_available), color = Color.White) },
            text = { Text(stringResource(R.string.update_downloaded), color = Color.White) },
            confirmButton = {
                TextButton(onClick = { playUpdate.completeUpdate() }) {
                    Text(stringResource(R.string.update_restart), color = Color.Yellow)
                }
            },
            dismissButton = {
                TextButton(onClick = { dismissed = true }) {
                    Text(stringResource(R.string.cancel), color = Color.White)
                }
            },
            containerColor = BackgroundColor
        )
    }

    @Composable
    fun LedStatus(
        color: String?,
        size: Dp = 30.dp,
        borderWidth: Dp = 2.dp,
        onClick: () -> Unit,
    ) {
        val interaction = remember { MutableInteractionSource() }
        val infiniteTransition = rememberInfiniteTransition(label = "breath")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
        )

        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    try {
                        Color(color?.toColorInt() ?: 0).copy(alpha = alpha)
                    } catch (_: Exception) {
                        Color.Transparent
                    }
                )
                .border(borderWidth, Color.White, CircleShape)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    role = Role.Button,
                    onClick = onClick
                )
                .semantics { })
    }

    @Composable
    fun ProgressIndicator(isBusy: Boolean) {
        if (isBusy) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {}, // dark transparent bg
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        strokeWidth = 4.dp, color = Color.White
                    )
                    Text(
                        text = stringResource(R.string.is_printing),
                        color = Color.White,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }
    }

    @Composable
    fun ConnectDevice(
        connected: Boolean,
        firmwareUpdate: UpdateRepository.FirmwareUpdate?,
        modifier: Modifier = Modifier
    ) {
        if (!connected) {
            Column(
                modifier = modifier.padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.connect_adapter),
                    fontSize = 10.sp,
                    color = Color.Yellow
                )
                Text(
                    text = stringResource(R.string.min_adapter_version),
                    fontSize = 8.sp,
                    color = Color.Yellow.copy(alpha = 0.7f)
                )
            }
        } else if (firmwareUpdate != null) {
            UpdateAvailable(firmwareUpdate)
        }
    }

    @Composable
    fun UpdateAvailable(
        firmwareUpdate: UpdateRepository.FirmwareUpdate,
        modifier: Modifier = Modifier
    ) {
        Text(
            stringResource(R.string.firmware_update_available, firmwareUpdate.version),
            modifier
                .padding(6.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() }, indication = null
                ) {
                    val intent = Intent(Intent.ACTION_VIEW, firmwareUpdate.releaseUrl.toUri())
                    startActivity(intent)
                },
            textAlign = TextAlign.Center,
            color = Color.Yellow,
            style = TextStyle(
                fontFamily = PressStart2P,
                fontWeight = FontWeight.Normal,
                fontSize = 10.sp,
                lineHeight = 14.sp,
                letterSpacing = 0.sp,
                textDecoration = TextDecoration.Underline
            )
        )
    }

    @Composable
    fun isLandscape(): Boolean {
        val configuration = LocalConfiguration.current
        return configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }
}
