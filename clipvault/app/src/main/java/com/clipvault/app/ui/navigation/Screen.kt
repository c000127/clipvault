package com.clipvault.app.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Screen {
    @Serializable data object Home : Screen
    @Serializable data class Detail(val id: Long) : Screen
    @Serializable data class New(val text: String? = null) : Screen
    @Serializable data object TagManager : Screen
    @Serializable data object AiSettings : Screen
    @Serializable data object Settings : Screen
}
