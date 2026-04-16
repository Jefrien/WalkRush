package dev.jefrien.walkrush.di

import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val AppModule = module {
    // Coroutine Dispatchers
    single { Dispatchers.IO }
    single { Dispatchers.Default }
    single { Dispatchers.Main }
    // App-wide utilities
    factory { androidContext().resources }
}