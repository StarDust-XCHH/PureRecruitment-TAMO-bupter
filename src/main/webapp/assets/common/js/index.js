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
