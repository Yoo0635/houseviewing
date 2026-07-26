"""공용 유틸리티 — 여러 모듈에서 공유하는 함수"""
from __future__ import annotations

import json
import re


def fmt_krw(amount: int) -> str:
    return f"{amount:,} 원"


def extract_json(raw: str) -> dict:
    """AI 응답 텍스트에서 JSON 추출 (마크다운 코드블록 포함 대응)."""
    raw = raw.strip()
    match = re.search(r"```(?:json)?\s*([\s\S]+?)```", raw, re.IGNORECASE)
    if match:
        raw = match.group(1).strip()
    return json.loads(raw)


def build_guidelines_html(risk_level: str, context: str = "risk") -> str:
    """위험도·컨텍스트별 대응 가이드라인 HTML (static). context: 'risk' | 'diff'"""
    _C = "#c0392b" if risk_level == "High" else ("#d35400" if risk_level == "Medium" else "#1e8449")

    _STEPS: dict = {
        ("risk", "High"): [
            ("계약 즉시 중단",
             "계약금 지급 및 계약서 서명을 즉시 보류하세요. 현재 선순위 채권 규모에서 계약을 강행하면 보증금 전액 손실 위험이 있습니다."),
            ("전문가 무료 상담 즉시 예약",
             "주택도시보증공사(HUG) 1566-9009 또는 법무사협회(1544-6272)에 연락해 계약 가능 여부를 전문가에게 확인받으세요."),
            ("전세보증보험 가입 가능 여부 확인",
             "HUG·SGI서울보증에 해당 매물의 보증보험 가입 가능 여부를 문의하세요. 보험 가입이 불가하면 계약을 포기하는 것을 강력히 권장합니다."),
            ("임차권등기명령 신청 준비 (이미 거주 중인 경우)",
             "퇴거 전 반드시 임차권등기명령을 신청해 대항력을 보호하세요. 임차권등기 없이 전출하면 우선변제권이 즉시 소멸됩니다. 관할 지방법원 또는 등기소에서 신청합니다."),
        ],
        ("risk", "Medium"): [
            ("근저당 말소 조건 특약 삽입",
             "잔금 지급일 이전까지 근저당 말소를 의무화하는 특약을 계약서에 명시하세요. 핵심 문구: '잔금 지급 전 을구 상의 근저당권 전부 말소'"),
            ("전세보증보험 즉시 가입",
             "입주 후 1개월 이내에 HUG 전세보증보험(1566-9009) 또는 SGI서울보증에 가입하세요. 보험료는 보증금의 약 0.128~0.154% 수준입니다."),
            ("전입신고 + 확정일자 이사 당일 취득",
             "이사 당일 주민센터(또는 정부24 www.gov.kr)에서 전입신고와 확정일자를 동시에 받으세요. 이날부터 우선변제권이 발생합니다."),
            ("3개월 주기 등기부 열람",
             "대법원 인터넷등기소(www.iros.go.kr)에서 3개월마다 등기부를 확인하세요. 신규 근저당·가압류 감지 시 즉시 HUG 또는 법무사에게 연락하세요."),
        ],
        ("risk", "Low"): [
            ("전입신고 + 확정일자 이사 당일 취득",
             "이사 당일 주민센터(또는 정부24 www.gov.kr)에서 전입신고와 확정일자를 반드시 받으세요. 이것이 보증금 보호의 기본이며, 하루라도 미루면 우선순위가 밀릴 수 있습니다."),
            ("전세보증보험 가입 검토",
             "현재 등기부 상태는 안전하지만, 예상치 못한 변동에 대비해 HUG 전세보증보험 가입을 적극 권장합니다. (HUG 1566-9009)"),
            ("6개월 주기 등기부 열람",
             "대법원 인터넷등기소(www.iros.go.kr)에서 반기마다 등기부를 확인하세요. 이상 감지 즉시 전문가에게 연락하세요."),
            ("이상 징후 즉시 신고",
             "관리비 고지서 미수령, 집주인 연락 두절 등 이상 징후 발생 시 즉시 HUG(1566-9009) 또는 법무사에게 연락하세요."),
        ],
        ("diff", "High"): [
            ("즉각 법무사에게 변동 내용 공유",
             "감지된 등기 변동(가압류·강제경매·소유자 변경 등)을 법무사에게 즉시 알리고 대응 방안을 협의하세요. 법무사협회 무료상담: 1544-6272"),
            ("전세보증보험 사고 접수 확인",
             "이미 보증보험에 가입되어 있다면 지금 바로 보험사에 연락해 사고 접수 및 청구 절차를 확인하세요. (HUG 1566-9009)"),
            ("임차권등기명령 신청 준비",
             "이사를 계획 중이라면 퇴거 전 반드시 임차권등기명령을 신청해 대항력을 보존하세요. 임차권등기 없이 전출하면 대항력이 즉시 소멸됩니다."),
            ("집주인에게 내용증명 발송",
             "변동 사항에 대해 집주인에게 서면으로 해명을 요구하는 내용증명을 발송하고, 추후 법적 대응에 대비해 모든 기록을 보관하세요."),
        ],
        ("diff", "Medium"): [
            ("집주인에게 변동 사항 서면 확인 요청",
             "새로운 근저당 설정 또는 권리 변동에 대해 집주인에게 서면으로 이유를 확인하고 기록을 남기세요."),
            ("전세보증보험 즉시 가입 또는 재확인",
             "아직 보증보험에 가입하지 않았다면 지금 즉시 가입하세요. 추가 등기 설정 이후에는 가입 요건을 충족하지 못할 수 있습니다. (HUG 1566-9009)"),
            ("등기부 열람 주기 단축",
             "정기 열람 주기를 단축하여 추가 변동 여부를 면밀히 모니터링하세요. 대법원 인터넷등기소(www.iros.go.kr)"),
            ("법무사 자문 예약",
             "변동 규모에 따라 계약 해지권 행사 또는 보증금 반환 청구 가능 여부를 법무사에게 확인하세요. 법무사협회 무료상담: 1544-6272"),
        ],
        ("diff", "Low"): [
            ("변동 내용 기록 및 보관",
             "이번 등기부 변동 내역을 PDF로 저장하고 기록을 안전하게 보관하세요. 향후 분쟁 발생 시 중요한 증거 자료가 됩니다."),
            ("등기부 열람 주기 단축",
             "정기 열람 주기를 단축해 추가 변동 여부를 확인하세요. 대법원 인터넷등기소(www.iros.go.kr)"),
            ("전세보증보험 유효성 확인",
             "가입된 보증보험이 만료되지 않았는지 갱신 여부를 확인하고, 만료 전 갱신하세요."),
            ("이상 징후 즉시 대응",
             "추가 등기 변동이나 집주인의 이상 징후 발견 시 즉시 HUG(1566-9009) 또는 법무사에게 연락하세요."),
        ],
    }

    _BANNER = {
        ("risk",  "High"):   "HIGH · 위험 — 즉각 대응 필요",
        ("risk",  "Medium"): "MEDIUM · 주의 — 계약 전 조치 필요",
        ("risk",  "Low"):    "LOW · 안전 — 기본 조치 유지",
        ("diff",  "High"):   "HIGH · 위험 — 즉각 대응 필요",
        ("diff",  "Medium"): "MEDIUM · 주의 — 면밀한 모니터링 필요",
        ("diff",  "Low"):    "LOW · 경미한 변동 — 기록 유지 권장",
    }

    steps  = _STEPS.get((context, risk_level), _STEPS[("risk", "Medium")])
    banner = _BANNER.get((context, risk_level), "")

    html = (
        f'<div style="background:{_C};color:#ffffff;padding:7px 14px;border-radius:4px;'
        f'margin-bottom:10px;font-size:9.5pt;font-weight:700;">{banner}</div>'
    )
    for i, (title, desc) in enumerate(steps, 1):
        html += (
            f'<div style="border:1px solid #e5e7eb;border-radius:6px;margin-bottom:6px;page-break-inside:avoid;">'
            f'<div style="display:table;width:100%;background:#1c2333;padding:6px 12px;border-radius:5px 5px 0 0;">'
            f'<div style="display:table-cell;width:64px;vertical-align:middle;">'
            f'<span style="display:inline-block;background:{_C};color:#ffffff;font-size:8pt;font-weight:700;padding:3px 8px;border-radius:3px;">STEP {i}</span>'
            f'</div>'
            f'<div style="display:table-cell;font-size:10pt;font-weight:700;color:#ffffff;vertical-align:middle;">{title}</div>'
            f'</div>'
            f'<div style="padding:5px 10px;font-size:9pt;color:#374151;line-height:1.45;">{desc}</div>'
            f'</div>'
        )
    html += (
        f'<div style="background:#f0f4ff;border:1px solid #c7d2fe;border-radius:6px;'
        f'padding:8px 12px;margin-top:4px;page-break-inside:avoid;">'
        f'<div style="font-size:8.5pt;font-weight:700;color:#3730a3;margin-bottom:5px;">&#9654; 주요 연락처 &amp; 참고 사이트</div>'
        f'<div style="display:table;width:100%;">'
        f'<div style="display:table-cell;font-size:8.5pt;color:#374151;line-height:1.7;width:50%;">'
        f'주택도시보증공사(HUG): <strong>1566-9009</strong><br/>'
        f'법무사협회 무료상담: <strong>1544-6272</strong><br/>'
        f'대한법률구조공단: <strong>132</strong>'
        f'</div>'
        f'<div style="display:table-cell;font-size:8.5pt;color:#374151;line-height:1.7;">'
        f'대법원 인터넷등기소: www.iros.go.kr<br/>'
        f'정부24 전입신고·확정일자: www.gov.kr<br/>'
        f'HUG 전세보증보험: www.khug.or.kr'
        f'</div>'
        f'</div>'
        f'</div>'
    )
    return html


