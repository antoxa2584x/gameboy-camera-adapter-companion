package ua.retrogaming.gcac.view

import ImageHelper
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ua.retrogaming.gcac.helper.ViewHelper
import ua.retrogaming.gcac.model.PhotoData
import ua.retrogaming.gcac.prefs.ImagesCache
import ua.retrogaming.gcac.ui.theme.BackgroundColor
import ua.retrogaming.gcac.view.component.CloseButton
import ua.retrogaming.gcac.view.component.GreenButton


class ImagePopup : KoinComponent {

    private val imageHelper: ImageHelper by inject()
    private val viewHelper: ViewHelper by inject()

    @Composable
    fun Render(currentPhoto: PhotoData?) {

        if (currentPhoto == null)
            return

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.8f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {               },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = AbsoluteAlignment.CenterRight
                ) {
                    CloseButton().Render { closeDialog() }
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = BackgroundColor,
                    )
                ) {
                    PhotoContent(currentPhoto)
                }
            }
        }
    }

    private fun closeDialog() {
        ImagesCache.currentPhoto = null
    }

    @Composable
    fun PhotoContent(currentPhoto: PhotoData) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DynamicAspectImage(currentPhoto.path)

            GreenButton().Render("Save", {
                ImagesCache.isPrinting = true
                val result = imageHelper.saveImageJpegScoped(currentPhoto, ImageHelper.SaveOptions(scale = 0))

                viewHelper.showToast(if (result) "Photo saved to Gallery" else "Error")

                ImagesCache.isPrinting = false
            })
        }
    }


    @Composable
    fun DynamicAspectImage(path: String) {
        var ratio by remember { mutableStateOf<Float?>(null) }

        SubcomposeAsyncImage(
            model = path,
            contentDescription = null,
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
                .let { m -> ratio?.let { m.aspectRatio(it) } ?: m }
                .clip(MaterialTheme.shapes.medium)// apply when known
        ) {
            val painter = painter
            if (ratio == null) {
                val w = painter.intrinsicSize.width
                val h = painter.intrinsicSize.height
                if (w > 0f && h > 0f) {
                    ratio = w / h
                }
            }
            SubcomposeAsyncImageContent()
        }
    }
}