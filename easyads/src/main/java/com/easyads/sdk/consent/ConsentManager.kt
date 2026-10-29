package com.easyads.sdk.consent

import android.app.Activity
import android.content.Context
import com.easyads.sdk.EasyAds
import com.easyads.sdk.core.AdType
import com.easyads.sdk.core.EasyAdsLogger
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform

/**
 * Manages Google User Messaging Platform (UMP) consent gathering for GDPR, EEA, and UK compliance.
 */
class ConsentManager internal constructor() {

    private var consentInformation: ConsentInformation? = null

    /**
     * Checks if ads can be requested based on user consent status.
     * Returns true if consent was gathered or is not required (e.g. non-EEA users).
     */
    fun canRequestAds(context: Context): Boolean {
        val info = consentInformation ?: UserMessagingPlatform.getConsentInformation(context.applicationContext ?: context)
        return info.canRequestAds()
    }

    /**
     * Checks if privacy options requirement status is REQUIRED under GDPR / Google policy.
     * If true, the app must provide an option in settings to revoke or update consent choices.
     */
    fun isPrivacyOptionsRequired(context: Context): Boolean {
        val info = consentInformation ?: UserMessagingPlatform.getConsentInformation(context.applicationContext ?: context)
        return info.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    }

    /**
     * For debugging/testing in EEA geography (e.g. to preview GDPR dialog in non-EEA countries).
     * Only takes effect when [EasyAds.isDebug] is true.
     */
    var isDebugGeographyEEA: Boolean = false

    /**
     * Optional test device hashed ID for Google UMP debug settings.
     */
    var testDeviceHashedId: String? = null

    /**
     * Requests consent information and shows the GDPR consent form if required.
     * Safe to call on every app launch (e.g. from SplashActivity / SplashFragment).
     *
     * @param activity The foreground Activity hosting the consent dialog.
     * @param testDeviceHashedId Optional test device ID for testing EEA geography in debug.
     * @param debugGeographyEEA Force EEA geography in debug builds to test the consent dialog.
     * @param onConsentComplete Callback invoked when consent gathering is finished (or failed)
     *                          and the app is free to initialize/request ads.
     */
    fun requestConsent(
        activity: Activity,
        testDeviceHashedId: String? = null,
        debugGeographyEEA: Boolean = false,
        onConsentComplete: (canRequestAds: Boolean) -> Unit
    ) {
        val paramsBuilder = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)

        val forceEEA = debugGeographyEEA || isDebugGeographyEEA
        val testId = testDeviceHashedId ?: this.testDeviceHashedId

        if (EasyAds.isDebug && (forceEEA || testId != null)) {
            val debugSettingsBuilder = ConsentDebugSettings.Builder(activity)
            if (forceEEA) {
                debugSettingsBuilder.setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
            }
            if (testId != null) {
                debugSettingsBuilder.addTestDeviceHashedId(testId)
            }
            paramsBuilder.setConsentDebugSettings(debugSettingsBuilder.build())
            EasyAdsLogger.d(AdType.GENERAL, "🛡️ UMP Debug settings applied (EEA=$forceEEA, testId=$testId)")
        }

        val consentParams = paramsBuilder.build()
        val info = UserMessagingPlatform.getConsentInformation(activity)
        consentInformation = info

        EasyAdsLogger.d(AdType.GENERAL, "🛡️ Requesting UMP consent info update...")

        info.requestConsentInfoUpdate(
            activity,
            consentParams,
            {
                EasyAdsLogger.d(
                    AdType.GENERAL,
                    "🛡️ Consent info updated: status=${info.consentStatus}, requirement=${info.privacyOptionsRequirementStatus}, canRequestAds=${info.canRequestAds()}"
                )
                if (activity.isFinishing || activity.isDestroyed) {
                    onConsentComplete(info.canRequestAds())
                    return@requestConsentInfoUpdate
                }
                // Load and show the consent form if required
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError: FormError? ->
                    if (formError != null) {
                        EasyAdsLogger.w(
                            AdType.GENERAL,
                            "⚠️ Consent form error [${formError.errorCode}]: ${formError.message}"
                        )
                    } else {
                        EasyAdsLogger.i(
                            AdType.GENERAL,
                            "✅ Consent form completed: canRequestAds=${info.canRequestAds()}"
                        )
                    }
                    onConsentComplete(info.canRequestAds())
                }
            },
            { requestConsentError: FormError ->
                EasyAdsLogger.w(
                    AdType.GENERAL,
                    "⚠️ Failed to update consent info [${requestConsentError.errorCode}]: ${requestConsentError.message}. Fallback canRequestAds=${info.canRequestAds()}"
                )
                onConsentComplete(info.canRequestAds())
            }
        )
    }

    /**
     * Shows the privacy options form so users can revoke or modify their consent at any time.
     * Google AdMob policy requires apps serving EEA/UK users to provide this in Settings.
     */
    fun showPrivacyOptionsForm(
        activity: Activity,
        onDismissed: ((FormError?) -> Unit)? = null
    ) {
        if (activity.isFinishing || activity.isDestroyed) {
            EasyAdsLogger.w(AdType.GENERAL, "⚠️ Activity is finishing or destroyed. Cannot present Privacy Options form.")
            onDismissed?.invoke(null)
            return
        }
        EasyAdsLogger.d(AdType.GENERAL, "🛡️ Presenting Privacy Options form...")
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            if (formError != null) {
                EasyAdsLogger.w(AdType.GENERAL, "⚠️ Privacy options form error: ${formError.message}")
            } else {
                EasyAdsLogger.i(AdType.GENERAL, "✅ Privacy options form dismissed: canRequestAds=${canRequestAds(activity)}")
            }
            onDismissed?.invoke(formError)
        }
    }

    /**
     * Resets consent state (useful during debugging to re-test the first-run consent dialog).
     */
    fun reset(context: Context) {
        UserMessagingPlatform.getConsentInformation(context).reset()
        EasyAdsLogger.i(AdType.GENERAL, "🔄 Consent state reset")
    }
}
