"""DTO 유효성 검증 테스트"""
import json
import pytest
from pydantic import ValidationError

from schemas.dto import (
    GeneratePdfRequest,
    GenerateDiffPdfRequest,
    RiskAnalysisRequest,
    RecoveryRenderData,
    AnalyzeResponseDTO,
)
from tests.conftest import RAW_HIGH_STR


# ── GeneratePdfRequest ──────────────────────────────────────────────────────

class TestGeneratePdfRequest:
    def test_계약전_필수필드만(self):
        req = GeneratePdfRequest(snapshotName="테스트", rawData=RAW_HIGH_STR)
        assert req.deposit is None
        assert req.contractType is None

    def test_계약후_보증금포함(self):
        req = GeneratePdfRequest(
            snapshotName="테스트",
            rawData=RAW_HIGH_STR,
            contractType="JEONSE",
            deposit=200_000_000,
            monthlyAmount=0,
            maintenanceFee=0,
            moveDate="2025-05-01",
            confirmDate="2025-05-01",
        )
        assert req.deposit == 200_000_000
        assert req.contractType == "JEONSE"

    def test_snapshotName_없으면_ValidationError(self):
        with pytest.raises(ValidationError):
            GeneratePdfRequest(rawData=RAW_HIGH_STR)

    def test_rawData_없으면_ValidationError(self):
        with pytest.raises(ValidationError):
            GeneratePdfRequest(snapshotName="테스트")

    def test_계약후_필수필드_누락시_ValidationError(self):
        with pytest.raises(ValidationError) as exc_info:
            GeneratePdfRequest(
                snapshotName="테스트",
                rawData=RAW_HIGH_STR,
                deposit=200_000_000,
            )
        assert "contractType" in str(exc_info.value)

    def test_snapshotName_공백이면_ValidationError(self):
        with pytest.raises(ValidationError):
            GeneratePdfRequest(snapshotName="   ", rawData=RAW_HIGH_STR)

    def test_계약후는_snapshotName_없어도_정상(self):
        req = GeneratePdfRequest(
            rawData=RAW_HIGH_STR,
            contractType="JEONSE",
            deposit=200_000_000,
            monthlyAmount=0,
            maintenanceFee=0,
            moveDate="2025-05-01",
            confirmDate="2025-05-01",
        )
        assert req.snapshotName is None


# ── GenerateDiffPdfRequest ──────────────────────────────────────────────────

class TestGenerateDiffPdfRequest:
    def test_정상_생성(self):
        req = GenerateDiffPdfRequest(
            snapshotName="에덴하우스 105호",
            originData=RAW_HIGH_STR,
            newData=RAW_HIGH_STR,
            contractType="JEONSE",
            deposit=200_000_000,
            monthlyAmount=0,
            maintenanceFee=0,
            moveDate="2025-05-01",
            confirmDate="2025-05-01",
        )
        assert req.snapshotName == "에덴하우스 105호"
        assert req.deposit == 200_000_000

    def test_필수필드_누락시_ValidationError(self):
        with pytest.raises(ValidationError):
            GenerateDiffPdfRequest(snapshotName="테스트")

    def test_snapshotName_공백이어도_정상(self):
        req = GenerateDiffPdfRequest(
            snapshotName=" ",
            originData=RAW_HIGH_STR,
            newData=RAW_HIGH_STR,
            contractType="JEONSE",
            deposit=200_000_000,
            monthlyAmount=0,
            maintenanceFee=0,
            moveDate="2025-05-01",
            confirmDate="2025-05-01",
        )
        assert req.snapshotName is None


# ── RiskAnalysisRequest ─────────────────────────────────────────────────────

class TestRiskAnalysisRequest:
    def _make(self, risk_score="High"):
        return RiskAnalysisRequest(
            user_name="홍길동",
            address="경기도 오산시 양산동 387",
            risk_score=risk_score,
            risk_score_num=85.0,
            max_claim_amount=373_000_000,
            analysis_summary="선순위 채권 과다",
            checklist=["전문가 상담 필요"],
        )

    def test_High_정상(self):
        req = self._make("High")
        assert req.risk_score == "High"

    def test_Medium_정상(self):
        req = self._make("Medium")
        assert req.risk_score == "Medium"

    def test_Low_정상(self):
        req = self._make("Low")
        assert req.risk_score == "Low"

    def test_잘못된_risk_score_거부(self):
        with pytest.raises(ValidationError):
            self._make("DANGER")

    def test_risk_score_num_범위초과_거부(self):
        with pytest.raises(ValidationError):
            RiskAnalysisRequest(
                user_name="홍길동",
                address="주소",
                risk_score="High",
                risk_score_num=150,  # > 100
                max_claim_amount=0,
                analysis_summary="test",
                checklist=[],
            )

    def test_signals_기본값_빈리스트(self):
        req = self._make()
        assert req.signals == []

    def test_playbook_기본값_빈리스트(self):
        req = self._make()
        assert req.playbook == []


# ── RecoveryRenderData ──────────────────────────────────────────────────────

class TestRecoveryRenderData:
    def _make(self, **kwargs):
        base = dict(
            user_name="홍길동",
            address="경기도 오산시",
            deposit_amount=200_000_000,
            move_in_date="2025-05-01",
            confirmed_date="2025-05-01",
            has_residency=True,
            has_priority_right=True,
            max_claim_amount=373_000_000,
            ltv_percent=85.0,
            risk_score="High",
        )
        base.update(kwargs)
        return RecoveryRenderData(**base)

    def test_정상_생성(self):
        rd = self._make()
        assert rd.deposit_amount == 200_000_000
        assert rd.risk_score == "High"

    def test_property_value_기본값_0(self):
        rd = self._make()
        assert rd.property_value == 0

    def test_recovery_rate_기본값_0(self):
        rd = self._make()
        assert rd.recovery_rate == 0.0

    def test_잘못된_risk_score_거부(self):
        with pytest.raises(ValidationError):
            self._make(risk_score="INVALID")


# ── AnalyzeResponseDTO ──────────────────────────────────────────────────────

class TestAnalyzeResponseDTO:
    def test_정상_생성(self):
        dto = AnalyzeResponseDTO(
            riskLevel="DANGER",
            rawData=RAW_HIGH_STR,
            mainReason="선순위 채권 과다",
            ltvScore=85,
        )
        assert dto.riskLevel == "DANGER"
        assert dto.ltvScore == 85

    def test_필수필드_누락_거부(self):
        with pytest.raises(ValidationError):
            AnalyzeResponseDTO(riskLevel="DANGER")
