import os
import json
import traceback
from typing import Optional, Dict, Any

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

from engine.diff_engine import diff_snapshots
from engine.risk_engine import compute_ltv_info, compute_risk
from engine.recovery_engine import compute_recovery

app = FastAPI(title="Mock Registry Analyze API")

BASE_DIR = "../mock_storage"
os.makedirs(BASE_DIR, exist_ok=True)


# -----------------------------
# DTO
# -----------------------------
class SaveBaselineRequest(BaseModel):
    doc_id: str
    snapshot: str   # mock 등기부 JSON 문자열


class AnalyzeRequest(BaseModel):
    doc_id: str
    snapshot: str   # mock 등기부 JSON 문자열
    deposit: Optional[int] = 0
    move_in_date: Optional[str] = None
    fixed_date: Optional[str] = None


# -----------------------------
# 유틸
# -----------------------------
def parse_snapshot_string(snapshot: str) -> Dict[str, Any]:
    try:
        parsed = json.loads(snapshot)
    except json.JSONDecodeError:
        raise HTTPException(status_code=400, detail="snapshot JSON 문자열 파싱 실패")

    # 전체 result JSON이면 snapshot 추출
    if "snapshot" in parsed and isinstance(parsed["snapshot"], dict):
        return parsed["snapshot"]

    # snapshot 자체만 들어온 경우
    if "gabu" in parsed or "eulgu" in parsed:
        return parsed

    raise HTTPException(status_code=400, detail="snapshot 데이터를 찾을 수 없음")


def baseline_file_path(doc_id: str) -> str:
    safe_doc_id = "".join(c for c in doc_id if c.isalnum() or c in ("-", "_"))
    return os.path.join(BASE_DIR, f"{safe_doc_id}_baseline.json")


def result_file_path(doc_id: str) -> str:
    safe_doc_id = "".join(c for c in doc_id if c.isalnum() or c in ("-", "_"))
    return os.path.join(BASE_DIR, f"{safe_doc_id}_latest_result.json")


def save_json(path: str, data: Dict[str, Any]) -> None:
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)


def load_baseline_snapshot(doc_id: str) -> Optional[Dict[str, Any]]:
    path = baseline_file_path(doc_id)
    if not os.path.exists(path):
        return None

    with open(path, "r", encoding="utf-8") as f:
        saved = json.load(f)

    # 저장된 게 snapshot 원문이면 그대로 사용
    if "gabu" in saved or "eulgu" in saved:
        return saved

    if "snapshot" in saved and isinstance(saved["snapshot"], dict):
        return saved["snapshot"]

    return None


def convert_risk_level(risk_result: Dict[str, Any]) -> str:
    level = str(risk_result.get("risk_level", "")).upper()

    if level == "LOW":
        return "SAFE"
    if level == "MEDIUM":
        return "WARNING"
    if level == "HIGH":
        return "DANGER"

    return level if level in {"SAFE", "WARNING", "DANGER"} else "WARNING"


def pick_main_reason(snapshot: Dict[str, Any]) -> str:
    eulgu = snapshot.get("eulgu", [])

    if eulgu and len(eulgu) > 0:
        purpose = eulgu[0].get("purpose")
        if purpose:
            return str(purpose)

    return "특이 위험 없음"


def extract_ltv_score(ltv_result: Dict[str, Any]) -> int:
    raw = ltv_result.get("ltv", 0)

    try:
        return int(round(float(raw) * 100))
    except Exception:
        return 0


# -----------------------------
# 1) 기준 snapshot 저장
# -----------------------------
@app.post("/baseline/save")
def save_baseline(req: SaveBaselineRequest):
    snapshot_dict = parse_snapshot_string(req.snapshot)
    save_json(baseline_file_path(req.doc_id), snapshot_dict)

    return {
        "ok": True,
        "message": "기준 snapshot 저장 완료",
        "doc_id": req.doc_id,
        "viewed_at": snapshot_dict.get("viewed_at"),
        "baseline_path": baseline_file_path(req.doc_id)
    }


# -----------------------------
# 2) 기준 데이터 조회
# -----------------------------
@app.get("/baseline/{doc_id}")
def get_baseline(doc_id: str):
    path = baseline_file_path(doc_id)
    if not os.path.exists(path):
        raise HTTPException(status_code=404, detail="기준 데이터가 없음")

    with open(path, "r", encoding="utf-8") as f:
        data = json.load(f)

    return data


