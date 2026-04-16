package dev.jefrien.walkrush.domain.model.routine

import kotlinx.serialization.Serializable

/**
 * Weekly plan containing workout sessions
 */
@Serializable
data class WeeklyPlan(
    val id: String,
    val routineId: String,
    val weekNumber: Int,
    val focus: String,
    val sessions: List<WorkoutSession> = emptyList()
)
