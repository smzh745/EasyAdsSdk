package com.easyads.demo

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.easyads.demo.databinding.ActivitySplashBinding
import com.easyads.sdk.EasyAds
import com.easyads.sdk.extensions.showSplashAppOpenAd

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Block auto-resume ads while splash is displaying
        EasyAds.isAppOpenBlocked = true

        // Attempt to show Splash App Open ad (max 5s wait)
        showSplashAppOpenAd(
            adUnitId = getString(R.string.test_app_open_id),
            timeoutMs = 5000L
        ) {
            navigateToMain()
        }
    }

    private fun navigateToMain() {
        EasyAds.isAppOpenBlocked = false
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
