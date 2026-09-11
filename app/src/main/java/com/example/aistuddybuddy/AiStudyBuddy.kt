package com.example.aistuddybuddy

import android.app.Application
import com.example.aistuddybuddy.di.appModule
import com.example.aistuddybuddy.di.repositoryModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class AiStudyBuddyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@AiStudyBuddyApp)
            modules(appModule, repositoryModule)
        }
    }
}