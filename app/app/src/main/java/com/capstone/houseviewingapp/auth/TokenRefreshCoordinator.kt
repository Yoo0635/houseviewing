package com.capstone.houseviewingapp.auth

import android.content.Context
import com.capstone.houseviewingapp.auth.model.ReissueRequest
import com.capstone.houseviewingapp.data.local.AuthTokenLocalStore
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object TokenRefreshCoordinator {
    private val mutex = Mutex()

    suspend fun refreshAfterUnauthorized(
        context: Context,
        failedAccessToken: String?,
        authRepository: AuthRepository = AuthRepositoryProvider.repository
    ): Result<String> = mutex.withLock {
        val appContext = context.applicationContext
        val currentAccessToken = AuthTokenLocalStore.getAccessToken(appContext)
        if (!failedAccessToken.isNullOrBlank()
            && !currentAccessToken.isNullOrBlank()
            && currentAccessToken != failedAccessToken
        ) {
            return@withLock Result.success(currentAccessToken)
        }

        val refreshToken = AuthTokenLocalStore.getRefreshToken(appContext)
        if (refreshToken.isNullOrBlank()) {
            AuthTokenLocalStore.clear(appContext)
            return@withLock Result.failure(IllegalStateException("REFRESH_TOKEN_NOT_FOUND"))
        }

        val deviceId = AuthTokenLocalStore.getOrCreateDeviceId(appContext)
        authRepository.reissue(ReissueRequest(refreshToken), deviceId)
            .fold(
                onSuccess = {
                    AuthTokenLocalStore.saveTokens(appContext, it.accessToken, it.refreshToken)
                    Result.success(it.accessToken)
                },
                onFailure = {
                    AuthTokenLocalStore.clear(appContext)
                    Result.failure(it)
                }
            )
    }
}
