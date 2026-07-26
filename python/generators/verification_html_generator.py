"""OCR 파싱 데이터 요약 페이지 생성기

risk 보고서 HTML 마지막에 삽입할 '등기부 파싱 데이터' 페이지를 생성한다.
별도 PDF가 아니라, 기존 HTML의 </body> 직전에 주입하는 방식으로 사용한다.
"""
from __future__ import annotations


# ─────────────────────────────────────────────
# 헬퍼
# ─────────────────────────────────────────────

def _fmt_krw(amount) -> str:
    try:
        return f"{int(amount):,}원"
    except Exception:
        return "—"


def _safe(val, default: str = "—") -> str:
    if val is None or val == "":
        return default
    return str(val)


# ─────────────────────────────────────────────
# 섹션 렌더러
# ─────────────────────────────────────────────

def _address_rows(snapshot: dict) -> str:
    addr       = snapshot.get("address") or {}
    address    = _safe(addr.get("address"), "파싱 실패")
    viewed_at  = _safe(snapshot.get("viewed_at"))
    return f"""
<tr><th>주소</th><td colspan="3">{address}</td></tr>
<tr><th>열람 일시</th><td colspan="3">{viewed_at}</td></tr>
"""


def _gabu_rows(snapshot: dict) -> str:
    gabu = snapshot.get("gabu") or []
    if not gabu:
        return '<tr><td colspan="4" class="empty">갑구 데이터 없음</td></tr>'
    rows = ""
    for e in gabu:
        rank    = _safe(e.get("rank"))
        purpose = _safe(e.get("purpose"))
        receipt = e.get("receipt") or {}
        date    = _safe(receipt.get("date"))
        owners  = e.get("owners") or []
        owner_str = ", ".join(
            f"{o.get('name','?')} ({o.get('share','')})" if o.get("share")
            else o.get("name", "?")
            for o in owners
        ) or "—"
        rows += f"<tr><td class='c'>{rank}</td><td>{purpose}</td><td>{owner_str}</td><td>{date}</td></tr>"
    return rows


def _eulgu_rows(snapshot: dict) -> str:
    eulgu = snapshot.get("eulgu") or []
    if not eulgu:
        return '<tr><td colspan="5" class="empty">을구 데이터 없음</td></tr>'
    rows = ""
    for e in eulgu:
        rank      = _safe(e.get("rank"))
        purpose   = _safe(e.get("purpose"))
        receipt   = e.get("receipt") or {}
        date      = _safe(receipt.get("date"))
        owners    = e.get("owners") or []
        creditor  = ", ".join(o.get("name", "?") for o in owners) or "—"
        max_claim = e.get("max_claim_amount")
        amount    = _fmt_krw(max_claim) if max_claim is not None else "—"
        rows += f"<tr><td class='c'>{rank}</td><td>{purpose}</td><td>{creditor}</td><td class='r'>{amount}</td><td>{date}</td></tr>"
    return rows


# ─────────────────────────────────────────────
# CSS (인라인 — 기존 보고서 스타일과 독립)
# ─────────────────────────────────────────────

_PAGE_CSS = """
.snap-page {
  font-family: 'Noto Sans KR', 'Malgun Gothic', sans-serif;
  font-size: 11px;
  color: #1a1a2e;
  padding: 24px 28px;
}
.snap-header {
  background: #1a1a2e;
  color: #ffffff;
  border-radius: 8px;
  border: 2px solid #1a1a2e;
  padding: 14px 20px;
  margin-bottom: 16px;
}
.snap-header h2 { font-size: 14px; font-weight: 700; margin-bottom: 8px; color: #ffffff; }
.snap-header-notice {
  font-size: 11px;
  font-weight: 600;
  color: #1a1a2e;
  background: #fffde7;
  border: 1.5px solid #f9a825;
  border-radius: 5px;
  padding: 7px 12px;
  margin-bottom: 16px;
}
.snap-section { margin-bottom: 18px; page-break-inside: avoid; }
.snap-section-title {
  font-size: 11px;
  font-weight: 700;
  color: #0f3460;
  border-left: 3px solid #e74c3c;
  padding-left: 8px;
  margin-bottom: 8px;
  page-break-after: avoid;
}
.snap-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 10px;
}
.snap-table th, .snap-table td {
  padding: 6px 9px;
  border: 1px solid #dde3ed;
  vertical-align: middle;
  word-break: break-word;
}
.snap-table th {
  background: #eef2f7;
  color: #1e3a5f;
  font-weight: 600;
  white-space: nowrap;
  width: 90px;
}
.snap-table thead th {
  background: #1a1a2e;
  color: #fff;
  width: auto;
  text-align: center;
}
.snap-table tbody tr:nth-child(even) td { background: #f9fafb; }
.snap-table td.c { text-align: center; color: #6b7280; width: 36px; }
.snap-table td.r { text-align: right; }
.snap-table td.empty { text-align: center; color: #9ca3af; padding: 10px; }
.snap-notice {
  background: #fff8e1;
  border: 1px solid #ffc107;
  border-radius: 6px;
  padding: 8px 12px;
  font-size: 10px;
  color: #7a5c00;
  margin-bottom: 16px;
}
"""


# ─────────────────────────────────────────────
# 공개 API
# ─────────────────────────────────────────────

def build_snapshot_page(snapshot: dict, page_break: bool = True) -> str:
    """등기부 파싱 데이터 요약 <div>를 반환한다.

    Args:
        snapshot: ocr_core.run() 또는 rawData JSON의 'snapshot' 키
        page_break: True면 새 페이지에서 시작 (combined/diff용),
                    False면 이전 내용에 이어서 출력 (risk 단일 PDF용)
    Returns:
        <style>…</style><div class="snap-page">…</div> 문자열
    """
    gabu_count  = len(snapshot.get("gabu") or [])
    eulgu_count = len(snapshot.get("eulgu") or [])

    addr_rows  = _address_rows(snapshot)
    gabu_rows  = _gabu_rows(snapshot)
    eulgu_rows = _eulgu_rows(snapshot)

    page_break_css = "page-break-before: always;" if page_break else "page-break-before: auto;"
    return f"""
<style>{_PAGE_CSS}</style>
<div class="snap-page" style="{page_break_css}">

  <div class="snap-header">
    <h2>등기부 OCR 파싱 데이터</h2>
  </div>
  <div class="snap-header-notice">
    ⚠ 아래 내용은 자동 파싱된 등기부 원문 데이터입니다. 원본과 비교하여 정확도를 확인하세요.
    &nbsp;|&nbsp; 갑구 {gabu_count}건 · 을구 {eulgu_count}건 파싱 완료
  </div>

  <!-- 기본 정보 -->
  <div class="snap-section">
    <div class="snap-section-title">기본 정보</div>
    <table class="snap-table">
      <tbody>{addr_rows}</tbody>
    </table>
  </div>

  <!-- 갑구 -->
  <div class="snap-section">
    <div class="snap-section-title">갑구 — 소유권에 관한 사항</div>
    <table class="snap-table">
      <thead>
        <tr><th>순위</th><th>등기목적</th><th>권리자</th><th>접수일</th></tr>
      </thead>
      <tbody>{gabu_rows}</tbody>
    </table>
  </div>

  <!-- 을구 -->
  <div class="snap-section">
    <div class="snap-section-title">을구 — 소유권 이외의 권리</div>
    <table class="snap-table">
      <thead>
        <tr><th>순위</th><th>등기목적</th><th>채권자 / 권리자</th><th>채권최고액</th><th>접수일</th></tr>
      </thead>
      <tbody>{eulgu_rows}</tbody>
    </table>
  </div>

</div>
"""
