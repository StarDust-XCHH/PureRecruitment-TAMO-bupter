# `common.dao` · 公用数据访问

上级包说明与数据根、启动日志、JSON 分工见 **[`../README.md`](../README.md)**。

本包存放 **MO / TA 共用** 的持久化访问逻辑，避免在各自模块里重复解析路径、加锁和契约形状。

- **岗位文件**：**`RecruitmentCoursesDao`** → `DataMountPaths.moRecruitmentCourses()`（`mountDataTAMObupter/common/recruitment-courses.json`）。
- **Gson 读字段**：**`GsonJsonObjectUtils`**（`common.util`）— 完整方法表与 **`import static`** 示例见 **[`../util/README.md`](../util/README.md)**；本 DAO 内部经静态导入使用，**不再提供重复 API**。

更完整的字段与治理说明见仓库内 `docs/api/mo-job-board-api-v2.md`、`docs/database/recruitment-courses-governance-notes.md`；数据目录旁的 `recruitment-courses-dao-notes.md` 记录 Admin/TA 专属写路径尚未实现等事项。

---

## `RecruitmentCoursesDao`

`final` 类，**全部为静态成员**。对 `recruitment-courses.json` 的读写均在 **`synchronized (FILE_LOCK)`** 内串行执行。内部归一化逻辑通过 **`GsonJsonObjectUtils`** 读取字符串与整型字段（静态导入），**不再在本类重复提供**上述纯函数 API。

### 可复用 API 索引（参数与返回值）

下面按 **是否触及磁盘** 划分；**签名与源码一致**。

#### A. 契约常量

| 成员 | 含义 | 用途 |
| --- | --- | --- |
| `JOB_BOARD_SCHEMA` | 固定字符串 `mo-ta-job-board` | 标识岗位板 JSON 的 schema 名；`readJobBoard` 响应体中的 `schema` 字段与此一致。 |
| `JOB_BOARD_ENTITY` | 固定字符串 `jobs` | 磁盘根上 `meta.entity` 等契约中的实体名。 |
| `JOB_BOARD_VERSION` | 固定字符串 `2.0` | 当前岗位板契约版本；`readJobBoard` 响应体中的 `version`。 |

#### B. 岗位文件（读写在 `FILE_LOCK` 内，可能 `IOException`）

| 签名 | 参数 | 返回值 |
| --- | --- | --- |
| `JsonObject readJobBoard()` | 无 | 内存中组装的 **`JsonObject`**：含 `schema`、`version`（与 A 中常量一致）、`generatedAt`（来自文件 `meta.updatedAt`）、`count`（归一化后的条数）、`items`（每条均经 `normalizeJobItem`）。**不写盘**。 |
| `void appendPublishedJob(JsonObject item)` | **`item`**：要追加的一条岗位，通常为业务层已拼好的完整字段；会写入磁盘 `items` 数组末尾。 | 无；成功则文件已持久化并更新 `meta` 与信封字段。 |
| `JsonObject findNormalizedJobByCourseCode(String courseCode)` | **`courseCode`**：课程编码，与磁盘行归一化后的 `courseCode` **忽略大小写**比较。 | 匹配到的 **单条** `normalizeJobItem` 结果；找不到则 **`null`**。 |
| `JsonObject findNormalizedJobByJobId(String jobId)` | **`jobId`**：岗位 ID；先 `trim`，若为空串则 **不进行查找**。 | 匹配到的归一化 **`JsonObject`**；找不到或入参为空则 **`null`**。比较 **忽略大小写**。 |
| `JsonArray findNormalizedJobsByOwnerMoId(String ownerMoId)` | **`ownerMoId`**：发布方 MO 标识；先 `trim`，若为空串则 **直接返回空数组**，不读盘遍历。 | 所有 `ownerMoId`（归一化后字段）与入参 **忽略大小写**相等的岗位，每项均为 `normalizeJobItem` 结果；可能为空数组。 |
| `JsonArray findNormalizedJobsByGovernance(String publishStatus, String auditStatus, String visibility)` | 三个筛选条件，均先 **`trim`**。某一参数为 **空串** 表示 **不参与筛选**。若 **三个都为空串**，则返回 **全部** 归一化岗位（与 `readJobBoard().getAsJsonArray("items")` 等价）。非空参数与磁盘归一化字段 **`publishStatus` / `auditStatus` / `visibility`** 做 **AND** 匹配，**忽略大小写**。 | 满足条件的 **`JsonArray`**，元素均为 `JsonObject`；无匹配则为空数组。 |
| `boolean existsJobId(String jobId)` | **`jobId`**：待检测的 ID；先 `trim`，空串则 **`false`**。 | **`true`** 当且仅当磁盘 **`items`** 中某条 **原始** `JsonObject` 的 `jobId` 字段（**不**经 `normalizeJobItem`，避免缺省补 ID）与入参 **忽略大小写**相等；否则 **`false`**。 |

