package com.easyads.sdk

import android.content.Context
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.easyads.sdk.appopen.AppOpenAdManager
import com.easyads.sdk.banner.BannerAdManager
import com.easyads.sdk.config.DefaultRemoteConfigManager
import com.easyads.sdk.config.RemoteConfigProvider
import com.easyads.sdk.consent.ConsentManager
import com.easyads.sdk.interstitial.InterstitialAdManager
import com.easyads.sdk.nativead.NativeAdManager
import com.easyads.sdk.rewarded.RewardedAdManager

/**
 * EasyAds: A lightweight, simple, robust facade for AdMob integration.
 *
 * Core principles:
 * 1. Screens never interact with raw SDK objects.
 * 2. Concurrency-safe: fullscreen ads never overlap.
 * 3. Instant ad-free switching: Setting [isAdFree] = true evicts all memory caches.
 * 4. Remote Config-driven: Every placement can be toggled on/off dynamically via remote keys.
 */
object EasyAds {

    val banner: BannerAdManager by lazy { BannerAdManager() }
    val interstitial: InterstitialAdManager by lazy { InterstitialAdManager() }
    val nativeAd: NativeAdManager by lazy { NativeAdManager() }
    val appOpen: AppOpenAdManager by lazy { AppOpenAdManager() }
    val rewarded: RewardedAdManager by lazy { RewardedAdManager() }
    val consent: ConsentManager by lazy { ConsentManager() }

    val remoteConfig = DefaultRemoteConfigManager()

    /**
     * Set to true if user purchases an ad-free tier or subscription.
     * Automatically purges all currently cached ads in memory.
     */
    var isAdFree: Boolean = false
        set(value) {
            field = value
            if (value) {
                destroyAllAds()
                com.easyads.sdk.core.EasyAdsLogger.i(com.easyads.sdk.core.AdType.GENERAL, "⭐️ User marked as Ad-Free. All in-memory ad caches destroyed.")
            }
        }

    /**
     * Temporarily blocks App Open ads (e.g. during Splash screen or initial onboarding).
     */
    var isAppOpenBlocked: Boolean = false


    /**
     * Controls whether EasyAds logging and debug features are enabled.
     */
    var isDebug: Boolean
        get() = com.easyads.sdk.core.EasyAdsLogger.isEnabled
        set(value) {
            com.easyads.sdk.core.EasyAdsLogger.isEnabled = value
        }

    /**
     * Initializes Google Mobile Ads SDK.
     */
    fun initialize(
        context: Context,
        isDebug: Boolean = false,
        testDeviceIds: List<String> = emptyList(),
        onInitialized: () -> Unit = {}
    ) {
        this.isDebug = isDebug
        com.easyads.sdk.core.EasyAdsLogger.i(com.easyads.sdk.core.AdType.GENERAL, "🚀 Initializing Google MobileAds SDK...")

        if (testDeviceIds.isNotEmpty()) {
            val configuration = RequestConfiguration.Builder()
                .setTestDeviceIds(testDeviceIds)
                .build()
            MobileAds.setRequestConfiguration(configuration)
        }

        MobileAds.initialize(context) { initializationStatus ->
            com.easyads.sdk.core.EasyAdsLogger.i(
                com.easyads.sdk.core.AdType.GENERAL,
                "✅ Google MobileAds SDK initialized successfully: ${initializationStatus.adapterStatusMap.keys}"
            )
            onInitialized()
        }
    }

    // --- Remote Config Helpers ---

    /**
     * Plugs in a custom [RemoteConfigProvider] (such as Firebase Remote Config).
     */
    fun setRemoteConfigProvider(provider: RemoteConfigProvider) {
        remoteConfig.setCustomProvider(provider)
    }

    /**
     * Plugs in a lambda evaluator for checking if a remote key is enabled.
     */
    fun setRemoteConfigProvider(evaluator: (String) -> Boolean) {
        remoteConfig.setCustomProvider(evaluator)
    }

    /**
     * Sets multiple key-value pairs at once (useful for tests or synced Remote Config maps).
     */
    fun setRemoteConfigValues(map: Map<String, Any>) {
        remoteConfig.setValues(map)
    }

    fun setRemoteConfigValue(key: String, value: Any) {
        remoteConfig.setValue(key, value)
    }

    fun isAdEnabled(key: String): Boolean = remoteConfig.isAdEnabled(key)

    fun getRemoteString(key: String, defaultValue: String = ""): String =
        remoteConfig.getString(key, defaultValue)

    fun getRemoteInt(key: String, defaultValue: Int = 1): Int =
        remoteConfig.getInt(key, defaultValue)

    /**
     * Releases and purges all cached ads from memory.
     */
    fun destroyAllAds() {
        banner.destroyAll()
        interstitial.destroyAll()
        nativeAd.destroyAll()
        appOpen.destroyAll()
        rewarded.destroyAll()
    }
}
