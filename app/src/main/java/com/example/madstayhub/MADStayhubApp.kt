package com.example.madstayhub

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MADStayhubApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Initialize App Check for security (Silent Captcha)
        // val firebaseAppCheck = FirebaseAppCheck.getInstance()
        
        // Automatically uses Debug provider for development and Play Integrity for production
        // if (BuildConfig.DEBUG) {
        //     firebaseAppCheck.installAppCheckProviderFactory(
        //         DebugAppCheckProviderFactory.getInstance()
        //     )
        // } else {
        //     firebaseAppCheck.installAppCheckProviderFactory(
        //         PlayIntegrityAppCheckProviderFactory.getInstance()
        //     )
        // }
    }
}
