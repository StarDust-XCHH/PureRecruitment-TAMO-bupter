# MO-TA Job Board API v2 (Draft)

## 1. Purpose and Scope / 目的与范围

This draft defines the semester-scale TA recruitment contract between MO and TA.  
本草案定义了 MO 与 TA 之间“学期级”TA 招募的数据与接口契约。

Key goals / 核心目标:
- Recruitment is semester-long (not one fixed class session).  
  招募面向整个学期，而不是单次固定课时。
- MO can define teaching weeks, assessment events, and required skills.  
  MO 可定义教学周、评估事件与技能要求。
- TA can directly read job board data from one stable API.  
  TA 端可通过稳定 API 直接读取岗位数据。
- MO can write a long-form recruitment brief text.  
  MO 可填写长文本招募说明。

Out of scope in this version / 本版本暂不覆盖:
- Strict server-side validation and rich error taxonomy.  
  严格服务端校验与完整错误码体系。
- Authentication/authorization details.  
  认证与鉴权细节。

---

## 2. Data File and Top-Level Schema / 数据文件与顶层结构

Data file / 数据文件:
- `mountDataTAMObupter/common/recruitment-courses.json`

Top-level JSON / 顶层 JSON:

```json
{
  "schema": "mo-ta-job-board",
  "version": "2.0",
  "generatedAt": "2026-04-02T12:00:00Z",
  "count": 1,
  "items": []
}
```

Field notes / 字段说明:
- `schema`: fixed string for contract identification.  
  固定字符串，用于识别契约类型。
- `version`: contract version, fixed `2.0` in this draft.  
  契约版本，当前草案固定为 `2.0`。
- `generatedAt`: ISO-8601 timestamp for last generation/update.  
  最近一次生成/更新时间（ISO-8601）。
- `count`: equals `items.length`.  
  数量字段，等于 `items.length`。
- `items`: array of MO job postings.  
  MO 发布岗位数组。

---

## 3. Job Item Schema (`items[]`) / 岗位对象结构

Each item MUST follow this structure.  
每个 `item` 必须满足以下结构：

```json
{
  "courseCode": "SE-TA-2026S",
  "courseName": "Software Engineering TA (Semester)",
  "ownerMoId": "MO-10001",
  "ownerMoName": "Dr. Zhang",
  "semester": "2026-Spring",
  "status": "OPEN",
  "recruitmentStatus": "OPEN",
  "publishStatus": "PENDING_REVIEW",
  "visibility": "INTERNAL",
  "isArchived": false,
  "auditStatus": "PENDING",
  "auditComment": "",
  "priority": "NORMAL",
  "dataVersion": 1,
  "lastSyncedAt": "",
  "studentCount": 180,
  "recruitedCount": 0,
  "applicationsTotal": 0,
  "applicationsPending": 0,
  "applicationsAccepted": 0,
  "applicationsRejected": 0,
  "lastApplicationAt": "",
  "lastSelectionAt": "",
  "taRecruitCount": 6,
  "campus": "Main",
  "applicationDeadline": "2026-04-30T23:59:59+08:00",

  "teachingWeeks": {
    "weeks": [1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20]
  },

  "assessmentEvents": [
    {
      "name": "ClassTest",
      "weeks": [8,9],
      "description": "In-class mid test support"
    },
    {
      "name": "CodingDay",
      "weeks": [12],
      "description": "On-site coding assistance"
    }
  ],

  "requiredSkills": {
    "fixedTags": ["Python", "Java", "Git"],
    "customSkills": [
      {
        "name": "Code Review",
        "description": "Can review student code and provide structured feedback"
      }
    ]
  },

  "courseDescription": "Semester-scale TA recruitment for software engineering module.",
  "recruitmentBrief": "Long-form recruitment brief written by MO. Supports multi-paragraph text and line breaks.",
  "workload": "Teaching support + Q&A + grading support",

  "createdAt": "2026-04-02T12:00:00Z",
  "updatedAt": "2026-04-02T12:00:00Z",
  "source": "mo-manual"
}
```

### 3.1 Required vs Optional (Implementation Snapshot) / 必填与选填（当前实现快照）

Required / 必填:
- `courseCode`
- `courseName`
- `recruitmentStatus` (or compatibility `status`)
- `requiredSkills`
- `courseDescription`

