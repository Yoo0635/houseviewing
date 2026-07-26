package com.capstone.houseviewingapp.data.remote.api

import com.capstone.houseviewingapp.auth.model.LoginRequest
import com.capstone.houseviewingapp.auth.model.LoginResponse
import com.capstone.houseviewingapp.auth.model.ReissueRequest
import com.capstone.houseviewingapp.auth.model.ReissueResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/login")
    fun login(@Body body: LoginRequest): Call<LoginResponse>

    @POST("auth/reissue")
    fun reissue(@Body body: ReissueRequest): Call<ReissueResponse>

    @POST("auth/logout")
    fun logout(@Header("Authorization") authorization: String): Call<Void>
}
