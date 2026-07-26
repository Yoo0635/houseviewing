"""PDF: 통합 시나리오 보고서 HTML 생성 (DIFF + RECOVERY + OCR 파싱)

시나리오별(SAFE / WARNING / DANGER) 1장 PDF.
구성:
  · Page 1 — 등기부 변동 내역 (DIFF)
  · Page 2 — 보증금 회수 분석 (RECOVERY)
  · Page 3 — 등기부 OCR 파싱 텍스트 (VERIFICATION)
"""
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

# ─────────────────────────────────────────────
# 공통 메타
# ─────────────────────────────────────────────

_LEVEL_MAP = {"HIGH": "High", "MEDIUM": "Medium", "LOW": "Low"}

_RISK_META = {
    "High":   {"label": "HIGH · 위험",   "color": "#c0392b"},
    "Medium": {"label": "MEDIUM · 주의", "color": "#d35400"},
    "Low":    {"label": "LOW · 안전",    "color": "#1e8449"},
}


# ─────────────────────────────────────────────
# 헬퍼
# ─────────────────────────────────────────────

def _safe(val, default: str = "—") -> str:
    if val is None or val == "":
        return default
    return str(val)


def _entry_label(entry: dict) -> str:
    rank    = entry.get("rank", "")
    purpose = entry.get("purpose", "")
    amt     = entry.get("max_claim_amount")
    base    = f"{rank}순위 {purpose}" if rank else purpose
    return f"{base} ({fmt_krw(amt)})" if isinstance(amt, int) else base


# ─────────────────────────────────────────────
# DIFF 섹션 내부 HTML 생성
# ─────────────────────────────────────────────

def _build_changes_rows(diff: dict) -> str:
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


# ─────────────────────────────────────────────
# OCR 파싱 섹션 내부 HTML 생성
# ─────────────────────────────────────────────

def _gabu_rows(snapshot: dict) -> str:
    gabu = snapshot.get("gabu") or []
    if not gabu:
        return '<tr><td colspan="4" style="text-align:center;color:#9ca3af;padding:10px;">갑구 데이터 없음</td></tr>'
    rows = ""
    for e in gabu:
        rank    = _safe(e.get("rank"))
        purpose = _safe(e.get("purpose"))
        receipt = e.get("receipt") or {}
        date    = _safe(receipt.get("date"))
        owners  = e.get("owners") or []
        owner_str = ", ".join(
            f"{o.get('name', '?')} ({o.get('share', '')})" if o.get("share")
            else o.get("name", "?")
            for o in owners
        ) or "—"
        rows += (
            f'<tr>'
            f'<td style="text-align:center;color:#6b7280;width:36px;">{rank}</td>'
            f'<td>{purpose}</td>'
            f'<td>{owner_str}</td>'
            f'<td>{date}</td>'
            f'</tr>'
        )
    return rows


def _eulgu_rows(snapshot: dict) -> str:
    eulgu = snapshot.get("eulgu") or []
    if not eulgu:
        return '<tr><td colspan="5" style="text-align:center;color:#9ca3af;padding:10px;">을구 데이터 없음</td></tr>'
    rows = ""
    for e in eulgu:
        rank      = _safe(e.get("rank"))
        purpose   = _safe(e.get("purpose"))
        receipt   = e.get("receipt") or {}
        date      = _safe(receipt.get("date"))
        owners    = e.get("owners") or []
        creditor  = ", ".join(o.get("name", "?") for o in owners) or "—"
        max_claim = e.get("max_claim_amount")
        amount    = fmt_krw(max_claim) if max_claim is not None else "—"
        rows += (
            f'<tr>'
            f'<td style="text-align:center;color:#6b7280;width:36px;">{rank}</td>'
            f'<td>{purpose}</td>'
            f'<td>{creditor}</td>'
            f'<td style="text-align:right;">{amount}</td>'
            f'<td>{date}</td>'
            f'</tr>'
        )
    return rows


# ─────────────────────────────────────────────
# 공개 API
# ─────────────────────────────────────────────

