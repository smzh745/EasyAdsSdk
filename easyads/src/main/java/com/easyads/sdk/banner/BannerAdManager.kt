package com.easyads.sdk.banner

import android.app.Activity
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.ViewGroup
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.easyads.sdk.EasyAds
import com.easyads.sdk.core.AdResult
import com.easyads.sdk.core.AdType
import com.easyads.sdk.core.EasyAdsLogger
import com.easyads.sdk.core.NetworkUtils
import com.easyads.sdk.core.isSafeForAd

class BannerAdManager internal constructor() {

    private val cachedBanners = mutableMapOf<String, AdView>()

    fun load(
        activity: Activity,
        container: ViewGroup,
        adUnitId: String,
        bannerType: BannerType = BannerType.ADAPTIVE,
        remoteKey: String? = null,
        onResult: (AdResult) -> Unit = {}
    ): AdView? {
        EasyAdsLogger.logRequest(AdType.BANNER, adUnitId, remoteKey, "type=${bannerType.name}")

        if (!activity.isSafeForAd()) {
            EasyAdsLogger.logSkippedOrBlocked(AdType.BANNER, "Activity invalid or finishing", adUnitId, remoteKey)
            onResult(AdResult.Failed("Activity is invalid or finishing"))
            return null
        }
        if (EasyAds.isAdFree) {
            EasyAdsLogger.logSkippedOrBlocked(AdType.BANNER, "User is Ad-Free", adUnitId, remoteKey)
            container.removeAllViews()
            onResult(AdResult.Failed("User is Ad-Free"))
            return null
        }
        if (remoteKey != null && !EasyAds.isAdEnabled(remoteKey)) {
            EasyAdsLogger.logSkippedOrBlocked(AdType.BANNER, "Disabled by Remote Config", adUnitId, remoteKey)
            container.removeAllViews()
            onResult(AdResult.Failed("Disabled by Remote Config"))
            return null
        }
        if (!NetworkUtils.isNetworkAvailable(activity)) {
            EasyAdsLogger.logSkippedOrBlocked(AdType.BANNER, "No internet connection", adUnitId, remoteKey)
            container.removeAllViews()
            onResult(AdResult.Failed("No internet connection"))
            return null
        }
        if (!EasyAds.consent.canRequestAds(activity)) {
            EasyAdsLogger.logSkippedOrBlocked(AdType.BANNER, "Consent not obtained to request ads", adUnitId, remoteKey)
            container.removeAllViews()
            onResult(AdResult.Failed("Consent not obtained"))
            return null
        }

        // Return cached ad view if available for this placement
        cachedBanners[adUnitId]?.let { cachedView ->
            EasyAdsLogger.d(AdType.BANNER, "📦 Reusing cached Banner view | adUnitId=$adUnitId")
            (cachedView.parent as? ViewGroup)?.removeView(cachedView)
            container.removeAllViews()
            container.addView(cachedView)
            EasyAdsLogger.logShown(AdType.BANNER, adUnitId, "Attached cached view")
            onResult(AdResult.Loaded)
            return cachedView
        }

        val adView = AdView(activity)
        adView.adUnitId = adUnitId
        adView.setAdSize(getAdaptiveAdSize(activity))

        val adRequestBuilder = AdRequest.Builder()

        // Handle Collapsible Banner parameter if requested
        when (bannerType) {
            BannerType.COLLAPSIBLE_TOP -> {
                val extras = Bundle().apply { putString("collapsible", "top") }
                adRequestBuilder.addNetworkExtrasBundle(AdMobAdapter::class.java, extras)
            }
            BannerType.COLLAPSIBLE_BOTTOM -> {
                val extras = Bundle().apply { putString("collapsible", "bottom") }
                adRequestBuilder.addNetworkExtrasBundle(AdMobAdapter::class.java, extras)
            }
            BannerType.ADAPTIVE -> { /* Standard Adaptive */ }
        }

        adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                if (!activity.isSafeForAd() || EasyAds.isAdFree) {
                    adView.destroy()
                    return
                }
                container.removeAllViews()
                container.addView(adView)
                cachedBanners[adUnitId] = adView
                EasyAdsLogger.logLoaded(AdType.BANNER, adUnitId)
                EasyAdsLogger.logShown(AdType.BANNER, adUnitId, "Attached to container")
                onResult(AdResult.Loaded)
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                EasyAdsLogger.logFailedToLoad(AdType.BANNER, adUnitId, error.code, error.message)
                container.removeAllViews()
                onResult(AdResult.Failed(error.message))
            }

            override fun onAdImpression() {
                EasyAdsLogger.logImpression(AdType.BANNER, adUnitId)
            }

            override fun onAdClicked() {
                EasyAdsLogger.logClicked(AdType.BANNER, adUnitId)
            }

            override fun onAdOpened() {
                EasyAdsLogger.logShown(AdType.BANNER, adUnitId, "Opened full screen overlay")
                onResult(AdResult.Shown)
            }

            override fun onAdClosed() {
                EasyAdsLogger.logDismissed(AdType.BANNER, adUnitId)
                onResult(AdResult.Dismissed)
            }
        }

        adView.loadAd(adRequestBuilder.build())
        return adView
    }

    private fun getAdaptiveAdSize(activity: Activity): AdSize {
        val display = activity.windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)
        val density = outMetrics.density
        var adWidthPixels = outMetrics.widthPixels.toFloat()
        if (adWidthPixels == 0f) adWidthPixels = outMetrics.widthPixels.toFloat()
        val adWidth = (adWidthPixels / density).toInt()
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth)
    }

    fun destroy(adUnitId: String) {
        cachedBanners.remove(adUnitId)?.let {
            (it.parent as? ViewGroup)?.removeView(it)
            it.destroy()
        }
    }

    fun destroyAll() {
        cachedBanners.values.forEach { adView ->
            (adView.parent as? ViewGroup)?.removeView(adView)
            adView.destroy()
        }
        cachedBanners.clear()
    }
}
