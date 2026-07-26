package com.capstone.houseviewingapp.analysis

import android.content.Context
import com.capstone.houseviewingapp.analysis.model.AnalysisResponse
import com.capstone.houseviewingapp.analysis.model.PdfDownloadResponse
import com.capstone.houseviewingapp.analysis.model.PreContractDiagnosisRequest
import com.capstone.houseviewingapp.analysis.model.ApiRiskLevel
import java.util.concurrent.atomic.AtomicLong

class MockAnalysisRepository : AnalysisRepository {
    private val pdfIdGen = AtomicLong(1L)
    private val records = mutableListOf<AnalysisResponse>()

    override suspend fun preContractDiagnoses(
        context: Context,
        accessToken: String,
        fileUri: String,
        request: PreContractDiagnosisRequest
    ): Result<PdfDownloadResponse> {
        if (accessToken.isBlank()) return Result.failure(IllegalStateException("UNAUTHORIZED"))
        if (fileUri.isBlank()) return Result.failure(IllegalArgumentException("FILE_REQUIRED"))
        if (request.nickname.isBlank()) return Result.failure(IllegalArgumentException("NICKNAME_REQUIRED"))
        if (request.address.isBlank()) return Result.failure(IllegalArgumentException("ADDRESS_REQUIRED"))

        val reportId = pdfIdGen.getAndIncrement()
        addRecord(reportId, request.nickname, request.address, ApiRiskLevel.WARNING)
        return Result.success(
            PdfDownloadResponse(
                pdfReportId = reportId,
                // 실제 백엔드 PdfDownloadResponse.filePath 와 동일하게, 목 구현은 URL을 만들지 않음(가짜 호스트 저장 방지)
                filePath = ""
            )
        )
    }

    override suspend fun postContractDiagnoses(
        context: Context,
        accessToken: String,
        houseId: Long,
        fileUri: String
    ): Result<PdfDownloadResponse> {
        if (accessToken.isBlank()) return Result.failure(IllegalStateException("UNAUTHORIZED"))
        if (houseId <= 0L) return Result.failure(IllegalArgumentException("HOUSE_ID_INVALID"))
        if (fileUri.isBlank()) return Result.failure(IllegalArgumentException("FILE_REQUIRED"))

        val reportId = pdfIdGen.getAndIncrement()
        addRecord(reportId, "post-$houseId", "mock address", ApiRiskLevel.SAFE)
        return Result.success(
            PdfDownloadResponse(
                pdfReportId = reportId,
                filePath = ""
            )
        )
    }

    override suspend fun changeDiagnoses(accessToken: String, houseId: Long): Result<PdfDownloadResponse> {
        if (accessToken.isBlank()) return Result.failure(IllegalStateException("UNAUTHORIZED"))
        if (houseId <= 0L) return Result.failure(IllegalArgumentException("HOUSE_ID_INVALID"))

        val reportId = pdfIdGen.getAndIncrement()
        addRecord(reportId, "change-$houseId", "mock address", ApiRiskLevel.DANGER)
        return Result.success(
            PdfDownloadResponse(
                pdfReportId = reportId,
                filePath = ""
            )
        )
    }

    override suspend fun getAnalyses(accessToken: String): Result<List<AnalysisResponse>> {
        if (accessToken.isBlank()) return Result.failure(IllegalStateException("UNAUTHORIZED"))
        return Result.success(records.toList())
    }

    override suspend fun getDiffAnalyses(accessToken: String): Result<List<AnalysisResponse>> {
        if (accessToken.isBlank()) return Result.failure(IllegalStateException("UNAUTHORIZED"))
        return Result.success(records.filter { it.riskLevel == ApiRiskLevel.DANGER })
    }

    private fun addRecord(pdfReportId: Long, nickname: String, address: String, risk: ApiRiskLevel) {
        val analysisType = if (nickname.startsWith("change-") || nickname.startsWith("post-")) "POST" else "PRE"
        records.add(
            0,
            AnalysisResponse(
                pdfReportId = pdfReportId,
                nickname = nickname,
                address = address,
                mainReason = "mock",
                riskLevel = risk,
                analysisType = analysisType,
                ltvScore = 50
            )
        )
    }
}
