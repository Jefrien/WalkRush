package dev.jefrien.walkrush.domain.model.routine

import kotlinx.serialization.Serializable

/**
 * Single workout session within a weekly plan.
 * A session is composed of multiple phases (warm-up, walk, run, cool-down, etc.)
 */
@Serializable
data class WorkoutSession(
    val id: String,
    val weeklyPlanId: String,
    val dayOfWeek: Int, // 1 = Monday, 7 = Sunday
    val type: SessionType,
    val estimatedCalories: Int,
    val notes: String,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val actualCalories: Int? = null,
    val userRating: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val phases: List<WorkoutPhase> = emptyList()
) {
    val totalDurationSeconds: Int
        get() = phases.sumOf { it.durationSeconds }

    val totalDurationMinutes: Int
        get() = totalDurationSeconds / 60
}
