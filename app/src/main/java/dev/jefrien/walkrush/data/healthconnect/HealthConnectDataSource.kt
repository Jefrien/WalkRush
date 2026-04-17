package dev.jefrien.walkrush.data.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Length
import dev.jefrien.walkrush.domain.datasource.DataSourceType
import dev.jefrien.walkrush.domain.datasource.HealthDataSource
import dev.jefrien.walkrush.domain.model.routine.PhaseType
import dev.jefrien.walkrush.domain.model.routine.WorkoutSession
import dev.jefrien.walkrush.domain.repository.HealthSessionData
import java.time.Instant
import java.time.ZoneOffset

class HealthConnectDataSource(
    private val context: Context
) : HealthDataSource {

    override val type = DataSourceType.HEALTH_CONNECT

    private val client by lazy { HealthConnectClient.getOrCreate(context) }

    override suspend fun isAvailable(): Boolean {
        return try {
            val status = HealthConnectClient.getSdkStatus(context, PROVIDER_PACKAGE_NAME)
            status == HealthConnectClient.SDK_AVAILABLE
        } catch (e: Exception) {
            false
        }
    }

    fun getSdkStatus(): Int {
        return try {
            HealthConnectClient.getSdkStatus(context, PROVIDER_PACKAGE_NAME)
        } catch (e: Exception) {
            HealthConnectClient.SDK_UNAVAILABLE
        }
    }

    fun isUpdateRequired(): Boolean {
        return getSdkStatus() == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED
    }

    override suspend fun hasPermissions(): Boolean {
        return try {
            val granted = client.permissionController.getGrantedPermissions()
            writePermissions.all { it in granted }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun hasReadPermissions(): Boolean {
        return try {
            val granted = client.permissionController.getGrantedPermissions()
            readPermissions.all { it in granted }
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun syncWorkout(session: WorkoutSession): Result<Unit> = try {
        if (!isAvailable()) {
            return Result.failure(IllegalStateException("Health Connect no está disponible"))
        }
        if (!hasPermissions()) {
            return Result.failure(SecurityException("Faltan permisos de escritura"))
        }

        val startTime = Instant.ofEpochMilli(session.createdAt)
        val durationMinutes = session.totalDurationMinutes
        val endTime = startTime.plusSeconds(durationMinutes * 60L)
        val zoneOffset = ZoneOffset.systemDefault().rules.getOffset(endTime)
        val metadata = Metadata.autoRecorded(device = Device(type = Device.TYPE_PHONE))

        val exerciseType = resolveExerciseType(session)
        val exerciseSession = ExerciseSessionRecord(
            startTime = startTime,
            startZoneOffset = zoneOffset,
            endTime = endTime,
            endZoneOffset = zoneOffset,
            exerciseType = exerciseType,
            title = "WalkRush - ${session.type.name.replace("_", " ")}",
            notes = session.notes.takeIf { it.isNotBlank() },
            metadata = metadata
        )

        val records = mutableListOf<androidx.health.connect.client.records.Record>(exerciseSession)

        val calories = session.actualCalories ?: session.estimatedCalories
        if (calories > 0) {
            records.add(
                TotalCaloriesBurnedRecord(
                    startTime = startTime,
                    startZoneOffset = zoneOffset,
                    endTime = endTime,
                    endZoneOffset = zoneOffset,
                    energy = Energy.calories(calories.toDouble()),
                    metadata = metadata
                )
            )
        }

        val estimatedDistanceKm = session.phases.sumOf {
            it.targetSpeedKmh * (it.durationSeconds / 3600.0)
        }
        if (estimatedDistanceKm > 0) {
            records.add(
                DistanceRecord(
                    startTime = startTime,
                    startZoneOffset = zoneOffset,
                    endTime = endTime,
                    endZoneOffset = zoneOffset,
                    distance = Length.kilometers(estimatedDistanceKm),
                    metadata = metadata
                )
            )
        }

        client.insertRecords(records)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun readSessionHealthData(startTime: Instant, endTime: Instant): HealthSessionData {
        if (!isAvailable()) {
            return HealthSessionData(healthConnectStatus = dev.jefrien.walkrush.data.healthconnect.HealthConnectStatus.NOT_AVAILABLE)
        }

        val hasRead = hasReadPermissions()
        if (!hasRead) {
            return HealthSessionData(healthConnectStatus = dev.jefrien.walkrush.data.healthconnect.HealthConnectStatus.PERMISSIONS_MISSING)
        }

        return try {
            val timeFilter = TimeRangeFilter.between(startTime, endTime)

            val heartRateBpm = try {
                val response = client.readRecords(
                    ReadRecordsRequest(
                        recordType = HeartRateRecord::class,
                        timeRangeFilter = timeFilter
                    )
                )
                response.records
                    .flatMap { it.samples }
                    .maxByOrNull { it.time }
                    ?.beatsPerMinute
                    ?.toInt()
            } catch (e: Exception) { null }

            val aggregateResponse = try {
                client.aggregate(
                    AggregateRequest(
                        metrics = setOf(
                            StepsRecord.COUNT_TOTAL,
                            DistanceRecord.DISTANCE_TOTAL,
                            TotalCaloriesBurnedRecord.ENERGY_TOTAL
                        ),
                        timeRangeFilter = timeFilter
                    )
                )
            } catch (e: Exception) { null }

            val totalSteps = aggregateResponse?.get(StepsRecord.COUNT_TOTAL)
            val totalDistance = aggregateResponse?.get(DistanceRecord.DISTANCE_TOTAL)
            val totalCalories = aggregateResponse?.get(TotalCaloriesBurnedRecord.ENERGY_TOTAL)

            HealthSessionData(
                heartRateBpm = heartRateBpm,
                caloriesBurned = totalCalories?.inKilocalories,
                distanceKm = totalDistance?.inKilometers,
                steps = totalSteps,
                healthConnectStatus = dev.jefrien.walkrush.data.healthconnect.HealthConnectStatus.CONNECTED
            )
        } catch (e: Exception) {
            HealthSessionData(healthConnectStatus = dev.jefrien.walkrush.data.healthconnect.HealthConnectStatus.ERROR)
        }
    }

    override fun getPermissionContract(): androidx.activity.result.contract.ActivityResultContract<Set<String>, Set<String>> {
        return androidx.health.connect.client.PermissionController.createRequestPermissionResultContract()
    }

    override fun getRequiredPermissions(): Set<String> = writePermissions + readPermissions

    companion object {
        const val PROVIDER_PACKAGE_NAME = "com.google.android.apps.healthdata"

        private val writePermissions = setOf(
            HealthPermission.getWritePermission(ExerciseSessionRecord::class),
            HealthPermission.getWritePermission(TotalCaloriesBurnedRecord::class),
            HealthPermission.getWritePermission(DistanceRecord::class)
        )

        private val readPermissions = setOf(
            HealthPermission.getReadPermission(HeartRateRecord::class),
            HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
            HealthPermission.getReadPermission(DistanceRecord::class),
            HealthPermission.getReadPermission(StepsRecord::class)
        )
    }
}

private fun resolveExerciseType(session: WorkoutSession): Int {
    val hasRun = session.phases.any { it.type == PhaseType.RUN }
    val hasWalk = session.phases.any { it.type == PhaseType.WALK }
    return when {
        hasRun -> ExerciseSessionRecord.EXERCISE_TYPE_RUNNING
        hasWalk -> ExerciseSessionRecord.EXERCISE_TYPE_WALKING
        else -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
    }
}
