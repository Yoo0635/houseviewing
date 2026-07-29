package com.house.houseviewing.infrastructure.python;

import com.house.houseviewing.global.exception.AppException;
import com.house.houseviewing.global.exception.ExceptionCode;
import com.house.houseviewing.global.file.pdf.dto.PdfPostReportRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PythonEngineClientTest {

    @Test
    @DisplayName("PDF API 타임아웃은 PDF_GENERATION_FAILED로 변환")
    void pdf_api_timeout_is_mapped_to_pdf_generation_failed(){
        WebClient webClient = WebClient.builder()
                .exchangeFunction(request -> Mono.error(new TimeoutException("read timeout")))
                .build();
        PythonEngineClient client = new PythonEngineClient(webClient);

        assertThatThrownBy(() -> client.postSendDataAndReceivePdf(PdfPostReportRequest.builder().build()).block())
                .isInstanceOf(AppException.class)
                .extracting("exceptionCode")
                .isEqualTo(ExceptionCode.PDF_GENERATION_FAILED);
    }
}
