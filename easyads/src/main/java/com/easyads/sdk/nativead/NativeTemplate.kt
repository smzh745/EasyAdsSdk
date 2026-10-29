package com.easyads.sdk.nativead

import androidx.annotation.LayoutRes
import com.easyads.sdk.R

/**
 * Defines the presentation size and layout style for Native Ads.
 */
sealed class NativeTemplate(@get:LayoutRes val layoutResId: Int) {
    /** Compact horizontal banner replacement (~56dp). */
    data object BANNER : NativeTemplate(R.layout.layout_native_banner)

    /** Compact card without media (~90dp). */
    data object SMALL : NativeTemplate(R.layout.layout_native_small)

    /** Standard card with 140dp media view (~250dp). */
    data object MEDIUM : NativeTemplate(R.layout.layout_native_medium)

    /** Prominent high-impact card with 180dp media view, star rating & full-width CTA (~360dp). */
    data object LARGE : NativeTemplate(R.layout.layout_native_large)

    /** Fully custom layout provided by host application. */
    data class CUSTOM(@get:LayoutRes val customLayoutRes: Int) : NativeTemplate(customLayoutRes)

    companion object {
        /**
         * Resolves a string value (e.g. from Remote Config: "banner", "small", "medium", "large")
         * into a corresponding NativeTemplate.
         */
        fun fromKey(key: String?, default: NativeTemplate = MEDIUM): NativeTemplate {
            return when (key?.lowercase()?.trim()) {
                "banner" -> BANNER
                "small" -> SMALL
                "medium" -> MEDIUM
                "large" -> LARGE
                else -> default
            }
        }
    }
}
