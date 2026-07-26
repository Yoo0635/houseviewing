package com.capstone.houseviewingapp.data.remote.api

import com.capstone.houseviewingapp.analysis.model.AnalysisResponse
import com.capstone.houseviewingapp.analysis.model.PdfDownloadResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface AnalysisApi {
    @Multipart
    @POST("analysis/pre-contract-diganoses")
    fun preContractDiagnoses(
        @Header("Authorization") authorization: String,
        @Part file: MultipartBody.Part,
        @Part("data") data: RequestBody
    ): Call<PdfDownloadResponse>

    @Multipart
    @POST("analysis/pre-contract-diagnoses")
    fun preContractDiagnosesAlt(
        @Header("Authorization") authorization: String,
        @Part file: MultipartBody.Part,
        @Part("data") data: RequestBody
    ): Call<PdfDownloadResponse>

    @Multipart
    @POST("analysis/{houseId}/post-contract-diagnoses")
    fun postContractDiagnoses(
        @Header("Authorization") authorization: String,
        @Path("houseId") houseId: Long,
        @Part file: MultipartBody.Part
    ): Call<PdfDownloadResponse>

    @POST("analysis/{houseId}/change-diagnoses")
    fun changeDiagnoses(
        @Header("Authorization") authorization: String,
        @Path("houseId") houseId: Long
    ): Call<PdfDownloadResponse>

    @GET("analyses")
    fun getAnalyses(
        @Header("Authorization") authorization: String
    ): Call<List<AnalysisResponse>>

    @GET("analyses/diff")
    fun getDiffAnalyses(
        @Header("Authorization") authorization: String
    ): Call<List<AnalysisResponse>>
}
