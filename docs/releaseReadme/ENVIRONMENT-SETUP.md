# 环境变量配置说明

本项目公开 release 采用 **Tomcat + WAR + 外部挂载目录** 的运行方式，因此部署前必须先理解环境变量的职责。

代码中与环境变量直接相关的两个核心入口分别是：

- 数据根目录环境变量 [`mountDataTAMObupter`](../../src/main/java/com/bupt/tarecruit/common/config/DataMountPaths.java:16)
- AI 服务密钥环境变量 [`TONGYI_API_KEY`](../../src/main/java/com/bupt/tarecruit/ta/service/TaAiAssistantService.java:385)

---

## 1. 为什么必须配置环境变量

本项目没有把所有运行数据硬编码进 WAR 包内部。

程序启动后会先通过 [`DataMountPaths.resolveDataRoot()`](../../src/main/java/com/bupt/tarecruit/common/config/DataMountPaths.java:147) 解析数据根目录：

- 若读取到 [`mountDataTAMObupter`](../../src/main/java/com/bupt/tarecruit/common/config/DataMountPaths.java:16)，则使用该环境变量的值作为数据根目录
- 若未读取到，则回退到默认相对目录 [`mountDataTAMObupter/`](../../mountDataTAMObupter/README.md)

而 AI 助理功能则会通过 [`TaAiAssistantService.QwenLongAiProvider.readApiKey()`](../../src/main/java/com/bupt/tarecruit/ta/service/TaAiAssistantService.java:575) 读取 [`TONGYI_API_KEY`](../../src/main/java/com/bupt/tarecruit/ta/service/TaAiAssistantService.java:385)。

因此：

- 数据目录环境变量控制“程序去哪找数据”
- AI 密钥环境变量控制“AI 功能能不能调用外部模型服务”

---

## 2. 环境变量一览

| 变量名 | 是否必须 | 用途 | 未配置时的结果 |
| --- | --- | --- | --- |
| [`mountDataTAMObupter`](../../src/main/java/com/bupt/tarecruit/common/config/DataMountPaths.java:16) | 强烈建议配置 | 指向运行时数据根目录 | 程序会尝试回退到仓库内默认相对目录 |
| [`TONGYI_API_KEY`](../../src/main/java/com/bupt/tarecruit/ta/service/TaAiAssistantService.java:385) | 仅 AI 功能必须 | 用于调用 AI 模型服务 | AI 功能不可用，但非 AI 功能仍可运行 |

---

## 3. 如何配置 [`mountDataTAMObupter`](../../src/main/java/com/bupt/tarecruit/common/config/DataMountPaths.java:16)

### 3.1 该变量应该指向什么

它应该指向 release 中测试数据目录 [`mountDataTAMObupter/`](../../mountDataTAMObupter/README.md) 的**绝对路径**。

例如，你把数据目录解压到了：

```text
D:\demo\mountDataTAMObupter
```

那么该变量就应配置为这个完整路径。

### 3.2 为什么推荐始终显式配置

虽然代码允许回退到默认相对路径 [`DEFAULT_DATA_ROOT`](../../src/main/java/com/bupt/tarecruit/common/config/DataMountPaths.java:18)，但相对路径依赖进程工作目录，不利于公开发布的稳定复现。

对 release 使用者来说，最稳妥的方法是：

- 总是手动配置 [`mountDataTAMObupter`](../../src/main/java/com/bupt/tarecruit/common/config/DataMountPaths.java:16)
- 总是使用绝对路径

### 3.3 Windows PowerShell 临时配置示例

```powershell
$env:mountDataTAMObupter = "D:\demo\mountDataTAMObupter"
```

这种方式只对当前 PowerShell 会话有效。

### 3.4 Windows 图形界面长期配置

可通过“系统环境变量”界面新增：

- 变量名：`mountDataTAMObupter`
- 变量值：数据目录绝对路径

配置完成后，重新启动 Tomcat 或对应终端，使变量生效。

### 3.5 Tomcat 场景下的重点注意

如果你是通过 Tomcat 启动脚本启动，而不是在当前命令行中直接启动，需要确保：

- 启动 Tomcat 的进程能读到 [`mountDataTAMObupter`](../../src/main/java/com/bupt/tarecruit/common/config/DataMountPaths.java:16)
- 不是只在某个无关终端里设置了变量

---

## 4. 如何配置 [`TONGYI_API_KEY`](../../src/main/java/com/bupt/tarecruit/ta/service/TaAiAssistantService.java:385)

### 4.1 该变量是什么