Recommended optional / 建议选填:
- `ownerMoId`
- `ownerMoName`
- `semester`
- `teachingWeeks`
- `assessmentEvents`
- `recruitmentBrief`
- `workload`
- `studentCount`
- `recruitedCount`
- `applicationsTotal`
- `applicationsPending`
- `applicationsAccepted`
- `applicationsRejected`
- `lastApplicationAt`
- `lastSelectionAt`
- `taRecruitCount`
- `campus`
- `applicationDeadline`
- `publishStatus`
- `visibility`
- `isArchived`
- `auditStatus`
- `auditComment`
- `priority`
- `dataVersion`
- `lastSyncedAt`

Implementation notes / 实现备注:
- `status` is currently kept as compatibility mirror of `recruitmentStatus`.  
  当前实现中 `status` 作为 `recruitmentStatus` 的兼容镜像字段保留。
- `moName` is **not** part of the contract; use `ownerMoId` and `ownerMoName` only. Legacy `moName` on disk is not written on new publishes; MO `GET` normalization removes it from the response and, when present, uses it once to backfill `ownerMoName` if missing.  
  契约中**不再**包含 `moName`，仅使用 `ownerMoId` 与 `ownerMoName`。新发布不再写入 `moName`；若磁盘上仍有历史 `moName`，`GET /api/mo/jobs` 归一化时会从响应中去掉该键，并在缺省 `ownerMoName` 时用其回填一次。
- `publishStatus` belongs to Admin governance lifecycle (Option A), but MO publish currently initializes it with default placeholder value only.  
  按方案 A，`publishStatus` 归属 Admin 治理生命周期；MO 发布当前仅做默认占位初始化。

### 3.2 Teaching Weeks Rules / 教学周规则

- Week range is `1-20`.  
  教学周范围为 `1-20`。
- `weeks` is the explicit week list used by TA-side schedule logic.  
  `weeks` 为显式周次列表，供 TA 侧排班/展示逻辑直接使用。
- `weeks` should be sorted and unique.  
  `weeks` 建议升序且去重。

### 3.3 Campus and TA Recruit Count / 校区与 TA 招聘人数

- `campus` supports `Main` or `Shahe`.  
  `campus` 支持 `Main` 或 `Shahe`。
- `taRecruitCount` means intended TA headcount for this job.  
  `taRecruitCount` 表示该岗位计划招聘 TA 人数。
- `recruitedCount` means currently recruited TA count.  
  `recruitedCount` 表示当前已招募 TA 人数。
- Current publish API initializes `recruitedCount` as `0`; it is not user-editable in MO publish form.  
  当前发布接口会将 `recruitedCount` 初始化为 `0`；MO 发布表单不可编辑该字段。
- Current MO publish API also initializes governance/process placeholder fields (`publishStatus`, `visibility`, `auditStatus`, counters, timestamps) with safe defaults for downstream TA/Admin processing.  
  当前 MO 发布接口也会初始化治理/流程占位字段（如 `publishStatus`、`visibility`、`auditStatus`、计数器、时间戳），供后续 TA/Admin 流程使用。

### 3.4 Assessment Events / 评估事件

- `assessmentEvents` allows MO-defined event names, such as:  
  `assessmentEvents` 允许 MO 自定义事件名，例如：
  - `ClassTest`
  - `CodingDay`
  - any custom event name needed by the module.
- `assessmentEvents[].weeks` supports multiple week values (`1-20`, sorted/unique recommended).  
  `assessmentEvents[].weeks` 支持多个周次（`1-20`，建议升序去重）。

### 3.5 Long Recruitment Brief / 长文本招募说明

- `recruitmentBrief` is long-form text for MO recruitment details.  
  `recruitmentBrief` 为 MO 的长文本招募说明字段。
- Supports multi-line content (`\n`) and multi-paragraph writing.  
  支持多行（`\n`）与多段文本。
- Suggested content / 建议包含内容:
  - role responsibilities
  - expected commitment
  - communication/collaboration expectations
  - grading/support expectations
  - preferred background

---

## 4. Fixed Skill Tags Dictionary / 固定技能标签字典

This contract includes a controlled fixed tag list for `requiredSkills.fixedTags`.  
本契约为 `requiredSkills.fixedTags` 提供固定可选标签列表：

```json
[
  "Python",
  "Java",
  "C/C++",
  "JavaScript",
  "TypeScript",
  "SQL",
  "Linux",
  "Git",
  "Data Structures",
  "Algorithms",
  "Machine Learning",
  "Computer Networks",
  "Operating Systems",
  "Database",
  "Software Engineering",
  "Web Development"
]
```

