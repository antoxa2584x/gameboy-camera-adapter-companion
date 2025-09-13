package ua.retrogaming.gcac

import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.chibatching.kotpref.Kotpref
import com.chibatching.kotpref.gsonpref.gson
import com.google.gson.Gson
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.dsl.module
import ua.retrogaming.gcac.helper.ImageSaver
import ua.retrogaming.gcac.helper.LedSerialClient
import ua.retrogaming.gcac.helper.SerialHelper
import ua.retrogaming.gcac.helper.ViewHelper
import ua.retrogaming.gcac.services.DiscoveryService

class MainApplication : Application() {

    private val appModule = module {
        single<LedSerialClient> { LedSerialClient() }
        single<SerialHelper> { SerialHelper(androidContext()) }
        single<DiscoveryService> { DiscoveryService(androidContext(), get(), get()) }
        single<ImageSaver> { ImageSaver(androidContext()) }
        single<ViewHelper> { ViewHelper(androidContext()) }

    }

    private val discoveryService: DiscoveryService by inject()

    override fun onCreate() {
        super.onCreate()

        Kotpref.init(this)
        Kotpref.gson = Gson()

//        ImagesCache.clear()

        startKoin {
            androidLogger()
            androidContext(this@MainApplication)
            modules(appModule)
        }

        discoveryService.init()

        ProcessLifecycleOwner.get().lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_STOP) {
                    discoveryService.disconnectDevice()
                }
            }
        )
    }
}