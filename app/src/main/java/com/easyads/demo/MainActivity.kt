package com.easyads.demo

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.easyads.demo.databinding.ActivityMainBinding
import com.easyads.sdk.EasyAds
import com.easyads.sdk.banner.BannerType
import com.easyads.sdk.extensions.loadInterstitialAd
import com.easyads.sdk.extensions.showInterstitialAd
import com.easyads.sdk.extensions.showRewardedAd
import com.easyads.sdk.nativead.NativeTemplate

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var currentNativeTemplate: NativeTemplate = NativeTemplate.MEDIUM
    private var earnedCoins = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupAdFreeSwitch()
        setupRemoteConfigSwitches()
        setupNativeSizeButtons()
        setupFullscreenButtons()
        loadAds()
    }

    private fun setupAdFreeSwitch() {
        binding.switchAdFree.isChecked = EasyAds.isAdFree
        binding.switchAdFree.setOnCheckedChangeListener { _, isChecked ->
            EasyAds.isAdFree = isChecked
            if (isChecked) {
                binding.tvAdFreeStatus.text = "Ad-Free Mode ON (All ads disabled)"
                binding.tvAdFreeStatus.setTextColor(getColor(android.R.color.holo_red_dark))
                binding.bannerAdView.destroy()
                binding.nativeAdView.destroy()
                Toast.makeText(this, "Ad-Free Mode Enabled. Memory purged.", Toast.LENGTH_SHORT).show()
            } else {
                binding.tvAdFreeStatus.text = "Ads Active"
                binding.tvAdFreeStatus.setTextColor(getColor(R.color.secondary))
                loadAds()
                Toast.makeText(this, "Ad-Free Mode Disabled.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupRemoteConfigSwitches() {
        // Initialize default remote config values
        EasyAds.setRemoteConfigValues(
            mapOf(
                REMOTE_KEY_BANNER to true,
                REMOTE_KEY_INTERSTITIAL to true,
                REMOTE_KEY_NATIVE to true
            )
        )

        binding.switchRemoteBanner.setOnCheckedChangeListener { _, isChecked ->
            EasyAds.setRemoteConfigValue(REMOTE_KEY_BANNER, isChecked)
            if (isChecked) {
                loadBanner()
            } else {
                binding.bannerAdView.destroy()
            }
            Toast.makeText(this, "Remote Config: Banner is now $isChecked", Toast.LENGTH_SHORT).show()
        }

        binding.switchRemoteInterstitial.setOnCheckedChangeListener { _, isChecked ->
            EasyAds.setRemoteConfigValue(REMOTE_KEY_INTERSTITIAL, isChecked)
            Toast.makeText(this, "Remote Config: Interstitial is now $isChecked", Toast.LENGTH_SHORT).show()
        }

        binding.switchRemoteNative.setOnCheckedChangeListener { _, isChecked ->
            EasyAds.setRemoteConfigValue(REMOTE_KEY_NATIVE, isChecked)
            if (isChecked) {
                loadNative(currentNativeTemplate)
            } else {
                binding.nativeAdView.destroy()
            }
            Toast.makeText(this, "Remote Config: Native is now $isChecked", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupNativeSizeButtons() {
        binding.btnNativeBanner.setOnClickListener {
            loadNative(NativeTemplate.BANNER)
        }
        binding.btnNativeSmall.setOnClickListener {
            loadNative(NativeTemplate.SMALL)
        }
        binding.btnNativeMedium.setOnClickListener {
            loadNative(NativeTemplate.MEDIUM)
        }
        binding.btnNativeLarge.setOnClickListener {
            loadNative(NativeTemplate.LARGE)
        }
    }

    private fun setupFullscreenButtons() {
        // Preload Interstitial and Rewarded upfront
        loadInterstitialAd(
            adUnitId = getString(R.string.test_interstitial_id),
            remoteKey = REMOTE_KEY_INTERSTITIAL
        )
        EasyAds.rewarded.load(this, getString(R.string.test_rewarded_id))

        binding.btnInterstitial.setOnClickListener {
            showInterstitialAd(
                adUnitId = getString(R.string.test_interstitial_id),
                remoteKey = REMOTE_KEY_INTERSTITIAL,
                autoReload = true
            ) {
                Toast.makeText(this, "Interstitial dismissed / continued", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnRewarded.setOnClickListener {
            showRewardedAd(
                adUnitId = getString(R.string.test_rewarded_id),
                autoReload = true,
                onUserEarnedReward = { amount, type ->
                    earnedCoins += amount
                    binding.tvRewardPoints.text = "Earned Coins: $earnedCoins ($type)"
                    Toast.makeText(this, "Reward earned: +$amount $type", Toast.LENGTH_SHORT).show()
                },
                onDismissed = {
                    // Ad closed
                }
            )
        }
    }

    private fun loadAds() {
        if (EasyAds.isAdFree) return
        loadBanner()
        loadNative(currentNativeTemplate)
    }

    private fun loadBanner() {
        binding.bannerAdView.load(
            activity = this,
            adUnitId = getString(R.string.test_banner_id),
            bannerType = BannerType.ADAPTIVE,
            remoteKey = REMOTE_KEY_BANNER
        )
    }

    private fun loadNative(template: NativeTemplate) {
        currentNativeTemplate = template
        binding.nativeAdView.loadAndShow(
            activity = this,
            adUnitId = getString(R.string.test_native_id),
            template = template,
            remoteKey = REMOTE_KEY_NATIVE
        )
    }

    override fun onResume() {
        super.onResume()
        binding.bannerAdView.resume()
    }

    override fun onPause() {
        super.onPause()
        binding.bannerAdView.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.bannerAdView.destroy()
        binding.nativeAdView.destroy()
    }

    companion object {
        const val REMOTE_KEY_BANNER = "banner_enabled"
        const val REMOTE_KEY_INTERSTITIAL = "interstitial_enabled"
        const val REMOTE_KEY_NATIVE = "native_enabled"
    }
}
