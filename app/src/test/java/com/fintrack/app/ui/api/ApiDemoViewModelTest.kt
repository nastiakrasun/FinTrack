package com.fintrack.app.ui.api

import com.fintrack.app.MainDispatcherRule
import com.fintrack.app.domain.repository.DemoApiRepository
import com.fintrack.app.domain.repository.DemoApiResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ApiDemoViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun exposesLoadingThenSuccess() {
        val viewModel = ApiDemoViewModel(FakeDemoApiRepository(DemoApiResult.Success(true, "token")))

        viewModel.checkApi()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(true, viewModel.uiState.value.authenticated)
        assertTrue(viewModel.uiState.value.message!!.contains("responded successfully"))
    }

    @Test
    fun exposesFriendlyNetworkError() {
        val message = "Check your internet connection."
        val viewModel = ApiDemoViewModel(FakeDemoApiRepository(DemoApiResult.Failure(message)))

        viewModel.checkApi()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(message, viewModel.uiState.value.errorMessage)
        assertEquals(null, viewModel.uiState.value.authenticated)
    }

    private class FakeDemoApiRepository(
        private val result: DemoApiResult
    ) : DemoApiRepository {
        override suspend fun checkProtectedEndpoint(): DemoApiResult = result
    }
}
