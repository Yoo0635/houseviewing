"""/engine 라우터 — Java 서버(Spring Boot)가 호출하는 분석/PDF 생성 엔드포인트"""
from __future__ import annotations

import json as json_lib
import os
import time
import traceback
from urllib.parse import quote

import pdfkit
from fastapi import APIRouter, HTTPException, Request
from fastapi.responses import Response, JSONResponse

from schemas.dto import (
    GeneratePdfRequest, GenerateDiffPdfRequest, GenerateCombinedPdfRequest,
    RiskAnalysisRequest, RecoveryRenderData,
)
from generators.risk_html_generator import generate_html_report
from generators.recovery_html_generator import generate_recovery_html_report
from generators.verification_html_generator import build_snapshot_page
from generators.combined_html_generator import generate_combined_html_report

router = APIRouter(prefix="/engine", tags=["Engine"])

# wkhtmltopdf 경로: 환경변수 우선, 없으면 OS별 기본 경로로 fallback
_WKHTMLTOPDF_PATH = os.getenv(
    "WKHTMLTOPDF_PATH",
    r"C:\Program Files\wkhtmltopdf\bin\wkhtmltopdf.exe",  # Windows 기본값
)
_PDFKIT_CONFIG = (
    pdfkit.configuration(wkhtmltopdf=_WKHTMLTOPDF_PATH)
    if os.path.exists(_WKHTMLTOPDF_PATH)
    else None  # Linux(Docker)에서는 PATH에 있으면 None으로 동작
)

_PDFKIT_OPTIONS: dict = {
    "encoding": "UTF-8",
    "page-size": "A4",
    "margin-top": "12mm",
    "margin-right": "12mm",
    "margin-bottom": "14mm",
    "margin-left": "12mm",
    "enable-local-file-access": "",
    "no-outline": "",
    "quiet": "",
    "javascript-delay": "1000",
}

_LEVEL_MAP = {"HIGH": "High", "MEDIUM": "Medium", "LOW": "Low"}


def _resolve_snapshot_name(raw: dict, snapshot_name: str | None, fallback: str = "analysis-report") -> str:
    if snapshot_name:
        return snapshot_name

    snapshot = raw.get("snapshot", {})
    address = snapshot.get("address", {})
    for candidate in (
        address.get("address"),
        address.get("addressName"),
        snapshot.get("snapshotName"),
    ):
        if isinstance(candidate, str) and candidate.strip():
            return candidate.strip()

    return fallback


def _extract_raw_info(raw: dict, snapshot_name: str) -> dict:
    """은섭 분석 JSON에서 공통 필드 추출"""
    risk_info = raw.get("risk", {})
    snapshot  = raw.get("snapshot", {})
    gabu      = snapshot.get("gabu", [])
    owners    = gabu[0].get("owners", []) if gabu else []

    return {
        "risk_level": _LEVEL_MAP.get(risk_info.get("risk_level", "MEDIUM"), "Medium"),
        "risk_score": float(risk_info.get("risk_score", 50)),
        "signals":    risk_info.get("signals", []),
        "max_claim":  int(risk_info.get("checks", {}).get("max_claim_amount_total", 0)),
        "owner_name": ", ".join(o["name"] for o in owners) if owners else "미확인",
        "address":    snapshot.get("address", {}).get("address", snapshot_name),
    }



def _build_render_data(snapshot_name: str, raw: dict) -> RiskAnalysisRequest:
    """은섭 분석 JSON → PDF 1 렌더링 데이터 변환"""
    info      = _extract_raw_info(raw, snapshot_name)
    signals   = info["signals"]
    checklist = [s["explain"] for s in signals] if signals else ["위험 시그널 없음"]
    playbook  = raw.get("recovery", {}).get("playbook", [])
    summary   = " / ".join(step["title"] for step in playbook[:2]) if playbook else "분석 결과 없음"

    ltv_raw = float((raw.get("ltv") or {}).get("ltv") or 0)
    ltv_pct = ltv_raw * 100 if ltv_raw < 1.5 else ltv_raw  # 0.27 → 27, 27 → 27

    return RiskAnalysisRequest(
        user_name=info["owner_name"],
        address=info["address"],
        risk_score=info["risk_level"],
        risk_score_num=info["risk_score"],
        max_claim_amount=info["max_claim"],
        ltv_percent=ltv_pct,
        analysis_summary=summary,
        checklist=checklist,
        signals=signals,
        playbook=playbook,
        recovery_priority=raw.get("recovery", {}).get("priority", ""),
    )


