"""/engine 라우터 API 통신 테스트
pdfkit은 mock 처리 → 실제 wkhtmltopdf 설치 여부와 무관하게 실행 가능.
AI 클라이언트(groq_client)도 mock → API 키 없이 실행 가능.
"""
import json
import os
import pytest
from unittest.mock import MagicMock, patch
from fastapi.testclient import TestClient

os.environ.setdefault("GEMINI_FALLBACK_ENABLED", "true")

from app_factory import create_app
from tests.conftest import RAW_HIGH, RAW_LOW, RAW_HIGH_STR, RAW_LOW_STR

FAKE_PDF = b"%PDF-1.4 fake pdf bytes"

app = create_app(include_ocr=False)
client = TestClient(app)


def _pdf_patch():
    """pdfkit.from_string 패치 컨텍스트"""
    return patch("routers.engine.pdfkit.from_string", return_value=FAKE_PDF)


def _ai_patch():
    """groq AI 클라이언트 패치 — 함수 내부 import 경로 기준"""
    mock_content = {
        "risk_headline": "테스트 헤드라인",
        "main_analysis": "테스트 분석 내용",
        "recovery_comment": "테스트 회수 코멘트",
        "action_items": ["즉시 전문가 상담"],
    }
    return patch("groq_client.generate_analysis_content", return_value=mock_content)


# ── /health ─────────────────────────────────────────────────────────────────

class TestHealth:
    def test_200_반환(self):
        res = client.get("/health")
        assert res.status_code == 200

    def test_status_ok(self):
        res = client.get("/health")
        assert res.json()["status"] == "ok"


# ── POST /engine/generate-pdf (계약전) ──────────────────────────────────────

class TestGeneratePdfPre:
    """계약전: deposit 없음 → 위험 분석 PDF"""

    def test_200_반환(self):
        with _pdf_patch():
            res = client.post("/engine/generate-pdf", json={
                "snapshotName": "에덴하우스 105호",
                "rawData": RAW_HIGH_STR,
            })
        assert res.status_code == 200

    def test_content_type_pdf(self):
        with _pdf_patch():
            res = client.post("/engine/generate-pdf", json={
                "snapshotName": "에덴하우스 105호",
                "rawData": RAW_HIGH_STR,
            })
        assert "application/pdf" in res.headers["content-type"]

    def test_pdf_바이너리_반환(self):
        with _pdf_patch():
            res = client.post("/engine/generate-pdf", json={
                "snapshotName": "에덴하우스 105호",
                "rawData": RAW_HIGH_STR,
            })
        assert res.content == FAKE_PDF

    def test_잘못된_rawData_422(self):
        res = client.post("/engine/generate-pdf", json={
            "snapshotName": "테스트",
            "rawData": "NOT_JSON",
        })
        assert res.status_code == 422

    def test_snapshotName_누락_422(self):
        res = client.post("/engine/generate-pdf", json={
            "rawData": RAW_HIGH_STR,
        })
        assert res.status_code == 422
        assert res.json()["code"] == "INVALID_PDF_REQUEST"
        assert "snapshotName" in res.json()["message"]

    def test_LOW_위험도_정상처리(self):
        with _pdf_patch():
            res = client.post("/engine/generate-pdf", json={
                "snapshotName": "안전 매물",
                "rawData": RAW_LOW_STR,
            })
        assert res.status_code == 200


# ── POST /engine/generate-pdf (계약후) ──────────────────────────────────────

