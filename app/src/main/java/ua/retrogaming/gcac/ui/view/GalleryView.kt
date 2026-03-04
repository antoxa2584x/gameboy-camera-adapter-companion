package ua.retrogaming.gcac.ui.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chibatching.kotpref.livedata.asLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ua.retrogaming.gcac.R
import ua.retrogaming.gcac.core.image.PocketCameraPalettes
import ua.retrogaming.gcac.data.image.ImageSaver
import ua.retrogaming.gcac.data.prefs.ImageCache
import ua.retrogaming.gcac.model.PhotoData
import ua.retrogaming.gcac.ui.MainActivity
import ua.retrogaming.gcac.ui.component.ColorSchemeCircle
import ua.retrogaming.gcac.ui.component.ColorSchemeSelector
import ua.retrogaming.gcac.ui.component.GbPaletteImage
import ua.retrogaming.gcac.ui.component.GreenButton
import ua.retrogaming.gcac.ui.theme.DarkRed
import ua.retrogaming.gcac.util.ViewHelper


class GalleryView() : KoinComponent {
    private val recentCache = ImageCache.asLiveData(ImageCache::photos)
    private val imageSaver: ImageSaver by inject()
    private val viewHelper: ViewHelper by inject()

    val colorScheme = ImageCache.asLiveData(ImageCache::colorScheme)

    private val schemes = listOf(
        ColorSchemeCircle(
            "grayscale",
            listOf(Color(0xFFFFFFFF), Color(0xFFBFBFBF), Color(0xFF7F7F7F), Color(0xFF3F3F3F))
        ),
        ColorSchemeCircle(
            "game-boy",
            listOf(Color(0xFFD0D93C), Color(0xFF78A46A), Color(0xFF545854), Color(0xFF244624))
        ),
        ColorSchemeCircle(
            "super-game-boy",
            listOf(Color(0xFFFFFFFF), Color(0xFFB5B3BD), Color(0xFF545367), Color(0xFF090713))
        ),
        ColorSchemeCircle(
            "game-boy-color-jpn",
            listOf(Color(0xFFF0F0F0), Color(0xFFDAC46A), Color(0xFF705834), Color(0xFF1E1E1E))
        ),
        ColorSchemeCircle(
            "game-boy-color-usa-gold",
            listOf(Color(0xFFF0F0F0), Color(0xFFDCA0A0), Color(0xFF884E4E), Color(0xFF1E1E1E))
        ),
        ColorSchemeCircle(
            "game-boy-color-usa-eur",
            listOf(Color(0xFFF0F0F0), Color(0xFF86C864), Color(0xFF3A6084), Color(0xFF1E1E1E))
        )
    )

    @Composable
    fun PrintingGallery(isLandscape: Boolean, bottomPadding: Dp = 0.dp) {
        Column(modifier = Modifier.fillMaxSize()) {
            ShowGallery(modifier = Modifier.fillMaxSize(), isLandscape, bottomPadding)
        }
    }

    @Composable
    fun ShowGallery(modifier: Modifier = Modifier, isLandscape: Boolean, bottomPadding: Dp = 0.dp) {
        val pathsSet by recentCache.observeAsState(initial = ImageCache.photos)
        val paths = pathsSet.sortedByDescending { it.created }

        if (paths.isEmpty()) {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.start_printing_hint),
                    modifier
                        .padding(24.dp)
                        .wrapContentHeight(align = Alignment.CenterVertically),
                    textAlign = TextAlign.Center,
                    fontSize = 8.sp,
                    color = Color.White
                )
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ColorSchemeSelector(
                    schemes = schemes,
                    onSchemeSelected = { scheme ->
                        ImageCache.colorScheme = scheme.name
                    }
                )

                ImageGrid(isLandscape, paths = paths, onClick = {
                    ImageCache.currentPhoto = it
                }, modifier = modifier.weight(1f), bottomPadding = bottomPadding)
            }

        }
    }

    @Composable
    fun ActionButtons(paths: List<PhotoData>, onDeleteAllRequest: () -> Unit) {
        val saveAllSuccessText = stringResource(R.string.all_saved)
        val saveErrorText = stringResource(R.string.save_error)
        val coroutineScope = rememberCoroutineScope()
        val context = LocalContext.current

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GreenButton().Render(
                fillMaxWidth = false,
                text = stringResource(R.string.save_all),
                onClick = {
                    (context as? MainActivity)?.checkStoragePermission { isGranted ->
                        if (!isGranted) return@checkStoragePermission

                        coroutineScope.launch(Dispatchers.IO) {
                            withContext(Dispatchers.Main) {
                                ImageCache.isPrinting = true
                            }
                            var allSuccess = true
                            val colorScheme = ImageCache.colorScheme
                            paths.forEach { photo ->
                                val result = imageSaver.saveImageJpegScoped(
                                    data = photo,
                                    opts = ImageSaver.SaveOptions(
                                        scale = 20,
                                        colorSchemeName = colorScheme,
                                        filter = ImageSaver.ImageFilter.PocketPalette(
                                            palette = PocketCameraPalettes.findPalletByName(colorScheme)
                                        )
                                    )
                                )
                                if (result == null) allSuccess = false
                            }
                            withContext(Dispatchers.Main) {
                                viewHelper.showToast(if (allSuccess) saveAllSuccessText else saveErrorText)
                                ImageCache.isPrinting = false
                            }
                        }
                    }
                }
            )

            GreenButton().Render(
                fillMaxWidth = false,
                text = stringResource(R.string.remove_all),
                containerColor = DarkRed,
                onClick = onDeleteAllRequest
            )
        }
    }

    @Composable
    fun ImageGrid(
        isLandscape: Boolean,
        paths: List<PhotoData>,
        modifier: Modifier = Modifier,
        onClick: (PhotoData) -> Unit = {},
        bottomPadding: Dp = 0.dp
    ) {
        val scheme by colorScheme.observeAsState(ImageCache.colorScheme)

        LazyVerticalGrid(
            columns = GridCells.Fixed(if (isLandscape) 6 else 3),
            contentPadding = PaddingValues(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 120.dp + bottomPadding),
            modifier = modifier.fillMaxSize(),
        ) {
            items(
                items = paths,
                key = { it.path } // stable key = file path
            ) { photo ->
                GbPaletteImage(
                    data = photo.path,
                    scheme = scheme,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { onClick(photo) }
                        .padding(4.dp),
                    contentScale = ContentScale.FillWidth
                )
            }
        }
    }
}