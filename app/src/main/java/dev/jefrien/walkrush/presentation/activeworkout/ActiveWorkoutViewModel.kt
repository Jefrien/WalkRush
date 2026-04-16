package dev.jefrien.walkrush.presentation.activeworkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jefrien.walkrush.domain.model.routine.WorkoutPhase
import dev.jefrien.walkrush.domain.model.routine.WorkoutSession
import dev.jefrien.walkrush.domain.repository.RoutineRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ActiveWorkoutViewModel(
    private val routineRepository: RoutineRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<Event>()
    val events: SharedFlow<Event> = _events.asSharedFlow()

    private var timerJob: Job? = null

    fun loadSession(sessionId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val session = routineRepository.getWorkoutSession(sessionId)
            if (session == null || session.phases.isEmpty()) {
                _events.emit(Event.ShowError("No se encontró la sesión o no tiene fases"))
                _events.emit(Event.NavigateBack)
                return@launch
            }
            _uiState.value = UiState(
                isLoading = false,
                session = session,
                totalDurationSeconds = session.totalDurationSeconds,
                remainingTotalSeconds = session.totalDurationSeconds
            )
            startTimer()
        }
    }

    fun togglePauseResume() {
        val state = _uiState.value
        if (state.isCompleted) return

        if (state.isRunning) {
            pauseTimer()
        } else {
            startTimer()
        }
    }

    fun finishWorkout(rating: Int? = null) {
        pauseTimer()
        val state = _uiState.value
        val session = state.session ?: return

        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true)

            val actualCalories = if (state.progress > 0) {
                (session.estimatedCalories * state.progress).toInt()
            } else session.estimatedCalories

            routineRepository.completeWorkoutSession(
                sessionId = session.id,
                actualDurationMinutes = state.elapsedSeconds / 60,
                actualCalories = actualCalories,
                userRating = rating
            ).onSuccess {
                _uiState.value = _uiState.value.copy(isSaving = false, isCompleted = true)
                _events.emit(Event.WorkoutComplete)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(isSaving = false)
                _events.emit(Event.ShowError(error.message ?: "Error guardando sesión"))
            }
        }
    }

    fun confirmCancel() {
        pauseTimer()
        viewModelScope.launch {
            _events.emit(Event.NavigateBack)
        }
    }

    fun skipPhase() {
        val state = _uiState.value
        val phases = state.session?.phases ?: return
        if (state.currentPhaseIndex >= phases.lastIndex) {
            finishWorkout()
            return
        }
        // Sumar el tiempo restante de la fase actual al elapsed time para saltarla
        val remainingInCurrentPhase = state.remainingPhaseSeconds
        val newElapsed = state.elapsedSeconds + remainingInCurrentPhase
        val newPhaseIndex = state.currentPhaseIndex + 1
        val newRemainingTotal = (state.totalDurationSeconds - newElapsed).coerceAtLeast(0)

        _uiState.value = state.copy(
            elapsedSeconds = newElapsed,
            currentPhaseIndex = newPhaseIndex,
            remainingTotalSeconds = newRemainingTotal,
            remainingPhaseSeconds = phases.getOrNull(newPhaseIndex)?.durationSeconds ?: 0
        )
        if (newPhaseIndex < phases.size) {
            viewModelScope.launch { _events.emit(Event.PhaseChanged(phases[newPhaseIndex])) }
        }
    }

    private fun startTimer() {
        if (timerJob?.isActive == true) return
        _uiState.value = _uiState.value.copy(isRunning = true)
        timerJob = viewModelScope.launch {
            while (_uiState.value.isRunning && !_uiState.value.isCompleted) {
                delay(1000)
                tick()
            }
        }
    }

    private fun tick() {
        val state = _uiState.value
        val phases = state.session?.phases ?: return
        if (phases.isEmpty()) return

        val newElapsed = state.elapsedSeconds + 1
        val newRemainingTotal = (state.totalDurationSeconds - newElapsed).coerceAtLeast(0)

        // Calculate current phase
        var accumulated = 0
        var newPhaseIndex = 0
        for ((index, phase) in phases.withIndex()) {
            accumulated += phase.durationSeconds
            if (newElapsed < accumulated) {
                newPhaseIndex = index
                break
            }
            newPhaseIndex = index + 1
        }

        val currentPhase = phases.getOrNull(newPhaseIndex)
        val newRemainingPhase = if (currentPhase != null) {
            (accumulated - newElapsed).coerceAtLeast(0)
        } else 0

        // Phase changed?
        if (newPhaseIndex != state.currentPhaseIndex && currentPhase != null) {
            viewModelScope.launch { _events.emit(Event.PhaseChanged(currentPhase)) }
        }

        // Workout complete?
        if (newElapsed >= state.totalDurationSeconds) {
            pauseTimer()
            _uiState.value = state.copy(
                elapsedSeconds = newElapsed,
                remainingTotalSeconds = 0,
                remainingPhaseSeconds = 0,
                currentPhaseIndex = newPhaseIndex.coerceAtMost(phases.lastIndex),
                isRunning = false
            )
            viewModelScope.launch { _events.emit(Event.WorkoutAutoComplete) }
            return
        }

        _uiState.value = state.copy(
            elapsedSeconds = newElapsed,
            remainingTotalSeconds = newRemainingTotal,
            remainingPhaseSeconds = newRemainingPhase,
            currentPhaseIndex = newPhaseIndex
        )
    }

    private fun pauseTimer() {
        timerJob?.cancel()
        timerJob = null
        _uiState.value = _uiState.value.copy(isRunning = false)
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    data class UiState(
        val isLoading: Boolean = true,
        val isSaving: Boolean = false,
        val session: WorkoutSession? = null,
        val isRunning: Boolean = false,
        val isCompleted: Boolean = false,
        val elapsedSeconds: Int = 0,
        val totalDurationSeconds: Int = 0,
        val remainingTotalSeconds: Int = 0,
        val remainingPhaseSeconds: Int = 0,
        val currentPhaseIndex: Int = 0
    ) {
        val currentPhase: WorkoutPhase?
            get() = session?.phases?.getOrNull(currentPhaseIndex)

        val formattedTotalTime: String
            get() = formatSeconds(remainingTotalSeconds)

        val formattedPhaseTime: String
            get() = formatSeconds(remainingPhaseSeconds)

        val progress: Float
            get() = if (totalDurationSeconds > 0) {
                (elapsedSeconds.toFloat() / totalDurationSeconds).coerceIn(0f, 1f)
            } else 0f

        val phaseProgress: Float
            get() {
                val phase = currentPhase ?: return 0f
                val elapsedInPhase = (phase.durationSeconds - remainingPhaseSeconds).coerceAtLeast(0)
                return if (phase.durationSeconds > 0) {
                    (elapsedInPhase.toFloat() / phase.durationSeconds).coerceIn(0f, 1f)
                } else 0f
            }

        private fun formatSeconds(total: Int): String {
            val mins = total / 60
            val secs = total % 60
            return "%02d:%02d".format(mins, secs)
        }
    }

    sealed class Event {
        object NavigateBack : Event()
        object WorkoutComplete : Event()
        object WorkoutAutoComplete : Event()
        data class PhaseChanged(val phase: WorkoutPhase) : Event()
        data class ShowError(val message: String) : Event()
    }
}
