"""PDF 1: 등기부 위험 분석 보고서 HTML 생성"""
from __future__ import annotations

import logging
import os
from datetime import datetime

from schemas.dto import RiskAnalysisRequest
from core.utils import build_guidelines_html, build_signals_html, fmt_krw

logger = logging.getLogger(__name__)

_FALLBACK_ENABLED = os.getenv("GEMINI_FALLBACK_ENABLED", "false").lower() != "false"

_RISK_META: dict[str, dict] = {
    "High": {
        "label": "HIGH · 위험",
        "color": "#c0392b",
        "light": "#fff5f5",
        "border": "#e74c3c",
        "icon": "",
        "bar_color": "#c0392b",
    },
    "Medium": {
        "label": "MEDIUM · 주의",
        "color": "#d35400",
        "light": "#fff8f0",
        "border": "#e67e22",
        "icon": "",
        "bar_color": "#e67e22",
    },
    "Low": {
        "label": "LOW · 안전",
        "color": "#1e8449",
        "light": "#f0fff4",
        "border": "#27ae60",
        "icon": "",
        "bar_color": "#27ae60",
    },
}

_PRIORITY_META = {
    "IMMEDIATE": {"label": "즉시 대응 필요",    "color": "#c0392b", "icon": ""},
    "SOON":      {"label": "계약 전 확인 필요", "color": "#d35400", "icon": ""},
    "NORMAL":    {"label": "기본 확인",         "color": "#1e8449", "icon": ""},
}

_LEGAL_TERMS = [
    ("갑구 (甲區)",  "소유권에 관한 사항을 기재하는 구역. 소유자 이름과 소유권 변동 내역이 기록됨."),
    ("을구 (乙區)",  "소유권 이외의 권리를 기재하는 구역. 근저당권·전세권·임차권 등이 기록됨."),
    ("근저당권",     "일정 한도(채권최고액) 내에서 반복적으로 담보 제공이 가능한 저당권. 은행 대출 시 주로 설정됨."),
    ("채권최고액",   "근저당권이 담보하는 최대 금액. 통상 실제 대출액의 120% 수준으로 설정됨."),
    ("말소 (抹消)",  "등기부에 기재된 권리를 삭제하는 행위. 채무 완제 후 근저당 말소 필요."),
    ("소유권보존",   "부동산을 처음으로 등기부에 등록하는 행위. 건물 신축 후 최초 등기 시 발생."),
    ("경매 배당",   "경매 낙찰금을 선순위 채권자부터 순서대로 배분하는 절차. 순위가 높을수록 먼저 변제."),
    ("위험 점수",   "등기부 분석 엔진이 산출한 0~100점 척도의 위험도. 40점 이하 안전 / 40~70점 주의 / 70점 초과 고위험."),
]


def _fallback_content(data: RiskAnalysisRequest) -> dict:
    """AI 호출 실패 시 기본 텍스트 콘텐츠."""
    score = data.ltv_percent
    if score >= 80:
        headline = "선순위 채권 과다 — 보증금 손실 위험"
        main = (
            f"LTV {score:.0f}%로 고위험 구간(80% 초과)에 해당합니다. "
            "선순위 근저당 채권액이 임차인의 보증금 회수 가능 금액을 크게 압박하고 있습니다. "
            "경매 진행 시 낙찰가에서 선순위 채권이 먼저 변제되므로, "
            "임차인 보증금의 전액 회수가 어려울 수 있습니다."
        )
    elif score >= 60:
        headline = "부분 손실 가능 — 면밀한 검토 필요"
        main = (
            f"LTV {score:.0f}%로 주의 구간(60~80%)에 해당합니다. "
            "경매 진행 시 임차인 보증금의 일부가 회수되지 않을 수 있습니다. "
            "선순위 채권 내용과 최우선 변제 요건을 반드시 확인하세요."
        )
    else:
        headline = "보증금 회수 가능성 높음"
        main = (
            f"LTV {score:.0f}%로 안전 구간(60% 미만)에 해당합니다. "
            "현재 등기부상 위험 요소가 적어 보증금 회수 가능성이 상대적으로 높습니다. "
            "단, 부동산 시장 변동에 따라 상황이 달라질 수 있으니 정기적으로 확인하세요."
        )
    return {
        "risk_headline": headline,
        "main_analysis": main,
        "recovery_comment": (
            "경매 배당 분석 결과는 현재 시세와 선순위 채권 현황을 기반으로 산출된 추정치로, "
            "실제 낙찰가에 따라 달라질 수 있습니다."
        ),
        "action_items": data.checklist,
    }


