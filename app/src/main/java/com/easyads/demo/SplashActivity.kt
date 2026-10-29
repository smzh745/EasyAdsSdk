package com.easyads.demo

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.easyads.demo.databinding.ActivitySplashBinding
import com.easyads.sdk.EasyAds
import com.easyads.sdk.core.TestAdUnits
import com.easyads.sdk.extensions.showSplashAppOpenAd

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Block auto-resume ads while splash is displaying
        EasyAds.isAppOpenBlocked = true

        // 1. Request Google UMP GDPR Consent first
        EasyAds.consent.requestConsent(this) { canRequestAds ->
            if (isFinishing || isDestroyed) return@requestConsent

            if (canRequestAds) {
                // 2. Load and show Splash App Open ad (safety timeout 4s)
                showSplashAppOpenAd(
                    adUnitId = TestAdUnits.APP_OPEN,
                    timeoutMs = 4000L
                ) {
                    navigateToMain()
                }
            } else {
                navigateToMain()
            }
        }
    }

    private fun navigateToMain() {
        EasyAds.isAppOpenBlocked = false
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
