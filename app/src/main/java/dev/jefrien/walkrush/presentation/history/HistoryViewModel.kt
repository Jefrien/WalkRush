package dev.jefrien.walkrush.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jefrien.walkrush.domain.model.routine.WorkoutSession
import dev.jefrien.walkrush.domain.repository.AuthRepository
import dev.jefrien.walkrush.domain.repository.RoutineRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val authRepository: AuthRepository,
    private val routineRepository: RoutineRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val userId = authRepository.currentUserId()
            if (userId == null) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Usuario no autenticado")
                return@launch
            }

            val routines = routineRepository.getRoutines(userId)
            val completedSessions = routines
                .flatMap { it.weeklyPlans }
                .flatMap { it.sessions }
                .filter { it.isCompleted }
                .sortedByDescending { it.completedAt ?: it.createdAt }

            val totalSessions = completedSessions.size
            val totalCalories = completedSessions.sumOf { it.actualCalories ?: it.estimatedCalories }
            val totalMinutes = completedSessions.sumOf { session ->
                val duration = session.phases.sumOf { it.durationSeconds }
                duration / 60
            }

            _uiState.value = HistoryUiState(
                isLoading = false,
                completedSessions = completedSessions,
                totalSessions = totalSessions,
                totalCalories = totalCalories,
                totalMinutes = totalMinutes
            )
        }
    }

    data class HistoryUiState(
        val isLoading: Boolean = true,
        val completedSessions: List<WorkoutSession> = emptyList(),
        val totalSessions: Int = 0,
        val totalCalories: Int = 0,
        val totalMinutes: Int = 0,
        val error: String? = null
    )
}
