package dev.jefrien.walkrush.di

import dev.jefrien.walkrush.BuildConfig
import dev.jefrien.walkrush.data.local.SupabaseSessionManager
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Network module: Supabase client and Ktor HTTP client
 */
val NetworkModule = module {

    // JSON configuration for serialization
    single {
        Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            encodeDefaults = true
            isLenient = true
        }
    }

    // Ktor HttpClient for Supabase
    single {
        HttpClient(Android) {
            install(ContentNegotiation) {
                json(get())
            }

            defaultRequest {
                contentType(ContentType.Application.Json)
            }

            engine {
                connectTimeout = 30_000
                socketTimeout = 30_000
            }
        }
    }

    single<SessionManager> {
        SupabaseSessionManager(androidContext(), get())
    }

    // Supabase Client
    single<SupabaseClient> {
        val sessionManager: SessionManager = get()
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            // HTTP client from Koin
            val httpClient = get<HttpClient>()

            // Install modules
            install(Postgrest)
            install(Auth) {
                scheme = "walkrush"
                host = "callback"
                flowType = FlowType.PKCE

                this.sessionManager = sessionManager
            }
            install(Functions)
            install(Realtime)
        }
    }
}