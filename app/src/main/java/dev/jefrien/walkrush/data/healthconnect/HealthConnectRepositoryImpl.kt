package dev.jefrien.walkrush.data.healthconnect

import android.content.Context
import android.os.Build
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Length
import dev.jefrien.walkrush.domain.model.routine.PhaseType
import dev.jefrien.walkrush.domain.model.routine.WorkoutSession
import dev.jefrien.walkrush.domain.repository.HealthConnectRepository
import dev.jefrien.walkrush.domain.repository.HealthSessionData
import java.time.Instant
import java.time.ZoneOffset

class HealthConnectRepositoryImpl(
    private val context: Context
) : HealthConnectRepository {

    private val client by lazy { HealthConnectClient.getOrCreate(context) }

    override suspend fun isAvailable(): Boolean {
        return try {
            val status = HealthConnectClient.getSdkStatus(context)
            status == HealthConnectClient.SDK_AVAILABLE
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun hasPermissions(): Boolean {
        return try {
            val granted = client.permissionController.getGrantedPermissions()
            requiredPermissions.all { it in granted }
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun syncWorkout(session: WorkoutSession): Result<Unit> = try {
        if (!isAvailable()) {
            return Result.failure(IllegalStateException("Health Connect no está disponible"))
        }
        if (!hasPermissions()) {
            return Result.failure(SecurityException("Faltan permisos de Health Connect"))
        }

        val startTime = Instant.ofEpochMilli(session.createdAt)
        val durationMinutes = session.totalDurationMinutes
        val endTime = startTime.plusSeconds(durationMinutes * 60L)
        val zoneOffset = ZoneOffset.systemDefault().rules.getOffset(endTime)

        val exerciseType = resolveExerciseType(session)

        val exerciseSession = ExerciseSessionRecord(
            startTime = startTime,
            startZoneOffset = zoneOffset,
            endTime = endTime,
            endZoneOffset = zoneOffset,
            exerciseType = exerciseType,
            title = "WalkRush - ${session.type.name.replace("_", " ")}",
            notes = session.notes.takeIf { it.isNotBlank() },
            metadata = androidx.health.connect.client.records.metadata.Metadata.manualEntry()
        )

        val records = mutableListOf<androidx.health.connect.client.records.Record>(exerciseSession)

        // Calories
        val calories = session.actualCalories ?: session.estimatedCalories
        if (calories > 0) {
            records.add(
                TotalCaloriesBurnedRecord(
                    startTime = startTime,
                    startZoneOffset = zoneOffset,
                    endTime = endTime,
                    endZoneOffset = zoneOffset,
                    energy = Energy.calories(calories.toDouble()),
                    metadata = androidx.health.connect.client.records.metadata.Metadata.manualEntry()
                )
            )
        }

        // Estimated distance
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
                    metadata = androidx.health.connect.client.records.metadata.Metadata.manualEntry()
                )
            )
        }

        val response = client.insertRecords(records)

        if (response.recordIdsList.isNotEmpty()) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException("No se insertaron registros"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun readSessionHealthData(startTime: Instant, endTime: Instant): HealthSessionData {
        if (!isAvailable() || !hasPermissions()) {
            return HealthSessionData()
        }

        return try {
            val timeFilter = TimeRangeFilter.between(startTime, endTime)

            // Heart rate
            val heartRateRecords = client.readRecords(
                ReadRecordsRequest(
                    recordType = HeartRateRecord::class,
                    timeRangeFilter = timeFilter
                )
            ).records
            val latestBpm = heartRateRecords
                .flatMap { it.samples }
                .maxByOrNull { it.time }
                ?.beatsPerMinute
                ?.toInt()

            // Calories
            val caloriesRecords = client.readRecords(
                ReadRecordsRequest(
                    recordType = TotalCaloriesBurnedRecord::class,
                    timeRangeFilter = timeFilter
                )
            ).records
            val totalCalories = caloriesRecords.sumOf { it.energy.inCalories }

            // Distance
            val distanceRecords = client.readRecords(
                ReadRecordsRequest(
                    recordType = DistanceRecord::class,
                    timeRangeFilter = timeFilter
                )
            ).records
            val totalDistanceKm = distanceRecords.sumOf { it.distance.inKilometers }

            // Steps
            val stepsRecords = client.readRecords(
                ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = timeFilter
                )
            ).records
            val totalSteps = stepsRecords.sumOf { it.count }

            HealthSessionData(
                heartRateBpm = latestBpm,
                caloriesBurned = totalCalories.takeIf { it > 0 },
                distanceKm = totalDistanceKm.takeIf { it > 0 },
                steps = totalSteps.takeIf { it > 0 }
            )
        } catch (e: Exception) {
            HealthSessionData()
        }
    }

    companion object {
        private val requiredPermissions = setOf(
            HealthPermission.getWritePermission(ExerciseSessionRecord::class),
            HealthPermission.getWritePermission(TotalCaloriesBurnedRecord::class),
            HealthPermission.getWritePermission(DistanceRecord::class),
            HealthPermission.getReadPermission(HeartRateRecord::class),
            HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
            HealthPermission.getReadPermission(DistanceRecord::class),
            HealthPermission.getReadPermission(StepsRecord::class)
        )

        fun getRequiredPermissions(): Set<String> = requiredPermissions
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
