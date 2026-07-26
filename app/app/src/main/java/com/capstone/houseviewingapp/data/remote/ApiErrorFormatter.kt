package com.capstone.houseviewingapp.data.remote

object ApiErrorFormatter {
    fun withCode(prefix: String, throwable: Throwable): String {
        val remote = throwable as? RemoteApiException
        if (remote != null) {
            val code = remote.code?.takeIf { it.isNotBlank() } ?: "UNKNOWN"
            val http = remote.statusCode?.toString() ?: "?"
            val detail = remote.message.trim().replace("\n", " ").take(140)
            val raw = remote.rawBody
                ?.replace("\n", " ")
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.take(140)
            val header = "$prefix (코드: $code, HTTP: $http)"
            return when {
                detail.isNotBlank() -> "$header\n$detail"
                raw != null -> "$header\n$raw"
                else -> header
            }
        }
        val message = throwable.message?.trim().orEmpty()
        return if (message.isNotBlank()) "$prefix\n$message" else prefix
    }
}