def _build_recovery_render_data(request: GeneratePdfRequest) -> RecoveryRenderData:
    """은섭 분석 JSON → PDF 2 렌더링 데이터 변환"""
    raw  = json_lib.loads(request.rawData)
    info = _extract_raw_info(raw, _resolve_snapshot_name(raw, request.snapshotName))

    # property_value: ltv 정보 → valuation 순으로 fallback
    ltv_info = raw.get("ltv") or {}
    prop_val = int(ltv_info.get("house_price_won") or 0)
    if prop_val == 0:
        prop_val = int((raw.get("valuation") or {}).get("median_price_won") or 0)

    # 예상 회수액: recovery 엔진 pre-computed 값 우선 사용
    recovery_calc     = (raw.get("recovery") or {}).get("calculation") or {}
    expected_recovery = int(recovery_calc.get("recoverable_amount") or 0)
    deposit           = request.deposit or 0
    recovery_rate     = round(expected_recovery / deposit * 100, 1) if deposit > 0 else 0.0

    has_residency   = bool(request.moveDate)
    has_priority    = has_residency and bool(request.confirmDate)

    return RecoveryRenderData(
        user_name=info["owner_name"],
        address=info["address"],
        contract_type=request.contractType or "",
        deposit_amount=deposit,
        monthly_amount=request.monthlyAmount or 0,
        maintenance_fee=request.maintenanceFee or 0,
        move_in_date=request.moveDate or "",
        confirmed_date=request.confirmDate or "",
        has_residency=has_residency,
        has_priority_right=has_priority,
        max_claim_amount=info["max_claim"],
        property_value=prop_val,
        expected_recovery=expected_recovery,
        recovery_rate=recovery_rate,
        ltv_percent=float((ltv_info.get("ltv") or 0)) * 100,
        risk_score=info["risk_level"],
        signals=info["signals"],
    )


@router.post(
    "/analyze/mock",
    summary="Mock 등기부 JSON 분석 (change-diagnoses 전용)",
    tags=["Engine"],
)
async def analyze_mock(request: Request) -> JSONResponse:
    """백엔드가 보낸 mock 등기부 JSON을 분석해 DiffAnalysisResult 형태로 반환한다.

    요청 body: {"snapshot": {...}} 또는 snapshot 자체 dict
    응답: {"riskLevel": "SAFE|WARNING|DANGER", "rawData": "...", "mainReason": "...", "ltvScore": 0}
    """
    try:
        body = await request.json()
    except Exception:
        raise HTTPException(status_code=422, detail="요청 body가 유효한 JSON이 아닙니다.")

    # Spring WebClient가 String을 JSON 문자열 리터럴로 보낼 경우 재파싱
    if isinstance(body, str):
        try:
            body = json_lib.loads(body)
        except Exception:
            raise HTTPException(status_code=422, detail="snapshot JSON 파싱 실패")

    # snapshot 추출
    if isinstance(body, dict) and "snapshot" in body and isinstance(body["snapshot"], dict):
        snapshot = body["snapshot"]
    elif isinstance(body, dict) and ("gabu" in body or "eulgu" in body):
        snapshot = body
    else:
        raise HTTPException(status_code=422, detail="snapshot 데이터를 찾을 수 없습니다.")

    try:
        from engines.risk_engine import compute_ltv_info, compute_risk
        from engines.recovery_engine import compute_recovery

        # 데모용 주소는 시세 고정 (ocr_core.py 와 동일한 로직)
        _addr = (snapshot.get("address") or {}).get("address", "")
        if "오산시 양산동 387" in _addr:
            valuation: dict = {"median_price_won": 30_000_000}  # 데모 고정값 (SAFE=0점/WARNING=78점/DANGER=99점)
        else:
            valuation: dict = {"median_price_won": 400_000_000}  # mock용 임시 집값 (4억)
        ltv_result      = compute_ltv_info(snapshot, valuation)
        risk_result     = compute_risk(snapshot, {}, valuation, ltv_result)
        recovery_result = compute_recovery(snapshot, valuation, ltv_result, {}, risk_result)


        raw_full = {
            "snapshot":  snapshot,
            "valuation": valuation,
            "ltv":       ltv_result,
            "risk":      risk_result,
            "recovery":  recovery_result,
        }

        # riskLevel 변환 (HIGH→DANGER, MEDIUM→WARNING, LOW→SAFE)
        level_raw = str(risk_result.get("risk_level", "LOW")).upper()
        risk_level_map = {"HIGH": "DANGER", "MEDIUM": "WARNING", "LOW": "SAFE"}
        risk_level = risk_level_map.get(level_raw, "WARNING")

        # mainReason
        signals = risk_result.get("signals", [])
        if signals:
            main_reason = str(signals[0].get("explain") or signals[0].get("code") or "위험 감지")
        else:
            main_reason = "위험 시그널 없음"

        # ltvScore: null 처리 + 비율값(0.14) → 퍼센트(14) 변환
        _ltv_raw = ltv_result.get("ltv") or 0
        ltv_score = int(float(_ltv_raw) * 100) if float(_ltv_raw or 0) < 1.5 else int(float(_ltv_raw))

        return JSONResponse({
            "riskLevel":  risk_level,
            "rawData":    json_lib.dumps(raw_full, ensure_ascii=False),
            "mainReason": main_reason,
            "ltvScore":   ltv_score,
        })

    except Exception as e:
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=f"mock 분석 실패: {e}")


