package ua.retrogaming.gcac.ui.view

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import ua.retrogaming.gcac.R
import ua.retrogaming.gcac.model.PhotoData
import ua.retrogaming.gcac.ui.MainEvent
import ua.retrogaming.gcac.ui.PrintState
import ua.retrogaming.gcac.ui.component.CloseButton
import ua.retrogaming.gcac.ui.component.GbPaletteImage
import ua.retrogaming.gcac.ui.component.GreenButton
import ua.retrogaming.gcac.ui.theme.BackgroundColor
import ua.retrogaming.gcac.ui.theme.DarkRed

@Composable
fun ImagePopup(
    photo: PhotoData,
    colorScheme: String,
    printState: PrintState,
    showPrint: Boolean,
    events: Flow<MainEvent>,
    onSave: (PhotoData) -> Unit,
    onPrint: (PhotoData) -> Unit,
    onRemove: (PhotoData) -> Unit,
    onClose: () -> Unit,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.remove_confirm_title), color = Color.White) },
            text = { Text(stringResource(R.string.remove_photo_confirm_msg), color = Color.White) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onRemove(photo)
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
                CloseButton(onClick = onClose)
            }

            Card(
                modifier = Modifier.padding(top = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = BackgroundColor,
                )
            ) {
                PhotoContent(
                    photo = photo,
                    colorScheme = colorScheme,
                    printState = printState,
                    showPrint = showPrint,
                    events = events,
                    onSave = onSave,
                    onPrint = onPrint,
                    onDeleteRequest = { showDeleteDialog = true },
                )
            }
        }
    }
}

@Composable
private fun PhotoContent(
    photo: PhotoData,
    colorScheme: String,
    printState: PrintState,
    showPrint: Boolean,
    events: Flow<MainEvent>,
    onSave: (PhotoData) -> Unit,
    onPrint: (PhotoData) -> Unit,
    onDeleteRequest: () -> Unit,
) {
    val shakeAnim = remember { Animatable(0f) }

    // Shake the save button when the photo was saved successfully
    LaunchedEffect(Unit) {
        events.collect { event ->
            if (event is MainEvent.PhotoSaved) {
                repeat(3) {
                    shakeAnim.animateTo(10f, tween(durationMillis = 50))
                    shakeAnim.animateTo(-10f, tween(durationMillis = 50))
                }
                shakeAnim.animateTo(0f, tween(durationMillis = 50))
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GbPaletteImage(
            data = photo.path,
            scheme = colorScheme,
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .padding(4.dp),
            contentScale = ContentScale.FillWidth,
            applyPalette = photo.filter.isEmpty(),
            upscale = 20
        )

        GreenButton(
            modifier = Modifier
                .padding(8.dp)
                .graphicsLayer {
                    translationX = shakeAnim.value
                },
            text = stringResource(R.string.save_photo),
            onClick = { onSave(photo) }
        )

        if (showPrint) {
            PrintButton(photo, printState, onPrint)
        }

        GreenButton(
            modifier = Modifier.padding(8.dp),
            text = stringResource(R.string.remove),
            containerColor = DarkRed,
            onClick = onDeleteRequest
        )
    }
}

@Composable
private fun PrintButton(
    photo: PhotoData,
    printState: PrintState,
    onPrint: (PhotoData) -> Unit,
) {
    val text = when (printState) {
        is PrintState.Idle -> stringResource(R.string.print_photo)
        is PrintState.Sending ->
            "${stringResource(R.string.print_sending)} ${printState.sent}/${printState.total}"
        is PrintState.Printing -> stringResource(R.string.print_in_progress)
    }

    GreenButton(
        modifier = Modifier.padding(8.dp),
        text = text,
        onClick = { if (printState is PrintState.Idle) onPrint(photo) }
    )
}
