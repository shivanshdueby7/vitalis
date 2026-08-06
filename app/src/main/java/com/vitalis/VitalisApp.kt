package com.vitalis

import android.app.Application
import com.vitalis.database.VitalisDatabase

class VitalisApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Pre-initialize database
        VitalisDatabase.getInstance(this)
    }
}
