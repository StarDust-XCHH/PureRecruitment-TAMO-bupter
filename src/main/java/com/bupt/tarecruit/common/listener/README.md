# `common.listener` · Servlet 监听器

上级包说明见 **[`../README.md`](../README.md)**。

---

## `DataMountStartupListener`

实现 **`jakarta.servlet.ServletContextListener`**。

| 项 | 说明 |
| --- | --- |
| **注解** | `@WebListener` |
| **`contextInitialized(ServletContextEvent sce)`** | 见下表 |
| **`contextDestroyed`** | **未覆盖**（无额外清理逻辑）。 |

### `contextInitialized` 行为

| 顺序 | 输出 | 说明 |
| --- | --- | --- |
| 1 | **`System.out`**：`[data-mount] root=... fromEnvironment=... env=...` | 来自 **`DataMountPaths`** |
| 2 | **`System.out`**：**`TaAccountDao.getDataMountStatusMessage()`** | **`[TA-DATA]`** 文案 |

数据根规则见 **`DataMountPaths`**（[`../config/README.md`](../config/README.md)）。
