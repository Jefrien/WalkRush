package dev.jefrien.walkrush.domain.repository

import dev.jefrien.walkrush.domain.model.routine.Routine
import dev.jefrien.walkrush.domain.model.routine.WorkoutSession
import dev.jefrien.walkrush.domain.model.userprofile.UserProfile

interface RoutineRepository {
    suspend fun generateAndSaveRoutine(profile: UserProfile): Result<Routine>
    suspend fun getActiveRoutine(userId: String): Routine?
    suspend fun getRoutines(userId: String): List<Routine>
    suspend fun getWorkoutSession(sessionId: String): WorkoutSession?
    suspend fun completeWorkoutSession(
        sessionId: String,
        actualDurationMinutes: Int,
        actualCalories: Int?,
        userRating: Int?
    ): Result<Unit>
}
