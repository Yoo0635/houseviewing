package com.capstone.houseviewingapp.analysis

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.capstone.houseviewingapp.analysis.model.AnalysisResponse
import com.capstone.houseviewingapp.analysis.model.PdfDownloadResponse
import com.capstone.houseviewingapp.analysis.model.PreContractDiagnosisRequest
import com.capstone.houseviewingapp.data.remote.NetworkModule
import com.capstone.houseviewingapp.data.remote.executeApi
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

class RemoteAnalysisRepository : AnalysisRepository {
    private val analysisApi = NetworkModule.analysisApi
    private val gson = Gson()

    override suspend fun preContractDiagnoses(
        context: Context,
        accessToken: String,
        fileUri: String,
        request: PreContractDiagnosisRequest
    ): Result<PdfDownloadResponse> {
        val filePart = createFilePart(context, fileUri).getOrElse { return Result.failure(it) }
        val dataJson = gson.toJson(request)
        val dataBody = dataJson.toRequestBody("application/json".toMediaTypeOrNull())
        val primary = analysisApi.preContractDiagnoses(
            authorization = bearer(accessToken),
            file = filePart,
            data = dataBody
        ).executeApi()
        val remote = primary.exceptionOrNull() as? com.capstone.houseviewingapp.data.remote.RemoteApiException
        if (remote?.statusCode != 404) return primary
        return analysisApi.preContractDiagnosesAlt(
            authorization = bearer(accessToken),
            file = filePart,
            data = dataBody
        ).executeApi()
    }

    override suspend fun postContractDiagnoses(
        context: Context,
        accessToken: String,
        houseId: Long,
        fileUri: String
    ): Result<PdfDownloadResponse> {
        val filePart = createFilePart(context, fileUri).getOrElse { return Result.failure(it) }
        return analysisApi.postContractDiagnoses(
            authorization = bearer(accessToken),
            houseId = houseId,
            file = filePart
        ).executeApi()
    }

    override suspend fun changeDiagnoses(accessToken: String, houseId: Long): Result<PdfDownloadResponse> {
        return analysisApi.changeDiagnoses(
            authorization = bearer(accessToken),
            houseId = houseId
        ).executeApi()
    }

    override suspend fun getAnalyses(accessToken: String): Result<List<AnalysisResponse>> {
        return analysisApi.getAnalyses(bearer(accessToken)).executeApi()
    }

    override suspend fun getDiffAnalyses(accessToken: String): Result<List<AnalysisResponse>> {
        return analysisApi.getDiffAnalyses(bearer(accessToken)).executeApi()
    }

    private fun createFilePart(context: Context, fileUri: String): Result<MultipartBody.Part> {
        return runCatching {
            val uri = Uri.parse(fileUri)
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw IllegalStateException("FILE_OPEN_FAILED")
            val mimeType = context.contentResolver.getType(uri)
                ?: guessMimeTypeFromUri(uri)
                ?: "application/pdf"
            val fileName = "upload_${System.currentTimeMillis()}.pdf"
            val body: RequestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
            MultipartBody.Part.createFormData("file", fileName, body)
        }
    }

    private fun guessMimeTypeFromUri(uri: Uri): String? {
        val path = uri.toString()
        val ext = MimeTypeMap.getFileExtensionFromUrl(path)?.lowercase().orEmpty()
        if (ext.isBlank()) return null
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
    }

    private fun bearer(token: String): String = "Bearer $token"
}
