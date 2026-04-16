package dev.jefrien.walkrush

import android.app.Application
import dev.jefrien.walkrush.di.AppModule
import dev.jefrien.walkrush.di.DataModule
import dev.jefrien.walkrush.di.DomainModule
import dev.jefrien.walkrush.di.NetworkModule
import dev.jefrien.walkrush.di.ViewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.logger.Level

class WalkRushApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.DEBUG else Level.NONE)
            androidContext(this@WalkRushApplication)

            modules(
                listOf(
                    NetworkModule,      // Supabase, Ktor clients
                    DataModule,         // Repositories, DataStore, Room
                    DomainModule,       // UseCases
                    ViewModelModule,    // ViewModels
                    AppModule           // App-level dependencies
                )
            )
        }
    }
}