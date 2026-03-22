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

        adminLogin.addEventListener('click', async function () {
            setInlineMessage(adminError, '');
            const adminAccount = document.getElementById('adminAccount').value.trim();
            const adminPassword = document.getElementById('adminPassword').value;

            if (!adminAccount || !adminPassword) {
                setInlineMessage(adminError, 'Enter admin account and password');
                return;
            }

            setLoading(adminLogin, 'Signing in...', true, 'Admin sign-in');

            try {
                // Mock admin login — replace with a real API when available
                if (adminAccount === 'admin' && adminPassword === 'admin123') {
                    const adminUser = {
                        username: 'admin',
                        role: 'ADMIN',
                        loginAt: new Date().toISOString()
                    };
                    localStorage.setItem('admin-user', JSON.stringify(adminUser));
                    window.location.replace('pages/admin/admin-home.jsp');
                } else {
                    setInlineMessage(adminError, 'Invalid credentials (demo: admin / admin123)');
                }
            } catch (error) {
                console.error("[JS] Admin login error:", error);
                setInlineMessage(adminError, 'Sign-in failed. Try again.');
            } finally {
                setLoading(adminLogin, 'Signing in...', false, 'Admin sign-in');
            }
        });

        openRegister.addEventListener('click', function (event) {
            event.preventDefault();
            if (roleInput.value !== 'TA') {
                setInlineMessage(loginError, 'Registration is only available for the TA role.');
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

        function saveMoUser(user) {
            const serialized = JSON.stringify(user);
            sessionStorage.setItem('mo-user', serialized);
            localStorage.setItem('mo-user', serialized);
        }

        loginForm.addEventListener('submit', async function (event) {
            event.preventDefault();
            setInlineMessage(loginError, '');
            clearFieldErrors([loginUsername, loginPassword]);

            const username = loginUsername.value.trim();
            const password = loginPassword.value; // do not trim password
            const role = roleInput.value || 'TA';

            console.group("--- Login debug ---");
            console.log("URL:", "api/ta/login");
            console.log("identifier:", username);
            console.log("password length:", password ? password.length : 0);
            console.groupEnd();

            if (!username || !password) {
                setInlineMessage(loginError, 'Enter username and password');
                return;
            }

            if (role === 'MO') {
                saveMoUser({
                    username: username,
                    role: 'MO',
                    loginAt: new Date().toISOString()
                });
                window.location.replace('pages/mo/mo-home.jsp');
                return;
            }

            setLoading(loginSubmit, 'Signing in...', true, 'Continue');

            try {
                // Relative URL so it works with any context path
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

                console.log("[JS] response status:", response.status);

                if (response.status === 404) {
                    console.error("[JS] Endpoint not found (404). Check servlet mapping / context path.");
                    setInlineMessage(loginError, 'Server endpoint not found (404)');
                    return;
                }

                const payload = await response.json();
                console.log("[JS] response body:", payload);

                if (!response.ok || !payload.success) {
                    setInlineMessage(loginError, payload.message || 'Sign-in failed');
                    return;
                }

                // success
                const data = payload.data || {};
                const user = {
                    taId: data.taId,
                    name: data.name,
                    username: data.username,
                    role: 'TA',
                    loginAt: data.loginAt
                };
                saveTaUser(user);
                window.location.replace('pages/ta/ta-home.jsp');

            } catch (error) {
                console.error("[JS] Network error:", error);
                setInlineMessage(loginError, 'Could not reach the server. Is the backend running?');
            } finally {
                setLoading(loginSubmit, 'Signing in...', false, 'Continue');
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
                setInlineMessage(registerError, 'Name, username, email, and phone are required');
                return;
            }

            if (!emailPattern.test(email)) {
                markInvalid(registerEmail);
                setInlineMessage(registerError, 'Invalid email format');
                return;
            }

            if (!phonePattern.test(phone)) {
                markInvalid(registerPhone);
                setInlineMessage(registerError, 'Invalid phone number');
                return;
            }

            if (password.length < 6) {
                markInvalid(registerPassword);
                setInlineMessage(registerError, 'Password must be at least 6 characters');
                return;
            }

            if (password !== confirmPassword) {
                markInvalid(registerPassword);
                markInvalid(registerConfirmPassword);
                setInlineMessage(registerError, 'Passwords do not match');
                return;
            }

            setLoading(registerSubmit, 'Registering...', true, 'Register and sign in');

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
                    setInlineMessage(registerError, payload.message || 'Registration failed. Check your details.');
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
                setInlineMessage(registerError, 'Registration request failed. Try again later.');
            } finally {
                setLoading(registerSubmit, 'Registering...', false, 'Register and sign in');
            }
        });
    })();
