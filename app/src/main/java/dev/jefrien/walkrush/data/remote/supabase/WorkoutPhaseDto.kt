package dev.jefrien.walkrush.data.remote.supabase

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WorkoutPhaseDto(
    val id: String,
    @SerialName("session_id") val sessionId: String,
    @SerialName("order_index") val orderIndex: Int,
    val type: String,
    val title: String,
    @SerialName("target_speed_kmh") val targetSpeedKmh: Float,
    @SerialName("target_incline_percent") val targetInclinePercent: Float? = 0f,
    @SerialName("duration_seconds") val durationSeconds: Int,
    val notes: String? = null
)
