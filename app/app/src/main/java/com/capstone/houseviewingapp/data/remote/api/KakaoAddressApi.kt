package com.capstone.houseviewingapp.data.remote.api

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

data class KakaoAddressSearchResponse(
    val documents: List<KakaoAddressDocument> = emptyList()
)

data class KakaoAddressDocument(
    val address_name: String? = null,
    val road_address: KakaoRoadAddress? = null,
    val address: KakaoJibunAddress? = null
)

data class KakaoRoadAddress(
    val address_name: String? = null,
    val zone_no: String? = null
)

data class KakaoJibunAddress(
    val address_name: String? = null
)

interface KakaoAddressApi {
    @GET("v2/local/search/address.json")
    fun searchAddress(
        @Header("Authorization") authorization: String,
        @Query("query") query: String
    ): Call<KakaoAddressSearchResponse>
}
