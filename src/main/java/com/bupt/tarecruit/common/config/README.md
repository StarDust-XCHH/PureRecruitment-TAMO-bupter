# `common.config` · 配置

上级包说明见 **[`../README.md`](../README.md)**。

---

## `DataMountPaths`

`final` 类，**全部为静态成员**。类加载时解析一次数据根，之后 **`root()`**、**`fromEnvironment()`** 不变。

### 常量

| 成员 | 含义 |
| --- | --- |
| `String DATA_MOUNT_ENV` | 环境变量名 **`mountDataTAMObupter`**：值为数据根目录路径。 |
| `Path DEFAULT_DATA_ROOT` | 未设置环境变量时使用的相对路径名（与 `DATA_MOUNT_ENV` 同名目录）；经 **`toAbsolutePath().normalize()`** 解析，**依赖进程工作目录**。 |

### 方法

| 签名 | 参数 | 返回值 |
| --- | --- | --- |
| `Path root()` | 无 | 当前数据根目录（**绝对路径、已规范化**）。 |
| `boolean fromEnvironment()` | 无 | **`true`**：根路径来自环境变量 **`DATA_MOUNT_ENV`**；**`false`**：使用 **`DEFAULT_DATA_ROOT`** 解析结果。 |
| `Path moDir()` | 无 | **`<root>/mo`**（预留，MO 专用子树；并非全部 MO 数据）。 |
| `Path taDir()` | 无 | **`<root>/ta`**。 |
| `Path moRecruitmentCourses()` | 无 | **`<root>/common/recruitment-courses.json`**（岗位板）。 |
| `Path taAccounts()` | 无 | **`<root>/ta/tas.json`**。 |
| `Path taProfiles()` | 无 | **`<root>/ta/profiles.json`**。 |
| `Path taApplicationStatus()` | 无 | **`<root>/ta/application-status.json`**。 |

### 启动日志

与 **`DataMountStartupListener`** 中 **`[data-mount]`** 行一致，详见 **`DataMountPaths`** 类上 JavaDoc。
