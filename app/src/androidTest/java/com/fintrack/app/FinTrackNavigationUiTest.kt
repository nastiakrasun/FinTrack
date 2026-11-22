package com.fintrack.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FinTrackNavigationUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun loginNavigatesToRegistrationAndBack() {
        composeRule.onNodeWithText("Welcome back").assertIsDisplayed()
        composeRule.onNodeWithText("New to FinTrack? Create an account").performClick()
        composeRule.onNodeWithText("Create your account").assertIsDisplayed()
        composeRule.onNodeWithText("Already have an account? Log in").performClick()
        composeRule.onNodeWithText("Welcome back").assertIsDisplayed()
    }

    @Test
    fun loginSubmitShowsValidationMessagesForEmptyFields() {
        composeRule.onNodeWithText("Log in").performClick()

        composeRule.onNodeWithText("Enter your email address.").assertIsDisplayed()
        composeRule.onNodeWithText("Enter your password.").assertIsDisplayed()
    }
}
