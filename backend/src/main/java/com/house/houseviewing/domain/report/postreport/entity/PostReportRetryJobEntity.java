package com.house.houseviewing.domain.report.postreport.entity;

import com.house.houseviewing.domain.analysis.postanalysis.entity.PostAnalysisEntity;
import com.house.houseviewing.domain.common.BaseTimeEntity;
import com.house.houseviewing.domain.report.postreport.enums.PostReportRetryStatus;
import com.house.houseviewing.global.exception.AppException;
import com.house.houseviewing.global.exception.ExceptionCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(
        name = "post_report_retry_jobs",
        uniqueConstraints = @UniqueConstraint(name = "uk_post_report_retry_analysis", columnNames = "analysis_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostReportRetryJobEntity extends BaseTimeEntity {

    public static final int MAX_ATTEMPTS = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "retry_job_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id", nullable = false)
    private PostAnalysisEntity analysis;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PostReportRetryStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(nullable = false)
    private Instant nextRetryAt;

    @Column(nullable = false)
    private String lastErrorCode;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String lastErrorMessage;

    private PostReportRetryJobEntity(PostAnalysisEntity analysis, Instant nextRetryAt, String errorCode, String errorMessage) {
        this.analysis = analysis;
        this.status = PostReportRetryStatus.PENDING_RETRY;
        this.attempts = 0;
        this.nextRetryAt = nextRetryAt;
        this.lastErrorCode = errorCode;
        this.lastErrorMessage = errorMessage;
    }

    public static PostReportRetryJobEntity pending(PostAnalysisEntity analysis, Instant nextRetryAt, String errorCode, String errorMessage) {
        return new PostReportRetryJobEntity(analysis, nextRetryAt, errorCode, errorMessage);
    }

    public void markRetrying() {
        this.status = PostReportRetryStatus.RETRYING;
        this.attempts += 1;
    }

    public void markSuccess() {
        this.status = PostReportRetryStatus.SUCCESS;
    }

    public void markRetryFailed(AppException cause, Instant failedAt) {
        this.lastErrorCode = cause.getExceptionCode().getCode();
        this.lastErrorMessage = cause.getMessage();
        if (this.attempts >= MAX_ATTEMPTS) {
            this.status = PostReportRetryStatus.DEAD_LETTER;
            this.nextRetryAt = failedAt;
            return;
        }
        this.status = PostReportRetryStatus.PENDING_RETRY;
        this.nextRetryAt = failedAt.plusSeconds(backoffSeconds(this.attempts));
    }

    public static AppException asPdfFailure(RuntimeException exception) {
        if (exception instanceof AppException appException) {
            return appException;
        }
        return new AppException(ExceptionCode.PDF_GENERATION_FAILED, exception.getMessage());
    }

    private long backoffSeconds(int failedAttempts) {
        return (long) Math.pow(2, failedAttempts - 1) * 60L;
    }
}
