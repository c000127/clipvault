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

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var themePreferences: ThemePreferences

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

                NavHost(
                    navController = navController,
                    startDestination = Screen.Home,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable<Screen.Home>(
                        enterTransition = { null },
                        exitTransition = { null },
                        popEnterTransition = { null },
                        popExitTransition = { null }
                    ) {
                        HomeScreen(
                            onItemClick = { id ->
                                navController.navigate(Screen.Detail(id))
                            },
                            onNewItem = {
                                try {
                                    android.util.Log.d("HomeScreen", "navigating to Screen.New()")
                                    navController.navigate(Screen.New())
                                } catch (e: Exception) {
                                    android.util.Log.e("HomeScreen", "navigation failed", e)
                                }
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
                        enterTransition = { null },
                        exitTransition = { null },
                        popEnterTransition = { null },
                        popExitTransition = { null }
                    ) { backStackEntry ->
                        DetailScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<Screen.New>(
                        enterTransition = { null },
                        exitTransition = { null },
                        popEnterTransition = { null },
                        popExitTransition = { null }
                    ) { backStackEntry ->
                        android.util.Log.d("MainActivity", "navigating to New screen")
                        val route = backStackEntry.toRoute<Screen.New>()
                        NewItemScreen(
                            onBack = { navController.popBackStack() },
                            initialText = route.text
                        )
                    }

                    composable<Screen.TagManager>(
                        enterTransition = { null },
                        exitTransition = { null },
                        popEnterTransition = { null },
                        popExitTransition = { null }
                    ) {
                        TagManagerScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<Screen.Settings>(
                        enterTransition = { null },
                        exitTransition = { null },
                        popEnterTransition = { null },
                        popExitTransition = { null }
                    ) {
                        SettingsScreen(
                            onBack = { navController.popBackStack() },
                            onAiSettings = {
                                navController.navigate(Screen.AiSettings)
                            }
                        )
                    }

                    composable<Screen.AiSettings>(
                        enterTransition = { null },
                        exitTransition = { null },
                        popEnterTransition = { null },
                        popExitTransition = { null }
                    ) {
                        AiSettingsScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
