# MO Service 层开发说明

本文档用于指导 MO 端开发者如何使用 [`MoTaApplicationReadService`](src/main/java/com/bupt/tarecruit/mo/service/MoTaApplicationReadService.java) 直接读取 TA 侧提交的申请信息与简历文件。

---

## 1. 目标与定位

[`MoTaApplicationReadService`](src/main/java/com/bupt/tarecruit/mo/service/MoTaApplicationReadService.java) 是一个 **只读服务类**，专门提供给 MO 端开发使用，帮助你从 TA 侧新的申请上传体系中直接获取：

- TA 的课程申请记录
- TA 对应的简历元数据
- 简历文件物理路径
- 简历二进制内容
- 单条申请对应的事件流时间线
- 面向课程维度、TA 维度的聚合视图

它的设计遵循 [`apiReadme.md`](mountDataTAMObupter/ta/apiReadme.md) 中定义的原则：

- 主记录来源于 [`applications.json`](mountDataTAMObupter/ta/applications.json)
- 时间线来源于 [`application-events.json`](mountDataTAMObupter/ta/application-events.json)
- 简历文件通过主记录中的 `resume.relativePath` 做映射
- **禁止通过扫描文件夹来反向推断业务关系**

---

## 2. 类所在位置

服务类位置：

- [`MoTaApplicationReadService.java`](src/main/java/com/bupt/tarecruit/mo/service/MoTaApplicationReadService.java)

包名：

- [`com.bupt.tarecruit.mo.service`](src/main/java/com/bupt/tarecruit/mo/service/MoTaApplicationReadService.java:1)

因此在 MO 端代码中可直接这样使用：

```java
import com.bupt.tarecruit.mo.service.MoTaApplicationReadService;
```

---

## 3. 使用前提

调用该服务前，默认系统中已经存在并维护以下文件：

- [`applications.json`](mountDataTAMObupter/ta/applications.json)
- [`application-events.json`](mountDataTAMObupter/ta/application-events.json)
- [`tas.json`](mountDataTAMObupter/ta/tas.json)
- [`profiles.json`](mountDataTAMObupter/ta/profiles.json)
- [`resume`](mountDataTAMObupter/ta/resume)

并且 TA 简历上传链路已经在 [`TaAccountDao.java`](src/main/java/com/bupt/tarecruit/ta/dao/TaAccountDao.java) 中按新架构完成落盘。

---

## 4. 最基础的调用方式

### 4.1 创建服务实例

```java
MoTaApplicationReadService service = new MoTaApplicationReadService();
```

该类当前是无状态只读服务，不依赖 Spring 容器，可直接 `new`。

---

## 5. 常用读取方法说明

---

### 5.1 读取全部申请

方法：[`getAllApplications()`](src/main/java/com/bupt/tarecruit/mo/service/MoTaApplicationReadService.java:30)

```java
MoTaApplicationReadService service = new MoTaApplicationReadService();
List<MoTaApplicationReadService.TaApplicationRecord> applications = service.getAllApplications();
```

适用场景：
- 做 MO 侧全量同步
- 做总览列表
- 构建中间缓存

返回结果：
- `List<TaApplicationRecord>`
- 每一项代表一条 TA 对课程的申请记录

---

### 5.2 读取全部有效申请

方法：[`getActiveApplications()`](src/main/java/com/bupt/tarecruit/mo/service/MoTaApplicationReadService.java:34)

```java
List<MoTaApplicationReadService.TaApplicationRecord> activeApplications = service.getActiveApplications();
```

适用场景：
- 只查看仍在流程中的申请
- 构建“待处理申请池”

---

### 5.3 通过课程代码读取该课程下全部申请

方法：[`getApplicationsByCourseCode()`](src/main/java/com/bupt/tarecruit/mo/service/MoTaApplicationReadService.java:41)

```java
List<MoTaApplicationReadService.TaApplicationRecord> courseApplications =
        service.getApplicationsByCourseCode("EBU6304");
```

适用场景：
- 查询某门课当前有哪些 TA 投递
- 构建课程候选人列表

返回结果中，每条记录都带有：
- TA 信息
- 课程信息
- 简历元数据
- 简历物理路径
- 当前状态

---

### 5.4 通过课程代码读取该课程下全部提交简历，并返回每份简历对应的 TA

