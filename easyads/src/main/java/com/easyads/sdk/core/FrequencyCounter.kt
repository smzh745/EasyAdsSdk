package com.easyads.sdk.core

import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

/**
 * Thread-safe frequency counter for triggering ads periodically (e.g. every N actions or random intervals between [minInterval] and [maxInterval]).
 *
 * Example:
 * ```kotlin
 * val downloadCounter = FrequencyCounter(minInterval = 2, maxInterval = 4)
 *
 * fun onDownloadFinished(context: Context) {
 *     downloadCounter.onAction {
 *         context.findActivity()?.let { activity ->
 *             EasyAds.interstitial.show(activity, adUnitId)
 *         }
 *     }
 * }
 * ```
 */
class FrequencyCounter(
    val minInterval: Int = 2,
    val maxInterval: Int = minInterval
) {
    require(minInterval > 0) { "minInterval must be greater than 0" }
    require(maxInterval >= minInterval) { "maxInterval cannot be less than minInterval" }

    private val counter = AtomicInteger(0)

    @Volatile
    private var currentTarget = nextTarget()

    val currentCount: Int get() = counter.get()
    val target: Int get() = currentTarget

    private fun nextTarget(): Int {
        return if (minInterval == maxInterval) {
            minInterval
        } else {
            Random.nextInt(minInterval, maxInterval + 1)
        }
    }

    /**
     * Increments the action count. If the target interval is reached, resets the counter,
     * computes a new random interval, and executes [onTargetReached].
     *
     * @return `true` if the target was reached and [onTargetReached] was invoked.
     */
    fun onAction(onTargetReached: () -> Unit): Boolean {
        val count = counter.incrementAndGet()
        return if (count >= currentTarget) {
            counter.set(0)
            currentTarget = nextTarget()
            onTargetReached()
            true
        } else {
            false
        }
    }

    /**
     * Resets the counter back to zero and picks a new target interval.
     */
    fun reset() {
        counter.set(0)
        currentTarget = nextTarget()
    }
}

