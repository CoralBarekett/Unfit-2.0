package com.app.unfit20

import android.app.Application
import com.app.unfit20.data.local.AppDatabase
import com.google.firebase.FirebaseApp

/**
 * Custom Application class for the Unfit20 app.
 * Used for initializing libraries and shared resources.
 */
class UnfitApplication : Application() {

    // Lazily initialized database instance
    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(this)
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Database is initialized automatically via the lazy delegate
    }
}