方法：[`getCourseApplicationsView()`](src/main/java/com/bupt/tarecruit/mo/service/MoTaApplicationReadService.java:50)

这是你当前后续开发中最核心的方法之一。

```java
MoTaApplicationReadService.CourseApplicationsView view = service.getCourseApplicationsView("EBU6304");
```

返回对象：[`CourseApplicationsView`](src/main/java/com/bupt/tarecruit/mo/service/MoTaApplicationReadService.java:403)

它提供：
- `courseCode`
- `courseName`
- `applications`
- `totalCount`
- `activeCount`
- `resumes`

其中 `resumes` 是 [`CourseResumeEntry`](src/main/java/com/bupt/tarecruit/mo/service/MoTaApplicationReadService.java:392) 列表，每一项都明确对应：
- `applicationId`
- `taId`
- `taName`
- `taStudentId`
- `status`
- `statusLabel`
- `submittedAt`
- `updatedAt`
- `resumeMeta`
- `resumeAbsolutePath`

示例：

```java
MoTaApplicationReadService.CourseApplicationsView view = service.getCourseApplicationsView("EBU6304");
for (MoTaApplicationReadService.CourseResumeEntry item : view.resumes()) {
    System.out.println(item.taName());
    System.out.println(item.resumeMeta().originalFileName());
    System.out.println(item.resumeAbsolutePath());
}
```

这个方法最适合用于：
- 课程维度筛选候选人
- 课程维度批量下载简历
- MO 审核端的“某门课程全部简历”展示

---

### 5.5 通过 TA ID 读取该 TA 的全部提交

方法：[`getApplicationsByTaId()`](src/main/java/com/bupt/tarecruit/mo/service/MoTaApplicationReadService.java:82)

```java
List<MoTaApplicationReadService.TaApplicationRecord> taApplications =
        service.getApplicationsByTaId("TA-10258");
```

适用场景：
- 已知 TA ID，查看 TA 全部申请历史
- 给 MO 展示“某个 TA 投递过哪些课程”

---

### 5.6 通过 TA 名字读取该 TA 全部提交

方法：[`getApplicationsByTaName()`](src/main/java/com/bupt/tarecruit/mo/service/MoTaApplicationReadService.java:91)

```java
List<MoTaApplicationReadService.TaApplicationRecord> records =
        service.getApplicationsByTaName("张三");
```

说明：
- 这是名字模糊匹配
- 会匹配 `taName` 和 `taRealName`

适用场景：
- MO 只知道学生姓名，不知道 TA ID
- 搜索模式查询

---

### 5.7 通过 TA 名字读取该 TA 全部提交，并包含该 TA 的全部简历

方法：[`getTaApplicationsViewByName()`](src/main/java/com/bupt/tarecruit/mo/service/MoTaApplicationReadService.java:102)

这是你当前后续开发中另一个核心方法。

```java
MoTaApplicationReadService.TaApplicationsView taView = service.getTaApplicationsViewByName("张三");
```

返回对象：[`TaApplicationsView`](src/main/java/com/bupt/tarecruit/mo/service/MoTaApplicationReadService.java:424)

它提供：
- `taId`
- `taName`
- `taRealName`
- `applications`
- `totalCount`
- `activeCount`
- `resumes`

其中 `resumes` 是 [`TaResumeEntry`](src/main/java/com/bupt/tarecruit/mo/service/MoTaApplicationReadService.java:413) 列表，每一项都带：
- `applicationId`
- `courseCode`
- `courseName`
- `status`
- `statusLabel`
- `submittedAt`
- `updatedAt`
- `resumeMeta`
- `resumeAbsolutePath`

示例：

```java
MoTaApplicationReadService.TaApplicationsView taView = service.getTaApplicationsViewByName("张三");
for (MoTaApplicationReadService.TaResumeEntry resume : taView.resumes()) {
    System.out.println(resume.courseCode());
    System.out.println(resume.courseName());
    System.out.println(resume.resumeAbsolutePath());
}
```

这个方法最适合用于：
- 以 TA 为中心查看全部投递记录
- 一次性拿到 TA 的全部简历
- 做 TA 画像或跨课程候选比较

---

### 5.8 通过申请 ID 读取单条申请

