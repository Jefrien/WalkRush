package dev.jefrien.walkrush.presentation.navigation

/**
 * Sealed class for type-safe navigation routes
 */
sealed class Route(val path: String) {

    // Auth
    object Auth : Route("auth")

    // Onboarding (shown once after first login)
    object Onboarding : Route("onboarding")

    // Main screens
    object Home : Route("home")
    object Profile : Route("profile")
    object History : Route("history")

    // Workout
    object ActiveWorkout : Route("active_workout/{sessionId}") {
        fun createRoute(sessionId: String) = "active_workout/$sessionId"
    }

    // Deep links
    object AuthCallback : Route("callback")

    companion object {
        fun fromString(route: String): Route {
            return when {
                route == Auth.path -> Auth
                route == Onboarding.path -> Onboarding
                route == Home.path -> Home
                route == Profile.path -> Profile
                route == History.path -> History
                route.startsWith("active_workout/") -> ActiveWorkout
                else -> Auth
            }
        }
    }
}