package com.vfdeginformatica.mysuperapp.common

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

object FirebaseAppCheckInitializer {
    fun initialize(
        application: Application,
        isDebuggable: Boolean
    ) {
        FirebaseApp.initializeApp(application)
        val firebaseAppCheck = FirebaseAppCheck.getInstance()

        if (isDebuggable) {
            val debugFactory =
                com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory.getInstance()
            firebaseAppCheck.installAppCheckProviderFactory(debugFactory)
        } else {
            firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
        }
    }
}