def generate_combined_html_report(
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
    """DIFF + RECOVERY + OCR 텍스트를 하나의 PDF로 합친 HTML을 반환한다.

    Args:
        snapshot_name:   등기부 제목 (주소 등)
        origin_raw:      직전 분석 JSON dict (은섭 엔진 결과)
        new_raw:         현재 분석 JSON dict (은섭 엔진 결과)
        contract_type:   "JEONSE" | "MONTHLY"
        deposit:         보증금 (원)
        monthly_amount:  월세 (원)
        maintenance_fee: 관리비 (원)
        move_date:       전입일 (YYYY-MM-DD)
        confirm_date:    확정일자 (YYYY-MM-DD)
    """
    now = datetime.now().strftime("%Y년 %m월 %d일  %H:%M")

    # ── 위험도 정보 ──────────────────────────────
    origin_risk   = origin_raw.get("risk", {})
    new_risk      = new_raw.get("risk", {})
    origin_level  = _LEVEL_MAP.get(origin_risk.get("risk_level", "LOW"), "Low")
    new_level     = _LEVEL_MAP.get(new_risk.get("risk_level", "MEDIUM"), "Medium")
    origin_ltv    = round(float(origin_raw.get("ltv", {}).get("ltv") or 0) * 100)
    new_ltv       = round(float(new_raw.get("ltv", {}).get("ltv") or 0) * 100)
    origin_meta   = _RISK_META[origin_level]
    new_meta      = _RISK_META[new_level]
    risk_color    = new_meta["color"]

    # ── 주소 & snapshot ──────────────────────────
    new_snapshot    = new_raw.get("snapshot", {})
    origin_snapshot = origin_raw.get("snapshot", {})
    address         = new_snapshot.get("address", {}).get("address", snapshot_name)
    contract_label  = "전세" if contract_type == "JEONSE" else "월세"

    # ── DIFF 계산 ────────────────────────────────
    diff         = diff_snapshots(origin_snapshot, new_snapshot)
    summary      = diff.get("summary", {})
    changes_rows = _build_changes_rows(diff)
    guidelines   = build_guidelines_html(new_level, "diff")
    signals_html = build_signals_html(new_risk.get("signals", [])) or (
        '<div style="padding:6px 0;font-size:9pt;color:#9ca3af;">감지된 위험 시그널이 없습니다.</div>'
    )

    # ── RECOVERY 계산 ────────────────────────────
    ltv_info          = new_raw.get("ltv") or {}
    prop_val          = int(ltv_info.get("house_price_won") or 0)
    if prop_val == 0:
        prop_val      = int((new_raw.get("valuation") or {}).get("median_price_won") or 0)
    recovery_calc     = (new_raw.get("recovery") or {}).get("calculation") or {}
    expected_recovery = int(recovery_calc.get("recoverable_amount") or 0)
    recovery_rate     = round(expected_recovery / deposit * 100, 1) if deposit > 0 else 0.0

    has_residency     = bool(move_date)
    has_priority      = has_residency and bool(confirm_date)

    # 채권 최고액
    new_info = new_raw.get("risk", {})
    max_claim = int(new_info.get("checks", {}).get("max_claim_amount_total", 0))

    # 회수율 색상
    if prop_val > 0:
        rec_color = "#1e8449" if recovery_rate >= 80 else ("#d35400" if recovery_rate >= 40 else "#c0392b")
        rec_amount_text = fmt_krw(expected_recovery)
        rec_rate_text   = f"{recovery_rate:.1f}%"
        rec_sub = (
            f"부동산 시세 {fmt_krw(prop_val)}에서 "
            f"선순위 채권 {fmt_krw(max_claim)} 변제 후 임차인 보증금 배당 예상액"
        )
    else:
        rec_color       = "#6b7280"
        rec_amount_text = "계산 불가"
        rec_rate_text   = "–"
        rec_sub         = "부동산 시세 정보가 없어 예상 회수액을 산출할 수 없습니다."

    # 우선변제권
    right_color = "#1e8449" if has_priority else "#c0392b"
    right_label = "우선변제권 확보" if has_priority else "우선변제권 미확보"
    right_desc  = (
        "전입신고와 확정일자가 모두 완료되어 경매 시 배당 우선순위가 보호됩니다."
        if has_priority else
        "전입신고 또는 확정일자가 누락되어 경매 배당 순위가 후순위로 밀릴 수 있습니다."
    )

    residency_color  = "#1e8449" if has_residency else "#c0392b"
    residency_label  = "완료" if has_residency else "미완료"
    confirmed_label  = "완료" if confirm_date else "미확인"
    confirmed_color  = "#1e8449" if confirm_date else "#c0392b"

    diff_claim = max_claim - deposit
    if diff_claim > 0:
        compare_text  = f"선순위 채권이 보증금보다 {fmt_krw(diff_claim)} 많습니다."
        compare_color = "#c0392b"
    elif diff_claim < 0:
        compare_text  = f"보증금이 선순위 채권보다 {fmt_krw(abs(diff_claim))} 많습니다."
        compare_color = "#d35400"
    else:
        compare_text  = "선순위 채권과 보증금이 동일합니다."
        compare_color = "#d35400"

    # 소유자 정보
    gabu       = new_snapshot.get("gabu", [])
    owners     = gabu[0].get("owners", []) if gabu else []
    owner_name = ", ".join(o["name"] for o in owners) if owners else "미확인"

    # ── OCR 파싱 카운트 ──────────────────────────
    gabu_count  = len(new_snapshot.get("gabu") or [])
    eulgu_count = len(new_snapshot.get("eulgu") or [])
    gabu_rows_html  = _gabu_rows(new_snapshot)
    eulgu_rows_html = _eulgu_rows(new_snapshot)
    viewed_at   = _safe(new_snapshot.get("viewed_at"))

    # ── 공통 table style shorthand ───────────────
    _tbl = 'style="width:100%;border-collapse:collapse;"'
    _th  = 'style="padding:6px 9px;border:1px solid #dde3ed;background:#eef2f7;color:#1e3a5f;font-weight:600;white-space:nowrap;width:90px;"'
    _td  = 'style="padding:6px 9px;border:1px solid #dde3ed;vertical-align:middle;word-break:break-word;"'
    _thr = 'style="padding:6px 9px;border:1px solid #dde3ed;background:#1a1a2e;color:#fff;text-align:center;"'

    # ────────────────────────────────────────────────────
    # HTML 생성
    # ────────────────────────────────────────────────────
    return f"""<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8"/>
  <title>통합 분석 보고서</title>
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
    .page-break {{ page-break-before: always; }}
    /* PAGE 1 전용 — 가이드라인 오버플로 방지 */
    .page-1 {{ padding: 18px 32px 16px; }}
    .page-1 .section {{ margin-bottom: 8px; }}
    .page-1 .top-bar {{ margin-bottom: 12px; }}
    .page-1 .header {{ margin-bottom: 10px; padding-bottom: 8px; }}
    .page-1 .risk-cmp {{ margin-bottom: 6px; }}
    .page-1 .info-tbl td {{ padding: 5px 10px; }}
    .top-bar {{
      background: #1c2333; color: #fff; padding: 6px 14px;
      font-size: 8pt; letter-spacing: 1.5px; margin-bottom: 24px;
      display: table; width: 100%;
    }}
    .top-bar-l {{ display: table-cell; }}
    .top-bar-r {{ display: table-cell; text-align: right; color: #8d97aa; }}
    .header {{
      display: table; width: 100%;
      border-bottom: 3px solid #1c2333; padding-bottom: 14px; margin-bottom: 20px;
    }}
    .h-title {{ display: table-cell; vertical-align: middle; }}
    .h-title h1 {{ font-size: 19pt; font-weight: 700; color: #1c2333; }}
    .h-title .sub {{ font-size: 9pt; color: #6b7280; margin-top: 3px; }}
    .h-badge {{ display: table-cell; text-align: right; vertical-align: middle; }}
    .badge {{
      display: inline-block; background: {risk_color}; color: #fff;
      font-size: 11pt; font-weight: 700; padding: 8px 20px; border-radius: 4px;
    }}
    .section {{ margin-bottom: 18px; page-break-inside: avoid; }}
    .sec-title {{
      font-size: 9.5pt; font-weight: 700; color: #1c2333; text-transform: uppercase;
      letter-spacing: 0.8px; border-bottom: 1.5px solid #e5e7eb;
      padding-bottom: 5px; margin-bottom: 10px; page-break-after: avoid;
    }}
    .info-tbl {{ width: 100%; border-collapse: collapse; }}
    .info-tbl td {{ padding: 7px 10px; font-size: 9.5pt; border-bottom: 1px solid #f3f4f6; }}
    .info-tbl td.lbl {{
      width: 28%; font-weight: 600; color: #374151;
      background: #f9fafb; border-right: 1px solid #e5e7eb;
    }}
    /* DIFF 위험도 비교 */
    .risk-cmp {{ display: table; width: 100%; border: 1px solid #e5e7eb; border-radius: 6px; margin-bottom: 10px; page-break-inside: avoid; }}
    .risk-cell {{ display: table-cell; width: 40%; padding: 16px 20px; vertical-align: middle; text-align: center; }}
    .risk-cell.before {{ background: #f9fafb; }}
    .risk-cell.after  {{ background: {risk_color}; }}
    .risk-arrow {{ display: table-cell; vertical-align: middle; text-align: center; font-size: 16pt; color: #6b7280; }}
    .risk-lbl {{ font-size: 7.5pt; color: #9ca3af; margin-bottom: 4px; }}
    .risk-val {{ font-size: 13pt; font-weight: 700; }}
    .risk-score {{ font-size: 9pt; margin-top: 3px; }}
    .sum-badges {{ font-size: 8.5pt; color: #6b7280; }}
    .badge-item {{ display: inline-block; padding: 3px 10px; border-radius: 12px; font-size: 8pt; font-weight: 700; margin-right: 6px; }}
    .change-tbl {{ width: 100%; border-collapse: collapse; border: 1px solid #e5e7eb; }}
    /* RECOVERY 히어로 */
    .hero {{
      background: #1c2333; border-radius: 8px;
      padding: 22px 28px; margin-bottom: 20px;
      display: table; width: 100%;
    }}
    .hero-l {{ display: table-cell; vertical-align: middle; }}
    .hero-eye {{ font-size: 8pt; color: #8d97aa; letter-spacing: 1.5px; text-transform: uppercase; margin-bottom: 6px; }}
    .hero-amt {{ font-size: 26pt; font-weight: 700; color: {rec_color}; letter-spacing: -1px; line-height: 1.1; }}
    .hero-sub {{ font-size: 8pt; color: #8d97aa; margin-top: 6px; line-height: 1.6; }}
    .hero-r {{ display: table-cell; vertical-align: middle; text-align: right; width: 120px; }}
    .hero-rl {{ font-size: 8pt; color: #8d97aa; margin-bottom: 4px; }}
    .hero-rate {{ font-size: 22pt; font-weight: 700; color: {rec_color}; }}
    /* 상태 카드 */
    .status-row {{ display: table; width: 100%; border-spacing: 8px; margin-bottom: 14px; }}
    .status-card {{
      display: table-cell; width: 33%; padding: 12px 14px;
      border: 1.5px solid #e5e7eb; border-radius: 6px; vertical-align: top; text-align: center;
    }}
    .st-icon {{ font-size: 14pt; font-weight: 700; margin-bottom: 4px; }}
    .st-lbl  {{ font-size: 8pt; color: #6b7280; margin-bottom: 2px; }}
    .st-val  {{ font-size: 10pt; font-weight: 700; }}
    /* 우선변제권 카드 */
    .priority-card {{
      border-radius: 6px; padding: 14px 18px;
      border: 1.5px solid {right_color}; border-left: 5px solid {right_color};
      margin-bottom: 14px; page-break-inside: avoid;
    }}
    .priority-hl  {{ font-size: 11pt; font-weight: 700; color: {right_color}; margin-bottom: 6px; }}
    .priority-desc {{ font-size: 9.5pt; color: #374151; line-height: 1.8; }}
    /* 채권 비교 카드 */
    .cmp-card {{ display: table; width: 100%; border: 1px solid #e5e7eb; border-radius: 6px; margin-bottom: 4px; page-break-inside: avoid; }}
    .cmp-cell {{ display: table-cell; width: 50%; padding: 12px 16px; vertical-align: middle; }}
    .cmp-cell.dark {{ background: #1c2333; }}
    .cmp-cell-lbl {{ font-size: 8pt; color: #8d97aa; margin-bottom: 4px; }}
    .cmp-cell-val  {{ font-size: 14pt; font-weight: 700; color: #fff; letter-spacing: -0.5px; }}
    .cmp-cell.light {{ background: #f9fafb; }}
    .cmp-cell.light .cmp-cell-lbl {{ color: #6b7280; }}
    .cmp-cell.light .cmp-cell-val {{ color: {risk_color}; }}
    .cmp-note {{ font-size: 8.5pt; padding: 7px 12px; background: #fff8f0; border: 1px solid #fed7aa; border-radius: 4px; color: {compare_color}; font-weight: 600; }}
    /* OCR 파싱 섹션 */
    .ocr-header {{
      background: #1a1a2e; color: #fff; border-radius: 10px;
      padding: 18px 22px; margin-bottom: 20px;
    }}
    .ocr-header h2 {{ font-size: 15pt; font-weight: 700; margin-bottom: 4px; }}
    .ocr-header p  {{ font-size: 9pt; color: #a8b2c1; }}
    .ocr-notice {{
      background: #fff8e1; border: 1px solid #ffc107; border-radius: 6px;
      padding: 8px 12px; font-size: 9pt; color: #7a5c00; margin-bottom: 16px;
    }}
    .ocr-sec-title {{
      font-size: 10pt; font-weight: 700; color: #0f3460;
      border-left: 3px solid #e74c3c; padding-left: 8px;
      margin-bottom: 8px; page-break-after: avoid;
    }}
    .ocr-tbl {{ width: 100%; border-collapse: collapse; font-size: 9pt; }}
    .ocr-tbl th, .ocr-tbl td {{ padding: 6px 9px; border: 1px solid #dde3ed; vertical-align: middle; word-break: break-word; }}
    .ocr-tbl th {{ background: #eef2f7; color: #1e3a5f; font-weight: 600; white-space: nowrap; width: 90px; }}
    .ocr-tbl thead th {{ background: #1a1a2e; color: #fff; width: auto; text-align: center; }}
    .ocr-tbl tbody tr:nth-child(even) td {{ background: #f9fafb; }}
    /* 공통 */
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
    .footer-l {{ display: table-cell; }}
    .footer-r {{ display: table-cell; text-align: right; }}
    .check-list {{ list-style: none; padding: 0; }}
  </style>
</head>
<body>

<!-- ════════════════════════════════════════
     PAGE 1 : 등기부 변동 내역 (DIFF)
     ════════════════════════════════════════ -->
<div class="page page-1">
  <div class="top-bar">
    <div class="top-bar-l">등기부 변동 보고서 &nbsp;·&nbsp; REGISTRY CHANGE ANALYSIS REPORT</div>
    <div class="top-bar-r">CONFIDENTIAL</div>
  </div>

  <div class="header">
    <div class="h-title">
      <h1>등기부 변동 보고서</h1>
      <div class="sub">등기부 변동 감지 및 위험도 재평가 &nbsp;|&nbsp; {now} 생성</div>
    </div>
    <div class="h-badge">
      <span class="badge">{new_meta["label"]}</span>
    </div>
  </div>

  <div class="section">
    <div class="sec-title">기본 정보</div>
    <table class="info-tbl">
      <tr>
        <td class="lbl">소재지</td>
        <td colspan="3">{address}</td>
      </tr>
      <tr>
        <td class="lbl">계약 유형</td>
        <td>{contract_label}</td>
        <td class="lbl" style="border-left:1px solid #e5e7eb;">보증금</td>
        <td style="font-weight:700;">{fmt_krw(deposit)}</td>
      </tr>
      <tr>
        <td class="lbl">전입일</td>
        <td>{move_date or "—"}</td>
        <td class="lbl" style="border-left:1px solid #e5e7eb;">확정일자</td>
        <td>{confirm_date or "—"}</td>
      </tr>
    </table>
  </div>

  <div class="section">
    <div class="sec-title">위험도 변동</div>
    <div class="risk-cmp">
      <div class="risk-cell before">
        <div class="risk-lbl">이전 LTV</div>
        <div class="risk-val" style="color:{origin_meta['color']};">{origin_ltv}%</div>
      </div>
      <div class="risk-arrow">&#8594;</div>
      <div class="risk-cell after">
        <div class="risk-lbl" style="color:#fff;">현재 LTV</div>
        <div class="risk-val" style="color:#fff;">{new_ltv}%</div>
      </div>
    </div>
    <div class="sum-badges">
      변동 요약:
      <span class="badge-item" style="background:#fff5f5;color:#c0392b;">추가 {summary.get("added_count", 0)}건</span>
      <span class="badge-item" style="background:#fefce8;color:#d35400;">변경 {summary.get("modified_count", 0)}건</span>
      <span class="badge-item" style="background:#f0fdf4;color:#1e8449;">말소 {summary.get("removed_count", 0)}건</span>
    </div>
  </div>

  <div class="section">
    <div class="sec-title">변동 항목 상세</div>
    <table class="change-tbl">{changes_rows}</table>
  </div>

  <div class="section">
    <div class="sec-title">현재 위험 시그널</div>
    <div class="check-list">{signals_html}</div>
  </div>

  <div style="margin-bottom:8px;page-break-inside:auto;">
    <div class="sec-title">대응 가이드라인</div>
    {guidelines}
  </div>

  <div class="disclaimer">
    ※ 본 보고서는 AI 기반 자동 분석 시스템에 의해 생성된 참고 자료이며 법적 효력이 없습니다.
    등기부 변동이 감지된 경우 즉시 법무사 또는 공인중개사에게 전문 자문을 받으시기 바랍니다.
  </div>

  <div class="footer">
    <div class="footer-l">부동산 권리 분석 서비스 &copy; 2025</div>
    <div class="footer-r">생성: {now} &nbsp;|&nbsp; 1 / 3</div>
  </div>
</div>


<!-- ════════════════════════════════════════
     PAGE 2 : 보증금 회수 분석 (RECOVERY)
     ════════════════════════════════════════ -->
<div class="page page-break">
  <div class="top-bar">
    <div class="top-bar-l">보증금 회수 분석 보고서 &nbsp;·&nbsp; DEPOSIT RECOVERY ANALYSIS REPORT</div>
    <div class="top-bar-r">CONFIDENTIAL</div>
  </div>

  <div class="header">
    <div class="h-title">
      <h1>보증금 회수 분석 보고서</h1>
      <div class="sub">임차인 배당 순위 및 회수 가능성 평가 &nbsp;|&nbsp; {now} 생성</div>
    </div>
    <div class="h-badge">
      <span class="badge">{new_meta["label"]}</span>
    </div>
  </div>

  <div class="hero">
    <div class="hero-l">
      <div class="hero-eye">&#9654; 예상 회수 보증금</div>
      <div class="hero-amt">{rec_amount_text}</div>
      <div class="hero-sub">{rec_sub}</div>
    </div>
    <div class="hero-r">
      <div class="hero-rl">회수율</div>
      <div class="hero-rate">{rec_rate_text}</div>
    </div>
  </div>

  <div class="section">
    <div class="sec-title">기본 정보</div>
    <table class="info-tbl">
      <tr>
        <td class="lbl">임차인</td>
        <td>{owner_name}</td>
        <td class="lbl" style="border-left:1px solid #e5e7eb;">위험 등급</td>
        <td style="color:{risk_color};font-weight:700;">{new_meta["label"]}</td>
      </tr>
      <tr>
        <td class="lbl">소재지</td>
        <td colspan="3">{address}</td>
      </tr>
      <tr>
        <td class="lbl">보증금</td>
        <td style="font-weight:700;">{fmt_krw(deposit)}</td>
        <td class="lbl" style="border-left:1px solid #e5e7eb;">LTV</td>
        <td style="color:{risk_color};font-weight:700;">{new_ltv}%</td>
      </tr>
    </table>
  </div>

  <div class="section">
    <div class="sec-title">대항력 · 우선변제권 현황</div>
    <div class="status-row">
      <div class="status-card">
        <div class="st-icon" style="color:{residency_color};">{"O" if has_residency else "X"}</div>
        <div class="st-lbl">전입신고</div>
        <div class="st-val" style="color:{residency_color};">{residency_label}</div>
      </div>
      <div class="status-card">
        <div class="st-icon" style="color:{confirmed_color};">{"O" if confirm_date else "X"}</div>
        <div class="st-lbl">확정일자</div>
        <div class="st-val" style="color:{confirmed_color};">{confirmed_label}</div>
        <div style="font-size:8pt;color:#9ca3af;margin-top:2px;">{confirm_date or "미입력"}</div>
      </div>
      <div class="status-card">
        <div class="st-icon" style="color:{right_color};">{"O" if has_priority else "X"}</div>
        <div class="st-lbl">우선변제권</div>
        <div class="st-val" style="color:{right_color};">{"확보" if has_priority else "미확보"}</div>
      </div>
    </div>
    <div class="priority-card">
      <div class="priority-hl">{right_label}</div>
      <div class="priority-desc">{right_desc}</div>
    </div>
  </div>

  <div class="section">
    <div class="sec-title">선순위 채권 vs 보증금</div>
    <div class="cmp-card">
      <div class="cmp-cell dark">
        <div class="cmp-cell-lbl">선순위 채권최고액 합계</div>
        <div class="cmp-cell-val">{fmt_krw(max_claim)}</div>
      </div>
      <div class="cmp-cell light">
        <div class="cmp-cell-lbl">임차인 보증금</div>
        <div class="cmp-cell-val">{fmt_krw(deposit)}</div>
      </div>
    </div>
    <div class="cmp-note">&#9654; {compare_text}</div>
  </div>

  <div class="section">
    <div class="sec-title">등기부 위험 시그널</div>
    <div class="check-list">{signals_html}</div>
  </div>

  <div class="disclaimer">
    ※ 본 보고서는 AI 기반 자동 분석 시스템에 의해 생성된 참고 자료이며 법적 효력이 없습니다.
    보증금 회수 가능 금액은 실제 경매 낙찰가, 배당 순위, 소액임차인 해당 여부에 따라 달라질 수 있습니다.
  </div>

  <div class="footer">
    <div class="footer-l">부동산 권리 분석 서비스 &copy; 2025</div>
    <div class="footer-r">생성: {now} &nbsp;|&nbsp; 2 / 3</div>
  </div>
</div>


<!-- ════════════════════════════════════════
     PAGE 3 : 등기부 OCR 파싱 텍스트
     ════════════════════════════════════════ -->
<div class="page page-break">
  <div class="ocr-header">
    <h2>등기부 OCR 파싱 데이터</h2>
    <p>아래 내용은 자동 파싱된 등기부 원문 데이터입니다. 원본과 비교하여 정확도를 확인하세요.</p>
  </div>

  <div class="ocr-notice">
    ※ 갑구 {gabu_count}건 · 을구 {eulgu_count}건이 파싱되었습니다.
    파싱 오류가 있을 경우 원본 등기부등본을 직접 확인하시기 바랍니다.
  </div>

  <div class="section">
    <div class="ocr-sec-title">기본 정보</div>
    <table class="ocr-tbl">
      <tbody>
        <tr>
          <th>주소</th>
          <td colspan="3">{address}</td>
        </tr>
        <tr>
          <th>열람 일시</th>
          <td colspan="3">{viewed_at}</td>
        </tr>
        <tr>
          <th>계약 유형</th>
          <td>{contract_label}</td>
          <th style="width:90px;">보증금</th>
          <td style="font-weight:700;">{fmt_krw(deposit)}</td>
        </tr>
      </tbody>
    </table>
  </div>

  <div class="section">
    <div class="ocr-sec-title">갑구 — 소유권에 관한 사항</div>
    <table class="ocr-tbl">
      <thead>
        <tr>
          <th>순위</th>
          <th>등기목적</th>
          <th>권리자</th>
          <th>접수일</th>
        </tr>
      </thead>
      <tbody>{gabu_rows_html}</tbody>
    </table>
  </div>

  <div class="section">
    <div class="ocr-sec-title">을구 — 소유권 이외의 권리</div>
    <table class="ocr-tbl">
      <thead>
        <tr>
          <th>순위</th>
          <th>등기목적</th>
          <th>채권자 / 권리자</th>
          <th>채권최고액</th>
          <th>접수일</th>
        </tr>
      </thead>
      <tbody>{eulgu_rows_html}</tbody>
    </table>
  </div>

  <div class="disclaimer">
    ※ OCR 파싱 결과는 자동 분석에 의한 것으로 실제 등기부 원본과 차이가 있을 수 있습니다.
    중요한 결정을 내리기 전에 반드시 원본 등기부등본을 확인하시기 바랍니다.
  </div>

  <div class="footer">
    <div class="footer-l">부동산 권리 분석 서비스 &copy; 2025</div>
    <div class="footer-r">생성: {now} &nbsp;|&nbsp; 3 / 3</div>
  </div>
</div>

</body>
</html>"""
