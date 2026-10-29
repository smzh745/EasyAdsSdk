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

    private val scope = CoroutineScope(Dispatchers.Main)

    /**
     * Initializes app-resume App Open ads. When the app is backgrounded and resumed,
     * an app open ad is shown automatically, respecting [FullscreenGate], [EasyAds.isAppOpenBlocked],
     * and optional [remoteKey].
     */
    fun enableAppResume(
        application: Application,
        adUnitId: String,
        remoteKey: String? = null
    ) {
        this.resumeAdUnitId = adUnitId
        this.resumeRemoteKey = remoteKey
        AppOpenLifecycleTracker(this).register(application)
        loadResumeAd(application)
    }

    private fun loadResumeAd(context: Context) {
        val unitId = resumeAdUnitId ?: return
        val rKey = resumeRemoteKey
        if (EasyAds.isAdFree || !NetworkUtils.isNetworkAvailable(context) || isResumeLoading) return
        if (rKey != null && !EasyAds.isAdEnabled(rKey)) return
        if (isResumeAdAvailable()) return

        isResumeLoading = true
        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            context,
            unitId,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    isResumeLoading = false
                    resumeAd = ad
                    resumeAdLoadedTime = SystemClock.elapsedRealtime()
                    Log.d("EasyAds", "Resume App Open ad loaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isResumeLoading = false
                    resumeAd = null
                    Log.e("EasyAds", "Resume App Open ad failed: ${error.message}")
                }
            }
        )
    }

    private fun isResumeAdAvailable(): Boolean {
        val fourHours = 4 * 60 * 60 * 1000L
        return resumeAd != null && (SystemClock.elapsedRealtime() - resumeAdLoadedTime) < fourHours
    }

    internal fun showOnResume(activity: Activity) {
        if (!activity.isSafeForAd() || EasyAds.isAdFree || EasyAds.isAppOpenBlocked) return
        val rKey = resumeRemoteKey
        if (rKey != null && !EasyAds.isAdEnabled(rKey)) return
        if (FullscreenGate.isShowing) return
        if (!isResumeAdAvailable()) {
            loadResumeAd(activity)
            return
        }

        val ad = resumeAd ?: return
        resumeAd = null

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                FullscreenGate.onAdShowing()
            }

            override fun onAdDismissedFullScreenContent() {
                FullscreenGate.onAdDismissed()
                loadResumeAd(activity)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                FullscreenGate.onAdDismissed()
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
            onDismissed()
            return
        }

        if (remoteKey != null && !EasyAds.isAdEnabled(remoteKey)) {
            Log.d("EasyAds", "Splash App Open ad '$remoteKey' disabled via Remote Config.")
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
            Log.w("EasyAds", "Splash App Open ad timed out")
            finishSplash()
        }

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

                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdShowedFullScreenContent() {
                            FullscreenGate.onAdShowing()
                        }

                        override fun onAdDismissedFullScreenContent() {
                            FullscreenGate.onAdDismissed()
                            finishSplash()
                        }

                        override fun onAdFailedToShowFullScreenContent(error: AdError) {
                            FullscreenGate.onAdDismissed()
                            finishSplash()
                        }
                    }

                    ad.show(activity)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e("EasyAds", "Splash App Open ad failed: ${error.message}")
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
