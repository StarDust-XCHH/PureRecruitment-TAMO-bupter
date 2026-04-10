# dev-data-tools

这是一个**独立于主项目运行时**的开发辅助工具工程，主要用于解决 [`mountDataTAMObupter`](mountDataTAMObupter) 下 JSON 数据在联调时频繁被改写、并与 Git 协同产生冲突的问题。

## 当前提供的命令

### 1. `workspace init`
从共享基线目录初始化本地工作副本目录：
- 基线目录默认是 [`mountDataTAMObupter`](mountDataTAMObupter)
- 工作副本目录默认是 [`mountDataTAMObupter/.workspace`](mountDataTAMObupter/.workspace)

### 2. `workspace reset`
删除并重新创建本地工作副本，使其重新对齐基线。

### 3. `workspace diff`
对比基线目录与工作副本目录，输出新增、删除、变更文件，并支持忽略运行态字段。

### 4. `workspace sync`
将工作副本中的有效改动同步回基线目录，同时忽略不应该提交到 Git 的运行态字段。

## 默认规则

工具默认会忽略以下目录：
- `.workspace`
- `.git`
- `image`
- `uploads`
- `avatars`
- `exports`
- `resumes`
- `target`

工具默认会忽略以下易变字段：
- `updatedAt`
- `createdAt`
- `lastUpdatedAt`
- `lastLoginAt`
- `failedAttempts`
- `profileSavedAt`
- `loginAt`

## 运行方式

在 [`tools/dev-data-tools`](tools/dev-data-tools) 下执行：

```bash
mvn package
java -jar target/dev-data-tools-1.0-SNAPSHOT-jar-with-dependencies.jar workspace init
java -jar target/dev-data-tools-1.0-SNAPSHOT-jar-with-dependencies.jar workspace diff
```

## 目录说明

- [`src/main/java/com/bupt/tools/cli`](tools/dev-data-tools/src/main/java/com/bupt/tools/cli)：命令行入口
- [`src/main/java/com/bupt/tools/core`](tools/dev-data-tools/src/main/java/com/bupt/tools/core)：工作副本、同步、差异分析核心逻辑
- [`src/main/java/com/bupt/tools/model`](tools/dev-data-tools/src/main/java/com/bupt/tools/model)：结果模型
- [`src/main/java/com/bupt/tools/support`](tools/dev-data-tools/src/main/java/com/bupt/tools/support)：文件与 JSON 支撑工具

## 设计目标

1. 不要求主站先运行
2. 尽量不侵入现有业务代码
3. 优先解决开发中的 JSON 污染与 Git 冲突
4. 保证任何新建目录都附带 README 说明
