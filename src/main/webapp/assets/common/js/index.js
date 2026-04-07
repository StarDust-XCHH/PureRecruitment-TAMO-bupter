(function () {
    const root = document.documentElement;
    const loginCard = document.getElementById('loginCard');
    const registerCard = document.getElementById('registerCard');
    const modalOverlay = document.getElementById('modalOverlay');
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
            loginTitle.textContent = 'MO Recruitment System';
            loginSub.textContent = 'Module Organizer 登录入口';
            loginIdentifierLabel.textContent = 'MO ID / 用户名 / 邮箱 / 手机号';
            loginUsername.placeholder = '请输入 MO ID、用户名、邮箱或手机号';
        } else {
            loginLogo.textContent = 'TA';
            loginTitle.textContent = 'TA Recruitment System';
            loginSub.textContent = '请选择登录身份';
            loginIdentifierLabel.textContent = '用户名 / 邮箱 / 手机号';
            loginUsername.placeholder = '请输入用户名、邮箱或手机号';
        }
    }

    /**
     * 根据角色更新注册页面 UI
     */
    function updateRegisterUI(role) {
        if (role === 'MO') {
            registerLogo.textContent = 'MO';
            registerTitle.textContent = 'MO Registration';
            registerSub.textContent = '成为课程负责人 Module Organizer';
            registerIdLabel.textContent = 'MO ID 自动生成';
        } else {
            registerLogo.textContent = 'TA';
            registerTitle.textContent = 'TA Registration';
            registerSub.textContent = '成为助教 Teaching Assistant';
            registerIdLabel.textContent = 'TA ID 自动生成';
        }
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

    tabs.forEach(function (tab) {
        tab.addEventListener('click', function () {
            tabs.forEach(function (item) {
                item.classList.remove('active');
            });
            tab.classList.add('active');
            const selectedRole = tab.getAttribute('data-role') || 'TA';
            roleInput.value = selectedRole;
            updateLoginUI(selectedRole);
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

    adminLogin.addEventListener('click', async function () {
        setInlineMessage(adminError, '');
        const adminAcc = document.getElementById('adminAccount').value.trim();
        const adminPwd = document.getElementById('adminPassword').value;

        if (!adminAcc || !adminPwd) {
            setInlineMessage(adminError, '请输入管理员账号和密码');
            return;
        }

        setLoading(adminLogin, '登录中...', true, '管理员登录');

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
                setInlineMessage(adminError, '账号或密码错误（测试账号: admin/admin123）');
            }
        } catch (error) {
            console.error("[JS] 管理员登录异常:", error);
            setInlineMessage(adminError, '登录失败，请重试');
        } finally {
            setLoading(adminLogin, '登录中...', false, '管理员登录');
        }
    });

    openRegister.addEventListener('click', function (event) {
        event.preventDefault();
        const selectedRole = roleInput.value;
        if (selectedRole !== 'TA' && selectedRole !== 'MO') {
            setInlineMessage(loginError, '目前仅开放 TA 和 MO 角色注册哦！');
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
            setInlineMessage(loginError, '请输入用户名和密码');
            return;
        }

        const loginUrl = role === 'MO' ? 'api/mo/login' : 'api/ta/login';

        setLoading(loginSubmit, '登录中...', true, '进入系统');

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
                setInlineMessage(loginError, '服务器接口未找到 (404)');
                return;
            }

            const payload = await response.json();
            console.log("[JS] 解析后的响应体:", payload);

            if (!response.ok || !payload.success) {
                setInlineMessage(loginError, payload.message || '登录失败');
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
            setInlineMessage(loginError, '连接服务器失败，请检查后端是否启动');
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
            setInlineMessage(registerError, (selectedRole === 'MO' ? 'MO ID' : 'TA ID') + ' 不能为空');
            return;
        }

        if (!name || !username || !email || !phone) {
            if (!name) markInvalid(registerName);
            if (!username) markInvalid(registerUsername);
            if (!email) markInvalid(registerEmail);
            if (!phone) markInvalid(registerPhone);
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
                if (payload.message && payload.message.includes('已存在')) {
                    if (selectedRole === 'TA') {
                        registerId.value = generateTaId();
                    } else {
                        registerId.value = generateMoId();
                    }
                    setInlineMessage(registerError, (selectedRole === 'MO' ? 'MO ID' : 'TA ID') + ' 冲突，已重新生成，请重试');
                    setLoading(registerSubmit, '注册中...', false, '完成注册并登录');
                    return;
                }
                setInlineMessage(registerError, payload.message || '注册失败，请检查信息后重试');
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
            setInlineMessage(registerError, '注册请求失败，请稍后再试');
        } finally {
            setLoading(registerSubmit, '注册中...', false, '完成注册并登录');
        }
    });
})();
