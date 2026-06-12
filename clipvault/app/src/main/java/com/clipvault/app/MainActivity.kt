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
                // 所有前进/返回转场使用纯 spring 方向滑动，不加 fade
                // sharedElement() 处理内容过渡，页面只需方向位移

                SharedTransitionLayout {
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Home,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable<Screen.Home>(
                            enterTransition = {
                                fadeIn(animationSpec = ClipVaultMotion.NonSpatialExpressiveSpring)
                            },
                            exitTransition = {
                                fadeOut(animationSpec = ClipVaultMotion.NonSpatialExpressiveSpring)
                            },
                            popEnterTransition = {
                                fadeIn(animationSpec = ClipVaultMotion.NonSpatialExpressiveSpring)
                            },
                            popExitTransition = {
                                fadeOut(animationSpec = ClipVaultMotion.NonSpatialExpressiveSpring)
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
                                fadeIn(animationSpec = ClipVaultMotion.NonSpatialExpressiveSpring)
                            },
                            exitTransition = {
                                fadeOut(animationSpec = ClipVaultMotion.NonSpatialExpressiveSpring)
                            },
                            popEnterTransition = {
                                fadeIn(animationSpec = ClipVaultMotion.NonSpatialExpressiveSpring)
                            },
                            popExitTransition = {
                                fadeOut(animationSpec = ClipVaultMotion.NonSpatialExpressiveSpring)
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
                                fadeIn(animationSpec = ClipVaultMotion.NonSpatialExpressiveSpring)
                            },
                            exitTransition = {
                                fadeOut(animationSpec = ClipVaultMotion.NonSpatialExpressiveSpring)
                            },
                            popEnterTransition = {
                                fadeIn(animationSpec = ClipVaultMotion.NonSpatialExpressiveSpring)
                            },
                            popExitTransition = {
                                fadeOut(animationSpec = ClipVaultMotion.NonSpatialExpressiveSpring)
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
                                slideInHorizontally(
                                    initialOffsetX = { it / 3 },
                                    animationSpec = ClipVaultMotion.PageSlide
                                )
                            },
                            exitTransition = {
                                slideOutHorizontally(
                                    targetOffsetX = { -it / 3 },
                                    animationSpec = ClipVaultMotion.PageSlide
                                )
                            },
                            popEnterTransition = {
                                slideInHorizontally(
                                    initialOffsetX = { -it / 3 },
                                    animationSpec = ClipVaultMotion.PageSlide
                                )
                            },
                            popExitTransition = {
                                slideOutHorizontally(
                                    targetOffsetX = { it / 3 },
                                    animationSpec = ClipVaultMotion.PageSlide
                                )
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
                                slideInHorizontally(
                                    initialOffsetX = { it / 3 },
                                    animationSpec = ClipVaultMotion.PageSlide
                                )
                            },
                            exitTransition = {
                                slideOutHorizontally(
                                    targetOffsetX = { -it / 3 },
                                    animationSpec = ClipVaultMotion.PageSlide
                                )
                            },
                            popEnterTransition = {
                                slideInHorizontally(
                                    initialOffsetX = { -it / 3 },
                                    animationSpec = ClipVaultMotion.PageSlide
                                )
                            },
                            popExitTransition = {
                                slideOutHorizontally(
                                    targetOffsetX = { it / 3 },
                                    animationSpec = ClipVaultMotion.PageSlide
                                )
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
                                slideInHorizontally(
                                    initialOffsetX = { it / 3 },
                                    animationSpec = ClipVaultMotion.PageSlide
                                )
                            },
                            exitTransition = {
                                slideOutHorizontally(
                                    targetOffsetX = { -it / 3 },
                                    animationSpec = ClipVaultMotion.PageSlide
                                )
                            },
                            popEnterTransition = {
                                slideInHorizontally(
                                    initialOffsetX = { -it / 3 },
                                    animationSpec = ClipVaultMotion.PageSlide
                                )
                            },
                            popExitTransition = {
                                slideOutHorizontally(
                                    targetOffsetX = { it / 3 },
                                    animationSpec = ClipVaultMotion.PageSlide
                                )
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
