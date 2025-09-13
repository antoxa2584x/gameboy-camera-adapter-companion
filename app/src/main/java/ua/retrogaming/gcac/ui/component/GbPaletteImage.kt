package ua.retrogaming.gcac.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.transformations
import ua.retrogaming.gcac.helper.PocketCameraPalettes
import ua.retrogaming.gcac.helper.PocketPaletteTransformation

@Composable
fun GbPaletteImage(
    data: Any, // Uri, File, URL, resource, etc.
    scheme: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val palette = remember(scheme) {
        PocketCameraPalettes.findPalletByName(scheme)
    }

    val request = remember(data, palette) {
        ImageRequest.Builder(context)
            .data(data)
            .transformations(PocketPaletteTransformation(palette))
            .build()
    }

    AsyncImage(
        model = request,
        contentDescription = "Game Boy palette image",
        modifier = modifier
    )
}