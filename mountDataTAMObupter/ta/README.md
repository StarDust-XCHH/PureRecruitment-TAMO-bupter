# TA 数据目录说明

## 本次移植记录

- 新增 TA 认证数据文件 [`tas.json`](mountDataTAMObupter/ta/tas.json)、[`profiles.json`](mountDataTAMObupter/ta/profiles.json)、[`settings.json`](mountDataTAMObupter/ta/settings.json)
- 登录接口 [`TaLoginServlet.java`](src/main/java/com/bupt/tarecruit/ta/controller/TaLoginServlet.java) 会读取并更新 [`tas.json`](mountDataTAMObupter/ta/tas.json) 中的登录时间与失败次数
- 注册接口 [`TaRegisterServlet.java`](src/main/java/com/bupt/tarecruit/ta/controller/TaRegisterServlet.java) 会同时初始化 TA 主账号、Profile、Settings 三类数据
- 注册数据落盘逻辑位于 [`TaAccountDao.java`](src/main/java/com/bupt/tarecruit/ta/dao/TaAccountDao.java)
- 当前 [`TaAccountDao.register()`](src/main/java/com/bupt/tarecruit/ta/dao/TaAccountDao.java:84) 生成的 [`profiles.json`](mountDataTAMObupter/ta/profiles.json) 与 [`settings.json`](mountDataTAMObupter/ta/settings.json) 记录格式如下：
  - [`profiles.json`](mountDataTAMObupter/ta/profiles.json) 使用根对象 `{ meta, items }`，其中 `meta.schema = "profile"`、`meta.entity = "profiles"`，`items` 中每条记录包含 `id`、`taId`、`avatar`、`realName`、`applicationIntent`、`studentId`、`contactEmail`、`bio`、`skills`、`availabilityHoursPerWeek`、`semester`、`title`、`lastUpdatedAt`
  - [`settings.json`](mountDataTAMObupter/ta/settings.json) 使用根对象 `{ meta, items }`，其中 `meta.schema = "setting"`、`meta.entity = "settings"`，`items` 中每条记录包含 `id`、`taId`、`theme`、`onboardingStep`、`guideCompleted`、`createdAt`

## 待开发与当前限制

- 由于头像上传流程尚未成功实现，当前注册阶段仅在 [`TaAccountDao.register()`](src/main/java/com/bupt/tarecruit/ta/dao/TaAccountDao.java:84) 中为 `avatar` 写入空字符串，后续需要补充文件上传接口、存储路径校验、文件类型与大小限制，以及上传成功后的路径回填逻辑
- 由于首次登录引导卡片尚未成功实现，当前注册阶段仅在 [`settings.json`](mountDataTAMObupter/ta/settings.json) 中写入默认值 `onboardingStep = 0` 与 `guideCompleted = false`，后续需要在首次登录时依据这两个字段驱动前端引导状态流转，并在用户完成引导后落盘更新
- 若后续需要完全对齐你提供的示例数据，应将 [`TaAccountDao.java`](src/main/java/com/bupt/tarecruit/ta/dao/TaAccountDao.java) 中的常量从 `PROFILE_SCHEMA = "ta"`、`SETTINGS_SCHEMA = "ta"` 分别调整为 `"profile"`、`"setting"`，以确保新写入文件的 `meta.schema` 与目标格式一致
- 在头像上传与首次登录引导能力未完成前，建议保留当前后端基础结构，仅在文档层明确说明“字段结构已预留、实际业务生成逻辑待补齐”，避免误导为功能已完整可用