def _build_checklist_html(data: RiskAnalysisRequest) -> str:
    if data.signals:
        return build_signals_html(data.signals)
    # signals 없을 때 fallback — <div>로 list bullet 깨짐 방지
    return "".join(
        f'<div style="display:table;width:100%;padding:6px 10px;margin-bottom:4px;'
        f'background:#fff5f5;border:1px solid #fecaca;border-radius:3px;'
        f'font-size:9pt;page-break-inside:avoid;">'
        f'<span style="display:table-cell;width:14px;color:#c0392b;font-weight:700;'
        f'padding-right:6px;vertical-align:middle;">-</span>'
        f'<span style="display:table-cell;vertical-align:middle;">{item}</span>'
        f'</div>'
        for item in data.checklist
    )


def _build_playbook_html(data: RiskAnalysisRequest, meta: dict) -> str:
    pri         = _PRIORITY_META.get(data.recovery_priority, _PRIORITY_META["NORMAL"])
    step_count  = len(data.playbook)

    html = (
        f'<div style="display:table;width:100%;border:1px solid #e5e7eb;'
        f'border-left:5px solid {pri["color"]};background:#f9fafb;'
        f'padding:10px 16px;border-radius:4px;margin-bottom:14px;">'
        f'<div style="display:table-cell;font-size:10pt;font-weight:700;'
        f'color:{pri["color"]};vertical-align:middle;">'
        f'{pri["icon"]}&nbsp; {pri["label"]}</div>'
        f'<div style="display:table-cell;text-align:right;font-size:8.5pt;'
        f'color:#6b7280;vertical-align:middle;">총 {step_count}단계 조치 필요</div>'
        f'</div>'
    )

    for i, step in enumerate(data.playbook):
        step_num   = step.get("step", i + 1)
        step_title = step.get("title", f"{step_num}단계")
        how_list   = step.get("how", [])
        output_text = step.get("output", "")

        if how_list:
            how_items_html = "".join(
                f'<div style="display:table;width:100%;padding:6px 10px;margin-bottom:4px;'
                f'background:#f9fafb;border-left:3px solid #d1d5db;">'
                f'<div style="display:table-cell;width:20px;font-size:8.5pt;font-weight:700;'
                f'color:#9ca3af;vertical-align:top;padding-top:1px;">{j + 1}.</div>'
                f'<div style="display:table-cell;font-size:9pt;color:#374151;'
                f'line-height:1.7;vertical-align:top;">{h}</div>'
                f'</div>'
                for j, h in enumerate(how_list)
            )
        else:
            how_items_html = (
                f'<div style="font-size:9pt;color:#9ca3af;padding:6px 0;font-style:italic;">'
                f'수행 방법이 아직 제공되지 않았습니다.</div>'
            )

        output_html = (
            f'<div style="display:table;width:100%;background:#f0fdf4;'
            f'border:1px solid #bbf7d0;border-radius:4px;padding:8px 12px;margin-top:10px;">'
            f'<div style="display:table-cell;width:24px;font-size:11pt;color:#059669;'
            f'font-weight:700;vertical-align:middle;">&#10003;</div>'
            f'<div style="display:table-cell;width:76px;font-size:8.5pt;font-weight:700;'
            f'color:#065f46;vertical-align:middle;">기대 결과물</div>'
            f'<div style="display:table-cell;font-size:9pt;color:#047857;'
            f'vertical-align:middle;">{output_text}</div>'
            f'</div>'
        ) if output_text else ""

        html += (
            f'<div class="playbook-step">'
            f'<div style="display:table;width:100%;background:#1c2333;padding:9px 14px;">'
            f'<div style="display:table-cell;width:60px;vertical-align:middle;">'
            f'<span style="display:inline-block;background:{meta["color"]};color:#ffffff;'
            f'font-size:8pt;font-weight:700;padding:3px 8px;border-radius:3px;">STEP {step_num}</span></div>'
            f'<div style="display:table-cell;font-size:10pt;font-weight:700;'
            f'color:#ffffff;vertical-align:middle;">{step_title}</div>'
            f'</div>'
            f'<div style="padding:12px 14px;background:#ffffff;">'
            f'<div style="font-size:7.5pt;font-weight:700;color:#9ca3af;'
            f'letter-spacing:0.8px;margin-bottom:8px;">&#9654; 수행 방법</div>'
            f'{how_items_html}{output_html}'
            f'</div>'
            f'</div>'
        )

    return html


