package dev.jefrien.walkrush.data.local

import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.serialization.json.Json

class SupabaseSessionManager(context: Context, private val json: Json) : SessionManager {

    private val settings: Settings = SharedPreferencesSettings(
        context.getSharedPreferences("supabase_auth", Context.MODE_PRIVATE)
    )

    override suspend fun saveSession(session: UserSession) {
        settings.putString("session", json.encodeToString(session))
    }

    override suspend fun deleteSession() {
        settings.remove("session")
    }

    override suspend fun loadSession(): UserSession? {
        val sessionString = settings.getStringOrNull("session") ?: return null
        return json.decodeFromString(sessionString)
    }
}