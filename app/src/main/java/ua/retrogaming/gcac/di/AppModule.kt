package ua.retrogaming.gcac.di

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import ua.retrogaming.gcac.data.image.ImageSaver
import ua.retrogaming.gcac.data.serial.LedSerialClient
import ua.retrogaming.gcac.data.serial.SerialHelper
import ua.retrogaming.gcac.data.serial.services.DiscoveryService
import ua.retrogaming.gcac.util.ViewHelper

val appModule = module {
    single { LedSerialClient() }
    single { SerialHelper(androidContext()) }
    single { DiscoveryService(androidContext(), get(), get()) }
    single { ImageSaver(androidContext()) }
    single { ViewHelper(androidContext()) }
}