Draft limits / 草案建议范围:
- `fixedTags`: 1 to 8 entries per job.  
  每个岗位 1 到 8 个。
- `customSkills`: 0 to 10 entries per job.  
  每个岗位 0 到 10 个。

---

## 5. API Contract / 接口契约

## 5.1 POST `/api/mo/jobs`

Purpose / 目的:
- MO creates one semester-scale recruitment job.  
  MO 创建一个学期级岗位。

Request body / 请求体:
- JSON object matching `items[]` schema (server can auto-fill `createdAt`, `updatedAt`).  
  传入与 `items[]` 一致的 JSON 对象（`createdAt`、`updatedAt` 可由服务端补齐）。

Example request (MO publish) / 请求示例（MO 发布）:

```json
{
  "courseCode": "SE-TA-2026S",
  "courseName": "Software Engineering TA (Semester)",
  "ownerMoId": "MO-10001",
  "ownerMoName": "Dr. Zhang",
  "semester": "2026-Spring",
  "recruitmentStatus": "OPEN",
  "status": "OPEN",
  "studentCount": 180,
  "taRecruitCount": 6,
  "campus": "Main",
  "applicationDeadline": "2026-04-30T23:59:59+08:00",
  "teachingWeeks": {
    "weeks": [1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20]
  },
  "assessmentEvents": [
    { "name": "ClassTest", "weeks": [8,9], "description": "In-class mid test support" },
    { "name": "CodingDay", "weeks": [12], "description": "On-site coding assistance" }
  ],
  "requiredSkills": {
    "fixedTags": ["Python", "Java", "Git"],
    "customSkills": [
      { "name": "Code Review", "description": "Can review student code and provide structured feedback" }
    ]
  },
  "courseDescription": "Semester-scale TA recruitment for software engineering module.",
  "recruitmentBrief": "This role supports the module throughout the semester.\n\nYou will answer student questions and support marking workflows.",
  "workload": "Teaching support + Q&A + grading support"
}
```

Success response (`201`) / 成功响应:

```json
{
  "success": true,
  "message": "课程发布成功",
  "item": {
    "jobId": "MOJOB-SE-2026S-001",
    "courseCode": "SE-TA-2026S"
  }
}
```

---

## 5.2 GET `/api/ta/jobs`

Purpose / 目的:
- TA retrieves the current job board list.  
  TA 读取当前岗位列表。

Success response (`200`) / 成功响应:

```json
{
  "schema": "mo-ta-job-board",
  "version": "2.0",
  "generatedAt": "2026-04-02T12:10:00Z",
  "count": 1,
  "items": [
    {
      "jobId": "MOJOB-SE-2026S-001",
      "courseCode": "SE-TA-2026S",
      "courseName": "Software Engineering TA (Semester)",
      "ownerMoId": "MO-10001",
      "ownerMoName": "Dr. Zhang",
      "status": "OPEN",
      "recruitmentStatus": "OPEN",
      "studentCount": -1,
      "recruitedCount": 0,
      "taRecruitCount": 6,
      "campus": "Main",
      "teachingWeeks": {
        "weeks": [1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20]
      },
      "requiredSkills": {
        "fixedTags": ["Python", "Java", "Git"],
        "customSkills": [
          { "name": "Code Review", "description": "Can review student code and provide structured feedback" }
        ]
      },
      "recruitmentBrief": "This role supports the module throughout the semester.\n\nYou will answer student questions and support marking workflows."
    }
  ]
}
```

---

## 5.3 GET `/api/common/skill-tags` (recommended)

Purpose / 目的:
- Return the fixed skill tag dictionary for MO form rendering and TA filtering consistency.  
  返回固定技能标签字典，用于 MO 表单渲染与 TA 过滤一致性。

Success response (`200`) / 成功响应:

```json
{
  "schema": "skill-tags",
  "version": "1.0",
  "items": [
    "Python",
    "Java",
    "C/C++",
    "JavaScript",
    "TypeScript",
    "SQL",
    "Linux",
    "Git"
  ]
}
```

---

## 6. Required/Optional Enforcement Notes / 必填与选填约束说明

Current MO-side publishing enforces these required fields:  
当前 MO 发布侧强制以下必填字段：
- `courseName`
- `courseCode`
- `recruitmentStatus` (or compatibility `status`)
- `requiredSkills` (at least one fixed tag or custom skill)
- `courseDescription`

