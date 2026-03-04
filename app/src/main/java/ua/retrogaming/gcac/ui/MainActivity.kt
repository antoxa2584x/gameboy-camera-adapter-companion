package ua.retrogaming.gcac.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.runtime.livedata.observeAsState
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
import coil3.compose.AsyncImage
import com.chibatching.kotpref.livedata.asLiveData
import ua.retrogaming.gcac.R
import ua.retrogaming.gcac.data.prefs.DeviceData
import ua.retrogaming.gcac.data.prefs.ImageCache
import ua.retrogaming.gcac.data.prefs.UpdateCheckData
import ua.retrogaming.gcac.ui.theme.BackgroundColor
import ua.retrogaming.gcac.ui.theme.CameraAdapterCompanionTheme
import ua.retrogaming.gcac.ui.theme.DarkRed
import ua.retrogaming.gcac.ui.theme.PressStart2P
import ua.retrogaming.gcac.ui.theme.SecondaryBackgroundColor
import ua.retrogaming.gcac.ui.view.GalleryView
import ua.retrogaming.gcac.ui.view.ImagePopup
import ua.retrogaming.gcac.ui.view.SettingsPopup
import java.util.Locale

class MainActivity : ComponentActivity() {
    val connected = DeviceData.asLiveData(DeviceData::deviceConnected)
    val updateAvailable = UpdateCheckData.asLiveData(UpdateCheckData::isUpdateAvailable)
    val isPrinting = ImageCache.asLiveData(ImageCache::isPrinting)
    val currentPhoto = ImageCache.asLiveData(ImageCache::currentPhoto)
    val ledStatus = DeviceData.asLiveData(DeviceData::ledStatus)
    val recentCache = ImageCache.asLiveData(ImageCache::photos)

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

        setContent {
            CameraAdapterCompanionTheme {
                val isLandscape = isLandscape()

                val led by ledStatus.observeAsState(DeviceData.ledStatus)
                var ledModalOpen by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    DeviceData.asLiveData(DeviceData::language).observe(this@MainActivity) { newLang ->
                        val currentLocale = resources.configuration.locales[0].language
                        if (newLang != null && newLang != currentLocale) {
                            updateLocale(newLang)
                            recreate()
                        }
                    }
                }

                val pathsSet by recentCache.observeAsState(initial = ImageCache.photos)
                val photoSubscription by currentPhoto.observeAsState(ImageCache.currentPhoto)
                val paths = pathsSet.sortedByDescending { it.created }
                var showDeleteAllDialog by remember { mutableStateOf(false) }
                var fabExpanded by remember { mutableStateOf(false) }

                val anyPopupOpen = (ledModalOpen && led != null) || photoSubscription != null

                if (anyPopupOpen) {
                    BackHandler {
                        if (photoSubscription != null) {
                            ImageCache.currentPhoto = null
                        } else if (ledModalOpen) {
                            ledModalOpen = false
                        }
                    }
                }

                if (ledModalOpen && led == null) {
                    ledModalOpen = false
                }

                if (showDeleteAllDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteAllDialog = false },
                        title = { Text(stringResource(R.string.remove_confirm_title), color = Color.White) },
                        text = { Text(stringResource(R.string.remove_all_confirm_msg), color = Color.White) },
                        confirmButton = {
                            TextButton(onClick = {
                                ImageCache.removeAll()
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
                        if (paths.isNotEmpty() && !anyPopupOpen) {
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
                                    GalleryView().ActionButtons(
                                        paths = paths,
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
                            if (led != null) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(top = 36.dp, end = 10.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    LedStatus(led?.hex) {
                                        ledModalOpen = true
                                    }
                                }
                            }

                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                AsyncImage(
                                    modifier = Modifier
                                        .fillMaxWidth(if (!isLandscape) 0.7f else 0.3f)
                                        .padding(
                                            top = if (!isLandscape) 48.dp else 20.dp, bottom = 10.dp
                                        ),
                                    model = "file:///android_asset/logo.webp",
                                    contentDescription = stringResource(R.string.app_name),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }

                        ConnectDevice()

                        GalleryView().PrintingGallery(isLandscape, bottomPadding)
                    }

                    if (ledModalOpen) SettingsPopup().Render({ ledModalOpen = false })

                    if (photoSubscription != null) {
                        ImagePopup().Render(photoSubscription)
                    }

                    ProgressIndicator()
                }
            }
        }
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
    fun PhotoModal() {
        // Already handled via anyPopupOpen and Scaffold content
    }

    @Composable
    fun ProgressIndicator() {
        val printing by isPrinting.observeAsState(ImageCache.isPrinting)

        if (printing) {
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
    fun ConnectDevice(modifier: Modifier = Modifier) {
        val connected by connected.observeAsState(DeviceData.deviceConnected)

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
        } else {
            UpdateAvailable()
        }
    }

    @Composable
    fun UpdateAvailable(modifier: Modifier = Modifier) {
        val updateAvailable by updateAvailable.observeAsState(UpdateCheckData.isUpdateAvailable)

        if (updateAvailable) Text(
            stringResource(R.string.firmware_update_available, UpdateCheckData.latestVersion),
            modifier
                .padding(6.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() }, indication = null
                ) {
                    val intent = Intent(Intent.ACTION_VIEW, UpdateCheckData.releaseUrl.toUri())
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
