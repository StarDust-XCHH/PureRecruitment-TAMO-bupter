# Tomcat 部署指南

本文件用于指导公开 release 使用者将项目以 WAR 形式部署到 Tomcat，并正确挂载测试数据目录。

该项目的部署核心不是只拷贝一个 WAR，而是同时满足以下两个条件：

1. Tomcat 成功部署 WAR
2. Tomcat 进程能够读取环境变量 [`mountDataTAMObupter`](../../src/main/java/com/bupt/tarecruit/common/config/DataMountPaths.java:16)

若需要 AI 助理能力，还需要：

3. Tomcat 进程能够读取环境变量 [`TONGYI_API_KEY`](../../src/main/java/com/bupt/tarecruit/ta/service/TaAiAssistantService.java:385)

---

## 1. 部署前准备

在开始前，请确认你已经拿到至少以下文件：

- WAR 包
- 测试数据目录压缩包 [`mountDataTAMObupter/`](../../mountDataTAMObupter/README.md)
- 文档目录 [`docs/releaseReadme/`](../releaseReadme/RELEASE-README.md)

并准备好本机 Tomcat 环境。

---

## 2. 推荐的本地目录准备方式

建议在本地先整理出一套清晰目录，例如：

```text
D:\demo\
├─ tomcat\
├─ data\
│  └─ mountDataTAMObupter\
└─ release\
   └─ PureRecruitment-TAMO-bupter.war
```

这样便于理解：

- [`tomcat/`](../../pom.xml) 是容器
- [`data/mountDataTAMObupter/`](../../mountDataTAMObupter/README.md) 是外部挂载数据
- WAR 是应用程序本体

---

## 3. 解压测试数据目录

先将公开 release 提供的数据包解压到固定位置，例如：

```text
D:\demo\data\mountDataTAMObupter
```

解压后建议检查关键文件是否存在：

- [`common/recruitment-courses.json`](../../mountDataTAMObupter/common/recruitment-courses.json)
- [`ta/applications.json`](../../mountDataTAMObupter/ta/applications.json)
- [`ta/application-events.json`](../../mountDataTAMObupter/ta/application-events.json)
- [`mo/mo-application-comments.json`](../../mountDataTAMObupter/mo/mo-application-comments.json)

---

## 4. 配置数据目录环境变量 [`mountDataTAMObupter`](../../src/main/java/com/bupt/tarecruit/common/config/DataMountPaths.java:16)

### 4.1 PowerShell 临时启动示例

如果你准备在 PowerShell 中启动 Tomcat，可先设置：

```powershell
$env:mountDataTAMObupter = "D:\demo\data\mountDataTAMObupter"
```

### 4.2 推荐做法

对于公开 release 的可复现部署，推荐：

- 始终使用绝对路径
- 始终显式设置该变量
- 不依赖默认相对回退路径 [`DEFAULT_DATA_ROOT`](../../src/main/java/com/bupt/tarecruit/common/config/DataMountPaths.java:18)

这样可避免 Tomcat 工作目录差异带来的路径问题。

---

## 5. 如需 AI 功能，配置 [`TONGYI_API_KEY`](../../src/main/java/com/bupt/tarecruit/ta/service/TaAiAssistantService.java:385)

若你希望公开 release 使用者体验 AI 助理能力，需要在启动 Tomcat 前配置：

```powershell
$env:TONGYI_API_KEY = "your_api_key_here"
```

若不配置该变量，AI 功能会不可用，但普通业务功能仍可使用，相关逻辑见 [`TaAiAssistantService`](../../src/main/java/com/bupt/tarecruit/ta/service/TaAiAssistantService.java:398)。

---

## 6. 将 WAR 部署到 Tomcat

将 release 中的 WAR 包复制到 Tomcat 的 `webapps` 目录中。

部署方式通常为：

```text
<TOMCAT_HOME>/webapps/PureRecruitment-TAMO-bupter.war
```

Tomcat 启动后会自动解压并部署该应用。

如果你打算修改上下文路径，也可以调整 WAR 命名或使用容器级上下文配置，但对公开 release 使用者而言，直接放入 `webapps` 是最简单方案。

---

## 7. 启动 Tomcat

启动方式取决于你的 Tomcat 安装方式。Windows 下常见做法是运行 `bin` 目录下的启动脚本。

