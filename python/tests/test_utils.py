"""utils.py 및 HTML 생성기 단위 테스트"""
import pytest
from core.utils import fmt_krw, build_signals_html, build_guidelines_html


# ── fmt_krw ──────────────────────────────────────────────────────────────────

class TestFmtKrw:
    def test_억원(self):
        assert fmt_krw(200_000_000) == "200,000,000 원"

    def test_만원(self):
        assert fmt_krw(10_000) == "10,000 원"

    def test_0원(self):
        assert fmt_krw(0) == "0 원"


# ── build_signals_html ────────────────────────────────────────────────────────

class TestBuildSignalsHtml:
    def test_빈리스트_빈문자열(self):
        assert build_signals_html([]) == ""

    def test_HIGH_배지포함(self):
        html = build_signals_html([{"severity": "HIGH", "explain": "위험 신호"}])
        assert "고위험" in html
        assert "위험 신호" in html

    def test_MEDIUM_배지포함(self):
        html = build_signals_html([{"severity": "MEDIUM", "explain": "주의 신호"}])
        assert "주의" in html
        assert "주의 신호" in html

    def test_li_태그_없음(self):
        """wkhtmltopdf 깨짐 방지: <li> 대신 <div> 사용"""
        html = build_signals_html([{"severity": "HIGH", "explain": "test"}])
        assert "<li>" not in html

    def test_여러_시그널_모두_포함(self):
        signals = [
            {"severity": "HIGH",   "explain": "첫번째"},
            {"severity": "MEDIUM", "explain": "두번째"},
        ]
        html = build_signals_html(signals)
        assert "첫번째" in html
        assert "두번째" in html


# ── build_guidelines_html ─────────────────────────────────────────────────────

class TestBuildGuidelinesHtml:
    @pytest.mark.parametrize("risk_level", ["High", "Medium", "Low"])
    @pytest.mark.parametrize("context",    ["risk", "diff"])
    def test_모든_조합_생성(self, risk_level, context):
        html = build_guidelines_html(risk_level, context)
        assert html  # 빈 문자열이 아님

    @pytest.mark.parametrize("risk_level", ["High", "Medium", "Low"])
    @pytest.mark.parametrize("context",    ["risk", "diff"])
    def test_STEP_4개_포함(self, risk_level, context):
        html = build_guidelines_html(risk_level, context)
        assert html.count("STEP") == 4

    def test_High_위험_배너색상(self):
        html = build_guidelines_html("High", "risk")
        assert "#c0392b" in html

    def test_Medium_주의_배너색상(self):
        html = build_guidelines_html("Medium", "risk")
        assert "#d35400" in html

    def test_Low_안전_배너색상(self):
        html = build_guidelines_html("Low", "risk")
        assert "#1e8449" in html

    def test_연락처박스_HUG포함(self):
        html = build_guidelines_html("High", "risk")
        assert "1566-9009" in html

    def test_연락처박스_법무사포함(self):
        html = build_guidelines_html("High", "diff")
        assert "1544-6272" in html

    def test_li_태그_없음(self):
        """wkhtmltopdf 깨짐 방지"""
        for level in ["High", "Medium", "Low"]:
            html = build_guidelines_html(level, "risk")
            assert "<li>" not in html

    def test_잘못된_risk_level_fallback_정상처리(self):
        """정의되지 않은 risk_level → Medium fallback, 에러 없음"""
        html = build_guidelines_html("UNKNOWN", "risk")
        assert html


# ── HTML 생성기 스모크 테스트 ─────────────────────────────────────────────────

class TestHtmlGenerators:
    def test_risk_html_생성(self):
        import os
        os.environ["GEMINI_FALLBACK_ENABLED"] = "true"
        from schemas.dto import RiskAnalysisRequest
        from generators.risk_html_generator import generate_html_report
        data = RiskAnalysisRequest(
            user_name="홍길동",
            address="경기도 오산시",
            risk_score="High",
            risk_score_num=85.0,
            max_claim_amount=373_000_000,
            analysis_summary="선순위 채권 과다",
            checklist=["전문가 상담"],
            signals=[{"severity": "HIGH", "explain": "위험"}],
        )
        html = generate_html_report(data)
        assert "<!DOCTYPE html>" in html
        assert "홍길동" in html
        assert "대응 가이드라인" in html
        assert "STEP 1" in html

    def test_recovery_html_생성(self):
        from schemas.dto import RecoveryRenderData
        from generators.recovery_html_generator import generate_recovery_html_report
        data = RecoveryRenderData(
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
        html = generate_recovery_html_report(data)
        assert "<!DOCTYPE html>" in html
        assert "홍길동" in html

    def test_diff_html_생성(self):
        from tests.conftest import RAW_LOW, RAW_HIGH
        from generators.diff_html_generator import generate_diff_html_report
        html = generate_diff_html_report(
            snapshot_name="에덴하우스 105호",
            origin_raw=RAW_LOW,
            new_raw=RAW_HIGH,
            contract_type="JEONSE",
            deposit=200_000_000,
            monthly_amount=0,
            maintenance_fee=0,
            move_date="2025-05-01",
            confirm_date="2025-05-01",
        )
        assert "<!DOCTYPE html>" in html
        assert "대응 가이드라인" in html
        assert "STEP 1" in html
