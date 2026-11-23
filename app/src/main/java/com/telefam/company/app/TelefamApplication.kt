package com.telefam.company.app

import android.app.Application

class TelefamApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        System.loadLibrary("telefam_core")
    }
}
