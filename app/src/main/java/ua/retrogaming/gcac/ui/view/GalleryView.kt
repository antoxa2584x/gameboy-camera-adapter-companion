package ua.retrogaming.gcac.ui.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chibatching.kotpref.livedata.asLiveData
import org.koin.core.component.KoinComponent
import ua.retrogaming.gcac.model.PhotoData
import ua.retrogaming.gcac.prefs.ImagesCache
import ua.retrogaming.gcac.ui.component.ColorSchemeCircle
import ua.retrogaming.gcac.ui.component.ColorSchemeSelector
import ua.retrogaming.gcac.ui.component.GbPaletteImage


class GalleryView() : KoinComponent {
    private val recentCache = ImagesCache.asLiveData(ImagesCache::photos)

    val colorScheme = ImagesCache.asLiveData(ImagesCache::colorScheme)

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
    fun PrintingGallery(isLandscape: Boolean) {
        Box(modifier = Modifier.fillMaxSize()) {
            ShowGallery(modifier = Modifier.fillMaxSize(), isLandscape)
        }
    }

    @Composable
    fun ShowGallery(modifier: Modifier = Modifier, isLandscape: Boolean) {
        val pathsSet by recentCache.observeAsState(initial = ImagesCache.photos)
        val paths = pathsSet.sortedByDescending { it.created }

        if (paths.isEmpty()) {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Start printing on your Game Boy\n" +
                            "Photos will appear here",
                    modifier
                        .padding(40.dp)
                        .wrapContentHeight(align = Alignment.CenterVertically),
                    textAlign = TextAlign.Center,
                    fontSize = 10.sp,
                    color = Color.White
                )
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ColorSchemeSelector(
                    schemes = schemes,
                    onSchemeSelected = { scheme ->
                        ImagesCache.colorScheme = scheme.name
                    }
                )

                ImageGrid(isLandscape, paths = paths, onClick = {
                    ImagesCache.currentPhoto = it
                }, modifier = modifier)
            }

        }
    }

    @Composable
    fun ImageGrid(
        isLandscape: Boolean,
        paths: List<PhotoData>,
        modifier: Modifier = Modifier,
        onClick: (PhotoData) -> Unit = {}
    ) {
        val photoSubscription by colorScheme.observeAsState(ImagesCache.colorScheme)

        LazyVerticalGrid(
            columns = GridCells.Fixed(if (isLandscape) 6 else 3),
            contentPadding = PaddingValues(8.dp),
            modifier = modifier.fillMaxSize(),
        ) {
            items(
                items = paths,
                key = { it } // stable key = file path
            ) { photo ->
                GbPaletteImage(
                    data = photo.path,
                    scheme = photoSubscription,
                    modifier = Modifier
                        .aspectRatio(1.14f)// square cells
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { onClick(photo) }
                        .padding(4.dp)
                )
            }
        }
    }
}