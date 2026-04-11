# PureRecruitment-TAMO-bupter Public Release 使用说明

本目录用于存放面向公开 release 的部署与交付文档，适用于当前项目的 **Tomcat + WAR + 外部挂载数据目录** 发布方式。

相关运行时事实可在代码中直接看到：

- 数据根目录通过环境变量 [`mountDataTAMObupter`](../../src/main/java/com/bupt/tarecruit/common/config/DataMountPaths.java:16) 解析，代码入口见 [`DataMountPaths`](../../src/main/java/com/bupt/tarecruit/common/config/DataMountPaths.java:14)
- AI 助理密钥通过环境变量 [`TONGYI_API_KEY`](../../src/main/java/com/bupt/tarecruit/ta/service/TaAiAssistantService.java:385) 读取，读取逻辑见 [`readApiKey()`](../../src/main/java/com/bupt/tarecruit/ta/service/TaAiAssistantService.java:575)
- 若未设置数据目录环境变量，程序会回退到仓库内默认目录 [`mountDataTAMObupter/`](../../mountDataTAMObupter/README.md)

---

## 1. 本次公开 release 包含什么

推荐发布时至少包含以下内容：

```text
release/
├─ app/
│  └─ PureRecruitment-TAMO-bupter.war
├─ data/
│  └─ mountDataTAMObupter/
├─ docs/
│  └─ releaseReadme/
│     ├─ RELEASE-README.md
│     ├─ ENVIRONMENT-SETUP.md
│     ├─ DATASET-README.md
│     └─ DEPLOY-ON-TOMCAT.md
└─ source/
   └─ PureRecruitment-TAMO-bupter-src.zip
```

各部分职责如下：

- [`app/`](../../pom.xml) ：放置部署到 Tomcat 的 WAR 包
- [`data/`](../../mountDataTAMObupter/README.md) ：放置测试数据目录，供运行时挂载使用
- [`docs/releaseReadme/`](../releaseReadme/RELEASE-README.md) ：放置 release 使用文档
- [`source/`](../../README.md) ：放置可选源码压缩包

---

## 2. 下载者最少需要拿到哪些文件

如仅需部署并体验系统，至少需要以下三类资源：

1. WAR 包
2. 测试数据包 [`mountDataTAMObupter/`](../../mountDataTAMObupter/README.md)
3. 本文档目录下的 4 份说明文档

如果只下载 WAR 而没有数据包，系统将缺少岗位、账户、申请、简历、AI 附件目录等运行所需资源。

---

## 3. 最快启动流程

### 第一步：准备 Tomcat

确保本机已安装可正常运行的 Tomcat，并知道 `webapps`、`bin` 等目录位置。

### 第二步：解压测试数据目录

将 release 中的数据包解压到本地某个固定路径，例如：

```text
D:\demo\mountDataTAMObupter
```

### 第三步：配置环境变量 [`mountDataTAMObupter`](../../src/main/java/com/bupt/tarecruit/common/config/DataMountPaths.java:16)

将该环境变量设置为上一步数据目录的绝对路径。

### 第四步：如需 AI 功能，配置 [`TONGYI_API_KEY`](../../src/main/java/com/bupt/tarecruit/ta/service/TaAiAssistantService.java:385)

若不配置该变量，AI 功能会被禁用，但普通业务功能仍可继续使用，相关禁用提示逻辑见 [`TaAiAssistantService`](../../src/main/java/com/bupt/tarecruit/ta/service/TaAiAssistantService.java:398)。

### 第五步：部署 WAR 到 Tomcat

将 WAR 包复制到 Tomcat 的 `webapps` 目录。

### 第六步：启动 Tomcat 并查看日志

应用启动时会打印数据挂载信息，日志入口见 [`DataMountStartupListener.contextInitialized()`](../../src/main/java/com/bupt/tarecruit/common/listener/DataMountStartupListener.java:16)。

你应能在控制台中看到类似输出：

```text
[data-mount] root=... fromEnvironment=true env=mountDataTAMObupter
```

### 第七步：访问系统并验证功能

建议至少验证：

- 首页是否可访问
- TA / MO 登录是否正常
- 岗位列表是否可加载
- 测试简历是否可下载
- 若已设置 [`TONGYI_API_KEY`](../../src/main/java/com/bupt/tarecruit/ta/service/TaAiAssistantService.java:385)，AI 助理是否可用

---

## 4. 文档导航

本目录中的 4 份文档分别解决不同问题：

- [`RELEASE-README.md`](./RELEASE-README.md) ：总入口，说明 release 架构和最短启动路径
- [`ENVIRONMENT-SETUP.md`](./ENVIRONMENT-SETUP.md) ：说明环境变量怎么配置
- [`DATASET-README.md`](./DATASET-README.md) ：说明测试数据目录内容和运行期会写入哪些目录
- [`DEPLOY-ON-TOMCAT.md`](./DEPLOY-ON-TOMCAT.md) ：说明如何把 WAR 部署到 Tomcat 并验证启动

---

## 5. 公开 release 的安全边界

当前公开 release 的边界建议如下：

### 可公开发布

- WAR 包
- 测试数据目录 [`mountDataTAMObupter/`](../../mountDataTAMObupter/README.md)
- 源码压缩包
- 部署文档和说明文档

### 不应公开发布

- 任何真实可用的 [`TONGYI_API_KEY`](../../src/main/java/com/bupt/tarecruit/ta/service/TaAiAssistantService.java:385) 值
- 任何真实生产环境的私有数据
- 仅供开发过程使用且与运行无关的本地中间文件

---

## 6. 推荐的 GitHub Release 附件清单

建议在 GitHub Release 页面至少上传：

1. `PureRecruitment-TAMO-bupter.war`
2. `mountDataTAMObupter-sample.zip`
3. `PureRecruitment-TAMO-bupter-release-docs.zip`
4. `PureRecruitment-TAMO-bupter-src.zip`

推荐在 Release 描述中同步说明：

- 数据目录通过 [`mountDataTAMObupter`](../../src/main/java/com/bupt/tarecruit/common/config/DataMountPaths.java:16) 指定
- AI 密钥通过 [`TONGYI_API_KEY`](../../src/main/java/com/bupt/tarecruit/ta/service/TaAiAssistantService.java:385) 指定
- 未内置任何真实 API Key
- 附带的数据仅为测试数据

---

## 7. 发布者自己的打包检查单

正式发布前，建议按以下顺序自查：

- [ ] WAR 已成功构建
- [ ] [`mountDataTAMObupter/`](../../mountDataTAMObupter/README.md) 内容完整
- [ ] 测试简历、图片、JSON 均可公开
- [ ] 文档中的环境变量名与代码一致
- [ ] 文档中的目录结构与 release 实际结构一致
- [ ] 未在任何文档、代码、数据中写入真实 API Key
- [ ] GitHub Release 描述已标出 AI 功能需要自行配置密钥

---

## 8. 建议阅读顺序

对于第一次接触该项目的使用者，建议按以下顺序阅读：

1. [`RELEASE-README.md`](./RELEASE-README.md)
2. [`ENVIRONMENT-SETUP.md`](./ENVIRONMENT-SETUP.md)
3. [`DEPLOY-ON-TOMCAT.md`](./DEPLOY-ON-TOMCAT.md)
4. [`DATASET-README.md`](./DATASET-README.md)

这样可以先理解整体结构，再完成配置和部署，最后理解测试数据内容。