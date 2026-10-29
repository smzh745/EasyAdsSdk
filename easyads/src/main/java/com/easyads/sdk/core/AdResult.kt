package com.easyads.sdk.core

/**
 * Standard callback result for all ad operations.
 */
sealed interface AdResult {
    data object Loaded : AdResult
    data object Shown : AdResult
    data object Dismissed : AdResult
    data class Failed(val reason: String) : AdResult
    data class RewardEarned(val amount: Int, val type: String) : AdResult
}
