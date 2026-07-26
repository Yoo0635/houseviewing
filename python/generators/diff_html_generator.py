"""PDF 3: 등기부 변동 비교 보고서 HTML 생성"""
from __future__ import annotations
from datetime import datetime
from core.utils import fmt_krw, build_signals_html, build_guidelines_html

try:
    from engines.diff_engine import diff_snapshots
except ImportError:
    def diff_snapshots(baseline: dict, current: dict) -> dict:  # type: ignore[misc]
        return {
            "baseline_present": False,
            "summary": {"added_count": 0, "removed_count": 0, "modified_count": 0},
            "changes": {
                "gabu": {"added": [], "removed": [], "modified": []},
                "eulgu": {"added": [], "removed": [], "modified": []},
            },
        }

_LEVEL_MAP = {"HIGH": "High", "MEDIUM": "Medium", "LOW": "Low"}
_RISK_META = {
    "High":   {"label": "HIGH · 위험",   "color": "#c0392b", "icon": ""},
    "Medium": {"label": "MEDIUM · 주의", "color": "#d35400", "icon": ""},
    "Low":    {"label": "LOW · 안전",    "color": "#1e8449", "icon": ""},
}


def _entry_label(entry: dict) -> str:
    """등기부 항목 → 읽기 쉬운 레이블"""
    rank    = entry.get("rank", "")
    purpose = entry.get("purpose", "")
    amt     = entry.get("max_claim_amount")
    base    = f"{rank}순위 {purpose}" if rank else purpose
    return f"{base} ({fmt_krw(amt)})" if isinstance(amt, int) else base


def _build_changes_html(diff: dict) -> str:
    changes  = diff.get("changes", {})
    added    = changes.get("eulgu", {}).get("added", [])
    removed  = changes.get("eulgu", {}).get("removed", [])
    modified = changes.get("eulgu", {}).get("modified", [])
    gabu_mod = changes.get("gabu", {}).get("modified", [])

    rows = ""
    for e in added:
        rows += (
            f'<tr><td style="padding:7px 10px;border-bottom:1px solid #fecaca;background:#fff5f5;">'
            f'<span style="display:inline-block;background:#c0392b;color:#fff;font-size:7.5pt;'
            f'font-weight:700;padding:2px 7px;border-radius:3px;margin-right:6px;">추가</span>'
            f'{_entry_label(e)}</td></tr>'
        )
    for e in removed:
        rows += (
            f'<tr><td style="padding:7px 10px;border-bottom:1px solid #bbf7d0;background:#f0fdf4;">'
            f'<span style="display:inline-block;background:#1e8449;color:#fff;font-size:7.5pt;'
            f'font-weight:700;padding:2px 7px;border-radius:3px;margin-right:6px;">말소</span>'
            f'{_entry_label(e)}</td></tr>'
        )
    for m in modified:
        before_label = _entry_label(m.get("before", {}))
        after_label  = _entry_label(m.get("after", {}))
        rows += (
            f'<tr><td style="padding:7px 10px;border-bottom:1px solid #fef08a;background:#fefce8;">'
            f'<span style="display:inline-block;background:#d35400;color:#fff;font-size:7.5pt;'
            f'font-weight:700;padding:2px 7px;border-radius:3px;margin-right:6px;">변경</span>'
            f'{before_label} → {after_label}</td></tr>'
        )
    for _ in gabu_mod:
        rows += (
            f'<tr><td style="padding:7px 10px;border-bottom:1px solid #fef08a;background:#fefce8;">'
            f'<span style="display:inline-block;background:#d35400;color:#fff;font-size:7.5pt;'
            f'font-weight:700;padding:2px 7px;border-radius:3px;margin-right:6px;">갑구변경</span>'
            f'소유자 정보 변동</td></tr>'
        )

    return rows or '<tr><td style="padding:10px;color:#9ca3af;font-size:9pt;">변경된 항목이 없습니다.</td></tr>'


