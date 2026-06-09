package com.mhq.fynecast

import android.app.Application
import com.mhq.fynecast.di.AppContainer
import com.mhq.fynecast.di.DefaultAppContainer

class FyneCastApplication: Application() {
    lateinit var container: AppContainer

    companion object {
        lateinit var instance: FyneCastApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        container = DefaultAppContainer(this)
    }
}