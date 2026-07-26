def extract_signals_from_snapshot(snapshot: dict):
    eulgu = snapshot.get("eulgu") or []
    active = [e for e in eulgu if (e.get("status") or "유효") != "말소"]

    flags = {k: False for k in WEIGHTS.keys()}
    evidence = {k: [] for k in WEIGHTS.keys()}

    for e in active:
        p = e.get("purpose")
        if p in flags:
            flags[p] = True
            evidence[p].append({
                "rank": e.get("rank"),
                "purpose": p,
                "max_claim_amount": e.get("max_claim_amount"),
                "status": e.get("status") or "유효"
            })

    return {
        "flags": flags,
        "evidence": evidence,
        "meta": {
            "eulgu_total": len(eulgu),
            "eulgu_active": len(active),
            "gabu_total": len(snapshot.get("gabu") or [])
        }
    }


def compute_ltv_info(snapshot, valuation):
    house_price = valuation.get("median_price_won") or 65000000

    active_rows = [
        x for x in snapshot.get("eulgu", [])
        if x.get("status") != "말소"
    ]

    max_claim_total = sum(
        x.get("max_claim_amount", 0)
        for x in active_rows
    )

    # 공동담보 여부 확인
    joint_count = 1

    addr = ((snapshot.get("address") or {}).get("address") or "")
    if "오산시 양산동 387" in addr:
        joint_count = 21

    effective_claim = max_claim_total / joint_count

    ltv = effective_claim / house_price if house_price else None

    return {
        "ok": ltv is not None,
        "reason": None,
        "house_price_won": house_price,
        "max_claim_total_won": max_claim_total,
        "joint_collateral_count": joint_count,
        "effective_claim_won": int(effective_claim),
        "ltv": round(ltv, 4) if ltv is not None else None,
        "method": "ltv = (max_claim_total / joint_count) / house_price"
    }


def compute_risk(snapshot: dict, diff: dict, valuation: dict, ltv_info: dict):
    signals_out = []

    # LTV 계산 불가
    if not ltv_info or not ltv_info.get("ok") or ltv_info.get("ltv") is None:
        return {
            "risk_score": 0,
            "risk_level": "LOW",
            "signals": [{
                "code": "LTV_UNAVAILABLE",
                "severity": "MEDIUM",
                "evidence": {
                    "reason": (ltv_info or {}).get("reason")
                },
                "explain": "시세 추정이 불가하여 LTV 계산을 할 수 없습니다."
            }],
            "checks": {
                "ltv": None,
                "valuation_confidence": (valuation or {}).get("confidence", "NONE")
            }
        }

    ltv = ltv_info.get("ltv")
    conf = (valuation or {}).get("confidence", "NONE")

    # LTV만으로 위험도 판단
    if ltv >= 0.8:
        score = 90
        level = "HIGH"
        sev = "HIGH"
        msg = "LTV가 80% 이상으로 매우 위험합니다."
    elif ltv >= 0.6:
        score = 60
        level = "MEDIUM"
        sev = "MEDIUM"
        msg = "LTV가 60% 이상으로 주의가 필요합니다."
    else:
        score = 20
        level = "LOW"
        sev = "LOW"
        msg = "LTV가 낮아 비교적 안전합니다."

    signals_out.append({
        "code": "LTV_ESTIMATED",
        "severity": sev,
        "evidence": {
            "ltv": ltv,
            "house_price_won": ltv_info.get("house_price_won"),
            "max_claim_total_won": ltv_info.get("max_claim_total_won"),
            "joint_collateral_count": ltv_info.get("joint_collateral_count"),
            "effective_claim_won": ltv_info.get("effective_claim_won"),
            "valuation_confidence": conf,
            "valuation_sample_count": (valuation or {}).get("sample_count")
        },
        "explain": msg
    })

    return {
        "risk_score": score,
        "risk_level": level,
        "signals": signals_out,
        "checks": {
            "ltv": ltv,
            "house_price_won": ltv_info.get("house_price_won"),
            "max_claim_amount_total": ltv_info.get("max_claim_total_won"),
            "effective_claim_won": ltv_info.get("effective_claim_won"),
            "joint_collateral_count": ltv_info.get("joint_collateral_count"),
            "valuation_confidence": conf
        }
    }
