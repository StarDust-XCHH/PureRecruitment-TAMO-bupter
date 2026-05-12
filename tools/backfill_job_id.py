#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
从 common/recruitment-courses.json 按 courseCode（忽略大小写，取文件中首次出现）
为以下文件补全缺失的 jobId：

  - ta/applications.json：courseSnapshot.jobId；可选将 legacy uniqueKey（TA::CC）升为 TA::CC::JOBID
  - ta/application-status.json：根级 jobId

默认 dry-run，仅打印计划变更；加 --apply 写回文件。

若岗位板存在多条相同 courseCode，会打印警告并采用首次出现的 jobId（与 Java findNormalizedJobByCourseCode 一致）。

用法（仓库根目录）：
  python tools/backfill_job_id.py
  python tools/backfill_job_id.py --apply
  python tools/backfill_job_id.py --data-root "D:/data/mountDataTAMObupter" --apply
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any


def _trim(s: Any) -> str:
    if s is None:
        return ""
    return str(s).strip()


def _upper(s: str) -> str:
    return s.strip().upper()


def load_json(path: Path) -> dict[str, Any]:
    with path.open(encoding="utf-8") as f:
        return json.load(f)


def save_json(path: Path, data: dict[str, Any]) -> None:
    with path.open("w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")


def build_course_code_to_job_id(jobs: list[dict[str, Any]]) -> tuple[dict[str, str], list[str]]:
    """courseCode upper -> jobId; warnings for duplicate courseCode."""
    m: dict[str, str] = {}
    warnings: list[str] = []
    for row in jobs:
        if not isinstance(row, dict):
            continue
        cc = _upper(_trim(row.get("courseCode")))
        jid = _trim(row.get("jobId"))
        if not cc or not jid:
            continue
        if cc in m:
            if m[cc].upper() == jid.upper():
                continue
            warnings.append(
                f"duplicate courseCode={cc!r}: keeping jobId={m[cc]!r}, also saw {jid!r}"
            )
            continue
        m[cc] = jid
    return m, warnings


def backfill_applications(data: dict[str, Any], cc_to_job: dict[str, str], fix_unique_key: bool) -> int:
    items = data.get("items")
    if not isinstance(items, list):
        return 0
    n = 0
    for item in items:
        if not isinstance(item, dict):
            continue
        ta_id = _trim(item.get("taId"))
        cc = _trim(item.get("courseCode"))
        snap = item.get("courseSnapshot")
        if isinstance(snap, dict):
            if not cc:
                cc = _trim(snap.get("courseCode"))
        else:
            snap = {}
            item["courseSnapshot"] = snap
        if not cc:
            print(f"  [skip application] missing courseCode: applicationId={_trim(item.get('applicationId'))}", file=sys.stderr)
            continue
        key = _upper(cc)
        jid = cc_to_job.get(key)
        if not jid:
            print(f"  [skip application] no job for courseCode={cc!r}: applicationId={_trim(item.get('applicationId'))}", file=sys.stderr)
            continue
        cur = _trim(snap.get("jobId")) if isinstance(snap, dict) else ""
        if cur.upper() != jid.upper():
            snap["jobId"] = jid
            n += 1
        if fix_unique_key and ta_id:
            canonical = f"{_upper(ta_id)}::{_upper(cc)}::{_upper(jid)}"
            uk_raw = _trim(item.get("uniqueKey"))
            parts = uk_raw.split("::") if uk_raw else []
            t, c, j = _upper(ta_id), _upper(cc), _upper(jid)
            need_uk = False
            if not parts:
                need_uk = True
            elif len(parts) == 2 and parts[0].upper() == t and parts[1].upper() == c:
                need_uk = True
            elif len(parts) == 3 and (
                parts[0].upper() != t or parts[1].upper() != c or parts[2].upper() != j
            ):
                need_uk = True
            if need_uk and _upper(uk_raw) != _upper(canonical):
                item["uniqueKey"] = canonical
                n += 1
    return n


def backfill_application_status(data: dict[str, Any], cc_to_job: dict[str, str]) -> int:
    items = data.get("items")
    if not isinstance(items, list):
        return 0
    n = 0
    for item in items:
        if not isinstance(item, dict):
            continue
        cc = _trim(item.get("courseCode"))
        if not cc:
            print(f"  [skip status] missing courseCode: taId={_trim(item.get('taId'))}", file=sys.stderr)
            continue
        jid = cc_to_job.get(_upper(cc))
        if not jid:
            print(f"  [skip status] no job for courseCode={cc!r}", file=sys.stderr)
            continue
        cur = _trim(item.get("jobId"))
        if cur.upper() != jid.upper():
            item["jobId"] = jid
            n += 1
    return n


def main() -> int:
    ap = argparse.ArgumentParser(description="Backfill jobId from recruitment-courses.json into TA JSON files.")
    ap.add_argument(
        "--data-root",
        type=Path,
        default=Path(__file__).resolve().parent.parent / "mountDataTAMObupter",
        help="Data mount root (contains ta/, common/)",
    )
    ap.add_argument("--apply", action="store_true", help="Write files (default is dry-run)")
    ap.add_argument(
        "--no-fix-unique-key",
        action="store_true",
        help="Do not rewrite applications uniqueKey to TA::CC::JOBID form",
    )
    args = ap.parse_args()
    root: Path = args.data_root
    courses_path = root / "common" / "recruitment-courses.json"
    apps_path = root / "ta" / "applications.json"
    status_path = root / "ta" / "application-status.json"

    if not courses_path.is_file():
        print(f"Missing {courses_path}", file=sys.stderr)
        return 2

    courses = load_json(courses_path)
    job_items = courses.get("items")
    if not isinstance(job_items, list):
        print("recruitment-courses.json: invalid items", file=sys.stderr)
        return 2

    cc_to_job, warns = build_course_code_to_job_id(job_items)
    for w in warns:
        print(f"WARNING: {w}", file=sys.stderr)

    total = 0
    if apps_path.is_file():
        apps = load_json(apps_path)
        c = backfill_applications(apps, cc_to_job, fix_unique_key=not args.no_fix_unique_key)
        print(f"applications.json: {c} field(s) would change")
        total += c
        if args.apply and c > 0:
            save_json(apps_path, apps)
            print(f"  wrote {apps_path}")
    else:
        print(f"(skip) not found: {apps_path}")

    if status_path.is_file():
        st = load_json(status_path)
        c = backfill_application_status(st, cc_to_job)
        print(f"application-status.json: {c} field(s) would change")
        total += c
        if args.apply and c > 0:
            save_json(status_path, st)
            print(f"  wrote {status_path}")
    else:
        print(f"(skip) not found: {status_path}")

    if not args.apply:
        print("\nDry-run only. Re-run with --apply to write changes.")
    elif total == 0:
        print("\nNo changes needed (or files missing).")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
