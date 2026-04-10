# support 包说明

本目录存放文件系统访问、JSON 处理、忽略规则过滤等底层支撑逻辑。

## 当前主要类

- [`JsonSupport.java`](tools/dev-data-tools/src/main/java/com/bupt/tools/support/JsonSupport.java)
- [`FileSupport.java`](tools/dev-data-tools/src/main/java/com/bupt/tools/support/FileSupport.java)
- [`SyncRules.java`](tools/dev-data-tools/src/main/java/com/bupt/tools/support/SyncRules.java)

## 设计说明

这里集中管理：
- 哪些目录和文件默认忽略
- 哪些字段被视为运行态字段
- JSON 深度比较与字段过滤逻辑
