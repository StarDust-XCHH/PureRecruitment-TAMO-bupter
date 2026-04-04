<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Recruitment System</title>
    <link rel="stylesheet" href="assets/common/css/index.css">
</head>
<body>
<div class="glow"></div>
<div class="glow two"></div>
<div class="modal-overlay hidden" id="modalOverlay" aria-hidden="true"></div>

<div class="page-shell">
    <div class="auth-shell">
        <section class="card" id="loginCard">
            <button class="theme-toggle" id="themeToggle" type="button">
                <span id="themeText">Theme</span>
                <span class="toggle-dot"></span>
            </button>

            <div class="brand" id="brandTrigger">
                <div class="logo" id="loginLogo">TA</div>
                <div>
                    <h1 id="loginTitle">TA Recruitment System</h1>
                    <div class="sub" id="loginSub">请选择登录身份</div>
                </div>
            </div>

            <div class="tabs">
                <button class="tab active" type="button" data-role="TA">TA 登录</button>
                <button class="tab" type="button" data-role="MO">MO 登录</button>
            </div>

            <form id="loginForm">
                <input id="roleInput" name="role" type="hidden" value="TA">

                <div class="field">
                    <label for="username" id="loginIdentifierLabel">用户名 / 邮箱 / 手机号</label>
                    <input id="username" name="username" type="text" placeholder="请输入用户名、邮箱或手机号" autocomplete="username">
                </div>

                <div class="field">
                    <label for="password">密码</label>
                    <input id="password" name="password" type="password" placeholder="请输入密码" autocomplete="current-password">
                </div>

                <div class="actions">
                    <button class="btn" id="loginSubmit" type="submit">进入系统</button>
                    <a class="link" id="openRegister" href="javascript:void(0)">注册账号</a>
                </div>

                <div class="error" id="loginError" aria-live="polite"></div>
            </form>

            <div class="admin" id="adminPanel">
                <div class="field">
                    <label for="adminAccount">管理员账号</label>
                    <input id="adminAccount" type="text" placeholder="请输入管理员账号">
                </div>
                <div class="field">
                    <label for="adminPassword">管理员密码</label>
                    <input id="adminPassword" type="password" placeholder="请输入管理员密码">
                </div>
                <button class="btn alt" id="adminLogin" type="button">管理员登录</button>
                <div class="error" id="adminError" aria-live="polite"></div>
            </div>

            <div class="hint">点击标题 5 次可解锁管理员入口</div>
        </section>

        <section class="card hidden" id="registerCard">
            <button class="theme-toggle" id="registerThemeToggle" type="button">
                <span>Theme</span>
                <span class="toggle-dot"></span>
            </button>

            <div class="brand">
                <div class="logo" id="registerLogo">TA</div>
                <div>
                    <h1 id="registerTitle">TA Registration</h1>
                    <div class="sub" id="registerSub">成为助教 Teaching Assistant</div>
                </div>
            </div>

            <form id="registerForm">
                <input id="registerRoleInput" name="role" type="hidden" value="TA">

                <div class="field-row">
                    <div class="field">
                        <label for="registerId" id="registerIdLabel">TA ID 自动生成</label>
                        <input id="registerId" name="id" type="text" readonly>
                    </div>
                    <div class="field">
                        <label for="registerName">姓名</label>
                        <input id="registerName" name="name" type="text" placeholder="请输入姓名" autocomplete="name">
                    </div>
                </div>

                <div class="field">
                    <label for="registerUsername">用户名 用于登录 需唯一</label>
                    <input id="registerUsername" name="username" type="text" placeholder="请输入唯一用户名" autocomplete="username">
                </div>

                <div class="field-row">
                    <div class="field">
                        <label for="registerEmail">邮箱</label>
                        <input id="registerEmail" name="email" type="email" placeholder="请输入邮箱" autocomplete="email">
                    </div>
                    <div class="field">
                        <label for="registerPhone">手机号</label>
                        <input id="registerPhone" name="phone" type="tel" placeholder="请输入手机号" autocomplete="tel">
                    </div>
                </div>

                <div class="field-row">
                    <div class="field">
                        <label for="registerPassword">密码</label>
                        <input id="registerPassword" name="password" type="password" placeholder="请至少输入 6 位密码" autocomplete="new-password">
                    </div>
                    <div class="field">
                        <label for="registerConfirmPassword">确认密码</label>
                        <input id="registerConfirmPassword" name="confirmPassword" type="password" placeholder="请再次输入密码" autocomplete="new-password">
                    </div>
                </div>

                <div class="actions">
                    <button class="btn alt" id="registerSubmit" type="submit">完成注册并登录</button>
                    <a class="link" id="backToLogin" href="#">返回登录</a>
                </div>

                <div class="error" id="registerError" aria-live="polite"></div>
            </form>
        </section>
    </div>
</div>

<script src="assets/common/js/index.js"></script>
</body>
</html>
