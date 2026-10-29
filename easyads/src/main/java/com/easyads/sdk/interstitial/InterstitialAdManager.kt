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
        if (EasyAds.isAdFree) {
            onResult(AdResult.Failed("User is Ad-Free"))
            return
        }
        if (remoteKey != null && !EasyAds.isAdEnabled(remoteKey)) {
            Log.d("EasyAds", "Interstitial with remoteKey '$remoteKey' is disabled via Remote Config.")
            onResult(AdResult.Failed("Disabled by Remote Config"))
            return
        }
        if (!NetworkUtils.isNetworkAvailable(context)) {
            onResult(AdResult.Failed("No internet connection"))
            return
        }
        if (cachedAds.containsKey(adUnitId)) {
            onResult(AdResult.Loaded)
            return
        }
        if (loadingAdUnits.contains(adUnitId)) {
            return
        }

        loadingAdUnits.add(adUnitId)
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            context,
            adUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    loadingAdUnits.remove(adUnitId)
                    cachedAds[adUnitId] = interstitialAd
                    Log.d("EasyAds", "Interstitial loaded: $adUnitId")
                    onResult(AdResult.Loaded)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    loadingAdUnits.remove(adUnitId)
                    Log.e("EasyAds", "Interstitial failed to load: ${loadAdError.message}")
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
        if (!activity.isSafeForAd() || EasyAds.isAdFree) {
            onDismissed()
            return
        }

        if (remoteKey != null && !EasyAds.isAdEnabled(remoteKey)) {
            Log.d("EasyAds", "Interstitial '$remoteKey' disabled via Remote Config. Continuing flow.")
            onDismissed()
            return
        }

        if (FullscreenGate.isShowing) {
            Log.w("EasyAds", "Fullscreen ad is already active. Skipping interstitial.")
            onDismissed()
            return
        }

        val ad = cachedAds.remove(adUnitId)
        if (ad == null) {
            Log.d("EasyAds", "No cached interstitial for $adUnitId. Proceeding.")
            if (autoReload) {
                load(activity, adUnitId, remoteKey)
            }
            onDismissed()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                FullscreenGate.onAdShowing()
            }

            override fun onAdDismissedFullScreenContent() {
                FullscreenGate.onAdDismissed()
                if (autoReload) {
                    load(activity, adUnitId, remoteKey)
                }
                onDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                FullscreenGate.onAdDismissed()
                Log.e("EasyAds", "Interstitial failed to show: ${adError.message}")
                if (autoReload) {
                    load(activity, adUnitId, remoteKey)
                }
                onDismissed()
            }
        }

        ad.show(activity)
    }

    fun destroyAll() {
        cachedAds.clear()
        loadingAdUnits.clear()
    }
}