# -----------------------------
# 3) snapshot 분석
# -----------------------------
@app.post("/engine/analyze")
def analyze_registry(req: AnalyzeRequest):
    try:
        current_snapshot = parse_snapshot_string(req.snapshot)

        baseline_snapshot = load_baseline_snapshot(req.doc_id)

        if baseline_snapshot:
            diff_result = diff_snapshots(baseline_snapshot, current_snapshot)
            baseline_present = True
            baseline_viewed_at = baseline_snapshot.get("viewed_at")
        else:
            diff_result = {
                "baseline_present": False,
                "baseline_viewed_at": None,
                "summary": {
                    "added_count": 0,
                    "removed_count": 0,
                    "modified_count": 0
                },
                "changes": {
                    "gabu": {"added": [], "removed": [], "modified": []},
                    "eulgu": {"added": [], "removed": [], "modified": []}
                }
            }
            baseline_present = False
            baseline_viewed_at = None

        valuation_result = {
    "ok": True,
    "confidence": "HIGH",
    "sample_count": 1,
    "median_price_won": 65000000,
    "reason": None
}

        ltv_result = compute_ltv_info(current_snapshot, valuation_result)

        risk_result = compute_risk(
            current_snapshot,
            diff_result,
            valuation_result,
            ltv_result
        )

        tenant_info = {
            "deposit": req.deposit or 0,
            "move_in_date": req.move_in_date,
            "fixed_date": req.fixed_date
        }

        recovery_result = compute_recovery(
            current_snapshot,
            valuation_result,
            ltv_result,
            tenant_info,
            risk_result
        )

        if isinstance(diff_result, dict):
            diff_result["baseline_present"] = baseline_present
            diff_result["baseline_viewed_at"] = baseline_viewed_at

        final_result = {
            "riskLevel": convert_risk_level(risk_result),
            "rawData": json.dumps(current_snapshot, ensure_ascii=False),
            "mainReason": pick_main_reason(current_snapshot),
            "ltvScore": extract_ltv_score(ltv_result),

            "snapshot": current_snapshot,
            "valuation": valuation_result,
            "ltv": ltv_result,
            "diff": diff_result,
            "risk": risk_result,
            "recovery": recovery_result
        }

        save_json(result_file_path(req.doc_id), final_result)
        return final_result

    except HTTPException:
        raise
    except Exception as e:
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=f"분석 중 오류 발생: {str(e)}")


# -----------------------------
# 4) 최신 분석 결과 조회
# -----------------------------
@app.get("/result/{doc_id}")
def get_latest_result(doc_id: str):
    path = result_file_path(doc_id)
    if not os.path.exists(path):
        raise HTTPException(status_code=404, detail="최신 분석 결과가 없음")

    with open(path, "r", encoding="utf-8") as f:
        data = json.load(f)

    return data


# --------------------------------------------------
# PDF 설정 (engine.py 와 동일)
# --------------------------------------------------
_WKHTMLTOPDF_PATH = os.getenv(
    "WKHTMLTOPDF_PATH",
    r"C:\Program Files\wkhtmltopdf\bin\wkhtmltopdf.exe",
)
_PDFKIT_CONFIG = (
    pdfkit.configuration(wkhtmltopdf=_WKHTMLTOPDF_PATH)
    if os.path.exists(_WKHTMLTOPDF_PATH)
    else None
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


class CombinedPdfFromStorageRequest(BaseModel):
    """저장된 분석 결과 2개를 조합해 통합 PDF 생성"""
    origin_doc_id:  str
    new_doc_id:     str
    contract_type:  str = "JEONSE"
    deposit:        int = 0
    monthly_amount: int = 0
    maintenance_fee: int = 0
    move_date:      str = ""
    confirm_date:   str = ""


# -----------------------------
# 5) 저장된 mock 결과로 통합 PDF 생성
# -----------------------------
@router.post(
    "/generate-pdf/combined",
    summary="저장된 mock 분석 결과 2개로 통합 PDF 생성 (DIFF+RECOVERY+OCR)",
)
def generate_combined_from_storage(req: CombinedPdfFromStorageRequest):
    """mock_storage에 저장된 origin/new 분석 결과를 읽어 combined PDF를 반환한다.

    시나리오별로 다른 위험도가 나오는 doc_id 쌍을 사용하면
    SAFE / WARNING / DANGER 3가지 PDF를 생성할 수 있다.
    """
    # origin 로드
    origin_path = result_file_path(req.origin_doc_id)
    if not os.path.exists(origin_path):
        raise HTTPException(status_code=404, detail=f"origin_doc_id '{req.origin_doc_id}' 분석 결과 없음")
    with open(origin_path, "r", encoding="utf-8") as f:
        origin_raw: dict = json.load(f)

    # new 로드
    new_path = result_file_path(req.new_doc_id)
    if not os.path.exists(new_path):
        raise HTTPException(status_code=404, detail=f"new_doc_id '{req.new_doc_id}' 분석 결과 없음")
    with open(new_path, "r", encoding="utf-8") as f:
        new_raw: dict = json.load(f)

    # snapshot_name 추출
    new_snapshot  = new_raw.get("snapshot", {})
    snapshot_name = new_snapshot.get("address", {}).get("address", req.new_doc_id)

    try:
        html_content = generate_combined_html_report(
            snapshot_name=snapshot_name,
            origin_raw=origin_raw,
            new_raw=new_raw,
            contract_type=req.contract_type,
            deposit=req.deposit,
            monthly_amount=req.monthly_amount,
            maintenance_fee=req.maintenance_fee,
            move_date=req.move_date,
            confirm_date=req.confirm_date,
        )
    except Exception as e:
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=f"HTML 생성 오류: {e}")

    try:
        pdf_bytes = pdfkit.from_string(
            html_content, False,
            options=_PDFKIT_OPTIONS,
            configuration=_PDFKIT_CONFIG,
        )
    except Exception as e:
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=f"PDF 변환 오류: {e}")

    risk_level = new_raw.get("riskLevel", "unknown").lower()
    from urllib.parse import quote
    encoded_name = quote(f"combined_{risk_level}_{req.new_doc_id}.pdf", safe="")

    return Response(
        content=pdf_bytes,
        media_type="application/pdf",
        headers={
            "Content-Disposition": (
                f'attachment; filename="combined_report.pdf"; '
                f"filename*=UTF-8''{encoded_name}"
            ),
            "Content-Length": str(len(pdf_bytes)),
        },
    )
