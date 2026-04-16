package dev.jefrien.walkrush.di

import dev.jefrien.walkrush.presentation.activeworkout.ActiveWorkoutViewModel
import dev.jefrien.walkrush.presentation.auth.AuthViewModel
import dev.jefrien.walkrush.presentation.history.HistoryViewModel
import dev.jefrien.walkrush.presentation.home.HomeViewModel
import dev.jefrien.walkrush.presentation.onboarding.OnboardingViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val ViewModelModule = module {
    viewModel { AuthViewModel(get(), get(), get(), get()) }
    viewModel { OnboardingViewModel(get(), get()) }
    viewModel { HomeViewModel(get(), get(), get()) }
    viewModel { ActiveWorkoutViewModel(get()) }
    viewModel { HistoryViewModel(get(), get()) }
}