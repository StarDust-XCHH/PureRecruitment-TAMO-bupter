# PureRecruitment-TAMO-bupter

Group-16

# 资源文件服务端部署挂载

```txt
见目录 mountDataTAMObupter/README.md
```

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


# 项目架构

```txt
PureRecruitment-TAMO-bupter/
├── pom.xml
├── README.md
├── .gitignore
├── data/
│   ├── README.md                        // 记录本地 JSON 数据存储总规范、命名约定、并发读写原则
│   ├── users.json
│   ├── jobs.json
│   ├── applications.json
│   ├── schedules.json
│   ├── interviews.json
│   ├── notices.json
│   ├── logs.json
│   ├── admin/
│   │   ├── README.md                    // 记录管理员模块专属数据文件说明、字段定义、维护策略
│   │   ├── permissions.json
│   │   ├── operation-records.json
│   │   └── dashboard-cache.json
│   ├── mo/
│   │   ├── README.md                    // 记录教师模块专属数据文件说明，如岗位发布、面试安排、录用反馈
│   │   ├── job-drafts.json
│   │   ├── interview-feedback.json
│   │   └── course-assignment.json
│   ├── ta/
│   │   ├── README.md                    // 记录助教模块专属数据文件说明，如简历草稿、申请记录、个人排班
│   │   ├── resume-drafts.json
│   │   ├── ta-preferences.json
│   │   └── availability.json
│   └── common/
│       ├── README.md                    // 记录跨模块共享 JSON 文件用途、共享字段、锁控制建议
│       ├── dictionaries.json
│       ├── enums.json
│       └── system-config.json
├── src/
│   ├── README.md                        // 记录源码目录整体结构、分层规则、开发协作约定
│   ├── main/
│   │   ├── README.md                    // 记录主程序目录说明，包括 java/resources/webapp 的职责边界
│   │   ├── java/
│   │   │   ├── README.md                // 记录后端 Java 源码组织规范与包命名约束
│   │   │   └── com/
│   │   │       ├── README.md            // 记录 com 根包用途与公司/组织命名约定
│   │   │       └── bupt/
│   │   │           ├── README.md        // 记录 bupt 包层级说明
│   │   │           └── tarecruit/
│   │   │               ├── README.md    // 记录系统总包说明、模块边界、公共规范与编码守则
│   │   │               ├── common/
│   │   │               │   ├── README.md                // 记录公共基建模块职责，避免与业务模块耦合
│   │   │               │   ├── controller/
│   │   │               │   │   ├── README.md            // 记录公共 Servlet、统一入口、健康检查、错误页跳转规范
│   │   │               │   │   ├── HealthCheckServlet.java
│   │   │               │   │   ├── AuthServlet.java
│   │   │               │   │   └── FileServlet.java
│   │   │               │   ├── service/
│   │   │               │   │   ├── README.md            // 记录公共服务逻辑，如认证、鉴权、文件处理、日志封装
│   │   │               │   │   ├── AuthService.java
│   │   │               │   │   ├── FileStorageService.java
│   │   │               │   │   └── LogService.java
│   │   │               │   ├── dao/
│   │   │               │   │   ├── README.md            // 记录公共 JSON DAO、读写锁封装、通用文件访问组件
│   │   │               │   │   ├── JsonFileDao.java
│   │   │               │   │   ├── UserDao.java
│   │   │               │   │   ├── NoticeDao.java
│   │   │               │   │   └── DictionaryDao.java
│   │   │               │   ├── model/
│   │   │               │   │   ├── README.md            // 记录跨模块通用实体定义，如 User、Notice、ApiResponse
│   │   │               │   │   ├── User.java
│   │   │               │   │   ├── Notice.java
│   │   │               │   │   ├── ApiResponse.java
│   │   │               │   │   └── PageResult.java
│   │   │               │   ├── filter/
│   │   │               │   │   ├── README.md            // 记录过滤器配置，如登录校验、角色权限、编码处理、CORS
│   │   │               │   │   ├── AuthFilter.java
│   │   │               │   │   ├── RoleFilter.java
│   │   │               │   │   └── EncodingFilter.java
│   │   │               │   ├── listener/
│   │   │               │   │   ├── README.md            // 记录监听器，如系统启动初始化、配置加载、资源释放
│   │   │               │   │   └── AppInitListener.java
│   │   │               │   ├── util/
│   │   │               │   │   ├── README.md            // 记录工具类，如 JSON 解析、日期处理、响应输出、路径解析
│   │   │               │   │   ├── JsonUtil.java
│   │   │               │   │   ├── ResponseUtil.java
│   │   │               │   │   ├── FilePathUtil.java
│   │   │               │   │   ├── DateTimeUtil.java
│   │   │               │   │   └── ValidationUtil.java
│   │   │               │   ├── constant/
│   │   │               │   │   ├── README.md            // 记录公共常量、角色枚举、状态码、文件路径常量
│   │   │               │   │   ├── RoleConstants.java
│   │   │               │   │   ├── StatusConstants.java
│   │   │               │   │   └── FileConstants.java
│   │   │               │   ├── config/
│   │   │               │   │   ├── README.md            // 记录全局配置类、初始化参数、Tomcat/Servlet 配置映射
│   │   │               │   │   └── AppConfig.java
│   │   │               │   └── exception/
│   │   │               │       ├── README.md            // 记录系统自定义异常定义与统一异常处理约定
│   │   │               │       ├── BusinessException.java
│   │   │               │       └── JsonStorageException.java
│   │   │               ├── ta/
│   │   │               │   ├── README.md                // 记录助教模块职责边界、接口清单、前后端协作契约
│   │   │               │   ├── controller/
│   │   │               │   │   ├── README.md            // 记录助教端 Servlet 路由：注册、登录、查看岗位、投递、进度查询
│   │   │               │   │   ├── TaAuthServlet.java
│   │   │               │   │   ├── TaProfileServlet.java
│   │   │               │   │   ├── TaJobServlet.java
│   │   │               │   │   ├── TaApplicationServlet.java
│   │   │               │   │   └── TaScheduleServlet.java
│   │   │               │   ├── service/
│   │   │               │   │   ├── README.md            // 记录助教模块业务逻辑：投递流程、状态流转、个人资料维护
│   │   │               │   │   ├── TaAuthService.java
│   │   │               │   │   ├── TaProfileService.java
│   │   │               │   │   ├── TaJobBrowseService.java
│   │   │               │   │   ├── TaApplicationService.java
│   │   │               │   │   └── TaScheduleService.java
│   │   │               │   ├── dao/
│   │   │               │   │   ├── README.md            // 记录助教模块 DAO：简历草稿、申请记录、意向时间段 JSON 读写
│   │   │               │   │   ├── TaProfileDao.java
│   │   │               │   │   ├── TaResumeDao.java
│   │   │               │   │   ├── TaApplicationDao.java
│   │   │               │   │   └── TaAvailabilityDao.java
│   │   │               │   └── model/
│   │   │               │       ├── README.md            // 记录助教实体：TAProfile、TAResume、TAApplication、Availability
│   │   │               │       ├── TaProfile.java
│   │   │               │       ├── TaResume.java
│   │   │               │       ├── TaApplication.java
│   │   │               │       └── Availability.java
│   │   │               ├── mo/
│   │   │               │   ├── README.md                // 记录教师模块职责边界、岗位管理流程、筛选与面试业务约束
│   │   │               │   ├── controller/
│   │   │               │   │   ├── README.md            // 记录教师端 Servlet 路由：发布岗位、筛选申请、安排面试、反馈结果
│   │   │               │   │   ├── MoAuthServlet.java
│   │   │               │   │   ├── MoJobServlet.java
│   │   │               │   │   ├── MoApplicationReviewServlet.java
│   │   │               │   │   ├── MoInterviewServlet.java
│   │   │               │   │   └── MoFeedbackServlet.java
│   │   │               │   ├── service/
│   │   │               │   │   ├── README.md            // 记录教师模块业务逻辑：岗位生命周期、筛选决策、面试安排
│   │   │               │   │   ├── MoAuthService.java
│   │   │               │   │   ├── MoJobService.java
│   │   │               │   │   ├── MoApplicationReviewService.java
│   │   │               │   │   ├── MoInterviewService.java
│   │   │               │   │   └── MoFeedbackService.java
│   │   │               │   ├── dao/
│   │   │               │   │   ├── README.md            // 记录教师模块 DAO：岗位、筛选结果、面试反馈 JSON 读写
│   │   │               │   │   ├── MoJobDao.java
│   │   │               │   │   ├── MoJobDraftDao.java
│   │   │               │   │   ├── MoInterviewDao.java
│   │   │               │   │   └── MoFeedbackDao.java
│   │   │               │   └── model/
│   │   │               │       ├── README.md            // 记录教师实体：Job、Interview、Feedback、CourseAssignment
│   │   │               │       ├── Job.java
│   │   │               │       ├── Interview.java
│   │   │               │       ├── InterviewFeedback.java
│   │   │               │       └── CourseAssignment.java
│   │   │               └── admin/
│   │   │                   ├── README.md                // 记录管理员模块职责边界、系统治理规则、运维/审计接口规范
│   │   │                   ├── controller/
│   │   │                   │   ├── README.md            // 记录管理员 Servlet 路由：用户管理、公告管理、统计分析、权限控制
│   │   │                   │   ├── AdminAuthServlet.java
│   │   │                   │   ├── AdminUserServlet.java
│   │   │                   │   ├── AdminNoticeServlet.java
│   │   │                   │   ├── AdminDashboardServlet.java
│   │   │                   │   └── AdminPermissionServlet.java
│   │   │                   ├── service/
│   │   │                   │   ├── README.md            // 记录管理员业务逻辑：账号审核、公告发布、统计汇总、权限分配
│   │   │                   │   ├── AdminAuthService.java
│   │   │                   │   ├── AdminUserService.java
│   │   │                   │   ├── AdminNoticeService.java
│   │   │                   │   ├── AdminDashboardService.java
│   │   │                   │   └── AdminPermissionService.java
│   │   │                   ├── dao/
│   │   │                   │   ├── README.md            // 记录管理员模块 DAO：权限、操作日志、看板缓存 JSON 读写
│   │   │                   │   ├── AdminUserDao.java
│   │   │                   │   ├── AdminPermissionDao.java
│   │   │                   │   ├── AdminOperationLogDao.java
│   │   │                   │   └── AdminDashboardDao.java
│   │   │                   └── model/
│   │   │                       ├── README.md            // 记录管理员实体：AdminUser、Permission、OperationLog、DashboardMetrics
│   │   │                       ├── AdminUser.java
│   │   │                       ├── Permission.java
│   │   │                       ├── OperationLog.java
│   │   │                       └── DashboardMetrics.java
│   │   ├── resources/
│   │   │   ├── README.md                // 记录资源目录用途，如配置文件、日志模板、初始化数据模板
│   │   │   ├── app.properties
│   │   │   ├── log-config.properties
│   │   │   └── templates/
│   │   │       ├── README.md            // 记录初始化 JSON 模板、导入导出模板文件说明
│   │   │       ├── users.template.json
│   │   │       ├── jobs.template.json
│   │   │       └── applications.template.json
│   │   └── webapp/
│   │       ├── README.md                // 记录前端资源组织方式、访问路径规划、静态资源协作规范
│   │       ├── index.html
│   │       ├── favicon.ico
│   │       ├── WEB-INF/
│   │       │   ├── README.md            // 记录 web.xml、Tomcat 部署描述、Servlet 映射与安全配置
│   │       │   └── web.xml
│   │       ├── pages/
│   │       │   ├── README.md            // 记录业务页面总目录规范，要求与后端模块一一镜像对应
│   │       │   ├── common/
│   │       │   │   ├── README.md        // 记录公共页面片段，如登录页、错误页、通用弹窗页
│   │       │   │   ├── login.html
│   │       │   │   ├── 403.html
│   │       │   │   ├── 404.html
│   │       │   │   └── 500.html
│   │       │   ├── ta/
│   │       │   │   ├── README.md        // 记录助教端页面：首页、岗位列表、申请记录、个人中心
│   │       │   │   ├── dashboard.html
│   │       │   │   ├── jobs.html
│   │       │   │   ├── application-list.html
│   │       │   │   ├── profile.html
│   │       │   │   └── schedule.html
│   │       │   ├── mo/
│   │       │   │   ├── README.md        // 记录教师端页面：岗位管理、申请筛选、面试安排、结果反馈
│   │       │   │   ├── dashboard.html
│   │       │   │   ├── job-manage.html
│   │       │   │   ├── application-review.html
│   │       │   │   ├── interview-manage.html
│   │       │   │   └── feedback.html
│   │       │   └── admin/
│   │       │       ├── README.md        // 记录管理员页面：用户管理、公告管理、数据看板、权限维护
│   │       │       ├── dashboard.html
│   │       │       ├── user-manage.html
│   │       │       ├── notice-manage.html
│   │       │       ├── permission-manage.html
│   │       │       └── analytics.html
│   │       ├── assets/
│   │       │   ├── README.md            // 记录静态资源总目录说明及公共/私有资源划分原则
│   │       │   ├── common/
│   │       │   │   ├── README.md        // 记录所有模块共享静态资源，如全局样式、请求封装、工具函数
│   │       │   │   ├── css/
│   │       │   │   │   ├── README.md    // 记录全局 CSS，如 reset、layout、theme、component 规范
│   │       │   │   │   ├── reset.css
│   │       │   │   │   ├── global.css
│   │       │   │   │   ├── layout.css
│   │       │   │   │   └── components.css
│   │       │   │   ├── js/
│   │       │   │   │   ├── README.md    // 记录公共 JS，如 API 请求、鉴权、本地存储、表单校验
│   │       │   │   │   ├── api-request.js
│   │       │   │   │   ├── auth.js
│   │       │   │   │   ├── storage.js
│   │       │   │   │   ├── validator.js
│   │       │   │   │   └── date-format.js
│   │       │   │   └── images/
│   │       │   │       ├── README.md    // 记录共享图片、logo、图标资源命名规范
│   │       │   │       ├── logo.png
│   │       │   │       └── empty-state.svg
│   │       │   ├── ta/
│   │       │   │   ├── README.md        // 记录助教端私有静态资源，避免与教师/管理员前端互相干扰
│   │       │   │   ├── css/
│   │       │   │   │   ├── README.md    // 记录助教页面私有样式，如 jobs/profile/schedule 页面样式
│   │       │   │   │   ├── ta-dashboard.css
│   │       │   │   │   ├── ta-jobs.css
│   │       │   │   │   ├── ta-profile.css
│   │       │   │   │   └── ta-schedule.css
│   │       │   │   ├── js/
│   │       │   │   │   ├── README.md    // 记录助教页面私有脚本，如岗位浏览、投递操作、状态跟踪
│   │       │   │   │   ├── ta-dashboard.js
│   │       │   │   │   ├── ta-jobs.js
│   │       │   │   │   ├── ta-application.js
│   │       │   │   │   ├── ta-profile.js
│   │       │   │   │   └── ta-schedule.js
│   │       │   │   └── images/
│   │       │   │       ├── README.md    // 记录助教端私有图片资源与插画
│   │       │   │       └── ta-banner.png
│   │       │   ├── mo/
│   │       │   │   ├── README.md        // 记录教师端私有静态资源，服务于岗位与面试管理页面
│   │       │   │   ├── css/
│   │       │   │   │   ├── README.md    // 记录教师页面私有样式
│   │       │   │   │   ├── mo-dashboard.css
│   │       │   │   │   ├── mo-job-manage.css
│   │       │   │   │   ├── mo-review.css
│   │       │   │   │   └── mo-interview.css
│   │       │   │   ├── js/
│   │       │   │   │   ├── README.md    // 记录教师页面私有脚本，如岗位发布、筛选申请、面试反馈
│   │       │   │   │   ├── mo-dashboard.js
│   │       │   │   │   ├── mo-job-manage.js
│   │       │   │   │   ├── mo-review.js
│   │       │   │   │   ├── mo-interview.js
│   │       │   │   │   └── mo-feedback.js
│   │       │   │   └── images/
│   │       │   │       ├── README.md    // 记录教师端私有图片资源
│   │       │   │       └── mo-banner.png
│   │       │   └── admin/
│   │       │       ├── README.md        // 记录管理员端私有静态资源，服务于治理、统计、公告、权限页面
│   │       │       ├── css/
│   │       │       │   ├── README.md    // 记录管理员页面私有样式
│   │       │       │   ├── admin-dashboard.css
│   │       │       │   ├── admin-user-manage.css
│   │       │       │   ├── admin-notice.css
│   │       │       │   └── admin-analytics.css
│   │       │       ├── js/
│   │       │       │   ├── README.md    // 记录管理员页面私有脚本，如用户治理、权限控制、统计看板
│   │       │       │   ├── admin-dashboard.js
│   │       │       │   ├── admin-user-manage.js
│   │       │       │   ├── admin-notice.js
│   │       │       │   ├── admin-permission.js
│   │       │       │   └── admin-analytics.js
│   │       │       └── images/
│   │       │           ├── README.md    // 记录管理员端私有图片资源
│   │       │           └── admin-banner.png
│   │       └── uploads/
│   │           ├── README.md            // 记录上传文件目录规范，如简历附件、头像、导出报表
│   │           ├── resumes/
│   │           │   ├── README.md        // 记录助教上传简历文件的命名规则、大小限制、清理策略
│   │           │   └── .gitkeep
│   │           ├── avatars/
│   │           │   ├── README.md        // 记录用户头像上传规则与默认头像策略
│   │           │   └── .gitkeep
│   │           └── exports/
│   │               ├── README.md        // 记录管理员导出报表文件的格式与生命周期管理
│   │               └── .gitkeep
│   └── test/
│       ├── README.md                    // 记录测试目录整体组织规范、命名规则、覆盖范围
│       ├── java/
│       │   ├── README.md                // 记录后端测试代码说明
│       │   └── com/
│       │       ├── README.md            // 记录测试代码 com 包说明
│       │       └── bupt/
│       │           ├── README.md        // 记录测试代码 bupt 包说明
│       │           └── tarecruit/
│       │               ├── README.md    // 记录测试总包说明与测试策略
│       │               ├── common/
│       │               │   ├── README.md                // 记录公共模块测试说明
│       │               │   ├── service/
│       │               │   │   ├── README.md            // 记录公共服务测试用例说明
│       │               │   │   └── AuthServiceTest.java
│       │               │   └── dao/
│       │               │       ├── README.md            // 记录公共 DAO 测试与 JSON 文件隔离策略
│       │               │       └── JsonFileDaoTest.java
│       │               ├── ta/
│       │               │   ├── README.md                // 记录助教模块测试范围与用例边界
│       │               │   ├── service/
│       │               │   │   ├── README.md            // 记录助教业务测试用例
│       │               │   │   └── TaApplicationServiceTest.java
│       │               │   └── dao/
│       │               │       ├── README.md            // 记录助教 DAO 测试说明
│       │               │       └── TaApplicationDaoTest.java
│       │               ├── mo/
│       │               │   ├── README.md                // 记录教师模块测试范围与用例边界
│       │               │   ├── service/
│       │               │   │   ├── README.md            // 记录教师业务测试用例
│       │               │   │   └── MoInterviewServiceTest.java
│       │               │   └── dao/
│       │               │       ├── README.md            // 记录教师 DAO 测试说明
│       │               │       └── MoJobDaoTest.java
│       │               └── admin/
│       │                   ├── README.md                // 记录管理员模块测试范围与用例边界
│       │                   ├── service/
│       │                   │   ├── README.md            // 记录管理员业务测试用例
│       │                   │   └── AdminDashboardServiceTest.java
│       │                   └── dao/
│       │                       ├── README.md            // 记录管理员 DAO 测试说明
│       │                       └── AdminPermissionDaoTest.java
│       └── resources/
│           ├── README.md                // 记录测试资源目录用途，如测试 JSON 样本、Mock 配置
│           ├── mock-json/
│           │   ├── README.md            // 记录测试用 JSON 样本说明，避免污染正式 data 目录
│           │   ├── users.test.json
│           │   ├── jobs.test.json
│           │   └── applications.test.json
│           └── fixtures/
│               ├── README.md            // 记录测试夹具、接口响应样本、边界场景输入
│               └── ta-application-fixture.json
└── docs/
    ├── README.md                        // 记录项目文档总入口、迭代文档索引与协作规范
    ├── api/
    │   ├── README.md                    // 记录接口文档规范、URL 设计、请求响应示例
    │   ├── common-api.md
    │   ├── ta-api.md
    │   ├── mo-api.md
    │   └── admin-api.md
    ├── database/
    │   ├── README.md                    // 记录 JSON 数据结构设计、字段字典、版本迁移策略
    │   ├── users-schema.md
    │   ├── jobs-schema.md
    │   └── applications-schema.md
    ├── frontend/
    │   ├── README.md                    // 记录前端页面结构、资源依赖图、交互规范
    │   ├── page-routing.md
    │   └── ui-guideline.md
    ├── backend/
    │   ├── README.md                    // 记录后端分层设计、Servlet 路由规范、异常处理约定
    │   ├── package-convention.md
    │   └── servlet-mapping.md
    └── sprint/
        ├── README.md                    // 记录敏捷迭代安排、任务拆分、里程碑与验收标准
        ├── sprint-1.md
        ├── sprint-2.md
        └── sprint-3.md

```