关键点不是脚本本身，而是：

- 启动 Tomcat 的那个进程必须能读到 [`mountDataTAMObupter`](../../src/main/java/com/bupt/tarecruit/common/config/DataMountPaths.java:16)
- 若需要 AI，还必须能读到 [`TONGYI_API_KEY`](../../src/main/java/com/bupt/tarecruit/ta/service/TaAiAssistantService.java:385)

如果你在某个终端设置了环境变量，但 Tomcat 是由其他入口启动的，则变量可能不会生效。

---

## 8. 启动后如何验证挂载成功

应用启动时会在初始化监听器中打印数据挂载状态，入口见 [`DataMountStartupListener.contextInitialized()`](../../src/main/java/com/bupt/tarecruit/common/listener/DataMountStartupListener.java:16)。

理想情况下，日志中应出现：

```text
[data-mount] root=D:\demo\data\mountDataTAMObupter fromEnvironment=true env=mountDataTAMObupter
```

这行日志非常关键，它能直接告诉你：

- 当前使用的数据根目录是什么
- 是否真正来自环境变量
- 使用的环境变量名是什么

### 如何理解日志

- `fromEnvironment=true` ：说明挂载成功，程序用了你指定的目录
- `fromEnvironment=false` ：说明程序没有读到变量，转而使用了默认相对路径

若你看到的是默认路径，则应优先排查环境变量注入问题。

---

## 9. 启动后建议的功能验证清单

Tomcat 成功启动后，建议至少检查以下功能：

### 基础验证

- [ ] 首页可打开
- [ ] 静态资源正常加载
- [ ] 无明显 404 / 500

### TA 侧验证

- [ ] TA 登录可用
- [ ] 岗位列表可显示
- [ ] 个人资料读取正常
- [ ] 申请记录可查看
- [ ] 简历下载可用

### MO 侧验证

- [ ] MO 登录可用
- [ ] 申请人列表可显示
- [ ] 评论与已读状态可用

### AI 侧验证

- [ ] 若配置了 [`TONGYI_API_KEY`](../../src/main/java/com/bupt/tarecruit/ta/service/TaAiAssistantService.java:385)，AI 助理可正常调用
- [ ] 若未配置该变量，AI 功能应显示禁用或提示，而不是导致系统整体故障

---

## 10. 常见部署问题

### 问题 1：WAR 部署成功，但页面没有业务数据

常见原因：

- [`mountDataTAMObupter`](../../src/main/java/com/bupt/tarecruit/common/config/DataMountPaths.java:16) 未生效
- 变量路径不是实际的数据根目录
- 数据包解压不完整

建议检查：

- Tomcat 日志中的 `[data-mount]`
- 数据目录中是否存在 [`common/recruitment-courses.json`](../../mountDataTAMObupter/common/recruitment-courses.json)
- Tomcat 启动进程是否继承了环境变量

### 问题 2：业务正常，但 AI 助理不可用

常见原因：

- 未设置 [`TONGYI_API_KEY`](../../src/main/java/com/bupt/tarecruit/ta/service/TaAiAssistantService.java:385)
- key 无效
- Tomcat 未继承该环境变量

建议检查：

- AI 相关接口提示
- 变量名是否准确
- 是否在启动 Tomcat 前完成配置

### 问题 3：本地开发能用，换台机器后不可用

常见原因：

- 依赖了相对路径回退，而不是显式配置 [`mountDataTAMObupter`](../../src/main/java/com/bupt/tarecruit/common/config/DataMountPaths.java:16)

建议解决：

- 对所有 release 使用者统一要求设置绝对路径环境变量

---

## 11. 推荐的公开部署说明语句

你可以在 GitHub Release 页面写一段简短提示：

```text
Deploy the WAR to Tomcat, extract the sample dataset, and set environment variable mountDataTAMObupter to the extracted dataset path before startup.
If you want to enable the AI assistant, also set TONGYI_API_KEY.
```

---

## 12. 进一步阅读

部署完成后，建议继续阅读：

- [`RELEASE-README.md`](./RELEASE-README.md)
- [`ENVIRONMENT-SETUP.md`](./ENVIRONMENT-SETUP.md)
- [`DATASET-README.md`](./DATASET-README.md)
- [`mountDataTAMObupter/README.md`](../../mountDataTAMObupter/README.md)
