#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
MO 端课程批量生成器：向 mountDataTAMObupter/common/recruitment-courses.json 追加随机课程，
并在 docs/log/ 下导出本次生成课程的 Excel（仅含 MO 表单侧字段，不含治理/流程初始化占位字段）。
生成的课程字段内容均为英文。

依赖：pip install pandas openpyxl
"""

from __future__ import annotations

import json
import random
import re
import sys
import uuid
from datetime import datetime, timedelta, timezone
from pathlib import Path

try:
    import pandas as pd
except ImportError:
    print("请先安装依赖: pip install pandas openpyxl", file=sys.stderr)
    sys.exit(1)

SCRIPT_DIR = Path(__file__).resolve().parent
REPO_ROOT = SCRIPT_DIR.parent
COURSES_JSON = REPO_ROOT / "mountDataTAMObupter" / "common" / "recruitment-courses.json"
LOG_DIR = REPO_ROOT / "docs" / "log"

JOB_BOARD_SCHEMA = "mo-ta-job-board"
JOB_BOARD_VERSION = "2.0"
JOB_BOARD_ENTITY = "jobs"

FIXED_SKILL_POOL = [
    "Python",
    "Java",
    "TypeScript",
    "C++",
    "Software Engineering",
    "Operating Systems",
    "Machine Learning",
    "Data Structures",
    "Computer Networks",
    "Databases",
]

COURSE_NAME_PARTS = (
    ("Advanced", "Programming", "Studio"),
    ("Distributed", "Systems", "Seminar"),
    ("Software", "Engineering", "Capstone"),
    ("Introduction", "to Machine", "Learning"),
    ("Data", "Science", "Fundamentals"),
    ("Computer", "Security", "Overview"),
    ("Compiler", "Construction", "Workshop"),
    ("Computer", "Vision", "Applications"),
    ("Natural", "Language", "Processing"),
    ("Cloud", "Computing", "Architecture"),
)

SEMESTERS = ["2026-Spring", "2026-Fall", "2025-Fall"]
CAMPUSES = ["Main", "Shahe"]
RECRUITMENT_STATUSES = ["OPEN"]


def _trim(s: str | None) -> str:
    return (s or "").strip()


def normalize_teaching_weeks(weeks: list[int] | None) -> dict:
    if not weeks:
        return {"weeks": []}
    seen: set[int] = set()
    out: list[int] = []
    for w in weeks:
        try:
            wi = int(w)
        except (TypeError, ValueError):
            continue
        if 1 <= wi <= 20 and wi not in seen:
            seen.add(wi)
            out.append(wi)
    out.sort()
    return {"weeks": out}


def normalize_assessment_events(raw: list[dict] | None) -> list[dict]:
    if not raw:
        return []
    out: list[dict] = []
    for ev in raw:
        if not isinstance(ev, dict):
            continue
        name = _trim(ev.get("name"))
        if not name:
            continue
        week_values: list[int] = []
        wk = ev.get("weeks")
        if isinstance(wk, list):
            for x in wk:
                try:
                    wi = int(x)
                except (TypeError, ValueError):
                    continue
                if 1 <= wi <= 20 and wi not in week_values:
                    week_values.append(wi)
        week_values.sort()
        out.append(
            {
                "name": name,
                "weeks": week_values,
                "description": _trim(ev.get("description")),
            }
        )
    return out


def normalize_required_skills(raw: dict | None) -> dict:
    fixed: list[str] = []
    custom: list[dict] = []
    if isinstance(raw, dict):
        ft = raw.get("fixedTags")
        if isinstance(ft, list):
            for t in ft:
                t = _trim(str(t) if t is not None else "")
                if t:
                    fixed.append(t)
        elif isinstance(ft, str) and ft.strip():
            for part in ft.split(","):
                t = _trim(part)
                if t:
                    fixed.append(t)
        cs = raw.get("customSkills")
        if isinstance(cs, list):
            for row in cs:
                if not isinstance(row, dict):
                    continue
                nm = _trim(row.get("name"))
                if not nm:
                    continue
                custom.append(
                    {
                        "name": nm,
                        "description": _trim(row.get("description")),
                    }
                )
    return {"fixedTags": fixed, "customSkills": custom}


def has_required_skills(skills: dict) -> bool:
    ft = skills.get("fixedTags") or []
    cs = skills.get("customSkills") or []
    return len(ft) > 0 or len(cs) > 0


def normalize_campus(value: str | None) -> str:
    c = _trim(value)
    if c.lower() == "main":
        return "Main"
    if c.lower() == "shahe":
        return "Shahe"
    return ""


def random_course_name() -> str:
    a, b, c = random.choice(COURSE_NAME_PARTS)
    suffix = random.randint(100, 999)
    return f"{a} {b} {c} {suffix}"


def random_iso_deadline() -> str:
    days = random.randint(14, 120)
    dt = datetime.now(timezone.utc) + timedelta(days=days)
    dt = dt.replace(hour=random.randint(18, 22), minute=random.choice([0, 15, 30]), second=0, microsecond=0)
    return dt.strftime("%Y-%m-%dT%H:%M:%S.000Z")


def format_teaching_weeks_plain(teaching_weeks: dict) -> str:
    w = teaching_weeks.get("weeks") if isinstance(teaching_weeks, dict) else None
    if not w:
        return ""
    return ", ".join(str(x) for x in w)


def format_assessment_events_plain(events: list[dict]) -> str:
    if not events:
        return ""
    lines: list[str] = []
    for ev in events:
        if not isinstance(ev, dict):
            continue
        name = _trim(ev.get("name"))
        if not name:
            continue
        weeks = ev.get("weeks")
        ws = ""
        if isinstance(weeks, list) and weeks:
            ws = ", ".join(str(x) for x in weeks)
        desc = _trim(ev.get("description"))
        parts = [name]
        if ws:
            parts.append(ws)
        if desc:
            parts.append(desc)
        lines.append(" | ".join(parts))
    return "\n".join(lines)


def format_fixed_tags_plain(required_skills: dict) -> str:
    if not isinstance(required_skills, dict):
        return ""
    ft = required_skills.get("fixedTags")
    if not isinstance(ft, list) or not ft:
        return ""
    return ", ".join(_trim(str(t)) for t in ft if _trim(str(t)))


def format_custom_tags_plain(required_skills: dict) -> str:
    if not isinstance(required_skills, dict):
        return ""
    cs = required_skills.get("customSkills")
    if not isinstance(cs, list) or not cs:
        return ""
    out: list[str] = []
    for row in cs:
        if not isinstance(row, dict):
            continue
        nm = _trim(row.get("name"))
        if not nm:
            continue
        desc = _trim(row.get("description"))
        out.append(f"{nm} - {desc}" if desc else nm)
    return "; ".join(out)


def unique_course_code(existing: set[str]) -> str:
    for _ in range(200):
        code = f"AUTO-{uuid.uuid4().hex[:6].upper()}"
        if code not in existing:
            existing.add(code)
            return code
    raise RuntimeError("Failed to generate a unique course code")


def build_mo_input(existing_codes: set[str]) -> tuple[dict, dict]:
    """
    返回 (full_item 写入 JSON, mo_only 用于 Excel)。
    full_item 对齐 MoRecruitmentDao.createCourse 写入磁盘的结构。
    """
    course_name = random_course_name()
    course_code = unique_course_code(existing_codes)
    recruitment_status = random.choice(RECRUITMENT_STATUSES)
    semester = random.choice(SEMESTERS)
    application_deadline = random_iso_deadline()

    weeks = sorted(random.sample(range(1, 21), k=random.randint(2, 8)))
    teaching_weeks = normalize_teaching_weeks(weeks)

    n_events = random.randint(0, 3)
    assessment_raw = []
    for i in range(n_events):
        assessment_raw.append(
            {
                "name": f"Assessment {i + 1} - {random.choice(['Midterm', 'Final', 'Lab', 'Project'])}",
                "weeks": random.sample(range(1, 21), k=min(3, random.randint(0, 3))),
                "description": random.choice(["", "In-class", "Online submission"]),
            }
        )
    assessment_events = normalize_assessment_events(assessment_raw)

    n_fixed = random.randint(1, 4)
    fixed_tags = random.sample(FIXED_SKILL_POOL, k=n_fixed)
    custom_skills = []
    if random.random() < 0.35:
        custom_skills.append(
            {
                "name": random.choice(
                    ["Communication", "Technical writing", "Prior TA experience"]
                ),
                "description": random.choice(
                    ["", "Prior coursework in the subject is preferred"]
                ),
            }
        )
    required_skills = normalize_required_skills({"fixedTags": fixed_tags, "customSkills": custom_skills})
    if not has_required_skills(required_skills):
        required_skills["fixedTags"] = [random.choice(FIXED_SKILL_POOL)]

    course_description = (
        f"{course_name}: core concepts and hands-on work. "
        f"TAs support office hours and assignment grading. Semester: {semester}."
    )
    recruitment_brief = ""
    if random.random() < 0.5:
        recruitment_brief = (
            f"Applications are open for TA positions in {course_name}.\n"
            f"Flexible hours; details to be discussed."
        )

    workload = ""
    if random.random() < 0.6:
        workload = random.choice(
            [
                "Approx. 6 hours per week",
                "Approx. 10 hours per week including grading",
                "Lab sections plus office hours, approx. 8 hours per week",
            ]
        )

    mo_id = f"MO-GEN-{uuid.uuid4().hex[:4].upper()}"
    campus = normalize_campus(random.choice(CAMPUSES))
    ta_recruit_count = random.randint(2, 15)
    student_count = random.choice([-1] + [random.randint(40, 280) for _ in range(3)])

    now = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%S.%f")[:-3] + "Z"
    job_id = "MOJOB-" + uuid.uuid4().hex[:8].upper()

    item: dict = {
        "jobId": job_id,
        "courseCode": course_code,
        "courseName": course_name,
        "ownerMoId": mo_id,
        "ownerMoName": mo_id,
        "semester": semester,
        "status": recruitment_status,
        "recruitmentStatus": recruitment_status,
        "publishStatus": "PENDING_REVIEW",
        "visibility": "INTERNAL",
        "isArchived": False,
        "auditStatus": "PENDING",
        "auditComment": "",
        "priority": "NORMAL",
        "dataVersion": 1,
        "lastSyncedAt": "",
        "studentCount": student_count,
        "recruitedCount": 0,
        "applicationsTotal": 0,
        "applicationsPending": 0,
        "applicationsAccepted": 0,
        "applicationsRejected": 0,
        "lastApplicationAt": "",
        "lastSelectionAt": "",
        "taRecruitCount": ta_recruit_count,
        "campus": campus,
        "applicationDeadline": application_deadline,
        "requiredSkills": required_skills,
        "courseDescription": course_description,
        "createdAt": now,
        "updatedAt": now,
        "source": "tools-genMoCourses",
    }

    if teaching_weeks["weeks"]:
        item["teachingWeeks"] = teaching_weeks
    if assessment_events:
        item["assessmentEvents"] = assessment_events
    if recruitment_brief:
        item["recruitmentBrief"] = recruitment_brief
    if workload:
        item["workload"] = workload

    mo_only: dict = {
        "courseCode": course_code,
        "courseName": course_name,
        "recruitmentStatus": recruitment_status,
        "semester": semester,
        "applicationDeadline": application_deadline,
        "campus": campus,
        "studentCount": student_count,
        "taRecruitCount": ta_recruit_count,
        "teachingWeeks": format_teaching_weeks_plain(teaching_weeks),
        "assessmentEvents": format_assessment_events_plain(assessment_events),
        "fixedTags": format_fixed_tags_plain(required_skills),
        "customTags": format_custom_tags_plain(required_skills),
        "courseDescription": course_description,
        "recruitmentBrief": recruitment_brief,
        "workload": workload,
        "ownerMoId": mo_id,
        "ownerMoName": mo_id,
        "source": "tools-genMoCourses",
    }

    return item, mo_only


def load_job_board() -> dict:
    if not COURSES_JSON.exists():
        return {
            "meta": {
                "schema": JOB_BOARD_SCHEMA,
                "entity": JOB_BOARD_ENTITY,
                "version": JOB_BOARD_VERSION,
                "updatedAt": datetime.now(timezone.utc).isoformat().replace("+00:00", "Z"),
            },
            "items": [],
        }
    with COURSES_JSON.open(encoding="utf-8") as f:
        return json.load(f)


def save_job_board(root: dict, items: list) -> None:
    now = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%S.%f")[:-3] + "Z"
    root["schema"] = JOB_BOARD_SCHEMA
    root["version"] = JOB_BOARD_VERSION
    root["generatedAt"] = now
    root["count"] = len(items)
    root["items"] = items
    if "meta" in root and isinstance(root["meta"], dict):
        root["meta"]["schema"] = JOB_BOARD_SCHEMA
        root["meta"]["entity"] = JOB_BOARD_ENTITY
        root["meta"]["version"] = JOB_BOARD_VERSION
        root["meta"]["updatedAt"] = now
    COURSES_JSON.parent.mkdir(parents=True, exist_ok=True)
    with COURSES_JSON.open("w", encoding="utf-8") as f:
        json.dump(root, f, ensure_ascii=False, indent=2)
        f.write("\n")


def write_excel(rows: list[dict], path: Path) -> None:
    LOG_DIR.mkdir(parents=True, exist_ok=True)
    columns = [
        "courseCode",
        "courseName",
        "recruitmentStatus",
        "semester",
        "applicationDeadline",
        "campus",
        "studentCount",
        "taRecruitCount",
        "teachingWeeks",
        "assessmentEvents",
        "fixedTags",
        "customTags",
        "courseDescription",
        "recruitmentBrief",
        "workload",
        "ownerMoId",
        "ownerMoName",
        "source",
    ]
    df = pd.DataFrame(rows, columns=columns)
    from openpyxl.utils import get_column_letter

    with pd.ExcelWriter(path, engine="openpyxl") as writer:
        df.to_excel(writer, index=False, sheet_name="mo_courses")
        ws = writer.sheets["mo_courses"]
        for i, col in enumerate(df.columns, start=1):
            letter = get_column_letter(i)
            maxlen = max(len(str(col)), int(df[col].astype(str).map(len).max())) + 2
            ws.column_dimensions[letter].width = min(max(maxlen, 10), 50)


def main() -> None:
    if len(sys.argv) > 1 and re.fullmatch(r"\d+", sys.argv[1].strip()):
        raw = sys.argv[1].strip()
    else:
        raw = input("请输入要生成的课程数量: ").strip()
    if not re.fullmatch(r"\d+", raw):
        print("请输入非负整数。", file=sys.stderr)
        sys.exit(1)
    n = int(raw)
    if n <= 0:
        print("数量须为正整数。", file=sys.stderr)
        sys.exit(1)
    if n > 500:
        print("单次不建议超过 500，请改小后重试。", file=sys.stderr)
        sys.exit(1)

    root = load_job_board()
    items = root.get("items")
    if not isinstance(items, list):
        items = []
    existing_codes = {
        _trim(str(it.get("courseCode", ""))) for it in items if isinstance(it, dict)
    }
    existing_codes.discard("")

    mo_rows: list[dict] = []
    new_items: list[dict] = []
    for _ in range(n):
        item, mo_only = build_mo_input(existing_codes)
        new_items.append(item)
        mo_rows.append(mo_only)

    items.extend(new_items)
    save_job_board(root, items)

    stamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    xlsx_path = LOG_DIR / f"mo_courses_generated_{stamp}.xlsx"
    write_excel(mo_rows, xlsx_path)

    print(f"已追加 {n} 条课程到: {COURSES_JSON}")
    print(f"MO 字段导出: {xlsx_path}")


if __name__ == "__main__":
    main()
