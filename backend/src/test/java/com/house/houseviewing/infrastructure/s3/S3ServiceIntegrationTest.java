package com.house.houseviewing.infrastructure.s3;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.house.houseviewing.global.file.pdf.dto.PdfUploadResult;
import io.awspring.cloud.s3.InMemoryBufferingS3OutputStreamProvider;
import io.awspring.cloud.s3.Jackson2JsonS3ObjectConverter;
import io.awspring.cloud.s3.PropertiesS3ObjectContentTypeResolver;
import io.awspring.cloud.s3.S3ObjectConverter;
import io.awspring.cloud.s3.S3OutputStreamProvider;
import io.awspring.cloud.s3.S3Resource;
import io.awspring.cloud.s3.S3Template;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class S3ServiceIntegrationTest {

    private static final String BUCKET_NAME = "house-viewing-storage-test";

    private LocalStackContainer localStackContainer;
    private S3Client s3Client;
    private S3Presigner s3Presigner;
    private S3Service s3Service;

    @BeforeAll
    void setUpContainer() {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(), "Docker is required for S3 integration test.");

        localStackContainer = new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.8.1"))
                .withServices(LocalStackContainer.Service.S3);
        localStackContainer.start();

        s3Client = S3Client.builder()
                .endpointOverride(localStackContainer.getEndpointOverride(LocalStackContainer.Service.S3))
                .region(Region.of(localStackContainer.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(localStackContainer.getAccessKey(), localStackContainer.getSecretKey())))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();

        s3Client.createBucket(CreateBucketRequest.builder().bucket(BUCKET_NAME).build());
    }

    @BeforeEach
    void setUpService() {
        if (s3Presigner != null) {
            s3Presigner.close();
        }

        s3Presigner = S3Presigner.builder()
                .endpointOverride(localStackContainer.getEndpointOverride(LocalStackContainer.Service.S3))
                .region(Region.of(localStackContainer.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(localStackContainer.getAccessKey(), localStackContainer.getSecretKey())))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();

        S3ObjectConverter objectConverter = new Jackson2JsonS3ObjectConverter(new ObjectMapper());
        S3OutputStreamProvider outputStreamProvider = new InMemoryBufferingS3OutputStreamProvider(
                s3Client,
                new PropertiesS3ObjectContentTypeResolver()
        );
        S3Template s3Template = new S3Template(s3Client, outputStreamProvider, objectConverter, s3Presigner);

        s3Service = new S3Service(s3Template);
        ReflectionTestUtils.setField(s3Service, "bucket", BUCKET_NAME);
    }

    @AfterAll
    void tearDown() {
        if (s3Presigner != null) {
            s3Presigner.close();
        }
        if (s3Client != null) {
            s3Client.close();
        }
        if (localStackContainer != null) {
            localStackContainer.stop();
        }
    }

    @Nested
    @DisplayName("PDF 업로드")
    class PdfUpload {

        @Test
        @DisplayName("성공 - LocalStack S3에 PDF 업로드 후 파일 검증")
        void 업로드_성공() {
            byte[] pdf = "integration-pdf-data".getBytes(StandardCharsets.UTF_8);

            PdfUploadResult result = s3Service.pdfUpload(pdf);

            HeadObjectResponse headObject = s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(BUCKET_NAME)
                    .key(result.getPdfKey())
                    .build());
            ResponseBytes<GetObjectResponse> uploadedObject = s3Client.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(BUCKET_NAME)
                    .key(result.getPdfKey())
                    .build());

            assertThat(result.getPdfKey()).startsWith("analysis_").endsWith("originFileName.pdf");
            assertThat(result.getPdfPath()).isNotBlank();
            assertThat(result.getPdfSizeBytes()).isEqualTo((long) pdf.length);
            assertThat(result.getPdfName()).isEqualTo("안전_진단_리포트.pdf");
            assertThat(headObject.contentType()).isEqualTo("application/pdf");
            assertThat(uploadedObject.asByteArray()).isEqualTo(pdf);
        }

        @Test
        @DisplayName("성공 - 빈 PDF 파일 업로드")
        void 빈_파일_업로드_성공() {
            byte[] emptyPdf = new byte[0];

            PdfUploadResult result = s3Service.pdfUpload(emptyPdf);

            HeadObjectResponse headObject = s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(BUCKET_NAME)
                    .key(result.getPdfKey())
                    .build());

            assertThat(result.getPdfSizeBytes()).isEqualTo(0L);
            assertThat(headObject.contentLength()).isEqualTo(0L);
        }

        @Test
        @DisplayName("성공 - 여러 PDF 파일 동시 업로드")
        void 여러_파일_동시_업로드() {
            List<byte[]> pdfFiles = List.of(
                    "pdf-file-1".getBytes(StandardCharsets.UTF_8),
                    "pdf-file-2".getBytes(StandardCharsets.UTF_8),
                    "pdf-file-3".getBytes(StandardCharsets.UTF_8)
            );

            List<PdfUploadResult> results = new ArrayList<>();
            for (byte[] pdf : pdfFiles) {
                results.add(s3Service.pdfUpload(pdf));
            }

            assertThat(results).hasSize(3);
            assertThat(results).extracting("pdfKey").doesNotHaveDuplicates();
            results.forEach(result -> {
                assertThat(result.getPdfKey()).startsWith("analysis_").endsWith("originFileName.pdf");
                assertThat(result.getPdfPath()).isNotBlank();
            });
        }

        @Test
        @DisplayName("성공 - 큰 PDF 파일 업로드")
        void 큰_파일_업로드_성공() {
            byte[] largePdf = new byte[1024 * 1024];
            for (int i = 0; i < largePdf.length; i++) {
                largePdf[i] = (byte) (i % 256);
            }

            PdfUploadResult result = s3Service.pdfUpload(largePdf);

            ResponseBytes<GetObjectResponse> uploadedObject = s3Client.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(BUCKET_NAME)
                    .key(result.getPdfKey())
                    .build());

            assertThat(result.getPdfSizeBytes()).isEqualTo((long) largePdf.length);
            assertThat(uploadedObject.asByteArray()).isEqualTo(largePdf);
        }

        @Test
        @DisplayName("성공 - 업로드된 파일의 URL 통해 접근 가능")
        void URL_접근_확인() throws IOException {
            byte[] pdf = "test-pdf-for-url".getBytes(StandardCharsets.UTF_8);

            PdfUploadResult result = s3Service.pdfUpload(pdf);

            assertThat(result.getPdfPath()).contains(result.getPdfKey());
            assertThat(new java.net.URL(result.getPdfPath())).isNotNull();
        }

        @Test
        @DisplayName("성공 - 파일 다운로드 후 내용 일치 확인")
        void 파일_다운로드_내용_일치() {
            String originalContent = "Safe diagnostic report content with special chars: 한글, 日本語, 🎉";
            byte[] pdf = originalContent.getBytes(StandardCharsets.UTF_8);

            PdfUploadResult result = s3Service.pdfUpload(pdf);

            ResponseBytes<GetObjectResponse> downloaded = s3Client.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(BUCKET_NAME)
                    .key(result.getPdfKey())
                    .build());

            assertThat(new String(downloaded.asByteArray(), StandardCharsets.UTF_8)).isEqualTo(originalContent);
        }

        @Test
        @DisplayName("성공 - 파일 메타데이터 정확성 검증")
        void 메타데이터_정확성() {
            byte[] pdf = "metadata-test".getBytes(StandardCharsets.UTF_8);

            PdfUploadResult result = s3Service.pdfUpload(pdf);

            HeadObjectResponse headObject = s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(BUCKET_NAME)
                    .key(result.getPdfKey())
                    .build());

            assertThat(headObject.contentType()).isEqualTo("application/pdf");
            assertThat(headObject.contentLength()).isEqualTo((long) pdf.length);
        }
    }

    @Nested
    @DisplayName("S3 직접 조작")
    class S3DirectOperation {

        @Test
        @DisplayName("업로드된 파일이 버킷에 실제로 존재")
        void 파일_존재_확인() {
            byte[] pdf = "existence-check".getBytes(StandardCharsets.UTF_8);

            PdfUploadResult result = s3Service.pdfUpload(pdf);

            HeadObjectResponse headObject = s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(BUCKET_NAME)
                    .key(result.getPdfKey())
                    .build());

            assertThat(headObject.eTag()).isNotNull();
        }

        @Test
        @DisplayName("존재하지 않는 파일 조회 시 예외 발생")
        void 존재하지_않는_파일_조회_시_예외() {
            assertThatThrownBy(() -> s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(BUCKET_NAME)
                    .key("non-existent-file.pdf")
                    .build()))
                    .isInstanceOf(NoSuchKeyException.class);
        }
    }
}
