package com.poketch.core.domain.repository

import com.poketch.core.domain.model.AuthState
import com.poketch.core.domain.model.AuthUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val authState: Flow<AuthState>
    val currentUser: AuthUser?

    suspend fun signInWithEmail(email: String, password: String): Result<AuthUser>
    suspend fun signUpWithEmail(email: String, password: String): Result<AuthUser>
    suspend fun signInWithGoogleIdToken(idToken: String): Result<AuthUser>
    suspend fun signOut(): Result<Unit>
}