def _recompute_recovery(new_raw: dict, deposit: int, move_date: str, confirm_date: str) -> dict:
    """PDF 생성 시점에 deposit으로 recovery 재계산 후 new_raw에 덮어씌우기"""
    try:
        from engines.recovery_engine import compute_recovery
        snapshot   = new_raw.get("snapshot") or {}
        valuation  = new_raw.get("valuation") or {}
        ltv_info   = new_raw.get("ltv") or {}
        risk       = new_raw.get("risk") or {}
        tenant_info = {
            "deposit":      deposit,
            "move_in_date": move_date or "",
            "fixed_date":   confirm_date or "",
        }
        new_raw["recovery"] = compute_recovery(snapshot, valuation, ltv_info, tenant_info, risk)
    except Exception:
        pass  # 재계산 실패 시 기존 rawData 값 유지
    return new_raw


def _pdf_bytes(html: str) -> bytes:
    """HTML → PDF 바이너리 변환"""
    try:
        return pdfkit.from_string(html, False, options=_PDFKIT_OPTIONS, configuration=_PDFKIT_CONFIG)
    except OSError as e:
        raise HTTPException(status_code=500, detail=f"PDF 변환 실패: wkhtmltopdf를 확인하세요. {e}")
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"PDF 변환 중 오류 발생: {e}")


@router.post(
    "/generate-pdf",
    summary="권리 분석 보고서 PDF 생성 (계약전/계약후 통합)",
    responses={
        200: {"content": {"application/pdf": {}}, "description": "PDF 파일 반환"},
        422: {"description": "요청 데이터 유효성 오류"},
        500: {"description": "PDF 변환 실패"},
    },
)
async def generate_pdf(request: GeneratePdfRequest) -> Response:
    import sys
    print(
        f"[DEBUG pdf] snapshotName={request.snapshotName!r} "
        f"rawData_len={len(request.rawData)} "
        f"deposit={request.deposit!r} contractType={request.contractType!r}",
        file=sys.stderr, flush=True,
    )
    print(f"[DEBUG pdf] rawData[:200]={request.rawData[:200]!r}", file=sys.stderr, flush=True)
    try:
        raw: dict = json_lib.loads(request.rawData)
    except ValueError as e:
        print(f"[DEBUG pdf] JSON parse error: {e}", file=sys.stderr, flush=True)
        raise HTTPException(status_code=422, detail="rawData가 유효한 JSON 문자열이 아닙니다.")

    resolved_snapshot_name = _resolve_snapshot_name(raw, request.snapshotName)

    t0 = time.perf_counter()

    if request.deposit is not None:
        # 계약후: 보증금 회수 분석 PDF
        render_data = _build_recovery_render_data(request)
        try:
            html_content = generate_recovery_html_report(render_data)
        except Exception as e:
            raise HTTPException(status_code=503, detail=f"보고서 HTML 생성 실패: {e}")
        filename = f"recovery_{render_data.user_name}.pdf"
    else:
        # 계약전: 위험 분석 PDF
        render_data = _build_render_data(resolved_snapshot_name, raw)
        try:
            html_content = generate_html_report(render_data)
        except Exception as e:
            err = str(e)
            if "429" in err or "RESOURCE_EXHAUSTED" in err:
                raise HTTPException(status_code=429, detail="AI API 호출 한도 초과. 잠시 후 다시 시도하세요.")
            raise HTTPException(status_code=503, detail=f"보고서 HTML 생성 실패: {err}")
        # 법적 용어 가이드 바로 뒤에 OCR 파싱 데이터 삽입 (page-break 없이)
        snapshot = raw.get("snapshot") or {}
        if snapshot:
            snap_page = build_snapshot_page(snapshot, page_break=False)
            html_content = html_content.replace("<!-- OCR_SNAPSHOT_HERE -->", snap_page)
        filename = f"report_{render_data.user_name}.pdf"

    ai_time   = time.perf_counter() - t0
    t1        = time.perf_counter()
    pdf_bytes = _pdf_bytes(html_content)
    pdf_time  = time.perf_counter() - t1

    encoded_name = quote(filename, safe="")
    return Response(
        content=pdf_bytes,
        media_type="application/pdf",
        headers={
            "Content-Disposition": f"attachment; filename=\"report.pdf\"; filename*=UTF-8''{encoded_name}",
            "Content-Length": str(len(pdf_bytes)),
            "X-AI-Time": f"{ai_time:.3f}",
            "X-PDF-Convert-Time": f"{pdf_time:.3f}",
        },
    )


