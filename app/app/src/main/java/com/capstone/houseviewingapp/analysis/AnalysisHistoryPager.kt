package com.capstone.houseviewingapp.analysis

import com.capstone.houseviewingapp.analysis.model.ApiRiskLevel

class AnalysisHistoryPager(
    private val repository: AnalysisRepository
) {
    private data class Key(val source: RecordSource, val riskLevel: ApiRiskLevel?)
    private data class State(
        val items: List<AnalysisRecordItem> = emptyList(),
        val nextOffset: Long = 0L,
        val hasNext: Boolean = true,
        val loading: Boolean = false
    )

    private val states = mutableMapOf<Key, State>()

    fun reset(source: RecordSource, riskLevel: ApiRiskLevel?) {
        states.remove(key(source, riskLevel))
    }

    suspend fun loadNext(
        accessToken: String,
        source: RecordSource,
        riskLevel: ApiRiskLevel?
    ): Result<List<AnalysisRecordItem>> {
        val key = key(source, riskLevel)
        val state = states[key] ?: State()
        if (state.loading || !state.hasNext) return Result.success(state.items)

        states[key] = state.copy(loading = true)
        val result = when (source) {
            RecordSource.MANUAL -> repository.getAnalyses(accessToken, state.nextOffset, riskLevel)
            RecordSource.AUTO -> repository.getDiffAnalyses(accessToken, state.nextOffset, riskLevel)
        }

        return result.fold(
            onSuccess = { page ->
                val incoming = page.items.map { it.toRecordItem(source) }
                val items = if (state.nextOffset == 0L) incoming else state.items + incoming
                states[key] = State(
                    items = items,
                    nextOffset = page.nextOffset ?: state.nextOffset + incoming.size,
                    hasNext = page.hasNext,
                    loading = false
                )
                Result.success(items)
            },
            onFailure = {
                states[key] = state.copy(loading = false)
                Result.failure(it)
            }
        )
    }

    private fun key(source: RecordSource, riskLevel: ApiRiskLevel?): Key = Key(source, riskLevel)
}
