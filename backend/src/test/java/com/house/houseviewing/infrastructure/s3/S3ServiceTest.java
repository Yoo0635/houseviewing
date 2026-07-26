package com.house.houseviewing.infrastructure.s3;

import com.house.houseviewing.global.exception.AppException;
import com.house.houseviewing.global.exception.ExceptionCode;
import com.house.houseviewing.global.file.pdf.dto.PdfUploadResult;
import io.awspring.cloud.s3.ObjectMetadata;
import io.awspring.cloud.s3.S3Resource;
import io.awspring.cloud.s3.S3Template;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class S3ServiceTest {

    @InjectMocks
    private S3Service s3Service;

    @Mock
    private S3Template s3Template;

    @Mock
    private S3Resource s3Resource;

    @Nested 
    @DisplayName("PDF 업로드")
    class PdfUpload {

        @BeforeEach
        void setUp() {
            ReflectionTestUtils.setField(s3Service, "bucket", "test-bucket");
        }

        @Test
        @DisplayName("성공 - 파일명 패턴 및 모든 필드 정확히 반환")
        void 성공() throws Exception {
            byte[] pdf = "pdf-data".getBytes(StandardCharsets.UTF_8);
            URL expectedUrl = new URL("https://test-bucket.s3.amazonaws.com/analysis_uuid-originFileName.pdf");

            given(s3Template.upload(anyString(), anyString(), any(ByteArrayInputStream.class), any(ObjectMetadata.class)))
                    .willReturn(s3Resource);
            given(s3Resource.getURL()).willReturn(expectedUrl);

            PdfUploadResult result = s3Service.pdfUpload(pdf);

            ArgumentCaptor<String> bucketCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<ObjectMetadata> metadataCaptor = ArgumentCaptor.forClass(ObjectMetadata.class);
            verify(s3Template).upload(bucketCaptor.capture(), keyCaptor.capture(), any(ByteArrayInputStream.class), metadataCaptor.capture());

            assertThat(bucketCaptor.getValue()).isEqualTo("test-bucket");
            assertThat(keyCaptor.getValue()).matches("analysis_[a-f0-9-]+originFileName\\.pdf");
            assertThat(metadataCaptor.getValue().getContentType()).isEqualTo("application/pdf");
            assertThat(result.getPdfKey()).isEqualTo(keyCaptor.getValue());
            assertThat(result.getPdfPath()).isEqualTo(expectedUrl.toString());
            assertThat(result.getPdfSizeBytes()).isEqualTo((long) pdf.length);
            assertThat(result.getPdfName()).isEqualTo("안전_진단_리포트.pdf");
        }

        @Test
        @DisplayName("성공 - 빈 PDF 배열도 업로드 가능")
        void 빈_배열_업로드_성공() throws Exception {
            byte[] emptyPdf = new byte[0];
            URL expectedUrl = new URL("https://test-bucket.s3.amazonaws.com/analysis_uuid-originFileName.pdf");

            given(s3Template.upload(anyString(), anyString(), any(ByteArrayInputStream.class), any(ObjectMetadata.class)))
                    .willReturn(s3Resource);
            given(s3Resource.getURL()).willReturn(expectedUrl);

            PdfUploadResult result = s3Service.pdfUpload(emptyPdf);

            assertThat(result.getPdfKey()).startsWith("analysis_").endsWith("originFileName.pdf");
            assertThat(result.getPdfSizeBytes()).isEqualTo(0L);
        }

        @Test
        @DisplayName("성공 - UUID가 포함된 고유한 파일명 생성")
        void 고유_파일명_생성() throws Exception {
            byte[] pdf = "pdf-data".getBytes(StandardCharsets.UTF_8);

            given(s3Template.upload(anyString(), anyString(), any(ByteArrayInputStream.class), any(ObjectMetadata.class)))
                    .willReturn(s3Resource);
            given(s3Resource.getURL()).willReturn(new URL("https://example.com/test.pdf"));

            PdfUploadResult result1 = s3Service.pdfUpload(pdf);
            PdfUploadResult result2 = s3Service.pdfUpload(pdf);

            assertThat(result1.getPdfKey()).isNotEqualTo(result2.getPdfKey());
            UUID.fromString(result1.getPdfKey().replace("analysis_", "").replace("originFileName.pdf", ""));
        }

        @Test
        @DisplayName("실패 - URL 가져오기 실패 시 IOException → AppException 변환")
        void URL_가져오기_실패() throws Exception {
            given(s3Template.upload(anyString(), anyString(), any(ByteArrayInputStream.class), any(ObjectMetadata.class)))
                    .willReturn(s3Resource);
            given(s3Resource.getURL()).willThrow(new java.net.MalformedURLException("URL generation failed"));

            assertThatThrownBy(() -> s3Service.pdfUpload("pdf-data".getBytes(StandardCharsets.UTF_8)))
                    .isInstanceOf(AppException.class)
                    .extracting("exceptionCode")
                    .isEqualTo(ExceptionCode.S3_UPLOAD_FAILED);
        }

        @Test
        @DisplayName("성공 - S3Template.upload에 전달되는 파라미터 정확성")
        void upload_파라미터_정확성() throws Exception {
            byte[] pdf = "test-pdf-content".getBytes(StandardCharsets.UTF_8);

            given(s3Template.upload(anyString(), anyString(), any(ByteArrayInputStream.class), any(ObjectMetadata.class)))
                    .willReturn(s3Resource);
            given(s3Resource.getURL()).willReturn(new URL("https://example.com/test.pdf"));

            s3Service.pdfUpload(pdf);

            ArgumentCaptor<ObjectMetadata> metadataCaptor = ArgumentCaptor.forClass(ObjectMetadata.class);
            verify(s3Template).upload(
                    org.mockito.ArgumentMatchers.eq("test-bucket"),
                    org.mockito.ArgumentMatchers.matches("analysis_.+originFileName\\.pdf"),
                    any(ByteArrayInputStream.class),
                    metadataCaptor.capture()
            );

            ObjectMetadata capturedMetadata = metadataCaptor.getValue();
            assertThat(capturedMetadata.getContentType()).isEqualTo("application/pdf");
        }
    }
}
