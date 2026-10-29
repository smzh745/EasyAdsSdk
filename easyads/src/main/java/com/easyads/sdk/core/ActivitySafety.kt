package com.easyads.sdk.core

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/**
 * Checks if the activity is valid and safe to display or bind an ad.
 */
fun Activity?.isSafeForAd(): Boolean {
    if (this == null) return false
    return !isFinishing && !isDestroyed
}

/**
 * Traverses the [ContextWrapper] chain to safely resolve the hosting [Activity],
 * avoiding the need for ActivityLifecycleCallbacks or unsafe casts.
 */
fun Context?.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) {
            return current
        }
        current = current.baseContext
    }
    return null
}
