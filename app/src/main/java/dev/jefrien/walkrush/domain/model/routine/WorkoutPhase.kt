package dev.jefrien.walkrush.domain.model.routine

import kotlinx.serialization.Serializable

enum class PhaseType {
    WARM_UP,
    WALK,
    RUN,
    RECOVERY,
    COOL_DOWN
}

@Serializable
data class WorkoutPhase(
    val id: String,
    val sessionId: String,
    val orderIndex: Int,
    val type: PhaseType,
    val title: String,
    val targetSpeedKmh: Float,
    val targetInclinePercent: Float? = 0f,
    val durationSeconds: Int,
    val notes: String? = null
)
