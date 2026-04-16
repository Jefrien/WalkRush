package dev.jefrien.walkrush.domain.model.userprofile

import kotlinx.serialization.Serializable

/**
 * User's treadmill hardware capabilities
 * Critical for AI routine generation
 */
@Serializable
data class TreadmillCapabilities(
    val hasIncline: Boolean,
    val maxInclinePercent: Float = 0f,
    val maxSpeedKmh: Float = 12f,
    val hasHeartRateMonitor: Boolean = false,
    val isSmartTreadmill: Boolean = false
) {
    companion object {
        val NO_INCLINE = TreadmillCapabilities(
            hasIncline = false,
            maxInclinePercent = 0f,
            maxSpeedKmh = 12f
        )
    }
}

/**
 * Extension for AI prompt generation
 */
fun TreadmillCapabilities.toPromptConstraint(): String = when {
    !hasIncline -> "CRITICAL: NO INCLINE SUPPORT. All sessions must have inclineMin=0, inclineMax=0. Never use incline."
    else -> "Incline supported: 0% to ${maxInclinePercent}%. Use varied incline for intensity."
}