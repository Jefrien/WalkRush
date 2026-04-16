package dev.jefrien.walkrush.data.remote.supabase

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RoutineDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("total_weeks") val totalWeeks: Int,
    @SerialName("projected_weight_loss_kg") val projectedWeightLossKg: Float? = null,
    val recommendations: List<String> = emptyList(),
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("generated_at") val generatedAt: String? = null
)

@Serializable
data class WeeklyPlanDto(
    val id: String,
    @SerialName("routine_id") val routineId: String,
    @SerialName("week_number") val weekNumber: Int,
    val focus: String
)

@Serializable
data class WorkoutSessionDto(
    val id: String,
    @SerialName("weekly_plan_id") val weeklyPlanId: String,
    @SerialName("day_of_week") val dayOfWeek: Int,
    @SerialName("duration_minutes") val durationMinutes: Int = 0,
    @SerialName("speed_min") val speedMin: Float = 0f,
    @SerialName("speed_max") val speedMax: Float = 0f,
    @SerialName("incline_min") val inclineMin: Float? = 0f,
    @SerialName("incline_max") val inclineMax: Float? = 0f,
    @SerialName("session_type") val sessionType: String,
    @SerialName("estimated_calories") val estimatedCalories: Int,
    val notes: String,
    @SerialName("is_completed") val isCompleted: Boolean = false,
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("actual_calories") val actualCalories: Int? = null,
    @SerialName("user_rating") val userRating: Int? = null
)
