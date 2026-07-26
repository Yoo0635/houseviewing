"""DTO 정의

외부 수신 (Java 서버 → FastAPI):
    AnalyzeResponseDTO      /engine/analyze 응답
    GeneratePdfRequest      /engine/generate-pdf 요청 (계약전/후 통합)
    GenerateDiffPdfRequest  /engine/generate-pdf/diff 요청

내부 렌더링:
    RiskAnalysisRequest     risk_html_generator 용
    RecoveryRenderData      recovery_html_generator 용
"""
from __future__ import annotations

from typing import List, Literal, Optional

from pydantic import BaseModel, Field, field_validator, model_validator


# ─────────────────────────────────────────────
# /engine/analyze 응답 DTO
# ─────────────────────────────────────────────

class AnalyzeResponseDTO(BaseModel):
    """OCR 분석 결과 (FastAPI → Java 서버)"""
    riskLevel: str = Field(..., description="DANGER / WARNING / SAFE")
    rawData:   str = Field(..., description="전체 분석 JSON 문자열 (image_files 포함)")
    mainReason: str = Field(..., description="주요 위험 사유 한 줄 요약")
    ltvScore:  int  = Field(..., description="LTV 수치 (정수, %)")


# ─────────────────────────────────────────────
# /engine/generate-pdf 요청 DTO
# ─────────────────────────────────────────────

class GeneratePdfRequest(BaseModel):
    """통합 PDF 생성 DTO (Java 서버 → FastAPI)

    계약전: snapshotName + rawData 만 전달
    계약후: deposit 포함 시 계약후로 판단
    """
    snapshotName:    Optional[str]  = Field(None, description="등기부 제목/파일명")
    rawData:         str            = Field(...,  description="분석 원본 데이터 JSON 문자열", min_length=1)
    contractType:    Optional[str]  = Field(None, description="계약 유형 (JEONSE / MONTHLY)")
    deposit:         Optional[int]  = Field(None, description="보증금 (원)", ge=0)
    monthlyAmount:   Optional[int]  = Field(None, description="월세 (원)", ge=0)
    maintenanceFee:  Optional[int]  = Field(None, description="관리비 (원)", ge=0)
    moveDate:        Optional[str]  = Field(None, description="전입일 (YYYY-MM-DD)")
    confirmDate:     Optional[str]  = Field(None, description="확정일자 (YYYY-MM-DD)")

    @field_validator("snapshotName", mode="before")
    @classmethod
    def normalize_snapshot_name(cls, value: str | None) -> str | None:
        if value is None:
            return None
        value = value.strip()
        return value or None

    @field_validator("rawData")
    @classmethod
    def validate_required_text(cls, value: str, info) -> str:
        if value is None or not value.strip():
            raise ValueError(f"{info.field_name} 필드는 비어 있을 수 없습니다.")
        return value

    @model_validator(mode="after")
    def validate_post_contract_fields(self) -> "GeneratePdfRequest":
        if self.deposit is None:
            if not self.snapshotName:
                raise ValueError("계약전 PDF 생성에는 snapshotName 필드가 필요합니다.")
            return self

        missing_fields: list[str] = []
        if not self.contractType:
            missing_fields.append("contractType")
        if not self.moveDate:
            missing_fields.append("moveDate")
        if not self.confirmDate:
            missing_fields.append("confirmDate")
        if self.contractType == "MONTHLY" and self.monthlyAmount is None:
            missing_fields.append("monthlyAmount")

        if missing_fields:
            fields = ", ".join(missing_fields)
            raise ValueError(f"계약후 PDF 생성에는 다음 필드가 필요합니다: {fields}")

        return self


class GenerateDiffPdfRequest(BaseModel):
    """등기부 변동 비교 PDF 요청 DTO (Java 서버 → FastAPI)"""
    snapshotName:    Optional[str] = Field(None, description="등기부 제목")
    originData:      str = Field(..., description="직전 분석 JSON 문자열", min_length=1)
    newData:         str = Field(..., description="변동 후 분석 JSON 문자열", min_length=1)
    contractType:    str = Field(..., description="계약 유형 (JEONSE / MONTHLY)")
    deposit:         int = Field(..., description="보증금 (원)", ge=0)
    monthlyAmount:   int = Field(..., description="월세 (원)", ge=0)
    maintenanceFee:  int = Field(..., description="관리비 (원)", ge=0)
    moveDate:        str = Field(..., description="전입일 (YYYY-MM-DD)")
    confirmDate:     str = Field(..., description="확정일자 (YYYY-MM-DD)")

    @field_validator("moveDate", "confirmDate", mode="before")
    @classmethod
    def coerce_date_field(cls, value) -> str:
        """Spring LocalDate 배열([2024,3,1]) → ISO 문자열 변환"""
        if isinstance(value, (list, tuple)) and len(value) == 3:
            return f"{value[0]:04d}-{value[1]:02d}-{value[2]:02d}"
        if value is None:
            return ""
        return str(value)


