package com.app.combined

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatActivity
import pyxis.uzuki.live.mediaresizer.MediaResizerGlobal

open class BaseApplication: Application() {
    override fun onCreate() {
        super.onCreate()

        MediaResizerGlobal.initializeApplication(this)
    }

}