package dev.jefrien.walkrush.data.remote.supabase

import android.util.Log
import dev.jefrien.walkrush.domain.model.userprofile.FitnessGoal
import dev.jefrien.walkrush.domain.model.userprofile.Gender
import dev.jefrien.walkrush.domain.model.userprofile.GoalType
import dev.jefrien.walkrush.domain.model.userprofile.IntensityLevel
import dev.jefrien.walkrush.domain.model.userprofile.TreadmillCapabilities
import dev.jefrien.walkrush.domain.model.userprofile.UserProfile
import dev.jefrien.walkrush.domain.repository.UserProfileRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val TAG = "SupabaseUserProfileRepo"

class SupabaseUserProfileRepository(
    private val supabase: SupabaseClient
) : UserProfileRepository {

    override suspend fun saveUserProfile(profile: UserProfile): Result<Unit> = try {
        val dto = profile.toDto()
        val existing = getUserProfile(profile.id)

        if (existing == null) {
            Log.d(TAG, "Insertando nuevo perfil para userId=${profile.id}")
            supabase.from("user_profiles")
                .insert(dto)
        } else {
            Log.d(TAG, "Actualizando perfil existente para userId=${profile.id}")
            supabase.from("user_profiles")
                .update(dto) {
                    filter { eq("id", profile.id) }
                }
        }

        Log.d(TAG, "Perfil guardado exitosamente")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Error guardando perfil: ${e.message}", e)
        Result.failure(e)
    }

    override suspend fun getUserProfile(userId: String): UserProfile? = try {
        supabase.from("user_profiles")
            .select {
                filter { eq("id", userId) }
            }
            .decodeSingleOrNull<UserProfileDto>()
            ?.toDomain()
            .also { profile ->
                Log.d(TAG, "getUserProfile($userId) -> ${if (profile != null) "ENCONTRADO" else "NO ENCONTRADO"}")
            }
    } catch (e: Exception) {
        Log.e(TAG, "Error obteniendo perfil para $userId: ${e.message}", e)
        null
    }

    override fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        // Simplified - in production use Realtime
        emit(getUserProfile(userId))
    }

    override suspend fun hasCompletedOnboarding(userId: String): Boolean {
        val hasProfile = getUserProfile(userId) != null
        Log.d(TAG, "hasCompletedOnboarding($userId) -> $hasProfile")
        return hasProfile
    }

    override suspend fun markOnboardingCompleted(userId: String) {
        // Already saved in user_profiles, could add flag if needed
    }

    // DTOs
    @Serializable
    data class UserProfileDto(
        val id: String,
        @SerialName("weight_kg") val weightKg: Float,
        @SerialName("height_cm") val heightCm: Float,
        val age: Int,
        val gender: String?,
        @SerialName("fitness_goal_type") val fitnessGoalType: String,
        @SerialName("target_weight_kg") val targetWeightKg: Float,
        @SerialName("timeline_months") val timelineMonths: Int,
        @SerialName("days_per_week") val daysPerWeek: Int,
        @SerialName("intensity_level") val intensityLevel: String,
        @SerialName("has_incline") val hasIncline: Boolean,
        @SerialName("max_incline_percent") val maxInclinePercent: Float,
        @SerialName("max_speed_kmh") val maxSpeedKmh: Float,
        @SerialName("created_at") val createdAt: String? = null,
    )

    private fun UserProfile.toDto() = UserProfileDto(
        id = id,
        weightKg = weightKg,
        heightCm = heightCm,
        age = age,
        gender = gender?.name,
        fitnessGoalType = fitnessGoal.type.name,
        targetWeightKg = targetWeightKg,
        timelineMonths = timelineMonths,
        daysPerWeek = daysPerWeek,
        intensityLevel = intensityLevel.name,
        hasIncline = treadmillCapabilities.hasIncline,
        maxInclinePercent = treadmillCapabilities.maxInclinePercent,
        maxSpeedKmh = treadmillCapabilities.maxSpeedKmh,
        createdAt = null,
    )

    private fun UserProfileDto.toDomain() = UserProfile(
        id = id,
        weightKg = weightKg,
        heightCm = heightCm,
        age = age,
        gender = gender?.let { Gender.valueOf(it) },
        fitnessGoal = FitnessGoal(
            type = GoalType.valueOf(fitnessGoalType),
            description = ""
        ),
        targetWeightKg = targetWeightKg,
        timelineMonths = timelineMonths,
        daysPerWeek = daysPerWeek,
        intensityLevel = IntensityLevel.valueOf(intensityLevel),
        treadmillCapabilities = TreadmillCapabilities(
            hasIncline = hasIncline,
            maxInclinePercent = maxInclinePercent,
            maxSpeedKmh = maxSpeedKmh
        ),
        createdAt = createdAt?.let {
            0L
        } ?: System.currentTimeMillis()
    )
}