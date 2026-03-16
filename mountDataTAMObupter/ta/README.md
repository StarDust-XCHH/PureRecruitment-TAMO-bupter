# TA 数据目录说明

## 本次移植记录

- 新增 TA 认证数据文件 [`tas.json`](mountDataTAMObupter/ta/tas.json)、[`profiles.json`](mountDataTAMObupter/ta/profiles.json)、[`settings.json`](mountDataTAMObupter/ta/settings.json)
- 登录接口 [`TaLoginServlet.java`](src/main/java/com/bupt/tarecruit/ta/controller/TaLoginServlet.java) 会读取并更新 [`tas.json`](mountDataTAMObupter/ta/tas.json) 中的登录时间与失败次数
- 注册接口 [`TaRegisterServlet.java`](src/main/java/com/bupt/tarecruit/ta/controller/TaRegisterServlet.java) 会同时初始化 TA 主账号、Profile、Settings 三类数据
- 注册数据落盘逻辑位于 [`TaAccountDao.java`](src/main/java/com/bupt/tarecruit/ta/dao/TaAccountDao.java)
- 当前默认空数组初始化，首次注册后会自动生成完整结构
