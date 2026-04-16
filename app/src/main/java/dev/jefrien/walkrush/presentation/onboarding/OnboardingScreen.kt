package dev.jefrien.walkrush.presentation.onboarding

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jefrien.walkrush.presentation.onboarding.components.StepIndicator
import dev.jefrien.walkrush.presentation.onboarding.steps.BasicInfoStep
import dev.jefrien.walkrush.presentation.onboarding.steps.GoalsStep
import dev.jefrien.walkrush.presentation.onboarding.steps.ScheduleStep
import dev.jefrien.walkrush.presentation.onboarding.steps.TreadmillStep
import dev.jefrien.walkrush.presentation.onboarding.steps.WelcomeStep
import org.koin.androidx.compose.koinViewModel

@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit,
    viewModel: OnboardingViewModel = koinViewModel()
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is OnboardingViewModel.OnboardingEvent.OnboardingComplete -> {
                    onOnboardingComplete()
                }
                is OnboardingViewModel.OnboardingEvent.ShowError -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            // Navigation buttons
            OnboardingBottomBar(
                currentStep = viewModel.currentStep,
                totalSteps = 5,
                canProceed = viewModel.canProceed,
                isLoading = uiState.value.isLoading,
                onBack = viewModel::previousStep,
                onNext = {
                    if (viewModel.currentStep == 4) {
                        viewModel.completeOnboarding()
                    } else {
                        viewModel.nextStep()
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Progress indicator
            StepIndicator(
                currentStep = viewModel.currentStep,
                totalSteps = 5
            )

            // Error message
            uiState.value.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 24.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Step content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp) // Space for bottom bar
            ) {
                when (viewModel.currentStep) {
                    0 -> WelcomeStep(
                        onStart = viewModel::nextStep
                    )

                    1 -> BasicInfoStep(
                        weight = viewModel.weight,
                        height = viewModel.height,
                        age = viewModel.age,
                        gender = viewModel.gender,
                        onWeightChange = viewModel::weight::set,
                        onHeightChange = viewModel::height::set,
                        onAgeChange = viewModel::age::set,
                        onGenderChange = viewModel::gender::set,
                        bmi = viewModel.bmi,
                        isValid = viewModel.isBasicInfoValid
                    )

                    2 -> GoalsStep(
                        currentWeight = viewModel.weight,
                        targetWeight = viewModel.targetWeight,
                        timelineMonths = viewModel.timelineMonths,
                        goalType = viewModel.goalType,
                        onTargetWeightChange = viewModel::targetWeight::set,
                        onTimelineChange = viewModel::timelineMonths::set,
                        onGoalTypeChange = viewModel::goalType::set,
                        isValid = viewModel.isGoalsValid
                    )

                    3 -> ScheduleStep(
                        daysPerWeek = viewModel.daysPerWeek,
                        intensityLevel = viewModel.intensityLevel,
                        onDaysChange = viewModel::daysPerWeek::set,
                        onIntensityChange = viewModel::intensityLevel::set
                    )

                    4 -> TreadmillStep(
                        hasIncline = viewModel.hasIncline,
                        maxInclinePercent = viewModel.maxInclinePercent,
                        maxSpeedKmh = viewModel.maxSpeedKmh,
                        onHasInclineChange = viewModel::hasIncline::set,
                        onMaxInclineChange = viewModel::maxInclinePercent::set,
                        onMaxSpeedChange = viewModel::maxSpeedKmh::set
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingBottomBar(
    currentStep: Int,
    totalSteps: Int,
    canProceed: Boolean,
    isLoading: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    androidx.compose.material3.BottomAppBar(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Back button (hidden on first step)
        if (currentStep > 0) {
            TextButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("Atrás")
            }
        } else {
            Box(modifier = Modifier.weight(1f))
        }

        // Step counter
        Text(
            text = "${currentStep + 1} / $totalSteps",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        // Next/Complete button
        Button(
            onClick = onNext,
            enabled = canProceed && !isLoading,
            modifier = Modifier.weight(1f)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .align(Alignment.CenterVertically),
                    strokeWidth = 2.dp
                )
            } else {
                Text(if (currentStep == totalSteps - 1) "Finalizar" else "Siguiente")
            }
        }
    }
}