def build_signals_html(signals: list) -> str:
    """위험 시그널 리스트 → badge 스타일 HTML. 빈 리스트면 빈 문자열 반환."""
    html = ""
    for sig in signals:
        severity = sig.get("severity", "MEDIUM")
        explain  = sig.get("explain", "")
        if severity == "HIGH":
            badge_bg, badge_text, item_bg, item_border = "#c0392b", "고위험", "#fff5f5", "#fecaca"
        else:
            badge_bg, badge_text, item_bg, item_border = "#d35400", "주의",   "#fff8f0", "#fed7aa"
        # <li> 대신 <div> 사용: wkhtmltopdf에서 display:table + <li> 조합 시
        # list bullet이 깨진 네모로 표시되는 문제 방지
        html += (
            f'<div style="background:{item_bg};border:1px solid {item_border};'
            f'padding:0;display:table;width:100%;margin-bottom:5px;border-radius:3px;'
            f'page-break-inside:avoid;">'
            f'<span style="display:table-cell;width:54px;background:{badge_bg};color:#ffffff;'
            f'font-size:7.5pt;font-weight:700;text-align:center;padding:8px 4px;vertical-align:middle;">'
            f'{badge_text}</span>'
            f'<span style="display:table-cell;font-size:9pt;color:#374151;padding:7px 10px;'
            f'vertical-align:middle;line-height:1.6;">{explain}</span>'
            f'</div>'
        )
    return html
