package dev.jefrien.walkrush.data.healthconnect

import dev.jefrien.walkrush.data.manager.HealthDataManager
import dev.jefrien.walkrush.domain.model.routine.WorkoutSession
import dev.jefrien.walkrush.domain.repository.HealthConnectRepository
import dev.jefrien.walkrush.domain.repository.HealthSessionData
import java.time.Instant

/**
 * Wrapper que implementa la interfaz legacy [HealthConnectRepository]
 * delegando al [HealthDataManager].
 */
class HealthConnectRepositoryWrapper(
    private val manager: HealthDataManager
) : HealthConnectRepository {

    override suspend fun isAvailable(): Boolean = manager.isAvailable()
    override suspend fun hasPermissions(): Boolean = manager.hasPermissions()
    override suspend fun syncWorkout(session: WorkoutSession): Result<Unit> = manager.syncWorkout(session)
    override suspend fun readSessionHealthData(startTime: Instant, endTime: Instant): HealthSessionData =
        manager.readSessionHealthData(startTime, endTime)
}
