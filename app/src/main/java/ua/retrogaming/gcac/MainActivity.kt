package ua.retrogaming.gcac

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import coil3.compose.AsyncImage
import com.chibatching.kotpref.livedata.asLiveData
import ua.retrogaming.gcac.prefs.DeviceData
import ua.retrogaming.gcac.prefs.ImagesCache
import ua.retrogaming.gcac.prefs.UpdateCheckData
import ua.retrogaming.gcac.ui.theme.CameraAdapterCompanionTheme
import ua.retrogaming.gcac.ui.theme.PressStart2P
import ua.retrogaming.gcac.ui.view.GalleryView
import ua.retrogaming.gcac.ui.view.ImagePopup
import ua.retrogaming.gcac.ui.view.LedPopup
import androidx.core.net.toUri

class MainActivity : ComponentActivity() {
    val connected = DeviceData.asLiveData(DeviceData::deviceConnected)
    val updateAvailable = UpdateCheckData.asLiveData(UpdateCheckData::isUpdateAvailable)
    val isPrinting = ImagesCache.asLiveData(ImagesCache::isPrinting)
    val currentPhoto = ImagesCache.asLiveData(ImagesCache::currentPhoto)
    val ledStatus = DeviceData.asLiveData(DeviceData::ledStatus)

    override fun onCreate(savedInstanceState: Bundle?) {
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

                Scaffold(modifier = Modifier.fillMaxSize()) { _ ->
                    Column(
                        modifier = Modifier.fillMaxSize(),
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
                                    contentDescription = "logo",
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }

                        ConnectDevice()

                        GalleryView().PrintingGallery(isLandscape)
                    }

                    if (ledModalOpen) LedPopup().Render({ ledModalOpen = false })

                    PhotoModal()
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

        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(Color(color?.toColorInt() ?: 0))
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
        val photoSubscription by currentPhoto.observeAsState(ImagesCache.currentPhoto)

        if (photoSubscription != null) {
            ImagePopup().Render(ImagesCache.currentPhoto)
        }
    }

    @Composable
    fun ProgressIndicator() {
        val printing by isPrinting.observeAsState(ImagesCache.isPrinting)

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
                CircularProgressIndicator(
                    strokeWidth = 4.dp, color = Color.White
                )
            }
        }
    }

    @Composable
    fun ConnectDevice(modifier: Modifier = Modifier) {
        val connected by connected.observeAsState(DeviceData.deviceConnected)

        if (!connected) Text(
            "Connect your Adapter", modifier.padding(10.dp), fontSize = 14.sp, color = Color.Yellow
        )
        else UpdateAvailable()
    }

    @Composable
    fun UpdateAvailable(modifier: Modifier = Modifier) {
        val updateAvailable by updateAvailable.observeAsState(UpdateCheckData.isUpdateAvailable)

        if (updateAvailable) Text(
            "Firmware update\navailable: v${UpdateCheckData.latestVersion}",
            modifier
                .padding(10.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() }, indication = null
                ) {
                    val intent = Intent(Intent.ACTION_VIEW, UpdateCheckData.releaseUrl.toUri())
                    startActivity(intent)
                },
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            color = Color.Yellow,
            style = TextStyle(
                fontFamily = PressStart2P,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.5.sp,
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
