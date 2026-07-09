package com.vfdeginformatica.mysuperapp

import android.app.Application
import com.vfdeginformatica.mysuperapp.common.FirebaseAppCheckInitializer
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MySuperApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        FirebaseAppCheckInitializer.initialize(
            application = this,
            isDebuggable = BuildConfig.IS_DEBUGGABLE
        )
    }
}