All other fields are optional in this revision.  
本版中其余字段均为选填。

Current MO publish implementation writes these proposal fields now:  
当前 MO 发布实现已写入以下提案字段：
- `ownerMoId`, `ownerMoName`, `semester`, `recruitmentStatus`, `applicationDeadline`
- compatibility `status` (mirrors `recruitmentStatus` for existing readers)
- placeholder/governance fields with defaults:
  - `publishStatus`, `visibility`, `isArchived`, `auditStatus`, `auditComment`, `priority`, `dataVersion`, `lastSyncedAt`
  - `applicationsTotal`, `applicationsPending`, `applicationsAccepted`, `applicationsRejected`, `lastApplicationAt`, `lastSelectionAt`

Implementation snapshot by responsibility / 按职责划分的当前实现快照:
- **MO input-driven fields**: entered by MO form or derived from MO identity/context.  
  `courseCode`, `courseName`, `recruitmentStatus`(+`status` mirror), `courseDescription`, `requiredSkills`, `teachingWeeks`, `assessmentEvents`, `studentCount`, `taRecruitCount`, `campus`, `semester`, `applicationDeadline`, `recruitmentBrief`, `workload`, `ownerMoId`, `ownerMoName`.
- **MO generated fields**: generated at publish time.  
  `jobId`, `createdAt`, `updatedAt`, `source`.
- **Placeholder-only fields (initialized, logic not implemented here)**: for downstream TA/Admin workflows.  
  `recruitedCount`, `applicationsTotal`, `applicationsPending`, `applicationsAccepted`, `applicationsRejected`, `lastApplicationAt`, `lastSelectionAt`, `publishStatus`, `visibility`, `isArchived`, `auditStatus`, `auditComment`, `priority`, `dataVersion`, `lastSyncedAt`.

Request/response boundary note / 请求与响应边界说明:
- `POST /api/mo/jobs` focuses on MO input-driven fields; placeholder/governance fields are initialized by server if omitted.  
  `POST /api/mo/jobs` 以 MO 输入字段为主；占位/治理字段如未提供由服务端初始化。
- `GET /api/mo/jobs` returns each item **normalized in memory** (v2 shape, defaults for missing governance/process fields). It **does not** write `recruitment-courses.json`, so refreshing the MO job list does not change on-disk rows for other courses.  
  `GET /api/mo/jobs` 在**内存中**对每条 `item` 做 v2 归一化（缺失的治理/流程字段在响应中补默认）；**不会**写回 `recruitment-courses.json`，避免拉列表时连带改写磁盘上其它课程。
- `GET /api/ta/jobs` reads the file and returns `items` as stored (TA backend pass-through). Legacy or partially shaped rows on disk may differ from the MO GET response until a write (e.g. `POST /api/mo/jobs`) updates that row.  
  `GET /api/ta/jobs` 按磁盘存储返回 `items`（TA 侧偏透传）。历史或未完整落盘的行可能与 MO `GET` 的归一化视图不一致，直至对该行有一次写操作（例如 MO 发布）更新文件。

---

## 7. Minimal Joint Test Checklist (MO + TA) / 最小联调检查清单

- MO posts one new semester-scale job via `POST /api/mo/jobs`.  
  MO 通过 `POST /api/mo/jobs` 发布一个学期岗位。
- TA can immediately read it from `GET /api/ta/jobs`.  
  TA 能立即通过 `GET /api/ta/jobs` 读取到该岗位。
- `teachingWeeks` values are present and valid within `1-20`.  
  `teachingWeeks` 字段存在且周次均在 `1-20`。
- `assessmentEvents` can be omitted and reading still works.  
  `assessmentEvents` 可缺省且读取不报错。
- `recruitedCount` defaults to `0` when missing in historical data.  
  历史数据缺失 `recruitedCount` 时默认归一为 `0`。
- `requiredSkills.fixedTags` and `requiredSkills.customSkills` are both returned.  
  `requiredSkills.fixedTags` 与 `requiredSkills.customSkills` 均可返回。
- `recruitmentBrief` multi-line text is preserved and readable.  
  `recruitmentBrief` 多行文本可完整保留并可读。

---

## 8. Open Items (To Finalize in Next Revision) / 待定项（下版确定）

- Validation strictness and error code matrix.  
  校验严格程度与错误码矩阵。
