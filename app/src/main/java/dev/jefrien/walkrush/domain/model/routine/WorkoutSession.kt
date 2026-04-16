package dev.jefrien.walkrush.domain.model.routine

import kotlinx.serialization.Serializable

/**
 * Single workout session within a weekly plan
 */
@Serializable
data class WorkoutSession(
    val id: String,
    val weeklyPlanId: String,
    val dayOfWeek: Int, // 1 = Monday, 7 = Sunday
    val durationMinutes: Int,
    val speedMin: Float,
    val speedMax: Float,
    val inclineMin: Float? = 0f,
    val inclineMax: Float? = 0f,
    val type: SessionType,
    val estimatedCalories: Int,
    val notes: String,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val actualDurationMinutes: Int? = null,
    val actualCalories: Int? = null,
    val userRating: Int? = null,
    val createdAt: Long = System.currentTimeMillis()
)
