package dev.jefrien.walkrush.presentation.postworkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jefrien.walkrush.data.manager.HealthDataManager
import dev.jefrien.walkrush.domain.model.routine.WorkoutSession
import dev.jefrien.walkrush.domain.repository.RoutineRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PostWorkoutViewModel(
    private val routineRepository: RoutineRepository,
    private val healthDataManager: HealthDataManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PostWorkoutUiState())
    val uiState: StateFlow<PostWorkoutUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<PostWorkoutEvent>()
    val events: SharedFlow<PostWorkoutEvent> = _events.asSharedFlow()

    fun loadSession(sessionId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val session = routineRepository.getWorkoutSession(sessionId)
            if (session == null) {
                _events.emit(PostWorkoutEvent.NavigateHome)
                return@launch
            }
            val hcAvailable = healthDataManager.isAvailable()
            _uiState.value = PostWorkoutUiState(
                isLoading = false,
                session = session,
                healthConnectAvailable = hcAvailable
            )
        }
    }

    fun setRating(rating: Int) {
        _uiState.value = _uiState.value.copy(rating = rating)
    }

    fun saveRatingAndFinish() {
        viewModelScope.launch {
            val state = _uiState.value
            val session = state.session ?: return@launch
            val rating = state.rating

            if (rating != null) {
                _uiState.value = state.copy(isSaving = true)
                routineRepository.completeWorkoutSession(
                    sessionId = session.id,
                    actualDurationMinutes = session.phases.sumOf { it.durationSeconds } / 60,
                    actualCalories = session.actualCalories,
                    userRating = rating
                )
            }

            if (state.healthConnectAvailable) {
                syncWithHealthDataSource(session)
            }

            _events.emit(PostWorkoutEvent.NavigateHome)
        }
    }

    fun skipRating() {
        viewModelScope.launch {
            val state = _uiState.value
            val session = state.session
            if (state.healthConnectAvailable && session != null) {
                syncWithHealthDataSource(session)
            }
            _events.emit(PostWorkoutEvent.NavigateHome)
        }
    }

    fun requestHealthConnectPermissions() {
        viewModelScope.launch {
            _events.emit(PostWorkoutEvent.RequestHealthConnectPermissions)
        }
    }

    fun syncWithHealthConnectAfterPermissions() {
        viewModelScope.launch {
            val session = _uiState.value.session ?: return@launch
            syncWithHealthDataSource(session)
            _uiState.value = _uiState.value.copy(healthConnectPermissionsNeeded = false)
        }
    }

    private suspend fun syncWithHealthDataSource(session: WorkoutSession) {
        val hasPermissions = healthDataManager.hasPermissions()
        if (!hasPermissions) {
            _uiState.value = _uiState.value.copy(healthConnectPermissionsNeeded = true)
            return
        }
        val result = healthDataManager.syncWorkout(session)
        _uiState.value = _uiState.value.copy(
            healthConnectSyncSuccess = result.isSuccess,
            healthConnectError = result.exceptionOrNull()?.message
        )
    }

    data class PostWorkoutUiState(
        val isLoading: Boolean = true,
        val isSaving: Boolean = false,
        val session: WorkoutSession? = null,
        val rating: Int? = null,
        val healthConnectAvailable: Boolean = false,
        val healthConnectPermissionsNeeded: Boolean = false,
        val healthConnectSyncSuccess: Boolean? = null,
        val healthConnectError: String? = null
    )

    sealed class PostWorkoutEvent {
        object NavigateHome : PostWorkoutEvent()
        object RequestHealthConnectPermissions : PostWorkoutEvent()
    }
}
