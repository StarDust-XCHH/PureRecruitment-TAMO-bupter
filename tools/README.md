# 独立开发数据工具

本目录用于存放**与主 Web 项目解耦、可单独运行**的开发辅助工具。

## 当前目标

解决以下开发痛点：

- [`mountDataTAMObupter`](mountDataTAMObupter) 下 JSON 数据文件在调试时被频繁改写
- Git 协同开发时容易产生无意义冲突
- 本地调试数据和团队共享基线数据混在一起
- 手工重置、对比、同步 JSON 文件成本高

## 当前提供的工具

- [`dev-data-tools`](dev-data-tools)

该工具是一个独立的 Java CLI 工程，后续可通过命令行运行。

- [**`ta-mo-submission-cleanup`**](ta-mo-submission-cleanup) — TA 申请 + TA AI + MO 已读/评论 **双端测试数据清理**（主类 `com.bupt.tarecruit.tools.DevApplicationDataCleanupTool`，与 WAR 同模块 `mvn exec:java` 运行；说明见子目录 README）。

- [`genMoCourses.py`](genMoCourses.py)（Python 3）

  在 **单一工作簿**（默认 **`docs/log/mo_courses.xlsx`**，工作表 `mo_courses`）中维护课程行，与 **`mountDataTAMObupter/common/recruitment-courses.json`** 双向同步：在 Excel 里**增行 / 删行 / 改行**后，用 `import` 写回 JSON；`export` 从 JSON 刷新整张表。

  - **`export`**：`JSON → Excel`（便于首次生成表或从 JSON 重建表）。
  - **`import`**：`Excel → JSON`。**以表中行为准**：表里没有的课程会从 JSON 删除；新行即新课程；改行即更新。可选 `--allow-empty` 在表无有效行时清空 JSON 中的 `items`。
  - **`generate`**：从 **`mos.json`** 取 MO 账号（**`ownerMoId` = `id`，`ownerMoName` = `name`**）；**`profiles.json`** 仅要求文件存在（与 `generate` 参数兼容），内容不参与岗位展示名。随机生成 **`EBT0001`–`EBT9999`** 写入 JSON 并 **重写同一 Excel**。招聘状态约 **70% OPEN / 30% CLOSED**。`--mode 1` 追加、`--mode 2` 覆盖；省略则交互选择。兼容简写：首参数为数字时等价于 `generate`。
  - 路径均可覆盖（相对**仓库根目录**）：`--excel`、`--courses-json`、`--mos-json`、`--profiles-json`。
  - **子命令可省略**：直接运行 `python tools/genMoCourses.py` 时会在终端用 **1 / 2 / 3** 选择 export / import / generate（非交互环境请仍写子命令名）。

  ```bash
  pip install pandas openpyxl
  python tools/genMoCourses.py export
  # 编辑 docs/log/mo_courses.xlsx 后：
  python tools/genMoCourses.py import
  python tools/genMoCourses.py generate 9 --mode 2
  python tools/genMoCourses.py 12 --mode 1
  ```

## 设计原则

1. 工具独立于主站运行，不依赖 servlet 生命周期
2. 工具尽量不侵入主业务代码
3. 工具优先服务开发联调、数据重置、差异比对、同步筛选
4. 所有新建或修改过的目录均补充 README 说明
