package com.fintrack.app.domain.repository

interface SessionTokenStore {
    fun currentToken(): String?

    fun activateDemoToken()

    fun clear()
}
