package com.app.unfit20

import android.app.Application
import android.content.Context

class UnfitApplication : Application() {
    companion object {
        private lateinit var instance: UnfitApplication

        fun getAppContext() : Context {
            return instance.applicationContext
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}