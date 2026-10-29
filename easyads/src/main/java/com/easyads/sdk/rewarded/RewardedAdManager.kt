package com.easyads.sdk.rewarded

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.easyads.sdk.EasyAds
import com.easyads.sdk.core.AdResult
import com.easyads.sdk.core.FullscreenGate
import com.easyads.sdk.core.NetworkUtils
import com.easyads.sdk.core.isSafeForAd

class RewardedAdManager internal constructor() {

    private val cachedAds = mutableMapOf<String, RewardedAd>()
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
            Log.d("EasyAds", "Rewarded ad '$remoteKey' is disabled via Remote Config.")
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
        val request = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            adUnitId,
            request,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    loadingAdUnits.remove(adUnitId)
                    cachedAds[adUnitId] = rewardedAd
                    Log.d("EasyAds", "Rewarded ad loaded for $adUnitId")
                    onResult(AdResult.Loaded)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    loadingAdUnits.remove(adUnitId)
                    Log.e("EasyAds", "Rewarded ad failed: ${loadAdError.message}")
                    onResult(AdResult.Failed(loadAdError.message))
                }
            }
        )
    }

    fun show(
        activity: Activity,
        adUnitId: String,
        remoteKey: String? = null,
        autoReload: Boolean = true,
        onUserEarnedReward: (amount: Int, type: String) -> Unit,
        onDismissed: () -> Unit
    ) {
        if (!activity.isSafeForAd() || EasyAds.isAdFree) {
            onDismissed()
            return
        }
        if (remoteKey != null && !EasyAds.isAdEnabled(remoteKey)) {
            Log.d("EasyAds", "Rewarded ad '$remoteKey' is disabled via Remote Config.")
            onDismissed()
            return
        }
        if (FullscreenGate.isShowing) {
            Log.w("EasyAds", "Another fullscreen ad is visible")
            onDismissed()
            return
        }

        val ad = cachedAds.remove(adUnitId)
        if (ad == null) {
            Log.d("EasyAds", "No rewarded ad cached for $adUnitId")
            if (autoReload) {
                load(activity, adUnitId, remoteKey)
            }
            onDismissed()
            return
        }

        var rewardGranted = false

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
                if (autoReload) {
                    load(activity, adUnitId, remoteKey)
                }
                onDismissed()
            }
        }

        ad.show(activity) { rewardItem ->
            rewardGranted = true
            onUserEarnedReward(rewardItem.amount, rewardItem.type)
        }
    }

    fun destroyAll() {
        cachedAds.clear()
        loadingAdUnits.clear()
    }
}
