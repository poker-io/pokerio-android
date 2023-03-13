package com.pokerio.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */

class HomeScreenInstrumentedTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun checkAnimation() {
        // Start the app
        composeTestRule.setContent {
            HomeScreen()
        }

        composeTestRule.onNodeWithTag("image_caption", useUnmergedTree = true)
            .assertDoesNotExist()
        composeTestRule.onRoot().performClick()
        composeTestRule.onNodeWithTag("image_caption", useUnmergedTree = true)
            .assertIsDisplayed()
    }
}
