package ua.retrogaming.gcac.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ua.retrogaming.gcac.R
import ua.retrogaming.gcac.core.GbPrinterPacketBuilder
import ua.retrogaming.gcac.core.Version
import ua.retrogaming.gcac.core.image.PocketCameraPalettes
import ua.retrogaming.gcac.data.image.ImageSaver
import ua.retrogaming.gcac.data.repository.DeviceRepository
import ua.retrogaming.gcac.data.repository.PhotoRepository
import ua.retrogaming.gcac.data.repository.UpdateRepository
import ua.retrogaming.gcac.data.serial.LedSerialClient
import ua.retrogaming.gcac.data.serial.PrintSerialClient
import ua.retrogaming.gcac.model.LedStatus
import ua.retrogaming.gcac.model.MobileMode
import ua.retrogaming.gcac.model.PhotoData

data class MainUiState(
    val connected: Boolean = false,
    val firmwareVersion: String? = null,
    /** True when the connected adapter's firmware supports GB Printer printing. */
    val printSupported: Boolean = false,
    val ledStatus: LedStatus? = null,
    val language: String = "en",
    val photos: List<PhotoData> = emptyList(),
    val colorScheme: String = "grayscale",
    val currentPhoto: PhotoData? = null,
    val isBusy: Boolean = false,
    val printState: PrintState = PrintState.Idle,
    val firmwareUpdate: UpdateRepository.FirmwareUpdate? = null,
    val appUpdate: UpdateRepository.AppUpdate? = null,
)

sealed interface PrintState {
    data object Idle : PrintState
    data class Sending(val sent: Int, val total: Int) : PrintState
    data object Printing : PrintState
}

/** One-shot UI events (toasts, animations). */
sealed interface MainEvent {
    data class Message(@StringRes val textRes: Int) : MainEvent
    data object PhotoSaved : MainEvent
}

