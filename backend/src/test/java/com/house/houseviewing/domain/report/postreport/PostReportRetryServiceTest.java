package com.house.houseviewing.domain.report.postreport;

import com.house.houseviewing.domain.analysis.postanalysis.entity.PostAnalysisEntity;
import com.house.houseviewing.domain.report.postreport.entity.PostReportEntity;
import com.house.houseviewing.domain.report.postreport.entity.PostReportRetryJobEntity;
import com.house.houseviewing.domain.report.postreport.enums.PostReportRetryStatus;
import com.house.houseviewing.domain.report.postreport.repository.PostReportRepository;
import com.house.houseviewing.domain.report.postreport.repository.PostReportRetryJobRepository;
import com.house.houseviewing.domain.report.postreport.service.PostReportRetryService;
import com.house.houseviewing.domain.report.postreport.service.PostReportService;
import com.house.houseviewing.global.exception.AppException;
import com.house.houseviewing.global.exception.ExceptionCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class PostReportRetryServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-07-29T00:00:00Z"),
            ZoneId.of("UTC")
    );

    PostReportRetryService retryService;

    @Mock PostReportRetryJobRepository retryJobRepository;
    @Mock PostReportRepository postReportRepository;
    @Mock PostReportService postReportService;
    @Mock StringRedisTemplate stringRedisTemplate;
    @Mock ZSetOperations<String, String> zSetOperations;

    @BeforeEach
    void setUp() {
        retryService = new PostReportRetryService(
                retryJobRepository,
                postReportRepository,
                postReportService,
                stringRedisTemplate,
                FIXED_CLOCK
        );
    }

    @Nested
    @DisplayName("PDF 재시도 작업 등록")
    class Enqueue {

        @Test
        @DisplayName("실패한 분석을 PENDING_RETRY로 저장하고 Redis Sorted Set에 적재")
        void 실패한_분석을_재시도_큐에_적재(){
            PostAnalysisEntity analysis = mockAnalysis(1L);
            AppException cause = new AppException(ExceptionCode.PDF_GENERATION_FAILED, "python 503");
            given(retryJobRepository.findByAnalysisId(1L)).willReturn(Optional.empty());
            given(retryJobRepository.save(any(PostReportRetryJobEntity.class))).willAnswer(invocation -> {
                PostReportRetryJobEntity job = invocation.getArgument(0);
                setId(job, 10L);
                return job;
            });
            given(stringRedisTemplate.opsForZSet()).willReturn(zSetOperations);

            retryService.enqueue(analysis, cause);

            ArgumentCaptor<PostReportRetryJobEntity> jobCaptor = ArgumentCaptor.forClass(PostReportRetryJobEntity.class);
            then(retryJobRepository).should().save(jobCaptor.capture());
            PostReportRetryJobEntity saved = jobCaptor.getValue();
            assertThat(saved.getStatus()).isEqualTo(PostReportRetryStatus.PENDING_RETRY);
            assertThat(saved.getAttempts()).isZero();
            assertThat(saved.getLastErrorCode()).isEqualTo("ER005");
            then(zSetOperations).should().add(eq("post-report:retry"), eq("10"), anyDouble());
        }
    }

    @Nested
    @DisplayName("PDF 재시도 실행")
    class Retry {

        @Test
        @DisplayName("Redis에서 만기 작업을 꺼내 PDF 생성 성공 시 SUCCESS로 전환")
        void 재처리_성공(){
            PostAnalysisEntity analysis = mockAnalysis(1L);
            PostReportRetryJobEntity job = PostReportRetryJobEntity.pending(analysis, FIXED_CLOCK.instant(), "ER005", "python 503");
            setId(job, 10L);
            given(stringRedisTemplate.opsForZSet()).willReturn(zSetOperations);
            given(zSetOperations.rangeByScore("post-report:retry", 0, FIXED_CLOCK.instant().toEpochMilli()))
                    .willReturn(Set.of("10"));
            given(retryJobRepository.findById(10L)).willReturn(Optional.of(job));
            given(postReportRepository.findByAnalysisId(1L)).willReturn(Optional.empty());
            given(postReportService.postRegister(analysis)).willReturn(PostReportEntity.builder().id(100L).pdfPath("/ok.pdf").build());

            retryService.retryDueJobs();

            assertThat(job.getStatus()).isEqualTo(PostReportRetryStatus.SUCCESS);
            assertThat(job.getAttempts()).isEqualTo(1);
            then(zSetOperations).should().remove("post-report:retry", "10");
        }

        @Test
        @DisplayName("이미 리포트가 있으면 PDF를 중복 생성하지 않고 SUCCESS로 전환")
        void 이미_성공한_작업은_중복_생성하지_않음(){
            PostAnalysisEntity analysis = mockAnalysis(1L);
            PostReportRetryJobEntity job = PostReportRetryJobEntity.pending(analysis, FIXED_CLOCK.instant(), "ER005", "python 503");
            setId(job, 10L);
            given(stringRedisTemplate.opsForZSet()).willReturn(zSetOperations);
            given(zSetOperations.rangeByScore("post-report:retry", 0, FIXED_CLOCK.instant().toEpochMilli()))
                    .willReturn(Set.of("10"));
            given(retryJobRepository.findById(10L)).willReturn(Optional.of(job));
            given(postReportRepository.findByAnalysisId(1L)).willReturn(Optional.of(PostReportEntity.builder().id(100L).build()));

            retryService.retryDueJobs();

            assertThat(job.getStatus()).isEqualTo(PostReportRetryStatus.SUCCESS);
            then(postReportService).should(never()).postRegister(any(PostAnalysisEntity.class));
            then(zSetOperations).should().remove("post-report:retry", "10");
        }

        @Test
        @DisplayName("3회 실패하면 DEAD_LETTER로 전환하고 Redis 큐에서 제거")
        void 최대_재시도_초과(){
            PostAnalysisEntity analysis = mockAnalysis(1L);
            PostReportRetryJobEntity job = PostReportRetryJobEntity.pending(analysis, FIXED_CLOCK.instant(), "ER005", "python 503");
            setId(job, 10L);
            job.markRetrying();
            job.markRetryFailed(new AppException(ExceptionCode.PDF_GENERATION_FAILED, "fail1"), FIXED_CLOCK.instant());
            job.markRetrying();
            job.markRetryFailed(new AppException(ExceptionCode.PDF_GENERATION_FAILED, "fail2"), FIXED_CLOCK.instant());
            given(stringRedisTemplate.opsForZSet()).willReturn(zSetOperations);
            given(zSetOperations.rangeByScore("post-report:retry", 0, FIXED_CLOCK.instant().toEpochMilli()))
                    .willReturn(Set.of("10"));
            given(retryJobRepository.findById(10L)).willReturn(Optional.of(job));
            given(postReportRepository.findByAnalysisId(1L)).willReturn(Optional.empty());
            willThrow(new AppException(ExceptionCode.PDF_GENERATION_FAILED, "fail3"))
                    .given(postReportService).postRegister(analysis);

            retryService.retryDueJobs();

            assertThat(job.getStatus()).isEqualTo(PostReportRetryStatus.DEAD_LETTER);
            assertThat(job.getAttempts()).isEqualTo(3);
            then(zSetOperations).should().remove("post-report:retry", "10");
        }
    }

    private static void setId(PostReportRetryJobEntity job, Long id) {
        try {
            java.lang.reflect.Field field = PostReportRetryJobEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(job, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static PostAnalysisEntity mockAnalysis(Long id) {
        PostAnalysisEntity analysis = mock(PostAnalysisEntity.class);
        given(analysis.getId()).willReturn(id);
        return analysis;
    }
}
