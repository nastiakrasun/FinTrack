package com.fintrack.app

import android.app.Application

class FinTrackApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
