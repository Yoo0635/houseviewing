"""AI 분석 공통 프롬프트 — gemini_client, groq_client에서 공유"""
from __future__ import annotations

from schemas.dto import RiskAnalysisRequest

SYSTEM_PROMPT = """당신은 부동산 임대차 전문 법무사이자 리스크 분석 전문가입니다.
임차인의 보증금 안전성을 분석하고, 전문적이면서도 임차인이 이해하기 쉬운 한국어로 분석 내용을 작성합니다.

[작성 원칙]
- 전문적이지만 임차인 관점에서 쉽게 이해할 수 있는 언어 사용
- 구체적인 수치와 근거를 포함한 분석
- 위험 등급에 따라 경고/안심 톤 조절
- 실질적으로 도움이 되는 조치 사항 제시

[언어 규칙 — 반드시 준수]
- 모든 출력은 순수 한국어로만 작성하세요.
- 한자(漢字), 중국어, 일본어 문자를 절대 사용하지 마세요.
- 영어 약어(LTV, HUG 등)는 허용하나, 그 외 외국어는 사용하지 마세요."""

_RISK_DESC = {
    "High":   "위험 (보증금 전액 회수 불가 가능성 높음)",
    "Medium": "주의 (부분 손실 가능성 있음)",
    "Low":    "안전 (보증금 회수 가능성 높음)",
}


def build_prompt(data: RiskAnalysisRequest) -> str:
    return f"""다음 부동산 임차 분석 데이터를 바탕으로 분석 텍스트를 작성하세요.

[입력 데이터]
- 임차인: {data.user_name}
- 주소: {data.address}
- 위험 등급: {data.risk_score} ({_RISK_DESC.get(data.risk_score, '')})
- 위험 점수: {data.risk_score_num:.1f}점 / 100
- 기존 분석 요약: {data.analysis_summary}
- 주의 항목: {', '.join(data.checklist)}

아래 JSON 형식으로만 응답하세요 (다른 텍스트 없이):
{{
  "risk_headline": "위험 등급을 한 문장으로 요약 (20자 이내)",
  "main_analysis": "핵심 위험 요인과 그 이유를 구체적으로 설명 (3~4문장, 수치 포함)",
  "recovery_comment": "선순위 채권 현황과 보증금 회수 가능성 평가 (2문장)",
  "action_items": ["즉시 해야 할 조치 1", "즉시 해야 할 조치 2", "즉시 해야 할 조치 3"]
}}"""
