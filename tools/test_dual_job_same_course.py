#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
同课号双岗位（jobId-first）外部自动化测试工具。

测试目标：
1) MO 创建两个相同 courseCode 的岗位（不同 jobId）
2) 同一 TA 对两个岗位分别投递（应允许）
3) MO 按 jobId 拉取 applicants（不串岗）
4) MO 按 jobId 决策（只影响对应岗位）
5) MO shortlist 以 jobId + applicationId 生效

默认依赖本地已启动的 Web 服务，且 mountData 中存在可用的 MO/TA 账号。
"""

from __future__ import annotations

import argparse
import json
import sys
import tempfile
import time
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

requests = None


def trim(v: Any) -> str:
    return "" if v is None else str(v).strip()


def http_ok(status: int) -> bool:
    """POST 创建岗位等接口可能返回 201 Created。"""
    return 200 <= status < 300


def load_json(path: Path) -> dict[str, Any]:
    with path.open("r", encoding="utf-8") as f:
        return json.load(f)


class DualJobTester:
    def __init__(
        self,
        base_url: str,
        data_root: Path,
        mo_id: str | None,
        ta_id: str | None,
        timeout_sec: int,
        marker_prefix: str,
    ) -> None:
        global requests
        if requests is None:
            try:
                import requests as _requests  # type: ignore
            except Exception as exc:  # pragma: no cover
                raise RuntimeError(
                    "缺少 requests 依赖，请先执行: pip install requests\n"
                    f"原始错误: {exc}"
                ) from exc
            requests = _requests
        self.base_url = base_url.rstrip("/")
        self.data_root = data_root
        self.mo_id = trim(mo_id)
        self.ta_id = trim(ta_id)
        self.timeout = timeout_sec
        self.session = requests.Session()
        self.marker_prefix = trim(marker_prefix) or "EXTDUAL"
        self.prefix = f"{self.marker_prefix}{int(time.time())}"
        self.course_code = f"EBT{int(time.time()) % 10000:04d}"
        self.created_job_ids: list[str] = []
        self.application_ids: dict[str, str] = {}

    def api(self, path: str) -> str:
        p = path if path.startswith("/") else "/" + path
        return self.base_url + p

    def resolve_accounts(self) -> None:
        if self.mo_id and self.ta_id:
            return
        mos_path = self.data_root / "mo" / "mos.json"
        tas_path = self.data_root / "ta" / "tas.json"
        if not mos_path.is_file() or not tas_path.is_file():
            raise RuntimeError(
                f"无法自动解析账号，请确认存在: {mos_path} 与 {tas_path}，"
                "或通过 --mo-id/--ta-id 显式传入。"
            )
        mos = load_json(mos_path).get("items", [])
        tas = load_json(tas_path).get("items", [])
        if not self.mo_id:
            self.mo_id = trim((mos[0] if mos else {}).get("id"))
        if not self.ta_id:
            self.ta_id = trim((tas[0] if tas else {}).get("id"))
        if not self.mo_id or not self.ta_id:
            raise RuntimeError("无法从数据文件中解析 moId 或 taId。")

    def create_job(self, label: str) -> str:
        body = {
            "moId": self.mo_id,
            "courseCode": self.course_code,
            "courseName": f"{self.prefix}-{label}",
            "semester": "2026-Spring",
            "recruitmentStatus": "OPEN",
            "requiredSkills": {
                "fixedTags": ["Python"],
                "customSkills": [],
            },
            "courseDescription": f"{self.prefix} description {label}",
            "recruitmentBrief": f"{self.prefix} brief {label}",
            "campus": "Main",
            "taRecruitCount": 2,
        }
        r = self.session.post(
            self.api("/api/mo/jobs"),
            json=body,
            timeout=self.timeout,
        )
        payload = self._safe_json(r)
        if not http_ok(r.status_code) or payload.get("success") is False:
            raise RuntimeError(f"创建岗位失败({label}): status={r.status_code}, payload={payload}")
        item = payload.get("item") or {}
        jid = trim(item.get("jobId"))
        if not jid:
            raise RuntimeError(f"创建岗位成功但无 jobId({label}): payload={payload}")
        self.created_job_ids.append(jid)
        return jid

    def submit_application(self, job_id: str) -> str:
        with tempfile.NamedTemporaryFile(prefix=f"{self.prefix}_{job_id}_", suffix=".pdf", delete=False) as tf:
            tf.write(b"%PDF-1.4\n1 0 obj<</Type/Catalog>>endobj\ntrailer<<>>\n%%EOF\n")
            temp_pdf = Path(tf.name)
        try:
            with temp_pdf.open("rb") as f:
                files = {"resumeFile": (temp_pdf.name, f, "application/pdf")}
                data = {
                    "taId": self.ta_id,
                    "jobId": job_id,
                    "courseCode": self.course_code,
                }
                r = self.session.post(
                    self.api("/api/ta/applications"),
                    data=data,
                    files=files,
                    timeout=self.timeout,
                )
            payload = self._safe_json(r)
            if r.status_code >= 400 or payload.get("success") is False:
                raise RuntimeError(f"投递失败(jobId={job_id}): status={r.status_code}, payload={payload}")
            data_obj = payload.get("data") or {}
            app_id = trim(data_obj.get("applicationId"))
            if not app_id:
                raise RuntimeError(f"投递成功但未返回 applicationId(jobId={job_id}): payload={payload}")
            self.application_ids[job_id] = app_id
            return app_id
        finally:
            try:
                temp_pdf.unlink(missing_ok=True)
            except Exception:
                pass

    def get_applicants_for_job(self, job_id: str) -> list[dict[str, Any]]:
        r = self.session.get(
            self.api("/api/mo/applicants"),
            params={"moId": self.mo_id, "jobId": job_id},
            timeout=self.timeout,
        )
        payload = self._safe_json(r)
        if not http_ok(r.status_code) or payload.get("success") is False:
            raise RuntimeError(f"查询 applicants 失败(jobId={job_id}): status={r.status_code}, payload={payload}")
        return payload.get("items") or []

    def decide_selected(self, job_id: str) -> dict[str, Any]:
        r = self.session.post(
            self.api("/api/mo/applications/select"),
            json={
                "jobId": job_id,
                "courseCode": self.course_code,
                "taId": self.ta_id,
                "moId": self.mo_id,
                "decision": "selected",
                "comment": f"{self.prefix} auto test select",
            },
            timeout=self.timeout,
        )
        payload = self._safe_json(r)
        if not http_ok(r.status_code) or payload.get("success") is False:
            raise RuntimeError(f"决策失败(jobId={job_id}): status={r.status_code}, payload={payload}")
        return payload

    def shortlist_add(self, job_id: str, application_id: str) -> None:
        r = self.session.post(
            self.api("/api/mo/applicants/shortlist"),
            json={
                "moId": self.mo_id,
                "jobId": job_id,
                "applicationId": application_id,
                "taId": self.ta_id,
                "name": self.ta_id,
            },
            timeout=self.timeout,
        )
        payload = self._safe_json(r)
        if not http_ok(r.status_code) or payload.get("success") is False:
            raise RuntimeError(f"加入 shortlist 失败(jobId={job_id}, app={application_id}): status={r.status_code}, payload={payload}")

    def shortlist_list(self) -> list[dict[str, Any]]:
        r = self.session.get(
            self.api("/api/mo/applicants/shortlist"),
            params={"moId": self.mo_id},
            timeout=self.timeout,
        )
        payload = self._safe_json(r)
        if not http_ok(r.status_code) or payload.get("success") is False:
            raise RuntimeError(f"查询 shortlist 失败: status={r.status_code}, payload={payload}")
        return payload.get("items") or []

    def shortlist_remove(self, application_id: str) -> None:
        r = self.session.delete(
            self.api("/api/mo/applicants/shortlist"),
            params={"moId": self.mo_id, "applicationId": application_id},
            timeout=self.timeout,
        )
        payload = self._safe_json(r)
        if not http_ok(r.status_code) or payload.get("success") is False:
            raise RuntimeError(f"移除 shortlist 失败(app={application_id}): status={r.status_code}, payload={payload}")

    @staticmethod
    def _safe_json(resp: requests.Response) -> dict[str, Any]:
        try:
            data = resp.json()
        except Exception:
            return {"_raw": resp.text}
        return data if isinstance(data, dict) else {"_raw": data}

    def run(self) -> None:
        self.resolve_accounts()
        print(f"[INFO] baseUrl={self.base_url}")
        print(f"[INFO] moId={self.mo_id}, taId={self.ta_id}, courseCode={self.course_code}")

        print("\n[STEP 1] 创建同课号双岗位 ...")
        job_a = self.create_job("A")
        job_b = self.create_job("B")
        if job_a == job_b:
            raise RuntimeError(f"两个岗位 jobId 不应相同: {job_a}")
        print(f"[PASS] created jobA={job_a}, jobB={job_b}")

        print("\n[STEP 2] 同一 TA 分别投递两个岗位 ...")
        app_a = self.submit_application(job_a)
        app_b = self.submit_application(job_b)
        if app_a == app_b:
            raise RuntimeError(f"两个申请 applicationId 不应相同: {app_a}")
        print(f"[PASS] appA={app_a}, appB={app_b}")

        print("\n[STEP 3] 按 jobId 拉取 applicants，检查不串岗 ...")
        list_a = self.get_applicants_for_job(job_a)
        list_b = self.get_applicants_for_job(job_b)
        ids_a = {trim(x.get("applicationId")) for x in list_a}
        ids_b = {trim(x.get("applicationId")) for x in list_b}
        if app_a not in ids_a:
            raise RuntimeError(f"A 岗未查到 appA: jobA={job_a}, appA={app_a}")
        if app_b not in ids_b:
            raise RuntimeError(f"B 岗未查到 appB: jobB={job_b}, appB={app_b}")
        if app_b in ids_a or app_a in ids_b:
            raise RuntimeError(f"出现串岗: A列表={ids_a}, B列表={ids_b}")
        print(f"[PASS] A列表={len(ids_a)}条, B列表={len(ids_b)}条")

        print("\n[STEP 4] 对 A 岗执行 selected，验证 B 岗不受影响 ...")
        self.decide_selected(job_a)
        list_a2 = self.get_applicants_for_job(job_a)
        list_b2 = self.get_applicants_for_job(job_b)
        row_a = next((x for x in list_a2 if trim(x.get("applicationId")) == app_a), {})
        row_b = next((x for x in list_b2 if trim(x.get("applicationId")) == app_b), {})
        status_a = trim(row_a.get("status"))
        status_b = trim(row_b.get("status"))
        if status_a != "已录用":
            raise RuntimeError(f"A 岗决策后状态异常，期望 已录用，实际 {status_a!r}")
        if status_b == "已录用":
            raise RuntimeError(f"B 岗被错误影响，状态不应变为 已录用")
        print(f"[PASS] statusA={status_a}, statusB={status_b or '(empty)'}")

        print("\n[STEP 5] shortlist 按 jobId + applicationId 校验 ...")
        self.shortlist_add(job_a, app_a)
        self.shortlist_add(job_b, app_b)
        rows = self.shortlist_list()
        row_map = {trim(x.get("applicationId")): trim(x.get("jobId")) for x in rows}
        if row_map.get(app_a) != job_a or row_map.get(app_b) != job_b:
            raise RuntimeError(f"shortlist jobId 对应关系异常: {row_map}")
        self.shortlist_remove(app_a)
        rows_after = self.shortlist_list()
        remain = {trim(x.get("applicationId")) for x in rows_after}
        if app_a in remain:
            raise RuntimeError("删除 appA 后仍存在于 shortlist")
        if app_b not in remain:
            raise RuntimeError("删除 appA 时误删了 appB")
        print("[PASS] shortlist add/remove 行为正确")

        print("\n✅ 全部校验通过：同课号双岗位场景可按 jobId 精确隔离。")
        print(
            "\n[INFO] 本次标识："
            f" prefix={self.prefix}, courseCode={self.course_code},"
            f" jobIds={self.created_job_ids}, appIds={self.application_ids}"
        )


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="自动化验证同课号双岗位（jobId-first）场景。",
    )
    parser.add_argument(
        "--mode",
        choices=["run", "cleanup"],
        default="run",
        help="run: 执行自动测试；cleanup: 清理该工具产生的数据。",
    )
    parser.add_argument(
        "--base-url",
        default="http://localhost:8788/PureRecruitment_TAMO_bupter_war_exploded/",
        help="服务基础地址（默认本地 Tomcat 常见上下文）。",
    )
    parser.add_argument(
        "--data-root",
        type=Path,
        default=Path(__file__).resolve().parent.parent / "mountDataTAMObupter",
        help="数据根目录，用于自动解析 moId/taId（可选）。",
    )
    parser.add_argument("--mo-id", default="", help="显式指定 moId（可选）。")
    parser.add_argument("--ta-id", default="", help="显式指定 taId（可选）。")
    parser.add_argument("--timeout", type=int, default=20, help="HTTP 超时秒数。")
    parser.add_argument(
        "--marker-prefix",
        default="EXTDUAL",
        help="测试标识前缀；run 模式会写入 courseName/recruitmentBrief，cleanup 按此前缀识别并清理。",
    )
    return parser.parse_args()


def now_iso() -> str:
    return datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")


def touch_meta_if_present(root: dict[str, Any]) -> None:
    meta = root.get("meta")
    if isinstance(meta, dict):
        meta["updatedAt"] = now_iso()


def load_root(path: Path) -> dict[str, Any]:
    if not path.is_file():
        return {}
    return load_json(path)


def save_root(path: Path, root: dict[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="\n") as f:
        json.dump(root, f, ensure_ascii=False, indent=2)
        f.write("\n")


def cleanup_generated_data(data_root: Path, marker_prefix: str) -> None:
    prefix = trim(marker_prefix) or "EXTDUAL"
    upper_prefix = prefix.upper()
    common_courses = data_root / "common" / "recruitment-courses.json"
    ta_apps = data_root / "ta" / "applications.json"
    ta_status = data_root / "ta" / "application-status.json"
    ta_events = data_root / "ta" / "application-events.json"
    mo_shortlist = data_root / "mo" / "mo-applicant-shortlist.json"
    mo_comments = data_root / "mo" / "mo-application-comments.json"
    mo_read_state = data_root / "mo" / "mo-application-read-state.json"

    print(f"[CLEANUP] markerPrefix={prefix}")
    if not common_courses.is_file():
        raise RuntimeError(f"缺少课程文件: {common_courses}")

    courses_root = load_root(common_courses)
    course_items = courses_root.get("items")
    if not isinstance(course_items, list):
        raise RuntimeError(f"课程文件结构异常: {common_courses}")

    keep_courses: list[dict[str, Any]] = []
    removed_job_ids: set[str] = set()
    for row in course_items:
        if not isinstance(row, dict):
            keep_courses.append(row)
            continue
        name = trim(row.get("courseName")).upper()
        brief = trim(row.get("recruitmentBrief")).upper()
        if name.startswith(upper_prefix) or upper_prefix in brief:
            jid = trim(row.get("jobId"))
            if jid:
                removed_job_ids.add(jid)
            continue
        keep_courses.append(row)

    if len(keep_courses) != len(course_items):
        courses_root["items"] = keep_courses
        touch_meta_if_present(courses_root)
        save_root(common_courses, courses_root)
    print(f"[CLEANUP] removed jobs={len(course_items) - len(keep_courses)}, jobIds={sorted(removed_job_ids)}")

    removed_app_ids: set[str] = set()
    if ta_apps.is_file():
        apps_root = load_root(ta_apps)
        items = apps_root.get("items")
        if isinstance(items, list):
            keep_items: list[dict[str, Any]] = []
            for row in items:
                if not isinstance(row, dict):
                    keep_items.append(row)
                    continue
                snap = row.get("courseSnapshot")
                snap = snap if isinstance(snap, dict) else {}
                snap_job = trim(snap.get("jobId"))
                app_id = trim(row.get("applicationId"))
                if snap_job and snap_job in removed_job_ids:
                    if app_id:
                        removed_app_ids.add(app_id)
                    continue
                keep_items.append(row)
            if len(keep_items) != len(items):
                apps_root["items"] = keep_items
                touch_meta_if_present(apps_root)
                save_root(ta_apps, apps_root)
            print(f"[CLEANUP] removed applications={len(items) - len(keep_items)}")

    if ta_status.is_file():
        root = load_root(ta_status)
        items = root.get("items")
        if isinstance(items, list):
            keep_items = []
            for row in items:
                if not isinstance(row, dict):
                    keep_items.append(row)
                    continue
                app_id = trim(row.get("applicationId"))
                jid = trim(row.get("jobId"))
                if (app_id and app_id in removed_app_ids) or (jid and jid in removed_job_ids):
                    continue
                keep_items.append(row)
            if len(keep_items) != len(items):
                root["items"] = keep_items
                touch_meta_if_present(root)
                save_root(ta_status, root)
            print(f"[CLEANUP] removed status rows={len(items) - len(keep_items)}")

    if ta_events.is_file() and removed_app_ids:
        root = load_root(ta_events)
        items = root.get("items")
        if isinstance(items, list):
            keep_items = []
            for row in items:
                if not isinstance(row, dict):
                    keep_items.append(row)
                    continue
                app_id = trim(row.get("applicationId"))
                if app_id and app_id in removed_app_ids:
                    continue
                keep_items.append(row)
            if len(keep_items) != len(items):
                root["items"] = keep_items
                touch_meta_if_present(root)
                save_root(ta_events, root)
            print(f"[CLEANUP] removed event rows={len(items) - len(keep_items)}")

    if mo_shortlist.is_file():
        root = load_root(mo_shortlist)
        items = root.get("items")
        if isinstance(items, list):
            keep_items = []
            for row in items:
                if not isinstance(row, dict):
                    keep_items.append(row)
                    continue
                app_id = trim(row.get("applicationId"))
                jid = trim(row.get("jobId"))
                if (app_id and app_id in removed_app_ids) or (jid and jid in removed_job_ids):
                    continue
                keep_items.append(row)
            if len(keep_items) != len(items):
                root["items"] = keep_items
                touch_meta_if_present(root)
                save_root(mo_shortlist, root)
            print(f"[CLEANUP] removed shortlist rows={len(items) - len(keep_items)}")

    if mo_comments.is_file() and removed_app_ids:
        root = load_root(mo_comments)
        items = root.get("items")
        if isinstance(items, list):
            keep_items = []
            for row in items:
                if not isinstance(row, dict):
                    keep_items.append(row)
                    continue
                app_id = trim(row.get("applicationId"))
                if app_id and app_id in removed_app_ids:
                    continue
                keep_items.append(row)
            if len(keep_items) != len(items):
                root["items"] = keep_items
                touch_meta_if_present(root)
                save_root(mo_comments, root)
            print(f"[CLEANUP] removed comment rows={len(items) - len(keep_items)}")

    if mo_read_state.is_file() and removed_app_ids:
        root = load_root(mo_read_state)
        items = root.get("items")
        if isinstance(items, list):
            keep_items = []
            for row in items:
                if not isinstance(row, dict):
                    keep_items.append(row)
                    continue
                app_id = trim(row.get("applicationId"))
                if app_id and app_id in removed_app_ids:
                    continue
                keep_items.append(row)
            if len(keep_items) != len(items):
                root["items"] = keep_items
                touch_meta_if_present(root)
                save_root(mo_read_state, root)
            print(f"[CLEANUP] removed read-state rows={len(items) - len(keep_items)}")

    print("✅ cleanup 完成")


def pick_mode_interactive() -> str:
    print("请选择运行模式：")
    print("  1) run     执行同课号双岗位自动测试")
    print("  2) cleanup 清理该工具历史测试数据")
    print("  3) exit    退出")
    while True:
        choice = trim(input("输入 1/2/3: "))
        if choice == "1":
            return "run"
        if choice == "2":
            return "cleanup"
        if choice == "3":
            return "exit"
        print("无效输入，请输入 1/2/3。")


def main() -> int:
    if len(sys.argv) == 1:
        mode = pick_mode_interactive()
        if mode == "exit":
            print("已退出。")
            return 0
        args = parse_args()
        args.mode = mode
    else:
        args = parse_args()

    if args.mode == "cleanup":
        try:
            cleanup_generated_data(args.data_root, args.marker_prefix)
            return 0
        except Exception as exc:
            print(f"\n❌ 清理失败: {exc}", file=sys.stderr)
            return 1

    tester = DualJobTester(
        base_url=args.base_url,
        data_root=args.data_root,
        mo_id=args.mo_id,
        ta_id=args.ta_id,
        timeout_sec=args.timeout,
        marker_prefix=args.marker_prefix,
    )
    try:
        tester.run()
        return 0
    except Exception as exc:
        print(f"\n❌ 测试失败: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())