class GenerateCombinedPdfRequest(BaseModel):
    """통합 시나리오 PDF 요청 DTO — DIFF + RECOVERY + OCR 텍스트 (Java 서버 → FastAPI)

    위험도 시나리오(SAFE/WARNING/DANGER)별로 호출하여 각 1장씩 총 3장 생성.
    """
    snapshotName:    Optional[str] = Field(None,  description="등기부 제목")
    originData:      str           = Field(...,   description="직전 분석 JSON 문자열", min_length=1)
    newData:         str           = Field(...,   description="변동 후 분석 JSON 문자열", min_length=1)
    contractType:    str           = Field(...,   description="계약 유형 (JEONSE / MONTHLY)")
    deposit:         int           = Field(...,   description="보증금 (원)", ge=0)
    monthlyAmount:   int           = Field(0,     description="월세 (원)", ge=0)
    maintenanceFee:  int           = Field(0,     description="관리비 (원)", ge=0)
    moveDate:        str           = Field("",    description="전입일 (YYYY-MM-DD)")
    confirmDate:     str           = Field("",    description="확정일자 (YYYY-MM-DD)")

    @field_validator("snapshotName", mode="before")
    @classmethod
    def normalize_combined_snapshot_name(cls, value: str | None) -> str | None:
        if value is None:
            return None
        value = value.strip()
        return value or None

    @field_validator("originData", "newData", "contractType")
    @classmethod
    def validate_combined_non_blank(cls, value: str, info) -> str:
        if value is None or not value.strip():
            raise ValueError(f"{info.field_name} 필드는 비어 있을 수 없습니다.")
        return value

    @field_validator("moveDate", "confirmDate", mode="before")
    @classmethod
    def coerce_date_field(cls, value) -> str:
        """Spring LocalDate 배열([2024,3,1]) → ISO 문자열 변환"""
        if isinstance(value, (list, tuple)) and len(value) == 3:
            return f"{value[0]:04d}-{value[1]:02d}-{value[2]:02d}"
        if value is None:
            return ""
        return str(value)

    @field_validator("snapshotName", mode="before")
    @classmethod
    def normalize_diff_snapshot_name(cls, value: str | None) -> str | None:
        if value is None:
            return None
        value = value.strip()
        return value or None

    @field_validator("moveDate", "confirmDate", mode="before")
    @classmethod
    def coerce_diff_date_field(cls, value) -> str:
        """Spring LocalDate 배열([2024,3,1]) → ISO 문자열 변환"""
        if isinstance(value, (list, tuple)) and len(value) == 3:
            return f"{value[0]:04d}-{value[1]:02d}-{value[2]:02d}"
        if value is None:
            return ""
        return str(value)

    @field_validator("originData", "newData", "contractType", "moveDate", "confirmDate")
    @classmethod
    def validate_non_blank_text(cls, value: str, info) -> str:
        if value is None or not value.strip():
            raise ValueError(f"{info.field_name} 필드는 비어 있을 수 없습니다.")
        return value


# ─────────────────────────────────────────────
# 내부 렌더링 DTO
# ─────────────────────────────────────────────

class RiskAnalysisRequest(BaseModel):
    """PDF 1 내부 렌더링 DTO (risk_html_generator 전용)"""
    user_name:         str
    address:           str
    risk_score:        Literal["High", "Medium", "Low"]
    risk_score_num:    float         = Field(default=0, ge=0, le=100)
    max_claim_amount:  int           = Field(default=0)
    ltv_percent:       float         = Field(default=0, ge=0, le=200)
    analysis_summary:  str
    checklist:         List[str]
    signals:           List[dict]    = Field(default_factory=list)
    playbook:          List[dict]    = Field(default_factory=list)
    recovery_priority: str           = ""

    @field_validator("risk_score")
    @classmethod
    def validate_risk_score(cls, v: str) -> str:
        if v not in {"High", "Medium", "Low"}:
            raise ValueError("risk_score는 High/Medium/Low 중 하나여야 합니다.")
        return v


class RecoveryRenderData(BaseModel):
    """PDF 2 내부 렌더링 DTO (recovery_html_generator 전용)"""
    user_name:          str
    address:            str
    contract_type:      str   = ""
    deposit_amount:     int
    monthly_amount:     int   = 0
    maintenance_fee:    int   = 0
    move_in_date:       str
    confirmed_date:     str
    has_residency:      bool
    has_priority_right: bool
    max_claim_amount:   int
    property_value:     int   = 0
    expected_recovery:  int   = 0
    recovery_rate:      float = 0.0
    ltv_percent:        float
    risk_score:         Literal["High", "Medium", "Low"]
    signals:            List[dict] = Field(default_factory=list)
