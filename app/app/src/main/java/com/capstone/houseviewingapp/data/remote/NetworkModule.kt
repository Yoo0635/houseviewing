package com.capstone.houseviewingapp.data.remote

import com.capstone.houseviewingapp.BuildConfig
import com.capstone.houseviewingapp.data.remote.api.AuthApi
import com.capstone.houseviewingapp.data.remote.api.ContractApi
import com.capstone.houseviewingapp.data.remote.api.HouseApi
import com.capstone.houseviewingapp.data.remote.api.KakaoAddressApi
import com.capstone.houseviewingapp.data.remote.api.AnalysisApi
import com.capstone.houseviewingapp.data.remote.api.SubscriptionApi
import com.capstone.houseviewingapp.data.remote.api.UserApi
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {

    private val gson: Gson = GsonBuilder()
        .serializeNulls()
        .create()

    private val logging: HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(ScalarsConverterFactory.create())
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    private val kakaoRetrofit: Retrofit = Retrofit.Builder()
        .baseUrl("https://dapi.kakao.com/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    val authApi: AuthApi = retrofit.create(AuthApi::class.java)
    val userApi: UserApi = retrofit.create(UserApi::class.java)
    val houseApi: HouseApi = retrofit.create(HouseApi::class.java)
    val contractApi: ContractApi = retrofit.create(ContractApi::class.java)
    val analysisApi: AnalysisApi = retrofit.create(AnalysisApi::class.java)
    val subscriptionApi: SubscriptionApi = retrofit.create(SubscriptionApi::class.java)
    val kakaoAddressApi: KakaoAddressApi = kakaoRetrofit.create(KakaoAddressApi::class.java)
}
