package com.example.heximagechecker

import android.app.Application
import android.content.res.Configuration
import android.util.Log
import java.util.*

class HexApplication : Application() {
    override fun onConfigurationChanged(newConfig: Configuration) {
        Log.d("config_change", "true")
        newConfig.setLocale(Locale.ENGLISH)
        super.onConfigurationChanged(newConfig)
        Locale.setDefault(newConfig.locales.get(0))
        baseContext.createConfigurationContext(newConfig)
    }

    override fun onCreate() {
        super.onCreate()
        onConfigurationChanged(Configuration())
    }
}