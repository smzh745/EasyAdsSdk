package com.easyads.sdk.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Gatekeeper ensuring that two fullscreen ads (App Open, Interstitial, Rewarded)
 * never present at the same time.
 * Also holds a short cooldown window after an ad is dismissed so Google AdActivity
 * closing does not re-trigger an immediate resume App Open ad.
 */
internal object FullscreenGate {

    @Volatile
    var isShowing: Boolean = false
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var cooldownJob: Job? = null

    fun onAdShowing() {
        cooldownJob?.cancel()
        isShowing = true
    }

    fun onAdDismissed() {
        cooldownJob?.cancel()
        cooldownJob = scope.launch {
            // 1-second hold-off covers Google AdActivity dismissal triggering Process ON_START
            delay(1000L)
            isShowing = false
        }
    }
}
