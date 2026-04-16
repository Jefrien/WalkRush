package dev.jefrien.walkrush.di

import dev.jefrien.walkrush.domain.usecase.auth.SignInUseCase
import dev.jefrien.walkrush.domain.usecase.auth.SignOutUseCase
import dev.jefrien.walkrush.domain.usecase.auth.SignUpUseCase
import org.koin.dsl.module

val DomainModule = module {
    factory { SignInUseCase(get()) }
    factory { SignUpUseCase(get()) }
    factory { SignOutUseCase(get()) }
}