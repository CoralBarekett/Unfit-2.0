package com.app.unfit20

import android.app.Application
import com.app.unfit20.data.local.AppDatabase
import com.google.firebase.FirebaseApp

/**
 * Application class for Unfit20 app
 * Used to initialize libraries and global components
 */
class UnfitApplication : Application() {

    // Database instance - updated to use the correct method name
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Initialize database
        // Already initialized through the lazy property
    }
}