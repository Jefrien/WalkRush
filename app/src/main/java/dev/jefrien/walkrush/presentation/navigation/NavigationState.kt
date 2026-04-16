package dev.jefrien.walkrush.presentation.navigation

/**
 * Navigation state holder for UI events
 */
sealed class NavigationEvent {
    data class Navigate(val route: Route) : NavigationEvent()
    data class NavigateWithArgs(val route: String) : NavigationEvent()
    object NavigateBack : NavigationEvent()
    object ClearBackStack : NavigationEvent()
}