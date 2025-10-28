package com.phonemanager

import android.app.Application

class PhoneManagerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Future: Initialize Koin, Timber, WorkManager here
    }
}
