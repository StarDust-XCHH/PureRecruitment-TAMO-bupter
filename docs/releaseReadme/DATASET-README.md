# 测试数据集说明

本文件说明公开 release 中附带的测试数据目录 [`mountDataTAMObupter/`](../../mountDataTAMObupter/README.md) 的结构、用途与运行期行为。

本项目并不是把业务数据写在传统独立数据库服务中，而是通过统一的数据根目录来组织 JSON、附件、图片、简历和 AI 相关文件。该路径由 [`DataMountPaths`](../../src/main/java/com/bupt/tarecruit/common/config/DataMountPaths.java:14) 统一解析。

---

## 1. 数据集的定位

公开 release 中附带的数据集是：

- **可直接运行的测试数据**
- **不包含真实生产敏感信息**
- **用于公开演示、验收、课程展示和功能体验**

使用者下载 release 后，不需要自己初始化数据库结构，只需：

1. 解压数据目录 [`mountDataTAMObupter/`](../../mountDataTAMObupter/README.md)
2. 将环境变量 [`mountDataTAMObupter`](../../src/main/java/com/bupt/tarecruit/common/config/DataMountPaths.java:16) 指向该目录
3. 部署 WAR 并启动 Tomcat

---

## 2. 顶层目录结构

测试数据根目录的逻辑结构如下：

```text
mountDataTAMObupter/
├─ common/
├─ mo/
├─ ta/
└─ admin/
```

其中最关键的业务目录是：

- [`common/`](../../mountDataTAMObupter/common/README.md)
- [`mo/`](../../mountDataTAMObupter/mo/README.md)
- [`ta/`](../../mountDataTAMObupter/ta/README.md)

---

## 3. 目录说明

### 3.1 [`common/`](../../mountDataTAMObupter/common/README.md)

该目录存放 MO 和 TA 共享的岗位主数据。

最关键文件为：

- [`recruitment-courses.json`](../../mountDataTAMObupter/common/recruitment-courses.json)

代码侧由 [`RecruitmentCoursesDao`](../../src/main/java/com/bupt/tarecruit/common/dao/RecruitmentCoursesDao.java:28) 统一读取。

该文件通常决定：

- 岗位板显示内容
- 课程基本信息
- 招聘状态
- TA 工作说明等

### 3.2 [`mo/`](../../mountDataTAMObupter/mo/README.md)

该目录存放 MO 角色相关数据，例如：

- `mos.json`
- `profiles.json`
- `settings.json`
- `mo-application-read-state.json`
- `mo-application-comments.json`

这些文件与 MO 登录、个人资料、设置、申请人阅读状态、评论线程等功能相关，路径定义见 [`DataMountPaths.moDir()`](../../src/main/java/com/bupt/tarecruit/common/config/DataMountPaths.java:37)。

### 3.3 [`ta/`](../../mountDataTAMObupter/ta/README.md)

该目录存放 TA 角色相关数据，是公开演示中最核心的数据区之一。

典型文件包括：

- `tas.json`
- `profiles.json`
- `settings.json`
- `applications.json`
- `application-status.json`
- `application-events.json`

路径定义见：

- [`DataMountPaths.taDir()`](../../src/main/java/com/bupt/tarecruit/common/config/DataMountPaths.java:67)
- [`DataMountPaths.taApplications()`](../../src/main/java/com/bupt/tarecruit/common/config/DataMountPaths.java:87)
- [`DataMountPaths.taApplicationEvents()`](../../src/main/java/com/bupt/tarecruit/common/config/DataMountPaths.java:91)
- [`DataMountPaths.taApplicationStatus()`](../../src/main/java/com/bupt/tarecruit/common/config/DataMountPaths.java:83)

### 3.4 [`ta/resume/`](../../mountDataTAMObupter/ta/apiReadme.md)

该目录存放 TA 投递简历文件。

目录结构遵循：

```text
ta/resume/{taId}/{courseCode}/{storedFileName}
```

相关说明见 [`apiReadme.md`](../../mountDataTAMObupter/ta/apiReadme.md)，路径定义见 [`DataMountPaths.taResumeRoot()`](../../src/main/java/com/bupt/tarecruit/common/config/DataMountPaths.java:95)。

这部分数据关系到：

- 简历上传后的落盘文件
- MO 查看与下载简历
- 申请记录中的简历元数据对应关系

### 3.5 [`ta/ai/`](../../mountDataTAMObupter/ta/ai/README.md)

该目录用于 AI 助理功能相关运行数据。

典型子目录包括：

- `conversations/`
- `attachments/`
- `exports/`

相关路径定义见：

