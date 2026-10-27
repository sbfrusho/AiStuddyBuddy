package com.example.aistuddybuddy

import com.example.aistuddybuddy.di.appModule
import com.example.aistuddybuddy.di.repositoryModule
import com.example.aistuddybuddy.di.useCaseModule
import com.example.aistuddybuddy.di.viewModelModule
import android.app.Application
import android.util.Log
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class AiStudyBuddyApp : Application() {

    companion object {
        const val DI_LOG_TAG = "StudyBuddyDI"
    }

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@AiStudyBuddyApp)
            modules(appModule, repositoryModule, useCaseModule, viewModelModule)
        }

        Log.d(DI_LOG_TAG, "Koin DI container started successfully for AiStudyBuddyApp")
    }
}