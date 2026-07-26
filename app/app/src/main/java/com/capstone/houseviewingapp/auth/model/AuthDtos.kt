package com.capstone.houseviewingapp.auth.model

/**
 * 백엔드 JSON 키와 동일한 DTO (users, auth 도메인)
 */

data class RegisterRequest(
    val name: String,
    val email: String,
    val loginId: String,
    val password: String
)

data class LoginRequest(
    val loginId: String,
    val password: String
)

data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val userId: Long,
    val loginId: String,
    val name: String
)

data class FindIdRequest(
    val email: String,
    val name: String
)

data class FindIdResponse(
    val loginId: String
)

/** 백엔드 UserVerifyPasswordRequest — 비밀번호 필드 없음, 본인 확인 후 resetToken 문자열 응답 */
data class VerifyPasswordRequest(
    val email: String,
    val name: String,
    val loginId: String
)

/**
 * 백엔드 UserResetPasswordRequest — 필드명이 refreshToken 이지만 값은 password/verify 로 받은 재설정 토큰
 */
data class ResetPasswordRequest(
    val refreshToken: String,
    val newPassword: String,
    val confirmPassword: String
)

data class ReissueRequest(
    val refreshToken: String
)

data class ReissueResponse(
    val accessToken: String
)

data class SubscriptionMeResponse(
    val planType: String
)

/** 백엔드 UserMeResponse — loginId 없음 (로컬 AuthTokenLocalStore 등에서 보관) */
data class MeResponse(
    val name: String,
    val email: String,
    val subscription: SubscriptionMeResponse?
)

data class ApiErrorResponse(
    val code: String? = null,
    val message: String? = null
)
