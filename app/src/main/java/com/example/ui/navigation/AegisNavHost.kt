package com.example.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.di.AegisContainer
import com.example.ui.about.AboutScreen
import com.example.ui.auth.GuardianGateScreen
import com.example.ui.blocking.BlockOverlayScreen
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.history.SecurityHistoryScreen
import com.example.ui.onboarding.OnboardingScreen
import com.example.ui.protection.LiveProtectionScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.splash.SplashScreen
import com.example.ui.theme.AegisCardBackground
import com.example.ui.theme.AegisCardBorder
import com.example.ui.theme.AegisChampagneGold
import com.example.ui.theme.AegisPrimaryCyan
import com.example.ui.theme.AegisTextMuted
import com.example.ui.theme.AegisTextPrimary
import com.example.ui.theme.ClassicalBodyFont

private data class MainTabItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val MainTabItems = listOf(
    MainTabItem(
        route = AegisDestinations.HOME,
        label = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    ),
    MainTabItem(
        route = AegisDestinations.EDITS,
        label = "Edits",
        selectedIcon = Icons.Filled.Tune,
        unselectedIcon = Icons.Outlined.Tune
    ),
    MainTabItem(
        route = AegisDestinations.STATUS,
        label = "Status",
        selectedIcon = Icons.Filled.MonitorHeart,
        unselectedIcon = Icons.Outlined.MonitorHeart
    ),
    MainTabItem(
        route = AegisDestinations.PROGRESS,
        label = "Progress",
        selectedIcon = Icons.Filled.Assessment,
        unselectedIcon = Icons.Outlined.Assessment
    ),
    MainTabItem(
        route = AegisDestinations.ABOUT,
        label = "About",
        selectedIcon = Icons.Filled.Info,
        unselectedIcon = Icons.Outlined.Info
    )
)

internal fun isStartupGuardianAuthorized(
    hasGuardianPin: Boolean,
    isSessionUnlocked: Boolean
): Boolean = hasGuardianPin && isSessionUnlocked

