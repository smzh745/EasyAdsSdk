package com.easyads.sdk.config

/**
 * Interface allowing host applications to plug in Firebase Remote Config,
 * SharedPreferences, or any remote flag provider into EasyAds.
 */
interface RemoteConfigProvider {
    /**
     * Returns true if the ad placement identified by [key] is enabled.
     */
    fun isAdEnabled(key: String): Boolean

    /**
     * Returns a string value for the given [key] (e.g. "small", "medium", "large").
     */
    fun getString(key: String, defaultValue: String = ""): String = defaultValue

    /**
     * Returns an integer value for the given [key] (e.g. frequency capping count).
     */
    fun getInt(key: String, defaultValue: Int = 1): Int = defaultValue
}
