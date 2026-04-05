# TA + MO 申请测试数据双端清理

实现类位于主 Web 工程包 **`com.bupt.tarecruit.tools.DevApplicationDataCleanupTool`**（`src/main/java/com/bupt/tarecruit/tools/`），因需调用 **`DataMountPaths`** 与 **`TaSubmissionCleanupTool`**，与 WAR **同一 Maven 模块**编译运行；本目录仅作文档与入口脚本归属 **`tools/`**。

## 作用

- 复用 **`TaSubmissionCleanupTool.main`**、**`TaAiDataCleanupTool.main`**
- 重置 **`mo-application-read-state.json`**、**`mo-application-comments.json`** 为空 `items`
- **不清理**：`recruitment-courses.json`、账号类 JSON

## 运行（仓库根目录）

**不传 `-Dexec.args` 时**会在终端打印菜单（1～4 选模式，0 退出）；与 `genMoCourses.py` 无参交互类似。无 stdin 时自动按「全部清理」执行。

```bash
mvn -q -DskipTests compile exec:java -Dexec.mainClass=com.bupt.tarecruit.tools.DevApplicationDataCleanupTool
```

非交互、直接指定模式：

```bash
mvn -q -DskipTests compile exec:java -Dexec.mainClass=com.bupt.tarecruit.tools.DevApplicationDataCleanupTool -Dexec.args="--mo-only"
```

参数：`--mo-only` | `--ta-only` | `--skip-ai`（说明见类 Javadoc）。

## 脚本

同目录下 **`run-dev-application-data-cleanup.ps1`** 可在 Windows 下从任意工作目录调用（内部切换到仓库根）。
