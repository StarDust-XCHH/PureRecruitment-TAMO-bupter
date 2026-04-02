# `common.filter` · Servlet 过滤器

上级包说明见 **[`../README.md`](../README.md)**。

---

## `EncodingFilter`

实现 **`jakarta.servlet.Filter`**。

| 项 | 说明 |
| --- | --- |
| **注解** | `@WebFilter(filterName = "encodingFilter", urlPatterns = "/*")` |
| **覆盖方法** | 仅 **`doFilter`**；未自定义 **`init` / `destroy`**。 |
| **行为** | **`request.setCharacterEncoding(UTF-8)`**、**`response.setCharacterEncoding(UTF-8)`**，然后 **`chain.doFilter`**。 |
| **不设置** | **`Content-Type`**；JSON 响应由 Servlet 或 **`ServletJsonResponseWriter`** 设置。 |
