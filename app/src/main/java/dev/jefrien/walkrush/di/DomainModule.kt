package dev.jefrien.walkrush.di

import dev.jefrien.walkrush.domain.usecase.auth.SignInUseCase
import dev.jefrien.walkrush.domain.usecase.auth.SignOutUseCase
import dev.jefrien.walkrush.domain.usecase.auth.SignUpUseCase
import dev.jefrien.walkrush.domain.usecase.userprofile.GetUserProfileUseCase
import dev.jefrien.walkrush.domain.usecase.userprofile.SaveUserProfileUseCase
import org.koin.dsl.module

val DomainModule = module {
    // Auth
    factory { SignInUseCase(get()) }
    factory { SignUpUseCase(get()) }
    factory { SignOutUseCase(get()) }

    // User Profile
    factory { SaveUserProfileUseCase(get(), get()) }
    factory { GetUserProfileUseCase(get(), get()) }
}