# TA 简历映射与 MO 数据供应调度开发说明

本文档位于 [`resume`](mountDataTAMObupter/ta/resume) 目录下，目的不是存放业务数据，而是为后续面向 MO 端的数据供应调度器开发提供“TA、课程、简历文件”三者关系的统一读取规范。

---

## 1. 总体设计目标

TA 侧简历申请体系目前采用以下三层存储：

1. [`applications.json`](mountDataTAMObupter/ta/applications.json)
   - 申请主数据
   - 一条记录代表一个 TA 对一个课程的一次有效申请
   - 是后续调度器的主查询入口

2. [`application-events.json`](mountDataTAMObupter/ta/application-events.json)
   - 申请事件流
   - 记录申请创建、状态变更、后续处理轨迹
   - 是时间线与审计信息来源

3. [`resume`](mountDataTAMObupter/ta/resume)
   - 简历文件存储目录
   - 实际保存 pdf/doc/docx 文件
   - 采用 “TA -> 课程 -> 文件” 的分层结构

对于 MO 端的“数据供应调度器”，推荐始终以 [`applications.json`](mountDataTAMObupter/ta/applications.json) 为主表，再去关联事件与简历路径，而不要反向扫描文件目录推断业务关系。

---

## 2. 目录结构说明

当前简历目录结构约定如下：

```text
mountDataTAMObupter/
└─ ta/
   ├─ applications.json
   ├─ application-events.json
   └─ resume/
      └─ {taId}/
         └─ {courseCode}/
            └─ {storedFileName}
```

含义如下：

- [`{taId}`](mountDataTAMObupter/ta/resume)
  - TA 唯一身份目录
  - 对应 TA 账号主键
- [`{courseCode}`](mountDataTAMObupter/ta/resume)
  - 课程唯一目录
  - 对应课程编码
- [`{storedFileName}`](mountDataTAMObupter/ta/resume)
  - 实际落盘文件名
  - 一般不是原始文件名，而是系统生成的安全文件名

因此，文件系统中的物理路径天然表达了：

`一个 TA -> 某一门课程 -> 对应的一份简历文件`

但正式业务读取时，仍应以 [`applications.json`](mountDataTAMObupter/ta/applications.json) 中的 `resume` 元数据为准，而不是仅依赖文件夹名称。

---

## 3. 申请唯一键规则

当前系统设计约定：

- 一条申请记录必须由：
  - 课程选择
  - TA 个人身份信息
- 共同生成唯一记录

推荐你在调度器中把以下字段视为业务唯一键：

- `taId`
- `courseCode`

通常可拼接为：

```text
applicationKey = taId + "::" + courseCode
```

如果后续需要扩展“同一 TA 对同一课程多版本投递”，则再引入版本号或时间戳；但当前这套体系默认是“同一 TA 对同一课程最多一条有效申请”。

---

## 4. [`applications.json`](mountDataTAMObupter/ta/applications.json) 的职责

该文件是 MO 侧调度器最应优先消费的主数据文件。

推荐理解为：

- 一条 item = 一条业务申请主记录
- 该记录已经包含：
  - TA 身份快照
  - 课程快照
  - 简历文件元数据
  - 当前状态
  - 时间信息

### 4.1 顶层结构

```json
{
  "schema": "ta-applications.v1",
  "version": 1,
  "items": []
}
```

### 4.2 单条申请记录建议关注字段

实际字段以代码写入结果为准，但调度器应重点读取以下几类字段：

#### A. 业务标识字段

- `applicationId`
  - 申请记录唯一 ID
  - 用于内部引用或事件关联
- `taId`
  - TA 唯一标识
- `courseCode`
  - 课程编码

#### B. TA 快照字段

一般位于 `taSnapshot` 对象内，例如：

- `taSnapshot.taId`
- `taSnapshot.name`
- `taSnapshot.realName`
- `taSnapshot.studentId`
- `taSnapshot.email`
- `taSnapshot.phone`
- `taSnapshot.department`
- `taSnapshot.skills`

说明：
- 使用快照而非运行时实时查询，是为了防止 TA 后续修改个人资料导致历史申请失真。
- MO 数据供应调度器如果面向“当次申请语义”，优先用快照；
- 如果面向“当前账户语义”，再额外调用 [`tas.json`](mountDataTAMObupter/ta/tas.json) 或 [`profiles.json`](mountDataTAMObupter/ta/profiles.json)。

#### C. 课程快照字段

一般位于 `courseSnapshot` 对象内，例如：

- `courseSnapshot.courseCode`
- `courseSnapshot.courseName`
- `courseSnapshot.moName`
- `courseSnapshot.studentCount`
- `courseSnapshot.keywordTags`
- `courseSnapshot.taWorkContents`
- `courseSnapshot.recruitmentStatus`