方法：[`getApplicationById()`](src/main/java/com/bupt/tarecruit/mo/service/MoTaApplicationReadService.java:133)

```java
MoTaApplicationReadService.TaApplicationRecord record = service.getApplicationById(applicationId);
```

适用场景：
- 精确定位某条申请
- 后续配合事件、文件下载使用

---

### 5.9 读取某条申请的事件时间线

方法：[`getApplicationEvents()`](src/main/java/com/bupt/tarecruit/mo/service/MoTaApplicationReadService.java:143)

```java
List<MoTaApplicationReadService.TaApplicationEventRecord> events =
        service.getApplicationEvents(applicationId);
```

返回对象：[`TaApplicationEventRecord`](src/main/java/com/bupt/tarecruit/mo/service/MoTaApplicationReadService.java:382)

适用场景：
- 构建 MO 端时间线视图
- 做操作轨迹审计
- 做状态变化历史展示

---

### 5.10 直接读取简历二进制内容

方法：[`readResumeBinary()`](src/main/java/com/bupt/tarecruit/mo/service/MoTaApplicationReadService.java:169)

```java
MoTaApplicationReadService.ResumeBinaryPayload payload = service.readResumeBinary(applicationId);
if (payload != null) {
    byte[] bytes = payload.content();
    String fileName = payload.originalFileName();
    String mimeType = payload.mimeType();
}
```

返回对象：[`ResumeBinaryPayload`](src/main/java/com/bupt/tarecruit/mo/service/MoTaApplicationReadService.java:434)

适用场景：
- 实现下载接口
- 实现文件转发
- 推送给 MO 侧文件处理器
- 对接后续文件扫描或解析模块

注意：
- 该方法内部会校验路径，只允许读取 [`resume`](mountDataTAMObupter/ta/resume) 根目录内的文件。
- 如果申请不存在或文件缺失，则返回 `null`。

---

### 5.11 导出统一 JSON 结果

方法：[`exportApplicationsAsJson()`](src/main/java/com/bupt/tarecruit/mo/service/MoTaApplicationReadService.java:193)

```java
JsonObject json = service.exportApplicationsAsJson();
```

适用场景：
- 做调度器中间输出
- 对接其他服务
- 快速生成 JSON 响应

---

## 6. 返回模型说明

### 6.1 主记录模型

主模型：[`TaApplicationRecord`](src/main/java/com/bupt/tarecruit/mo/service/MoTaApplicationReadService.java:353)

重点字段：
- `applicationId`
- `uniqueKey`
- `taId`
- `taName`
- `taRealName`
- `taStudentId`
- `taEmail`
- `taPhone`
- `courseCode`
- `courseName`
- `moName`
- `status`
- `statusLabel`
- `summary`
- `nextAction`
- `active`
- `submittedAt`
- `updatedAt`
- `resumeMeta`
- `resumeAbsolutePath`
- `courseSnapshot`
- `taSnapshot`
- `rawRecord`

这已经足够覆盖绝大多数 MO 端场景。

---

### 6.2 简历元数据模型

模型：[`ResumeMeta`](src/main/java/com/bupt/tarecruit/mo/service/MoTaApplicationReadService.java:343)

字段：
- `originalFileName`
- `storedFileName`
- `relativePath`
- `extension`
- `mimeType`
- `size`
- `sha256`

说明：
- `relativePath` 用于和挂载目录拼接
- `sha256` 可用于完整性校验与去重

---

## 7. 推荐使用模式

### 7.1 课程维度推荐模式

如果你在做“某门课的所有候选人和简历”：

优先使用：
- [`getCourseApplicationsView()`](src/main/java/com/bupt/tarecruit/mo/service/MoTaApplicationReadService.java:50)

不要自己先调：
- [`getApplicationsByCourseCode()`](src/main/java/com/bupt/tarecruit/mo/service/MoTaApplicationReadService.java:41)
- 然后再手动组装简历列表

因为聚合视图已经帮你整理好了。

---

### 7.2 TA 维度推荐模式

如果你在做“某个 TA 的全部申请和全部简历”：

优先使用：
- [`getTaApplicationsViewByName()`](src/main/java/com/bupt/tarecruit/mo/service/MoTaApplicationReadService.java:102)
- 或 [`getApplicationsByTaId()`](src/main/java/com/bupt/tarecruit/mo/service/MoTaApplicationReadService.java:82)

