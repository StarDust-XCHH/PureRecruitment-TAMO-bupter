# cli 包说明

本目录存放命令行入口类。

## 职责

- 接收命令行参数
- 分发 `workspace init`、`workspace reset`、`workspace diff`、`workspace sync`
- 输出执行报告与错误信息

## 当前入口

- [`DevDataToolsApplication.java`](tools/dev-data-tools/src/main/java/com/bupt/tools/cli/DevDataToolsApplication.java)