#### C. 领域归一化 / 校验（无文件 IO）

| 签名 | 参数 | 返回值 |
| --- | --- | --- |
| `JsonObject normalizeTeachingWeeks(JsonObject source)` | **`source`**：可选为 `null`；若非 null，读取其 **`weeks`** 数组，元素解析为整数周次，合法范围为 **1–20**，去重后升序。 | 新的 **`JsonObject`**，仅含键 **`weeks`**，值为 **`JsonArray`**（可能为空）。 |
| `JsonArray normalizeAssessmentEvents(JsonElement source)` | **`source`**：应为考核事件数组的根；若为 `null`、JSON null、或非数组，则按无事件处理。数组内每个元素应为对象，需有非空 **`name`**；周次来自 **`weeks`** 或旧字段 **`week`**（单周），规则同上周范围与去重排序。 | **`JsonArray`**，元素为归一化后的事件对象（含 `name`、`weeks`、`description` 等）；无有效事件时为空数组。 |
| `JsonObject normalizeRequiredSkills(JsonElement source)` | **`source`**：可为 `null`；若为对象，则读取 **`fixedTags`**（字符串列表）与 **`customSkills`**（对象数组，每项需非空 **`name`**）。 | **`JsonObject`**，含 **`fixedTags`**、**`customSkills`** 两个数组（结构稳定，便于落盘与校验）。 |
| `boolean hasRequiredSkills(JsonObject skills)` | **`skills`**：归一化后的技能对象（可为 `null`）。 | **`true`** 当 **`fixedTags`** 与 **`customSkills`** 中 **至少一个数组长度大于 0**；若二者皆缺、为空数组或 `skills == null`，则为 **`false`**。 |
| `String normalizeCampus(String value)` | **`value`**：用户或磁盘中的校区原始字符串，可为 `null`。 | 若为 **`Main`** 或 **`Shahe`**（**忽略大小写**），返回规范写法 **`Main`** 或 **`Shahe`**；否则返回 **`""`**（表示无效或未设置）。 |
| `JsonObject normalizeJobItem(JsonObject source)` | **`source`**：一条岗位对象（磁盘行或内存草稿）；方法内 **`deepCopy`** 后再改，不修改调用方传入实例。 | 与 v2 契约一致的 **`JsonObject`**（字段补全、别名兼容、治理默认值等），供列表与详情统一展示。 |

---

### 调用关系（业务 DAO）

- **MO**（`MoRecruitmentDao`）：`getPendingCourses` → `readJobBoard()`；`createCourse` → `appendPublishedJob`；`decideApplication` → `findNormalizedJobByCourseCode`；发布入参解析 → **`RecruitmentCoursesDao` 的 C 类方法** + **`GsonJsonObjectUtils`**。
- **TA**（`TaAccountDao`）：岗位大厅 → `readJobBoard()`；申请状态等 JSON 读字段 → **`GsonJsonObjectUtils`**。

Admin 对治理字段的专用写接口、TA 对岗位流程统计字段的更新 **尚未在本包实现**（见数据目录下 `recruitment-courses-dao-notes.md`）。

### 并发

所有触及 `recruitment-courses.json` 的入口均在 **`synchronized (FILE_LOCK)`** 内执行；业务侧 **`MoRecruitmentDao` / `TaAccountDao` 实例锁** 与文件锁独立——若同一线程先拿业务 DAO 锁再进本类，仍由 `FILE_LOCK` 保证文件级互斥。
