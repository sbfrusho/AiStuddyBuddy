package com.example.aistuddybuddy.di

import com.example.aistuddybuddy.MainViewModel
import com.example.aistuddybuddy.domain.repository.DiVerificationRepository
import com.example.aistuddybuddy.domain.repository.DiVerificationRepositoryImpl
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    prettyPrint = true
                    isLenient = true
                })
            }
        }
    }
}

val repositoryModule = module {

    single<DiVerificationRepository> {
        DiVerificationRepositoryImpl(httpClient = get())
    }

    viewModel {
        MainViewModel(repository = get())
    }
}