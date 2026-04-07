package com.vfdeginformatica.mysuperapp

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MySuperApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initFirebaseAppCheck()
    }

    private fun initFirebaseAppCheck() {
        FirebaseApp.initializeApp(this)
        val firebaseAppCheck = FirebaseAppCheck.getInstance()

        if (BuildConfig.IS_DEBUGGABLE) {
            // Flavor dev: usa o provedor de debug para emuladores/testes
            // Registre o token gerado no Logcat no Firebase Console:
            // Firebase Console → App Check → Apps → Manage debug tokens
            val debugFactory =
                com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory.getInstance()
            firebaseAppCheck.installAppCheckProviderFactory(debugFactory)
        } else {
            // Flavor prd: Play Integrity — valida integridade real do dispositivo
            firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
        }
    }
}
