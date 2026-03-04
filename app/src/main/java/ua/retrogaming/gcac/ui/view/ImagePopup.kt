package ua.retrogaming.gcac.ui.view

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import ua.retrogaming.gcac.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ua.retrogaming.gcac.core.image.PocketCameraPalettes
import ua.retrogaming.gcac.data.image.ImageSaver
import ua.retrogaming.gcac.data.prefs.ImageCache
import ua.retrogaming.gcac.model.PhotoData
import ua.retrogaming.gcac.ui.component.CloseButton
import ua.retrogaming.gcac.ui.component.GbPaletteImage
import ua.retrogaming.gcac.ui.component.GreenButton
import ua.retrogaming.gcac.ui.MainActivity
import ua.retrogaming.gcac.ui.theme.BackgroundColor
import ua.retrogaming.gcac.ui.theme.DarkRed
import ua.retrogaming.gcac.util.ViewHelper


class ImagePopup : KoinComponent {

    private val imageSaver: ImageSaver by inject()
    private val viewHelper: ViewHelper by inject()

    @Composable
    fun Render(currentPhoto: PhotoData?) {

        if (currentPhoto == null)
            return

        var showDeleteDialog by remember { mutableStateOf(false) }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text(stringResource(R.string.remove_confirm_title), color = Color.White) },
                text = { Text(stringResource(R.string.remove_photo_confirm_msg), color = Color.White) },
                confirmButton = {
                    TextButton(onClick = {
                        ImageCache.removePhoto(currentPhoto)
                        showDeleteDialog = false
                        closeDialog()
                    }) {
                        Text(stringResource(R.string.confirm), color = DarkRed)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text(stringResource(R.string.cancel), color = Color.White)
                    }
                },
                containerColor = BackgroundColor
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.8f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = AbsoluteAlignment.CenterRight
                ) {
                    CloseButton().Render { closeDialog() }
                }

                Card(
                    modifier = Modifier.padding(top = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = BackgroundColor,
                    )
                ) {
                    PhotoContent(currentPhoto, onDeleteRequest = { showDeleteDialog = true })
                }
            }
        }
    }

    private fun closeDialog() {
        ImageCache.currentPhoto = null
    }

    @Composable
    fun PhotoContent(currentPhoto: PhotoData, onDeleteRequest: () -> Unit) {
        val shakeAnim = remember { Animatable(0f) }
        val coroutineScope = rememberCoroutineScope()
        val context = LocalContext.current

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DynamicAspectImage(currentPhoto)

            val saveSuccessText = stringResource(R.string.photo_saved)
            val saveErrorText = stringResource(R.string.save_error)

            GreenButton().Render(
                modifier = Modifier
                    .padding(8.dp)
                    .graphicsLayer {
                        translationX = shakeAnim.value
                    },
                text = stringResource(R.string.save_photo),
                onClick = {
                    (context as? MainActivity)?.checkStoragePermission { isGranted ->
                        if (!isGranted) return@checkStoragePermission

                        coroutineScope.launch(Dispatchers.IO) {
                            withContext(Dispatchers.Main) {
                                ImageCache.isPrinting = true
                            }

                            val colorScheme = ImageCache.colorScheme
                            val resultPath = imageSaver.saveImageJpegScoped(
                                data = currentPhoto,
                                opts = ImageSaver.SaveOptions(
                                    scale = 20,
                                    colorSchemeName = colorScheme,
                                    filter = ImageSaver.ImageFilter.PocketPalette(
                                        palette = PocketCameraPalettes.findPalletByName(colorScheme)
                                    )
                                )
                            )

                            withContext(Dispatchers.Main) {
                                viewHelper.showToast(if (resultPath != null) saveSuccessText else saveErrorText)
                                ImageCache.isPrinting = false

                                if (resultPath != null) {
                                    ImageCache.currentPhoto = currentPhoto.copy(
                                        path = resultPath,
                                        filter = colorScheme
                                    )
                                    coroutineScope.launch {
                                        repeat(3) {
                                            shakeAnim.animateTo(
                                                targetValue = 10f,
                                                animationSpec = tween(durationMillis = 50)
                                            )
                                            shakeAnim.animateTo(
                                                targetValue = -10f,
                                                animationSpec = tween(durationMillis = 50)
                                            )
                                        }
                                        shakeAnim.animateTo(
                                            targetValue = 0f,
                                            animationSpec = tween(durationMillis = 50)
                                        )
                                    }
                                }
                            }
                        }
                    }
                })

            GreenButton().Render(
                modifier = Modifier.padding(8.dp),
                text = stringResource(R.string.remove),
                containerColor = DarkRed,
                onClick = {
                    onDeleteRequest()
                })
        }
    }


    @Composable
    fun DynamicAspectImage(photo: PhotoData) {
        GbPaletteImage(
            data = photo.path,
            scheme = ImageCache.colorScheme,
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .padding(4.dp),
            contentScale = ContentScale.FillWidth,
            applyPalette = photo.filter.isEmpty(),
            upscale = 20
        )
    }
}