- Final enum sets for `recruitmentStatus`, `publishStatus`, `visibility`, `auditStatus`, `priority`.  
  `recruitmentStatus`、`publishStatus`、`visibility`、`auditStatus`、`priority` 的最终枚举集合。
- How `recruitedCount` should be updated by downstream workflow (TA/Admin operations).  
  `recruitedCount` 由下游流程（TA/Admin 操作）更新的策略。
- Final maximum length for `recruitmentBrief`.  
  `recruitmentBrief` 最终长度上限。

---

## 9. Layered Field Ownership (Proposal + Current Bridge) / 总数据库字段分层建议（提案与当前落地）

This section describes **who should own lifecycle semantics** (long-term target), and how **current MO publish** bridges the gap by **initializing placeholders once**.  
本节说明**长期语义归属**（目标态），以及**当前 MO 发布**如何通过**一次性占位初始化**衔接目标态。

### 9.1 Semantic ownership (Option A) / 语义归属（方案 A）

| Layer | Fields (semantic owner) | Meaning |
| --- | --- | --- |
| MO (business content) | `courseCode`, `courseName`, `semester`, `ownerMoId`, `ownerMoName`, `recruitmentStatus`, compatibility `status`, `courseDescription`, `recruitmentBrief`, `requiredSkills`, `teachingWeeks`, `assessmentEvents`, `taRecruitCount`, `campus`, `studentCount`, `applicationDeadline`, `workload` | MO defines what is being recruited and under what constraints. TA/Admin read these as the job definition. |
| TA / application workflow (process metrics) | `recruitedCount`, `applicationsTotal`, `applicationsPending`, `applicationsAccepted`, `applicationsRejected`, `lastApplicationAt`, `lastSelectionAt` | Updated by application/selection events, not by MO publish form. |
| Admin (platform governance) | `publishStatus`, `visibility`, `isArchived`, `auditStatus`, `auditComment`, `priority`, `dataVersion`, `lastSyncedAt` | Admin policy controls visibility, audit, archiving, and sync bookkeeping. |

### 9.2 What MO publish does today / 当前 MO 发布实际做了什么

- **MO form + request body**: user-driven fields plus identity-derived `ownerMoId` / `ownerMoName` (no separate `moName` field).  
  **MO 表单与请求体**：用户填写字段及身份派生的 `ownerMoId` / `ownerMoName`（不再使用单独的 `moName` 字段）。
- **MO server on `POST /api/mo/jobs`**: generates `jobId`, `createdAt`, `updatedAt`, sets `source`, mirrors `status` from `recruitmentStatus`, and **initializes** governance/process placeholders with defaults so the JSON is a complete “course DB” row for TA/Admin to consume later.  
  **服务端在发布时**：生成 `jobId`、时间戳、`source`，维护 `status` 与 `recruitmentStatus` 一致，并对治理/流程字段做**默认占位初始化**，使单条记录可作为后续 TA/Admin 调用的完整行数据。
- **Important**: initializing a field does **not** mean MO owns its **lifecycle**; downstream components should still treat governance/process fields per §9.1 when implementing updates.  
  **重要**：初始化不等于语义上的**生命周期归属**；后续实现仍应按 §9.1 由对应侧更新这些字段。

### 9.3 Recommended constraints / 建议约束

- **Option A (clarified)**: MO owns `recruitmentStatus` (hiring progress semantics). Admin owns **changes** to `publishStatus` / `visibility` / audit fields after placeholders exist. MO does not claim to replace Admin review—defaults are placeholders only.  
  **方案 A（澄清）**：MO 负责 `recruitmentStatus`（招聘进度语义）。占位存在后，**变更** `publishStatus`、`visibility`、审计类字段由 Admin 负责。MO 的默认值不代表完成审核。
- `recruitedCount <= taRecruitCount` when `taRecruitCount >= 0` (business rule suggestion).  
  当 `taRecruitCount >= 0` 时，建议满足 `recruitedCount <= taRecruitCount`。
- Use `ownerMoId` as stable join key; `ownerMoName` for display.  
  `ownerMoId` 作稳定关联键，`ownerMoName` 作展示。
- Store `applicationDeadline` as ISO-8601 with timezone when possible.  
  `applicationDeadline` 尽量使用带时区的 ISO-8601。
- Prefer event-driven updates for counters and timestamps once workflow code exists.  
  流程代码就绪后，计数与时间戳建议事件驱动更新。

