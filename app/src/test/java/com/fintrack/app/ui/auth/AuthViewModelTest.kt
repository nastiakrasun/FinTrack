package com.fintrack.app.ui.auth

import com.fintrack.app.MainDispatcherRule
import com.fintrack.app.domain.repository.AuthRepository
import com.fintrack.app.domain.repository.AuthResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private class FakeAuthRepository : AuthRepository {
    private val users = mutableMapOf<String, String>()
    var lastPassword: String? = null

    override fun register(email: String, password: CharArray): AuthResult {
        lastPassword = String(password)
        if (email in users) return AuthResult.EMAIL_ALREADY_REGISTERED
        users[email] = String(password)
        return AuthResult.SUCCESS
    }

    override fun login(email: String, password: CharArray): AuthResult {
        lastPassword = String(password)
        return if (users[email] == String(password)) {
            AuthResult.SUCCESS
        } else {
            AuthResult.INVALID_CREDENTIALS
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeAuthRepository()
    private val viewModel by lazy { AuthViewModel(repository, mainDispatcherRule.dispatcher) }
    private val state get() = viewModel.uiState.value

    private fun registerUser(email: String = "user@example.com", password: String = "secure-pass") {
        viewModel.onEmailChange(email)
        viewModel.onPasswordChange(password)
        viewModel.onConfirmPasswordChange(password)
        viewModel.register()
        viewModel.logout()
    }

    @Test
    fun loginWithEmptyFieldsShowsFieldErrorsAndDoesNotCallRepository() {
        viewModel.login()

        assertNotNull(state.emailError)
        assertNotNull(state.passwordError)
        assertNull(state.signedInEmail)
        assertNull(repository.lastPassword)
    }

    @Test
    fun loginRejectsInvalidEmailAndShortPassword() {
        viewModel.onEmailChange("not-an-email")
        viewModel.onPasswordChange("short")

        viewModel.login()

        assertEquals("Enter a valid email, for example name@example.com.", state.emailError)
        assertTrue(state.passwordError!!.contains("at least 8"))
        assertNull(state.signedInEmail)
    }

    @Test
    fun loginWithWrongCredentialsShowsGeneralErrorAndClearsPassword() {
        registerUser()
        viewModel.onEmailChange("user@example.com")
        viewModel.onPasswordChange("wrong-pass")

        viewModel.login()

        assertEquals("Email or password is incorrect.", state.formError)
        assertEquals("", state.password)
        assertFalse(state.isLoading)
        assertNull(state.signedInEmail)
    }

    @Test
    fun loginSuccessExposesSignedInEmailAndClearsPassword() {
        registerUser()
        viewModel.onEmailChange("  user@example.com ")
        viewModel.onPasswordChange("secure-pass")

        viewModel.login()

        assertEquals("user@example.com", state.signedInEmail)
        assertEquals("secure-pass", repository.lastPassword)
        assertEquals("", state.password)
        assertNull(state.formError)
    }

    @Test
    fun registrationValidatesEveryField() {
        viewModel.register()

        assertNotNull(state.emailError)
        assertNotNull(state.passwordError)
        assertEquals("Confirm your password.", state.confirmPasswordError)
    }

    @Test
    fun registrationRejectsMismatchingPasswords() {
        viewModel.onEmailChange("user@example.com")
        viewModel.onPasswordChange("secure-pass")
        viewModel.onConfirmPasswordChange("other-pass")

        viewModel.register()

        assertNull(state.emailError)
        assertNull(state.passwordError)
        assertEquals("Passwords do not match. Re-enter the same password.", state.confirmPasswordError)
        assertNull(state.signedInEmail)
    }

    @Test
    fun registrationSuccessSignsUserInAndClearsSecrets() {
        viewModel.onEmailChange("user@example.com")
        viewModel.onPasswordChange("secure-pass")
        viewModel.onConfirmPasswordChange("secure-pass")

        viewModel.register()

        assertEquals("user@example.com", state.signedInEmail)
        assertEquals("", state.password)
        assertEquals("", state.confirmPassword)
    }

    @Test
    fun registrationWithExistingEmailShowsEmailError() {
        registerUser()
        viewModel.onEmailChange("user@example.com")
        viewModel.onPasswordChange("secure-pass")
        viewModel.onConfirmPasswordChange("secure-pass")

        viewModel.register()

        assertEquals("An account with this email already exists.", state.emailError)
        assertNull(state.signedInEmail)
    }

    @Test
    fun editingAFieldClearsItsErrorAndTheGeneralError() {
        viewModel.login()
        assertNotNull(state.emailError)

        viewModel.onEmailChange("user@example.com")

        assertNull(state.emailError)
        assertNull(state.formError)
        assertNotNull(state.passwordError)
    }

    @Test
    fun switchingScreensKeepsEmailButDropsPasswordsAndErrors() {
        viewModel.onEmailChange("user@example.com")
        viewModel.onPasswordChange("short")
        viewModel.onConfirmPasswordChange("short")
        viewModel.login()

        viewModel.onAuthScreenSwitched()

        assertEquals(AuthUiState(email = "user@example.com"), state)
    }

    @Test
    fun logoutResetsAllState() {
        registerUser()
        viewModel.onEmailChange("user@example.com")
        viewModel.onPasswordChange("secure-pass")
        viewModel.login()
        assertNotNull(state.signedInEmail)

        viewModel.logout()

        assertEquals(AuthUiState(), state)
    }

    @Test
    fun loadingStateIsShownWhileRepositoryCallIsRunning() {
        val dispatcher = StandardTestDispatcher()
        val loadingViewModel = AuthViewModel(repository, dispatcher)
        loadingViewModel.onEmailChange("user@example.com")
        loadingViewModel.onPasswordChange("secure-pass")
        loadingViewModel.onConfirmPasswordChange("secure-pass")

        loadingViewModel.register()
        assertTrue(loadingViewModel.uiState.value.isLoading)
        assertNull(loadingViewModel.uiState.value.signedInEmail)

        // A second submit while loading is ignored.
        loadingViewModel.register()
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(loadingViewModel.uiState.value.isLoading)
        assertEquals("user@example.com", loadingViewModel.uiState.value.signedInEmail)
    }
}
