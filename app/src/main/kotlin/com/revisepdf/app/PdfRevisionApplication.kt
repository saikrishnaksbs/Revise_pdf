package com.revisepdf.app

import android.app.Application

class PdfRevisionApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.notificationHelper.ensureChannel()
    }
}
