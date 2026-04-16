package dev.jefrien.walkrush.di

import dev.jefrien.walkrush.presentation.auth.AuthViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.koin.plugin.module.dsl.viewModel

val ViewModelModule = module {
    viewModel { AuthViewModel(get(), get(), get()) }
}