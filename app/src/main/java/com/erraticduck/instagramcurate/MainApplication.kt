package com.erraticduck.instagramcurate

import android.app.Application
import android.content.Context
import com.erraticduck.instagramcurate.data.SearchSessionDatabase

class MainApplication : Application() {

    lateinit var searchSessionDatabase: SearchSessionDatabase

    override fun onCreate() {
        super.onCreate()
        instance = this
        searchSessionDatabase = SearchSessionDatabase.getInstance(this)
    }

    companion object {
        lateinit var instance: MainApplication
            private set
    }

}