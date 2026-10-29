package com.easyads.sdk.nativead.view

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import com.easyads.sdk.EasyAds
import com.easyads.sdk.core.AdResult
import com.easyads.sdk.core.isSafeForAd
import com.easyads.sdk.nativead.NativeTemplate

class EasyNativeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var currentAdUnitId: String? = null

    /**
     * Loads and displays a Native Ad with the specified size/template.
     *
     * @param activity Hosting activity
     * @param adUnitId AdMob Native ad unit ID
     * @param template Size/template style (BANNER, SMALL, MEDIUM, LARGE, or CUSTOM)
     * @param remoteKey Optional Remote Config toggle key (e.g. "native_home_enabled")
     * @param remoteTemplateKey Optional Remote Config key to dynamically select template ("banner", "small", etc.)
     * @param onResult Callback for load outcome
     */
    fun loadAndShow(
        activity: Activity,
        adUnitId: String,
        template: NativeTemplate = NativeTemplate.MEDIUM,
        remoteKey: String? = null,
        remoteTemplateKey: String? = null,
        onResult: (AdResult) -> Unit = {}
    ) {
        currentAdUnitId = adUnitId

        if (!activity.isSafeForAd() || EasyAds.isAdFree) {
            removeAllViews()
            onResult(AdResult.Failed("Unavailable (Ad-Free or invalid activity)"))
            return
        }

        if (remoteKey != null && !EasyAds.isAdEnabled(remoteKey)) {
            removeAllViews()
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

        EasyAds.nativeAd.load(activity, adUnitId, remoteKey) { nativeAd ->
            if (nativeAd != null && activity.isSafeForAd() && !EasyAds.isAdFree) {
                EasyAds.nativeAd.bind(nativeAd, this@EasyNativeView, effectiveTemplate)
                onResult(AdResult.Loaded)
            } else {
                removeAllViews()
                onResult(AdResult.Failed("Failed to load"))
            }
        }
    }

    fun destroy() {
        currentAdUnitId?.let { EasyAds.nativeAd.destroy(it) }
        removeAllViews()
    }
}
