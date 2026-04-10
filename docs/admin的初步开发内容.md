# PureRecruitment Admin 模块

## 一、模块概述

Admin 模块是 PureRecruitment TA/MO 招聘系统的后台管理模块，提供管理员对系统用户、课程、公告等资源的统一管理功能。

## 二、技术架构

### 2.1 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Java Servlet + DAO |
| 前端 | JSP + 原生 JavaScript |
| 存储 | JSON 文件持久化 |
| 安全 | SHA-256 密码哈希 + Salt |

### 2.2 目录结构

```
src/main/java/com/bupt/tarecruit/admin/
├── controller/
│   ├── AdminAuthServlet.java    # 管理员认证
│   ├── AdminUserServlet.java    # 用户管理
│   ├── AdminCourseServlet.java   # 课程管理
│   └── AdminNoticeServlet.java   # 公告管理
└── dao/
    ├── AdminAccountDao.java      # 账户数据访问
    └── AdminDataDao.java         # 通用数据访问

src/main/webapp/
├── pages/admin/
│   └── admin-home.jsp            # 管理后台主页
└── assets/admin/
    ├── js/admin-home.js         # 前端交互逻辑
    └── css/                      # 样式文件

mountDataTAMObupter/admin/
├── admins.json                   # 管理员账户数据
├── profiles.json                 # 管理员资料
└── settings.json                # 管理员设置
```

## 三、功能清单

### 3.1 认证管理 ✅

| 功能 | 描述 | 状态 |
|------|------|------|
| 管理员登录 | 用户名/密码验证，SHA-256+Salt 加密 | ✅ |
| 默认管理员初始化 | 首次访问自动创建默认账号 (admin/admin123) | ✅ |
| 获取当前用户信息 | 获取登录管理员完整信息 | ✅ |
| 退出登录 | 清除登录状态并跳转 | ✅ |
| 登录失败锁定 | 连续失败后锁定账户 | ✅ |

### 3.2 数据看板 ✅

| 功能 | 描述 | 状态 |
|------|------|------|
| 用户统计卡片 | 显示总用户数、TA用户数、MO用户数、管理员数 | ✅ |
| 最近登录用户 | 显示最近5次登录的用户记录 | ✅ |
| 公告预览 | 显示最新5条公告卡片 | ✅ |
| 数据刷新 | 手动刷新看板数据 | ✅ |

### 3.3 用户管理 ✅

| 功能 | 描述 | 状态 |
|------|------|------|
| 用户列表 | 获取并展示所有用户 (TA/MO/ADMIN) | ✅ |
| 角色筛选 | 按角色过滤用户列表 | ✅ |
| 关键词搜索 | 按用户名/邮箱/姓名搜索 | ✅ |
| 用户编辑 | 编辑用户信息 | ⚠️ 前端显示，待开发 |
| 用户删除 | 删除指定用户 | ⚠️ 前端显示，待开发 |

### 3.4 课程管理 ✅

| 功能 | 描述 | 状态 |
|------|------|------|
| 课程列表 | 获取并展示所有课程 | ✅ |
| 状态筛选 | 按开放/关闭状态筛选 | ✅ |
| 关键词搜索 | 按课程编号/名称/MO姓名搜索 | ✅ |
| 课程统计 | 统计课程总数、各状态数量 | ✅ |
| 课程详情 | 查看课程完整信息 | ✅ |

### 3.5 公告管理 ✅

| 功能 | 描述 | 状态 |
|------|------|------|
| 公告列表 | 获取并展示所有公告 | ✅ |
| 创建公告 | 创建新公告 (支持优先级设置) | ✅ |
| 删除公告 | 删除指定公告 | ✅ |
| 编辑公告 | 编辑现有公告 | ⚠️ 待开发 |

### 3.6 权限管理 ⚠️

| 功能 | 描述 | 状态 |
|------|------|------|
| 权限配置页面 | 管理员权限管理界面 | ⚠️ 占位页面 |

### 3.7 系统设置 ⚠️

| 功能 | 描述 | 状态 |
|------|------|------|
| 主题设置 | 界面主题切换 | ⚠️ 占位页面 |
| 通知设置 | 系统通知偏好 | ⚠️ 占位页面 |

## 四、API 接口

### 4.1 AdminAuthServlet

| URL | 方法 | action | 描述 |
|-----|------|--------|------|
| `/api/admin/auth` | POST | `login` | 管理员登录 |
| `/api/admin/auth` | POST | `init` | 初始化默认管理员 |
| `/api/admin/auth` | GET | `me` | 获取当前用户信息 |

### 4.2 AdminUserServlet

| URL | 方法 | 描述 |
|-----|------|------|
| `/api/admin/users` | GET | 获取用户列表 |
| `/api/admin/users/stats` | GET | 获取用户统计 |

### 4.3 AdminCourseServlet

| URL | 方法 | 描述 |
|-----|------|------|
| `/api/admin/courses` | GET | 获取课程列表 |
| `/api/admin/courses/stats` | GET | 获取课程统计 |

### 4.4 AdminNoticeServlet

| URL | 方法 | 描述 |
|-----|------|------|
| `/api/admin/notices` | GET | 获取公告列表 |
| `/api/admin/notices` | POST | 创建公告 |
| `/api/admin/notices/{id}` | DELETE | 删除公告 |

## 五、数据结构

### 5.1 管理员账户 (admins.json)

```json
{
  "id": "ADMIN-00001",
  "username": "admin",
  "email": "admin@bupt.edu.cn",
  "status": "active",
  "auth": {
    "passwordHash": "sha256_hash",
    "passwordSalt": "random_salt",
    "lastLoginAt": "2026-04-07T10:30:00",
    "failedAttempts": 0
  }
}
```

### 5.2 管理员资料 (profiles.json)

```json
{
  "id": "PROFILE-ADMIN-00001",
  "adminId": "ADMIN-00001",
  "realName": "Administrator",
  "title": "管理员",
  "permissions": ["users:read", "courses:read", "notices:read"]
}
```

### 5.3 公告 (内存存储)

```json
{
  "id": 1,
  "title": "系统公告",
  "content": "公告内容",
  "priority": "high",
  "createdAt": "2026-04-07T10:00:00",
  "author": "admin"
}
```

## 六、开发状态

### 已完成
- 认证系统完整实现
- 数据看板功能
- 用户管理 CRUD 基础功能
- 课程管理完整功能
- 公告管理完整功能
- SPA 单页应用架构

### 待开发
- 用户编辑后端 API
- 用户删除后端 API
- 公告编辑功能
- 权限管理页面
- 系统设置页面
- 公告数据持久化 (当前为内存存储)
