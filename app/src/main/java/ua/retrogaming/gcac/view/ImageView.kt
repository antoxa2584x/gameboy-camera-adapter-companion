package ua.retrogaming.gcac.view

import ImageHelper
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ua.retrogaming.gcac.helper.ViewHelper
import ua.retrogaming.gcac.model.PhotoData
import ua.retrogaming.gcac.prefs.ImagesCache
import ua.retrogaming.gcac.ui.theme.PressStart2P
import ua.retrogaming.gcac.ui.theme.Red
import ua.retrogaming.gcac.ui.theme.SecondaryBackgroundColor


class ImageView : KoinComponent {

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
                ) {},
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(0.9f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = AbsoluteAlignment.CenterRight
                ) {
                    CloseButton()
                }

                AsyncImage(
                    model = currentPhoto.path,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .aspectRatio(1f) // square cells
                        .clip(MaterialTheme.shapes.medium)
                        .padding(4.dp)
                )

                SaveButton(currentPhoto)
            }
        }
    }

    @Composable
    fun SaveButton(currentPhoto: PhotoData) {
        Button(
            {
                ImagesCache.isPrinting = true
                val result = imageHelper.saveImageJpegScoped(currentPhoto)

                viewHelper.showToast(if (result) "Saved" else "Error")

                ImagesCache.isPrinting = false
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonColors(
                SecondaryBackgroundColor,
                Color.White,
                SecondaryBackgroundColor,
                Color.White
            )
        ) {
            Text(
                "Save", style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = PressStart2P
                )
            )
        }
    }

    @Composable
    fun CloseButton() {
        Button(
            {
                ImagesCache.currentPhoto = null
            },
            modifier = Modifier
                .height(40.dp)
                .width(40.dp),
            contentPadding = PaddingValues(0.dp),
            colors = ButtonColors(
                Red,
                Color.White,
                Red,
                Color.White
            )
        ) {
            Text(
                "X", style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = PressStart2P
                )
            )
        }
    }
}