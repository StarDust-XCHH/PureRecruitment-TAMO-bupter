# PureRecruitment-TAMO-bupter

- 保护性推送 2026年4月11日 16点39分

[![Version](https://img.shields.io/badge/version-v1.0.0_(Pre--release)-orange.svg)](https://github.com/StarDust-XCHH/PureRecruitment-TAMO-bupter/releases/tag/v1.0.0)

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


# 软件发布相关


太棒了！选择“猫头鹰与鸟类”作为主题不仅极具学术气息（完美契合高校教务与助教招募的背景），而且自然界中鸟类种类繁多，足够支撑你走完 A 到 Z 的漫长版本迭代。

为了符合 ROS 2 **“形容词 (Adjective) + 动物名 (Noun)”** 的极客命名法，并结合 **语义化版本控制 (SemVer)**，我为你制定了这套完整的 A-Z 命名蓝图与发布规范。

---

### 一、 版本生命周期示例（如何区分测试版与正式版）

在 ROS 2 和标准开源项目中，**代号是不变的，状态是通过版本号后缀来区分的**。我们以首发版本 **A** 为例（代号：Astute Athene 敏锐的小鸮）：

* **内部测试版 (Alpha):** 核心功能刚跑通，给开发组成员自己联调。
   * 🏷️ 发布名称：`Astute Athene - v1.0.0-alpha.1`
* **公开测试版 (Beta):** 准备好让少数外部用户（如部分 TA 或 MO）试用，收集 Bug。
   * 🏷️ 发布名称：`Astute Athene - v1.0.0-beta.1`
* **候选发布版 (RC):** 代码冻结，如果没有发现致命 Bug，这就是正式版。
   * 🏷️ 发布名称：`Astute Athene - v1.0.0-rc.1`
* **正式发布版 (GA):** 稳定版本，面向全员公开。
   * 🏷️ 发布名称：`Astute Athene - v1.0.0`

---

### 二、 从 A 到 Z 的完整命名蓝图 (The Aviary Roadmap)

在这个列表中，我以**猫头鹰（鸮类）**和**猛禽/智慧鸟类**为主，挑选了寓意好、适合形容软件系统的形容词。

#### **【奠基阶段：A - E】**
* **A版本 (v1.0.0): Astute Athene** (敏锐的小鸮)
   * *寓意：像智慧女神雅典娜的爱鸟一样，敏锐地启动整个招募系统。*
* **B版本 (v2.0.0): Brilliant Bubo** (卓越的雕鸮)
   * *寓意：雕鸮体型庞大，代表系统承载能力和架构的卓越升级。*
* **C版本 (v3.0.0): Candid Corvus** (坦诚的渡鸦)
   * *寓意：渡鸦智商极高，代表系统 AI 助手逻辑变得更加聪明和透明。*
* **D版本 (v4.0.0): Dashing Diomedea** (潇洒的信天翁)
   * *寓意：信天翁善于长途飞行，代表项目进入长期支持（LTS）或性能飞跃。*
* **E版本 (v5.0.0): Eloquent Eagle** (雄辩的雄鹰)
   * *寓意：系统交互与 AI 表达能力达到炉火纯青的阶段。*

#### **【进阶阶段：F - J】**
* **F版本 (v6.0.0): Fearless Falcon** (无畏的猎鹰)
   * *寓意：猎鹰俯冲速度极快，代表系统响应速度和检索能力的突破。*
* **G版本 (v7.0.0): Graceful Glaucidium** (优雅的鸺鹠 / 一种微型猫头鹰)
   * *寓意：UI/UX 迎来重大重构，界面变得极其优雅且轻量化。*
* **H版本 (v8.0.0): Hardy Hawk** (坚韧的苍鹰)
   * *寓意：系统稳定性极强，能够抵御高并发和复杂错误。*
* **I版本 (v9.0.0): Insightful Ibis** (洞察的朱鹮)
   * *寓意：加入深度数据分析功能，能够洞察招募数据的潜在规律。*
* **J版本 (v10.0.0): Jubilant Jay** (喜悦的松鸦)
   * *寓意：第十个大版本的里程碑，一个充满成就感和活力的版本。*

#### **【成熟阶段：K - O】**
* **K版本 (v11.0.0): Keen Kestrel** (敏锐的红隼)
* **L版本 (v12.0.0): Lucid Lark** (明晰的云雀)
* **M版本 (v13.0.0): Mighty Megascops** (强大的鸣角鸮)
* **N版本 (v14.0.0): Noble Nyctea** (高贵的雪鸮) —— *（极其漂亮的猛禽，适合作为重要的 LTS 版本）*
* **O版本 (v15.0.0): Outstanding Osprey** (杰出的鱼鹰)

#### **【扩展阶段：P - T】**
* **P版本 (v16.0.0): Proud Ptilopsis** (骄傲的白脸鸮)
* **Q版本 (v17.0.0): Quick Quetzal** (敏捷的绿咬鹃) —— *（中美洲神鸟，适合引入全新前端框架的版本）*
* **R版本 (v18.0.0): Resilient Raven** (充满韧性的渡鸦)
* **S版本 (v19.0.0): Swift Strix** (迅捷的林鸮)
* **T版本 (v20.0.0): Tenacious Tyto** (顽强的仓鸮 / 猴面鹰)

#### **【终极图鉴：U - Z】**
* **U版本 (v21.0.0): Unique Upupa** (独特的戴胜)
* **V版本 (v22.0.0): Valiant Vulture** (英勇的秃鹫) —— *（擅长清理，适合作为深度优化、清理历史债务重构的版本）*
* **W版本 (v23.0.0): Wise Woodpecker** (智慧的啄木鸟) —— *（精准捕捉代码 Bug 和系统漏洞）*
* **X版本 (v24.0.0): Xenial Xema** (友好的叉尾鸥) —— *（Xenial 意为好客的，适合强化开放 API 与第三方集成的版本）*
* **Y版本 (v25.0.0): Youthful Yuhina** (充满活力的凤鹛)
* **Z版本 (v26.0.0): Zealous Zosterops** (热情的绣眼鸟) —— *（Z 字母的完美收官，一个热情洋溢的终极进化版）*

### 💡 极客建议

当你把你的项目打成 WAR 包时，可以在首页的 Footer（页脚）或者控制台启动日志里，悄悄打出一行字：
> `System booting... Powered by PureRecruitment v1.0.0 (Astute Athene)`

这会给每一个审阅你代码的老师或使用你系统的同学一种“这不只是个大作业，这是一个正规软件工程产品”的震撼感。第一步，准备好发布你的 **Astute Athene v1.0.0-beta.1** 了吗？

