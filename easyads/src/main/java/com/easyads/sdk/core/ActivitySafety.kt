package com.easyads.sdk.core

import android.app.Activity

/**
 * Checks if the activity is valid and safe to display or bind an ad.
 */
fun Activity?.isSafeForAd(): Boolean {
    if (this == null) return false
    return !isFinishing && !isDestroyed
}
