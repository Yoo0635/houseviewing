package com.house.houseviewing.infrastructure.s3;

import com.house.houseviewing.global.exception.AppException;
import com.house.houseviewing.global.exception.ExceptionCode;
import com.house.houseviewing.global.file.pdf.dto.PdfUploadResult;
import io.awspring.cloud.s3.ObjectMetadata;
import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Template s3Template;

    @Setter
    @Autowired(required = false)
    private S3Presigner s3Presigner;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;

    /** Presigned URL 유효 기간 (기본 7일) */
    private static final Duration PRESIGNED_DURATION = Duration.ofDays(7);

    public PdfUploadResult pdfUpload(byte[] pdf){
        String s3FileName = "analysis_" + UUID.randomUUID() + ".pdf";
        try{
            s3Template.upload(bucket, s3FileName, new ByteArrayInputStream(pdf),
                    ObjectMetadata.builder().contentType("application/pdf").build());

            String pdfUrl = generatePresignedUrl(s3FileName);

            return PdfUploadResult.builder()
                    .pdfKey(s3FileName)
                    .pdfPath(pdfUrl)
                    .pdfSizeBytes((long) pdf.length)
                    .pdfName("안전_진단_리포트.pdf")
                    .build();
        } catch (Exception e){
            throw new AppException(ExceptionCode.S3_UPLOAD_FAILED);
        }
    }

    /**
     * S3 객체에 대한 Presigned GET URL을 생성합니다.
     * S3Presigner 빈이 없는 환경(로컬 등)에서는 일반 S3 URL을 반환합니다.
     */
    private String generatePresignedUrl(String key) {
        if (s3Presigner == null) {
            // S3Presigner 빈이 없는 경우 (로컬/테스트) 기본 URL 형태 반환
            return "https://" + bucket + ".s3.amazonaws.com/" + key;
        }
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(PRESIGNED_DURATION)
                .getObjectRequest(r -> r.bucket(bucket).key(key))
                .build();
        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }
}
