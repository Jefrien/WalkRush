package dev.jefrien.walkrush.di

import dev.jefrien.walkrush.data.remote.supabase.SupabaseAuthRepository
import dev.jefrien.walkrush.data.remote.supabase.SupabaseUserProfileRepository
import dev.jefrien.walkrush.domain.repository.AuthRepository
import dev.jefrien.walkrush.domain.repository.UserProfileRepository
import org.koin.dsl.module

val DataModule = module {
    single<AuthRepository> { SupabaseAuthRepository(get()) }
    single<UserProfileRepository> { SupabaseUserProfileRepository(get()) }
}