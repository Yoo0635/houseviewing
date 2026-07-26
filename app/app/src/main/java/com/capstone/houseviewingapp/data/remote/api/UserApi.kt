package com.capstone.houseviewingapp.data.remote.api

import com.capstone.houseviewingapp.auth.model.FindIdRequest
import com.capstone.houseviewingapp.auth.model.FindIdResponse
import com.capstone.houseviewingapp.auth.model.MeResponse
import com.capstone.houseviewingapp.auth.model.RegisterRequest
import com.capstone.houseviewingapp.auth.model.ResetPasswordRequest
import com.capstone.houseviewingapp.auth.model.VerifyPasswordRequest
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST

interface UserApi {
    @POST("users/register")
    fun register(@Body body: RegisterRequest): Call<Void>

    @GET("users/me")
    fun me(@Header("Authorization") authorization: String): Call<MeResponse>

    @POST("users/find-id")
    fun findId(@Body body: FindIdRequest): Call<FindIdResponse>

    @POST("users/password/verify")
    fun verifyPassword(@Body body: VerifyPasswordRequest): Call<String>

    @PATCH("users/password/reset")
    fun resetPassword(@Body body: ResetPasswordRequest): Call<Void>

    @DELETE("users/me")
    fun deleteMe(@Header("Authorization") authorization: String): Call<Void>
}
