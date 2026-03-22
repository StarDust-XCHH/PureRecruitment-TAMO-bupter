<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>TA Recruitment System</title>
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
                <div class="logo">TA</div>
                <div>
                    <h1>TA Recruitment System</h1>
                    <div class="sub">Choose how you want to sign in</div>
                </div>
            </div>

            <div class="tabs">
                <button class="tab active" type="button" data-role="TA">TA sign-in</button>
                <button class="tab" type="button" data-role="MO">MO sign-in</button>
            </div>

            <form id="loginForm">
                <input id="roleInput" name="role" type="hidden" value="TA">

                <div class="field">
                    <label for="username">Username, email, or phone</label>
                    <input id="username" name="username" type="text" placeholder="Enter username, email, or phone" autocomplete="username">
                </div>

                <div class="field">
                    <label for="password">Password</label>
                    <input id="password" name="password" type="password" placeholder="Enter password" autocomplete="current-password">
                </div>

                <div class="actions">
                    <button class="btn" id="loginSubmit" type="submit">Continue</button>
                    <a class="link" id="openRegister" href="jsp/register.jsp">Create account</a>
                </div>

                <div class="error" id="loginError" aria-live="polite"></div>
            </form>

            <div class="admin" id="adminPanel">
                <div class="field">
                    <label for="adminAccount">Admin account</label>
                    <input id="adminAccount" type="text" placeholder="Admin username">
                </div>
                <div class="field">
                    <label for="adminPassword">Admin password</label>
                    <input id="adminPassword" type="password" placeholder="Admin password">
                </div>
                <button class="btn alt" id="adminLogin" type="button">Admin sign-in</button>
                <div class="error" id="adminError" aria-live="polite"></div>
            </div>

            <div class="hint">Tap the title five times to reveal the admin entry</div>
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
                    <div class="sub">Become a Teaching Assistant</div>
                </div>
            </div>

            <form id="registerForm">
                <div class="field-row">
                    <div class="field">
                        <label for="registerTaId">TA ID (auto-generated)</label>
                        <input id="registerTaId" name="taId" type="text" readonly>
                    </div>
                    <div class="field">
                        <label for="registerName">Full name</label>
                        <input id="registerName" name="name" type="text" placeholder="Your name" autocomplete="name">
                    </div>
                </div>

                <div class="field">
                    <label for="registerUsername">Username (unique, used to sign in)</label>
                    <input id="registerUsername" name="username" type="text" placeholder="Choose a unique username" autocomplete="username">
                </div>

                <div class="field-row">
                    <div class="field">
                        <label for="registerEmail">Email</label>
                        <input id="registerEmail" name="email" type="email" placeholder="Email address" autocomplete="email">
                    </div>
                    <div class="field">
                        <label for="registerPhone">Phone</label>
                        <input id="registerPhone" name="phone" type="tel" placeholder="Phone number" autocomplete="tel">
                    </div>
                </div>

                <div class="field-row">
                    <div class="field">
                        <label for="registerPassword">Password</label>
                        <input id="registerPassword" name="password" type="password" placeholder="At least 6 characters" autocomplete="new-password">
                    </div>
                    <div class="field">
                        <label for="registerConfirmPassword">Confirm password</label>
                        <input id="registerConfirmPassword" name="confirmPassword" type="password" placeholder="Re-enter password" autocomplete="new-password">
                    </div>
                </div>

                <div class="actions">
                    <button class="btn alt" id="registerSubmit" type="submit">Register and sign in</button>
                    <a class="link" id="backToLogin" href="#">Back to sign-in</a>
                </div>

                <div class="error" id="registerError" aria-live="polite"></div>
            </form>
        </section>
    </div>
</div>

<script src="assets/common/js/index.js"></script>
</body>
</html>

