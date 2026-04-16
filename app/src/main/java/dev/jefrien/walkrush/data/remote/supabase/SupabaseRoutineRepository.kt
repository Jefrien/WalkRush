package dev.jefrien.walkrush.data.remote.supabase

import android.util.Log
import dev.jefrien.walkrush.data.remote.openai.OpenAIRoutineGenerator
import dev.jefrien.walkrush.domain.model.routine.Routine
import dev.jefrien.walkrush.domain.model.routine.SessionType
import dev.jefrien.walkrush.domain.model.routine.WeeklyPlan
import dev.jefrien.walkrush.domain.model.routine.WorkoutSession
import dev.jefrien.walkrush.domain.model.userprofile.UserProfile
import dev.jefrien.walkrush.domain.repository.RoutineRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant

private const val TAG = "SupabaseRoutineRepo"

class SupabaseRoutineRepository(
    private val supabase: SupabaseClient,
    private val openAIGenerator: OpenAIRoutineGenerator
) : RoutineRepository {

    override suspend fun generateAndSaveRoutine(profile: UserProfile): Result<Routine> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Generando rutina con IA para userId=${profile.id}")

            val result = openAIGenerator.generateRoutine(profile)
            if (result.isFailure) {
                return@withContext Result.failure(result.exceptionOrNull()!!)
            }

        val routine = result.getOrThrow()
        Log.d(TAG, "Rutina generada por IA. Guardando en Supabase...")

        // 1. Desactivar rutinas anteriores
        try {
            supabase.from("routines")
                .update({
                    set("is_active", false)
                }) {
                    filter { eq("user_id", profile.id) }
                }
        } catch (e: Exception) {
            Log.w(TAG, "No se pudieron desactivar rutinas previas: ${e.message}")
        }

        // 2. Insertar rutina
        val routineDto = RoutineDto(
            id = routine.id,
            userId = routine.userId,
            totalWeeks = routine.totalWeeks,
            projectedWeightLossKg = routine.projectedWeightLossKg,
            recommendations = routine.recommendations,
            isActive = true,
            generatedAt = java.time.Instant.now().toString()
        )
        supabase.from("routines").insert(routineDto)

        // 3. Insertar weekly plans y sessions
        routine.weeklyPlans.forEach { week ->
            val weekDto = WeeklyPlanDto(
                id = week.id,
                routineId = week.routineId,
                weekNumber = week.weekNumber,
                focus = week.focus
            )
            supabase.from("weekly_plans").insert(weekDto)

            val sessionsDto = week.sessions.map { session ->
                WorkoutSessionDto(
                    id = session.id,
                    weeklyPlanId = session.weeklyPlanId,
                    dayOfWeek = session.dayOfWeek,
                    durationMinutes = session.durationMinutes,
                    speedMin = session.speedMin,
                    speedMax = session.speedMax,
                    inclineMin = session.inclineMin,
                    inclineMax = session.inclineMax,
                    sessionType = session.type.name,
                    estimatedCalories = session.estimatedCalories,
                    notes = session.notes
                )
            }
            supabase.from("workout_sessions").insert(sessionsDto)
        }

            Log.d(TAG, "Rutina guardada exitosamente en Supabase")
            Result.success(routine)
        } catch (e: Exception) {
            Log.e(TAG, "Error guardando rutina: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun getActiveRoutine(userId: String): Routine? = try {
        val routineDto = supabase.from("routines")
            .select {
                filter {
                    eq("user_id", userId)
                    eq("is_active", true)
                }
                order("generated_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                limit(1)
            }
            .decodeSingleOrNull<RoutineDto>()

        routineDto?.let { mapRoutineDtoToDomain(it) }
    } catch (e: Exception) {
        Log.e(TAG, "Error obteniendo rutina activa: ${e.message}", e)
        null
    }

    override suspend fun getRoutines(userId: String): List<Routine> = try {
        val routines = supabase.from("routines")
            .select {
                filter { eq("user_id", userId) }
                order("generated_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
            }
            .decodeList<RoutineDto>()

        routines.map { mapRoutineDtoToDomain(it) }
    } catch (e: Exception) {
        Log.e(TAG, "Error obteniendo rutinas: ${e.message}", e)
        emptyList()
    }

    private suspend fun mapRoutineDtoToDomain(dto: RoutineDto): Routine {
        val weeks = supabase.from("weekly_plans")
            .select {
                filter { eq("routine_id", dto.id) }
                order("week_number", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
            }
            .decodeList<WeeklyPlanDto>()
            .map { weekDto ->
                val sessions = supabase.from("workout_sessions")
                    .select {
                        filter { eq("weekly_plan_id", weekDto.id) }
                        order("day_of_week", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
                    }
                    .decodeList<WorkoutSessionDto>()
                    .map { sessionDto ->
                        WorkoutSession(
                            id = sessionDto.id,
                            weeklyPlanId = sessionDto.weeklyPlanId,
                            dayOfWeek = sessionDto.dayOfWeek,
                            durationMinutes = sessionDto.durationMinutes,
                            speedMin = sessionDto.speedMin,
                            speedMax = sessionDto.speedMax,
                            inclineMin = sessionDto.inclineMin,
                            inclineMax = sessionDto.inclineMax,
                            type = SessionType.valueOf(sessionDto.sessionType),
                            estimatedCalories = sessionDto.estimatedCalories,
                            notes = sessionDto.notes
                        )
                    }

                WeeklyPlan(
                    id = weekDto.id,
                    routineId = weekDto.routineId,
                    weekNumber = weekDto.weekNumber,
                    focus = weekDto.focus,
                    sessions = sessions
                )
            }

        return Routine(
            id = dto.id,
            userId = dto.userId,
            totalWeeks = dto.totalWeeks,
            projectedWeightLossKg = dto.projectedWeightLossKg,
            recommendations = dto.recommendations,
            isActive = dto.isActive,
            generatedAt = 0L, // Simplificado
            weeklyPlans = weeks
        )
    }
}
