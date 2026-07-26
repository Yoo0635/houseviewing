"""/engine/analyze 라우터 — OCR 파싱 + 위험도 분석 엔드포인트"""
from __future__ import annotations

import json
import os
import shutil
import traceback
import uuid

from fastapi import APIRouter, File, Form, HTTPException, UploadFile

from schemas.dto import AnalyzeResponseDTO
from engines.ocr_core import run

router = APIRouter(prefix="/engine", tags=["OCR"])

UPLOAD_DIR = "uploads"
os.makedirs(UPLOAD_DIR, exist_ok=True)


def _convert_risk_level(result: dict) -> str:
    level = str(result.get("risk", {}).get("risk_level", "")).upper()
    if level == "HIGH":
        return "DANGER"
    elif level == "MEDIUM":
        return "WARNING"
    return "SAFE"


def _extract_main_reason(result: dict) -> str:
    eulgu = result.get("snapshot", {}).get("eulgu", [])

    if eulgu:
        purpose = eulgu[0].get("purpose")
        if purpose:
            return str(purpose)

    return "특이 위험 없음"


def extract_ltv_score(result: dict) -> int:
    try:
        ltv = float(result.get("ltv", {}).get("ltv", 0))
        return int(round(ltv * 100))
    except:
        return 0


@router.post("/analyze", response_model=AnalyzeResponseDTO)
async def analyze_registry(
    file: UploadFile = File(...),
    deposit: int = Form(0),
    move_in_date: str = Form(""),
    fixed_date: str = Form(""),
):
    if not file.filename.lower().endswith(".pdf"):
        raise HTTPException(status_code=400, detail="PDF 파일만 업로드 가능")

    filename = f"{uuid.uuid4()}.pdf"
    save_path = os.path.join(UPLOAD_DIR, filename)

    try:
        with open(save_path, "wb") as buffer:
            shutil.copyfileobj(file.file, buffer)

        tenant_info = {
            "deposit": deposit,
            "move_in_date": move_in_date,
            "fixed_date": fixed_date,
        }

        result = run(save_path, tenant_info=tenant_info)

        return {
            "riskLevel": _convert_risk_level(result),
            "rawData": json.dumps(result, ensure_ascii=False),
            "mainReason": _extract_main_reason(result),
            "ltvScore": extract_ltv_score(result),
        }

    except Exception as e:
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=f"분석 실패: {str(e)}")
