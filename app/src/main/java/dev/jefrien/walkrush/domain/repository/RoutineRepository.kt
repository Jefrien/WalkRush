package dev.jefrien.walkrush.domain.repository

import dev.jefrien.walkrush.domain.model.routine.Routine
import dev.jefrien.walkrush.domain.model.userprofile.UserProfile

interface RoutineRepository {
    suspend fun generateAndSaveRoutine(profile: UserProfile): Result<Routine>
    suspend fun getActiveRoutine(userId: String): Routine?
    suspend fun getRoutines(userId: String): List<Routine>
}
