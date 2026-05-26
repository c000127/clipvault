package com.clipvault.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.clipvault.app.ui.aisettings.AiSettingsScreen
import com.clipvault.app.ui.detail.DetailScreen
import com.clipvault.app.ui.home.HomeScreen
import com.clipvault.app.ui.navigation.Screen
import com.clipvault.app.ui.newitem.NewItemScreen
import com.clipvault.app.ui.settings.SettingsScreen
import com.clipvault.app.ui.tagmanager.TagManagerScreen
import com.clipvault.app.ui.theme.ClipVaultTheme
import dagger.hilt.android.AndroidEntryPoint

import javax.inject.Inject
import com.clipvault.app.ui.theme.ThemePreferences
import com.clipvault.app.ui.theme.ThemeMode
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import com.clipvault.app.ui.theme.ClipVaultMotion

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var themePreferences: ThemePreferences

    @OptIn(ExperimentalSharedTransitionApi::class, androidx.compose.animation.ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val themeMode by themePreferences.themeMode.collectAsState(initial = ThemeMode.FOLLOW_SYSTEM)
            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.FOLLOW_SYSTEM -> isSystemInDarkTheme()
            }
            ClipVaultTheme(darkTheme = darkTheme) {
                val navController = rememberNavController()

                SharedTransitionLayout {
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Home,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable<Screen.Home>(
                            enterTransition = { fadeIn(androidx.compose.animation.core.tween(ClipVaultMotion.ShortDuration)) },
                            exitTransition = { fadeOut(androidx.compose.animation.core.tween(ClipVaultMotion.ShortDuration)) }
                        ) {
                            HomeScreen(
                                sharedTransitionScope = this@SharedTransitionLayout,
                                animatedVisibilityScope = this@composable,
                                onItemClick = { id ->
                                    navController.navigate(Screen.Detail(id))
                                },
                                onNewItem = {
                                    navController.navigate(Screen.New())
                                },
                                onTagManager = {
                                    navController.navigate(Screen.TagManager)
                                },
                                onSettings = {
                                    navController.navigate(Screen.Settings)
                                }
                            )
                        }

                        composable<Screen.Detail>(
                            enterTransition = { fadeIn(androidx.compose.animation.core.tween(ClipVaultMotion.MediumDuration)) },
                            exitTransition = { fadeOut(androidx.compose.animation.core.tween(ClipVaultMotion.ShortDuration)) }
                        ) {
                            DetailScreen(
                                sharedTransitionScope = this@SharedTransitionLayout,
                                animatedVisibilityScope = this@composable,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable<Screen.New>(
                            enterTransition = { fadeIn(androidx.compose.animation.core.tween(ClipVaultMotion.ShortDuration)) },
                            exitTransition = { fadeOut(androidx.compose.animation.core.tween(ClipVaultMotion.ShortDuration)) }
                        ) { backStackEntry ->
                            val route = backStackEntry.toRoute<Screen.New>()
                            NewItemScreen(
                                onBack = { navController.popBackStack() },
                                initialText = route.text
                            )
                        }

                        composable<Screen.TagManager> {
                            TagManagerScreen(onBack = { navController.popBackStack() })
                        }

                        composable<Screen.Settings> {
                            SettingsScreen(
                                onBack = { navController.popBackStack() },
                                onAiSettings = { navController.navigate(Screen.AiSettings) }
                            )
                        }

                        composable<Screen.AiSettings> {
                            AiSettingsScreen(onBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}
