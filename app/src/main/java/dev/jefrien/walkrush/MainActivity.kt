package dev.jefrien.walkrush

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import dev.jefrien.walkrush.domain.repository.AuthRepository
import dev.jefrien.walkrush.domain.repository.UserProfileRepository
import dev.jefrien.walkrush.presentation.navigation.Route
import dev.jefrien.walkrush.presentation.navigation.WalkRushNavHost
import dev.jefrien.walkrush.ui.theme.WalkRushTheme
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val authRepository: AuthRepository by inject()
    private val userProfileRepository: UserProfileRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            WalkRushTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Estado para determinar ruta inicial
                    var startDestination by remember { mutableStateOf<String?>(null) }
                    var isLoading by remember { mutableStateOf(true) }

                    LaunchedEffect(Unit) {
                        startDestination = determineStartDestination()
                        isLoading = false
                    }

                    if (!isLoading && startDestination != null) {
                        WalkRushNavHost(
                            startDestination = startDestination!!
                        )
                    }
                }
            }
        }
    }

    private suspend fun determineStartDestination(): String {
        // 1. Esperar a que Supabase cargue la sesión desde almacenamiento
        authRepository.awaitSessionInitialization()

        // 2. ¿Está autenticado?
        val isAuthenticated = authRepository.isAuthenticated()

        if (!isAuthenticated) {
            return Route.Auth.path
        }

        // 2. ¿Tiene perfil completo?
        val userId = authRepository.currentUserId() ?: return Route.Auth.path
        val hasProfile = userProfileRepository.hasCompletedOnboarding(userId)

        return if (hasProfile) {
            Route.Home.path
        } else {
            Route.Onboarding.path
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle OAuth deep links
        val data = intent.data ?: return
        if (data.toString().startsWith("walkrush://callback")) {
            lifecycleScope.launch {
                authRepository.handleDeepLink(data.toString())
            }
        }
    }
}