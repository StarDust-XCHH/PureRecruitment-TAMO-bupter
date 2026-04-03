# `common.model` · 通用模型

上级包说明见 **[`../README.md`](../README.md)**；HTTP 写出与工具分工见 **[`../util/README.md`](../util/README.md)**。

---

## `ApiResponse<T>`

泛型 **`T`** 为 **`data`** 载荷类型；失败时 **`data`** 恒为 **`null`**。

### 工厂方法

| 签名 | 参数 | 返回值 |
| --- | --- | --- |
| `static <T> ApiResponse<T> success(String message, T data)` | **`message`**：提示文案；**`data`**：成功载荷，可为 **`null`**。 | **`success == true`** 的实例。 |
| `static <T> ApiResponse<T> failure(String message)` | **`message`**：错误说明。 | **`success == false`**，**`data == null`**。 |

### 访问器（实例方法）

| 签名 | 返回值 |
| --- | --- |
| `boolean isSuccess()` | 是否成功。 |
| `String getMessage()` | 提示或错误信息。 |
| `T getData()` | 成功时的数据；失败时为 **`null`**。 |

### JSON 形态（经 `JsonUtils.toJson`）

与字段名一致：**`success`**（布尔）、**`message`**（字符串）、**`data`**（由 **`JsonUtils.toJsonValue`** 递归序列化）。

- **读请求 / 读文件 JSON**：请使用 Gson + **`GsonJsonObjectUtils`**；**`ApiResponse`** 不负责解析入站 JSON。
