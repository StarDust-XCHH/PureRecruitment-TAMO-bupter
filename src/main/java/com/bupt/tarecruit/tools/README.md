# `com.bupt.tarecruit.tools` · 主工程内可执行开发工具

本包存放 **带 `main`、通过 `mvn exec:java` 运行** 的辅助程序，与 **WAR 主模块** 同源码树编译，以便复用 **`DataMountPaths`**、既有 TA 清理类等运行时路径与逻辑。

仓库侧 **`tools/`** 目录另有脚本与说明（如 PowerShell 入口）；**实现类**在此包内。

---

## `DevApplicationDataCleanupTool`

**用途**：测试环境下一次性对齐 **TA 申请 / TA AI / MO 申请旁路 JSON**（已读状态、申请评论、**候选短名单** `mo-applicant-shortlist.json`），避免只清 TA 后 MO 侧仍引用旧 `applicationId`。类名刻意不用 `Ta*` 前缀，以免与 **`TaSubmissionCleanupTool`** 等 TA 专用工具混淆。

结束前会调用 **`MoRecruitmentDao.syncAllPublishedJobApplicationStatsFromTa()`**，仅刷新 **`recruitment-courses.json`** 中与申请相关的统计字段；本工具**不直接改写**课程文件。

**不清理**：课程岗位条目本身（不删 JSON 中的课程行）、账号与 profile 类 JSON。

**运行**（仓库根目录）：

```bash
mvn -q -DskipTests compile exec:java -Dexec.mainClass=com.bupt.tarecruit.tools.DevApplicationDataCleanupTool
```

- **无命令行参数**：终端交互菜单选模式（无 stdin 时退化为「全部清理」）。
- **有参数**：`--mo-only` | `--ta-only` | `--skip-ai`（与菜单选项对应，见类 Javadoc）。

**可编程调用**：`resetMoApplicationSidecarFiles()` 仅重置 MO 三个申请旁路结构化文件（已读、评论、短名单）。

更完整的说明与 Windows 脚本见 **[`../../../../../../tools/ta-mo-submission-cleanup/README.md`](../../../../../../tools/ta-mo-submission-cleanup/README.md)**（由本文件到仓库 `tools/ta-mo-submission-cleanup/` 的相对路径）。

---

## 与 `ta.util` 下工具的关系

- **`TaSubmissionCleanupTool`**、**`TaAiDataCleanupTool`** 仍在 **`com.bupt.tarecruit.ta.util`**；本工具通过调用其 `main` 组合行为，**不修改**上述类源码。
