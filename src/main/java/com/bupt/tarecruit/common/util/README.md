# `common.util` · 通用工具

上级包索引、数据根与 **`JsonUtils` / Gson 读字段** 的分工说明见 **[`../README.md`](../README.md)**。

本包均为 `final` + 私有构造 + `static` 方法。下文 **签名与源码一致**。

---

## `AuthUtils`

认证与时间相关辅助，无 Servlet 依赖。

| 签名 | 参数 | 返回值 / 说明 |
| --- | --- | --- |
| `String nowIso()` | 无 | 当前时刻的 **ISO-8601** 字符串（`DateTimeFormatter.ISO_INSTANT`）。 |
| `String generateSalt()` | 无 | 随机 **UUID** 字符串，作密码盐。 |
| `String sha256(String value)` | **`value`**：待哈希的明文；`null` 行为以调用方为准（内部按 UTF-8 字节摘要）。 | **十六进制小写**的 SHA-256 摘要；算法不可用时抛 **`IllegalStateException`**。 |
| `String hashPassword(String password, String salt)` | **`password`**、**`salt`**：按 **`salt + password`** 拼接后做 **`sha256`**。 | 存库的 **`passwordHash`** 常用此结果。 |

---

## `JsonUtils`

将 **`ApiResponse`**、`Map`、`List`、字符串等 **手写**为 JSON 文本字符串（**不**使用 Gson `JsonObject`）。

| 签名 | 参数 | 返回值 / 说明 |
| --- | --- | --- |
| `String quote(String value)` | 任意字符串，可为 **`null`**。 | JSON 字符串字面量（含转义）；**`null`** 参数产出四个字符 **`null`**（非带引号的 null 字符串）。 |
| `String toJson(ApiResponse<?> response)` | **`response`**：非 null。 | 形如 **`{"success":bool,"message":...,"data":...}`** 的单行 JSON；**`data`** 由 **`toJsonValue`** 递归。 |
| `String toJsonValue(Object value)` | **`value`**：任意对象。 | **`null`** → **`null`**；`String` → 带引号转义；`Number`/`Boolean` → 字面量；`Map` → 对象（键转字符串后 `quote`）；`List` / **对象数组** → 数组；其它类型 → **`String.valueOf` 后 `quote`**。 |

---

## `ServletJsonResponseWriter`

向 **`HttpServletResponse`** 写出 **`ApiResponse`** JSON。

| 签名 | 参数 | 返回值 / 说明 |
| --- | --- | --- |
| `void write(HttpServletResponse response, int status, ApiResponse<?> apiResponse)` | **`response`**：Servlet 响应；**`status`**：HTTP 状态码；**`apiResponse`**：响应体模型。 | 设置 **`status`**、**`Content-Type: application/json; charset=UTF-8`**、UTF-8 编码，正文为 **`JsonUtils.toJson(apiResponse)`**。可能 **`IOException`**。 |

---

## `GsonJsonObjectUtils`

针对 **Gson `com.google.gson.JsonObject`** 的纯函数读字段与字符串处理，**无 IO**。

| 签名 | 参数 | 返回值 |
| --- | --- | --- |
| `String trim(String value)` | 任意字符串，可为 **`null`**。 | **`null`** → **`""`**；否则 **`trim()`**。 |
| `String getAsString(JsonObject object, String key)` | **`object`**、**`key`** 可为 **`null`**。 | 缺键 / JSON null → **`""`**；否则该键的字符串。 |
| `int getAsInt(JsonObject object, String key, int fallback)` | **`fallback`**：解析失败或缺省时的默认值。 | 缺键、null、非数字 → **`fallback`**。 |
| `Integer getOptionalInt(JsonObject object, String key)` | 同上。 | 缺键、null、解析失败 → **`null`**。 |
| `String firstNonBlank(String primary, String fallback)` | 两段文本，可为 **`null`**。 | **`primary`** 经 **`trim`** 后若 **`isBlank()`** 则返回 **`trim(fallback)`**，否则返回 **`primary`** 侧 trim 结果。 |

```java
import static com.bupt.tarecruit.common.util.GsonJsonObjectUtils.firstNonBlank;
import static com.bupt.tarecruit.common.util.GsonJsonObjectUtils.getAsString;
import static com.bupt.tarecruit.common.util.GsonJsonObjectUtils.getOptionalInt;
import static com.bupt.tarecruit.common.util.GsonJsonObjectUtils.trim;
```

---

## 与 `GsonJsonObjectUtils` / `JsonUtils` 是否重复？

**不重复**：读 Gson 树用 **`GsonJsonObjectUtils`**；写 **`ApiResponse`** 用 **`JsonUtils`**（通常经 **`ServletJsonResponseWriter.write`**）。二者栈不同。

---

## 当前引用关系（便于排查）

- **`AuthUtils`**：`TaAccountDao`。
- **`GsonJsonObjectUtils`**：`RecruitmentCoursesDao`、`MoRecruitmentDao`、`TaAccountDao`（静态导入）。
- **`ServletJsonResponseWriter`**：`TaLoginServlet`、`TaRegisterServlet`、`TaProfileSettingsServlet`。
- **`JsonUtils`**：由 **`ServletJsonResponseWriter`** 调用；业务代码一般只调 **`ServletJsonResponseWriter.write`**。

新增 Servlet 返回 **`ApiResponse`** JSON 时优先复用 **`ServletJsonResponseWriter`**。