- [`DataMountPaths.taAiRoot()`](../../src/main/java/com/bupt/tarecruit/common/config/DataMountPaths.java:107)
- [`DataMountPaths.taAiConversationRoot()`](../../src/main/java/com/bupt/tarecruit/common/config/DataMountPaths.java:111)
- [`DataMountPaths.taAiAttachmentRoot()`](../../src/main/java/com/bupt/tarecruit/common/config/DataMountPaths.java:119)
- [`DataMountPaths.taAiExportRoot()`](../../src/main/java/com/bupt/tarecruit/common/config/DataMountPaths.java:135)

即使使用者不配置 [`TONGYI_API_KEY`](../../src/main/java/com/bupt/tarecruit/ta/service/TaAiAssistantService.java:385)，这些目录结构仍建议保留，以确保数据挂载结构完整。

### 3.6 [`admin/`](../../mountDataTAMObupter/admin/README.md)

该目录目前为管理侧数据预留区域。若你的公开 release 当前未依赖其中实际业务数据，也建议保留该结构，以保证目录组织一致。

---

## 4. 运行时会读取哪些核心数据

公开 release 启动后，系统通常会从数据目录中读取以下内容：

- 岗位主数据
- MO 账号与资料
- TA 账号与资料
- 申请主记录
- 申请状态与事件流
- 简历文件
- MO 评论和已读状态
- AI 对话与附件目录

这些读取入口最终都会汇总到 [`DataMountPaths.root()`](../../src/main/java/com/bupt/tarecruit/common/config/DataMountPaths.java:26) 解析出来的数据根上。

---

## 5. 运行时哪些目录可能会被写入

使用者运行系统后，以下区域可能发生变化：

### 一定可能被写入

- [`ta/applications.json`](../../mountDataTAMObupter/ta/applications.json)
- [`ta/application-events.json`](../../mountDataTAMObupter/ta/application-events.json)
- [`ta/application-status.json`](../../mountDataTAMObupter/ta/application-status.json)
- [`mo/mo-application-read-state.json`](../../mountDataTAMObupter/mo/mo-application-read-state.json)
- [`mo/mo-application-comments.json`](../../mountDataTAMObupter/mo/mo-application-comments.json)

### 上传、导出或 AI 交互时可能被写入

- [`ta/resume/`](../../mountDataTAMObupter/ta/apiReadme.md)
- [`ta/ai/conversations/`](../../mountDataTAMObupter/ta/ai/README.md)
- [`ta/ai/attachments/`](../../mountDataTAMObupter/ta/ai/README.md)
- [`ta/ai/exports/`](../../mountDataTAMObupter/ta/ai/README.md)

因此，公开 release 的数据目录应被视为：

- **初始测试数据快照**
- **运行后会持续产生变更的可写目录**

---

## 6. 发布者在打包数据集时的建议

### 应保留

- 完整目录层级
- 所有测试 JSON 文件
- 样例简历文件
- 图片资源
- AI 目录骨架

### 应复查

- 简历是否均为测试样例
- 图片是否可公开
- 账号信息是否均为测试信息
- 注释或说明文件中是否含有真实密钥或真实私密信息

### 不应放入公开数据包

- 真实用户数据
- 真实业务日志
- 不必要的本地缓存文件
- 临时调试输出

---

## 7. 推荐的数据包命名方式

建议将测试数据目录单独压缩为：

- `mountDataTAMObupter-sample.zip`

解压后目录名最好仍然保持为：

- `mountDataTAMObupter`

这样可以与代码中的默认命名保持一致，减少理解成本。

---

## 8. 对使用者的说明建议

你可以在 GitHub Release 描述或项目主页中用简短语言说明：

```text
This release includes a sample mounted runtime dataset under mountDataTAMObupter.
Please extract it to your local machine and set environment variable mountDataTAMObupter to that absolute path before starting Tomcat.
```

---

## 9. 进一步阅读

如果使用者想进一步理解数据路径与运行逻辑，建议阅读：

- [`ENVIRONMENT-SETUP.md`](./ENVIRONMENT-SETUP.md)
- [`DEPLOY-ON-TOMCAT.md`](./DEPLOY-ON-TOMCAT.md)
- [`mountDataTAMObupter/README.md`](../../mountDataTAMObupter/README.md)
- [`DataMountPaths`](../../src/main/java/com/bupt/tarecruit/common/config/DataMountPaths.java:14)
- [`RecruitmentCoursesDao`](../../src/main/java/com/bupt/tarecruit/common/dao/RecruitmentCoursesDao.java:28)
- [`apiReadme.md`](../../mountDataTAMObupter/ta/apiReadme.md)
