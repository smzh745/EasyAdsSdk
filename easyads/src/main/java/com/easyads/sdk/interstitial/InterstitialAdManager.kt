package com.easyads.sdk.interstitial

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.easyads.sdk.EasyAds
import com.easyads.sdk.core.AdResult
import com.easyads.sdk.core.AdType
import com.easyads.sdk.core.EasyAdsLogger
import com.easyads.sdk.core.FullscreenGate
import com.easyads.sdk.core.NetworkUtils
import com.easyads.sdk.core.isSafeForAd

class InterstitialAdManager internal constructor() {

    private val cachedAds = mutableMapOf<String, InterstitialAd>()
    private val loadingAdUnits = mutableSetOf<String>()

    fun isLoaded(adUnitId: String): Boolean = cachedAds.containsKey(adUnitId)

    fun load(
        context: Context,
        adUnitId: String,
        remoteKey: String? = null,
        onResult: (AdResult) -> Unit = {}
    ) {
        EasyAdsLogger.logRequest(AdType.INTERSTITIAL, adUnitId, remoteKey)

        if (EasyAds.isAdFree) {
            EasyAdsLogger.logSkippedOrBlocked(AdType.INTERSTITIAL, "User is Ad-Free", adUnitId, remoteKey)
            onResult(AdResult.Failed("User is Ad-Free"))
            return
        }
        if (remoteKey != null && !EasyAds.isAdEnabled(remoteKey)) {
            EasyAdsLogger.logSkippedOrBlocked(AdType.INTERSTITIAL, "Disabled by Remote Config", adUnitId, remoteKey)
            onResult(AdResult.Failed("Disabled by Remote Config"))
            return
        }
        if (!NetworkUtils.isNetworkAvailable(context)) {
            EasyAdsLogger.logSkippedOrBlocked(AdType.INTERSTITIAL, "No internet connection", adUnitId, remoteKey)
            onResult(AdResult.Failed("No internet connection"))
            return
        }
        if (!EasyAds.consent.canRequestAds(context)) {
            EasyAdsLogger.logSkippedOrBlocked(AdType.INTERSTITIAL, "Consent not obtained to request ads", adUnitId, remoteKey)
            onResult(AdResult.Failed("Consent not obtained"))
            return
        }
        if (cachedAds.containsKey(adUnitId)) {
            EasyAdsLogger.d(AdType.INTERSTITIAL, "📦 Interstitial ad already in memory cache | adUnitId=$adUnitId")
            onResult(AdResult.Loaded)
            return
        }
        if (loadingAdUnits.contains(adUnitId)) {
            EasyAdsLogger.d(AdType.INTERSTITIAL, "⏳ Interstitial ad is already loading in background | adUnitId=$adUnitId")
            return
        }

        loadingAdUnits.add(adUnitId)
        val adRequest = AdRequest.Builder().build()
        val appContext = context.applicationContext ?: context

        InterstitialAd.load(
            appContext,
            adUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    loadingAdUnits.remove(adUnitId)
                    cachedAds[adUnitId] = interstitialAd
                    EasyAdsLogger.logLoaded(AdType.INTERSTITIAL, adUnitId)
                    onResult(AdResult.Loaded)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    loadingAdUnits.remove(adUnitId)
                    EasyAdsLogger.logFailedToLoad(
                        AdType.INTERSTITIAL,
                        adUnitId,
                        loadAdError.code,
                        loadAdError.message
                    )
                    onResult(AdResult.Failed(loadAdError.message))
                }
            }
        )
    }

    /**
     * Shows the interstitial ad if ready.
     * If not loaded, disabled by Remote Config, or user is Ad-Free, [onDismissed] is called immediately
     * so user flow is never interrupted.
     */
    fun show(
        activity: Activity,
        adUnitId: String,
        remoteKey: String? = null,
        autoReload: Boolean = true,
        onDismissed: () -> Unit
    ) {
        EasyAdsLogger.logShowRequested(AdType.INTERSTITIAL, adUnitId, remoteKey)

        if (!activity.isSafeForAd() || EasyAds.isAdFree) {
            EasyAdsLogger.logSkippedOrBlocked(
                AdType.INTERSTITIAL,
                if (EasyAds.isAdFree) "User is Ad-Free" else "Activity invalid or finishing",
                adUnitId,
                remoteKey
            )
            onDismissed()
            return
        }

        if (remoteKey != null && !EasyAds.isAdEnabled(remoteKey)) {
            EasyAdsLogger.logSkippedOrBlocked(AdType.INTERSTITIAL, "Disabled by Remote Config", adUnitId, remoteKey)
            onDismissed()
            return
        }

        if (FullscreenGate.isShowing) {
            EasyAdsLogger.logSkippedOrBlocked(AdType.INTERSTITIAL, "Another fullscreen ad is actively displaying", adUnitId, remoteKey)
            onDismissed()
            return
        }

        val ad = cachedAds.remove(adUnitId)
        if (ad == null) {
            EasyAdsLogger.logSkippedOrBlocked(AdType.INTERSTITIAL, "No cached ad available in memory", adUnitId, remoteKey)
            if (autoReload) {
                load(activity, adUnitId, remoteKey)
            }
            onDismissed()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                FullscreenGate.onAdShowing()
                EasyAdsLogger.logShown(AdType.INTERSTITIAL, adUnitId)
            }

            override fun onAdImpression() {
                EasyAdsLogger.logImpression(AdType.INTERSTITIAL, adUnitId)
            }

            override fun onAdClicked() {
                EasyAdsLogger.logClicked(AdType.INTERSTITIAL, adUnitId)
            }

            override fun onAdDismissedFullScreenContent() {
                ad.fullScreenContentCallback = null
                FullscreenGate.onAdDismissed()
                EasyAdsLogger.logDismissed(AdType.INTERSTITIAL, adUnitId)
                if (autoReload) {
                    load(activity, adUnitId, remoteKey)
                }
                onDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                ad.fullScreenContentCallback = null
                FullscreenGate.onAdDismissed()
                EasyAdsLogger.logFailedToShow(
                    AdType.INTERSTITIAL,
                    adUnitId,
                    adError.code,
                    adError.message
                )
                if (autoReload) {
                    load(activity, adUnitId, remoteKey)
                }
                onDismissed()
            }
        }

        ad.show(activity)
    }

    /**
     * Loads and displays an interstitial ad. If already loaded, it shows immediately.
     * Otherwise it waits up to [timeoutMs] to load and show.
     * [onDismissed] is guaranteed to be invoked.
     */
    fun loadAndShow(
        activity: Activity,
        adUnitId: String,
        remoteKey: String? = null,
        timeoutMs: Long = 4000L,
        onDismissed: () -> Unit
    ) {
        if (!activity.isSafeForAd() || EasyAds.isAdFree || !NetworkUtils.isNetworkAvailable(activity)) {
            onDismissed()
            return
        }

        if (remoteKey != null && !EasyAds.isAdEnabled(remoteKey)) {
            onDismissed()
            return
        }

        if (isLoaded(adUnitId)) {
            show(activity, adUnitId, remoteKey, autoReload = true, onDismissed = onDismissed)
            return
        }

        var isHandled = false
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val timeoutRunnable = Runnable {
            if (!isHandled) {
                isHandled = true
                onDismissed()
            }
        }
        handler.postDelayed(timeoutRunnable, timeoutMs)

        load(activity, adUnitId, remoteKey) { result ->
            if (!isHandled) {
                handler.removeCallbacks(timeoutRunnable)
                isHandled = true
                if (result is AdResult.Loaded) {
                    show(activity, adUnitId, remoteKey, autoReload = true, onDismissed = onDismissed)
                } else {
                    onDismissed()
                }
            }
        }
    }

    fun destroyAll() {
        cachedAds.clear()
        loadingAdUnits.clear()
    }
}
