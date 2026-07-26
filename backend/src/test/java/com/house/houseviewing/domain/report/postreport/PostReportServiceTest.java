package com.house.houseviewing.domain.report.postreport;

import com.house.houseviewing.domain.analysis.postanalysis.entity.PostAnalysisEntity;
import com.house.houseviewing.domain.analysis.postanalysis.enums.AnalysisType;
import com.house.houseviewing.domain.analysis.postanalysis.repository.PostAnalysisRepository;
import com.house.houseviewing.domain.contract.entity.ContractEntity;
import com.house.houseviewing.domain.contract.repository.ContractRepository;
import com.house.houseviewing.domain.house.entity.HouseEntity;
import com.house.houseviewing.domain.report.postreport.entity.PostReportEntity;
import com.house.houseviewing.domain.report.postreport.repository.PostReportRepository;
import com.house.houseviewing.domain.report.postreport.service.PostReportService;
import com.house.houseviewing.domain.user.entity.UserEntity;
import com.house.houseviewing.fixture.ContractFixture;
import com.house.houseviewing.fixture.HouseFixture;
import com.house.houseviewing.fixture.PostAnalysisFixture;
import com.house.houseviewing.fixture.UserFixture;
import com.house.houseviewing.global.exception.AppException;
import com.house.houseviewing.global.exception.ExceptionCode;
import com.house.houseviewing.global.file.pdf.dto.PdfDiffReportRequest;
import com.house.houseviewing.global.file.pdf.dto.PdfUploadResult;
import com.house.houseviewing.global.file.pdf.service.PdfReportTransferAndReceiveService;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PostReportServiceTest {

    @InjectMocks PostReportService postReportService;

    @Mock PostReportRepository postReportRepository;
    @Mock ContractRepository contractRepository;
    @Mock PdfReportTransferAndReceiveService pdfReportTransferAndReceiveService;
    @Mock PostAnalysisRepository postAnalysisRepository;

    @Nested
    @DisplayName("사후 리포트 등록")
    class PostRegister {

        @Test
        @DisplayName("성공")
        void 성공(){
            UserEntity user = UserFixture.createDefaultWithId(1L);
            HouseEntity house = HouseFixture.createWithUserAndId(user, 1L);
            ContractEntity contract = ContractFixture.createWithHouseAndId(house, 1L);
            PostAnalysisEntity analysis = PostAnalysisFixture.createWithId(house, 1L);
            analysis.addContract(contract);

            PdfUploadResult uploadResult = PdfUploadResult.builder()
                    .pdfKey("post-key")
                    .pdfPath("/post/path")
                    .pdfName("post.pdf")
                    .pdfSizeBytes(2048L)
                    .build();

            given(contractRepository.findById(anyLong())).willReturn(java.util.Optional.of(contract));
            given(pdfReportTransferAndReceiveService.postTransferAndReceive(any())).willReturn(uploadResult);
            given(postReportRepository.save(any(PostReportEntity.class))).willAnswer(invocation -> invocation.getArgument(0));

            PostReportEntity result = postReportService.postRegister(analysis);

            assertThat(result).isNotNull();
            assertThat(result.getPdfKey()).isEqualTo("post-key");
            verify(postReportRepository).save(any(PostReportEntity.class));
        }

        @Test
        @DisplayName("실패: 계약을 찾을 수 없음")
        void 계약_없음(){
            UserEntity user = UserFixture.createDefaultWithId(1L);
            HouseEntity house = HouseFixture.createWithUserAndId(user, 1L);
            PostAnalysisEntity analysis = mock(PostAnalysisEntity.class);
            ContractEntity mockContract = mock(ContractEntity.class);
            when(mockContract.getId()).thenReturn(1L);
            when(analysis.getContract()).thenReturn(mockContract);

            given(contractRepository.findById(anyLong())).willReturn(java.util.Optional.empty());

            assertThatThrownBy(() -> postReportService.postRegister(analysis))
                    .isInstanceOf(AppException.class)
                    .extracting("exceptionCode")
                    .isEqualTo(ExceptionCode.CONTRACT_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("차이 리포트 등록")
    class DiffRegister {

        @Test
        @DisplayName("성공")
        void 성공(){
            UserEntity user = UserFixture.createDefaultWithId(1L);
            HouseEntity house = HouseFixture.createWithUserAndId(user, 1L);
            ContractEntity contract = ContractFixture.createWithHouseAndId(house, 1L);
            PostAnalysisEntity analysis = PostAnalysisFixture.createDiffWithId(house, 1L);
            analysis.addContract(contract);

            PostAnalysisEntity existingAnalysis = PostAnalysisFixture.createWithId(house, 2L);
            existingAnalysis.addContract(contract);

            PdfUploadResult uploadResult = PdfUploadResult.builder()
                    .pdfKey("diff-key")
                    .pdfPath("/diff/path")
                    .pdfName("diff.pdf")
                    .pdfSizeBytes(3072L)
                    .build();

            given(contractRepository.findById(anyLong())).willReturn(java.util.Optional.of(contract));
            given(postAnalysisRepository.findTop2ByContractHouseIdOrderByCreatedAtDesc(anyLong()))
                    .willReturn(List.of(analysis, existingAnalysis));
            given(pdfReportTransferAndReceiveService.diffTransferAndReceive(any())).willReturn(uploadResult);
            given(postReportRepository.save(any(PostReportEntity.class))).willAnswer(invocation -> invocation.getArgument(0));

            PostReportEntity result = postReportService.diffRegister(analysis);
            ArgumentCaptor<PdfDiffReportRequest> requestCaptor = ArgumentCaptor.forClass(PdfDiffReportRequest.class);

            assertThat(result).isNotNull();
            assertThat(result.getPdfKey()).isEqualTo("diff-key");
            verify(pdfReportTransferAndReceiveService).diffTransferAndReceive(requestCaptor.capture());
            assertThat(requestCaptor.getValue().getOriginData()).isEqualTo(existingAnalysis.getRawData());
            assertThat(requestCaptor.getValue().getNewData()).isEqualTo(analysis.getRawData());
            verify(postReportRepository).save(any(PostReportEntity.class));
        }
    }
}
