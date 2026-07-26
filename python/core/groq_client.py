"""Groq API 클라이언트 (OpenAI 호환 SDK, LLaMA 모델) — 분석 텍스트 JSON 생성"""
from __future__ import annotations

import logging
import os

from dotenv import load_dotenv
from openai import OpenAI

from core.ai_prompt import SYSTEM_PROMPT, build_prompt
from schemas.dto import RiskAnalysisRequest
from core.utils import extract_json

load_dotenv()
logger = logging.getLogger(__name__)

_API_KEY: str | None = os.getenv("GROQ_API_KEY")
_MODEL_NAME: str = os.getenv("GROQ_MODEL", "llama-3.3-70b-versatile")


def generate_analysis_content(data: RiskAnalysisRequest) -> dict:
    """Groq API 호출 → {risk_headline, main_analysis, recovery_comment, action_items}"""
    if not _API_KEY:
        raise ValueError("GROQ_API_KEY 환경 변수가 설정되지 않았습니다.")

    client = OpenAI(
        api_key=_API_KEY,
        base_url="https://api.groq.com/openai/v1",
    )

    logger.info(f"Groq 분석 텍스트 생성 시작 (model={_MODEL_NAME})")

    response = client.chat.completions.create(
        model=_MODEL_NAME,
        messages=[
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user",   "content": build_prompt(data)},
        ],
        temperature=0.4,
        max_tokens=1024,
    )

    content = extract_json(response.choices[0].message.content)
    logger.info("Groq 분석 텍스트 생성 완료")
    return content
