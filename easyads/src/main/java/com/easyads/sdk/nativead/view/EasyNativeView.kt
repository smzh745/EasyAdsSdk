package com.easyads.sdk.nativead.view

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import com.easyads.sdk.EasyAds
import com.easyads.sdk.R
import com.easyads.sdk.core.AdResult
import com.easyads.sdk.core.AdType
import com.easyads.sdk.core.EasyAdsLogger
import com.easyads.sdk.core.isSafeForAd
import com.easyads.sdk.nativead.NativeTemplate
import com.facebook.shimmer.ShimmerFrameLayout

class EasyNativeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var currentAdUnitId: String? = null
    private var shimmerView: View? = null
    private var isAdLoaded: Boolean = false
    private var isAdLoading: Boolean = false

    /**
     * Optional custom brand style applied to native ads displayed in this view.
     */
    var style: com.easyads.sdk.nativead.NativeAdStyle? = null

    /**
     * Loads and displays a Native Ad with automatic Shimmer placeholder during loading.
     */
    fun loadAndShow(
        activity: Activity,
        adUnitId: String,
        template: NativeTemplate = NativeTemplate.MEDIUM,
        remoteKey: String? = null,
        remoteTemplateKey: String? = null,
        style: com.easyads.sdk.nativead.NativeAdStyle? = null,
        adChoicesPlacement: Int? = null,
        showShimmer: Boolean = true,
        forceReload: Boolean = false,
        onResult: (AdResult) -> Unit = {}
    ) {
        // If ad is already loaded in this view, just ensure it's visible without re-requesting
        if (!forceReload && isAdLoaded && currentAdUnitId == adUnitId) {
            visibility = VISIBLE
            EasyAdsLogger.d(AdType.NATIVE, "Ad already loaded in view. Ensuring VISIBLE without reload | adUnitId=$adUnitId  | remoteKey: $remoteKey")
            onResult(AdResult.Loaded)
            return
        }

        // If ad is already currently loading for this view, ignore duplicate call
        if (!forceReload && isAdLoading && currentAdUnitId == adUnitId) {
            EasyAdsLogger.d(AdType.NATIVE, "Ad is already loading for this view. Ignoring duplicate call | adUnitId=$adUnitId | remoteKey: $remoteKey")
            return
        }

        currentAdUnitId = adUnitId
        EasyAdsLogger.logShowRequested(AdType.NATIVE, adUnitId, remoteKey, "template=${template.javaClass.simpleName}")

        if (!activity.isSafeForAd() || EasyAds.isAdFree) {
            EasyAdsLogger.logSkippedOrBlocked(
                AdType.NATIVE,
                if (EasyAds.isAdFree) "User is Ad-Free" else "Activity invalid or finishing",
                adUnitId,
                remoteKey
            )
            hideAndClean()
            onResult(AdResult.Failed("Unavailable (Ad-Free or invalid activity)"))
            return
        }

        if (remoteKey != null && !EasyAds.isAdEnabled(remoteKey)) {
            EasyAdsLogger.logSkippedOrBlocked(AdType.NATIVE, "Disabled by Remote Config", adUnitId, remoteKey)
            hideAndClean()
            onResult(AdResult.Failed("Disabled by Remote Config"))
            return
        }

        // Dynamically resolve template from remote config if requested
        val effectiveTemplate = if (remoteTemplateKey != null) {
            val remoteSize = EasyAds.getRemoteString(remoteTemplateKey)
            NativeTemplate.fromKey(remoteSize, template)
        } else {
            template
        }

        isAdLoading = true
        isAdLoaded = false

        if (showShimmer) {
            displayShimmer(effectiveTemplate)
        }

        EasyAds.nativeAd.load(activity, adUnitId, remoteKey, adChoicesPlacement) { nativeAd ->
            isAdLoading = false
            stopShimmer()
            if (nativeAd != null && activity.isSafeForAd() && !EasyAds.isAdFree) {
                isAdLoaded = true
                visibility = VISIBLE
                EasyAds.nativeAd.bind(nativeAd, this@EasyNativeView, effectiveTemplate)
                onResult(AdResult.Loaded)
            } else {
                isAdLoaded = false
                hideAndClean()
                onResult(AdResult.Failed("Failed to load"))
            }
        }
    }

    fun loadAndShow(
        fragment: Fragment,
        adUnitId: String,
        template: NativeTemplate = NativeTemplate.MEDIUM,
        remoteKey: String? = null,
        remoteTemplateKey: String? = null,
        style: com.easyads.sdk.nativead.NativeAdStyle? = null,
        adChoicesPlacement: Int? = null,
        showShimmer: Boolean = true,
        forceReload: Boolean = false,
        onResult: (AdResult) -> Unit = {}
    ) {
        val act = fragment.activity ?: run {
            hideAndClean()
            onResult(AdResult.Failed("Fragment not attached to activity"))
            return
        }
        loadAndShow(act, adUnitId, template, remoteKey, remoteTemplateKey, style ?: this.style, adChoicesPlacement, showShimmer, forceReload) { result ->
            if (fragment.isAdded) {
                onResult(result)
            }
        }
    }

    private fun displayShimmer(template: NativeTemplate) {
        removeAllViews()
        val shimmerLayoutId = when (template) {
            NativeTemplate.BANNER, NativeTemplate.SMALL -> R.layout.layout_native_shimmer_banner
            else -> R.layout.layout_native_shimmer_medium
        }
        val view = LayoutInflater.from(context).inflate(shimmerLayoutId, this, false)
        shimmerView = view
        addView(view)
        visibility = VISIBLE
        (view as? ShimmerFrameLayout)?.startShimmer()
    }

    private fun stopShimmer() {
        (shimmerView as? ShimmerFrameLayout)?.stopShimmer()
        shimmerView = null
        removeAllViews()
    }

    private fun hideAndClean() {
        stopShimmer()
        visibility = GONE
    }

    fun destroy() {
        isAdLoaded = false
        isAdLoading = false
        stopShimmer()
        currentAdUnitId?.let { EasyAds.nativeAd.destroy(it) }
        currentAdUnitId = null
        removeAllViews()
    }
}
