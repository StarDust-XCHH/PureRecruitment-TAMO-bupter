# `common/` 数据文件说明

本目录存放 MO–TA 共享及部分 MO 只读参考数据。

| 文件 | 用途 |
| --- | --- |
| `recruitment-courses.json` | 已发布招聘岗位板（MO/TA 读写，见下文字段说明） |
| `modules-catalog.json` | 官方课程编码 → 名称目录（MO 发布岗位时自动匹配课程名） |

---

## `modules-catalog.json`

课程编码与英文课程名称对照表，供 MO 端「发布岗位」表单在填写 **Module code** 时实时匹配并填入 **Opening / module name**。

**数据来源：** 从 [QMPlus](https://www.qmplus.com/) 获取的 **Diet 列表**中提取并整理为本 JSON；非运行时从 QMPlus 拉取，更新目录时需重新导出后覆盖本文件。

**读取方式：** `DataMountPaths.modulesCatalog()` → `GET /api/mo/modules-catalog`（及 `/api/common/modules-catalog`）。

### 顶层结构

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `schema` | string | 固定为 `modules-catalog`。 |
| `version` | string | 契约版本，当前为 `1.0`。 |
| `source` | string | 数据来源说明（如 QMPlus Diet 列表）。 |
| `updatedAt` | string | ISO-8601，最近一次整理/写入时间。 |
| `count` | number | 与 `modules` 长度一致。 |
| `modules` | array | 课程条目列表。 |

### `modules[]` 字段

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `code` | string | 课程编码，如 `EBU6304`（三位字母前缀 + 四位数字）。 |
| `name` | string | 课程英文名称，如 `Software Engineering`。 |

---

# `recruitment-courses.json` 字段说明

**MO–TA 共享** 招聘岗位数据；契约与接口细节以仓库内 `docs/mo-job-board-api-v2.md` 为准；以下为字段速查。

---

## 顶层结构

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `schema` | string | 固定为 `mo-ta-job-board`，标识数据契约。 |
| `version` | string | 契约版本，当前为 `2.0`。 |
| `generatedAt` | string | ISO-8601 时间，最近一次列表生成/归一化时间。 |
| `count` | number | 与 `items` 长度一致。 |
| `items` | array | 岗位对象列表，见下表。 |

部分历史文件可能还带 `meta`（如 `schema` / `entity` / `updatedAt`）；读取端以 `items` 为主，服务端会逐步归一为 v2 顶层结构。

---

## `items[]` 岗位字段

### 标识与归属

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `jobId` | string | 岗位唯一 ID（如 `MOJOB-XXXXXXXX`），发布时由服务端生成。 |
| `courseCode` | string | 课程编号。 |
| `courseName` | string | 课程/岗位名称。 |
| `ownerMoId` | string | MO 稳定标识，建议作关联键。 |
| `ownerMoName` | string | MO 展示名；可与 `ownerMoId` 相同，用于列表/详情展示。 |
| `semester` | string | 学期，如 `2026-Spring`；可空字符串。 |

### 状态（招聘 / 兼容 / 治理占位）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `recruitmentStatus` | string | 招聘业务状态（如 `OPEN`）；**语义主字段**。 |
| `status` | string | 与 `recruitmentStatus` 一致的 **兼容镜像**，供旧读取方使用。 |
| `publishStatus` | string | 平台发布/审核类状态；MO 发布时常为占位（如 `PENDING_REVIEW`）。 |
| `visibility` | string | 可见性（如 `INTERNAL`）。 |
| `isArchived` | boolean | 是否归档。 |
| `auditStatus` | string | 审核状态占位（如 `PENDING`）。 |
| `auditComment` | string | 审核备注。 |
| `priority` | string | 优先级（如 `NORMAL`）。 |
| `dataVersion` | number | 数据版本号，占位或递增策略由后续流程定义。 |
| `lastSyncedAt` | string | 同步时间占位，常为空字符串。 |

### 规模、截止与校区

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `studentCount` | number | 选课人数等；未知时常为 `-1`。 |
| `taRecruitCount` | number | 计划招聘 TA 人数。 |
| `recruitedCount` | number | 已招人数；发布时常初始化为 `0`。 |
| `campus` | string | `Main` 或 `Shahe`（归一化后）。 |
| `applicationDeadline` | string | 申请截止时间，建议 ISO-8601 带时区；可空。 |

### 申请流程占位统计（岗位级聚合）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `applicationsTotal` | number | 申请总数占位。 |
| `applicationsPending` | number | 待处理数占位。 |
| `applicationsAccepted` | number | 已接受数占位。 |
| `applicationsRejected` | number | 已拒绝数占位。 |
| `lastApplicationAt` | string | 最近申请时间占位。 |
| `lastSelectionAt` | string | 最近筛选/录用时间占位。 |

### 教学内容与技能

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `teachingWeeks` | object | `{ "weeks": [1,…,20] }`，周次在 1–20 内、去重升序。 |
| `assessmentEvents` | array | 评估事件列表，元素含 `name`、`weeks`、`description`。 |
| `requiredSkills` | object | **必填结构**，见下节。 |
| `courseDescription` | string | 课程/岗位描述。 |
| `recruitmentBrief` | string | MO 长文本招募说明，支持多段与换行。 |
| `workload` | string | 工作量说明（如答疑、阅卷支持等）。 |

### 审计字段

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `createdAt` | string | 创建时间 ISO-8601。 |
| `updatedAt` | string | 更新时间 ISO-8601。 |
| `source` | string | 数据来源标识（如 `mo-manual-v2`）。 |

---

## `requiredSkills` 结构

```json
{
  "fixedTags": ["Python", "Java"],
  "customSkills": [
    { "name": "技能名", "description": "说明" }
  ]
}
```

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `fixedTags` | string[] | 预置技能标签列表。 |
| `customSkills` | object[] | 自定义技能；每项含 `name`（必填）、`description`（可选）。 |

---

## 写入与治理说明

- MO 发布会写入业务字段，并对治理/流程类字段做 **默认值占位**；后续由 TA/Admin 流程更新计数与时间戳等，详见 `docs/recruitment-courses-governance-notes.md`。
- `GET /api/mo/jobs` 仅在响应中做 v2 归一化，**不会**因刷新列表而改写本文件，避免影响其它课程的磁盘数据。
- 服务端路径由 `DataMountPaths.moRecruitmentCourses()` 解析，默认指向本目录下的 `recruitment-courses.json`。
