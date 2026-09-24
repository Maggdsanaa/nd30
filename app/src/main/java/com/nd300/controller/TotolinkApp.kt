package com.nd300.controller

import android.app.Application
import com.nd300.controller.notifications.NotificationHelper

class TotolinkApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannel(this)
    }
}
