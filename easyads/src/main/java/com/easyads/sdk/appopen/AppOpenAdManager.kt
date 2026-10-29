package com.easyads.sdk.appopen

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.easyads.sdk.EasyAds
import com.easyads.sdk.core.AdType
import com.easyads.sdk.core.EasyAdsLogger
import com.easyads.sdk.core.FullscreenGate
import com.easyads.sdk.core.NetworkUtils
import com.easyads.sdk.core.isSafeForAd
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AppOpenAdManager internal constructor() {

    private var resumeAd: AppOpenAd? = null
    private var resumeAdUnitId: String? = null
    private var resumeRemoteKey: String? = null
    private var resumeAdLoadedTime: Long = 0L
    private var isResumeLoading = false
    private var allowedActivityClass: Class<out Activity>? = null
    private var disallowedActivityClasses: Set<Class<out Activity>> = emptySet()
    private var isResumeTrackingRegistered = false

    private val scope = CoroutineScope(Dispatchers.Main)

    /**
     * Initializes app-resume App Open ads. When the app is backgrounded and resumed,
     * an app open ad is shown automatically, respecting [FullscreenGate], [EasyAds.isAppOpenBlocked],
     * and optional [remoteKey].
     * If [allowedActivityClass] is provided, only that activity will show resume ads.
     * If [disallowedActivityClasses] are provided, those activities will never show resume ads.
     */
    fun enableAppResume(
        application: Application,
        adUnitId: String,
        remoteKey: String? = null,
        allowedActivityClass: Class<out Activity>? = null,
        disallowedActivityClasses: Set<Class<out Activity>> = emptySet()
    ) {
        this.resumeAdUnitId = adUnitId
        this.resumeRemoteKey = remoteKey
        this.allowedActivityClass = allowedActivityClass
        this.disallowedActivityClasses = disallowedActivityClasses
        if (!isResumeTrackingRegistered) {
            isResumeTrackingRegistered = true
            EasyAdsLogger.d(
                AdType.APP_OPEN,
                "App resume tracking registered | adUnitId=$adUnitId | remoteKey=$remoteKey | allowedActivity=${allowedActivityClass?.simpleName ?: "ANY"} | disallowed=${disallowedActivityClasses.map { it.simpleName }}"
            )
            AppOpenLifecycleTracker(this).register(application)
        }
        loadResumeAd(application)
    }

    private fun loadResumeAd(context: Context) {
        val unitId = resumeAdUnitId ?: return
        val rKey = resumeRemoteKey
        if (EasyAds.isAdFree) {
            EasyAdsLogger.logSkippedOrBlocked(AdType.APP_OPEN, "User is Ad-Free", unitId, rKey)
            return
        }
        if (!NetworkUtils.isNetworkAvailable(context)) {
            EasyAdsLogger.logSkippedOrBlocked(AdType.APP_OPEN, "No internet connection", unitId, rKey)
            return
        }
        if (!EasyAds.consent.canRequestAds(context)) {
            EasyAdsLogger.logSkippedOrBlocked(AdType.APP_OPEN, "Consent not obtained to request ads", unitId, rKey)
            return
        }
        if (isResumeLoading) {
            EasyAdsLogger.d(AdType.APP_OPEN, "⏳ Resume ad is already loading in background")
            return
        }
        if (rKey != null && !EasyAds.isAdEnabled(rKey)) {
            EasyAdsLogger.logSkippedOrBlocked(AdType.APP_OPEN, "Disabled by Remote Config", unitId, rKey)
            return
        }
        if (isResumeAdAvailable()) {
            EasyAdsLogger.d(AdType.APP_OPEN, "📦 Valid unexpired resume ad already cached in memory")
            return
        }

        EasyAdsLogger.logRequest(AdType.APP_OPEN, unitId, rKey, "App Resume Ad")
        isResumeLoading = true
        val request = AdRequest.Builder().build()
        val appContext = context.applicationContext ?: context
        AppOpenAd.load(
            appContext,
            unitId,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    isResumeLoading = false
                    resumeAd = ad
                    resumeAdLoadedTime = SystemClock.elapsedRealtime()
                    EasyAdsLogger.logLoaded(AdType.APP_OPEN, unitId, "App Resume Ad")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isResumeLoading = false
                    resumeAd = null
                    EasyAdsLogger.logFailedToLoad(AdType.APP_OPEN, unitId, error.code, error.message)
                    // Auto-retry in background after 15 seconds so cache is not left permanently empty
                    scope.launch {
                        delay(15000L)
                        if (!isResumeAdAvailable() && !isResumeLoading) {
                            loadResumeAd(context)
                        }
                    }
                }
            }
        )
    }

    private fun isResumeAdAvailable(): Boolean {
        val fourHours = 4 * 60 * 60 * 1000L
        return resumeAd != null && (SystemClock.elapsedRealtime() - resumeAdLoadedTime) < fourHours
    }

    internal fun showOnResume(activity: Activity) {
        val unitId = resumeAdUnitId ?: "unknown"
        val rKey = resumeRemoteKey
        if (!activity.isSafeForAd() || EasyAds.isAdFree || EasyAds.isAppOpenBlocked) {
            EasyAdsLogger.logSkippedOrBlocked(
                AdType.APP_OPEN,
                "Resume display blocked (AdFree=${EasyAds.isAdFree}, blocked=${EasyAds.isAppOpenBlocked})",
                unitId,
                rKey
            )
            return
        }
        val target = allowedActivityClass
        if (target != null && activity.javaClass != target) {
            EasyAdsLogger.d(AdType.APP_OPEN, "Skipping resume ad: Activity ${activity.javaClass.simpleName} does not match allowed target ${target.simpleName}")
            return
        }
        if (disallowedActivityClasses.contains(activity.javaClass)) {
            EasyAdsLogger.d(AdType.APP_OPEN, "Skipping resume ad: Activity ${activity.javaClass.simpleName} is in disallowed list")
            return
        }
        if (rKey != null && !EasyAds.isAdEnabled(rKey)) {
            EasyAdsLogger.logSkippedOrBlocked(AdType.APP_OPEN, "Disabled by Remote Config", unitId, rKey)
            return
        }
        if (FullscreenGate.isShowing) {
            EasyAdsLogger.logSkippedOrBlocked(AdType.APP_OPEN, "Another fullscreen ad is currently active", unitId, rKey)
            return
        }
        if (!isResumeAdAvailable()) {
            EasyAdsLogger.d(AdType.APP_OPEN, "No unexpired resume ad ready. Triggering fresh pre-fetch.")
            loadResumeAd(activity)
            return
        }

        val ad = resumeAd ?: return
        resumeAd = null

        EasyAdsLogger.logShowRequested(AdType.APP_OPEN, unitId, rKey, "On App Resume")

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                FullscreenGate.onAdShowing()
                EasyAdsLogger.logShown(AdType.APP_OPEN, unitId, "On Resume")
            }

            override fun onAdImpression() {
                EasyAdsLogger.logImpression(AdType.APP_OPEN, unitId)
            }

            override fun onAdClicked() {
                EasyAdsLogger.logClicked(AdType.APP_OPEN, unitId)
            }

            override fun onAdDismissedFullScreenContent() {
                ad.fullScreenContentCallback = null
                FullscreenGate.onAdDismissed()
                EasyAdsLogger.logDismissed(AdType.APP_OPEN, unitId)
                loadResumeAd(activity)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                ad.fullScreenContentCallback = null
                FullscreenGate.onAdDismissed()
                EasyAdsLogger.logFailedToShow(AdType.APP_OPEN, unitId, error.code, error.message)
                loadResumeAd(activity)
            }
        }

        ad.show(activity)
    }

    /**
     * Loads and displays a splash App Open ad with a timeout safety guard (default 5000ms).
     * [onDismissed] is guaranteed to be called so your splash screen never hangs.
     */
    fun loadAndShowSplash(
        activity: Activity,
        adUnitId: String,
        remoteKey: String? = null,
        timeoutMs: Long = 5000L,
        onDismissed: () -> Unit
    ) {
        if (!activity.isSafeForAd() || EasyAds.isAdFree || !NetworkUtils.isNetworkAvailable(activity)) {
            EasyAdsLogger.logSkippedOrBlocked(
                AdType.APP_OPEN,
                if (EasyAds.isAdFree) "User is Ad-Free" else "Activity invalid or no internet",
                adUnitId,
                remoteKey
            )
            onDismissed()
            return
        }

        if (remoteKey != null && !EasyAds.isAdEnabled(remoteKey)) {
            EasyAdsLogger.logSkippedOrBlocked(AdType.APP_OPEN, "Disabled by Remote Config", adUnitId, remoteKey)
            onDismissed()
            return
        }

        if (!EasyAds.consent.canRequestAds(activity)) {
            EasyAdsLogger.logSkippedOrBlocked(AdType.APP_OPEN, "Consent not obtained to request ads", adUnitId, remoteKey)
            onDismissed()
            return
        }

        var isHandled = false
        var timeoutJob: Job? = null

        fun finishSplash() {
            if (!isHandled) {
                isHandled = true
                timeoutJob?.cancel()
                onDismissed()
            }
        }

        timeoutJob = scope.launch {
            delay(timeoutMs)
            EasyAdsLogger.w(AdType.APP_OPEN, "⚠️ [TIMEOUT] Splash App Open ad timed out ($timeoutMs ms)")
            finishSplash()
        }

        EasyAdsLogger.logRequest(AdType.APP_OPEN, adUnitId, remoteKey, "Splash App Open")
        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            activity,
            adUnitId,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    if (isHandled || !activity.isSafeForAd()) {
                        return
                    }
                    timeoutJob.cancel()
                    EasyAdsLogger.logLoaded(AdType.APP_OPEN, adUnitId, "Splash App Open")

                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdShowedFullScreenContent() {
                            FullscreenGate.onAdShowing()
                            EasyAdsLogger.logShown(AdType.APP_OPEN, adUnitId, "Splash App Open")
                        }

                        override fun onAdImpression() {
                            EasyAdsLogger.logImpression(AdType.APP_OPEN, adUnitId)
                        }

                        override fun onAdClicked() {
                            EasyAdsLogger.logClicked(AdType.APP_OPEN, adUnitId)
                        }

                        override fun onAdDismissedFullScreenContent() {
                            FullscreenGate.onAdDismissed()
                            EasyAdsLogger.logDismissed(AdType.APP_OPEN, adUnitId)
                            finishSplash()
                        }

                        override fun onAdFailedToShowFullScreenContent(error: AdError) {
                            FullscreenGate.onAdDismissed()
                            EasyAdsLogger.logFailedToShow(AdType.APP_OPEN, adUnitId, error.code, error.message)
                            finishSplash()
                        }
                    }

                    ad.show(activity)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    EasyAdsLogger.logFailedToLoad(AdType.APP_OPEN, adUnitId, error.code, error.message)
                    finishSplash()
                }
            }
        )
    }

    fun destroyAll() {
        resumeAd = null
        isResumeLoading = false
    }
}
