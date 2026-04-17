package dev.jefrien.walkrush.presentation.navigation

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import dev.jefrien.walkrush.presentation.activeworkout.ActiveWorkoutScreen
import dev.jefrien.walkrush.presentation.auth.AuthScreen
import dev.jefrien.walkrush.presentation.history.HistoryScreen
import dev.jefrien.walkrush.presentation.home.HomeScreen
import dev.jefrien.walkrush.presentation.onboarding.OnboardingScreen
import dev.jefrien.walkrush.presentation.postworkout.PostWorkoutScreen
import dev.jefrien.walkrush.presentation.profile.ProfileScreen
import kotlinx.coroutines.flow.SharedFlow

/**
 * Main navigation host for WalkRush app
 */
@Composable
fun WalkRushNavHost(
    startDestination: String = Route.Auth.path,
    navigationEvents: SharedFlow<NavigationEvent>? = null,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    // Handle external navigation events
    LaunchedEffect(navigationEvents) {
        navigationEvents?.collect { event ->
            when (event) {
                is NavigationEvent.Navigate -> {
                    navController.navigate(event.route.path) {
                        // Clear back stack for auth -> home transition
                        if (event.route == Route.Home && navController.currentDestination?.route == Route.Onboarding.path) {
                            popUpTo(Route.Auth.path) { inclusive = true }
                        }
                    }
                }
                is NavigationEvent.NavigateWithArgs -> {
                    navController.navigate(event.route)
                }
                NavigationEvent.NavigateBack -> {
                    navController.popBackStack()
                }
                NavigationEvent.ClearBackStack -> {
                    navController.popBackStack(0, false)
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Auth Screen
        composable(
            route = Route.Auth.path,
            deepLinks = listOf(
                navDeepLink { uriPattern = "walkrush://callback" }
            )
        ) {
            AuthScreen(
                onNavigateToHome = {
                    navController.navigate(Route.Home.path) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToOnboarding = {
                    navController.navigate(Route.Onboarding.path) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )

        }

        // Onboarding
        composable(route = Route.Onboarding.path) {
            OnboardingScreen(
                onOnboardingComplete = {
                    navController.navigate(Route.Home.path) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Home Screen
        composable(route = Route.Home.path) {
            HomeScreen(
                onNavigateToProfile = {
                    navController.navigate(Route.Profile.path)
                },
                onNavigateToHistory = {
                    navController.navigate(Route.History.path)
                },
                onStartWorkout = { sessionId ->
                    navController.navigate(Route.ActiveWorkout.createRoute(sessionId))
                },
                onSignOut = {
                    navController.navigate(Route.Auth.path) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Profile Screen
        composable(route = Route.Profile.path) {
            ProfileScreen(
                onNavigateBack = { navController.popBackStack() },
                onSignOut = {
                    navController.navigate(Route.Auth.path) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // History Screen
        composable(route = Route.History.path) {
            HistoryScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Active Workout Screen
        composable(
            route = Route.ActiveWorkout.path,
            arguments = listOf(
                navArgument("sessionId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            ActiveWorkoutScreen(
                sessionId = sessionId,
                onWorkoutComplete = {
                    navController.navigate(Route.PostWorkout.createRoute(sessionId)) {
                        popUpTo(Route.Home.path) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onCancel = { navController.popBackStack() }
            )
        }

        // Post Workout Screen
        composable(
            route = Route.PostWorkout.path,
            arguments = listOf(
                navArgument("sessionId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            PostWorkoutScreen(
                sessionId = sessionId,
                onNavigateHome = {
                    navController.navigate(Route.Home.path) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}