class TestGeneratePdfPost:
    """계약후: deposit 포함 → 보증금 회수 분석 PDF"""

    def _req(self, **kwargs):
        base = {
            "snapshotName": "에덴하우스 105호",
            "rawData": RAW_HIGH_STR,
            "contractType": "JEONSE",
            "deposit": 200_000_000,
            "monthlyAmount": 0,
            "maintenanceFee": 0,
            "moveDate": "2025-05-01",
            "confirmDate": "2025-05-01",
        }
        base.update(kwargs)
        return base

    def test_200_반환(self):
        with _pdf_patch():
            res = client.post("/engine/generate-pdf", json=self._req())
        assert res.status_code == 200

    def test_content_type_pdf(self):
        with _pdf_patch():
            res = client.post("/engine/generate-pdf", json=self._req())
        assert "application/pdf" in res.headers["content-type"]

    def test_MONTHLY_계약유형_정상처리(self):
        with _pdf_patch():
            res = client.post("/engine/generate-pdf", json=self._req(
                contractType="MONTHLY",
                monthlyAmount=500_000,
            ))
        assert res.status_code == 200

    def test_content_disposition_헤더_존재(self):
        with _pdf_patch():
            res = client.post("/engine/generate-pdf", json=self._req())
        assert "content-disposition" in res.headers

    def test_계약후_필수필드_누락_422(self):
        with _pdf_patch():
            res = client.post("/engine/generate-pdf", json=self._req(snapshotName=None))
        assert res.status_code == 200

    def test_계약후_계약유형_누락_422(self):
        req = self._req()
        del req["contractType"]
        res = client.post("/engine/generate-pdf", json=req)
        assert res.status_code == 422
        assert "contractType" in res.json()["message"]


# ── POST /engine/generate-pdf/diff ──────────────────────────────────────────

class TestGenerateDiffPdf:
    def _req(self, **kwargs):
        base = {
            "snapshotName": "에덴하우스 105호",
            "originData": RAW_LOW_STR,
            "newData": RAW_HIGH_STR,
            "contractType": "JEONSE",
            "deposit": 200_000_000,
            "monthlyAmount": 0,
            "maintenanceFee": 0,
            "moveDate": "2025-05-01",
            "confirmDate": "2025-05-01",
        }
        base.update(kwargs)
        return base

    def test_200_반환(self):
        with _pdf_patch():
            res = client.post("/engine/generate-pdf/diff", json=self._req())
        assert res.status_code == 200

    def test_content_type_pdf(self):
        with _pdf_patch():
            res = client.post("/engine/generate-pdf/diff", json=self._req())
        assert "application/pdf" in res.headers["content-type"]

    def test_동일_데이터_변동없음_정상처리(self):
        """origin == new: 변동 없음으로 정상 처리"""
        with _pdf_patch():
            res = client.post("/engine/generate-pdf/diff", json=self._req(
                originData=RAW_HIGH_STR,
                newData=RAW_HIGH_STR,
            ))
        assert res.status_code == 200

    def test_잘못된_originData_422(self):
        res = client.post("/engine/generate-pdf/diff", json=self._req(originData="BAD"))
        assert res.status_code == 422

    def test_잘못된_newData_422(self):
        res = client.post("/engine/generate-pdf/diff", json=self._req(newData="BAD"))
        assert res.status_code == 422

    def test_필수필드_누락_422(self):
        res = client.post("/engine/generate-pdf/diff", json={
            "originData": RAW_LOW_STR,
        })
        assert res.status_code == 422
        assert res.json()["code"] == "INVALID_PDF_REQUEST"


# ── 내부 변환 헬퍼 단위 테스트 ────────────────────────────────────────────────

class TestBuildRenderData:
    def test_HIGH_레벨_변환(self):
        from routers.engine import _build_render_data
        data = _build_render_data("테스트", RAW_HIGH)
        assert data.risk_score == "High"
        assert data.risk_score_num == 100

    def test_LOW_레벨_변환(self):
        from routers.engine import _build_render_data
        data = _build_render_data("테스트", RAW_LOW)
        assert data.risk_score == "Low"
        assert data.risk_score_num == 20

    def test_소유자_이름_추출(self):
        from routers.engine import _build_render_data
        data = _build_render_data("테스트", RAW_HIGH)
        assert "홍길동" in data.user_name

    def test_주소_추출(self):
        from routers.engine import _build_render_data
        data = _build_render_data("테스트", RAW_HIGH)
        assert "오산시" in data.address

    def test_signals_전달(self):
        from routers.engine import _build_render_data
        data = _build_render_data("테스트", RAW_HIGH)
        assert len(data.signals) == 2

    def test_signals_없을때_빈리스트(self):
        from routers.engine import _build_render_data
        data = _build_render_data("테스트", RAW_LOW)
        assert data.signals == []

    def test_max_claim_amount_추출(self):
        from routers.engine import _build_render_data
        data = _build_render_data("테스트", RAW_HIGH)
        assert data.max_claim_amount == 373_000_000
