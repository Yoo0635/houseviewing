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

/**
 * Mock / Remote 공통. 네트워크 호출은 suspend 로 메인 스레드 블로킹·ANR 방지.
 */
interface AuthRepository {
    suspend fun register(request: RegisterRequest): Result<Unit>
    suspend fun isLoginIdAvailable(loginId: String): Result<Boolean>
    suspend fun isEmailAvailable(email: String): Result<Boolean>
    suspend fun login(request: LoginRequest): Result<LoginResponse>
    suspend fun logout(accessToken: String): Result<Unit>
    suspend fun reissue(request: ReissueRequest): Result<ReissueResponse>
    suspend fun findId(request: FindIdRequest): Result<FindIdResponse>
    /** 백엔드 POST /users/password/verify — 응답 본문이 재설정용 토큰 문자열 */
    suspend fun verifyPassword(request: VerifyPasswordRequest): Result<String>
    suspend fun resetPassword(request: ResetPasswordRequest): Result<Unit>
    suspend fun me(accessToken: String): Result<MeResponse>
    suspend fun deleteMe(accessToken: String): Result<Unit>
}
