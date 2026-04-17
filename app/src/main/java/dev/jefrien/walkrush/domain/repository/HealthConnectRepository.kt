package dev.jefrien.walkrush.domain.repository

import dev.jefrien.walkrush.domain.model.routine.WorkoutSession
import dev.jefrien.walkrush.data.healthconnect.HealthConnectStatus
import java.time.Instant

data class HealthSessionData(
    val heartRateBpm: Int? = null,
    val caloriesBurned: Double? = null,
    val distanceKm: Double? = null,
    val steps: Long? = null,
    val healthConnectStatus: HealthConnectStatus = HealthConnectStatus.CONNECTED
)

/**
 * Interfaz legacy para compatibilidad. Se mantiene para no romper código existente.
 * Nuevo código debería usar [dev.jefrien.walkrush.data.manager.HealthDataManager] directamente.
 */
interface HealthConnectRepository {
    suspend fun isAvailable(): Boolean
    suspend fun hasPermissions(): Boolean
    suspend fun syncWorkout(session: WorkoutSession): Result<Unit>
    suspend fun readSessionHealthData(startTime: Instant, endTime: Instant): HealthSessionData
}
