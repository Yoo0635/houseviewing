package com.capstone.houseviewingapp.auth

import com.capstone.houseviewingapp.auth.model.FindIdRequest
import com.capstone.houseviewingapp.auth.model.FindIdResponse
import com.capstone.houseviewingapp.auth.model.LoginRequest
import com.capstone.houseviewingapp.auth.model.LoginResponse
import com.capstone.houseviewingapp.auth.model.MeResponse
import com.capstone.houseviewingapp.auth.model.ReissueRequest
import com.capstone.houseviewingapp.auth.model.ReissueResponse
import com.capstone.houseviewingapp.auth.model.RegisterRequest
import com.capstone.houseviewingapp.auth.model.ResetPasswordRequest
import com.capstone.houseviewingapp.auth.model.VerifyPasswordRequest
import com.capstone.houseviewingapp.data.remote.NetworkModule
import com.capstone.houseviewingapp.data.remote.RemoteApiException
import com.capstone.houseviewingapp.data.remote.executeApi
import com.capstone.houseviewingapp.data.remote.executeApiString
import com.capstone.houseviewingapp.data.remote.executeApiVoid

/**
 * JWT: 인증이 필요한 요청은 [bearer] 로 Authorization 헤더에 전달
 */
class RemoteAuthRepository(
    private val authApi: com.capstone.houseviewingapp.data.remote.api.AuthApi = NetworkModule.authApi,
    private val userApi: com.capstone.houseviewingapp.data.remote.api.UserApi = NetworkModule.userApi
) : AuthRepository {

    private fun bearer(accessToken: String): String = "Bearer $accessToken"

    override suspend fun register(request: RegisterRequest): Result<Unit> {
        val result = userApi.register(request).executeApiVoid()
        return result.fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { Result.failure(mapRegisterException(it)) }
        )
    }

    private fun mapRegisterException(t: Throwable): Throwable {
        val re = t as? RemoteApiException ?: return t
        return when (re.code) {
            "DB001" -> IllegalStateException("DUPLICATE_LOGIN_ID")
            "DB002" -> IllegalStateException("DUPLICATE_EMAIL")
            "DB003" -> IllegalStateException("DUPLICATE_RESOURCE")
            else -> re
        }
    }

    /**
     * 백엔드에 중복 확인 API 없음 — 가입 시점에만 검증됨
     */
    override suspend fun isLoginIdAvailable(loginId: String): Result<Boolean> =
        Result.success(true)

    override suspend fun isEmailAvailable(email: String): Result<Boolean> =
        Result.success(true)

    override suspend fun login(request: LoginRequest): Result<LoginResponse> {
        val result = authApi.login(request).executeApi()
        return result.fold(
            onSuccess = { Result.success(it) },
            onFailure = { Result.failure(mapLoginException(it)) }
        )
    }

    private fun mapLoginException(t: Throwable): Throwable {
        val re = t as? RemoteApiException ?: return t
        return when (re.code) {
            "AU004" -> IllegalArgumentException("LOGIN_FAILED")
            else -> re
        }
    }

    override suspend fun logout(accessToken: String): Result<Unit> {
        val result = authApi.logout(bearer(accessToken)).executeApiVoid()
        return result.fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { Result.failure(it) }
        )
    }

    override suspend fun reissue(request: ReissueRequest): Result<ReissueResponse> {
        val result = authApi.reissue(request).executeApi()
        return result.fold(
            onSuccess = { Result.success(it) },
            onFailure = { Result.failure(it) }
        )
    }

    override suspend fun findId(request: FindIdRequest): Result<FindIdResponse> {
        val result = userApi.findId(request).executeApi()
        return result.fold(
            onSuccess = { Result.success(it) },
            onFailure = { Result.failure(it) }
        )
    }

    override suspend fun verifyPassword(request: VerifyPasswordRequest): Result<String> {
        val result = userApi.verifyPassword(request).executeApiString()
        return result.fold(
            onSuccess = { Result.success(it.trim()) },
            onFailure = { Result.failure(it) }
        )
    }

    override suspend fun resetPassword(request: ResetPasswordRequest): Result<Unit> {
        val result = userApi.resetPassword(request).executeApiVoid()
        return result.fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { Result.failure(it) }
        )
    }

    override suspend fun me(accessToken: String): Result<MeResponse> {
        val result = userApi.me(bearer(accessToken)).executeApi()
        return result.fold(
            onSuccess = { Result.success(it) },
            onFailure = { Result.failure(it) }
        )
    }

    override suspend fun deleteMe(accessToken: String): Result<Unit> {
        val result = userApi.deleteMe(bearer(accessToken)).executeApiVoid()
        return result.fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { Result.failure(it) }
        )
    }
}
