package ua.retrogaming.gcac.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import ua.retrogaming.gcac.data.analytics.AnalyticsClient
import ua.retrogaming.gcac.data.image.ImageSaver
import ua.retrogaming.gcac.data.image.PhotoFileStore
import ua.retrogaming.gcac.data.repository.DeviceRepository
import ua.retrogaming.gcac.data.repository.PhotoRepository
import ua.retrogaming.gcac.data.repository.UpdateRepository
import ua.retrogaming.gcac.data.serial.LedSerialClient
import ua.retrogaming.gcac.data.serial.PrintSerialClient
import ua.retrogaming.gcac.data.serial.SerialHelper
import ua.retrogaming.gcac.data.serial.services.DiscoveryService
import ua.retrogaming.gcac.ui.MainViewModel

val APPLICATION_SCOPE = named("applicationScope")

val appModule = module {
    // App-lifetime scope for work that must outlive any single screen
    single(APPLICATION_SCOPE) { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

    // Telemetry
    single { AnalyticsClient(androidContext()) }

    // Storage
    single { PhotoFileStore(androidContext()) }

    // Repositories
    single { DeviceRepository() }
    single { PhotoRepository(get()) }
    single { UpdateRepository() }

    // Serial clients / services
    single { LedSerialClient() }
    single { PrintSerialClient() }
    single { SerialHelper(get(), get(), get(), get(), get()) }
    single {
        DiscoveryService(
            context = androidContext(),
            serialHelper = get(),
            ledSerialClient = get(),
            printSerialClient = get(),
            deviceRepository = get(),
            updateRepository = get(),
            analytics = get(),
            applicationScope = get(APPLICATION_SCOPE),
        )
    }

    // Misc
    single { ImageSaver(androidContext(), get()) }

    // ViewModels
    viewModelOf(::MainViewModel)
}
