package dev.jefrien.walkrush.domain.datasource

import dev.jefrien.walkrush.domain.model.routine.WorkoutSession
import dev.jefrien.walkrush.domain.repository.HealthSessionData
import java.time.Instant

interface HealthDataSource {
    val type: DataSourceType
    suspend fun isAvailable(): Boolean
    suspend fun hasPermissions(): Boolean
    suspend fun syncWorkout(session: WorkoutSession): Result<Unit>
    suspend fun readSessionHealthData(startTime: Instant, endTime: Instant): HealthSessionData
    fun getPermissionContract(): androidx.activity.result.contract.ActivityResultContract<Set<String>, Set<String>>?
    fun getRequiredPermissions(): Set<String>
}
