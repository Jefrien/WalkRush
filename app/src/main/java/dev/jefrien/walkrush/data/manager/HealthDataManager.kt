package dev.jefrien.walkrush.data.manager

import android.content.Context
import android.content.SharedPreferences
import dev.jefrien.walkrush.data.healthconnect.HealthConnectDataSource
import dev.jefrien.walkrush.data.samsunghealth.SamsungHealthDataSource
import dev.jefrien.walkrush.domain.datasource.DataSourceType
import dev.jefrien.walkrush.domain.datasource.HealthDataSource
import dev.jefrien.walkrush.domain.model.routine.WorkoutSession
import dev.jefrien.walkrush.domain.repository.HealthSessionData
import java.time.Instant

class HealthDataManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val healthConnect = HealthConnectDataSource(context)
    private val samsungHealth = SamsungHealthDataSource(context)

    var activeSourceType: DataSourceType
        get() = DataSourceType.fromName(prefs.getString(KEY_DATA_SOURCE, DataSourceType.HEALTH_CONNECT.name)!!)
        set(value) {
            prefs.edit().putString(KEY_DATA_SOURCE, value.name).apply()
        }

    val activeSource: HealthDataSource
        get() = when (activeSourceType) {
            DataSourceType.HEALTH_CONNECT -> healthConnect
            DataSourceType.SAMSUNG_HEALTH -> samsungHealth
        }

    val availableSources: List<DataSourceType>
        get() = DataSourceType.entries.filter {
            when (it) {
                DataSourceType.HEALTH_CONNECT -> healthConnect.isAvailable()
                DataSourceType.SAMSUNG_HEALTH -> samsungHealth.isAvailable()
            }
        }

    suspend fun isAvailable(): Boolean = activeSource.isAvailable()
    suspend fun hasPermissions(): Boolean = activeSource.hasPermissions()

    suspend fun syncWorkout(session: WorkoutSession): Result<Unit> {
        return activeSource.syncWorkout(session)
    }

    suspend fun readSessionHealthData(startTime: Instant, endTime: Instant): HealthSessionData {
        return activeSource.readSessionHealthData(startTime, endTime)
    }

    fun getPermissionContract(): androidx.activity.result.contract.ActivityResultContract<Set<String>, Set<String>>? {
        return activeSource.getPermissionContract()
    }

    fun getRequiredPermissions(): Set<String> {
        return activeSource.getRequiredPermissions()
    }

    companion object {
        private const val PREFS_NAME = "walkrush_health_prefs"
        private const val KEY_DATA_SOURCE = "active_data_source"
    }
}