class MainViewModel(
    private val photoRepository: PhotoRepository,
    private val deviceRepository: DeviceRepository,
    private val updateRepository: UpdateRepository,
    private val imageSaver: ImageSaver,
    private val printSerialClient: PrintSerialClient,
    private val ledSerialClient: LedSerialClient,
) : ViewModel() {

    private val printState = MutableStateFlow<PrintState>(PrintState.Idle)

    private val _events = MutableSharedFlow<MainEvent>()
    val events: SharedFlow<MainEvent> = _events.asSharedFlow()

    private data class DeviceState(
        val connected: Boolean,
        val ledStatus: LedStatus?,
        val firmwareVersion: String?,
        val language: String,
    )

    private data class GalleryState(
        val photos: List<PhotoData>,
        val colorScheme: String,
        val currentPhoto: PhotoData?,
        val isBusy: Boolean,
    )

    private val deviceState = combine(
        deviceRepository.connected,
        deviceRepository.ledStatus,
        deviceRepository.firmwareVersion,
        deviceRepository.language,
        ::DeviceState
    )

    private val galleryState = combine(
        photoRepository.photos,
        photoRepository.colorScheme,
        photoRepository.currentPhoto,
        photoRepository.isBusy,
        ::GalleryState
    )

    val uiState: StateFlow<MainUiState> = combine(
        deviceState,
        galleryState,
        updateRepository.firmwareUpdate,
        updateRepository.appUpdate,
        printState,
    ) { device, gallery, firmwareUpdate, appUpdate, print ->
        MainUiState(
            connected = device.connected,
            firmwareVersion = device.firmwareVersion,
            printSupported = device.connected && device.firmwareVersion != null &&
                    Version.compare(device.firmwareVersion, MIN_PRINT_FIRMWARE) > 0,
            ledStatus = device.ledStatus,
            language = device.language,
            photos = gallery.photos.sortedByDescending { it.created },
            colorScheme = gallery.colorScheme,
            currentPhoto = gallery.currentPhoto,
            isBusy = gallery.isBusy,
            printState = print,
            firmwareUpdate = firmwareUpdate,
            appUpdate = appUpdate,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainUiState())

    // --- Gallery ---

    fun openPhoto(photo: PhotoData) = photoRepository.setCurrentPhoto(photo)

    fun closePhoto() = photoRepository.setCurrentPhoto(null)

    fun selectColorScheme(scheme: String) = photoRepository.setColorScheme(scheme)

    fun removePhoto(photo: PhotoData) {
        photoRepository.removePhoto(photo)
        emit(MainEvent.Message(R.string.photo_removed))
    }

    fun removeAll() {
        photoRepository.removeAll()
        emit(MainEvent.Message(R.string.all_removed))
    }

    fun savePhoto(photo: PhotoData) {
        viewModelScope.launch(Dispatchers.IO) {
            photoRepository.setBusy(true)
            try {
                val resultPath = saveToGallery(photo)
                if (resultPath != null) {
                    // Keep the popup in sync with the saved variant
                    photoRepository.setCurrentPhoto(
                        photo.copy(path = resultPath, filter = photoRepository.colorScheme.value)
                    )
                    _events.emit(MainEvent.PhotoSaved)
                    _events.emit(MainEvent.Message(R.string.photo_saved))
                } else {
                    _events.emit(MainEvent.Message(R.string.save_error))
                }
            } finally {
                photoRepository.setBusy(false)
            }
        }
    }

    fun saveAll() {
        viewModelScope.launch(Dispatchers.IO) {
            photoRepository.setBusy(true)
            try {
                val allSuccess = photoRepository.photos.value.all { saveToGallery(it) != null }
                _events.emit(MainEvent.Message(if (allSuccess) R.string.all_saved else R.string.save_error))
            } finally {
                photoRepository.setBusy(false)
            }
        }
    }

    private fun saveToGallery(photo: PhotoData): String? {
        val colorScheme = photoRepository.colorScheme.value
        return imageSaver.saveImageJpegScoped(
            data = photo,
            opts = ImageSaver.SaveOptions(
                scale = 20,
                colorSchemeName = colorScheme,
                filter = ImageSaver.ImageFilter.PocketPalette(
                    palette = PocketCameraPalettes.findPalletByName(colorScheme)
                )
            )
        )
    }

    // --- Printing ---

    fun printPhoto(photo: PhotoData) {
        if (printState.value != PrintState.Idle) return

        viewModelScope.launch(Dispatchers.IO) {
            val sourcePath = photo.originalPath.ifEmpty { photo.path }
            printBitmapInternal(BitmapFactory.decodeFile(sourcePath), recycle = true)
        }
    }

    /** Print an arbitrary bitmap (already prepared for the printer) from the print popup. */
    fun printBitmap(bitmap: Bitmap, exposure: Int) {
        if (printState.value != PrintState.Idle) return

        viewModelScope.launch(Dispatchers.IO) {
            printBitmapInternal(bitmap, recycle = false, exposure = exposure)
        }
    }

    private suspend fun printBitmapInternal(
        bitmap: Bitmap?,
        recycle: Boolean,
        exposure: Int = GbPrinterPacketBuilder.DEFAULT_EXPOSURE,
    ) {
        printState.value = PrintState.Sending(0, 0)
        try {
            val result = if (bitmap == null) {
                PrintSerialClient.PrintResult.Error(PrintSerialClient.STATUS_IO_ERROR)
            } else {
                printSerialClient.printImage(bitmap, exposure) { sent, total ->
                    printState.value =
                        if (sent == total) PrintState.Printing else PrintState.Sending(sent, total)
                }.also { if (recycle) bitmap.recycle() }
            }

            _events.emit(
                MainEvent.Message(
                    when (result) {
                        is PrintSerialClient.PrintResult.Success -> R.string.print_done
                        is PrintSerialClient.PrintResult.NotConnected -> R.string.connect_adapter
                        is PrintSerialClient.PrintResult.PrinterDisconnected -> R.string.printer_disconnected
                        is PrintSerialClient.PrintResult.Error -> R.string.print_error
                    }
                )
            )
        } finally {
            printState.value = PrintState.Idle
        }
    }

    // --- Settings ---

    fun setLedColor(color: Color, useRgb: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                ledSerialClient.setLedColor(color, useRgb)
                delay(1_000)
                ledSerialClient.loadLedStatus()
            } catch (_: Exception) {
                _events.emit(MainEvent.Message(R.string.connect_adapter))
            }
        }
    }

    fun setLanguage(language: String) = deviceRepository.setLanguage(language)

    /** Switch adapter compatibility mode; the adapter reboots and disconnects. */
    fun setMobileMode(mode: MobileMode) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                ledSerialClient.setMobileMode(mode)
                _events.emit(MainEvent.Message(R.string.adapter_rebooting))
            } catch (_: Exception) {
                _events.emit(MainEvent.Message(R.string.connect_adapter))
            }
        }
    }

    fun skipAppUpdate() = updateRepository.skipAppUpdate()

    private fun emit(event: MainEvent) {
        viewModelScope.launch { _events.emit(event) }
    }

    companion object {
        /** Printing over the adapter requires firmware newer than this. */
        const val MIN_PRINT_FIRMWARE = "2.0.1"
    }
}
