package dev.jefrien.walkrush.presentation.healthtest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jefrien.walkrush.data.healthconnect.HealthConnectRepositoryImpl
import dev.jefrien.walkrush.data.healthconnect.HealthConnectStatus
import dev.jefrien.walkrush.domain.repository.HealthConnectRepository
import dev.jefrien.walkrush.domain.repository.HealthSessionData
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant

class HealthTestViewModel(
    private val healthConnectRepository: HealthConnectRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HealthTestUiState())
    val uiState: StateFlow<HealthTestUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

    init {
        checkStatus()
    }

    fun checkStatus() {
        viewModelScope.launch {
            val impl = healthConnectRepository as? HealthConnectRepositoryImpl
            val sdkStatus = impl?.getSdkStatus()
            val isUpdateRequired = impl?.isUpdateRequired() ?: false
            val available = healthConnectRepository.isAvailable()
            val hasWrite = healthConnectRepository.hasPermissions()
            val hasRead = (impl?.hasReadPermissions() ?: false)

            _uiState.value = _uiState.value.copy(
                sdkStatus = sdkStatus,
                isUpdateRequired = isUpdateRequired,
                isAvailable = available,
                hasWritePermissions = hasWrite,
                hasReadPermissions = hasRead
            )
        }
    }

    fun startPolling() {
        if (pollingJob?.isActive == true) return
        pollingJob = viewModelScope.launch {
            while (true) {
                readLatestData()
                delay(3000)
            }
        }
        _uiState.value = _uiState.value.copy(isPolling = true)
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
        _uiState.value = _uiState.value.copy(isPolling = false)
    }

    fun togglePolling() {
        if (_uiState.value.isPolling) stopPolling() else startPolling()
    }

    private suspend fun readLatestData() {
        val start = Instant.now().minusSeconds(300) // últimos 5 min
        val end = Instant.now()
        val data = healthConnectRepository.readSessionHealthData(start, end)
        _uiState.value = _uiState.value.copy(
            latestData = data,
            dataHistory = (_uiState.value.dataHistory + data).takeLast(20)
        )
    }

    fun writeTestWorkout() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isWriting = true, writeMessage = null)
            // Simulamos un ejercicio de 10 minutos con 200 calorías
            val session = dev.jefrien.walkrush.domain.model.routine.WorkoutSession(
                id = "test-${System.currentTimeMillis()}",
                weeklyPlanId = "",
                dayOfWeek = 1,
                type = dev.jefrien.walkrush.domain.model.routine.SessionType.STEADY_STATE,
                estimatedCalories = 200,
                notes = "Sesión de prueba Health Connect",
                phases = listOf(
                    dev.jefrien.walkrush.domain.model.routine.WorkoutPhase(
                        id = "",
                        sessionId = "",
                        orderIndex = 0,
                        type = dev.jefrien.walkrush.domain.model.routine.PhaseType.WALK,
                        title = "Caminata de prueba",
                        targetSpeedKmh = 5f,
                        targetInclinePercent = 0f,
                        durationSeconds = 600
                    )
                )
            )
            val result = healthConnectRepository.syncWorkout(session)
            _uiState.value = _uiState.value.copy(
                isWriting = false,
                writeMessage = if (result.isSuccess) "✅ Escritura exitosa" else "❌ Error: ${result.exceptionOrNull()?.message}"
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }

    data class HealthTestUiState(
        val sdkStatus: Int? = null,
        val isUpdateRequired: Boolean = false,
        val isAvailable: Boolean = false,
        val hasWritePermissions: Boolean = false,
        val hasReadPermissions: Boolean = false,
        val isPolling: Boolean = false,
        val latestData: HealthSessionData? = null,
        val dataHistory: List<HealthSessionData> = emptyList(),
        val isWriting: Boolean = false,
        val writeMessage: String? = null
    ) {
        val canRead: Boolean
            get() = isAvailable && hasReadPermissions

        val canWrite: Boolean
            get() = isAvailable && hasWritePermissions
    }
}
