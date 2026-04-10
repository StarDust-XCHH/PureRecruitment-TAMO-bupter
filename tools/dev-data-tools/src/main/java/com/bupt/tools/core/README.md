# core 包说明

本目录存放与具体命令无关的核心业务逻辑。

## 当前核心能力

- 初始化本地工作副本
- 重置本地工作副本
- 计算基线与工作副本差异
- 将工作副本按规则同步回基线

## 主要类

- [`WorkspaceService.java`](tools/dev-data-tools/src/main/java/com/bupt/tools/core/WorkspaceService.java)
- [`WorkspacePaths.java`](tools/dev-data-tools/src/main/java/com/bupt/tools/core/WorkspacePaths.java)
