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
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.material.button.MaterialButton
import com.easyads.sdk.EasyAds
import com.easyads.sdk.R
import com.easyads.sdk.core.NetworkUtils

class NativeAdManager internal constructor() {

    private val cachedNativeAds = mutableMapOf<String, NativeAd>()

    fun load(
        context: Context,
        adUnitId: String,
        remoteKey: String? = null,
        onResult: (NativeAd?) -> Unit = {}
    ) {
        if (EasyAds.isAdFree) {
            onResult(null)
            return
        }
        if (remoteKey != null && !EasyAds.isAdEnabled(remoteKey)) {
            Log.d("EasyAds", "Native ad with remoteKey '$remoteKey' is disabled via Remote Config.")
            onResult(null)
            return
        }
        if (!NetworkUtils.isNetworkAvailable(context)) {
            onResult(null)
            return
        }

        cachedNativeAds[adUnitId]?.let {
            onResult(it)
            return
        }

        val adLoader = AdLoader.Builder(context, adUnitId)
            .forNativeAd { nativeAd ->
                cachedNativeAds[adUnitId]?.destroy()
                cachedNativeAds[adUnitId] = nativeAd
                Log.d("EasyAds", "Native ad loaded for $adUnitId")
                onResult(nativeAd)
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e("EasyAds", "Native ad failed to load: ${error.message}")
                    onResult(null)
                }
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
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
    }

    fun destroy(adUnitId: String) {
        cachedNativeAds.remove(adUnitId)?.destroy()
    }

    fun destroyAll() {
        cachedNativeAds.values.forEach { it.destroy() }
        cachedNativeAds.clear()
    }
}
