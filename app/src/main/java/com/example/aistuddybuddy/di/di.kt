package com.example.aistuddybuddy.di

import androidx.room.Room
import com.example.aistuddybuddy.data.ai.AiRepositoryImpl
import com.example.aistuddybuddy.data.bookmarks.BookmarkRepositoryImpl
import com.example.aistuddybuddy.data.bookmarks.StudyBuddyDatabase
import com.example.aistuddybuddy.data.image.BitmapLoader
import com.example.aistuddybuddy.data.ocr.MlKitOcrRepository
import com.example.aistuddybuddy.data.settings.KeyCipher
import com.example.aistuddybuddy.data.settings.SettingsRepositoryImpl
import com.example.aistuddybuddy.domain.repository.AiRepository
import com.example.aistuddybuddy.domain.repository.BookmarkRepository
import com.example.aistuddybuddy.domain.repository.OcrRepository
import com.example.aistuddybuddy.domain.repository.SettingsRepository
import com.example.aistuddybuddy.domain.usecase.AskAiUseCase
import com.example.aistuddybuddy.domain.usecase.ExtractQuestionsUseCase
import com.example.aistuddybuddy.domain.usecase.ProcessOcrImageUseCase
import com.example.aistuddybuddy.domain.usecase.ScanQuestionsUseCase
import com.example.aistuddybuddy.ui.screens.bookmarks.BookmarksViewModel
import com.example.aistuddybuddy.ui.screens.questions.ScanViewModel
import com.example.aistuddybuddy.ui.screens.settings.SettingsViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single {
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            isLenient = true
        }
    }
    single {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(get<Json>())
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 90_000
                connectTimeoutMillis = 15_000
            }
        }
    }
}

val repositoryModule = module {
    single<SettingsRepository> { SettingsRepositoryImpl(androidContext(), KeyCipher()) }
    single<OcrRepository> { MlKitOcrRepository() }
    single<AiRepository> { AiRepositoryImpl(httpClient = get(), settingsRepository = get(), json = get()) }
    single { BitmapLoader(androidContext()) }
    single {
        Room.databaseBuilder(androidContext(), StudyBuddyDatabase::class.java, "studybuddy.db").build()
    }
    single<BookmarkRepository> { BookmarkRepositoryImpl(get<StudyBuddyDatabase>().bookmarkDao()) }
}

val useCaseModule = module {
    factory { ProcessOcrImageUseCase(ocrRepository = get()) }
    factory { ExtractQuestionsUseCase(processOcrImage = get()) }
    factory { AskAiUseCase(aiRepository = get(), settingsRepository = get()) }
    factory { ScanQuestionsUseCase(aiRepository = get(), settingsRepository = get(), extractWithOcr = get()) }
}

val viewModelModule = module {
    viewModel {
        ScanViewModel(
            scanQuestions = get(),
            askAi = get(),
            bookmarkRepository = get(),
            bitmapLoader = get()
        )
    }
    viewModel { BookmarksViewModel(bookmarkRepository = get()) }
    viewModel { SettingsViewModel(settingsRepository = get()) }
}
