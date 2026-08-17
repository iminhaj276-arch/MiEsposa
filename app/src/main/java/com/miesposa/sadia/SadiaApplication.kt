package com.miesposa.sadia

import android.app.Application

/**
 * App-wide entry point. Kept intentionally thin — real wiring happens in a lightweight
 * manual DI container (ServiceLocator) so the project stays easy to read without pulling
 * in Hilt/Dagger for an MVP. Can be swapped for Hilt later without changing feature code.
 */
class SadiaApplication : Application() {
    lateinit var container: ServiceLocator
        private set

    override fun onCreate() {
        super.onCreate()
        container = ServiceLocator(applicationContext)
    }
}
