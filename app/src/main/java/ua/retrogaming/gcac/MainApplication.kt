package ua.retrogaming.gcac

import android.app.Application
import com.chibatching.kotpref.Kotpref
import com.chibatching.kotpref.gsonpref.gson
import com.google.gson.Gson
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import ua.retrogaming.gcac.data.serial.services.DiscoveryService
import ua.retrogaming.gcac.di.appModule

class MainApplication : Application() {

    private val discoveryService: DiscoveryService by inject()

    override fun onCreate() {
        super.onCreate()

        Kotpref.init(this)
        Kotpref.gson = Gson()

        startKoin {
            androidLogger()
            androidContext(this@MainApplication)
            modules(appModule)
        }

        discoveryService.init()
    }
}