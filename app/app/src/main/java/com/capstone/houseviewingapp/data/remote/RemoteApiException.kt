package com.capstone.houseviewingapp.data.remote

class RemoteApiException(
    val code: String?,
    val statusCode: Int?,
    val rawBody: String?,
    override val message: String
) : Exception(message)
