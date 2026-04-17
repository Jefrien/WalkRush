package dev.jefrien.walkrush.di

import dev.jefrien.walkrush.presentation.activeworkout.ActiveWorkoutViewModel
import dev.jefrien.walkrush.presentation.auth.AuthViewModel
import dev.jefrien.walkrush.presentation.history.HistoryViewModel
import dev.jefrien.walkrush.presentation.home.HomeViewModel
import dev.jefrien.walkrush.presentation.onboarding.OnboardingViewModel
import dev.jefrien.walkrush.presentation.postworkout.PostWorkoutViewModel
import dev.jefrien.walkrush.presentation.profile.ProfileViewModel
import dev.jefrien.walkrush.presentation.calendar.CalendarViewModel
import dev.jefrien.walkrush.presentation.settings.SettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val ViewModelModule = module {
    viewModel { AuthViewModel(get(), get(), get(), get()) }
    viewModel { OnboardingViewModel(get(), get()) }
    viewModel { HomeViewModel(get(), get(), get(), get()) }
    viewModel { ActiveWorkoutViewModel(get(), get()) }
    viewModel { HistoryViewModel(get(), get()) }
    viewModel { ProfileViewModel(get(), get()) }
    viewModel { PostWorkoutViewModel(get(), get()) }
    viewModel { SettingsViewModel(get(), get(), get()) }
    viewModel { CalendarViewModel(get(), get(), get()) }
}
