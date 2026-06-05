package ua.retrogaming.gcac.ui.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import ua.retrogaming.gcac.R
import ua.retrogaming.gcac.core.GbPrinterPacketBuilder
import ua.retrogaming.gcac.ui.MainViewModel
import ua.retrogaming.gcac.ui.PrintState
import ua.retrogaming.gcac.ui.component.CloseButton
import ua.retrogaming.gcac.ui.component.GreenButton
import ua.retrogaming.gcac.ui.theme.SecondaryBackgroundColor

/**
 * Full-screen "print any photo" feature, mirroring the adapter web
 * interface. The photo is cropped manually (pinch to zoom, drag to move)
 * inside a fixed 160x144 GB frame; the preview shows the exact 4-shade
 * image that will print, with the exposure slider's burn level simulated
 * on screen.
 */
@Composable
fun PrintScreen(
    connected: Boolean,
    printSupported: Boolean,
    printState: PrintState,
    onPrint: (Bitmap, Int) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current

    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    var source by remember { mutableStateOf<Bitmap?>(null) }
    var loadFailed by remember { mutableStateOf(false) }

    // Crop transform: scale >= 1 zooms in, offset moves the photo under the frame
    var cropScale by remember { mutableFloatStateOf(1f) }
    var cropOffset by remember { mutableStateOf(Offset.Zero) }
    var frameSize by remember { mutableStateOf(IntSize.Zero) }

    var exposure by remember { mutableFloatStateOf(GbPrinterPacketBuilder.DEFAULT_EXPOSURE.toFloat()) }

    // The exact 160x144 4-shade bitmap that goes to the printer
    var printerBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) pickedUri = uri
    }

    LaunchedEffect(pickedUri) {
        val uri = pickedUri ?: return@LaunchedEffect
        val decoded = withContext(Dispatchers.IO) { decodeSource(context, uri) }
        loadFailed = decoded == null
        if (decoded != null) {
            source = decoded
            cropScale = 1f
            cropOffset = Offset.Zero
        }
    }

    // Re-extract the printable frame whenever the photo or the crop changes
    LaunchedEffect(source, cropScale, cropOffset, frameSize) {
        val src = source ?: return@LaunchedEffect
        if (frameSize.width == 0) return@LaunchedEffect
        printerBitmap = withContext(Dispatchers.Default) {
            GbPrinterPacketBuilder.quantizeForPrinter(
                extractFrame(src, cropScale, cropOffset, frameSize.width)
            )
        }
    }

    // On-screen tone simulation of the printer burn level; print data unchanged
    val preview = remember(printerBitmap, exposure.toInt()) {
        printerBitmap?.let { simulateExposure(it, exposure.toInt()) }
    }

    val launchPicker = {
        pickImage.launch(
            PickVisualMediaRequest(
                ActivityResultContracts.PickVisualMedia.ImageOnly
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.print_popup_title),
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )

            CloseButton(onClick = onClose)
        }

        val src = source
        if (src == null) {
            // Empty state: description and picker button centered vertically
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.print_pick_hint),
                    color = Color.Yellow.copy(alpha = 0.8f),
                    fontSize = 8.sp,
                    lineHeight = 12.sp,
                    textAlign = TextAlign.Center
                )

                if (loadFailed) {
                    Text(
                        text = stringResource(R.string.image_load_error),
                        color = Color.Yellow,
                        fontSize = 8.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                GreenButton(
                    modifier = Modifier.padding(8.dp),
                    text = stringResource(R.string.choose_photo),
                    enabled = printState is PrintState.Idle,
                    onClick = launchPicker
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                CropEditor(
                    source = src,
                    scale = cropScale,
                    offset = cropOffset,
                    onSizeChanged = { frameSize = it },
                    onTransform = { zoom, pan ->
                        val newScale = (cropScale * zoom).coerceIn(1f, 8f)
                        val applied = newScale / cropScale
                        cropOffset = clampOffset(
                            cropOffset * applied + pan, newScale, frameSize, src
                        )
                        cropScale = newScale
                    }
                )

                Text(
                    text = stringResource(R.string.print_crop_hint),
                    color = Color.Yellow.copy(alpha = 0.8f),
                    fontSize = 8.sp,
                    lineHeight = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp)
                )

                if (loadFailed) {
                    Text(
                        text = stringResource(R.string.image_load_error),
                        color = Color.Yellow,
                        fontSize = 8.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (preview != null) {
                    // Result preview and exposure side by side to save vertical space
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            bitmap = preview.asImageBitmap(),
                            contentDescription = stringResource(R.string.print_popup_title),
                            modifier = Modifier
                                .fillMaxWidth(0.4f)
                                .clip(MaterialTheme.shapes.small),
                            contentScale = ContentScale.FillWidth,
                            filterQuality = FilterQuality.None
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        ExposureSlider(
                            exposure = exposure,
                            enabled = printState is PrintState.Idle,
                            onExposureChange = { exposure = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                GreenButton(
                    modifier = Modifier.padding(8.dp),
                    text = stringResource(R.string.choose_photo),
                    enabled = printState is PrintState.Idle,
                    onClick = launchPicker
                )

                val toPrint = printerBitmap
                if (toPrint != null) {
                    when {
                        connected && printSupported -> {
                            val text = when (printState) {
                                is PrintState.Idle -> stringResource(R.string.print_photo)
                                is PrintState.Sending ->
                                    "${stringResource(R.string.print_sending)} ${printState.sent}/${printState.total}"

                                is PrintState.Printing -> stringResource(R.string.print_in_progress)
                            }

                            GreenButton(
                                modifier = Modifier.padding(8.dp),
                                text = text,
                                onClick = {
                                    if (printState is PrintState.Idle) {
                                        onPrint(toPrint, exposure.toInt())
                                    }
                                }
                            )
                        }

                        // Adapter attached while the screen was open, firmware too old
                        connected -> Text(
                            text = stringResource(
                                R.string.print_update_required_msg,
                                MainViewModel.MIN_PRINT_FIRMWARE
                            ),
                            color = Color.Yellow,
                            fontSize = 8.sp,
                            lineHeight = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(8.dp)
                        )

                        else -> Text(
                            text = stringResource(R.string.connect_adapter),
                            color = Color.Yellow,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}

/** Fixed 160:144 GB frame; the photo pans/zooms underneath it. */
@Composable
private fun CropEditor(
    source: Bitmap,
    scale: Float,
    offset: Offset,
    onSizeChanged: (IntSize) -> Unit,
    onTransform: (zoom: Float, pan: Offset) -> Unit,
) {
    val image = remember(source) { source.asImageBitmap() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(FRAME_WIDTH.toFloat() / FRAME_HEIGHT)
            .clip(MaterialTheme.shapes.medium)
            .background(Color.Black)
            .onSizeChanged(onSizeChanged)
            .pointerInput(source) {
                detectTransformGestures { _, pan, zoom, _ ->
                    onTransform(zoom, pan)
                }
            }
    ) {
        // Same fill-then-transform math as extractFrame, in editor pixels,
        // so the drag area always matches the printed result exactly
        Canvas(modifier = Modifier.fillMaxSize()) {
            val base = maxOf(
                size.width / source.width,
                size.height / source.height
            )
            val total = base * scale
            val drawW = (source.width * total).roundToInt()
            val drawH = (source.height * total).roundToInt()

            drawImage(
                image = image,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(source.width, source.height),
                dstOffset = IntOffset(
                    ((size.width - drawW) / 2f + offset.x).roundToInt(),
                    ((size.height - drawH) / 2f + offset.y).roundToInt()
                ),
                dstSize = IntSize(drawW, drawH)
            )
        }
    }
}

/** Printer burn-in darkness, 0..0x7F like the web UI's exposure slider. */
@Composable
private fun ExposureSlider(
    exposure: Float,
    enabled: Boolean,
    onExposureChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(
                R.string.print_exposure,
                (exposure / MAX_EXPOSURE * 100).toInt()
            ),
            color = Color.White,
            fontSize = 8.sp,
            textAlign = TextAlign.Center
        )

        Slider(
            value = exposure,
            onValueChange = onExposureChange,
            valueRange = 0f..MAX_EXPOSURE,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = SecondaryBackgroundColor,
                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
            )
        )
    }
}

/** Keeps the photo covering the whole frame (no empty edges). */
private fun clampOffset(
    offset: Offset,
    scale: Float,
    frame: IntSize,
    source: Bitmap,
): Offset {
    if (frame.width == 0 || frame.height == 0) return Offset.Zero
    val base = maxOf(
        frame.width.toFloat() / source.width,
        frame.height.toFloat() / source.height
    )
    val maxX = ((source.width * base * scale - frame.width) / 2f).coerceAtLeast(0f)
    val maxY = ((source.height * base * scale - frame.height) / 2f).coerceAtLeast(0f)
    return Offset(offset.x.coerceIn(-maxX, maxX), offset.y.coerceIn(-maxY, maxY))
}

/**
 * Renders the visible part of the crop editor into a 160x144 frame,
 * mirroring the editor's fill-then-transform math at printer resolution.
 */
private fun extractFrame(
    source: Bitmap,
    scale: Float,
    offset: Offset,
    frameWidthPx: Int,
): Bitmap {
    val target = Bitmap.createBitmap(FRAME_WIDTH, FRAME_HEIGHT, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(target)
    canvas.drawColor(android.graphics.Color.WHITE)

    // Editor px -> printer px; the editor box is the same 10:9 aspect
    val ratio = FRAME_WIDTH.toFloat() / frameWidthPx
    val base = maxOf(
        FRAME_WIDTH.toFloat() / source.width,
        FRAME_HEIGHT.toFloat() / source.height
    )
    val total = base * scale

    val matrix = Matrix().apply {
        postScale(total, total)
        postTranslate(
            FRAME_WIDTH / 2f - source.width * total / 2f + offset.x * ratio,
            FRAME_HEIGHT / 2f - source.height * total / 2f + offset.y * ratio
        )
    }
    canvas.drawBitmap(source, matrix, Paint(Paint.FILTER_BITMAP_FLAG))
    return target
}

/**
 * Approximates the printer's burn level on screen by lightening/darkening
 * the printed shades. Unprinted paper (white) is unaffected, and the
 * underlying print data is not modified.
 */
private fun simulateExposure(printerBitmap: Bitmap, exposure: Int): Bitmap {
    val shift = ((GbPrinterPacketBuilder.DEFAULT_EXPOSURE - exposure) * 1.2f).toInt()
    if (shift == 0) return printerBitmap

    val w = printerBitmap.width
    val h = printerBitmap.height
    val pixels = IntArray(w * h)
    printerBitmap.getPixels(pixels, 0, w, 0, 0, w, h)
    for (i in pixels.indices) {
        val gray = pixels[i] and 0xFF
        if (gray == 255) continue // unprinted paper stays white
        val v = (gray + shift).coerceIn(0, 255)
        pixels[i] = (0xFF shl 24) or (v shl 16) or (v shl 8) or v
    }
    return Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888)
}

/**
 * Decodes [uri] with downsampling and EXIF rotation applied. Returns the
 * photo as-is (cropping/quantization happen later); null when unreadable.
 */
private fun decodeSource(context: Context, uri: Uri): Bitmap? = try {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, bounds)
    }

    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
        null
    } else {
        // Downsample close to the printer width; exact scaling happens later
        var sampleSize = 1
        while (bounds.outWidth / (sampleSize * 2) >= TARGET_DECODE_WIDTH) {
            sampleSize *= 2
        }

        val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val decoded = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        }

        decoded?.let { applyExifRotation(context, uri, it) }
    }
} catch (_: Exception) {
    null
}

private fun applyExifRotation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
    val orientation = try {
        context.contentResolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } ?: ExifInterface.ORIENTATION_NORMAL
    } catch (_: Exception) {
        ExifInterface.ORIENTATION_NORMAL
    }

    val degrees = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> return bitmap
    }

    val matrix = Matrix().apply { postRotate(degrees) }
    val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    if (rotated !== bitmap) bitmap.recycle()
    return rotated
}

// GB printer frame
private const val FRAME_WIDTH = 160
private const val FRAME_HEIGHT = 144

// 4x the 160 px printer width keeps enough detail for the final downscale
private const val TARGET_DECODE_WIDTH = 640

// Highest exposure value the PRINT packet accepts
private const val MAX_EXPOSURE = 0x7F.toFloat()
