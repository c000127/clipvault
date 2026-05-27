package com.clipvault.app.ui.home

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.clipvault.app.ui.theme.ClipVaultTheme
import org.junit.Rule
import org.junit.Test
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.AnimatedVisibility

class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @OptIn(ExperimentalSharedTransitionApi::class)
    @Test
    fun homeScreen_showsEmptyState_whenNoClips() {
        composeTestRule.setContent {
            ClipVaultTheme {
                SharedTransitionLayout {
                    AnimatedVisibility(visible = true) {
                        HomeScreen(
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this@AnimatedVisibility,
                            onItemClick = {},
                            onNewItem = {}
                        )
                    }
                }
            }
        }

        // Verify empty state text is shown
        composeTestRule.onNodeWithText("No clips yet. Tap + to add one!").assertIsDisplayed()
    }
}
