package com.capstone.houseviewingapp.data.remote.api

import com.capstone.houseviewingapp.registration.remote.ContractRegisterRequest
import com.capstone.houseviewingapp.registration.remote.ContractRegisterResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface ContractApi {
    @POST("contracts/register")
    fun register(
        @Header("Authorization") authorization: String,
        @Body body: ContractRegisterRequest
    ): Call<ContractRegisterResponse>
}
