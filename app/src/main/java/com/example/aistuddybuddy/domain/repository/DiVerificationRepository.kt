package com.example.aistuddybuddy.domain.repository

import io.ktor.client.HttpClient

/**
 * Minimal repository used only to verify, at app boot, that Koin can resolve
 * a repository which itself depends on a service-layer singleton (HttpClient).
 */
interface DiVerificationRepository {
    fun verifyDependencyChain(): String
}

class DiVerificationRepositoryImpl(
    private val httpClient: HttpClient
) : DiVerificationRepository {

    override fun verifyDependencyChain(): String {
        return if (this.hashCode() != 0) {
            "Koin Initialized Successfully"
        } else {
            "Dependency Chain Failed"
        }
    }
}