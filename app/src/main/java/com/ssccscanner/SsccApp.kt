package com.ssccscanner

import android.app.Application
import com.ssccscanner.data.ScannerDatabase
import com.ssccscanner.data.ScannerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SsccApp : Application() {

    lateinit var repository: ScannerRepository
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        repository = ScannerRepository(this, ScannerDatabase.build(this))
        // Apply the auto-compress / auto-delete retention settings on launch.
        appScope.launch {
            runCatching { repository.runRetentionCleanup() }
        }
    }
}
