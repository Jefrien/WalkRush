package dev.jefrien.walkrush

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import dev.jefrien.walkrush.domain.repository.AuthRepository
import dev.jefrien.walkrush.presentation.navigation.NavigationEvent
import dev.jefrien.walkrush.presentation.navigation.Route
import dev.jefrien.walkrush.presentation.navigation.WalkRushNavHost
import dev.jefrien.walkrush.ui.theme.WalkRushTheme
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val authRepository: AuthRepository by inject()

    // Shared flow for navigation events from deep links
    private val _navigationEvents = MutableSharedFlow<NavigationEvent>()
    private val navigationEvents = _navigationEvents.asSharedFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val startDestination = determineStartDestination()

        setContent {
            WalkRushTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WalkRushNavHost(
                        startDestination = startDestination,
                        navigationEvents = navigationEvents
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle deep links (OAuth callbacks)
        handleDeepLink(intent)
    }

    private fun determineStartDestination(): String {
        // Check if user is authenticated
        val isAuthenticated = authRepository.currentUserId() != null

        return if (isAuthenticated) {
            // TODO: Check if onboarding completed in DataStore
            Route.Home.path
        } else {
            Route.Auth.path
        }
    }

    private fun handleDeepLink(intent: Intent) {
        val data = intent.data ?: return
        val url = data.toString()

        if (url.startsWith("walkrush://callback")) {
            lifecycleScope.launch {
                authRepository.handleDeepLink(url)
                _navigationEvents.emit(
                    NavigationEvent.Navigate(
                        Route.Home
                    )
                )
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    WalkRushTheme {
        Greeting("Android")
    }
}