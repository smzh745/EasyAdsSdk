package com.easyads.sdk.config

import java.util.concurrent.ConcurrentHashMap

/**
 * Built-in default implementation of [RemoteConfigProvider].
 * Provides thread-safe in-memory key-value storage and intelligent parsing
 * of flags (compatible with boolean, integer 0/1, and string values).
 */
class DefaultRemoteConfigManager : RemoteConfigProvider {

    private val values = ConcurrentHashMap<String, Any>()
    private var customProvider: RemoteConfigProvider? = null

    /**
     * Plug in a custom provider (such as Firebase Remote Config).
     */
    fun setCustomProvider(provider: RemoteConfigProvider) {
        this.customProvider = provider
    }

    /**
     * Plug in a functional evaluator lambda for boolean flags.
     */
    fun setCustomProvider(evaluator: (String) -> Boolean) {
        this.customProvider = object : RemoteConfigProvider {
            override fun isAdEnabled(key: String): Boolean = evaluator(key)
        }
    }

    /**
     * Bulk update key-value pairs (e.g. from fetched Firebase Remote Config map or JSON).
     */
    fun setValues(map: Map<String, Any>) {
        values.putAll(map)
    }

    fun setValue(key: String, value: Any) {
        values[key] = value
    }

    override fun isAdEnabled(key: String): Boolean {
        customProvider?.let {
            return it.isAdEnabled(key)
        }

        val raw = values[key] ?: return true // Enabled by default if not specified

        return when (raw) {
            is Boolean -> raw
            is Number -> raw.toInt() != 0
            is String -> {
                val clean = raw.trim().lowercase()
                clean != "0" && clean != "false" && clean != "off"
            }
            else -> true
        }
    }

    override fun getString(key: String, defaultValue: String): String {
        customProvider?.let {
            return it.getString(key, defaultValue)
        }
        return values[key]?.toString() ?: defaultValue
    }

    override fun getInt(key: String, defaultValue: Int): Int {
        customProvider?.let {
            return it.getInt(key, defaultValue)
        }
        val raw = values[key] ?: return defaultValue
        return when (raw) {
            is Number -> raw.toInt()
            is String -> raw.toIntOrNull() ?: defaultValue
            else -> defaultValue
        }
    }
}
