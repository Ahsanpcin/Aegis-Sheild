package com.example.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.di.AegisContainer
import com.example.ui.blocking.BlockOverlayScreen
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.history.SecurityHistoryScreen
import com.example.ui.onboarding.OnboardingScreen
import com.example.ui.protection.LiveProtectionScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.splash.SplashScreen

@Composable
fun AegisNavHost(
    container: AegisContainer,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    onRequestStartProtection: () -> Unit = {},
    onRequestStopProtection: () -> Unit = {}
) {
    val isOnboardingDone by container.preferences.isOnboardingCompleted.collectAsState()

    NavHost(
        navController = navController,
        startDestination = AegisDestinations.SPLASH,
        modifier = modifier,
        enterTransition = { fadeIn(animationSpec = tween(300)) },
        exitTransition = { fadeOut(animationSpec = tween(300)) },
        popEnterTransition = { fadeIn(animationSpec = tween(300)) },
        popExitTransition = { fadeOut(animationSpec = tween(300)) }
    ) {
        composable(AegisDestinations.SPLASH) {
            SplashScreen(
                isOnboardingCompleted = isOnboardingDone,
                onNavigateNext = { dest ->
                    navController.navigate(dest) {
                        popUpTo(AegisDestinations.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(AegisDestinations.ONBOARDING) {
            OnboardingScreen(
                onComplete = {
                    container.preferences.setOnboardingCompleted(true)
                    navController.navigate(AegisDestinations.DASHBOARD) {
                        popUpTo(AegisDestinations.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        composable(AegisDestinations.DASHBOARD) {
            DashboardScreen(
                protectionRepo = container.protectionRepository,
                historyRepo = container.securityHistoryRepository,
                modelManager = container.modelManager,
                onNavigateToLive = { navController.navigate(AegisDestinations.LIVE_PROTECTION) },
                onNavigateToHistory = { navController.navigate(AegisDestinations.SECURITY_HISTORY) },
                onNavigateToSettings = { navController.navigate(AegisDestinations.SETTINGS) },
                onNavigateToBlockScreen = { navController.navigate(AegisDestinations.BLOCK_SCREEN) },
                onModelUpdated = { container.refreshModelTelemetry() },
                onRequestStartProtection = onRequestStartProtection,
                onRequestStopProtection = onRequestStopProtection
            )
        }

        composable(AegisDestinations.LIVE_PROTECTION) {
            LiveProtectionScreen(
                protectionRepo = container.protectionRepository,
                historyRepo = container.securityHistoryRepository,
                modelManager = container.modelManager,
                preferences = container.preferences,
                onNavigateBack = { navController.popBackStack() },
                onTriggerBlockOverlay = { navController.navigate(AegisDestinations.BLOCK_SCREEN) },
                onModelUpdated = { container.refreshModelTelemetry() }
            )
        }

        composable(AegisDestinations.SECURITY_HISTORY) {
            SecurityHistoryScreen(
                historyRepo = container.securityHistoryRepository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(AegisDestinations.SETTINGS) {
            SettingsScreen(
                preferences = container.preferences,
                modelManager = container.modelManager,
                protectionRepo = container.protectionRepository,
                onNavigateBack = { navController.popBackStack() },
                onModelUpdated = { container.refreshModelTelemetry() }
            )
        }

        composable(AegisDestinations.BLOCK_SCREEN) {
            BlockOverlayScreen(
                preferences = container.preferences,
                onDismiss = { navController.popBackStack() }
            )
        }
    }
}
