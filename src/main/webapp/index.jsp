<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>TA Recruitment System</title>
    <style>
        :root {
            --bg-gradient: radial-gradient(circle at 20% 10%, #1a2140, #0b1020 60%, #05070f 100%);
            --card: rgba(255, 255, 255, 0.08);
            --card-border: rgba(255, 255, 255, 0.15);
            --text: #e8edf5;
            --muted: #aab3c5;
            --accent: #5dd6ff;
            --accent-2: #9b7bff;
            --accent-3: #45f3c2;
            --glow-1: rgba(93, 214, 255, 0.35);
            --glow-2: rgba(155, 123, 255, 0.35);
            --input-bg: rgba(3, 8, 18, 0.6);
            --input-border: rgba(255, 255, 255, 0.15);
            --danger-bg: rgba(255, 120, 120, 0.15);
            --danger-text: #ffb4b4;
        }

        :root[data-theme='light'] {
            --bg-gradient: radial-gradient(circle at 20% 10%, #f5f7ff, #e9edf7 55%, #dfe6f4 100%);
            --card: rgba(255, 255, 255, 0.85);
            --card-border: rgba(15, 20, 35, 0.1);
            --text: #1a2236;
            --muted: #5b657a;
            --accent: #3a6df0;
            --accent-2: #6a52ff;
            --accent-3: #1cc7a6;
            --glow-1: rgba(58, 109, 240, 0.25);
            --glow-2: rgba(106, 82, 255, 0.2);
            --input-bg: rgba(255, 255, 255, 0.9);
            --input-border: rgba(15, 20, 35, 0.2);
            --danger-bg: rgba(220, 80, 80, 0.12);
            --danger-text: #a11616;
        }

        * {
            box-sizing: border-box;
        }

        body {
            margin: 0;
            position: relative;
            font-family: Segoe UI, PingFang SC, Microsoft YaHei, sans-serif;
            background: var(--bg-gradient);
            color: var(--text);
            min-height: 100vh;
            overflow-y: auto;
            overflow-x: hidden;
            padding: 24px 0;
            transition: background 0.35s ease, color 0.35s ease;
        }

        .page-shell {
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 24px 16px;
        }

        .glow {
            position: absolute;
            width: 420px;
            height: 420px;
            background: radial-gradient(circle, var(--glow-1), transparent 70%);
            filter: blur(20px);
            animation: float 8s ease-in-out infinite;
            top: -120px;
            left: -120px;
            pointer-events: none;
        }

        .glow.two {
            width: 320px;
            height: 320px;
            background: radial-gradient(circle, var(--glow-2), transparent 70%);
            bottom: -80px;
            right: -80px;
            top: auto;
            left: auto;
            animation-delay: 1.6s;
        }

        .auth-shell {
            position: relative;
            width: min(420px, 92vw);
            min-height: auto;
        }

        .card {
            position: absolute;
            top: 50%;
            left: 0;
            width: min(420px, 92vw);
            padding: 32px 28px;
            background: var(--card);
            border: 1px solid var(--card-border);
            border-radius: 18px;
            backdrop-filter: blur(14px);
            box-shadow: 0 18px 40px rgba(3, 6, 20, 0.45);
            transform: translateY(-50%);
            transform-style: preserve-3d;
            transition: transform 0.4s ease, box-shadow 0.4s ease, background 0.35s ease, border-color 0.35s ease;
        }

        .card:hover {
            transform: translateY(calc(-50% - 6px)) rotateX(2deg) rotateY(-2deg);
            box-shadow: 0 24px 50px rgba(5, 10, 30, 0.55);
        }

        .hidden {
            display: none !important;
        }

        .fade-in {
            animation: cardFadeIn 0.4s ease forwards;
        }

        .theme-toggle {
            position: absolute;
            top: 20px;
            right: 24px;
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 8px;
            padding: 6px 10px;
            border-radius: 999px;
            border: 1px solid rgba(255, 255, 255, 0.18);
            background: rgba(255, 255, 255, 0.08);
            color: var(--text);
            font-size: 12px;
            cursor: pointer;
            transition: all 0.25s ease;
            min-width: 88px;
        }

        :root[data-theme='light'] .theme-toggle {
            border-color: rgba(15, 20, 35, 0.18);
            background: rgba(15, 25, 50, 0.06);
        }

        .theme-toggle:hover {
            transform: translateY(-1px);
        }

        .toggle-dot {
            width: 16px;
            height: 16px;
            border-radius: 50%;
            background: var(--accent);
            box-shadow: 0 0 10px rgba(93, 214, 255, 0.5);
            transition: transform 0.25s ease, background 0.25s ease;
            margin-left: auto;
            transform: translateX(0);
        }

        :root[data-theme='light'] .toggle-dot {
            background: rgba(58, 109, 240, 0.5);
            transform: translateX(-2px);
            box-shadow: 0 0 8px rgba(58, 109, 240, 0.25);
        }

        .brand {
            display: flex;
            align-items: center;
            gap: 12px;
            margin-bottom: 18px;
            cursor: pointer;
            user-select: none;
        }

        .logo {
            width: 42px;
            height: 42px;
            border-radius: 12px;
            background: linear-gradient(135deg, var(--accent), var(--accent-2));
            display: grid;
            place-items: center;
            font-weight: 700;
            letter-spacing: 1px;
            color: #0a0f1e;
            box-shadow: 0 6px 20px rgba(93, 214, 255, 0.4);
        }

        h1 {
            font-size: 20px;
            margin: 0;
        }

        .sub {
            color: var(--muted);
            margin-top: 4px;
            font-size: 12px;
        }

        .tabs {
            display: flex;
            gap: 10px;
            margin: 18px 0 20px;
        }

        .tab {
            flex: 1;
            padding: 10px 12px;
            border-radius: 12px;
            border: 1px solid transparent;
            background: rgba(255, 255, 255, 0.06);
            color: var(--muted);
            font-weight: 600;
            cursor: pointer;
            transition: all 0.25s ease;
        }

        :root[data-theme='light'] .tab {
            background: rgba(15, 25, 50, 0.05);
        }

        .tab.active {
            background: linear-gradient(135deg, rgba(93, 214, 255, 0.25), rgba(155, 123, 255, 0.25));
            color: var(--text);
            border-color: rgba(93, 214, 255, 0.6);
            box-shadow: inset 0 0 12px rgba(93, 214, 255, 0.2);
        }

        .field-row {
            display: flex;
            gap: 12px;
            width: 100%;
        }

        .field-row .field {
            flex: 1;
            margin-bottom: 16px;
        }

        .field {
            display: flex;
            flex-direction: column;
            gap: 8px;
            margin-bottom: 16px;
            width: 100%;
        }

        label {
            font-size: 13px;
            color: var(--muted);
        }

        input[type='text'],
        input[type='password'],
        input[type='email'],
        input[type='tel'] {
            width: 100%;
            padding: 12px 14px;
            border-radius: 12px;
            border: 1px solid var(--input-border);
            background: var(--input-bg);
            color: var(--text);
            outline: none;
            transition: border 0.2s ease, box-shadow 0.2s ease, background 0.35s ease;
        }

        input::placeholder {
            color: rgba(170, 179, 197, 0.65);
        }

        input[readonly] {
            background: rgba(255, 255, 255, 0.03);
            color: var(--muted);
            cursor: not-allowed;
        }

        :root[data-theme='light'] input[readonly] {
            background: rgba(15, 20, 35, 0.05);
        }

        input:focus {
            border-color: rgba(93, 214, 255, 0.7);
            box-shadow: 0 0 0 3px rgba(93, 214, 255, 0.15);
        }

        input[aria-invalid='true'] {
            border-color: rgba(255, 120, 120, 0.7);
            box-shadow: 0 0 0 3px rgba(255, 120, 120, 0.12);
        }

        .actions {
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 12px;
            margin-top: 10px;
        }

        .btn {
            flex: 1;
            padding: 12px 16px;
            border-radius: 12px;
            border: none;
            color: #081018;
            font-weight: 700;
            cursor: pointer;
            background: linear-gradient(135deg, var(--accent), var(--accent-3));
            box-shadow: 0 10px 24px rgba(69, 243, 194, 0.25);
            transition: transform 0.2s ease, box-shadow 0.2s ease, background 0.35s ease, opacity 0.2s ease;
        }

        .btn:hover:not(:disabled) {
            transform: translateY(-2px);
            box-shadow: 0 12px 28px rgba(69, 243, 194, 0.35);
        }

        .btn.alt {
            background: linear-gradient(135deg, var(--accent-2), var(--accent));
        }

        .btn:disabled {
            cursor: not-allowed;
            opacity: 0.7;
            transform: none;
            box-shadow: none;
        }

        .link {
            color: var(--accent);
            text-decoration: none;
            font-size: 13px;
            cursor: pointer;
            font-weight: 500;
        }

        .link:hover,
        .link:focus-visible {
            text-decoration: underline;
        }

        .error {
            margin-top: 10px;
            padding: 8px 10px;
            border-radius: 10px;
            background: var(--danger-bg);
            color: var(--danger-text);
            font-size: 12px;
            display: none;
        }

        .admin {
            position: absolute;
            top: calc(100% + 18px);
            left: 0;
            right: 0;
            margin-top: 0;
            padding: 18px 18px 16px;
            background: var(--card);
            border: 1px solid var(--card-border);
            border-radius: 16px;
            backdrop-filter: blur(14px);
            box-shadow: 0 14px 30px rgba(3, 6, 20, 0.35);
            display: none;
        }

        :root[data-theme='light'] .admin {
            box-shadow: 0 12px 24px rgba(15, 20, 35, 0.08);
        }

        .admin.show {
            display: block;
            animation: reveal 0.4s ease;
        }

        .hint {
            margin-top: 12px;
            font-size: 11px;
            color: rgba(255, 255, 255, 0.4);
            text-align: center;
        }

        :root[data-theme='light'] .hint {
            color: rgba(0, 0, 0, 0.45);
        }

        @keyframes float {
            0%, 100% {
                transform: translateY(0) translateX(0);
            }
            50% {
                transform: translateY(20px) translateX(10px);
            }
        }

        @keyframes reveal {
            from {
                opacity: 0;
                transform: translateY(-6px);
            }
            to {
                opacity: 1;
                transform: translateY(0);
            }
        }

        @keyframes cardFadeIn {
            from {
                opacity: 0;
                transform: translateY(15px);
            }
            to {
                opacity: 1;
                transform: translateY(0);
            }
        }

        @media (max-width: 560px) {
            .page-shell {
                padding: 20px 12px;
            }

            .auth-shell {
                min-height: auto;
            }

            .field-row {
                flex-direction: column;
                gap: 0;
            }

            .actions {
                flex-direction: column;
                align-items: stretch;
            }

            .theme-toggle {
                right: 20px;
            }
        }

        @media (prefers-reduced-motion: reduce) {
            *, *::before, *::after {
                animation: none !important;
                transition: none !important;
                scroll-behavior: auto !important;
            }
        }
    </style>
</head>
<body>
<div class="glow"></div>
<div class="glow two"></div>

<div class="page-shell">
    <div class="auth-shell">
        <section class="card" id="loginCard">
            <button class="theme-toggle" id="themeToggle" type="button">
                <span id="themeText">Theme</span>
                <span class="toggle-dot"></span>
            </button>

            <div class="brand" id="brandTrigger">
                <div class="logo">TA</div>
                <div>
                    <h1>TA Recruitment System</h1>
                    <div class="sub">请选择登录身份</div>
                </div>
            </div>

            <div class="tabs">
                <button class="tab active" type="button" data-role="TA">TA 登录</button>
                <button class="tab" type="button" data-role="MO">MO 登录</button>
            </div>

            <form id="loginForm">
                <input id="roleInput" name="role" type="hidden" value="TA">

                <div class="field">
                    <label for="username">用户名 / 邮箱 / 手机号</label>
                    <input id="username" name="username" type="text" placeholder="请输入用户名、邮箱或手机号" autocomplete="username">
                </div>

                <div class="field">
                    <label for="password">密码</label>
                    <input id="password" name="password" type="password" placeholder="请输入密码" autocomplete="current-password">
                </div>

                <div class="actions">
                    <button class="btn" id="loginSubmit" type="submit">进入系统</button>
                    <a class="link" id="openRegister" href="jsp/register.jsp">注册账号</a>
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
                <div class="logo">TA</div>
                <div>
                    <h1>TA Registration</h1>
                    <div class="sub">成为助教 Teaching Assistant</div>
                </div>
            </div>

            <form id="registerForm">
                <div class="field-row">
                    <div class="field">
                        <label for="registerTaId">TA ID 自动生成</label>
                        <input id="registerTaId" name="taId" type="text" readonly>
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

<script>
    (function () {
        const root = document.documentElement;
        const loginCard = document.getElementById('loginCard');
        const registerCard = document.getElementById('registerCard');
        const loginForm = document.getElementById('loginForm');
        const registerForm = document.getElementById('registerForm');
        const roleInput = document.getElementById('roleInput');
        const tabs = Array.from(document.querySelectorAll('.tab'));
        const loginError = document.getElementById('loginError');
        const registerError = document.getElementById('registerError');
        const adminError = document.getElementById('adminError');
        const adminPanel = document.getElementById('adminPanel');
        const brandTrigger = document.getElementById('brandTrigger');
        const themeToggle = document.getElementById('themeToggle');
        const registerThemeToggle = document.getElementById('registerThemeToggle');
        const themeText = document.getElementById('themeText');
        const openRegister = document.getElementById('openRegister');
        const backToLogin = document.getElementById('backToLogin');
        const adminLogin = document.getElementById('adminLogin');
        const loginSubmit = document.getElementById('loginSubmit');
        const registerSubmit = document.getElementById('registerSubmit');
        const registerTaId = document.getElementById('registerTaId');
        const loginUsername = document.getElementById('username');
        const loginPassword = document.getElementById('password');
        const registerName = document.getElementById('registerName');
        const registerUsername = document.getElementById('registerUsername');
        const registerEmail = document.getElementById('registerEmail');
        const registerPhone = document.getElementById('registerPhone');
        const registerPassword = document.getElementById('registerPassword');
        const registerConfirmPassword = document.getElementById('registerConfirmPassword');

        const adminRevealThreshold = 5;
        let brandTapCount = 0;
        let brandTapTimer = null;

        function updateThemeText() {
            themeText.textContent = root.getAttribute('data-theme') === 'light' ? 'Light' : 'Dark';
        }

        function applyTheme(theme) {
            if (theme === 'light') {
                root.setAttribute('data-theme', 'light');
            } else {
                root.removeAttribute('data-theme');
            }
            localStorage.setItem('ta-theme', theme);
            updateThemeText();
        }

        function toggleTheme() {
            const nextTheme = root.getAttribute('data-theme') === 'light' ? 'dark' : 'light';
            applyTheme(nextTheme);
        }

        function setInlineMessage(target, message) {
            if (message) {
                target.textContent = message;
                target.style.display = 'block';
            } else {
                target.textContent = '';
                target.style.display = 'none';
            }
        }

        function clearFieldErrors(fields) {
            fields.forEach(function (field) {
                field.setAttribute('aria-invalid', 'false');
            });
        }

        function markInvalid(field) {
            field.setAttribute('aria-invalid', 'true');
        }

        function generateTaId() {
            return 'TA-' + Math.floor(Math.random() * 90000 + 10000);
        }

        function saveTaUser(user) {
            const serialized = JSON.stringify(user);
            sessionStorage.setItem('ta-user', serialized);
            localStorage.setItem('ta-user', serialized);
        }

        function showCard(cardToShow, cardToHide) {
            setInlineMessage(loginError, '');
            setInlineMessage(registerError, '');
            cardToHide.classList.add('hidden');
            cardToShow.classList.remove('hidden');
            cardToShow.classList.remove('fade-in');
            void cardToShow.offsetWidth;
            cardToShow.classList.add('fade-in');
        }

        function setLoading(button, loadingText, isLoading, fallbackText) {
            button.disabled = isLoading;
            button.textContent = isLoading ? loadingText : fallbackText;
        }

        const savedTheme = localStorage.getItem('ta-theme');
        applyTheme(savedTheme === 'light' ? 'light' : 'dark');

        tabs.forEach(function (tab) {
            tab.addEventListener('click', function () {
                tabs.forEach(function (item) {
                    item.classList.remove('active');
                });
                tab.classList.add('active');
                roleInput.value = tab.getAttribute('data-role') || 'TA';
            });
        });

        themeToggle.addEventListener('click', toggleTheme);
        registerThemeToggle.addEventListener('click', function () {
            themeToggle.click();
        });

        function revealAdminPanel() {
            adminPanel.classList.add('show');
            adminPanel.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
        }

        function resetBrandTapCounter() {
            brandTapCount = 0;
            if (brandTapTimer) {
                clearTimeout(brandTapTimer);
                brandTapTimer = null;
            }
        }

        function handleBrandTap() {
            brandTapCount += 1;
            if (brandTapTimer) {
                clearTimeout(brandTapTimer);
            }
            brandTapTimer = setTimeout(function () {
                resetBrandTapCounter();
            }, 2000);

            if (brandTapCount >= adminRevealThreshold) {
                revealAdminPanel();
                resetBrandTapCounter();
            }
        }

        brandTrigger.addEventListener('click', handleBrandTap);

        adminLogin.addEventListener('click', function () {
            setInlineMessage(adminError, '当前仅保留管理员入口彩蛋，暂未接入管理员登录接口');
        });

        openRegister.addEventListener('click', function (event) {
            event.preventDefault();
            if (roleInput.value !== 'TA') {
                setInlineMessage(loginError, '目前仅开放 TA 角色注册哦！');
                return;
            }
            registerTaId.value = generateTaId();
            showCard(registerCard, loginCard);
        });

        backToLogin.addEventListener('click', function (event) {
            event.preventDefault();
            showCard(loginCard, registerCard);
            if (registerUsername.value.trim()) {
                loginUsername.value = registerUsername.value.trim();
            }
        });

        loginForm.addEventListener('submit', async function (event) {
            event.preventDefault();
            setInlineMessage(loginError, '');
            clearFieldErrors([loginUsername, loginPassword]);

            const username = loginUsername.value.trim();
            const password = loginPassword.value.trim();
            const role = roleInput.value;

            if (!username || !password) {
                if (!username) {
                    markInvalid(loginUsername);
                }
                if (!password) {
                    markInvalid(loginPassword);
                }
                setInlineMessage(loginError, '请输入用户名和密码');
                return;
            }

            if (role !== 'TA') {
                setInlineMessage(loginError, '当前仅支持 TA 账号登录');
                return;
            }

            setLoading(loginSubmit, '登录中...', true, '进入系统');

            try {
                const response = await fetch('api/ta/login', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8'
                    },
                    body: new URLSearchParams({
                        identifier: username,
                        password: password
                    })
                });

                let payload = {};
                try {
                    payload = await response.json();
                } catch (error) {
                    payload = {};
                }

                if (!response.ok || !payload.success) {
                    setInlineMessage(loginError, payload.message || '登录失败，请检查账号或密码');
                    return;
                }

                const data = payload.data || {};
                const user = {
                    taId: data.taId || '',
                    name: data.name || '',
                    username: data.username || '',
                    email: data.email || '',
                    phone: data.phone || '',
                    department: data.department || '',
                    status: data.status || '',
                    account: data.username || username,
                    role: 'TA',
                    loginAt: data.loginAt || new Date().toISOString(),
                    isFirstLogin: Boolean(data.isFirstLogin)
                };

                saveTaUser(user);
                window.location.replace('pages/ta/ta-home.jsp');
            } catch (error) {
                setInlineMessage(loginError, '登录请求失败，请稍后再试');
            } finally {
                setLoading(loginSubmit, '登录中...', false, '进入系统');
            }
        });

        registerForm.addEventListener('submit', async function (event) {
            event.preventDefault();
            setInlineMessage(registerError, '');
            clearFieldErrors([
                registerName,
                registerUsername,
                registerEmail,
                registerPhone,
                registerPassword,
                registerConfirmPassword
            ]);

            const taId = registerTaId.value.trim();
            const name = registerName.value.trim();
            const username = registerUsername.value.trim();
            const email = registerEmail.value.trim();
            const phone = registerPhone.value.trim();
            const password = registerPassword.value;
            const confirmPassword = registerConfirmPassword.value;

            const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
            const phonePattern = /^\+?\d{7,15}$/;

            if (!name || !username || !email || !phone) {
                if (!name) {
                    markInvalid(registerName);
                }
                if (!username) {
                    markInvalid(registerUsername);
                }
                if (!email) {
                    markInvalid(registerEmail);
                }
                if (!phone) {
                    markInvalid(registerPhone);
                }
                setInlineMessage(registerError, '姓名、用户名、邮箱、手机号不能为空');
                return;
            }

            if (!emailPattern.test(email)) {
                markInvalid(registerEmail);
                setInlineMessage(registerError, '邮箱格式不正确');
                return;
            }

            if (!phonePattern.test(phone)) {
                markInvalid(registerPhone);
                setInlineMessage(registerError, '手机号格式不正确');
                return;
            }

            if (password.length < 6) {
                markInvalid(registerPassword);
                setInlineMessage(registerError, '密码至少 6 位');
                return;
            }

            if (password !== confirmPassword) {
                markInvalid(registerPassword);
                markInvalid(registerConfirmPassword);
                setInlineMessage(registerError, '两次输入的密码不一致');
                return;
            }

            setLoading(registerSubmit, '注册中...', true, '完成注册并登录');

            try {
                const response = await fetch('api/ta/register', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8'
                    },
                    body: new URLSearchParams({
                        taId: taId,
                        name: name,
                        username: username,
                        email: email,
                        phone: phone,
                        password: password,
                        confirmPassword: confirmPassword
                    })
                });

                let payload = {};
                try {
                    payload = await response.json();
                } catch (error) {
                    payload = {};
                }

                if (!response.ok || !payload.success) {
                    setInlineMessage(registerError, payload.message || '注册失败，请检查信息后重试');
                    return;
                }

                const registered = payload.data || {};
                const user = {
                    taId: registered.taId || taId,
                    name: registered.name || name,
                    username: registered.username || username,
                    email: registered.email || email,
                    phone: registered.phone || phone,
                    role: 'TA',
                    account: registered.username || username,
                    loginAt: new Date().toISOString(),
                    isFirstLogin: true,
                    createdAt: registered.createdAt || null
                };

                saveTaUser(user);
                window.location.replace('pages/ta/ta-home.jsp');
            } catch (error) {
                setInlineMessage(registerError, '注册请求失败，请稍后再试');
            } finally {
                setLoading(registerSubmit, '注册中...', false, '完成注册并登录');
            }
        });
    })();
</script>
</body>
</html>
