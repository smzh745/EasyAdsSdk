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
import com.easyads.sdk.core.AdType
import com.easyads.sdk.core.EasyAdsLogger
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
        EasyAdsLogger.logRequest(AdType.REWARDED, adUnitId, remoteKey)

        if (EasyAds.isAdFree) {
            EasyAdsLogger.logSkippedOrBlocked(AdType.REWARDED, "User is Ad-Free", adUnitId, remoteKey)
            onResult(AdResult.Failed("User is Ad-Free"))
            return
        }
        if (remoteKey != null && !EasyAds.isAdEnabled(remoteKey)) {
            EasyAdsLogger.logSkippedOrBlocked(AdType.REWARDED, "Disabled by Remote Config", adUnitId, remoteKey)
            onResult(AdResult.Failed("Disabled by Remote Config"))
            return
        }
        if (!NetworkUtils.isNetworkAvailable(context)) {
            EasyAdsLogger.logSkippedOrBlocked(AdType.REWARDED, "No internet connection", adUnitId, remoteKey)
            onResult(AdResult.Failed("No internet connection"))
            return
        }
        if (!EasyAds.consent.canRequestAds(context)) {
            EasyAdsLogger.logSkippedOrBlocked(AdType.REWARDED, "Consent not obtained to request ads", adUnitId, remoteKey)
            onResult(AdResult.Failed("Consent not obtained"))
            return
        }
        if (cachedAds.containsKey(adUnitId)) {
            EasyAdsLogger.d(AdType.REWARDED, "📦 Rewarded ad already cached in memory | adUnitId=$adUnitId")
            onResult(AdResult.Loaded)
            return
        }
        if (loadingAdUnits.contains(adUnitId)) {
            EasyAdsLogger.d(AdType.REWARDED, "⏳ Rewarded ad is already loading in background | adUnitId=$adUnitId")
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
                    EasyAdsLogger.logLoaded(AdType.REWARDED, adUnitId)
                    onResult(AdResult.Loaded)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    loadingAdUnits.remove(adUnitId)
                    EasyAdsLogger.logFailedToLoad(AdType.REWARDED, adUnitId, loadAdError.code, loadAdError.message)
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
        EasyAdsLogger.logShowRequested(AdType.REWARDED, adUnitId, remoteKey)

        if (!activity.isSafeForAd() || EasyAds.isAdFree) {
            EasyAdsLogger.logSkippedOrBlocked(
                AdType.REWARDED,
                if (EasyAds.isAdFree) "User is Ad-Free" else "Activity invalid or finishing",
                adUnitId,
                remoteKey
            )
            onDismissed()
            return
        }
        if (remoteKey != null && !EasyAds.isAdEnabled(remoteKey)) {
            EasyAdsLogger.logSkippedOrBlocked(AdType.REWARDED, "Disabled by Remote Config", adUnitId, remoteKey)
            onDismissed()
            return
        }
        if (FullscreenGate.isShowing) {
            EasyAdsLogger.logSkippedOrBlocked(AdType.REWARDED, "Another fullscreen ad is visible", adUnitId, remoteKey)
            onDismissed()
            return
        }

        val ad = cachedAds.remove(adUnitId)
        if (ad == null) {
            EasyAdsLogger.logSkippedOrBlocked(AdType.REWARDED, "No rewarded ad cached for $adUnitId", adUnitId, remoteKey)
            if (autoReload) {
                load(activity, adUnitId, remoteKey)
            }
            onDismissed()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                FullscreenGate.onAdShowing()
                EasyAdsLogger.logShown(AdType.REWARDED, adUnitId)
            }

            override fun onAdImpression() {
                EasyAdsLogger.logImpression(AdType.REWARDED, adUnitId)
            }

            override fun onAdClicked() {
                EasyAdsLogger.logClicked(AdType.REWARDED, adUnitId)
            }

            override fun onAdDismissedFullScreenContent() {
                FullscreenGate.onAdDismissed()
                EasyAdsLogger.logDismissed(AdType.REWARDED, adUnitId)
                if (autoReload) {
                    load(activity, adUnitId, remoteKey)
                }
                onDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                FullscreenGate.onAdDismissed()
                EasyAdsLogger.logFailedToShow(AdType.REWARDED, adUnitId, adError.code, adError.message)
                if (autoReload) {
                    load(activity, adUnitId, remoteKey)
                }
                onDismissed()
            }
        }

        ad.show(activity) { rewardItem ->
            EasyAdsLogger.i(AdType.REWARDED, "🎁 User earned reward: ${rewardItem.amount} ${rewardItem.type} | adUnitId=$adUnitId")
            onUserEarnedReward(rewardItem.amount, rewardItem.type)
        }
    }

    fun destroyAll() {
        cachedAds.clear()
        loadingAdUnits.clear()
    }
}
