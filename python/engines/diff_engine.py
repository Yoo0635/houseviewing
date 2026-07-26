import json


def entry_key(section: str, e: dict):
    rank = e.get("rank")
    purpose = e.get("purpose")
    r = e.get("receipt") or {}
    d = r.get("date")
    n = r.get("number")
    amt = e.get("max_claim_amount")
    owners = e.get("owners") or []
    owners_sig = tuple(sorted((o.get("name"), o.get("share")) for o in owners))
    return (section, rank, purpose, d, n, amt, owners_sig)


def diff_snapshots(baseline: dict, current: dict):
    if not baseline:
        return {
            "baseline_present": False,
            "baseline_viewed_at": None,
            "summary": {
                "added_count": 0,
                "removed_count": 0,
                "modified_count": 0
            },
            "changes": {
                "gabu": {"added": [], "removed": [], "modified": []},
                "eulgu": {"added": [], "removed": [], "modified": []}
            }
        }

    def index_by(section_name: str, arr: list):
        idx = {}
        for e in arr or []:
            k = entry_key(section_name, e)
            idx[k] = e
        return idx

    out = {
        "baseline_present": True,
        "baseline_viewed_at": baseline.get("viewed_at"),
        "summary": {
            "added_count": 0,
            "removed_count": 0,
            "modified_count": 0
        },
        "changes": {
            "gabu": {"added": [], "removed": [], "modified": []},
            "eulgu": {"added": [], "removed": [], "modified": []}
        }
    }

    for sec in ("gabu", "eulgu"):
        b_arr = baseline.get(sec) or []
        c_arr = current.get(sec) or []

        b_idx = index_by(sec, b_arr)
        c_idx = index_by(sec, c_arr)

        b_keys = set(b_idx.keys())
        c_keys = set(c_idx.keys())

        added = [c_idx[k] for k in sorted(c_keys - b_keys)]
        removed = [b_idx[k] for k in sorted(b_keys - c_keys)]

        modified = []

        def rp_key(e):
            return (e.get("rank"), e.get("purpose"))

        b_rp = {}
        for e in b_arr:
            b_rp.setdefault(rp_key(e), []).append(e)

        c_rp = {}
        for e in c_arr:
            c_rp.setdefault(rp_key(e), []).append(e)

        for k in set(b_rp.keys()) & set(c_rp.keys()):
            be = b_rp[k][0]
            ce = c_rp[k][0]
            if json.dumps(be, ensure_ascii=False, sort_keys=True) != json.dumps(ce, ensure_ascii=False, sort_keys=True):
                modified.append({
                    "key": {"rank": k[0], "purpose": k[1]},
                    "before": be,
                    "after": ce
                })

        out["changes"][sec]["added"] = added
        out["changes"][sec]["removed"] = removed
        out["changes"][sec]["modified"] = modified

    out["summary"]["added_count"] = (
        len(out["changes"]["gabu"]["added"]) +
        len(out["changes"]["eulgu"]["added"])
    )
    out["summary"]["removed_count"] = (
        len(out["changes"]["gabu"]["removed"]) +
        len(out["changes"]["eulgu"]["removed"])
    )
    out["summary"]["modified_count"] = (
        len(out["changes"]["gabu"]["modified"]) +
        len(out["changes"]["eulgu"]["modified"])
    )

    return out