这是 AI 助理功能访问外部模型服务的 API Key。

代码中常量定义见 [`API_KEY_ENV`](../../src/main/java/com/bupt/tarecruit/ta/service/TaAiAssistantService.java:385)。

### 4.2 公开 release 为什么不附带真实值

公开 release 的原则是：

- 发布配置方法
- 不发布真实密钥

因此，使用者需要自己申请并填写自己的 key。

### 4.3 Windows PowerShell 临时配置示例

```powershell
$env:TONGYI_API_KEY = "your_api_key_here"
```

### 4.4 如果不配置会发生什么

若未读取到有效 key，AI 功能会显示禁用或报出“未读取到环境变量”的提示，见 [`TaAiAssistantService`](../../src/main/java/com/bupt/tarecruit/ta/service/TaAiAssistantService.java:398) 与 [`TaAiAssistantService`](../../src/main/java/com/bupt/tarecruit/ta/service/TaAiAssistantService.java:407)。

这不会影响以下普通能力：

- 普通页面访问
- TA / MO 登录
- 岗位查看
- 申请与简历管理
- 基于 JSON 数据的常规业务流转

---

## 5. 启动后如何确认环境变量生效

应用启动时会打印数据挂载日志，入口见 [`DataMountStartupListener.contextInitialized()`](../../src/main/java/com/bupt/tarecruit/common/listener/DataMountStartupListener.java:16)。

若配置成功，控制台应出现类似内容：

```text
[data-mount] root=D:\demo\mountDataTAMObupter fromEnvironment=true env=mountDataTAMObupter
```

其中：

- `root=...` 表示当前实际解析出的目录
- `fromEnvironment=true` 表示确实来自环境变量
- `env=mountDataTAMObupter` 表示使用的变量名

如果看到 `fromEnvironment=false`，说明程序没有从环境变量中读到值，而是回退到了默认路径。

---

## 6. 推荐的最小配置组合

对于公开 release 的普通体验者，推荐最小配置如下：

### 必配

- [`mountDataTAMObupter`](../../src/main/java/com/bupt/tarecruit/common/config/DataMountPaths.java:16)

### 选配

- [`TONGYI_API_KEY`](../../src/main/java/com/bupt/tarecruit/ta/service/TaAiAssistantService.java:385)

如果只想演示非 AI 业务流程，完全可以只配置数据目录变量。

---

## 7. 常见错误与排查

### 问题 1：系统能启动，但看不到数据

可能原因：

- [`mountDataTAMObupter`](../../src/main/java/com/bupt/tarecruit/common/config/DataMountPaths.java:16) 配置错误
- 路径写成了父目录或错误目录
- Tomcat 进程没有读取到变量

排查方法：

- 看启动日志中的 `[data-mount]`
- 对照实际解压路径检查是否一致
- 检查目录下是否确实存在 [`common/recruitment-courses.json`](../../mountDataTAMObupter/common/recruitment-courses.json) 等文件

### 问题 2：普通功能正常，但 AI 不可用

可能原因：

- 未配置 [`TONGYI_API_KEY`](../../src/main/java/com/bupt/tarecruit/ta/service/TaAiAssistantService.java:385)
- key 无效
- Tomcat 进程未读到该变量

排查方法：

- 检查 AI 页面或接口返回的提示信息
- 确认环境变量名是否完全一致
- 确认没有多余空格

### 问题 3：在终端里设置了变量，但 Tomcat 仍然读不到

可能原因：

- 你在一个终端设置了变量，但 Tomcat 是由别的方式启动的
- 设置变量后未重启 Tomcat

解决方法：

- 在启动 Tomcat 的同一上下文中配置变量
- 修改系统环境变量后重新启动 Tomcat

---

## 8. 面向公开 release 的推荐写法

如果你要在 GitHub Release 描述中简短说明环境变量，建议采用以下表述：

```text
Runtime data directory is resolved from environment variable mountDataTAMObupter.
AI assistant API key is resolved from environment variable TONGYI_API_KEY.
No real API key is included in this public release.
```

---

## 9. 进一步阅读

完成环境变量配置后，建议继续阅读：

- [`DEPLOY-ON-TOMCAT.md`](./DEPLOY-ON-TOMCAT.md)
- [`DATASET-README.md`](./DATASET-README.md)
- [`mountDataTAMObupter/README.md`](../../mountDataTAMObupter/README.md)
- [`DataMountPaths`](../../src/main/java/com/bupt/tarecruit/common/config/DataMountPaths.java:14)

这些内容可以帮助使用者进一步理解路径解析、目录结构和部署方式。