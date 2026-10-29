package com.easyads.demo

import android.app.Application
import com.easyads.sdk.EasyAds
import com.easyads.sdk.core.TestAdUnits

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        // Modern fluent initialization
        EasyAds.init(this) {
            isDebug = true
            isDebugGeographyEEA = true // Simulates EEA user for GDPR testing in debug
            // testDeviceHashedId = "YOUR_TEST_DEVICE_HASHED_ID" // If testing on physical phone

            enableAppResume(
                application = this@App,
                adUnitId = TestAdUnits.APP_OPEN,
                disallowedActivityClasses = setOf(SplashActivity::class.java)
            )
        }
    }
}
