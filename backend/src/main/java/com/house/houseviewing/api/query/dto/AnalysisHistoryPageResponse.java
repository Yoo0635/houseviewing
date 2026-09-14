package com.house.houseviewing.api.query.dto;

import com.house.houseviewing.domain.analysis.postanalysis.dto.response.AnalysisResponse;

import java.util.List;

public record AnalysisHistoryPageResponse(
        List<AnalysisResponse> items,
        Long nextOffset,
        boolean hasNext
) {
}
