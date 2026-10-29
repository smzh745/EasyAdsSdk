package com.easyads.demo

import android.app.Application
import com.easyads.sdk.EasyAds

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        // 1. Initialize EasyAds with debug logging enabled
        EasyAds.initialize(this, isDebug = true)

        // 2. Enable EEA geography simulation to test GDPR consent dialog in debug
        EasyAds.consent.isDebugGeographyEEA = true
        // EasyAds.consent.reset(this) // Uncomment to clear consent on every launch during testing
        // EasyAds.consent.testDeviceHashedId = "YOUR_TEST_DEVICE_HASHED_ID" // If testing on a physical phone

        // 3. Enable auto App-Resume App Open Ad, excluding SplashActivity
        EasyAds.appOpen.enableAppResume(
            application = this,
            adUnitId = getString(R.string.test_app_open_id),
            disallowedActivityClasses = setOf(SplashActivity::class.java)
        )
    }
}
