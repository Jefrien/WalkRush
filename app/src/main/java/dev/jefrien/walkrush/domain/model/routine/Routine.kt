package dev.jefrien.walkrush.domain.model.routine

import kotlinx.serialization.Serializable

/**
 * AI-generated personalized workout routine
 */
@Serializable
data class Routine(
    val id: String,
    val userId: String,
    val totalWeeks: Int,
    val projectedWeightLossKg: Float? = null,
    val recommendations: List<String> = emptyList(),
    val isActive: Boolean = true,
    val generatedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long? = null,
    val weeklyPlans: List<WeeklyPlan> = emptyList()
)