---

### 7.3 文件下载推荐模式

如果你已经拿到 `applicationId`，并想直接下载该简历：

推荐顺序：
1. 调用 [`readResumeBinary()`](src/main/java/com/bupt/tarecruit/mo/service/MoTaApplicationReadService.java:169)
2. 读取 `content()`
3. 读取 `originalFileName()` 和 `mimeType()`
4. 返回给前端或下游文件处理组件

---

## 8. 一个完整的课程简历读取示例

```java
MoTaApplicationReadService service = new MoTaApplicationReadService();
MoTaApplicationReadService.CourseApplicationsView view = service.getCourseApplicationsView("EBU6304");

System.out.println("课程: " + view.courseCode() + " / " + view.courseName());
System.out.println("总申请数: " + view.totalCount());

for (MoTaApplicationReadService.CourseResumeEntry resume : view.resumes()) {
    System.out.println("TA: " + resume.taName());
    System.out.println("学号: " + resume.taStudentId());
    System.out.println("简历: " + resume.resumeMeta().originalFileName());
    System.out.println("路径: " + resume.resumeAbsolutePath());
    System.out.println("状态: " + resume.statusLabel());
    System.out.println("-----");
}
```

---

## 9. 一个完整的 TA 全申请读取示例

```java
MoTaApplicationReadService service = new MoTaApplicationReadService();
MoTaApplicationReadService.TaApplicationsView taView = service.getTaApplicationsViewByName("张三");

System.out.println("TA: " + taView.taName() + " / " + taView.taId());
System.out.println("总投递数: " + taView.totalCount());

for (MoTaApplicationReadService.TaResumeEntry resume : taView.resumes()) {
    System.out.println("课程: " + resume.courseCode() + " - " + resume.courseName());
    System.out.println("简历路径: " + resume.resumeAbsolutePath());
    System.out.println("文件名: " + resume.resumeMeta().originalFileName());
}
```

---

## 10. 开发注意事项

### 10.1 不要反向扫描目录推断业务

错误方式：
- 遍历 [`resume`](mountDataTAMObupter/ta/resume) 文件夹
- 靠目录名猜 TA 和课程

正确方式：
- 先查 [`applications.json`](mountDataTAMObupter/ta/applications.json)
- 再使用记录中的 `resume.relativePath`
- 再由服务类解析绝对路径

---

### 10.2 以申请快照为准

如果你要表达“申请发生当时”的语义：
- 优先使用 `courseSnapshot`
- 优先使用 `taSnapshot`

如果你要表达“当前实时账户信息”：
- 再额外读取当前账户/资料

---

### 10.3 文件缺失要容错

虽然当前类已经做了安全路径控制，但不能假设磁盘文件永远存在。

因此你在上层开发中仍应处理：
- `readResumeBinary()` 返回 `null`
- `resumeAbsolutePath` 为空
- 文件已被人工删除或磁盘损坏

---

## 11. 适合后续扩展的方向

后续如果 MO 端还要扩展，这个服务类可以继续加入：

- 按状态过滤申请
- 按时间范围筛选申请
- 按 MO 名字筛选课程申请
- 输出分页 DTO
- 输出下载流封装器
- 输出面向前端接口的统一 VO

建议扩展时继续保持原则：
- 只读
- 主记录优先
- 统一从 [`MoTaApplicationReadService`](src/main/java/com/bupt/tarecruit/mo/service/MoTaApplicationReadService.java) 暴露能力

---

## 12. 最终建议

对于后续 MO 端开发，推荐优先使用以下三个入口方法：

1. 课程维度看全部简历：
   - [`getCourseApplicationsView()`](src/main/java/com/bupt/tarecruit/mo/service/MoTaApplicationReadService.java:50)
2. TA 维度看全部投递：
   - [`getTaApplicationsViewByName()`](src/main/java/com/bupt/tarecruit/mo/service/MoTaApplicationReadService.java:102)
3. 精确读取并下载简历文件：
   - [`readResumeBinary()`](src/main/java/com/bupt/tarecruit/mo/service/MoTaApplicationReadService.java:169)

这样基本已经可以覆盖后续 MO 数据供应调度、审核列表、候选人检索、简历下载等主要场景。