package com.fintrack.app.data.network

import com.fintrack.app.domain.repository.SessionTokenStore

class InMemoryDemoTokenStore : SessionTokenStore {
    @Volatile
    private var token: String? = null

    override fun currentToken(): String? = token

    override fun activateDemoToken() {
        token = DEMO_ACCESS_TOKEN
    }

    override fun clear() {
        token = null
    }

    private companion object {
        const val DEMO_ACCESS_TOKEN = "fintrack-demo-access-token"
    }
}
