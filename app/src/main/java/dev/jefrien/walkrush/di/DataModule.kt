package dev.jefrien.walkrush.di

import dev.jefrien.walkrush.data.healthconnect.HealthConnectRepositoryWrapper
import dev.jefrien.walkrush.data.manager.HealthDataManager
import dev.jefrien.walkrush.data.remote.supabase.SupabaseAuthRepository
import dev.jefrien.walkrush.data.remote.supabase.SupabaseRoutineRepository
import dev.jefrien.walkrush.data.remote.supabase.SupabaseUserProfileRepository
import dev.jefrien.walkrush.domain.repository.AuthRepository
import dev.jefrien.walkrush.domain.repository.HealthConnectRepository
import dev.jefrien.walkrush.domain.repository.RoutineRepository
import dev.jefrien.walkrush.domain.repository.UserProfileRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val DataModule = module {
    single { HealthDataManager(androidContext()) }
    single<HealthConnectRepository> { HealthConnectRepositoryWrapper(get()) }
    single<AuthRepository> { SupabaseAuthRepository(get()) }
    single<UserProfileRepository> { SupabaseUserProfileRepository(get()) }
    single<RoutineRepository> { SupabaseRoutineRepository(get(), get()) }
}
