package com.house.houseviewing.domain.analysis.preanalysis;

import com.house.houseviewing.api.query.service.AnalysisQueryService;
import com.house.houseviewing.domain.analysis.preanalysis.controller.PreAnalysisController;
import com.house.houseviewing.domain.analysis.preanalysis.dto.request.PreContractDiagnosisRequest;
import com.house.houseviewing.global.exception.AppException;
import com.house.houseviewing.global.exception.ExceptionCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class PreAnalysisControllerTest {

    @Mock AnalysisQueryService analysisQueryService;

    @Test
    void invalid_idempotency_key_is_rejected_before_analysis() {
        PreAnalysisController controller = new PreAnalysisController(analysisQueryService);
        PreContractDiagnosisRequest request = PreContractDiagnosisRequest.builder()
                .nickname("테스트")
                .address("서울")
                .build();

        assertThatThrownBy(() -> controller.diagnosePreContract(
                null,
                "not-a-uuid",
                new MockMultipartFile("file", "test.pdf", "application/pdf", "data".getBytes()),
                request
        ))
                .isInstanceOf(AppException.class)
                .extracting("exceptionCode")
                .isEqualTo(ExceptionCode.INVALID_IDEMPOTENCY_KEY);
        verifyNoInteractions(analysisQueryService);
    }
}
