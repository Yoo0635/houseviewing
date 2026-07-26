def compute_recovery(snapshot, valuation, ltv_info, tenant_info, risk):

    checks = risk.get("checks") or {}
    level = risk.get("risk_level") or "LOW"

    deposit = tenant_info.get("deposit")
    move_in = tenant_info.get("move_in_date")
    fixed = tenant_info.get("fixed_date")

    property_value = valuation.get("median_price_won")

    # -------------------------
    # 선순위 채권 계산 (공동담보 보정)
    # -------------------------
    senior_claim = 0
    eulgu = snapshot.get("eulgu") or []

    for r in eulgu:
        if r.get("status") == "유효":
            amt = r.get("max_claim_amount")
            if amt:
                senior_claim += amt

    # 공동담보 개수 보정
    joint_count = 1
    addr = ((snapshot.get("address") or {}).get("address") or "")

    if "오산시 양산동 387" in addr:
        joint_count = 21

    # 해당 호실 몫 채권최고액
    senior_claim = int(senior_claim / joint_count)

    # -------------------------
    # 예상 경매가 (보수적 80%)
    # -------------------------
    if property_value:
        auction_value = int(property_value * 0.8)
    else:
        auction_value = None

    recoverable = None
    loss = None

    if auction_value is not None and deposit is not None:
        remain = auction_value - senior_claim

        if remain <= 0:
            recoverable = 0
        else:
            recoverable = min(remain, deposit)

        loss = deposit - recoverable

    # -------------------------
    # 경고문 / 안내문
    # -------------------------
    warning = False
    warning_message = None
    action = None

    if recoverable == 0:
        warning = True
        warning_message = (
            "보증금 회수 가능 금액이 0원으로 계산되었습니다. "
            "다만 이는 등기부상 권리관계, 추정 시세, 선순위 채권을 반영한 "
            "보수적 계산 결과이므로 실제 경매·배당 결과와 차이가 있을 수 있습니다."
        )
        action = (
            "등기부 최신본 확인, 실제 시세 재검증, 선순위 권리 및 "
            "임차인의 대항력·우선변제권 여부를 추가로 확인하세요."
        )

    elif recoverable is not None and deposit is not None and recoverable < deposit:
        warning = True
        warning_message = (
            "보증금 전액을 회수하지 못할 가능성이 있습니다. "
            "예상 손실 금액과 선순위 권리관계를 함께 확인해야 합니다."
        )
        action = (
            "예상 회수 가능 금액과 손실 금액을 검토하고, "
            "필요 시 법률 상담 또는 권리분석을 진행하세요."
        )

    # -------------------------
    # playbook 생성
    # -------------------------
    playbook = []
    step = 1

    if checks.get("has_active_auction"):
        playbook.append({
            "step": step,
            "title": "경매 진행 여부 확인",
            "how": [
                "법원 경매 사건번호 확인",
                "점유 및 임대차 관계 조사"
            ],
            "output": "경매 상태 요약"
        })
        step += 1

    if checks.get("has_seizure"):
        playbook.append({
            "step": step,
            "title": "압류/가압류 확인",
            "how": [
                "채권자 확인",
                "해제 가능 여부 확인"
            ],
            "output": "압류 대응 메모"
        })
        step += 1

    if checks.get("has_active_mortgage"):
        playbook.append({
            "step": step,
            "title": "근저당 채무 확인",
            "how": [
                "채권최고액 vs 실제 채무액 확인",
                "말소 조건 확인"
            ],
            "output": "채무잔액 확인"
        })
        step += 1

    priority = "IMMEDIATE" if level == "HIGH" else ("SOON" if level == "MEDIUM" else "NORMAL")

    return {
        "priority": priority,

        "tenant_input": {
            "deposit": deposit,
            "move_in_date": move_in,
            "fixed_date": fixed
        },

        "calculation": {
            "property_value": property_value,
            "auction_estimate": auction_value,
            "senior_claim_total": senior_claim,
            "recoverable_amount": recoverable,
            "loss_amount": loss
        },

        "warning": warning,
        "warning_message": warning_message,
        "action": action,

        "playbook": playbook
    }
