# PureRecruitment-TAMO-bupter

# Group-16 Members 成员
|Github UserName|Chinese Name|Name in English|BUPT ID|QM ID|Email|
|---|---|---|---|---|---|
|StarDust-XCHH|管宇涵|Yuhan Guan|2023213116|231220725|lmtgyh@163.com|
|zfh53|张峰豪|Fenghao Zhang|2023213136|231221216|zhang.fenghao@qq.com|
|6a696c6c|李玉峰|Yufeng Li|2023213131|231222556|3171506351@qq.com|
|StellaWang309|汪以琳|Yilin Wang|2023213143|231222305|1938508113@qq.com|
|TTslmy|刘慧颖|Huiying Liu|2023213172|231221788|2497604794@qq.com|
|Au2789|庞博文|Bowen Pang|2023213124|231220998|pbw3319@163.com|


- Yuxuan:yuxuanwwang@outlook.com    (Support TA)



# 资源文件服务端部署挂载

部署与目录结构说明见 [`mountDataTAMObupter/README.md`](mountDataTAMObupter/README.md)（环境变量 `mountDataTAMObupter`、子目录索引）。

# 文档与岗位数据（开发说明）

- **本地 Tomcat / GitHub**：[`docs/Github操作指南+Tomcat配置.docx`](docs/Github操作指南+Tomcat配置.docx)。
- **MO 模块开发日志**：按时间条目记录 MO 界面与后端进展，持续追加，见 [`docs/log/MO-module-development-log.md`](docs/log/MO-module-development-log.md)。
- **招聘岗位 API 与契约**：[`docs/api/mo-job-board-api-v2.md`](docs/api/mo-job-board-api-v2.md)；MO/TA 数据交互说明见 [`docs/backend/mo-ta-interaction-log.md`](docs/backend/mo-ta-interaction-log.md)（含 MO 申请人列表、详情、已读/未读、评论、简历下载、录用/拒绝等接口与 JSON 路径）。
- **岗位主数据文件**：[`mountDataTAMObupter/common/recruitment-courses.json`](mountDataTAMObupter/common/recruitment-courses.json)，由 `com.bupt.tarecruit.common.dao.RecruitmentCoursesDao` 集中管理路径、锁与读写（MO 与 TA 岗位列表同源读取）；公共包与 `common.dao` 说明见 [`src/main/java/com/bupt/tarecruit/common/README.md`](src/main/java/com/bupt/tarecruit/common/README.md) 与 [`src/main/java/com/bupt/tarecruit/common/dao/README.md`](src/main/java/com/bupt/tarecruit/common/dao/README.md)。挂载目录旁补充说明见 [`mountDataTAMObupter/common/recruitment-courses-dao-notes.md`](mountDataTAMObupter/common/recruitment-courses-dao-notes.md)。

# 一阶段任务要求

```txt
见目录 docs/sprint/sprint-1.md
```


一阶段分工如下：
1. 头脑风暴：所有人按照https://qmplus.qmul.ac.uk/mod/resource/view.php?id=2522133文件要求，提交至少三条用户视角的头脑风暴（可以参考课程slide进行书写，务必注意不同需求的区分；必须包含包括验收标准、优先级、估算及迭代计划；具体要求参见文档和课程slide）
2. 原型设计：（可以是程序，可以是ppt，可以是墨刀）
   1. 管宇涵，完成TA界面的原型设计，包含登录界面
   2. 张峰豪完成MO界面的原型设计
   3. 李玉峰完成admin部分的原型设计
3. 简报内容：在张峰豪的帮助下，其余人员完成项目阶段性简报，具体要求见pdf文档


文案内容请上传到独属于个人分支的https://github.com/StarDust-XCHH/PureRecruitment-TAMO-bupter/blob/e68036ff47aa010577c8fe5cc642046171900416/peojectFile文件夹（推荐新建子文件夹）


## 一阶段完成内容

- TA+LogOn=story+prototype<——StarDust——>`projectFile/StarDustXCHH/imgStage1Prototype`



