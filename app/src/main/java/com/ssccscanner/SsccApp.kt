package com.ssccscanner

import android.app.Application
import com.ssccscanner.data.ScannerDatabase
import com.ssccscanner.data.ScannerRepository

class SsccApp : Application() {

    lateinit var repository: ScannerRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = ScannerRepository(this, ScannerDatabase.build(this))
    }
}
