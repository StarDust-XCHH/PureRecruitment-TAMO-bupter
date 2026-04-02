# `common` · 跨模块公共层

`com.bupt.tarecruit.common` 聚合 **配置、持久化访问、模型、工具、Web 过滤器与监听器**，供 MO / TA 等模块复用，避免在业务包里散落路径与契约。

**子文档索引**

| 目录 / 主题 | 说明 |
| --- | --- |
| [`dao/README.md`](dao/README.md) | `RecruitmentCoursesDao`、岗位 JSON 契约与 API |
| [`util/README.md`](util/README.md) | `AuthUtils`、`JsonUtils`、`ServletJsonResponseWriter`、**`GsonJsonObjectUtils` 完整 API 表** |
| [`listener/README.md`](listener/README.md) | `DataMountStartupListener`、启动日志 |
| [`filter/README.md`](filter/README.md) | `EncodingFilter` |
| [`model/README.md`](model/README.md) | `ApiResponse` 与 HTTP 序列化关系 |
| [`config/README.md`](config/README.md) | `DataMountPaths`、环境变量 |

---

## 包结构速览

| 包 | 内容 |
| --- | --- |
| **`config`** | **`DataMountPaths`**：数据根路径；环境变量 **`mountDataTAMObupter`** 未设置时，使用相对目录 `mountDataTAMObupter` 并 **`toAbsolutePath().normalize()`**（与工作目录有关，部署时需确认）。详见 [`config/README.md`](config/README.md)。 |
| **`dao`** | 共用 JSON 文件访问（如 `RecruitmentCoursesDao`）。 |
| **`model`** | **`ApiResponse<T>`** 等统一 API 响应壳。 |
| **`util`** | 认证散列、HTTP JSON 写出、Gson `JsonObject` 读字段等。 |
| **`filter`** | **`EncodingFilter`**：全局请求/响应 UTF-8。 |
| **`listener`** | **`DataMountStartupListener`**：见下文。 |

---

## 数据根与启动日志

应用启动时 **`DataMountStartupListener`** 会打印两行（标准输出）：

1. **`[data-mount]`**：直接来自 **`DataMountPaths`** — 绝对路径 **`root`**、是否由环境变量解析（**`fromEnvironment`**）、环境变量名 **`env`**（即 `mountDataTAMObupter`）。
2. **`[TA-DATA]`**：**`TaAccountDao.getDataMountStatusMessage()`** 的中文说明（保留原有 TA 侧提示）。

两行信息可能部分重叠，便于对照「配置类解析结果」与「业务 DAO 文案」。

---

## 两套 JSON 用法（刻意分工）

- **写 HTTP 响应**：**`JsonUtils`** 将 **`ApiResponse`** 及 `Map`/`List` 等 **手写**为 JSON 字符串；通常经 **`ServletJsonResponseWriter`**。  
- **读 Gson 树**：**`GsonJsonObjectUtils`** 从 **`JsonObject`** 安全取字段；与 Gson 解析请求体、DAO 读文件一致。

不强行合并成「全部 Gson 序列化」，避免无收益的中等规模重构；细节见 [`util/README.md`](util/README.md)。

---

## 相关仓库文档

- `docs/api/mo-job-board-api-v2.md`、`docs/database/recruitment-courses-governance-notes.md`  
- 挂载目录旁说明：`mountDataTAMObupter/common/README.md` 等
