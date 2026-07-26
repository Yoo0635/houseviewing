package com.capstone.houseviewingapp.data.remote

import android.util.Log
import com.capstone.houseviewingapp.auth.model.ApiErrorResponse
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Response

private val gson = Gson()
private const val TAG = "ApiExecutor"

suspend fun <T> Call<T>.executeApi(): Result<T> = withContext(Dispatchers.IO) {
    runCatching {
        val response = execute()
        if (response.isSuccessful) {
            response.body() ?: throw IllegalStateException("EMPTY_BODY")
        } else {
            val exception = response.toApiException()
            logApiFailure(request(), response.code(), exception)
            throw exception
        }
    }
}

suspend fun Call<Void>.executeApiVoid(): Result<Unit> = withContext(Dispatchers.IO) {
    runCatching {
        val response = execute()
        if (response.isSuccessful) {
            Unit
        } else {
            val exception = response.toApiException()
            logApiFailure(request(), response.code(), exception)
            throw exception
        }
    }
}

suspend fun Call<String>.executeApiString(): Result<String> = withContext(Dispatchers.IO) {
    runCatching {
        val response = execute()
        if (response.isSuccessful) {
            response.body().orEmpty()
        } else {
            val exception = response.toApiException()
            logApiFailure(request(), response.code(), exception)
            throw exception
        }
    }
}

private fun <T> Response<T>.toApiException(): RemoteApiException {
    val statusCode = code()
    val raw = errorBody()?.string()?.trim()
    val parsed = raw?.let {
        runCatching { gson.fromJson(it, ApiErrorResponse::class.java) }.getOrNull()
    }
    val message = parsed?.message
        ?.takeIf { it.isNotBlank() }
        ?: message().takeIf { it.isNotBlank() }
        ?: "HTTP $statusCode"
    return RemoteApiException(
        code = parsed?.code,
        statusCode = statusCode,
        rawBody = raw,
        message = message
    )
}

private fun logApiFailure(
    request: okhttp3.Request,
    statusCode: Int,
    exception: RemoteApiException
) {
    Log.e(
        TAG,
        "API 실패 ${request.method} ${request.url} / http=$statusCode / code=${exception.code ?: "UNKNOWN"} / message=${exception.message} / raw=${exception.rawBody.orEmpty()}"
    )
}