def _render_template(data: RiskAnalysisRequest, content: dict) -> str:
    meta          = _RISK_META[data.risk_score]
    now           = datetime.now().strftime("%Y년 %m월 %d일  %H:%M")
    risk_num      = data.ltv_percent
    risk_bar      = min(risk_num, 100)
    risk_bar_color = meta["bar_color"]
    max_claim_fmt  = f"{data.max_claim_amount:,}"

    action_items_html = "".join(
        f'<div style="display:table;width:100%;padding:7px 10px;margin-bottom:5px;'
        f'background:#fff8f0;border:1px solid #fed7aa;border-radius:3px;font-size:9.5pt;'
        f'page-break-inside:avoid;">'
        f'<span style="display:table-cell;width:16px;color:#d35400;font-weight:700;'
        f'padding-right:6px;vertical-align:top;">&gt;</span>'
        f'<span style="display:table-cell;vertical-align:top;line-height:1.6;">{item}</span>'
        f'</div>'
        for item in content.get("action_items", data.checklist)
    )
    checklist_html    = _build_checklist_html(data)
    playbook_html     = _build_playbook_html(data, meta)
    guidelines_html   = build_guidelines_html(data.risk_score, "risk")
    legal_terms_html  = "".join(
        f'<tr><td class="term-name">{term}</td><td class="term-desc">{desc}</td></tr>'
        for term, desc in _LEGAL_TERMS
    )

    return f"""<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8"/>
  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
  <title>부동산 권리 분석 보고서</title>
  <link rel="preconnect" href="https://fonts.googleapis.com"/>
  <link href="https://fonts.googleapis.com/css2?family=Noto+Sans+KR:wght@400;600;700&display=swap" rel="stylesheet"/>
  <style>
    * {{ box-sizing: border-box; margin: 0; padding: 0; }}

    body {{
      font-family: "Noto Sans KR", "Malgun Gothic", "맑은 고딕", "Apple SD Gothic Neo", Arial, sans-serif;
      font-size: 10pt;
      color: #1c2333;
      background: #ffffff;
      -webkit-print-color-adjust: exact;
    }}

    .page {{ width: 100%; max-width: 680px; margin: 0 auto; padding: 28px 32px 44px; }}

    .top-bar {{
      background: #1c2333; color: #ffffff; padding: 6px 14px;
      font-size: 8pt; letter-spacing: 1.5px; margin-bottom: 24px;
      display: table; width: 100%;
    }}
    .top-bar-left {{ display: table-cell; }}
    .top-bar-right {{ display: table-cell; text-align: right; color: #8d97aa; }}

    .header {{
      display: table; width: 100%;
      border-bottom: 3px solid #1c2333; padding-bottom: 14px; margin-bottom: 20px;
    }}
    .header-title {{ display: table-cell; vertical-align: middle; }}
    .header-title h1 {{ font-size: 19pt; font-weight: 700; color: #1c2333; letter-spacing: -0.5px; }}
    .header-title .subtitle {{ font-size: 9pt; color: #6b7280; margin-top: 3px; }}
    .header-badge {{ display: table-cell; text-align: right; vertical-align: middle; }}
    .badge {{
      display: inline-block; background: {meta["color"]}; color: #ffffff;
      font-size: 11pt; font-weight: 700; padding: 8px 20px; border-radius: 4px; letter-spacing: 0.3px;
    }}

    .section {{ margin-bottom: 18px; page-break-inside: avoid; }}
    .section-title {{
      font-size: 9.5pt; font-weight: 700; color: #1c2333; text-transform: uppercase;
      letter-spacing: 0.8px; border-bottom: 1.5px solid #e5e7eb; padding-bottom: 5px; margin-bottom: 10px;
      page-break-after: avoid;
    }}

    .info-table {{ width: 100%; border-collapse: collapse; }}
    .info-table td {{ padding: 7px 10px; font-size: 9.5pt; border-bottom: 1px solid #f3f4f6; }}
    .info-table td.label {{ width: 24%; font-weight: 600; color: #374151; background: #f9fafb; border-right: 1px solid #e5e7eb; }}
    .info-table td.value {{ color: #1c2333; }}

    .risk-card {{
      background: {meta["light"]}; border: 1.5px solid {meta["border"]};
      border-left: 5px solid {meta["color"]}; border-radius: 4px; padding: 14px 18px;
    }}
    .risk-headline {{ font-size: 11.5pt; font-weight: 700; color: {meta["color"]}; margin-bottom: 8px; }}
    .risk-body {{ font-size: 9.5pt; line-height: 1.8; color: #374151; }}

    .ltv-row {{ display: table; width: 100%; margin-bottom: 6px; }}
    .ltv-label {{ display: table-cell; font-size: 9pt; color: #6b7280; }}
    .ltv-value {{ display: table-cell; text-align: right; font-size: 13pt; font-weight: 700; color: {risk_bar_color}; }}
    .ltv-track {{ background: #e5e7eb; border-radius: 3px; height: 10px; position: relative; margin-bottom: 4px; }}
    .ltv-fill {{ background: {risk_bar_color}; width: {risk_bar:.1f}%; height: 100%; border-radius: 3px; }}
    .ltv-markers {{ position: relative; height: 14px; font-size: 7.5pt; color: #9ca3af; }}
    .marker-60 {{ position: absolute; left: 60%; border-left: 1px dashed #9ca3af; height: 6px; top: -16px; }}
    .marker-80 {{ position: absolute; left: 80%; border-left: 1px dashed #9ca3af; height: 6px; top: -16px; }}
    .marker-60-label {{ position: absolute; left: 58%; top: 0; }}
    .marker-80-label {{ position: absolute; left: 78%; top: 0; }}
    .ltv-legend {{ font-size: 7.5pt; color: #9ca3af; text-align: right; margin-top: 2px; }}

    .two-col {{ display: table; width: 100%; border-spacing: 10px; }}
    .col-left {{ display: table-cell; width: 55%; vertical-align: top; padding-right: 10px; }}
    .col-right {{ display: table-cell; width: 45%; vertical-align: top; }}

    .recovery-card {{ background: #1c2333; color: #ffffff; border-radius: 6px; padding: 16px 18px; page-break-inside: avoid; }}
    .recovery-card .card-label {{ font-size: 8.5pt; color: #8d97aa; margin-bottom: 6px; }}
    .recovery-card .amount {{ font-size: 16pt; font-weight: 700; color: #ffffff; margin-bottom: 10px; letter-spacing: -0.5px; }}
    .recovery-card .comment {{ font-size: 8.5pt; line-height: 1.7; color: #c5cad5; border-top: 1px solid #3a4255; padding-top: 8px; }}

    .action-list {{ list-style: none; padding: 0; }}
    .action-list li {{
      display: table; width: 100%; padding: 7px 10px; margin-bottom: 5px;
      background: #fff8f0; border: 1px solid #fed7aa; border-radius: 3px; font-size: 9.5pt;
      page-break-inside: avoid;
    }}
    .bullet {{ display: table-cell; width: 16px; color: #d35400; font-size: 9pt; padding-right: 6px; vertical-align: top; }}

    .check-list {{ list-style: none; padding: 0; }}
    .check-list li {{
      display: table; width: 100%; padding: 6px 10px; margin-bottom: 4px;
      background: #fff5f5; border: 1px solid #fecaca; border-radius: 3px; font-size: 9pt;
      page-break-inside: avoid;
    }}
    .chk-icon {{ display: table-cell; width: 14px; color: #c0392b; font-weight: 700; padding-right: 6px; }}

    .playbook-step {{ border: 1px solid #e5e7eb; border-radius: 6px; margin-bottom: 10px; page-break-inside: avoid; }}

    .term-table {{ width: 100%; border-collapse: collapse; font-size: 8.5pt; }}
    .term-table tr {{ border-bottom: 1px solid #f3f4f6; }}
    .term-name {{ width: 28%; padding: 6px 10px; font-weight: 700; color: #1c2333; background: #f9fafb; vertical-align: top; border-right: 1px solid #e5e7eb; }}
    .term-desc {{ padding: 6px 10px; color: #374151; line-height: 1.7; }}

    .disclaimer {{
      margin-top: 24px; padding: 10px 14px; background: #f9fafb;
      border: 1px solid #e5e7eb; border-radius: 3px; font-size: 7.5pt; color: #9ca3af; line-height: 1.7;
    }}

    .footer {{
      display: table; width: 100%; margin-top: 14px; font-size: 7.5pt; color: #d1d5db;
      border-top: 1px solid #e5e7eb; padding-top: 8px;
    }}
    .footer-left {{ display: table-cell; }}
    .footer-right {{ display: table-cell; text-align: right; }}
  </style>
</head>
<body>
<div class="page">

  <div class="top-bar">
    <div class="top-bar-left">부동산 권리 분석 보고서 &nbsp;·&nbsp; REAL ESTATE RIGHTS ANALYSIS REPORT</div>
    <div class="top-bar-right">CONFIDENTIAL</div>
  </div>

  <div class="header">
    <div class="header-title">
      <h1>권리 분석 보고서</h1>
      <div class="subtitle">임차인 보증금 위험도 평가 &nbsp;|&nbsp; {now} 생성</div>
    </div>
    <div class="header-badge">
      <span class="badge">{meta["icon"]}{meta["label"]}</span>
    </div>
  </div>

  <div class="section">
    <div class="section-title">기본 정보</div>
    <table class="info-table">
      <tr>
        <td class="label">임차인 성명</td>
        <td class="value">{data.user_name}</td>
        <td class="label" style="border-left:1px solid #e5e7eb;">위험 등급</td>
        <td class="value" style="color:{meta["color"]};font-weight:700;">{data.risk_score} — {meta["label"].split("·")[1].strip()}</td>
      </tr>
      <tr>
        <td class="label">소재지</td>
        <td class="value" colspan="3">{data.address}</td>
      </tr>
    </table>
  </div>

  <div class="section">
    <div class="section-title">핵심 위험 분석</div>
    <div class="risk-card">
      <div class="risk-headline">{meta["icon"]}{content.get("risk_headline", data.analysis_summary[:30])}</div>
      <div class="risk-body">{content.get("main_analysis", data.analysis_summary)}</div>
    </div>
  </div>

  <div class="section">
    <div class="section-title">등기부 위험 점수</div>
    <div class="ltv-row">
      <div class="ltv-label">LTV (담보인정비율) &nbsp;|&nbsp; 채권최고액 합계: {max_claim_fmt}원</div>
      <div class="ltv-value">{risk_num:.0f}%</div>
    </div>
    <div class="ltv-track">
      <div class="ltv-fill"></div>
      <div class="marker-60"></div>
      <div class="marker-80"></div>
    </div>
    <div class="ltv-markers">
      <span class="marker-60-label">60%</span>
      <span class="marker-80-label">80%</span>
    </div>
    <div class="ltv-legend">&#9632; 안전(~60%) &nbsp; &#9632; 주의(60~80%) &nbsp; &#9632; 위험(80%+)</div>
  </div>

  <div class="section">
    <div class="section-title">즉시 조치사항</div>
    <div class="action-list">{action_items_html}</div>
  </div>

  <div class="section">
    <div class="section-title">주의사항 체크리스트</div>
    <div class="check-list">{checklist_html}</div>
  </div>

  <div style="margin-bottom:18px;page-break-inside:auto;">
    <div class="section-title">대응 가이드라인</div>
    {guidelines_html}
  </div>

  <div style="margin-bottom:18px;page-break-inside:auto;">
    <div class="section-title">단계별 분석 플랜</div>
    {playbook_html}
  </div>

  <div class="section">
    <div class="section-title">법적 용어 가이드</div>
    <table class="term-table">
      {legal_terms_html}
    </table>
  </div>

  <!-- OCR_SNAPSHOT_HERE -->

  <div class="disclaimer">
    ※ 본 보고서는 AI 기반 자동 분석 시스템에 의해 생성된 참고 자료이며 법적 효력이 없습니다.
    실제 계약 전 반드시 법무사 또는 공인중개사의 전문 자문을 받으시기 바랍니다.
    분석 결과에 따른 모든 법적·재산적 책임은 이용자 본인에게 있습니다.
  </div>

  <div class="footer">
    <div class="footer-left">부동산 권리 분석 서비스 &copy; 2025</div>
    <div class="footer-right">생성: {now} &nbsp;|&nbsp; AI-Powered Analysis</div>
  </div>

</div>
</body>
</html>"""


def generate_html_report(data: RiskAnalysisRequest) -> str:
    """AI 텍스트 + 고정 템플릿으로 HTML 보고서 생성. AI 실패 시 fallback 사용."""
    try:
        from core.groq_client import generate_analysis_content
        content = generate_analysis_content(data)
        logger.info("AI 콘텐츠 생성 성공")
    except Exception as exc:
        if _FALLBACK_ENABLED:
            logger.warning(f"AI 실패 → 기본 텍스트로 대체: {exc}")
            content = _fallback_content(data)
        else:
            raise

    return _render_template(data, content)