说明：
- 课程信息也应以申请发生时快照为准；
- 这样可以避免课程后续修改导致 MO 查看历史申请时信息不一致。

#### D. 简历元数据字段

一般位于 `resume` 对象内，例如：

- `resume.originalFileName`
- `resume.storedFileName`
- `resume.relativePath`
- `resume.extension`
- `resume.mimeType`
- `resume.size`
- `resume.sha256`

这是 MO 调度器关联简历文件的核心字段。

#### E. 状态字段

例如：

- `status`
- `statusLabel`
- `statusTone`
- `summary`
- `nextAction`
- `active`

MO 端如果只需要获取“待处理申请池”，可优先按 `status` 或 `active` 过滤。

#### F. 时间字段

例如：

- `submittedAt`
- `updatedAt`

MO 调度器如果要做增量同步，推荐优先基于：

- `updatedAt`
- 或事件流中的事件时间

---

## 5. [`application-events.json`](mountDataTAMObupter/ta/application-events.json) 的职责

该文件用于保存申请事件流，不建议作为主表使用。

推荐用途：

- 还原申请时间线
- 为 MO 端提供操作轨迹
- 支持调度器做增量更新
- 支持审计、补偿、重放逻辑

### 5.1 顶层结构

```json
{
  "schema": "ta-application-events.v1",
  "version": 1,
  "items": []
}
```

### 5.2 单条事件建议关注字段

常见字段建议包括：

- `eventId`
- `applicationId`
- `taId`
- `courseCode`
- `eventType`
- `eventTime`
- `operatorType`
- `payload`

其中：

- `applicationId`
  - 把事件绑定回主申请记录
- `taId + courseCode`
  - 可作为冗余索引键
- `eventType`
  - 例如：`APPLICATION_SUBMITTED`、`STATUS_UPDATED`
- `eventTime`
  - 可作为调度器增量拉取基准

---

## 6. 简历文件如何和 TA、课程做对应

后续开发中，最重要的问题就是：

**如何从 MO 侧稳定拿到“这个 TA 申请这门课程时上传的那份简历”？**

答案是：

### 6.1 第一原则：先查 [`applications.json`](mountDataTAMObupter/ta/applications.json)

不要直接扫描 [`resume`](mountDataTAMObupter/ta/resume) 目录来反推业务。

正确顺序：

1. 读取 [`applications.json`](mountDataTAMObupter/ta/applications.json)
2. 找到目标申请记录
3. 从该记录的 `resume.relativePath` 或其他简历元数据中拿到物理路径
4. 再读取或传输对应文件

### 6.2 典型映射路径

假设某条申请记录中：

- `taId = TA-10258`
- `courseCode = EBU6304`
- `resume.relativePath = ta/resume/TA-10258/EBU6304/TA-10258_EBU6304_resume.pdf`

那么 MO 调度器应理解为：

- TA：`TA-10258`
- 课程：`EBU6304`
- 简历物理文件：
  [`mountDataTAMObupter/ta/resume/TA-10258/EBU6304/TA-10258_EBU6304_resume.pdf`](mountDataTAMObupter/ta/resume)

### 6.3 推荐使用 `resume.relativePath`

推荐优先读取：

- `resume.relativePath`

而不是手工拼接路径。原因：

- 后续目录命名规则可能变化
- 文件名生成策略可能变化
- 只有主记录中的相对路径是最终权威映射

MO 调度器拿到 `relativePath` 后，可以：

- 拼接数据根目录得到绝对路径
- 读取文件
- 生成下载流
- 推送给 MO 端处理器

---

## 7. 推荐给 MO 调度器的读取流程

建议你后续开发时采用如下顺序。

### 7.1 全量拉取流程

1. 读取 [`applications.json`](mountDataTAMObupter/ta/applications.json)
2. 遍历 `items`
3. 对每条申请：
   - 取 `applicationId`
   - 取 `taId`
   - 取 `courseCode`
   - 取 `status`
   - 取 `resume.relativePath`
   - 取 `resume.originalFileName`
   - 取 `resume.sha256`
4. 将结果转换为 MO 所需 DTO
5. 如需时间线，再按 `applicationId` 关联 [`application-events.json`](mountDataTAMObupter/ta/application-events.json)

### 7.2 增量拉取流程

推荐两种模式：

#### 模式 A：按主记录 `updatedAt` 增量

适合场景：
- 只关心申请当前状态
- 不要求完整事件轨迹

