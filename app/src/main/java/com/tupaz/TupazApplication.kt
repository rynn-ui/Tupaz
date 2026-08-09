package com.tupaz

import android.app.Application

/**
 * Main application class for Tupaz video enhancement app.
 */
class TupazApplication : Application() {
    companion object {
        lateinit var instance: TupazApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
