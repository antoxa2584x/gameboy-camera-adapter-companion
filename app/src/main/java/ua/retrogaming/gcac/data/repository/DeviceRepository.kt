package ua.retrogaming.gcac.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ua.retrogaming.gcac.data.prefs.DeviceData
import ua.retrogaming.gcac.model.LedStatus

/**
 * Single source of truth for adapter/device state.
 *
 * Connection, LED status and firmware version are session-scoped and live
 * in memory only; the UI language is persisted via [DeviceData].
 */
class DeviceRepository {

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private val _ledStatus = MutableStateFlow<LedStatus?>(null)
    val ledStatus: StateFlow<LedStatus?> = _ledStatus.asStateFlow()

    private val _firmwareVersion = MutableStateFlow<String?>(null)
    val firmwareVersion: StateFlow<String?> = _firmwareVersion.asStateFlow()

    private val _language = MutableStateFlow(DeviceData.language)
    val language: StateFlow<String> = _language.asStateFlow()

    fun setConnected(connected: Boolean, firmwareVersion: String? = _firmwareVersion.value) {
        _connected.value = connected
        _firmwareVersion.value = if (connected) firmwareVersion else _firmwareVersion.value
        if (!connected) _ledStatus.value = null
    }

    fun setLedStatus(status: LedStatus?) {
        _ledStatus.value = status
    }

    fun setLanguage(language: String) {
        if (DeviceData.language == language) return
        DeviceData.language = language
        _language.value = language
    }
}