流程：
1. 保存上次同步时间点 `lastSyncTime`
2. 读取 [`applications.json`](mountDataTAMObupter/ta/applications.json)
3. 过滤 `updatedAt > lastSyncTime` 的记录
4. 同步这些记录与其简历文件元数据

#### 模式 B：按事件流 `eventTime` 增量

适合场景：
- 需要操作轨迹
- 需要事件重放
- 需要更细粒度同步

流程：
1. 保存上次事件时间点或事件游标
2. 读取 [`application-events.json`](mountDataTAMObupter/ta/application-events.json)
3. 过滤 `eventTime > lastEventCursor`
4. 根据 `applicationId` 回查 [`applications.json`](mountDataTAMObupter/ta/applications.json)
5. 合并主记录与事件内容

---

## 8. 推荐的 MO 供应 DTO 结构

如果你要做“面向 MO 端的数据供应调度器”，推荐统一出一个中间 DTO，例如：

```json
{
  "applicationId": "...",
  "taId": "TA-10258",
  "courseCode": "EBU6304",
  "courseName": "Business Intelligence",
  "taName": "xxx",
  "studentId": "2023xxxx",
  "status": "SUBMITTED",
  "submittedAt": "2026-04-04T10:00:00Z",
  "resume": {
    "originalFileName": "resume.pdf",
    "relativePath": "ta/resume/TA-10258/EBU6304/xxx.pdf",
    "mimeType": "application/pdf",
    "size": 123456,
    "sha256": "..."
  }
}
```

这样 MO 端不必感知 TA 侧完整存储细节，只消费统一调度器输出。

---

## 9. 后续开发时的约束建议

为了保证 MO 调度器稳定，建议遵守以下约束：

### 9.1 永远不要直接把文件目录当数据库

错误方式：
- 扫描 [`resume`](mountDataTAMObupter/ta/resume) 目录并猜测 TA、课程、状态

正确方式：
- 主数据来自 [`applications.json`](mountDataTAMObupter/ta/applications.json)
- 文件目录只做二级资源读取

### 9.2 以申请主记录为准，不以当前账户信息为准

因为：
- TA 可能修改头像、邮箱、技能、个人简介
- 课程也可能修改名称、标签、MO 负责人

如果你要查看“申请发生时”的真实语义，应优先消费快照。

### 9.3 文件读取前最好校验 `sha256`

如果后续有以下需求：
- 文件完整性校验
- 分发去重
- 防止文件被篡改

则可以优先使用：
- `resume.sha256`

### 9.4 调度器应容忍文件缺失

极端情况下，可能出现：
- JSON 已写入
- 文件被人工删除
- 或磁盘异常导致文件不存在

因此调度器读取时应增加：
- 文件存在性检查
- 缺失告警日志
- 补偿或跳过机制

---

## 10. 简单伪代码示例

### 10.1 通过申请主记录找到简历文件

```text
load applications.json
for each application in items:
    taId = application.taId
    courseCode = application.courseCode
    resumeRelativePath = application.resume.relativePath
    absoluteResumePath = dataRoot + "/" + resumeRelativePath
    if file exists:
        supply to MO pipeline
```

### 10.2 用事件流补全时间线

```text
load application-events.json
group events by applicationId
load applications.json
for each application:
    timeline = groupedEvents[application.applicationId]
    build MO payload
```

---

## 11. 你后续最应该依赖的字段清单

如果目标只是为 MO 端做“稳定可用的数据供应调度”，建议优先锁定以下字段：

### 必选字段

- `applicationId`
- `taId`
- `courseCode`
- `status`
- `submittedAt`
- `updatedAt`
- `resume.relativePath`
- `resume.originalFileName`
- `resume.mimeType`
- `resume.size`
- `resume.sha256`

### 推荐字段

- `taSnapshot.realName`
- `taSnapshot.studentId`
- `taSnapshot.email`
- `taSnapshot.phone`
- `courseSnapshot.courseName`
- `courseSnapshot.moName`
- `courseSnapshot.keywordTags`

### 时间线字段

- `eventId`
- `applicationId`
- `eventType`
- `eventTime`
- `payload`

---

## 12. 最终结论

后续面向 MO 端开发数据供应调度器时，请按下面的主从关系理解整套 TA 申请体系：

- 主索引来源：[`applications.json`](mountDataTAMObupter/ta/applications.json)
- 时间线来源：[`application-events.json`](mountDataTAMObupter/ta/application-events.json)
- 文件资源来源：[`resume`](mountDataTAMObupter/ta/resume)

即：

**先找到申请记录，再找到对应 TA 和课程，再根据申请记录中的简历元数据去定位文件。**

这是当前这套架构中最稳定、最适合后续 MO 调度器复用的调用方式。
