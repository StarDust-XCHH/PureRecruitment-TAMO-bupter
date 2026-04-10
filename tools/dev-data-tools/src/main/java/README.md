# Java 源码目录说明

本目录存放 [`dev-data-tools`](tools/dev-data-tools) 的 Java 源码。

## 包结构

- [`com.bupt.tools.cli`](tools/dev-data-tools/src/main/java/com/bupt/tools/cli)：命令行入口与参数分发
- [`com.bupt.tools.core`](tools/dev-data-tools/src/main/java/com/bupt/tools/core)：工作副本初始化、重置、差异分析、同步核心逻辑
- [`com.bupt.tools.model`](tools/dev-data-tools/src/main/java/com/bupt/tools/model)：命令结果与差异模型
- [`com.bupt.tools.support`](tools/dev-data-tools/src/main/java/com/bupt/tools/support)：文件系统访问、JSON 比较、规则过滤支撑类

## 当前约束

1. 工具必须独立运行
2. 不直接依赖主项目 servlet 代码
3. 通过路径扫描 [`mountDataTAMObupter`](mountDataTAMObupter) 工作
4. 尽量把运行态字段过滤逻辑集中在支撑层实现
