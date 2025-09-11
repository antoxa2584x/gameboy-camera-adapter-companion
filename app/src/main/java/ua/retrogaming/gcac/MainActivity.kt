package ua.retrogaming.gcac

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import coil3.compose.AsyncImage
import com.chibatching.kotpref.livedata.asLiveData
import ua.retrogaming.gcac.prefs.DevicePrefs
import ua.retrogaming.gcac.prefs.ImagesCache
import ua.retrogaming.gcac.ui.theme.CameraAdapterCompanionTheme
import ua.retrogaming.gcac.view.GalleryView
import ua.retrogaming.gcac.view.ImageView

class MainActivity : ComponentActivity() {
    val connected = DevicePrefs.asLiveData(DevicePrefs::deviceConnected)
    val isPrinting = ImagesCache.asLiveData(ImagesCache::isPrinting)
    val currentPhoto = ImagesCache.asLiveData(ImagesCache::currentPhoto)
    val ledStatus = DevicePrefs.asLiveData(DevicePrefs::ledStatus)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(
                Color.Transparent.toArgb()
            ),
            navigationBarStyle = SystemBarStyle.dark(
                Color.Transparent.toArgb(),
            )
        )

        setContent {
            CameraAdapterCompanionTheme {
                val isLandscape = isLandscape()

                val led by ledStatus.observeAsState(DevicePrefs.ledStatus)

                Scaffold(modifier = Modifier.fillMaxSize()) { _ ->
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AsyncImage(
                            modifier = Modifier
                                .fillMaxWidth(if (!isLandscape) 0.7f else 0.3f)
                                .padding(top = if (!isLandscape) 48.dp else 20.dp, bottom = 10.dp),
                            model = "file:///android_asset/logo.webp",
                            contentDescription = "logo",
                            contentScale = ContentScale.Fit
                        )

                        led?.let {
                            Text(text = if(it.useRgb) "RGB" else "GRB", color = Color(it.hex.toColorInt()) )
                        }

                        ConnectDevice()
                        GalleryView().PrintingGallery(isLandscape)
                    }

                    PhotoModal()
                    ProgressIndicator()
                }
            }
        }
    }

    @Composable
    fun PhotoModal() {
        val photoSubscription by currentPhoto.observeAsState(ImagesCache.currentPhoto)

        AnimatedVisibility(
            visible = photoSubscription != null,
            enter = fadeIn() + scaleIn(initialScale = 0.9f),            // pop-in
            exit = fadeOut() + scaleOut(targetScale = 0.9f)              // pop-out
        ) {
            ImageView().Render(ImagesCache.currentPhoto)
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
                    strokeWidth = 4.dp,
                    color = Color.White
                )
            }
        }
    }

    @Composable
    fun ConnectDevice(modifier: Modifier = Modifier) {
        val connected by connected.observeAsState(initial = DevicePrefs.deviceConnected)
        if (!connected)
            Text(
                "Connect your Adapter",
                modifier.padding(10.dp),
                fontSize = 14.sp,
                color = Color.Yellow
            )
    }

    @Composable
    fun isLandscape(): Boolean {
        val configuration = LocalConfiguration.current
        return configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

}
