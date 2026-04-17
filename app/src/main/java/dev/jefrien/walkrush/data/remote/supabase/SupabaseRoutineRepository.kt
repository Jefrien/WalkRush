package dev.jefrien.walkrush.data.remote.supabase

import android.util.Log
import dev.jefrien.walkrush.data.remote.openai.OpenAIRoutineGenerator
import dev.jefrien.walkrush.domain.model.routine.PhaseType
import dev.jefrien.walkrush.domain.model.routine.Routine
import dev.jefrien.walkrush.domain.model.routine.SessionType
import dev.jefrien.walkrush.domain.model.routine.WeeklyPlan
import dev.jefrien.walkrush.domain.model.routine.WorkoutPhase
import dev.jefrien.walkrush.domain.model.routine.WorkoutSession
import dev.jefrien.walkrush.domain.model.userprofile.UserProfile
import dev.jefrien.walkrush.domain.repository.RoutineRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
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

            // Desactivar rutinas anteriores
            try {
                supabase.from("routines")
                    .update({ set("is_active", false) }) {
                        filter { eq("user_id", profile.id) }
                    }
            } catch (e: Exception) {
                Log.w(TAG, "No se pudieron desactivar rutinas previas: ${e.message}")
            }

            // Insertar rutina
            val routineDto = RoutineDto(
                id = routine.id,
                userId = routine.userId,
                totalWeeks = routine.totalWeeks,
                projectedWeightLossKg = routine.projectedWeightLossKg,
                recommendations = routine.recommendations,
                isActive = true,
                generatedAt = Instant.now().toString()
            )
            supabase.from("routines").insert(routineDto)

            // Insertar weekly plans, sessions y phases
            routine.weeklyPlans.forEach { week ->
                val weekDto = WeeklyPlanDto(
                    id = week.id,
                    routineId = week.routineId,
                    weekNumber = week.weekNumber,
                    focus = week.focus
                )
                supabase.from("weekly_plans").insert(weekDto)

                week.sessions.forEach { session ->
                    val sessionDto = WorkoutSessionDto(
                        id = session.id,
                        weeklyPlanId = session.weeklyPlanId,
                        dayOfWeek = session.dayOfWeek,
                        durationMinutes = session.totalDurationMinutes,
                        speedMin = 0f,
                        speedMax = 0f,
                        inclineMin = 0f,
                        inclineMax = 0f,
                        sessionType = session.type.name,
                        estimatedCalories = session.estimatedCalories,
                        notes = session.notes
                    )
                    supabase.from("workout_sessions").insert(sessionDto)

                    val phasesDto = session.phases.map { phase ->
                        WorkoutPhaseDto(
                            id = phase.id,
                            sessionId = phase.sessionId,
                            orderIndex = phase.orderIndex,
                            type = phase.type.name,
                            title = phase.title,
                            targetSpeedKmh = phase.targetSpeedKmh,
                            targetInclinePercent = phase.targetInclinePercent,
                            durationSeconds = phase.durationSeconds,
                            notes = phase.notes
                        )
                    }
                    if (phasesDto.isNotEmpty()) {
                        supabase.from("workout_phases").insert(phasesDto)
                    }
                }
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
                order("generated_at", Order.DESCENDING)
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
                order("generated_at", Order.DESCENDING)
            }
            .decodeList<RoutineDto>()

        routines.map { mapRoutineDtoToDomain(it) }
    } catch (e: Exception) {
        Log.e(TAG, "Error obteniendo rutinas: ${e.message}", e)
        emptyList()
    }

    override suspend fun getWorkoutSession(sessionId: String): WorkoutSession? = try {
        val sessionDto = supabase.from("workout_sessions")
            .select { filter { eq("id", sessionId) } }
            .decodeSingleOrNull<WorkoutSessionDto>()

        sessionDto?.let { mapSessionDtoToDomain(it, loadPhases = true) }
    } catch (e: Exception) {
        Log.e(TAG, "Error obteniendo sesión: ${e.message}", e)
        null
    }

    override suspend fun completeWorkoutSession(
        sessionId: String,
        actualDurationMinutes: Int,
        actualCalories: Int?,
        userRating: Int?
    ): Result<Unit> = try {
        supabase.from("workout_sessions")
            .update({
                set("is_completed", true)
                set("completed_at", Instant.now().toString())
                set("actual_duration_minutes", actualDurationMinutes)
                actualCalories?.let { set("actual_calories", it) }
                userRating?.let { set("user_rating", it) }
            }) {
                filter { eq("id", sessionId) }
            }
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Error completando sesión: ${e.message}", e)
        Result.failure(e)
    }

    private suspend fun mapRoutineDtoToDomain(dto: RoutineDto): Routine {
        val weeks = supabase.from("weekly_plans")
            .select {
                filter { eq("routine_id", dto.id) }
                order("week_number", Order.ASCENDING)
            }
            .decodeList<WeeklyPlanDto>()
            .map { weekDto ->
                val sessions = supabase.from("workout_sessions")
                    .select {
                        filter { eq("weekly_plan_id", weekDto.id) }
                        order("day_of_week", Order.ASCENDING)
                    }
                    .decodeList<WorkoutSessionDto>()
                    .map { sessionDto ->
                        mapSessionDtoToDomain(sessionDto, loadPhases = true)
                    }

                WeeklyPlan(
                    id = weekDto.id,
                    routineId = weekDto.routineId,
                    weekNumber = weekDto.weekNumber,
                    focus = weekDto.focus,
                    sessions = sessions
                )
            }

        val generatedAtMillis = dto.generatedAt?.let {
            try {
                java.time.Instant.parse(it).toEpochMilli()
            } catch (e: Exception) {
                System.currentTimeMillis()
            }
        } ?: System.currentTimeMillis()

        return Routine(
            id = dto.id,
            userId = dto.userId,
            totalWeeks = dto.totalWeeks,
            projectedWeightLossKg = dto.projectedWeightLossKg,
            recommendations = dto.recommendations,
            isActive = dto.isActive,
            generatedAt = generatedAtMillis,
            weeklyPlans = weeks
        )
    }

    private suspend fun mapSessionDtoToDomain(dto: WorkoutSessionDto, loadPhases: Boolean): WorkoutSession {
        val phases = if (loadPhases) {
            try {
                supabase.from("workout_phases")
                    .select {
                        filter { eq("session_id", dto.id) }
                        order("order_index", Order.ASCENDING)
                    }
                    .decodeList<WorkoutPhaseDto>()
                    .map { phaseDto ->
                        WorkoutPhase(
                            id = phaseDto.id,
                            sessionId = phaseDto.sessionId,
                            orderIndex = phaseDto.orderIndex,
                            type = PhaseType.valueOf(phaseDto.type),
                            title = phaseDto.title,
                            targetSpeedKmh = phaseDto.targetSpeedKmh,
                            targetInclinePercent = phaseDto.targetInclinePercent,
                            durationSeconds = phaseDto.durationSeconds,
                            notes = phaseDto.notes
                        )
                    }
            } catch (e: Exception) {
                Log.w(TAG, "No se pudieron cargar fases para sesión ${dto.id}: ${e.message}")
                emptyList()
            }
        } else emptyList()

        return WorkoutSession(
            id = dto.id,
            weeklyPlanId = dto.weeklyPlanId,
            dayOfWeek = dto.dayOfWeek,
            type = SessionType.valueOf(dto.sessionType),
            estimatedCalories = dto.estimatedCalories,
            notes = dto.notes,
            isCompleted = dto.isCompleted,
            completedAt = dto.completedAt?.let {
                try {
                    java.time.Instant.parse(it).toEpochMilli()
                } catch (e: Exception) { null }
            },
            actualCalories = dto.actualCalories,
            userRating = dto.userRating,
            phases = phases
        )
    }
}
