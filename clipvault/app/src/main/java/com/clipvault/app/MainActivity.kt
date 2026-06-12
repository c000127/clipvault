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
import androidx.navigation.NavDestination.Companion.hasRoute
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
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import com.clipvault.app.ui.theme.ClipVaultMotion

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var themePreferences: ThemePreferences

    @OptIn(ExperimentalSharedTransitionApi::class)
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
                
                // Helper to perform safe back navigation
                val onBackSafe: () -> Unit = {
                    val currentEntry = navController.currentBackStackEntry
                    val prevEntry = navController.previousBackStackEntry
                    
                    android.util.Log.d("Navigation", "Back requested. Current: ${currentEntry?.destination?.route}, Prev: ${prevEntry?.destination?.route}, State: ${currentEntry?.lifecycle?.currentState}")
                    
                    if (currentEntry?.lifecycle?.currentState == androidx.lifecycle.Lifecycle.State.RESUMED &&
                        prevEntry != null) {
                        navController.popBackStack()
                    } else if (prevEntry == null) {
                        android.util.Log.w("Navigation", "Prevented popping the root destination.")
                    }
                }

                // [动效] 全局页面转场规范
                // 所有前进/返回转场使用统一的 PageSlide spring + fade 组合
                // 与 ClipVaultMotion.Token 系统一致

                SharedTransitionLayout {
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Home,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable<Screen.Home>(
                            enterTransition = {
                                slideInHorizontally(
                                    initialOffsetX = { it / 3 },
                                    animationSpec = ClipVaultMotion.PageSlide
                                ) + fadeIn(animationSpec = tween(ClipVaultMotion.Standard))
                            },
                            exitTransition = {
                                slideOutHorizontally(
                                    targetOffsetX = { -it / 3 },
                                    animationSpec = ClipVaultMotion.PageSlide
                                ) + fadeOut(animationSpec = tween(ClipVaultMotion.Quick))
                            },
                            popEnterTransition = {
                                slideInHorizontally(
                                    initialOffsetX = { -it / 3 },
                                    animationSpec = ClipVaultMotion.PageSlide
                                ) + fadeIn(animationSpec = tween(ClipVaultMotion.Standard))
                            },
                            popExitTransition = {
                                slideOutHorizontally(
                                    targetOffsetX = { it / 3 },
                                    animationSpec = ClipVaultMotion.PageSlide
                                ) + fadeOut(animationSpec = tween(ClipVaultMotion.Quick))
                            }
                        ) {
                            HomeScreen(
                                sharedTransitionScope = this@SharedTransitionLayout,
                                animatedVisibilityScope = this@composable,
                                onItemClick = { id ->
                                    if (navController.currentDestination?.hasRoute<Screen.Detail>() == false) {
                                        navController.navigate(Screen.Detail(id))
                                    }
                                },
                                onNewItem = {
                                    if (navController.currentDestination?.hasRoute<Screen.New>() == false) {
                                        navController.navigate(Screen.New())
                                    }
                                },
                                onTagManager = {
                                    if (navController.currentDestination?.hasRoute<Screen.TagManager>() == false) {
                                        navController.navigate(Screen.TagManager)
                                    }
                                },
                                onSettings = {
                                    if (navController.currentDestination?.hasRoute<Screen.Settings>() == false) {
                                        navController.navigate(Screen.Settings)
                                    }
                                }
                            )
                        }

                        composable<Screen.Detail>(
                            enterTransition = {
                                slideInHorizontally(
                                    initialOffsetX = { it / 2 },
                                    animationSpec = ClipVaultMotion.PageSlide
                                ) + fadeIn(animationSpec = tween(ClipVaultMotion.Standard))
                            },
                            exitTransition = {
                                slideOutHorizontally(
                                    targetOffsetX = { -it / 3 },
                                    animationSpec = ClipVaultMotion.PageSlide
                                ) + fadeOut(animationSpec = tween(ClipVaultMotion.Quick))
                            },
                            popEnterTransition = {
                                slideInHorizontally(
                                    initialOffsetX = { -it / 3 },
                                    animationSpec = ClipVaultMotion.PageSlide
                                ) + fadeIn(animationSpec = tween(ClipVaultMotion.Standard))
                            },
                            popExitTransition = {
                                slideOutHorizontally(
                                    targetOffsetX = { it / 2 },
                                    animationSpec = ClipVaultMotion.PageSlide
                                ) + fadeOut(animationSpec = tween(ClipVaultMotion.Quick))
                            }
                        ) {
                            DetailScreen(
                                sharedTransitionScope = this@SharedTransitionLayout,
                                animatedVisibilityScope = this@composable,
                                onBack = onBackSafe
                            )
                        }

                        composable<Screen.New>(
                            enterTransition = {
                                slideInHorizontally(
                                    initialOffsetX = { it / 2 },
                                    animationSpec = ClipVaultMotion.PageSlide
                                ) + fadeIn(animationSpec = tween(ClipVaultMotion.Standard))
                            },
                            exitTransition = {
                                slideOutHorizontally(
                                    targetOffsetX = { -it / 3 },
                                    animationSpec = ClipVaultMotion.PageSlide
                                ) + fadeOut(animationSpec = tween(ClipVaultMotion.Quick))
                            },
                            popEnterTransition = {
                                slideInHorizontally(
                                    initialOffsetX = { -it / 3 },
                                    animationSpec = ClipVaultMotion.PageSlide
                                ) + fadeIn(animationSpec = tween(ClipVaultMotion.Standard))
                            },
                            popExitTransition = {
                                slideOutHorizontally(
                                    targetOffsetX = { it / 2 },
                                    animationSpec = ClipVaultMotion.PageSlide
                                ) + fadeOut(animationSpec = tween(ClipVaultMotion.Quick))
                            }
                        ) { backStackEntry ->
                            val route = backStackEntry.toRoute<Screen.New>()
                            NewItemScreen(
                                sharedTransitionScope = this@SharedTransitionLayout,
                                animatedVisibilityScope = this@composable,
                                onBack = onBackSafe,
                                initialText = route.text
                            )
                        }

                        composable<Screen.TagManager>(
                            enterTransition = {
                                fadeIn(animationSpec = tween(ClipVaultMotion.Standard))
                            },
                            exitTransition = {
                                fadeOut(animationSpec = tween(ClipVaultMotion.Quick))
                            },
                            popEnterTransition = {
                                fadeIn(animationSpec = tween(ClipVaultMotion.Standard))
                            },
                            popExitTransition = {
                                fadeOut(animationSpec = tween(ClipVaultMotion.Quick))
                            }
                        ) {
                            TagManagerScreen(
                                sharedTransitionScope = this@SharedTransitionLayout,
                                animatedVisibilityScope = this@composable,
                                onBack = onBackSafe
                            )
                        }

                        composable<Screen.Settings>(
                            enterTransition = {
                                fadeIn(animationSpec = tween(ClipVaultMotion.Standard))
                            },
                            exitTransition = {
                                fadeOut(animationSpec = tween(ClipVaultMotion.Quick))
                            },
                            popEnterTransition = {
                                fadeIn(animationSpec = tween(ClipVaultMotion.Standard))
                            },
                            popExitTransition = {
                                fadeOut(animationSpec = tween(ClipVaultMotion.Quick))
                            }
                        ) {
                            SettingsScreen(
                                sharedTransitionScope = this@SharedTransitionLayout,
                                animatedVisibilityScope = this@composable,
                                onBack = onBackSafe,
                                onAiSettings = { 
                                    if (navController.currentDestination?.hasRoute<Screen.AiSettings>() == false) {
                                        navController.navigate(Screen.AiSettings)
                                    }
                                }
                            )
                        }

                        composable<Screen.AiSettings>(
                            enterTransition = {
                                fadeIn(animationSpec = tween(ClipVaultMotion.Standard))
                            },
                            exitTransition = {
                                fadeOut(animationSpec = tween(ClipVaultMotion.Quick))
                            },
                            popEnterTransition = {
                                fadeIn(animationSpec = tween(ClipVaultMotion.Standard))
                            },
                            popExitTransition = {
                                fadeOut(animationSpec = tween(ClipVaultMotion.Quick))
                            }
                        ) {
                            AiSettingsScreen(
                                sharedTransitionScope = this@SharedTransitionLayout,
                                animatedVisibilityScope = this@composable,
                                onBack = onBackSafe
                            )
                        }
                    }
                }
            }
        }
    }
}
