package com.capstone.houseviewingapp.data.remote.api

import com.capstone.houseviewingapp.house.model.HouseMeResponse
import com.capstone.houseviewingapp.house.model.HousesResponse
import com.capstone.houseviewingapp.registration.remote.HouseRegisterRequest
import com.capstone.houseviewingapp.registration.remote.HouseRegisterResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface HouseApi {
    @POST("houses/register")
    fun register(
        @Header("Authorization") authorization: String,
        @Body body: HouseRegisterRequest
    ): Call<HouseRegisterResponse>

    @GET("houses/{houseId}")
    fun getHouse(
        @Header("Authorization") authorization: String,
        @Path("houseId") houseId: Long
    ): Call<HouseMeResponse>

    @GET("houses")
    fun getHouses(
        @Header("Authorization") authorization: String
    ): Call<List<HousesResponse>>
}
