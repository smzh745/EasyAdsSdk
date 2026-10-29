package com.easyads.sdk.extensions

import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.easyads.sdk.EasyAds
import com.easyads.sdk.banner.BannerType
import com.easyads.sdk.core.AdResult
import com.easyads.sdk.nativead.NativeTemplate

fun Fragment.loadBannerAd(
    container: ViewGroup,
    adUnitId: String,
    bannerType: BannerType = BannerType.ADAPTIVE,
    remoteKey: String? = null,
    onResult: (AdResult) -> Unit = {}
) {
    activity?.let { EasyAds.banner.load(it, container, adUnitId, bannerType, remoteKey, onResult) }
}

fun Fragment.showInterstitialAd(
    adUnitId: String,
    remoteKey: String? = null,
    autoReload: Boolean = true,
    onDismissed: () -> Unit
) {
    val act = activity
    if (act != null) {
        EasyAds.interstitial.show(act, adUnitId, remoteKey, autoReload, onDismissed)
    } else {
        onDismissed()
    }
}

fun Fragment.loadAndShowInterstitialAd(
    adUnitId: String,
    remoteKey: String? = null,
    timeoutMs: Long = 4000L,
    onDismissed: () -> Unit
) {
    val act = activity
    if (act != null) {
        EasyAds.interstitial.loadAndShow(act, adUnitId, remoteKey, timeoutMs, onDismissed)
    } else {
        onDismissed()
    }
}

fun Fragment.loadInterstitialAd(
    adUnitId: String,
    remoteKey: String? = null,
    onResult: (AdResult) -> Unit = {}
) {
    context?.let { EasyAds.interstitial.load(it, adUnitId, remoteKey, onResult) }
}

fun Fragment.loadNativeAd(
    container: ViewGroup,
    adUnitId: String,
    template: NativeTemplate = NativeTemplate.MEDIUM,
    remoteKey: String? = null,
    onResult: (AdResult) -> Unit = {}
) {
    val ctx = context ?: return
    EasyAds.nativeAd.load(ctx, adUnitId, remoteKey) { nativeAd ->
        if (nativeAd != null && isAdded) {
            EasyAds.nativeAd.bind(nativeAd, container, template)
            onResult(AdResult.Loaded)
        } else {
            onResult(AdResult.Failed("Native ad failed to load or disabled"))
        }
    }
}

fun Fragment.showRewardedAd(
    adUnitId: String,
    remoteKey: String? = null,
    autoReload: Boolean = true,
    onUserEarnedReward: (amount: Int, type: String) -> Unit,
    onDismissed: () -> Unit
) {
    val act = activity
    if (act != null) {
        EasyAds.rewarded.show(act, adUnitId, remoteKey, autoReload, onUserEarnedReward, onDismissed)
    } else {
        onDismissed()
    }
}
