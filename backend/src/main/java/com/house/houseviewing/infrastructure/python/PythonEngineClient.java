package com.house.houseviewing.infrastructure.python;

import com.house.houseviewing.global.file.diff.dto.DiffAnalysisResult;
import com.house.houseviewing.global.file.pdf.dto.PdfDiffReportRequest;
import com.house.houseviewing.global.file.pdf.dto.PdfPostReportRequest;
import com.house.houseviewing.global.file.pdf.dto.PdfPreReportRequest;
import com.house.houseviewing.global.file.snapshot.dto.SnapshotAnalysisResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.house.houseviewing.global.exception.AppException;
import com.house.houseviewing.global.exception.ExceptionCode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class PythonEngineClient {

    private final WebClient pythonWebClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PythonEngineClient(@Qualifier("pythonWebClient") WebClient pythonWebClient) {
        this.pythonWebClient = pythonWebClient;
    }

    public Mono<SnapshotAnalysisResult> sendSnapshot(MultipartFile file){
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", file.getResource())
                .filename(file.getOriginalFilename());

        return pythonWebClient.post()
                .uri("/engine/analyze")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .retrieve()
                .bodyToMono(SnapshotAnalysisResult.class);
    }

    public Mono<byte[]> postSendDataAndReceivePdf(PdfPostReportRequest request){
        return sendPdfRequest("/engine/generate-pdf", request);
    }

    public Mono<byte[]> preSendDataAndReceivePdf(PdfPreReportRequest request){
        return sendPdfRequest("/engine/generate-pdf", request);
    }

    public Mono<DiffAnalysisResult> diffSendSnapshot(String snapshot){
        return pythonWebClient.post()
                .uri("/engine/analyze/mock")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(snapshot)
                .retrieve()
                .bodyToMono(DiffAnalysisResult.class);
    }

    public Mono<byte[]> diffSendDataAndReceivePdf(PdfDiffReportRequest request){
        return sendPdfRequest("/engine/generate-pdf/diff", request);
    }

    private Mono<byte[]> sendPdfRequest(String uri, Object request) {
        return pythonWebClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        return response.bodyToMono(byte[].class);
                    }
                    return response.bodyToMono(String.class)
                            .defaultIfEmpty("")
                            .flatMap(body -> Mono.error(toPdfException(response.statusCode(), body)));
                });
    }

    private RuntimeException toPdfException(HttpStatusCode statusCode, String body) {
        String message = extractErrorMessage(body);

        if (statusCode.value() == 422) {
            return new AppException(ExceptionCode.INVALID_PDF_REQUEST, message);
        }
        if (statusCode.is5xxServerError()) {
            return new AppException(ExceptionCode.PDF_GENERATION_FAILED, message);
        }
        return new AppException(ExceptionCode.PDF_SAVE_FAILED, message);
    }

    private String extractErrorMessage(String body) {
        if (body == null || body.isBlank()) {
            return ExceptionCode.PDF_GENERATION_FAILED.getMessage();
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            if (root.hasNonNull("message")) {
                return root.get("message").asText();
            }
            if (root.has("detail")) {
                JsonNode detail = root.get("detail");
                if (detail.isTextual()) {
                    return detail.asText();
                }
                if (detail.isArray() && !detail.isEmpty()) {
                    JsonNode first = detail.get(0);
                    if (first.hasNonNull("msg")) {
                        return first.get("msg").asText();
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return body;
    }
}
