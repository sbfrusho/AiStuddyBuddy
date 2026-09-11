package com.example.aistuddybuddy.di

import com.example.aistuddybuddy.domain.repository.OcrRepository
import com.example.aistuddybuddy.domain.repository.OcrRepositoryImpl
import com.example.aistuddybuddy.domain.usecase.ProcessOcrImageUseCase
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
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
    single <OcrRepository>{ OcrRepositoryImpl() }
    factory { ProcessOcrImageUseCase(get()) }
    // single<AiRepository> { AiRepositoryImpl(get(), get()) }
}