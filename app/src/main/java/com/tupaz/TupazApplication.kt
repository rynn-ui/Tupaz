package com.tupaz

import android.app.Application

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
