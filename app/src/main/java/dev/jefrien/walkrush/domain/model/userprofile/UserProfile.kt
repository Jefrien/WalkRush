package dev.jefrien.walkrush.domain.model.userprofile

import kotlinx.serialization.Serializable

/**
 * User fitness profile collected during onboarding
 */
@Serializable
data class UserProfile(
    val id: String,
    val weightKg: Float,
    val heightCm: Float,
    val age: Int,
    val gender: Gender? = null,
    val fitnessGoal: FitnessGoal,
    val targetWeightKg: Float,
    val timelineMonths: Int,
    val daysPerWeek: Int,
    val intensityLevel: IntensityLevel,
    val treadmillCapabilities: TreadmillCapabilities,
    val createdAt: Long = System.currentTimeMillis()
)

enum class Gender { MALE, FEMALE, OTHER }

enum class IntensityLevel {
    BEGINNER,      // 20-30 min, velocidad moderada
    INTERMEDIATE,  // 30-45 min, intervalos
    INTENSE        // 45-60 min, alta intensidad
}

@Serializable
data class FitnessGoal(
    val type: GoalType,
    val description: String
)

enum class GoalType {
    WEIGHT_LOSS,
    MAINTENANCE,
    ENDURANCE,
    CARDIO_HEALTH
}