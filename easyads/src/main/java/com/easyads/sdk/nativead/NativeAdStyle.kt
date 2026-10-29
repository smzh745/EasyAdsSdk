package com.easyads.sdk.nativead

import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.TextView
import androidx.annotation.ColorInt
import com.easyads.sdk.R
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

/**
 * Customizes the visual appearance of Native Ads (colors, typography, corner radii)
 * to seamlessly blend with your application's design system and brand palette.
 */
data class NativeAdStyle(
    @ColorInt val ctaBackgroundColor: Int? = null,
    @ColorInt val ctaTextColor: Int? = null,
    val ctaCornerRadiusDp: Float? = null,
    @ColorInt val cardBackgroundColor: Int? = null,
    val cardCornerRadiusDp: Float? = null,
    val cardElevationDp: Float? = null,
    @ColorInt val primaryTextColor: Int? = null,
    @ColorInt val secondaryTextColor: Int? = null,
    @ColorInt val adBadgeBackgroundColor: Int? = null,
    @ColorInt val adBadgeTextColor: Int? = null
) {

    /**
     * Applies this style definition to an inflated [NativeAdView].
     */
    fun applyTo(adView: NativeAdView) {
        val density = adView.resources.displayMetrics.density

        // 1. CTA Button
        val ctaView = adView.findViewById<MaterialButton>(R.id.adCallToAction)
        if (ctaView != null) {
            ctaBackgroundColor?.let { color ->
                ctaView.backgroundTintList = ColorStateList.valueOf(color)
            }
            ctaTextColor?.let { color ->
                ctaView.setTextColor(color)
            }
            ctaCornerRadiusDp?.let { dp ->
                ctaView.cornerRadius = (dp * density).toInt()
            }
        }

        // 2. Headlines & Body text
        val headlineView = adView.findViewById<TextView>(R.id.adHeadline)
        primaryTextColor?.let { headlineView?.setTextColor(it) }

        val bodyView = adView.findViewById<TextView>(R.id.adBody)
        secondaryTextColor?.let { bodyView?.setTextColor(it) }

        val advertiserView = adView.findViewById<TextView>(R.id.adAdvertiser)
        secondaryTextColor?.let { advertiserView?.setTextColor(it) }

        // 3. Ad Attribution Badge
        val badgeView = adView.findViewById<TextView>(R.id.adBadge)
        if (badgeView != null) {
            adBadgeTextColor?.let { badgeView.setTextColor(it) }
            adBadgeBackgroundColor?.let { color ->
                badgeView.background?.let { bg ->
                    if (bg is GradientDrawable) {
                        bg.setColor(color)
                    } else {
                        badgeView.backgroundTintList = ColorStateList.valueOf(color)
                    }
                }
            }
        }

        // 4. Hosting Card (if present)
        val cardView = adView.findViewById<MaterialCardView>(R.id.nativeCardView)
            ?: (adView.parent as? MaterialCardView)
            ?: (adView.getChildAt(0) as? MaterialCardView)

        if (cardView != null) {
            cardBackgroundColor?.let { cardView.setCardBackgroundColor(it) }
            cardCornerRadiusDp?.let { cardView.radius = it * density }
            cardElevationDp?.let { cardView.cardElevation = it * density }
        }
    }
}
