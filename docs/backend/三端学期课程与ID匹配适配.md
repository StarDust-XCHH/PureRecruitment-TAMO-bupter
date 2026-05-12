# 三端同课号多岗位（jobId）匹配说明（严格模式）

本文记录当前分支在 `courseCode` 与 `jobId` 并存时的真实行为，目标是支持“相同 `courseCode` 的多个岗位同时存在”且不串单。

## 1. 结论

- 全链路主键采用 `jobId`（HTTP、前端筛选、短名单、MO 决策、读写权限校验）。
- `courseCode` 仅作为展示字段与交叉校验字段，不再作为岗位定位主键。
- 在同一 `courseCode` 多岗位场景下，MO/TA 的申请匹配按 `jobId` 区分岗位实例。

## 2. HTTP 契约（当前）

### MO

- `GET /api/mo/applicants?moId=...`
  - 不带 `jobId`：返回当前 MO 名下所有岗位的合并结果。
  - 带 `jobId`：只返回该岗位申请。
  - 仅带 `courseCode`（不带 `jobId`）被拒绝（400）。
- `POST /api/mo/applications/select`
  - 必须带 `jobId`，`courseCode` 可选（非空则与岗位交叉校验）。
- `POST /api/mo/applications/mark-read`
  - 通过申请快照中的 `jobId` 做归属校验（非课号）。
- `POST /api/mo/applications/comment`
  - 通过申请快照中的 `jobId` 做归属校验（非课号）。
- `GET /api/mo/applicants/detail`
  - 通过申请快照中的 `jobId` 做归属校验；详情返回顶层 `jobId`。

### TA

- `GET /api/ta/jobs` 返回岗位 `jobId`。
- `POST /api/ta/applications` 必须带 `jobId` + `courseCode`；后端按 `jobId` 解析岗位并校验 `courseCode` 一致。

## 3. 关键匹配规则

- `MoTaApplicationStatusMatcher`
  - 仅接受：
    - `applicationId` 一致，或
    - `jobId` 一致（且 `taId` 一致）。
  - 不再有仅按 `courseCode` 的回退匹配。

- `MoTaApplicationReadService#getApplicationsForCourseScopedToJob(courseCode, jobId)`
  - 当 `jobId` 非空时，TA 申请快照中 `courseSnapshot.jobId` 必须存在且相等。

- `MoTaApplicationsMutationDao#findApplicationByTaAndCourse`
  - 当传入 `jobId` 时：快照 `jobId` 必须一致，`uniqueKey` 必须命中带 `jobId` 的 canonical 形式。

- 申请统计写回（`MoRecruitmentDao` + `RecruitmentCoursesDao`）
  - 新增按 `jobId` 写回的 `syncPublishedJobApplicationStatsByJobId(...)`。
  - `getApplicantsForJob`、决策后同步、全量重算优先按 `jobId` 同步统计，避免同课号多岗位时把统计写到错误岗位行。

- `TaAccountDao` 重复投递判重
  - 仅按 canonical `uniqueKey` 判重，不再使用历史 `TA::COURSECODE` 回退。

## 4. 短名单（Shortlist）

- 接口改为 `jobId` 语义：
  - `POST /api/mo/applicants/shortlist`：`moId` + `jobId` + `applicationId`（可选 `taId`/`name`）。
  - `DELETE /api/mo/applicants/shortlist`：`moId` + `applicationId`。
- 存储 `mo-applicant-shortlist.json` 行结构包含 `jobId` + `courseCode`，去重按 `(moId, applicationId)`。
- 前端 `MoShortlistStore` 按 `jobId` 分桶（`jobIdsWithEntries` / `listForJob`）。

## 5. 多岗位同课号能力确认

在“同一 `courseCode`、不同 `jobId`”下：

- TA 可分别投递（申请唯一键含 `jobId`）。
- MO 人选筛选、决策、已读、评论、短名单均以 `jobId` / `applicationId` 定位。
- 不会因为课号相同将 A 岗申请匹配到 B 岗状态。

## 6. 仍需注意

- 兼容层（本地脚本/旧调用）里仍有 course-first 的辅助方法（如旧签名重载），但 HTTP 主路径与前端已切换到 strict `jobId`。
- 历史无 `jobId` 的旧短名单行不会出现在按 `jobId` 分桶视图中；建议重建该数据或补齐 `jobId`。
