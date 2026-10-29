package com.easyads.sdk.core

import android.util.Log

enum class AdType(val displayName: String) {
    BANNER("BANNER"),
    INTERSTITIAL("INTERSTITIAL"),
    NATIVE("NATIVE"),
    APP_OPEN("APP_OPEN"),
    REWARDED("REWARDED"),
    REMOTE_CONFIG("REMOTE_CONFIG"),
    GENERAL("GENERAL")
}

/**
 * Central logger for EasyAds SDK.
 * Filter in Logcat with tag: `EasyAds`
 */
object EasyAdsLogger {

    const val TAG = "EasyAds"
    var isEnabled: Boolean = false

    fun d(type: AdType, message: String) {
        if (isEnabled) Log.d(TAG, "[${type.displayName}] $message")
    }

    fun i(type: AdType, message: String) {
        if (isEnabled) Log.i(TAG, "[${type.displayName}] $message")
    }

    fun w(type: AdType, message: String) {
        if (isEnabled) Log.w(TAG, "[${type.displayName}] $message")
    }

    fun e(type: AdType, message: String, throwable: Throwable? = null) {
        if (isEnabled) {
            if (throwable != null) {
                Log.e(TAG, "[${type.displayName}] $message", throwable)
            } else {
                Log.e(TAG, "[${type.displayName}] $message")
            }
        }
    }

    fun logRequest(type: AdType, adUnitId: String, remoteKey: String? = null, extra: String? = null) {
        val extraInfo = if (extra != null) " | $extra" else ""
        d(type, "🔄 [REQUEST] Loading ad | adUnitId=$adUnitId | remoteKey=${remoteKey ?: "none"}$extraInfo")
    }

    fun logLoaded(type: AdType, adUnitId: String, extra: String? = null) {
        val extraInfo = if (extra != null) " | $extra" else ""
        i(type, "✅ [LOADED] Ad loaded successfully | adUnitId=$adUnitId$extraInfo")
    }

    fun logFailedToLoad(type: AdType, adUnitId: String, errorCode: Int, errorMessage: String) {
        e(type, "❌ [FAILED_TO_LOAD] Ad failed to load | adUnitId=$adUnitId | code=$errorCode | reason=$errorMessage")
    }

    fun logSkippedOrBlocked(type: AdType, reason: String, adUnitId: String? = null, remoteKey: String? = null) {
        val idInfo = if (adUnitId != null) " | adUnitId=$adUnitId" else ""
        val keyInfo = if (remoteKey != null) " | remoteKey=$remoteKey" else ""
        w(type, "⚠️ [BLOCKED/SKIPPED] $reason$idInfo$keyInfo")
    }

    fun logShowRequested(type: AdType, adUnitId: String, remoteKey: String? = null, extra: String? = null) {
        val extraInfo = if (extra != null) " | $extra" else ""
        d(type, "🎬 [SHOW_REQUESTED] Requesting to show ad | adUnitId=$adUnitId | remoteKey=${remoteKey ?: "none"}$extraInfo")
    }

    fun logShown(type: AdType, adUnitId: String, extra: String? = null) {
        val extraInfo = if (extra != null) " | $extra" else ""
        i(type, "📺 [SHOWN] Ad is now visible on screen | adUnitId=$adUnitId$extraInfo")
    }

    fun logImpression(type: AdType, adUnitId: String) {
        i(type, "👁️ [IMPRESSION] Ad impression recorded by AdMob | adUnitId=$adUnitId")
    }

    fun logClicked(type: AdType, adUnitId: String) {
        i(type, "👆 [CLICKED] User clicked ad | adUnitId=$adUnitId")
    }

    fun logDismissed(type: AdType, adUnitId: String) {
        i(type, "🚪 [DISMISSED] Ad dismissed / closed | adUnitId=$adUnitId")
    }

    fun logFailedToShow(type: AdType, adUnitId: String, errorCode: Int, errorMessage: String) {
        e(type, "❌ [FAILED_TO_SHOW] Ad failed to display | adUnitId=$adUnitId | code=$errorCode | reason=$errorMessage")
    }

    fun logRemoteConfig(key: String, isEnabled: Boolean) {
        d(AdType.REMOTE_CONFIG, "⚙️ [EVALUATION] Key '$key' -> ${if (isEnabled) "ENABLED (true)" else "DISABLED (false)"}")
    }
}
