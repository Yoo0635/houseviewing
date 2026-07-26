"""PDF 2: 보증금 회수 분석 보고서 HTML 생성"""
from __future__ import annotations

from datetime import datetime
from schemas.dto import RecoveryRenderData
from core.utils import fmt_krw, build_signals_html


def generate_recovery_html_report(data: RecoveryRenderData) -> str:
    now = datetime.now().strftime("%Y년 %m월 %d일  %H:%M")

    _RISK_META = {
        "High":   {"label": "HIGH · 위험",   "color": "#c0392b", "icon": ""},
        "Medium": {"label": "MEDIUM · 주의", "color": "#d35400", "icon": ""},
        "Low":    {"label": "LOW · 안전",    "color": "#1e8449", "icon": ""},
    }
    meta = _RISK_META[data.risk_score]

    _RIGHT_META = {
        True:  {"label": "우선변제권 확보",  "color": "#1e8449", "icon": "&#10003;",
                "desc": "전입신고와 확정일자가 모두 완료되어 경매 시 배당 우선순위가 보호됩니다."},
        False: {"label": "우선변제권 미확보", "color": "#c0392b", "icon": "&#9888;",
                "desc": "전입신고 또는 확정일자가 누락되어 경매 배당 순위가 후순위로 밀릴 수 있습니다."},
    }
    right = _RIGHT_META[data.has_priority_right]

    residency_color = "#1e8449" if data.has_residency else "#c0392b"
    residency_label = "완료" if data.has_residency else "미완료"
    confirmed_label = "완료" if data.confirmed_date else "미확인"
    confirmed_color = "#1e8449" if data.confirmed_date else "#c0392b"

    has_prop_val = data.property_value > 0
    if has_prop_val:
        recovery_color = "#1e8449" if data.recovery_rate >= 80 else (
            "#d35400" if data.recovery_rate >= 40 else "#c0392b"
        )
        recovery_amount_text = fmt_krw(data.expected_recovery)
        recovery_rate_text   = f"{data.recovery_rate:.1f}%"
        recovery_sub = (
            f"부동산 시세 {fmt_krw(data.property_value)}에서 "
            f"선순위 채권 {fmt_krw(data.max_claim_amount)} 변제 후 "
            f"임차인 보증금 배당 예상액"
        )
    else:
        recovery_color       = "#6b7280"
        recovery_amount_text = "계산 불가"
        recovery_rate_text   = "–"
        recovery_sub         = "부동산 시세 정보가 없어 예상 회수액을 산출할 수 없습니다."

    diff = data.max_claim_amount - data.deposit_amount
    if diff > 0:
        compare_text = f"선순위 채권이 보증금보다 {fmt_krw(diff)} 많습니다."
        compare_color = "#c0392b"
    elif diff < 0:
        compare_text = f"보증금이 선순위 채권보다 {fmt_krw(abs(diff))} 많습니다."
        compare_color = "#d35400"
    else:
        compare_text = "선순위 채권과 보증금이 동일합니다."
        compare_color = "#d35400"

    signals_html = build_signals_html(data.signals) or (
        '<div style="padding:6px 0;font-size:9pt;color:#9ca3af;">감지된 위험 시그널이 없습니다.</div>'
    )

    return f"""<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8"/>
  <title>보증금 회수 분석 보고서</title>
  <link rel="preconnect" href="https://fonts.googleapis.com"/>
  <link href="https://fonts.googleapis.com/css2?family=Noto+Sans+KR:wght@400;600;700&display=swap" rel="stylesheet"/>
  <style>
    * {{ box-sizing: border-box; margin: 0; padding: 0; }}
    body {{
      font-family: "Noto Sans KR", "Malgun Gothic", Arial, sans-serif;
      font-size: 10pt; color: #1c2333; background: #ffffff;
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
    .header-title h1 {{ font-size: 19pt; font-weight: 700; color: #1c2333; }}
    .header-title .subtitle {{ font-size: 9pt; color: #6b7280; margin-top: 3px; }}
    .header-badge {{ display: table-cell; text-align: right; vertical-align: middle; }}
    .badge {{
      display: inline-block; background: {meta["color"]}; color: #ffffff;
      font-size: 11pt; font-weight: 700; padding: 8px 20px; border-radius: 4px;
    }}
    .section {{ margin-bottom: 18px; page-break-inside: avoid; }}
    .section-title {{
      font-size: 9.5pt; font-weight: 700; color: #1c2333; text-transform: uppercase;
      letter-spacing: 0.8px; border-bottom: 1.5px solid #e5e7eb;
      padding-bottom: 5px; margin-bottom: 10px; page-break-after: avoid;
    }}
    .info-table {{ width: 100%; border-collapse: collapse; }}
    .info-table td {{ padding: 7px 10px; font-size: 9.5pt; border-bottom: 1px solid #f3f4f6; }}
    .info-table td.label {{
      width: 28%; font-weight: 600; color: #374151;
      background: #f9fafb; border-right: 1px solid #e5e7eb;
    }}
    .info-table td.value {{ color: #1c2333; }}
    .status-row {{
      display: table; width: 100%; border-spacing: 8px; margin-bottom: 14px;
    }}
    .status-card {{
      display: table-cell; width: 33%; padding: 12px 14px;
      border: 1.5px solid #e5e7eb; border-radius: 6px; vertical-align: top;
      text-align: center; page-break-inside: avoid;
    }}
    .status-icon {{ font-size: 14pt; margin-bottom: 4px; }}
    .status-label {{ font-size: 8pt; color: #6b7280; margin-bottom: 2px; }}
    .status-value {{ font-size: 10pt; font-weight: 700; }}
    .priority-card {{
      border-radius: 6px; padding: 14px 18px;
      border: 1.5px solid {right["color"]}; border-left: 5px solid {right["color"]};
      margin-bottom: 14px; page-break-inside: avoid;
    }}
    .priority-headline {{
      font-size: 11pt; font-weight: 700; color: {right["color"]}; margin-bottom: 6px;
    }}
    .priority-desc {{ font-size: 9.5pt; color: #374151; line-height: 1.8; }}
    .compare-card {{
      display: table; width: 100%; border: 1px solid #e5e7eb;
      border-radius: 6px; margin-bottom: 4px; page-break-inside: avoid;
    }}
    .compare-cell {{
      display: table-cell; width: 50%; padding: 12px 16px; vertical-align: middle;
    }}
    .compare-cell.dark {{ background: #1c2333; }}
    .compare-cell-label {{ font-size: 8pt; color: #8d97aa; margin-bottom: 4px; }}
    .compare-cell-value {{
      font-size: 14pt; font-weight: 700; color: #ffffff; letter-spacing: -0.5px;
    }}
    .compare-cell.light {{ background: #f9fafb; }}
    .compare-cell.light .compare-cell-label {{ color: #6b7280; }}
    .compare-cell.light .compare-cell-value {{ color: {meta["color"]}; }}
    .compare-note {{
      font-size: 8.5pt; padding: 7px 12px;
      background: #fff8f0; border: 1px solid #fed7aa; border-radius: 4px;
      color: {compare_color}; font-weight: 600;
    }}
    .check-list {{ list-style: none; padding: 0; }}
    .disclaimer {{
      margin-top: 24px; padding: 10px 14px; background: #f9fafb;
      border: 1px solid #e5e7eb; border-radius: 3px;
      font-size: 7.5pt; color: #9ca3af; line-height: 1.7;
    }}
    .footer {{
      display: table; width: 100%; margin-top: 14px;
      font-size: 7.5pt; color: #d1d5db;
      border-top: 1px solid #e5e7eb; padding-top: 8px;
    }}
    .footer-left {{ display: table-cell; }}
    .footer-right {{ display: table-cell; text-align: right; }}
    .hero-card {{
      background: #1c2333; border-radius: 8px;
      padding: 22px 28px; margin-bottom: 20px;
      display: table; width: 100%;
    }}
    .hero-left {{ display: table-cell; vertical-align: middle; }}
    .hero-eyebrow {{
      font-size: 8pt; color: #8d97aa; letter-spacing: 1.5px;
      text-transform: uppercase; margin-bottom: 6px;
    }}
    .hero-amount {{
      font-size: 26pt; font-weight: 700; color: {recovery_color};
      letter-spacing: -1px; line-height: 1.1;
    }}
    .hero-sub {{
      font-size: 8pt; color: #8d97aa; margin-top: 6px; line-height: 1.6;
    }}
    .hero-right {{
      display: table-cell; vertical-align: middle; text-align: right;
      width: 120px;
    }}
    .hero-rate-label {{
      font-size: 8pt; color: #8d97aa; margin-bottom: 4px;
    }}
    .hero-rate {{
      font-size: 22pt; font-weight: 700; color: {recovery_color};
    }}
  </style>
</head>
<body>
<div class="page">

  <div class="top-bar">
    <div class="top-bar-left">보증금 회수 분석 보고서 &nbsp;·&nbsp; DEPOSIT RECOVERY ANALYSIS REPORT</div>
    <div class="top-bar-right">CONFIDENTIAL</div>
  </div>

  <div class="header">
    <div class="header-title">
      <h1>보증금 회수 분석 보고서</h1>
      <div class="subtitle">임차인 배당 순위 및 회수 가능성 평가 &nbsp;|&nbsp; {now} 생성</div>
    </div>
    <div class="header-badge">
      <span class="badge">{meta["icon"]}{meta["label"]}</span>
    </div>
  </div>

  <div class="hero-card">
    <div class="hero-left">
      <div class="hero-eyebrow">&#9654; 예상 회수 보증금</div>
      <div class="hero-amount">{recovery_amount_text}</div>
      <div class="hero-sub">{recovery_sub}</div>
    </div>
    <div class="hero-right">
      <div class="hero-rate-label">회수율</div>
      <div class="hero-rate">{recovery_rate_text}</div>
    </div>
  </div>

  <div class="section">
    <div class="section-title">기본 정보</div>
    <table class="info-table">
      <tr>
        <td class="label">임차인 성명</td>
        <td class="value">{data.user_name}</td>
        <td class="label" style="border-left:1px solid #e5e7eb;">위험 등급</td>
        <td class="value" style="color:{meta["color"]};font-weight:700;">{meta["label"]}</td>
      </tr>
      <tr>
        <td class="label">소재지</td>
        <td class="value" colspan="3">{data.address}</td>
      </tr>
      <tr>
        <td class="label">보증금</td>
        <td class="value" style="font-weight:700;">{fmt_krw(data.deposit_amount)}</td>
        <td class="label" style="border-left:1px solid #e5e7eb;">위험 점수</td>
        <td class="value" style="color:{meta["color"]};font-weight:700;">{data.ltv_percent:.0f}점 / 100</td>
      </tr>
    </table>
  </div>

  <div class="section">
    <div class="section-title">대항력 · 우선변제권 현황</div>
    <div class="status-row">
      <div class="status-card">
        <div class="status-icon" style="color:{residency_color};font-size:16pt;font-weight:700;">
          {"O" if data.has_residency else "X"}
        </div>
        <div class="status-label">전입신고</div>
        <div class="status-value" style="color:{residency_color};">{residency_label}</div>
      </div>
      <div class="status-card">
        <div class="status-icon" style="color:{confirmed_color};font-size:16pt;font-weight:700;">
          {"O" if data.confirmed_date else "X"}
        </div>
        <div class="status-label">확정일자</div>
        <div class="status-value" style="color:{confirmed_color};">{confirmed_label}</div>
        <div style="font-size:8pt;color:#9ca3af;margin-top:2px;">{data.confirmed_date or "미입력"}</div>
      </div>
      <div class="status-card">
        <div class="status-icon" style="color:{right["color"]};font-size:16pt;font-weight:700;">
          {"O" if data.has_priority_right else "X"}
        </div>
        <div class="status-label">우선변제권</div>
        <div class="status-value" style="color:{right["color"]};">{"확보" if data.has_priority_right else "미확보"}</div>
      </div>
    </div>
    <div class="priority-card">
      <div class="priority-headline">{right["icon"]} &nbsp;{right["label"]}</div>
      <div class="priority-desc">{right["desc"]}</div>
    </div>
  </div>

  <div class="section">
    <div class="section-title">선순위 채권 vs 보증금</div>
    <div class="compare-card">
      <div class="compare-cell dark">
        <div class="compare-cell-label">선순위 채권최고액 합계</div>
        <div class="compare-cell-value">{fmt_krw(data.max_claim_amount)}</div>
      </div>
      <div class="compare-cell light">
        <div class="compare-cell-label">임차인 보증금</div>
        <div class="compare-cell-value">{fmt_krw(data.deposit_amount)}</div>
      </div>
    </div>
    <div class="compare-note">&#9654; {compare_text}</div>
  </div>

  <div class="section">
    <div class="section-title">등기부 위험 시그널</div>
    <div class="check-list">{signals_html}</div>
  </div>

  <div class="disclaimer">
    ※ 본 보고서는 AI 기반 자동 분석 시스템에 의해 생성된 참고 자료이며 법적 효력이 없습니다.
    보증금 회수 가능 금액은 실제 경매 낙찰가, 배당 순위, 소액임차인 해당 여부에 따라 달라질 수 있습니다.
    실제 계약 전 반드시 법무사 또는 공인중개사의 전문 자문을 받으시기 바랍니다.
  </div>

  <div class="footer">
    <div class="footer-left">부동산 권리 분석 서비스 &copy; 2025</div>
    <div class="footer-right">생성: {now} &nbsp;|&nbsp; AI-Powered Analysis</div>
  </div>

</div>
</body>
</html>"""
