# 数据根目录 `mountDataTAMObupter`

本目录为 **运行时 JSON 数据根** 在仓库中的默认位置：Tomcat/本地启动时，若未配置环境变量，应用会回退到该相对路径（经工作目录解析为绝对路径）。生产部署建议通过环境变量指向服务器上的独立数据目录。

## 解析方式（与代码一致）

- **环境变量名**：`mountDataTAMObupter`（见 `[DataMountPaths.java](../src/main/java/com/bupt/tarecruit/common/config/DataMountPaths.java)`）。
- **未设置或为空**：使用相对路径 `mountDataTAMObupter`，即本仓库下该文件夹；路径随进程工作目录变化，部署时需确认工作目录。
- 启动时控制台会打印 `[data-mount]` 一行（解析结果、是否来自环境变量），可与业务侧日志对照。

## 子目录与文档索引


| 路径                            | 说明                                                                                                                                                                                       |
| ----------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `[common/](common/README.md)` | MO–TA **共享** 岗位主数据 `[recruitment-courses.json](common/recruitment-courses.json)`；字段与契约见该目录 README，DAO 旁注见 `[recruitment-courses-dao-notes.md](common/recruitment-courses-dao-notes.md)`。 |
| `[ta/](ta/README.md)`         | TA 账号、档案、设置、申请状态等 JSON；读写由 `TaAccountDao` 等负责。                                                                                                                                           |
| `mo/`                         | MO 账号/资料/设置（`mos.json` 等）及申请人辅助数据：`mo-application-read-state.json`（已读）、`mo-application-comments.json`（评论）、`mo-applicant-shortlist.json`（候选短名单）。与 TA 侧 `applications.json` / `application-status.json` 的协作见 `[docs/backend/mo-ta-interaction-log.md](../docs/backend/mo-ta-interaction-log.md)`。                                                |


## 部署（当前：本地 Tomcat）

当前开发/联调以 **本地 Tomcat 容器** 部署 WAR 为主。GitHub 仓库操作、Tomcat 配置及与本项目相关的数据目录设置，**以 `[docs/Github操作指南+Tomcat配置.docx](../docs/Github操作指南+Tomcat配置.docx)` 为准**。

### 环境变量补充（数据根）

在 Tomcat 或系统环境中需要单独指定数据根目录时，可将 `mountDataTAMObupter` 指向目标路径（具体配置入口见上述 Word 文档）。命令行示例（路径按实际修改）：

```bash
export mountDataTAMObupter=/var/lib/tamo-data
```

Windows（PowerShell）示例：

```powershell
$env:mountDataTAMObupter = "D:\data\tamo"
```

## 与仓库文档的关系

- 项目总说明中的 **文档与岗位数据** 入口：仓库根 `[README.md](../README.md)`。
- 部署与 GitHub：`[docs/Github操作指南+Tomcat配置.docx](../docs/Github操作指南+Tomcat配置.docx)`。
- 接口契约：`[docs/api/mo-job-board-api-v2.md](../docs/api/mo-job-board-api-v2.md)`。