@Composable
fun AegisNavHost(
    container: AegisContainer,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    onRequestStartProtection: () -> Unit = {},
    onRequestStopProtection: () -> Unit = {}
) {
    val isOnboardingDone by container.preferences.isOnboardingCompleted.collectAsState()
    val hasGuardianPin by container.preferences.hasGuardianPin.collectAsState()
    val isGuardianUnlocked by container.preferences.isGuardianSessionUnlocked.collectAsState()

    val isGuardianAuthorized = isStartupGuardianAuthorized(
        hasGuardianPin = hasGuardianPin,
        isSessionUnlocked = isGuardianUnlocked
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showMainNavigation = isGuardianAuthorized && MainTabItems.any { it.route == currentRoute }

    val navigateToTab: (String) -> Unit = { targetRoute ->
        if (currentRoute != targetRoute) {
            navController.navigate(targetRoute) {
                popUpTo(AegisDestinations.HOME) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val useNavigationRail = showMainNavigation && maxWidth >= 720.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (useNavigationRail) {
                NavigationRail(
                    containerColor = com.example.ui.theme.AegisLavenderSoft,
                    contentColor = AegisTextPrimary,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    MainTabItems.forEach { tab ->
                        val selected = currentRoute == tab.route
                        NavigationRailItem(
                            selected = selected,
                            onClick = { navigateToTab(tab.route) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.label
                                )
                            },
                            label = {
                                Text(
                                    text = tab.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    fontFamily = ClassicalBodyFont,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = AegisPrimaryCyan,
                                selectedTextColor = AegisPrimaryCyan,
                                indicatorColor = com.example.ui.theme.AegisPurple.copy(alpha = 0.18f),
                                unselectedIconColor = AegisTextMuted,
                                unselectedTextColor = AegisTextMuted
                            ),
                            modifier = Modifier.testTag("nav_tab_${tab.route}")
                        )
                    }
                }
                VerticalDivider(color = AegisCardBorder)
            }

            Scaffold(
                modifier = Modifier.weight(1f),
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    if (showMainNavigation && !useNavigationRail) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                        colors = listOf(
                                            com.example.ui.theme.AegisSecondaryBackground,
                                            com.example.ui.theme.AegisLavenderSoft
                                        )
                                    )
                                )
                        ) {
                            HorizontalDivider(
                                thickness = 1.5.dp,
                                color = AegisChampagneGold.copy(alpha = 0.65f)
                            )
                            NavigationBar(
                                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                                contentColor = AegisTextPrimary,
                                windowInsets = NavigationBarDefaults.windowInsets,
                                tonalElevation = 0.dp
                            ) {
                                MainTabItems.forEach { tab ->
                                    val selected = currentRoute == tab.route
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { navigateToTab(tab.route) },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = tab.label
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = tab.label,
                                                fontSize = 11.sp,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                fontFamily = ClassicalBodyFont,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = AegisPrimaryCyan,
                                            selectedTextColor = AegisPrimaryCyan,
                                            indicatorColor = com.example.ui.theme.AegisPurple.copy(alpha = 0.18f),
                                            unselectedIconColor = AegisTextMuted,
                                            unselectedTextColor = AegisTextMuted
                                        ),
                                        modifier = Modifier.testTag("nav_tab_${tab.route}")
                                    )
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = AegisDestinations.SPLASH,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            bottom = if (showMainNavigation && !useNavigationRail) {
                                innerPadding.calculateBottomPadding()
                            } else {
                                0.dp
                            }
                        ),
                    enterTransition = { fadeIn(animationSpec = tween(220)) },
                    exitTransition = { fadeOut(animationSpec = tween(220)) },
                    popEnterTransition = { fadeIn(animationSpec = tween(220)) },
                    popExitTransition = { fadeOut(animationSpec = tween(220)) }
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
                                navController.navigate(AegisDestinations.HOME) {
                                    popUpTo(AegisDestinations.ONBOARDING) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(AegisDestinations.HOME) {
                        if (!isGuardianAuthorized) {
                            GuardianGateScreen(
                                preferences = container.preferences,
                                protectionRepo = container.protectionRepository,
                                onUnlocked = { }
                            )
                        } else {
                            DashboardScreen(
                                protectionRepo = container.protectionRepository,
                                modelManager = container.modelManager,
                                preferences = container.preferences,
                                onModelUpdated = { container.refreshModelTelemetry() },
                                onRequestStartProtection = onRequestStartProtection,
                                onRequestStopProtection = onRequestStopProtection
                            )
                        }
                    }

                    composable(AegisDestinations.EDITS) {
                        if (!isGuardianAuthorized) {
                            GuardianGateScreen(
                                preferences = container.preferences,
                                protectionRepo = container.protectionRepository,
                                onUnlocked = { }
                            )
                        } else {
                            SettingsScreen(
                                preferences = container.preferences,
                                modelManager = container.modelManager,
                                protectionRepo = container.protectionRepository,
                                onModelUpdated = { container.refreshModelTelemetry() }
                            )
                        }
                    }

                    composable(AegisDestinations.STATUS) {
                        if (!isGuardianAuthorized) {
                            GuardianGateScreen(
                                preferences = container.preferences,
                                protectionRepo = container.protectionRepository,
                                onUnlocked = { }
                            )
                        } else {
                            LiveProtectionScreen(
                                protectionRepo = container.protectionRepository,
                                preferences = container.preferences,
                                onRequestArmShield = onRequestStartProtection
                            )
                        }
                    }

                    composable(AegisDestinations.PROGRESS) {
                        if (!isGuardianAuthorized) {
                            GuardianGateScreen(
                                preferences = container.preferences,
                                protectionRepo = container.protectionRepository,
                                onUnlocked = { }
                            )
                        } else {
                            SecurityHistoryScreen(
                                historyRepo = container.securityHistoryRepository,
                                preferences = container.preferences
                            )
                        }
                    }

                    composable(AegisDestinations.ABOUT) {
                        if (!isGuardianAuthorized) {
                            GuardianGateScreen(
                                preferences = container.preferences,
                                protectionRepo = container.protectionRepository,
                                onUnlocked = { }
                            )
                        } else {
                            AboutScreen(
                                preferences = container.preferences
                            )
                        }
                    }

                    composable(AegisDestinations.BLOCK_SCREEN) {
                        BlockOverlayScreen(
                            preferences = container.preferences,
                            onDismiss = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
