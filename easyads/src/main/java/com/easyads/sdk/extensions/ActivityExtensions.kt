package com.easyads.sdk.extensions

import android.app.Activity
import android.view.ViewGroup
import com.easyads.sdk.EasyAds
import com.easyads.sdk.banner.BannerType
import com.easyads.sdk.core.AdResult
import com.easyads.sdk.nativead.NativeTemplate

fun Activity.loadBannerAd(
    container: ViewGroup,
    adUnitId: String,
    bannerType: BannerType = BannerType.ADAPTIVE,
    remoteKey: String? = null,
    onResult: (AdResult) -> Unit = {}
) {
    EasyAds.banner.load(this, container, adUnitId, bannerType, remoteKey, onResult)
}

fun Activity.showInterstitialAd(
    adUnitId: String,
    remoteKey: String? = null,
    autoReload: Boolean = true,
    onDismissed: () -> Unit
) {
    EasyAds.interstitial.show(this, adUnitId, remoteKey, autoReload, onDismissed)
}

fun Activity.loadInterstitialAd(
    adUnitId: String,
    remoteKey: String? = null,
    onResult: (AdResult) -> Unit = {}
) {
    EasyAds.interstitial.load(this, adUnitId, remoteKey, onResult)
}

fun Activity.loadNativeAd(
    container: ViewGroup,
    adUnitId: String,
    template: NativeTemplate = NativeTemplate.MEDIUM,
    remoteKey: String? = null,
    onResult: (AdResult) -> Unit = {}
) {
    EasyAds.nativeAd.load(this, adUnitId, remoteKey) { nativeAd ->
        if (nativeAd != null) {
            EasyAds.nativeAd.bind(nativeAd, container, template)
            onResult(AdResult.Loaded)
        } else {
            onResult(AdResult.Failed("Native ad failed to load or disabled"))
        }
    }
}

fun Activity.showRewardedAd(
    adUnitId: String,
    remoteKey: String? = null,
    autoReload: Boolean = true,
    onUserEarnedReward: (amount: Int, type: String) -> Unit,
    onDismissed: () -> Unit
) {
    EasyAds.rewarded.show(this, adUnitId, remoteKey, autoReload, onUserEarnedReward, onDismissed)
}

fun Activity.showSplashAppOpenAd(
    adUnitId: String,
    remoteKey: String? = null,
    timeoutMs: Long = 5000L,
    onDismissed: () -> Unit
) {
    EasyAds.appOpen.loadAndShowSplash(this, adUnitId, remoteKey, timeoutMs, onDismissed)
}
