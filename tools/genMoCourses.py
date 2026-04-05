#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
MO 端课程工具：在 **单一 Excel 工作簿**（默认 docs/log/mo_courses.xlsx）中维护课程行，
与 mountDataTAMObupter/common/recruitment-courses.json 双向同步。

子命令（可省略，省略时在终端交互选择）：
  export   — JSON → Excel（导出当前全部岗位到表内，便于编辑）
  import   — Excel → JSON（**以表中行为准**：新增行=新课程，删行=从 JSON 删除，改行=更新）
  generate — 按 mos.json（ownerMoId=id，ownerMoName=name）随机生成 EBT#### 课程；profiles 文件仅须存在；**重写同一 Excel** 为当前全量

招聘状态随机（仅 generate）：**70% OPEN**、**30% CLOSED**。

路径均可使用相对于仓库根目录的相对路径。

依赖：pip install pandas openpyxl
"""

from __future__ import annotations

import argparse
import copy
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

DEFAULT_COURSES_JSON = "mountDataTAMObupter/common/recruitment-courses.json"
DEFAULT_MOS_JSON = "mountDataTAMObupter/mo/mos.json"
DEFAULT_PROFILES_JSON = "mountDataTAMObupter/mo/profiles.json"
DEFAULT_EXCEL_REL = "docs/log/mo_courses.xlsx"
LOG_DIR = REPO_ROOT / "docs" / "log"
DEFAULT_EXCEL = LOG_DIR / "mo_courses.xlsx"

JOB_BOARD_SCHEMA = "mo-ta-job-board"
JOB_BOARD_VERSION = "2.0"
JOB_BOARD_ENTITY = "jobs"

EBT_CODE_PATTERN = re.compile(r"^EBT(\d{4})$", re.IGNORECASE)

EXCEL_COLUMNS = [
    "jobId",
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

SHEET_NAME = "mo_courses"

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


def _trim(s: str | None) -> str:
    return (s or "").strip()


def resolve_repo_path(relative: str) -> Path:
    p = Path(relative.strip())
    if p.is_absolute():
        return p
    return (REPO_ROOT / p).resolve()


def resolve_excel_path(arg: str | None) -> Path:
    if not arg or not _trim(arg):
        return DEFAULT_EXCEL
    return resolve_repo_path(arg)


def pick_recruitment_status() -> str:
    return random.choices(["OPEN", "CLOSED"], weights=[7, 3], k=1)[0]


def slugify_course_code(code: str) -> str:
    """与 Java MoRecruitmentDao.sanitizeCode + normalizeSlug 类似：仅字母数字，连字符连接，小写。"""
    s = re.sub(r"[^A-Za-z0-9]+", "-", _trim(code))
    s = re.sub(r"(^-|-$)", "", s)
    return s.lower()


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


def _cell_str(v) -> str:
    if v is None:
        return ""
    if isinstance(v, float) and pd.isna(v):
        return ""
    return _trim(str(v))


def parse_int_cell(v, default: int = -1) -> int:
    s = _cell_str(v)
    if not s:
        return default
    try:
        return int(float(s))
    except ValueError:
        return default


def parse_teaching_weeks_plain(s: str) -> dict:
    s = _trim(s)
    if not s:
        return {"weeks": []}
    weeks: list[int] = []
    for part in s.split(","):
        part = _trim(part)
        if not part:
            continue
        try:
            w = int(float(part))
            if 1 <= w <= 20:
                weeks.append(w)
        except ValueError:
            continue
    return normalize_teaching_weeks(weeks)


def parse_assessment_events_plain(s: str) -> list[dict]:
    s = _trim(s)
    if not s:
        return []
    raw: list[dict] = []
    for line in s.splitlines():
        line = _trim(line)
        if not line:
            continue
        parts = [p.strip() for p in line.split(" | ", 2)]
        name = parts[0] if parts else ""
        ws_str = parts[1] if len(parts) > 1 else ""
        desc = parts[2] if len(parts) > 2 else ""
        week_list: list[int] = []
        for p in ws_str.split(","):
            p = _trim(p)
            if not p:
                continue
            try:
                wi = int(float(p))
                if 1 <= wi <= 20 and wi not in week_list:
                    week_list.append(wi)
            except ValueError:
                continue
        week_list.sort()
        if name:
            raw.append({"name": name, "weeks": week_list, "description": desc})
    return normalize_assessment_events(raw)


def parse_fixed_tags_plain(s: str) -> list[str]:
    out: list[str] = []
    for part in _trim(s).split(","):
        t = _trim(part)
        if t:
            out.append(t)
    return out


def parse_custom_tags_plain(s: str) -> list[dict]:
    out: list[dict] = []
    for piece in _trim(s).split(";"):
        piece = _trim(piece)
        if not piece:
            continue
        if " - " in piece:
            nm, _, rest = piece.partition(" - ")
            nm, rest = _trim(nm), _trim(rest)
            out.append({"name": nm, "description": rest})
        else:
            out.append({"name": piece, "description": ""})
    return out


def _job_id_key(job_id: object) -> str:
    return _trim(str(job_id)).upper()


def new_unique_mo_job_id(occupied_upper: set[str]) -> str:
    """Return MOJOB-xxxxxxxx not in occupied_upper (case-insensitive); register the key in the set."""
    for _ in range(64):
        jid = "MOJOB-" + uuid.uuid4().hex[:8].upper()
        k = _job_id_key(jid)
        if k not in occupied_upper:
            occupied_upper.add(k)
            return jid
    raise RuntimeError("无法生成唯一的 jobId")


def new_job_item_shell(now: str) -> dict:
    return {
        "publishStatus": "PENDING_REVIEW",
        "visibility": "INTERNAL",
        "isArchived": False,
        "auditStatus": "PENDING",
        "auditComment": "",
        "priority": "NORMAL",
        "dataVersion": 1,
        "lastSyncedAt": "",
        "recruitedCount": 0,
        "applicationsTotal": 0,
        "applicationsPending": 0,
        "applicationsAccepted": 0,
        "applicationsRejected": 0,
        "lastApplicationAt": "",
        "lastSelectionAt": "",
        "createdAt": now,
        "updatedAt": now,
        "source": "tools-genMoCourses-excel",
    }


def json_item_to_excel_row(it: dict) -> dict:
    tw = it.get("teachingWeeks") if isinstance(it.get("teachingWeeks"), dict) else {}
    ae = it.get("assessmentEvents") if isinstance(it.get("assessmentEvents"), list) else []
    rs = it.get("requiredSkills") if isinstance(it.get("requiredSkills"), dict) else {}
    st = _trim(str(it.get("recruitmentStatus", it.get("status", "OPEN"))))
    return {
        "jobId": _trim(str(it.get("jobId", ""))),
        "courseCode": _trim(str(it.get("courseCode", ""))),
        "courseName": _trim(str(it.get("courseName", ""))),
        "recruitmentStatus": st or "OPEN",
        "semester": _trim(str(it.get("semester", ""))),
        "applicationDeadline": _trim(str(it.get("applicationDeadline", ""))),
        "campus": _trim(str(it.get("campus", ""))),
        "studentCount": it.get("studentCount", -1),
        "taRecruitCount": it.get("taRecruitCount", 0),
        "teachingWeeks": format_teaching_weeks_plain(tw),
        "assessmentEvents": format_assessment_events_plain(ae),
        "fixedTags": format_fixed_tags_plain(rs),
        "customTags": format_custom_tags_plain(rs),
        "courseDescription": _trim(str(it.get("courseDescription", ""))),
        "recruitmentBrief": _trim(str(it.get("recruitmentBrief", ""))),
        "workload": _trim(str(it.get("workload", ""))),
        "ownerMoId": _trim(str(it.get("ownerMoId", ""))),
        "ownerMoName": _trim(str(it.get("ownerMoName", ""))),
        "source": _trim(str(it.get("source", "excel-import"))),
    }


def excel_row_to_item(
    row: dict,
    existing: dict | None,
    taken_job_ids_upper: set[str] | None = None,
) -> dict:
    """由 Excel 行构造/更新 JSON item；existing 为同 courseCode 的旧记录（可保留计数等）。

    若提供 taken_job_ids_upper（大写 jobId），新增课程时生成的 jobId 不与其中任一重复；
    用户填写的新增 jobId 若与集合冲突则报错。更新行时调用方应先暂移本条旧 jobId 再校验。
    """
    cc = _cell_str(row.get("courseCode"))
    if not cc:
        raise ValueError("courseCode 不能为空")
    cname = _cell_str(row.get("courseName"))
    if not cname:
        raise ValueError(f"课程 {cc}: courseName 不能为空")

    now = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%S.%f")[:-3] + "Z"
    status = _cell_str(row.get("recruitmentStatus", "OPEN")).upper()
    if status not in ("OPEN", "CLOSED"):
        status = "OPEN"

    campus = normalize_campus(_cell_str(row.get("campus")))
    if not campus:
        campus = "Main"

    teaching_weeks = parse_teaching_weeks_plain(_cell_str(row.get("teachingWeeks")))
    assessment_events = parse_assessment_events_plain(_cell_str(row.get("assessmentEvents")))
    fixed = parse_fixed_tags_plain(_cell_str(row.get("fixedTags")))
    custom = parse_custom_tags_plain(_cell_str(row.get("customTags")))
    required_skills = normalize_required_skills({"fixedTags": fixed, "customSkills": custom})
    if not has_required_skills(required_skills):
        required_skills["fixedTags"] = [random.choice(FIXED_SKILL_POOL)]

    owner_mo_id = _cell_str(row.get("ownerMoId"))
    owner_mo_name = _cell_str(row.get("ownerMoName"))
    if not owner_mo_id:
        raise ValueError(f"课程 {cc}: ownerMoId 不能为空")
    if not owner_mo_name:
        owner_mo_name = owner_mo_id

    job_id = _cell_str(row.get("jobId"))
    if existing:
        base = copy.deepcopy(existing)
        if not job_id:
            job_id = _trim(str(base.get("jobId", "")))
        if not job_id:
            if taken_job_ids_upper is not None:
                job_id = new_unique_mo_job_id(taken_job_ids_upper)
            else:
                job_id = "MOJOB-" + uuid.uuid4().hex[:8].upper()
        elif taken_job_ids_upper is not None:
            k = _job_id_key(job_id)
            if k in taken_job_ids_upper:
                raise ValueError(f"课程 {cc}: jobId 与已有岗位重复: {job_id}")
        created = base.get("createdAt") or now
    else:
        base = new_job_item_shell(now)
        if not job_id:
            if taken_job_ids_upper is not None:
                job_id = new_unique_mo_job_id(taken_job_ids_upper)
            else:
                job_id = "MOJOB-" + uuid.uuid4().hex[:8].upper()
        else:
            if taken_job_ids_upper is not None:
                k = _job_id_key(job_id)
                if k in taken_job_ids_upper:
                    raise ValueError(f"课程 {cc}: jobId 与已有岗位重复: {job_id}")
        created = now

    sc = parse_int_cell(row.get("studentCount"), -1)
    trc = parse_int_cell(row.get("taRecruitCount"), 2)
    if trc < 0:
        trc = 2

    item: dict = {
        **base,
        "jobId": job_id,
        "courseCode": cc,
        "courseName": cname,
        "ownerMoId": owner_mo_id,
        "ownerMoName": owner_mo_name,
        "semester": _cell_str(row.get("semester")) or "2026-Spring",
        "status": status,
        "recruitmentStatus": status,
        "studentCount": sc,
        "taRecruitCount": trc,
        "campus": campus,
        "applicationDeadline": _cell_str(row.get("applicationDeadline")) or random_iso_deadline(),
        "requiredSkills": required_skills,
        "courseDescription": _cell_str(row.get("courseDescription")) or f"{cname} TA recruitment.",
        "recruitmentBrief": _cell_str(row.get("recruitmentBrief")),
        "workload": _cell_str(row.get("workload")),
        "createdAt": created,
        "updatedAt": now,
        "source": _cell_str(row.get("source")) or "excel-import",
    }

    if teaching_weeks["weeks"]:
        item["teachingWeeks"] = teaching_weeks
    else:
        item.pop("teachingWeeks", None)

    if assessment_events:
        item["assessmentEvents"] = assessment_events
    else:
        item.pop("assessmentEvents", None)

    if not item.get("recruitmentBrief"):
        item.pop("recruitmentBrief", None)
    if not item.get("workload"):
        item.pop("workload", None)

    item["jobSlug"] = slugify_course_code(cc)
    return item


def load_mo_accounts(mos_path: Path, profiles_path: Path) -> list[tuple[str, str]]:
    if not mos_path.is_file():
        raise FileNotFoundError(f"找不到 MO 账号文件: {mos_path}")
    if not profiles_path.is_file():
        raise FileNotFoundError(f"找不到 MO 资料文件: {profiles_path}")

    with mos_path.open(encoding="utf-8") as f:
        mos_root = json.load(f)

    mos_items = mos_root.get("items")
    if not isinstance(mos_items, list):
        raise ValueError("mos.json 缺少 items 数组")

    out: list[tuple[str, str]] = []
    for acc in mos_items:
        if not isinstance(acc, dict):
            continue
        mo_id = _trim(acc.get("id"))
        if not mo_id:
            continue
        name = _trim(acc.get("name"))
        display = name or mo_id
        out.append((mo_id, display))

    if not out:
        raise ValueError("mos.json 中未找到任何带 id 的 MO 账号")
    return out


def max_ebt_numeric_suffix(items: list) -> int:
    m = 0
    for it in items:
        if not isinstance(it, dict):
            continue
        cc = _trim(str(it.get("courseCode", "")))
        match = EBT_CODE_PATTERN.fullmatch(cc)
        if match:
            m = max(m, int(match.group(1)))
    return m


def build_mo_input(
    ebt_index: int,
    owner_mo_id: str,
    owner_mo_name: str,
    occupied_job_ids_upper: set[str],
) -> tuple[dict, dict]:
    course_name = random_course_name()
    course_code = f"EBT{ebt_index:04d}"
    recruitment_status = pick_recruitment_status()
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

    campus = normalize_campus(random.choice(CAMPUSES))
    ta_recruit_count = random.randint(2, 15)
    student_count = random.choice([-1] + [random.randint(40, 280) for _ in range(3)])

    now = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%S.%f")[:-3] + "Z"
    job_id = new_unique_mo_job_id(occupied_job_ids_upper)

    item: dict = {
        **new_job_item_shell(now),
        "jobId": job_id,
        "courseCode": course_code,
        "courseName": course_name,
        "ownerMoId": owner_mo_id,
        "ownerMoName": owner_mo_name or owner_mo_id,
        "semester": semester,
        "status": recruitment_status,
        "recruitmentStatus": recruitment_status,
        "studentCount": student_count,
        "taRecruitCount": ta_recruit_count,
        "campus": campus,
        "applicationDeadline": application_deadline,
        "requiredSkills": required_skills,
        "courseDescription": course_description,
        "source": "tools-genMoCourses",
        "jobSlug": slugify_course_code(course_code),
    }

    if teaching_weeks["weeks"]:
        item["teachingWeeks"] = teaching_weeks
    if assessment_events:
        item["assessmentEvents"] = assessment_events
    if recruitment_brief:
        item["recruitmentBrief"] = recruitment_brief
    if workload:
        item["workload"] = workload

    mo_only: dict = json_item_to_excel_row(item)
    return item, mo_only


def load_job_board(courses_path: Path) -> dict:
    if not courses_path.exists():
        return {
            "meta": {
                "schema": JOB_BOARD_SCHEMA,
                "entity": JOB_BOARD_ENTITY,
                "version": JOB_BOARD_VERSION,
                "updatedAt": datetime.now(timezone.utc).isoformat().replace("+00:00", "Z"),
            },
            "items": [],
        }
    with courses_path.open(encoding="utf-8") as f:
        return json.load(f)


def save_job_board(root: dict, items: list, courses_path: Path) -> None:
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
    courses_path.parent.mkdir(parents=True, exist_ok=True)
    with courses_path.open("w", encoding="utf-8") as f:
        json.dump(root, f, ensure_ascii=False, indent=2)
        f.write("\n")


def write_excel(rows: list[dict], path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    df = pd.DataFrame(rows, columns=EXCEL_COLUMNS)
    from openpyxl.utils import get_column_letter

    with pd.ExcelWriter(path, engine="openpyxl") as writer:
        df.to_excel(writer, index=False, sheet_name=SHEET_NAME)
        ws = writer.sheets[SHEET_NAME]
        for i, col in enumerate(df.columns, start=1):
            letter = get_column_letter(i)
            collen = 0 if df.empty else int(df[col].astype(str).map(len).max())
            maxlen = max(len(str(col)), collen) + 2
            ws.column_dimensions[letter].width = min(max(maxlen, 10), 50)


def read_excel_as_item_rows(path: Path) -> list[dict]:
    if not path.is_file():
        raise FileNotFoundError(f"找不到 Excel: {path}")
    df = pd.read_excel(path, sheet_name=SHEET_NAME, dtype=object)
    df = df.fillna("")
    rows: list[dict] = []
    for _, series in df.iterrows():
        row = {k: series.get(k, "") for k in EXCEL_COLUMNS}
        if all(_cell_str(row.get(c)) == "" for c in EXCEL_COLUMNS):
            continue
        rows.append(row)
    return rows


def cmd_export(courses_path: Path, excel_path: Path) -> None:
    root = load_job_board(courses_path)
    items = root.get("items")
    if not isinstance(items, list):
        items = []
    rows = [json_item_to_excel_row(it) for it in items if isinstance(it, dict)]
    write_excel(rows, excel_path)
    print(f"已导出 {len(rows)} 条到: {excel_path}")


def cmd_import(courses_path: Path, excel_path: Path, allow_empty: bool) -> None:
    raw_rows = read_excel_as_item_rows(excel_path)
    root = load_job_board(courses_path)
    old_items = root.get("items")
    if not isinstance(old_items, list):
        old_items = []
    by_code: dict[str, dict] = {}
    for it in old_items:
        if isinstance(it, dict):
            k = _trim(str(it.get("courseCode", ""))).upper()
            if k:
                by_code[k] = it

    new_items: list[dict] = []
    seen: set[str] = set()
    errors: list[str] = []
    taken_job_ids_upper: set[str] = set()
    for it in old_items:
        if isinstance(it, dict):
            jk = _job_id_key(it.get("jobId", ""))
            if jk:
                taken_job_ids_upper.add(jk)

    for i, row in enumerate(raw_rows, start=1):
        cc = _cell_str(row.get("courseCode"))
        if not cc:
            continue
        ku = cc.upper()
        if ku in seen:
            errors.append(f"第 {i} 行: 重复的 courseCode {cc}")
            continue
        seen.add(ku)
        existing = by_code.get(ku)
        prev_job_key = _job_id_key(existing.get("jobId", "")) if existing else ""
        if prev_job_key:
            taken_job_ids_upper.discard(prev_job_key)
        try:
            item = excel_row_to_item(row, existing, taken_job_ids_upper)
        except ValueError as e:
            if prev_job_key:
                taken_job_ids_upper.add(prev_job_key)
            errors.append(f"第 {i} 行: {e}")
            continue
        jk = _job_id_key(item.get("jobId", ""))
        if jk:
            taken_job_ids_upper.add(jk)
        new_items.append(item)

    if errors:
        for e in errors:
            print(e, file=sys.stderr)
        print("导入因错误已中止。", file=sys.stderr)
        sys.exit(1)

    if not new_items and not allow_empty:
        print("Excel 中无有效课程行；若需清空 JSON，请使用 --allow-empty", file=sys.stderr)
        sys.exit(1)

    old_codes = {_trim(str(it.get("courseCode", ""))).upper() for it in old_items if isinstance(it, dict)}
    new_codes = {_trim(str(it.get("courseCode", ""))).upper() for it in new_items}
    deleted = len(old_codes - new_codes)

    save_job_board(root, new_items, courses_path)
    print(f"已从 Excel 写回 {len(new_items)} 条到: {courses_path}")
    if deleted:
        print(f"已移除 JSON 中不在表内的课程: {deleted} 条")


def cmd_generate(
    n: int,
    append_mode: bool,
    courses_path: Path,
    mos_path: Path,
    profiles_path: Path,
    excel_path: Path,
) -> None:
    mo_list = load_mo_accounts(mos_path, profiles_path)
    root = load_job_board(courses_path)
    items = root.get("items")
    if not isinstance(items, list):
        items = []

    kept: list = []
    if append_mode:
        kept = [it for it in items if isinstance(it, dict)]
        start_idx = max_ebt_numeric_suffix(kept) + 1
    else:
        kept = []
        start_idx = 1

    if start_idx + n - 1 > 9999:
        print("EBT 编号将超过 9999。", file=sys.stderr)
        sys.exit(1)

    occupied_job_ids_upper: set[str] = set()
    for it in kept:
        if isinstance(it, dict):
            jk = _job_id_key(it.get("jobId", ""))
            if jk:
                occupied_job_ids_upper.add(jk)

    new_items: list[dict] = []
    for i in range(n):
        ebt_num = start_idx + i
        mo_id, mo_name = mo_list[i % len(mo_list)]
        item, _ = build_mo_input(ebt_num, mo_id, mo_name, occupied_job_ids_upper)
        new_items.append(item)

    final_items = kept + new_items
    save_job_board(root, final_items, courses_path)

    excel_rows = [json_item_to_excel_row(it) for it in final_items]
    write_excel(excel_rows, excel_path)

    mode = "追加" if append_mode else "覆盖"
    print(f"{mode}：已写入 JSON {len(final_items)} 条，并同步 Excel: {excel_path}")


def normalize_argv(argv: list[str]) -> list[str]:
    if not argv:
        return argv
    if argv[0] in ("export", "import", "generate", "-h", "--help"):
        return argv
    if re.fullmatch(r"\d+", argv[0]):
        return ["generate"] + argv
    return argv


def prompt_command() -> str:
    print("未指定子命令，请选择操作：")
    print("  1 — export   JSON → Excel")
    print("  2 — import   Excel → JSON（以表中行为准）")
    print("  3 — generate 随机生成 EBT 课程并同步 Excel")
    while True:
        try:
            s = input("请输入 1 / 2 / 3: ").strip()
        except EOFError:
            print("", file=sys.stderr)
            print("非交互环境请显式指定子命令，例如: export / import / generate", file=sys.stderr)
            sys.exit(1)
        if s == "1":
            return "export"
        if s == "2":
            return "import"
        if s == "3":
            return "generate"
        print("请输入 1、2 或 3。", file=sys.stderr)


def build_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(
        description="MO 课程：单一 Excel 与 recruitment-courses.json 同步 / 随机生成"
    )
    p.add_argument(
        "--excel",
        default=DEFAULT_EXCEL_REL,
        help=f"工作簿路径（相对仓库根，默认 {DEFAULT_EXCEL_REL}）",
    )
    p.add_argument(
        "--courses-json",
        default=DEFAULT_COURSES_JSON,
        help=f"岗位 JSON（默认 {DEFAULT_COURSES_JSON}）",
    )
    p.add_argument(
        "--allow-empty",
        action="store_true",
        help="仅 import：允许 Excel 无有效行时清空 JSON 中的 items",
    )
    p.add_argument(
        "--mode",
        choices=["1", "2"],
        default=None,
        help="仅 generate：1=追加，2=覆盖；省略则交互询问",
    )
    p.add_argument("--mos-json", default=DEFAULT_MOS_JSON, help="仅 generate")
    p.add_argument("--profiles-json", default=DEFAULT_PROFILES_JSON, help="仅 generate")

    sub = p.add_subparsers(
        dest="command",
        required=False,
        metavar="{export,import,generate}",
        help="子命令（可省略，省略时交互选择）",
    )
    sub.add_parser("export", help="JSON → Excel")
    sub.add_parser("import", help="Excel → JSON（表中有则留/改，表中无则删）")
    p_gen = sub.add_parser("generate", help="随机生成 EBT 课程并写 JSON + 同步全表到 Excel")
    p_gen.add_argument(
        "n",
        nargs="?",
        type=int,
        default=None,
        help="生成条数（省略则交互输入）",
    )

    return p


def main(argv: list[str] | None = None) -> None:
    argv = normalize_argv(argv if argv is not None else sys.argv[1:])
    parser = build_parser()
    args = parser.parse_args(argv)

    command = args.command
    if not command:
        command = prompt_command()

    excel_path = resolve_excel_path(args.excel)
    courses_path = resolve_repo_path(args.courses_json)

    if command == "export":
        cmd_export(courses_path, excel_path)
        return

    if command == "import":
        cmd_import(courses_path, excel_path, args.allow_empty)
        return

    if command == "generate":
        n = getattr(args, "n", None)
        if n is None:
            raw = input("请输入要生成的课程数量: ").strip()
            if not re.fullmatch(r"\d+", raw):
                print("请输入非负整数。", file=sys.stderr)
                sys.exit(1)
            n = int(raw)

        if args.mode is None:
            while True:
                m = input("请选择写入方式：1-追加记录，2-覆盖（清空后仅写入本次）: ").strip()
                if m == "1":
                    append_mode = True
                    break
                if m == "2":
                    append_mode = False
                    break
                print("请输入 1 或 2。", file=sys.stderr)
        else:
            append_mode = args.mode == "1"

        if n <= 0 or n > 500 or n > 9999:
            print("数量须为 1–500（且 EBT 不超过 9999）。", file=sys.stderr)
            sys.exit(1)

        mos_path = resolve_repo_path(args.mos_json)
        profiles_path = resolve_repo_path(args.profiles_json)
        try:
            cmd_generate(n, append_mode, courses_path, mos_path, profiles_path, excel_path)
        except (OSError, ValueError, json.JSONDecodeError) as e:
            print(f"失败: {e}", file=sys.stderr)
            sys.exit(1)


if __name__ == "__main__":
    main()
