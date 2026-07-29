package com.house.houseviewing.domain.report.postreport.service;

import com.house.houseviewing.domain.analysis.postanalysis.entity.PostAnalysisEntity;
import com.house.houseviewing.domain.report.postreport.entity.PostReportRetryJobEntity;
import com.house.houseviewing.domain.report.postreport.enums.PostReportRetryStatus;
import com.house.houseviewing.domain.report.postreport.repository.PostReportRepository;
import com.house.houseviewing.domain.report.postreport.repository.PostReportRetryJobRepository;
import com.house.houseviewing.global.exception.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PostReportRetryService {

    public static final String RETRY_QUEUE_KEY = "post-report:retry";

    private final PostReportRetryJobRepository retryJobRepository;
    private final PostReportRepository postReportRepository;
    private final PostReportService postReportService;
    private final StringRedisTemplate stringRedisTemplate;
    private final Clock clock;

    @Transactional
    public void enqueue(PostAnalysisEntity analysis, AppException cause) {
        if (postReportRepository.findByAnalysisId(analysis.getId()).isPresent()) {
            return;
        }

        PostReportRetryJobEntity job = retryJobRepository.findByAnalysisId(analysis.getId())
                .orElseGet(() -> retryJobRepository.save(PostReportRetryJobEntity.pending(
                        analysis,
                        clock.instant(),
                        cause.getExceptionCode().getCode(),
                        cause.getMessage()
                )));

        addToRedisQueue(job);
    }

    @Scheduled(
            initialDelayString = "${pdf.retry.initial-delay-ms:30000}",
            fixedDelayString = "${pdf.retry.polling-delay-ms:30000}"
    )
    @Transactional
    public void retryDueJobs() {
        Instant now = clock.instant();
        Set<String> jobIds = stringRedisTemplate.opsForZSet()
                .rangeByScore(RETRY_QUEUE_KEY, 0, now.toEpochMilli());
        if (jobIds == null || jobIds.isEmpty()) {
            return;
        }

        for (String jobId : jobIds) {
            retryOne(Long.parseLong(jobId), now);
        }
    }

    private void retryOne(Long jobId, Instant now) {
        retryJobRepository.findById(jobId)
                .filter(job -> job.getStatus() == PostReportRetryStatus.PENDING_RETRY)
                .ifPresent(job -> {
                    if (postReportRepository.findByAnalysisId(job.getAnalysis().getId()).isPresent()) {
                        markSuccessAndRemove(job);
                        return;
                    }

                    job.markRetrying();
                    try {
                        postReportService.postRegister(job.getAnalysis());
                        markSuccessAndRemove(job);
                    } catch (RuntimeException exception) {
                        job.markRetryFailed(PostReportRetryJobEntity.asPdfFailure(exception), now);
                        if (job.getStatus() == PostReportRetryStatus.DEAD_LETTER) {
                            stringRedisTemplate.opsForZSet().remove(RETRY_QUEUE_KEY, String.valueOf(job.getId()));
                            return;
                        }
                        addToRedisQueue(job);
                    }
                });
    }

    private void markSuccessAndRemove(PostReportRetryJobEntity job) {
        job.markSuccess();
        stringRedisTemplate.opsForZSet().remove(RETRY_QUEUE_KEY, String.valueOf(job.getId()));
    }

    private void addToRedisQueue(PostReportRetryJobEntity job) {
        stringRedisTemplate.opsForZSet()
                .add(RETRY_QUEUE_KEY, String.valueOf(job.getId()), job.getNextRetryAt().toEpochMilli());
    }
}
