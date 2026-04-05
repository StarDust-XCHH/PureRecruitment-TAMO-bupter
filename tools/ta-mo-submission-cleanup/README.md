# TA + MO 申请测试数据双端清理

实现类位于主 Web 工程包 **`com.bupt.tarecruit.tools.DevApplicationDataCleanupTool`**（`src/main/java/com/bupt/tarecruit/tools/`），因需调用 **`DataMountPaths`** 与 **`TaSubmissionCleanupTool`**，与 WAR **同一 Maven 模块**编译运行；本目录仅作文档与入口脚本归属 **`tools/`**。

## 作用

- 复用 **`TaSubmissionCleanupTool.main`**、**`TaAiDataCleanupTool.main`**
- 重置 **`mo-application-read-state.json`**、**`mo-application-comments.json`** 为空 `items`
- 流程结束后调用 **`MoRecruitmentDao.syncAllPublishedJobApplicationStatsFromTa()`**，按当前 TA 投递与 **`application-status.json`** 重算并写回 **`recruitment-courses.json`** 中的申请/已录用等统计字段（本工具**不直接改写**课程 JSON）
- **不删除**岗位条目：不清理 `recruitment-courses.json` 里的课程行本身；**不清理**账号类 JSON

## 运行（仓库根目录）

**不传 `-Dexec.args` 时**会在终端打印菜单（1～4 选模式，0 退出）；与 `genMoCourses.py` 无参交互类似。无 stdin 时自动按「全部清理」执行。

清理成功结束后会**再询问一次**：是否执行 `tools/genMoCourses.py import`（默认 Excel `docs/log/mo_courses.xlsx` → 当前数据挂载下的 `recruitment-courses.json`）。需本机已安装 Python 且可运行 `pandas`/`openpyxl`。自动化或脚本中若不想停顿，可加 **`--no-gen-mo-courses-offer`**（可与 `--mo-only` 等组合）。

```bash
mvn -q -DskipTests compile exec:java -Dexec.mainClass=com.bupt.tarecruit.tools.DevApplicationDataCleanupTool
```

非交互、直接指定模式：

```bash
mvn -q -DskipTests compile exec:java -Dexec.mainClass=com.bupt.tarecruit.tools.DevApplicationDataCleanupTool -Dexec.args="--mo-only"
```

参数：`--mo-only` | `--ta-only` | `--skip-ai` | `--no-gen-mo-courses-offer`（说明见类 Javadoc）。

## 脚本

同目录下 **`run-dev-application-data-cleanup.ps1`** 可在 Windows 下从任意工作目录调用（内部切换到仓库根）。
