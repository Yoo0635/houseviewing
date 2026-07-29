package com.capstone.houseviewingapp.data.remote

import android.content.Context
import com.capstone.houseviewingapp.BuildConfig
import com.capstone.houseviewingapp.auth.TokenRefreshCoordinator
import com.capstone.houseviewingapp.data.local.AuthTokenLocalStore
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
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit

object NetworkModule {
    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

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
        .addInterceptor { chain ->
            val original = chain.request()
            val accessToken = appContext?.let { AuthTokenLocalStore.getAccessToken(it) }
            val request = if (original.header("Authorization") == null
                && !accessToken.isNullOrBlank()
                && shouldAttachAccessToken(original)
            ) {
                original.newBuilder()
                    .header("Authorization", "Bearer $accessToken")
                    .build()
            } else {
                original
            }
            chain.proceed(request)
        }
        .addInterceptor(logging)
        .authenticator { _, response ->
            val context = appContext ?: return@authenticator null
            if (responseCount(response) >= 2 || !shouldRefreshToken(response.request)) {
                return@authenticator null
            }

            val failedAccessToken = response.request.header("Authorization")
                ?.removePrefix("Bearer ")
                ?.takeIf { it.isNotBlank() }

            val newAccessToken = runBlocking {
                TokenRefreshCoordinator.refreshAfterUnauthorized(context, failedAccessToken).getOrNull()
            } ?: return@authenticator null

            response.request.newBuilder()
                .header("Authorization", "Bearer $newAccessToken")
                .build()
        }
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

    private fun shouldAttachAccessToken(request: Request): Boolean {
        val path = request.url.encodedPath
        return !path.startsWith("/auth/")
    }

    private fun shouldRefreshToken(request: Request): Boolean {
        val path = request.url.encodedPath
        return !path.startsWith("/auth/")
                && request.header("Authorization")?.startsWith("Bearer ") == true
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
