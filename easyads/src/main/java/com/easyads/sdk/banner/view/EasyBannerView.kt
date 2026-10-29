package com.easyads.sdk.banner.view

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import com.google.android.gms.ads.AdView
import com.easyads.sdk.EasyAds
import com.easyads.sdk.banner.BannerType
import com.easyads.sdk.core.AdResult

class EasyBannerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var activeAdView: AdView? = null
    private var currentAdUnitId: String? = null

    fun load(
        activity: Activity,
        adUnitId: String,
        bannerType: BannerType = BannerType.ADAPTIVE,
        remoteKey: String? = null,
        onResult: (AdResult) -> Unit = {}
    ) {
        currentAdUnitId = adUnitId
        activeAdView = EasyAds.banner.load(
            activity = activity,
            container = this,
            adUnitId = adUnitId,
            bannerType = bannerType,
            remoteKey = remoteKey,
            onResult = onResult
        )
    }

    fun load(
        fragment: androidx.fragment.app.Fragment,
        adUnitId: String,
        bannerType: BannerType = BannerType.ADAPTIVE,
        remoteKey: String? = null,
        onResult: (AdResult) -> Unit = {}
    ) {
        val act = fragment.activity ?: return
        load(act, adUnitId, bannerType, remoteKey, onResult)
    }

    fun pause() {
        activeAdView?.pause()
    }

    fun resume() {
        activeAdView?.resume()
    }

    fun destroy() {
        currentAdUnitId?.let { EasyAds.banner.destroy(it) }
        activeAdView?.destroy()
        activeAdView = null
        removeAllViews()
    }
}
