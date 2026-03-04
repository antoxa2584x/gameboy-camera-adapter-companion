package ua.retrogaming.gcac.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.transformations
import ua.retrogaming.gcac.core.image.PocketCameraPalettes
import ua.retrogaming.gcac.ui.coil.PocketPaletteTransformation

@Composable
fun GbPaletteImage(
    data: Any, // Uri, File, URL, resource, etc.
    scheme: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    applyPalette: Boolean = true,
    upscale: Int = 1
) {
    val context = LocalContext.current

    val palette = remember(scheme) {
        PocketCameraPalettes.findPalletByName(scheme)
    }

    val request = remember(data, palette, applyPalette, upscale) {
        val builder = ImageRequest.Builder(context)
            .data(data)
        
        if (applyPalette) {
            builder.transformations(PocketPaletteTransformation(palette, upscale = upscale))
        }
        
        builder.build()
    }

    AsyncImage(
        model = request,
        contentDescription = "Game Boy palette image",
        modifier = modifier,
        contentScale = contentScale
    )
}