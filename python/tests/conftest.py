"""공통 픽스처 및 목 데이터"""
import json
import pytest

# ─── 공통 분석 JSON (은섭 엔진 출력 형태) ───────────────────────────────────
RAW_HIGH = {
    "snapshot": {
        "address": {"address": "경기도 오산시 양산동 387 에덴하우스 제105호"},
        "viewed_at": "2025-04-10",
        "gabu": [
            {
                "rank": "1", "purpose": "소유권이전",
                "owners": [{"name": "홍길동", "share": ""}],
                "receipt": {"date": "2020-03-15"},
            }
        ],
        "eulgu": [
            {
                "rank": "1", "purpose": "근저당권설정",
                "owners": [{"name": "국민은행"}],
                "max_claim_amount": 373000000,
                "receipt": {"date": "2020-03-15"},
            }
        ],
    },
    "risk": {
        "risk_level": "HIGH",
        "risk_score": 100,
        "checks": {"max_claim_amount_total": 373000000},
        "signals": [
            {"severity": "HIGH",   "explain": "선순위 근저당 채권최고액이 보증금을 초과합니다."},
            {"severity": "MEDIUM", "explain": "소액임차인 최우선변제 요건 미달 위험이 있습니다."},
        ],
    },
    "ltv": {"ltv_ratio": 1.2, "ltv_score": 100, "house_price_won": 310000000},
    "recovery": {
        "priority": "IMMEDIATE",
        "playbook": [
            {"step": 1, "title": "전문가 상담 즉시 예약", "how": ["HUG에 연락하세요."], "output": "상담 예약 완료"},
        ],
        "calculation": {"recoverable_amount": 0},
    },
}

RAW_LOW = {
    "snapshot": RAW_HIGH["snapshot"],
    "risk": {
        "risk_level": "LOW",
        "risk_score": 20,
        "checks": {"max_claim_amount_total": 50000000},
        "signals": [],
    },
    "ltv": {"ltv_ratio": 0.3, "ltv_score": 20, "house_price_won": 450000000},
    "recovery": {
        "priority": "NORMAL",
        "playbook": [],
        "calculation": {"recoverable_amount": 150000000},
    },
}

RAW_HIGH_STR = json.dumps(RAW_HIGH, ensure_ascii=False)
RAW_LOW_STR  = json.dumps(RAW_LOW,  ensure_ascii=False)
