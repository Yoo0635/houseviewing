package com.capstone.houseviewingapp.analysis

import android.content.Context
import com.capstone.houseviewingapp.analysis.model.AnalysisResponse
import com.capstone.houseviewingapp.analysis.model.PdfDownloadResponse
import com.capstone.houseviewingapp.analysis.model.PreContractDiagnosisRequest

interface AnalysisRepository {
    suspend fun preContractDiagnoses(
        context: Context,
        accessToken: String,
        fileUri: String,
        request: PreContractDiagnosisRequest
    ): Result<PdfDownloadResponse>

    suspend fun postContractDiagnoses(
        context: Context,
        accessToken: String,
        houseId: Long,
        fileUri: String
    ): Result<PdfDownloadResponse>

    suspend fun changeDiagnoses(
        accessToken: String,
        houseId: Long
    ): Result<PdfDownloadResponse>

    suspend fun getAnalyses(accessToken: String): Result<List<AnalysisResponse>>
    suspend fun getDiffAnalyses(accessToken: String): Result<List<AnalysisResponse>>
}
