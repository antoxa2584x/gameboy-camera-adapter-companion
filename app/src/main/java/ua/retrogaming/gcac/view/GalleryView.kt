package ua.retrogaming.gcac.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.chibatching.kotpref.livedata.asLiveData
import org.koin.core.component.KoinComponent
import ua.retrogaming.gcac.model.PhotoData
import ua.retrogaming.gcac.prefs.ImagesCache
import java.io.File


class GalleryView(): KoinComponent {
    private val recentCache = ImagesCache.asLiveData(ImagesCache::photos)

    @Composable
    fun PrintingGallery(isLandscape: Boolean) {
        Box(modifier = Modifier.fillMaxSize()) {
            ShowGallery(modifier = Modifier.fillMaxSize(), isLandscape)
        }
    }

    @Composable
    fun ShowGallery(modifier: Modifier = Modifier, isLandscape: Boolean) {
        val pathsSet by recentCache.observeAsState(initial = ImagesCache.photos)
        val paths = pathsSet.sortedBy { it.created }

        if (paths.isEmpty()) {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center,) {
                Text(
                    "Start printing on your Game Boy\n" +
                            "Photos will appear here",
                    modifier.padding(40.dp).wrapContentHeight(align = Alignment.CenterVertically),
                    textAlign = TextAlign.Center,
                    fontSize = 10.sp,
                    color = Color.White
                )
            }
        } else {
            ImageGrid(isLandscape, paths = paths, onClick = {
                ImagesCache.currentPhoto = it
            }, modifier = modifier)
        }
    }

    @Composable
    fun ImageGrid(
        isLandscape: Boolean,
        paths: List<PhotoData>,
        modifier: Modifier = Modifier,
        onClick: (PhotoData) -> Unit = {}
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(if(isLandscape) 6 else 3),
            contentPadding = PaddingValues(8.dp),
            modifier = modifier.fillMaxSize(),
        ) {
            items(
                items = paths,
                key = { it } // stable key = file path
            ) { photo ->
                val model = ImageRequest.Builder(LocalContext.current)
                    .data(File(photo.path))
                    .crossfade(true)
                    .build()

                AsyncImage(
                    model = model,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .aspectRatio(1f) // square cells
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { onClick(photo) }
                        .padding(4.dp)
                )
            }
        }
    }
}