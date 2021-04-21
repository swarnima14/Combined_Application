package com.app.combined

import android.app.Application
import pyxis.uzuki.live.mediaresizer.MediaResizerGlobal

class BaseApplication: Application(){
    override fun onCreate() {
        super.onCreate()

        MediaResizerGlobal.initializeApplication(this)
    }
}