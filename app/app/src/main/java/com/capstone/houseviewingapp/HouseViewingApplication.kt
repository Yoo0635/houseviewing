package com.capstone.houseviewingapp

import android.app.Application
import com.capstone.houseviewingapp.data.remote.NetworkModule

class HouseViewingApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NetworkModule.init(this)
    }
}