def generate_diff_html_report(
    snapshot_name: str,
    origin_raw: dict,
    new_raw: dict,
    contract_type: str,
    deposit: int,
    monthly_amount: int,
    maintenance_fee: int,
    move_date: str,
    confirm_date: str,
) -> str:
    now = datetime.now().strftime("%Y년 %m월 %d일  %H:%M")

    # 위험도 비교
    origin_risk = origin_raw.get("risk", {})
    new_risk    = new_raw.get("risk", {})
    origin_level = _LEVEL_MAP.get(origin_risk.get("risk_level", "LOW"), "Low")
    new_level    = _LEVEL_MAP.get(new_risk.get("risk_level", "MEDIUM"), "Medium")
    origin_ltv   = round(float(origin_raw.get("ltv", {}).get("ltv") or 0) * 100)
    new_ltv      = round(float(new_raw.get("ltv", {}).get("ltv") or 0) * 100)
    origin_meta  = _RISK_META[origin_level]
    new_meta     = _RISK_META[new_level]

    # 주소
    new_snapshot = new_raw.get("snapshot", {})
    address      = new_snapshot.get("address", {}).get("address", snapshot_name)

    # diff 계산
    origin_snapshot = origin_raw.get("snapshot", {})
    diff    = diff_snapshots(origin_snapshot, new_snapshot)
    summary = diff.get("summary", {})

    # 변경 항목 HTML
    changes_html = _build_changes_html(diff)

    # 대응 가이드라인
    guidelines_html = build_guidelines_html(new_level, "diff")

    # 현재 위험 시그널
    new_signals  = new_risk.get("signals", [])
    signals_html = build_signals_html(new_signals) or (
        '<div style="padding:6px 0;font-size:9pt;color:#9ca3af;">감지된 위험 시그널이 없습니다.</div>'
    )

    contract_label   = "전세" if contract_type == "JEONSE" else "월세"
    risk_change_color = new_meta["color"]

    return f"""<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8"/>
  <title>등기부 변동 보고서</title>
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
      display: inline-block; background: {risk_change_color}; color: #ffffff;
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
    .risk-compare {{
      display: table; width: 100%; border: 1px solid #e5e7eb;
      border-radius: 6px; margin-bottom: 10px; page-break-inside: avoid;
    }}
    .risk-cell {{ display: table-cell; width: 40%; padding: 16px 20px; vertical-align: middle; text-align: center; }}
    .risk-cell.before {{ background: #f9fafb; }}
    .risk-cell.after {{ background: {risk_change_color}; }}
    .risk-arrow {{ display: table-cell; vertical-align: middle; text-align: center; font-size: 16pt; color: #6b7280; }}
    .risk-label {{ font-size: 7.5pt; color: #9ca3af; margin-bottom: 4px; }}
    .risk-value {{ font-size: 13pt; font-weight: 700; }}
    .risk-score-val {{ font-size: 9pt; margin-top: 3px; }}
    .summary-badges {{ font-size: 8.5pt; color: #6b7280; }}
    .badge-item {{
      display: inline-block; padding: 3px 10px; border-radius: 12px;
      font-size: 8pt; font-weight: 700; margin-right: 6px;
    }}
    .change-table {{ width: 100%; border-collapse: collapse; border: 1px solid #e5e7eb; }}
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
  </style>
</head>
<body>
<div class="page">

  <div class="top-bar">
    <div class="top-bar-left">등기부 변동 보고서 &nbsp;·&nbsp; REGISTRY CHANGE ANALYSIS REPORT</div>
    <div class="top-bar-right">CONFIDENTIAL</div>
  </div>

  <div class="header">
    <div class="header-title">
      <h1>등기부 변동 보고서</h1>
      <div class="subtitle">등기부 변동 감지 및 위험도 재평가 &nbsp;|&nbsp; {now} 생성</div>
    </div>
    <div class="header-badge">
      <span class="badge">{new_meta["icon"]}{new_meta["label"]}</span>
    </div>
  </div>

  <div class="section">
    <div class="section-title">기본 정보</div>
    <table class="info-table">
      <tr>
        <td class="label">소재지</td>
        <td colspan="3">{address}</td>
      </tr>
      <tr>
        <td class="label">계약 유형</td>
        <td>{contract_label}</td>
        <td class="label" style="border-left:1px solid #e5e7eb;">보증금</td>
        <td style="font-weight:700;">{fmt_krw(deposit)}</td>
      </tr>
      <tr>
        <td class="label">전입일</td>
        <td>{move_date}</td>
        <td class="label" style="border-left:1px solid #e5e7eb;">확정일자</td>
        <td>{confirm_date}</td>
      </tr>
    </table>
  </div>

  <div class="section">
    <div class="section-title">위험도 변동</div>
    <div class="risk-compare">
      <div class="risk-cell before">
        <div class="risk-label">이전 LTV</div>
        <div class="risk-value" style="color:{origin_meta["color"]};">{origin_ltv}%</div>
      </div>
      <div class="risk-arrow">&#8594;</div>
      <div class="risk-cell after">
        <div class="risk-label" style="color:#ffffff;">현재 LTV</div>
        <div class="risk-value" style="color:#ffffff;">{new_ltv}%</div>
      </div>
    </div>
    <div class="summary-badges">
      변동 요약:
      <span class="badge-item" style="background:#fff5f5;color:#c0392b;">추가 {summary.get("added_count", 0)}건</span>
      <span class="badge-item" style="background:#fefce8;color:#d35400;">변경 {summary.get("modified_count", 0)}건</span>
      <span class="badge-item" style="background:#f0fdf4;color:#1e8449;">말소 {summary.get("removed_count", 0)}건</span>
    </div>
  </div>

  <div class="section">
    <div class="section-title">변동 항목 상세</div>
    <table class="change-table">
      {changes_html}
    </table>
  </div>

  <div class="section">
    <div class="section-title">현재 위험 시그널</div>
    <div class="check-list">{signals_html}</div>
  </div>

  <div style="margin-bottom:18px;page-break-inside:auto;">
    <div class="section-title">대응 가이드라인</div>
    {guidelines_html}
  </div>

  <div class="disclaimer">
    ※ 본 보고서는 AI 기반 자동 분석 시스템에 의해 생성된 참고 자료이며 법적 효력이 없습니다.
    등기부 변동이 감지된 경우 즉시 법무사 또는 공인중개사에게 전문 자문을 받으시기 바랍니다.
    변동 내용에 따라 임차권등기, 보증보험 청구 등 즉각적인 대응이 필요할 수 있습니다.
  </div>

  <div class="footer">
    <div class="footer-left">부동산 권리 분석 서비스 &copy; 2025</div>
    <div class="footer-right">생성: {now} &nbsp;|&nbsp; AI-Powered Analysis</div>
  </div>

</div>
</body>
</html>"""
