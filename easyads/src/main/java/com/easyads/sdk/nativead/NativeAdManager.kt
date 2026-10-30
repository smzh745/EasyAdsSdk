package com.easyads.sdk.nativead

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.AdChoicesView
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.material.button.MaterialButton
import com.easyads.sdk.EasyAds
import com.easyads.sdk.R
import com.easyads.sdk.core.AdType
import com.easyads.sdk.core.EasyAdsLogger
import com.easyads.sdk.core.NetworkUtils

class NativeAdManager internal constructor() {

    private val cachedNativeAds = mutableMapOf<String, NativeAd>()
    private val inFlightCallbacks = mutableMapOf<String, MutableList<(NativeAd?) -> Unit>>()

    /**
     * Default AdChoices icon placement for native ads.
     * Defaults to [NativeAdOptions.ADCHOICES_TOP_LEFT] so the Google AdChoices / info icon
     * never overlaps Call-To-Action (Install) buttons situated on the right side of native banner layouts.
     */
    var defaultAdChoicesPlacement: Int = NativeAdOptions.ADCHOICES_TOP_LEFT

    fun isLoaded(adUnitId: String): Boolean = cachedNativeAds.containsKey(adUnitId)

    fun load(
        context: Context,
        adUnitId: String,
        remoteKey: String? = null,
        adChoicesPlacement: Int? = null,
        onResult: (NativeAd?) -> Unit = {}
    ) {
        if (EasyAds.isAdFree) {
            EasyAdsLogger.logSkippedOrBlocked(AdType.NATIVE, "User is Ad-Free", adUnitId, remoteKey)
            onResult(null)
            return
        }
        if (remoteKey != null && !EasyAds.isAdEnabled(remoteKey)) {
            EasyAdsLogger.logSkippedOrBlocked(AdType.NATIVE, "Disabled by Remote Config", adUnitId, remoteKey)
            onResult(null)
            return
        }
        if (!NetworkUtils.isNetworkAvailable(context)) {
            EasyAdsLogger.logSkippedOrBlocked(AdType.NATIVE, "No internet connection", adUnitId, remoteKey)
            onResult(null)
            return
        }
        if (!EasyAds.consent.canRequestAds(context)) {
            EasyAdsLogger.logSkippedOrBlocked(AdType.NATIVE, "Consent not obtained to request ads", adUnitId, remoteKey)
            onResult(null)
            return
        }

        cachedNativeAds[adUnitId]?.let {
            EasyAdsLogger.d(AdType.NATIVE, "📦 Native ad reused from memory cache | adUnitId=$adUnitId")
            onResult(it)
            return
        }

        synchronized(inFlightCallbacks) {
            val pending = inFlightCallbacks[adUnitId]
            if (pending != null) {
                EasyAdsLogger.d(AdType.NATIVE, "⏳ Native ad is already in-flight for $adUnitId. Queuing callback. | remoteKey: $remoteKey")
                pending.add(onResult)
                return
            }
            inFlightCallbacks[adUnitId] = mutableListOf(onResult)
        }

        EasyAdsLogger.logRequest(AdType.NATIVE, adUnitId, remoteKey)

        val appContext = context.applicationContext ?: context
        val adLoader = AdLoader.Builder(appContext, adUnitId)
            .forNativeAd { nativeAd ->
                cachedNativeAds[adUnitId]?.destroy()
                cachedNativeAds[adUnitId] = nativeAd
                EasyAdsLogger.logLoaded(AdType.NATIVE, adUnitId, "headline='${nativeAd.headline}'")
                val callbacks = synchronized(inFlightCallbacks) {
                    inFlightCallbacks.remove(adUnitId)
                }.orEmpty()
                callbacks.forEach { it.invoke(nativeAd) }
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    EasyAdsLogger.logFailedToLoad(AdType.NATIVE, adUnitId, error.code, error.message)
                    val callbacks = synchronized(inFlightCallbacks) {
                        inFlightCallbacks.remove(adUnitId)
                    }.orEmpty()
                    callbacks.forEach { it.invoke(null) }
                }

                override fun onAdImpression() {
                    EasyAdsLogger.logImpression(AdType.NATIVE, adUnitId)
                }

                override fun onAdClicked() {
                    EasyAdsLogger.logClicked(AdType.NATIVE, adUnitId)
                }

                override fun onAdOpened() {
                    EasyAdsLogger.d(AdType.NATIVE, "📂 Native ad opened overlay | adUnitId=$adUnitId")
                }

                override fun onAdClosed() {
                    EasyAdsLogger.d(AdType.NATIVE, "📁 Native ad overlay closed | adUnitId=$adUnitId")
                }
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setAdChoicesPlacement(adChoicesPlacement ?: defaultAdChoicesPlacement)
                    .build()
            )
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }

    fun bind(
        nativeAd: NativeAd,
        container: ViewGroup,
        template: NativeTemplate = NativeTemplate.MEDIUM
    ) {
        container.removeAllViews()
        val inflater = LayoutInflater.from(container.context)
        val adView = inflater.inflate(template.layoutResId, container, false) as NativeAdView

        // Locate standard subviews
        val headlineView = adView.findViewById<TextView>(R.id.adHeadline)
        val bodyView = adView.findViewById<TextView>(R.id.adBody)
        val callToActionView = adView.findViewById<MaterialButton>(R.id.adCallToAction)
        val iconView = adView.findViewById<ImageView>(R.id.adAppIcon)
        val mediaView = adView.findViewById<MediaView>(R.id.adMedia)
        val starsView = adView.findViewById<RatingBar>(R.id.adStars)
        val advertiserView = adView.findViewById<TextView>(R.id.adAdvertiser)
        val adChoicesView = adView.findViewById<AdChoicesView>(R.id.adChoicesView)
            ?: adView.findViewById<AdChoicesView>(R.id.adChoices)

        adChoicesView?.let {
            adView.adChoicesView = it
        }

        // Populate Headline
        headlineView?.text = nativeAd.headline
        adView.headlineView = headlineView

        // Populate Body
        if (nativeAd.body != null) {
            bodyView?.text = nativeAd.body
            bodyView?.visibility = View.VISIBLE
            adView.bodyView = bodyView
        } else {
            bodyView?.visibility = View.GONE
        }

        // Populate Call to Action
        if (nativeAd.callToAction != null) {
            callToActionView?.text = nativeAd.callToAction
            callToActionView?.visibility = View.VISIBLE
            adView.callToActionView = callToActionView
        } else {
            callToActionView?.visibility = View.GONE
        }

        // Populate Icon
        if (nativeAd.icon != null) {
            iconView?.setImageDrawable(nativeAd.icon?.drawable)
            iconView?.visibility = View.VISIBLE
            adView.iconView = iconView
        } else if (nativeAd.images.isNotEmpty() && nativeAd.images[0].drawable != null) {
            iconView?.setImageDrawable(nativeAd.images[0].drawable)
            iconView?.visibility = View.VISIBLE
            adView.imageView = iconView
        } else {
            iconView?.visibility = View.GONE
        }

        // Populate Media View
        mediaView?.let {
            if (nativeAd.mediaContent != null) {
                it.mediaContent = nativeAd.mediaContent
                it.visibility = View.VISIBLE
                adView.mediaView = it
            } else {
                it.visibility = View.GONE
            }
        }

        // Populate Star Rating (optional)
        starsView?.let {
            val rating = nativeAd.starRating
            if (rating != null && rating > 0) {
                it.rating = rating.toFloat()
                it.visibility = View.VISIBLE
                adView.starRatingView = it
            } else {
                it.visibility = View.GONE
            }
        }

        // Populate Advertiser (optional)
        advertiserView?.let {
            if (nativeAd.advertiser != null) {
                it.text = nativeAd.advertiser
                it.visibility = View.VISIBLE
                adView.advertiserView = it
            } else {
                it.visibility = View.GONE
            }
        }

        adView.setNativeAd(nativeAd)
        container.addView(adView)
        EasyAdsLogger.logShown(AdType.NATIVE, nativeAd.headline ?: "NativeAd", "template=${template.javaClass.simpleName}")
    }

    fun destroy(adUnitId: String) {
        synchronized(inFlightCallbacks) {
            inFlightCallbacks.remove(adUnitId)
        }
        cachedNativeAds.remove(adUnitId)?.destroy()
    }

    fun destroyAll() {
        synchronized(inFlightCallbacks) {
            inFlightCallbacks.clear()
        }
        cachedNativeAds.values.forEach { it.destroy() }
        cachedNativeAds.clear()
    }
}
