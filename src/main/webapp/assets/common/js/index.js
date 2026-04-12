(function () {
    const root = document.documentElement;
    const loginCard = document.getElementById('loginCard');
    const registerCard = document.getElementById('registerCard');
    const modalOverlay = document.getElementById('modalOverlay');
    const loginForm = document.getElementById('loginForm');
    const registerForm = document.getElementById('registerForm');
    const roleInput = document.getElementById('roleInput');
    const loginTabs = Array.from(document.querySelectorAll('#loginCard .tabs .tab'));
    const registerTabs = Array.from(document.querySelectorAll('#registerCard .tabs .tab'));
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
    const adminAccount = document.getElementById('adminAccount');
    const adminPassword = document.getElementById('adminPassword');
    const loginSubmit = document.getElementById('loginSubmit');
    const registerSubmit = document.getElementById('registerSubmit');
    const registerId = document.getElementById('registerId');
    const registerRoleInput = document.getElementById('registerRoleInput');
    const loginUsername = document.getElementById('username');
    const loginPassword = document.getElementById('password');
    const registerName = document.getElementById('registerName');
    const registerUsername = document.getElementById('registerUsername');
    const registerEmail = document.getElementById('registerEmail');
    const registerPhone = document.getElementById('registerPhone');
    const registerPassword = document.getElementById('registerPassword');
    const registerConfirmPassword = document.getElementById('registerConfirmPassword');

    // 动态 UI 元素
    const loginLogo = document.getElementById('loginLogo');
    const loginTitle = document.getElementById('loginTitle');
    const loginSub = document.getElementById('loginSub');
    const registerLogo = document.getElementById('registerLogo');
    const registerTitle = document.getElementById('registerTitle');
    const registerSub = document.getElementById('registerSub');
    const registerIdLabel = document.getElementById('registerIdLabel');
    const loginIdentifierLabel = document.getElementById('loginIdentifierLabel');
    const adminUnlockHint = document.getElementById('adminUnlockHint');

    const adminRevealThreshold = 5;
    let brandTapCount = 0;
    let brandTapTimer = null;

    function updateThemeText() {
        const isLight = root.getAttribute('data-theme') === 'light';
        const label = isLight ? 'Light' : 'Dark';
        if (themeText) {
            themeText.textContent = label;
        }
        const registerThemeText = document.getElementById('registerThemeText');
        if (registerThemeText) {
            registerThemeText.textContent = label;
        }
        [themeToggle, registerThemeToggle].forEach(function (toggle) {
            if (!toggle) {
                return;
            }
            toggle.setAttribute('aria-label', isLight ? 'Switch to dark theme' : 'Switch to light theme');
            toggle.setAttribute('title', isLight ? 'Switch to dark theme' : 'Switch to light theme');
            const icon = toggle.querySelector('.theme-toggle-icon');
            if (icon) {
                icon.textContent = isLight ? '☀️' : '🌙';
            }
        });
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

    /**
     * 生成唯一的 TA ID
     * 格式：TA-XXXXX（5位数字）
     */
    function generateTaId() {
        return 'TA-' + Math.floor(Math.random() * 90000 + 10000);
    }

    /**
     * 生成唯一的 MO ID
     * 格式：MO-XXXXX（5位数字）
     * 使用日期时间戳 + 随机数组合，降低重复概率
     */
    function generateMoId() {
        const timestamp = Date.now().toString(36).toUpperCase().slice(-3);
        const random = Math.floor(Math.random() * 900 + 100);
        return 'MO-' + random + timestamp;
    }

    /**
     * 根据角色更新登录页面 UI
     */
    function updateLoginUI(role) {
        if (role === 'MO') {
            loginLogo.textContent = 'MO';
            loginTitle.textContent = 'Teaching Assistant Recruitment';
            loginSub.textContent = 'Module Organizer · Sign in';
            loginIdentifierLabel.textContent = 'Username, email, or phone';
            loginUsername.placeholder = 'Enter username, email, or phone';
        } else {
            loginLogo.textContent = 'TA';
            loginTitle.textContent = 'Teaching Assistant Recruitment';
            loginSub.textContent = 'Teaching Assistant · Sign in';
            loginIdentifierLabel.textContent = 'Username, email, or phone';
            loginUsername.placeholder = 'Enter username, email, or phone';
        }
    }

    /**
     * 根据角色更新注册页面 UI
     */
    function updateRegisterUI(role) {
        if (role === 'MO') {
            registerLogo.textContent = 'MO';
            registerTitle.textContent = 'Sign up';
            registerSub.textContent = 'Module Organizer';
            registerIdLabel.textContent = 'Auto-generated account ID';
        } else {
            registerLogo.textContent = 'TA';
            registerTitle.textContent = 'Sign up';
            registerSub.textContent = 'Teaching Assistant';
            registerIdLabel.textContent = 'Auto-generated account ID';
        }
    }

    function syncRegisterRoleTabs(role) {
        registerTabs.forEach(function (t) {
            const r = t.getAttribute('data-role') || 'TA';
            const on = r === role;
            t.classList.toggle('active', on);
            t.setAttribute('aria-selected', on ? 'true' : 'false');
        });
    }

    /**
     * 切换注册身份：保留已填表单项，按角色重新生成 ID。
     */
    function applyRegisterRole(role) {
        const next = role === 'MO' ? 'MO' : 'TA';
        if (registerRoleInput.value === next) {
            return;
        }
        registerRoleInput.value = next;
        registerId.value = next === 'TA' ? generateTaId() : generateMoId();
        updateRegisterUI(next);
        syncRegisterRoleTabs(next);
        setInlineMessage(registerError, '');
    }

    /**
     * 注册提交前：与登录页同风格的确认层（非浏览器原生 confirm）。
     */
    function showRegisterConfirm(role) {
        return new Promise(function (resolve) {
            const modal = document.getElementById('registerConfirmModal');
            const msg = document.getElementById('registerConfirmMessage');
            const okBtn = document.getElementById('registerConfirmOk');
            const cancelBtn = document.getElementById('registerConfirmCancel');
            if (!modal || !msg || !okBtn || !cancelBtn) {
                resolve(true);
                return;
            }

            msg.textContent =
                role === 'MO'
                    ? 'Create a Module Organizer (MO) account?'
                    : 'Create a Teaching Assistant (TA) account?';

            function finish(result) {
                modal.classList.add('hidden');
                modal.setAttribute('aria-hidden', 'true');
                document.removeEventListener('keydown', onDocKey);
                modal.removeEventListener('click', onBackdrop);
                okBtn.removeEventListener('click', onOk);
                cancelBtn.removeEventListener('click', onCancel);
                resolve(result);
                if (!result && registerSubmit && typeof registerSubmit.focus === 'function') {
                    registerSubmit.focus();
                }
            }

            function onOk() {
                finish(true);
            }
            function onCancel() {
                finish(false);
            }
            function onBackdrop(ev) {
                if (ev.target === modal) {
                    finish(false);
                }
            }
            function onDocKey(ev) {
                if (ev.key === 'Escape') {
                    ev.preventDefault();
                    finish(false);
                }
            }

            okBtn.addEventListener('click', onOk);
            cancelBtn.addEventListener('click', onCancel);
            modal.addEventListener('click', onBackdrop);
            document.addEventListener('keydown', onDocKey);

            modal.classList.remove('hidden');
            modal.setAttribute('aria-hidden', 'false');
            okBtn.focus();
        });
    }

    function saveTaUser(user) {
        const serialized = JSON.stringify(user);
        sessionStorage.setItem('ta-user', serialized);
        localStorage.setItem('ta-user', serialized);
    }

    function saveMoUser(user) {
        const serialized = JSON.stringify(user);
        sessionStorage.setItem('mo-user', serialized);
        localStorage.setItem('mo-user', serialized);
    }

    function setOverlayState(isActive) {
        modalOverlay.classList.toggle('hidden', !isActive);
        modalOverlay.classList.toggle('active', isActive);
        modalOverlay.setAttribute('aria-hidden', String(!isActive));
    }

    function showCard(cardToShow, cardToHide) {
        setInlineMessage(loginError, '');
        setInlineMessage(registerError, '');
        cardToHide.classList.add('hidden');
        cardToShow.classList.remove('hidden');
        cardToShow.classList.remove('fade-in');
        void cardToShow.offsetWidth;
        cardToShow.classList.add('fade-in');
        setOverlayState(cardToShow === registerCard);
    }

    function setLoading(button, loadingText, isLoading, fallbackText) {
        button.disabled = isLoading;
        button.textContent = isLoading ? loadingText : fallbackText;
    }

    const savedTheme = localStorage.getItem('ta-theme');
    applyTheme(savedTheme === 'light' ? 'light' : 'dark');
    setOverlayState(false);

    loginTabs.forEach(function (tab) {
        tab.addEventListener('click', function () {
            loginTabs.forEach(function (item) {
                item.classList.remove('active');
            });
            tab.classList.add('active');
            const selectedRole = tab.getAttribute('data-role') || 'TA';
            roleInput.value = selectedRole;
            updateLoginUI(selectedRole);
        });
    });

    registerTabs.forEach(function (tab) {
        tab.addEventListener('click', function () {
            const selectedRole = tab.getAttribute('data-role') || 'TA';
            applyRegisterRole(selectedRole);
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
        if (adminUnlockHint) {
            adminUnlockHint.textContent = '';
            adminUnlockHint.hidden = true;
        }
    }

    function updateAdminUnlockHint() {
        if (!adminUnlockHint) return;
        if (brandTapCount < 2) {
            adminUnlockHint.textContent = '';
            adminUnlockHint.hidden = true;
            return;
        }
        if (brandTapCount >= adminRevealThreshold) {
            adminUnlockHint.textContent = '';
            adminUnlockHint.hidden = true;
            return;
        }
        const remaining = adminRevealThreshold - brandTapCount;
        adminUnlockHint.hidden = false;
        adminUnlockHint.textContent =
            remaining === 1
                ? 'One more click on the title to unlock the admin entry.'
                : remaining + ' more clicks on the title to unlock the admin entry.';
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
            return;
        }
        updateAdminUnlockHint();
    }

    brandTrigger.addEventListener('click', handleBrandTap);

    adminLogin.addEventListener('click', async function () {
        setInlineMessage(adminError, '');
        const adminAcc = document.getElementById('adminAccount').value.trim();
        const adminPwd = document.getElementById('adminPassword').value;

        if (!adminAcc || !adminPwd) {
            setInlineMessage(adminError, 'Enter admin username and password.');
            return;
        }

        setLoading(adminLogin, 'Signing in…', true, 'Admin Sign in');

        try {
            if (adminAcc === 'admin' && adminPwd === 'admin123') {
                const adminUser = {
                    username: 'admin',
                    role: 'ADMIN',
                    loginAt: new Date().toISOString()
                };
                localStorage.setItem('admin-user', JSON.stringify(adminUser));
                window.location.replace('pages/admin/admin-home.jsp');
            } else {
                setInlineMessage(adminError, 'Invalid credentials (demo: admin / admin123).');
            }
        } catch (error) {
            console.error("[JS] 管理员登录异常:", error);
            setInlineMessage(adminError, 'Unable to sign in. Try again.');
        } finally {
            setLoading(adminLogin, 'Signing in…', false, 'Admin Sign in');
        }
    });

    openRegister.addEventListener('click', function (event) {
        event.preventDefault();
        const selectedRole = roleInput.value;
        if (selectedRole !== 'TA' && selectedRole !== 'MO') {
            setInlineMessage(loginError, 'Only TA or MO registration is available.');
            return;
        }

        // 根据角色生成对应的 ID
        if (selectedRole === 'TA') {
            registerId.value = generateTaId();
        } else if (selectedRole === 'MO') {
            registerId.value = generateMoId();
        }

        // 更新注册页面 UI
        registerRoleInput.value = selectedRole;
        updateRegisterUI(selectedRole);
        syncRegisterRoleTabs(selectedRole);

        showCard(registerCard, loginCard);
    });

    backToLogin.addEventListener('click', function (event) {
        event.preventDefault();
        showCard(loginCard, registerCard);
        if (registerUsername.value.trim()) {
            loginUsername.value = registerUsername.value.trim();
        }
    });

    modalOverlay.addEventListener('click', function () {
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
        const password = loginPassword.value;
        const role = roleInput.value || 'TA';

        console.group("--- 登录请求调试 ---");
        console.log("角色:", role);
        console.log("请求 URL:", role === 'MO' ? 'api/mo/login' : 'api/ta/login');
        console.log("识别码 (identifier):", username);
        console.log("密码长度:", password ? password.length : 0);
        console.groupEnd();

        if (!username || !password) {
            setInlineMessage(loginError, 'Enter username and password.');
            return;
        }

        const loginUrl = role === 'MO' ? 'api/mo/login' : 'api/ta/login';

        setLoading(loginSubmit, 'Signing in…', true, 'Sign in');

        try {
            const response = await fetch(loginUrl, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8'
                },
                body: new URLSearchParams({
                    identifier: username,
                    password: password
                })
            });

            console.log("[JS] 原始响应状态码:", response.status);

            if (response.status === 404) {
                console.error("[JS] 错误：接口地址不存在 (404)");
                setInlineMessage(loginError, 'Server endpoint not found (404).');
                return;
            }

            const payload = await response.json();
            console.log("[JS] 解析后的响应体:", payload);

            if (!response.ok || !payload.success) {
                setInlineMessage(loginError, payload.message || 'Unable to sign in.');
                return;
            }

            const data = payload.data || {};

            if (role === 'MO') {
                const moId = data.moId || '';
                const user = {
                    moId: moId,
                    name: data.name,
                    username: data.username,
                    email: data.email,
                    phone: data.phone,
                    role: 'MO',
                    loginAt: data.loginAt,
                    isFirstLogin: data.isFirstLogin,
                    onboardingKey: moId ? 'mo-onboarding:' + moId : ''
                };
                saveMoUser(user);
                window.location.replace('pages/mo/mo-home.jsp');
            } else {
                const user = {
                    taId: data.taId,
                    name: data.name,
                    username: data.username,
                    role: 'TA',
                    loginAt: data.loginAt,
                    isFirstLogin: data.isFirstLogin
                };
                saveTaUser(user);
                window.location.replace('pages/ta/ta-home.jsp');
            }

        } catch (error) {
            console.error("[JS] 网络请求异常:", error);
            setInlineMessage(loginError, 'Could not reach the server. Is the backend running?');
        } finally {
            setLoading(loginSubmit, 'Signing in…', false, 'Sign in');
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

        const userId = registerId.value.trim();
        const name = registerName.value.trim();
        const username = registerUsername.value.trim();
        const email = registerEmail.value.trim();
        const phone = registerPhone.value.trim();
        const password = registerPassword.value;
        const confirmPassword = registerConfirmPassword.value;
        const selectedRole = registerRoleInput.value || 'TA';

        const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        const phonePattern = /^\+?\d{7,15}$/;

        if (!userId) {
            setInlineMessage(registerError, 'Account ID is missing.');
            return;
        }

        if (!name || !username || !email || !phone) {
            if (!name) markInvalid(registerName);
            if (!username) markInvalid(registerUsername);
            if (!email) markInvalid(registerEmail);
            if (!phone) markInvalid(registerPhone);
            setInlineMessage(registerError, 'Name, username, email, and phone are required.');
            return;
        }

        if (!emailPattern.test(email)) {
            markInvalid(registerEmail);
            setInlineMessage(registerError, 'Invalid email format.');
            return;
        }

        if (!phonePattern.test(phone)) {
            markInvalid(registerPhone);
            setInlineMessage(registerError, 'Invalid phone number.');
            return;
        }

        if (password.length < 6) {
            markInvalid(registerPassword);
            setInlineMessage(registerError, 'Password must be at least 6 characters.');
            return;
        }

        if (password !== confirmPassword) {
            markInvalid(registerPassword);
            markInvalid(registerConfirmPassword);
            setInlineMessage(registerError, 'Passwords do not match.');
            return;
        }

        const confirmed = await showRegisterConfirm(selectedRole);
        if (!confirmed) {
            return;
        }

        setLoading(registerSubmit, 'Signing up…', true, 'Sign up');

        const registerUrl = selectedRole === 'MO' ? 'api/mo/register' : 'api/ta/register';
        const registerParamId = selectedRole === 'MO' ? 'moId' : 'taId';

        try {
            const response = await fetch(registerUrl, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8'
                },
                body: new URLSearchParams({
                    [registerParamId]: userId,
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
                // 如果 ID 重复，重新生成
                if (payload.message && (/already exists|already taken|already registered/i.test(payload.message) || payload.message.includes('已存在'))) {
                    if (selectedRole === 'TA') {
                        registerId.value = generateTaId();
                    } else {
                        registerId.value = generateMoId();
                    }
                    setInlineMessage(registerError, 'Account ID conflict; a new ID was generated. Try again.');
                    setLoading(registerSubmit, 'Signing up…', false, 'Sign up');
                    return;
                }
                setInlineMessage(registerError, payload.message || 'Unable to complete registration. Check your details and try again.');
                return;
            }

            const registered = payload.data || {};
            const finalUserId = selectedRole === 'MO' ? registered.moId : registered.taId;
            const resolvedId = finalUserId || userId;
            const user = {
                [selectedRole === 'MO' ? 'moId' : 'taId']: resolvedId,
                name: registered.name || name,
                username: registered.username || username,
                email: registered.email || email,
                phone: registered.phone || phone,
                role: selectedRole,
                loginAt: new Date().toISOString(),
                isFirstLogin: true,
                createdAt: registered.createdAt || null,
                onboardingKey: selectedRole === 'MO' && resolvedId ? 'mo-onboarding:' + resolvedId : ''
            };

            if (selectedRole === 'MO') {
                saveMoUser(user);
                window.location.replace('pages/mo/mo-home.jsp');
            } else {
                saveTaUser(user);
                window.location.replace('pages/ta/ta-home.jsp');
            }
        } catch (error) {
            setInlineMessage(registerError, 'Something went wrong. Please try again later.');
        } finally {
            setLoading(registerSubmit, 'Signing up…', false, 'Sign up');
        }
    });
})();