@router.post(
    "/generate-pdf/diff",
    summary="등기부 변동 비교 PDF 생성",
    responses={
        200: {"content": {"application/pdf": {}}, "description": "PDF 파일 반환"},
        422: {"description": "요청 데이터 유효성 오류"},
        500: {"description": "PDF 변환 실패"},
    },
)
async def generate_diff_pdf(request: GenerateDiffPdfRequest) -> Response:
    try:
        origin_raw: dict = json_lib.loads(request.originData)
        new_raw: dict    = json_lib.loads(request.newData)
    except ValueError:
        raise HTTPException(status_code=422, detail="originData 또는 newData가 유효한 JSON이 아닙니다.")

    snapshot_name = _resolve_snapshot_name(new_raw, request.snapshotName, fallback="diff-analysis")
    new_raw = _recompute_recovery(new_raw, request.deposit, request.moveDate, request.confirmDate)

    try:
        html_content = generate_combined_html_report(
            snapshot_name=snapshot_name,
            origin_raw=origin_raw,
            new_raw=new_raw,
            contract_type=request.contractType,
            deposit=request.deposit,
            monthly_amount=request.monthlyAmount,
            maintenance_fee=request.maintenanceFee,
            move_date=request.moveDate,
            confirm_date=request.confirmDate,
        )
    except Exception as e:
        raise HTTPException(status_code=503, detail=f"변동 보고서 생성 실패: {e}")

    pdf_bytes    = _pdf_bytes(html_content)
    encoded_name = quote(f"diff_{snapshot_name}.pdf", safe="")
    return Response(
        content=pdf_bytes,
        media_type="application/pdf",
        headers={
            "Content-Disposition": f"attachment; filename=\"diff_report.pdf\"; filename*=UTF-8''{encoded_name}",
            "Content-Length": str(len(pdf_bytes)),
        },
    )


@router.post(
    "/generate-pdf/combined",
    summary="통합 시나리오 PDF 생성 (DIFF + RECOVERY + OCR 텍스트)",
    description=(
        "위험도 시나리오(SAFE / WARNING / DANGER)별로 호출하여 1장씩 생성합니다.\n\n"
        "- **Page 1** : 등기부 변동 내역 (DIFF)\n"
        "- **Page 2** : 보증금 회수 분석 (RECOVERY)\n"
        "- **Page 3** : 등기부 OCR 파싱 텍스트 (VERIFICATION)"
    ),
    responses={
        200: {"content": {"application/pdf": {}}, "description": "통합 시나리오 PDF 반환"},
        422: {"description": "요청 데이터 유효성 오류"},
        500: {"description": "PDF 변환 실패"},
    },
)
async def generate_combined_pdf(request: GenerateCombinedPdfRequest) -> Response:
    import sys
    print(f"[DEBUG combined] contractType={request.contractType!r} deposit={request.deposit!r} moveDate={request.moveDate!r}", file=sys.stderr, flush=True)
    try:
        origin_raw: dict = json_lib.loads(request.originData)
        new_raw: dict    = json_lib.loads(request.newData)
    except ValueError:
        raise HTTPException(status_code=422, detail="originData 또는 newData가 유효한 JSON이 아닙니다.")

    snapshot_name = _resolve_snapshot_name(new_raw, request.snapshotName, fallback="combined-report")
    new_raw = _recompute_recovery(new_raw, request.deposit, request.moveDate, request.confirmDate)

    try:
        html_content = generate_combined_html_report(
            snapshot_name=snapshot_name,
            origin_raw=origin_raw,
            new_raw=new_raw,
            contract_type=request.contractType,
            deposit=request.deposit,
            monthly_amount=request.monthlyAmount,
            maintenance_fee=request.maintenanceFee,
            move_date=request.moveDate,
            confirm_date=request.confirmDate,
        )
    except Exception as e:
        raise HTTPException(status_code=503, detail=f"통합 보고서 HTML 생성 실패: {e}")

    pdf_bytes    = _pdf_bytes(html_content)
    risk_level   = new_raw.get("risk", {}).get("risk_level", "UNKNOWN").lower()
    encoded_name = quote(f"combined_{risk_level}_{snapshot_name}.pdf", safe="")
    return Response(
        content=pdf_bytes,
        media_type="application/pdf",
        headers={
            "Content-Disposition": f"attachment; filename=\"combined_report.pdf\"; filename*=UTF-8''{encoded_name}",
            "Content-Length": str(len(pdf_bytes)),
        },
    )
