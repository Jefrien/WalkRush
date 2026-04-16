package dev.jefrien.walkrush.di

import dev.jefrien.walkrush.data.remote.supabase.SupabaseAuthRepository
import dev.jefrien.walkrush.domain.repository.AuthRepository
import org.koin.dsl.module

val DataModule = module {
    single<AuthRepository> { SupabaseAuthRepository(get()) }
}