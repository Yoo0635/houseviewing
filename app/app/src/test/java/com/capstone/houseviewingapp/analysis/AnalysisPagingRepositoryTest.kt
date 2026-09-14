package com.capstone.houseviewingapp.analysis

import android.content.Context
import com.capstone.houseviewingapp.analysis.model.AnalysisHistoryPageResponse
import com.capstone.houseviewingapp.analysis.model.AnalysisResponse
import com.capstone.houseviewingapp.analysis.model.ApiRiskLevel
import com.capstone.houseviewingapp.analysis.model.PdfDownloadResponse
import com.capstone.houseviewingapp.analysis.model.PreContractDiagnosisRequest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalysisPagingRepositoryTest {
    @Test
    fun pageResponseParsesNextOffsetAndHasNext() {
        val first = AnalysisHistoryPageResponse(listOf(response(1)), nextOffset = 10L, hasNext = true)
        val last = AnalysisHistoryPageResponse(emptyList(), nextOffset = null, hasNext = false)

        assertEquals(10L, first.nextOffset)
        assertEquals(null, last.nextOffset)
        assertFalse(last.hasNext)
    }

    @Test
    fun forwardsOffsetAndRiskFilterToManualRepository() = runBlocking {
        val repository = FakeAnalysisRepository()
        repository.manualResults += Result.success(page(2, nextOffset = 20L, hasNext = true))
        val pager = AnalysisHistoryPager(repository)

        pager.loadNext("token", RecordSource.MANUAL, ApiRiskLevel.DANGER)

        assertEquals(listOf(Request(offset = 0L, riskLevel = ApiRiskLevel.DANGER)), repository.manualRequests)
    }

    @Test
    fun forwardsOffsetAndRiskFilterToDiffRepository() = runBlocking {
        val repository = FakeAnalysisRepository()
        repository.diffResults += Result.success(page(3, nextOffset = 30L, hasNext = true))
        val pager = AnalysisHistoryPager(repository)

        pager.loadNext("token", RecordSource.AUTO, ApiRiskLevel.WARNING)

        assertEquals(listOf(Request(offset = 0L, riskLevel = ApiRiskLevel.WARNING)), repository.diffRequests)
    }

    @Test
    fun replacesItemsAtOffsetZeroAndAppendsLaterPages() = runBlocking {
        val repository = FakeAnalysisRepository()
        repository.manualResults += Result.success(page(1, nextOffset = 10L, hasNext = true))
        repository.manualResults += Result.success(page(2, nextOffset = 20L, hasNext = false))
        val pager = AnalysisHistoryPager(repository)

        assertEquals(listOf(record(1)), pager.loadNext("token", RecordSource.MANUAL, null).getOrThrow())
        assertEquals(
            listOf(record(1), record(2)),
            pager.loadNext("token", RecordSource.MANUAL, null).getOrThrow()
        )
        pager.reset(RecordSource.MANUAL, null)
        repository.manualResults += Result.success(page(9, nextOffset = 10L, hasNext = false))

        assertEquals(listOf(record(9)), pager.loadNext("token", RecordSource.MANUAL, null).getOrThrow())
    }

    @Test
    fun suppressesConcurrentLoadsForSameTabAndFilter() = runBlocking {
        val gate = CompletableDeferred<AnalysisHistoryPageResponse>()
        val repository = FakeAnalysisRepository()
        repository.manualDeferred += gate
        val pager = AnalysisHistoryPager(repository)

        val first = async(start = CoroutineStart.UNDISPATCHED) {
            pager.loadNext("token", RecordSource.MANUAL, ApiRiskLevel.SAFE)
        }
        val second = async { pager.loadNext("token", RecordSource.MANUAL, ApiRiskLevel.SAFE) }
        gate.complete(page(4, nextOffset = 10L, hasNext = true))

        assertEquals(listOf(record(4)), first.await().getOrThrow())
        assertEquals(emptyList<AnalysisRecordItem>(), second.await().getOrThrow())
        assertEquals(1, repository.manualRequests.size)
    }

    @Test
    fun preservesItemsAndOffsetAfterFailure() = runBlocking {
        val repository = FakeAnalysisRepository()
        repository.manualResults += Result.success(page(5, nextOffset = 10L, hasNext = true))
        repository.manualResults += Result.failure(IllegalStateException("boom"))
        repository.manualResults += Result.success(page(6, nextOffset = 20L, hasNext = false))
        val pager = AnalysisHistoryPager(repository)

        assertEquals(listOf(record(5)), pager.loadNext("token", RecordSource.MANUAL, null).getOrThrow())
        assertTrue(pager.loadNext("token", RecordSource.MANUAL, null).isFailure)

        assertEquals(
            listOf(record(5), record(6)),
            pager.loadNext("token", RecordSource.MANUAL, null).getOrThrow()
        )
        assertEquals(listOf(0L, 10L, 10L), repository.manualRequests.map { it.offset })
    }

    @Test
    fun stopsLoadingWhenLastPageHasNoNext() = runBlocking {
        val repository = FakeAnalysisRepository()
        repository.diffResults += Result.success(page(7, nextOffset = 10L, hasNext = false))
        val pager = AnalysisHistoryPager(repository)

        assertEquals(listOf(record(7, RecordSource.AUTO)), pager.loadNext("token", RecordSource.AUTO, null).getOrThrow())
        assertEquals(listOf(record(7, RecordSource.AUTO)), pager.loadNext("token", RecordSource.AUTO, null).getOrThrow())
        assertEquals(1, repository.diffRequests.size)
    }

    @Test
    fun resetsToOffsetZeroAfterTabOrFilterChange() = runBlocking {
        val repository = FakeAnalysisRepository()
        repository.manualResults += Result.success(page(8, nextOffset = 10L, hasNext = true))
        repository.manualResults += Result.success(page(9, nextOffset = 10L, hasNext = false))
        val pager = AnalysisHistoryPager(repository)

        pager.loadNext("token", RecordSource.MANUAL, ApiRiskLevel.SAFE)
        pager.reset(RecordSource.MANUAL, ApiRiskLevel.DANGER)
        pager.loadNext("token", RecordSource.MANUAL, ApiRiskLevel.DANGER)

        assertEquals(
            listOf(
                Request(offset = 0L, riskLevel = ApiRiskLevel.SAFE),
                Request(offset = 0L, riskLevel = ApiRiskLevel.DANGER)
            ),
            repository.manualRequests
        )
    }

    private data class Request(val offset: Long, val riskLevel: ApiRiskLevel?)

    private class FakeAnalysisRepository : AnalysisRepository {
        val manualRequests = mutableListOf<Request>()
        val diffRequests = mutableListOf<Request>()
        val manualResults = ArrayDeque<Result<AnalysisHistoryPageResponse>>()
        val diffResults = ArrayDeque<Result<AnalysisHistoryPageResponse>>()
        val manualDeferred = ArrayDeque<CompletableDeferred<AnalysisHistoryPageResponse>>()

        override suspend fun preContractDiagnoses(
            context: Context,
            accessToken: String,
            idempotencyKey: String,
            fileUri: String,
            request: PreContractDiagnosisRequest
        ): Result<PdfDownloadResponse> = error("unused")

        override suspend fun postContractDiagnoses(
            context: Context,
            accessToken: String,
            houseId: Long,
            fileUri: String
        ): Result<PdfDownloadResponse> = error("unused")

        override suspend fun changeDiagnoses(accessToken: String, houseId: Long): Result<PdfDownloadResponse> {
            return error("unused")
        }

        override suspend fun getAnalyses(
            accessToken: String,
            offset: Long,
            riskLevel: ApiRiskLevel?
        ): Result<AnalysisHistoryPageResponse> {
            manualRequests += Request(offset, riskLevel)
            manualDeferred.removeFirstOrNull()?.let { return Result.success(it.await()) }
            return manualResults.removeFirst()
        }

        override suspend fun getDiffAnalyses(
            accessToken: String,
            offset: Long,
            riskLevel: ApiRiskLevel?
        ): Result<AnalysisHistoryPageResponse> {
            diffRequests += Request(offset, riskLevel)
            return diffResults.removeFirst()
        }
    }

    private fun page(id: Long, nextOffset: Long, hasNext: Boolean): AnalysisHistoryPageResponse {
        return AnalysisHistoryPageResponse(listOf(response(id)), nextOffset, hasNext)
    }

    private fun response(id: Long): AnalysisResponse {
        return AnalysisResponse(
            pdfReportId = id,
            nickname = "home-$id",
            address = "address-$id",
            mainReason = "reason-$id",
            riskLevel = ApiRiskLevel.SAFE,
            analysisType = "PRE",
            ltvScore = id.toInt()
        )
    }

    private fun record(id: Long, source: RecordSource = RecordSource.MANUAL): AnalysisRecordItem {
        return AnalysisRecordItem(
            title = "home-$id",
            address = "address-$id",
            riskSummary = "reason-$id",
            level = RiskLevel.BLUE,
            source = source,
            ltv = id.toDouble(),
            pdfReportId = id
        )
    }
}
