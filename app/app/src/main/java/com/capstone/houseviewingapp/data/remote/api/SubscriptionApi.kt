package com.capstone.houseviewingapp.data.remote.api

import retrofit2.Call
import retrofit2.http.Header
import retrofit2.http.POST

interface SubscriptionApi {
    @POST("subscriptions/premium")
    fun subscribePremium(
        @Header("Authorization") authorization: String
    ): Call<Void>
}
