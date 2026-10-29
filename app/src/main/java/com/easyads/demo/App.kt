package com.easyads.demo

import android.app.Application
import com.easyads.sdk.EasyAds

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        // 1. Initialize EasyAds
        EasyAds.initialize(this)

        // 2. Enable auto App-Resume App Open Ad (optional)
        EasyAds.appOpen.enableAppResume(
            application = this,
            adUnitId = getString(R.string.test_app_open_id)
        )
    }
}
