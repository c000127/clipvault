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

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            ClipVaultTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = Screen.Home,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable<Screen.Home> {
                        HomeScreen(
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

                    composable<Screen.Detail> { backStackEntry ->
                        val route = backStackEntry.toRoute<Screen.Detail>()
                        DetailScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<Screen.New> { backStackEntry ->
                        val route = backStackEntry.toRoute<Screen.New>()
                        NewItemScreen(
                            onBack = { navController.popBackStack() },
                            initialText = route.text
                        )
                    }

                    composable<Screen.TagManager> {
                        TagManagerScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<Screen.Settings> {
                        SettingsScreen(
                            onBack = { navController.popBackStack() },
                            onAiSettings = {
                                navController.navigate(Screen.AiSettings)
                            }
                        )
                    }

                    composable<Screen.AiSettings> {
                        AiSettingsScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
