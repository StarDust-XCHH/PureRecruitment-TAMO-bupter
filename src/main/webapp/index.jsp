<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Teaching Assistant Recruitment</title>
    <link rel="stylesheet" href="assets/common/css/index.css">
</head>
<body>
<div class="glow"></div>
<div class="glow two"></div>
<div class="modal-overlay hidden" id="modalOverlay" aria-hidden="true"></div>

<div class="page-shell">
    <div class="auth-shell">
        <section class="card" id="loginCard">
            <button class="theme-toggle theme-toggle-tag" id="themeToggle" type="button" aria-label="Toggle theme" title="Toggle theme">
                <span class="theme-toggle-icon" aria-hidden="true">☾</span>
                <span class="theme-toggle-pulse" aria-hidden="true"></span>
            </button>

            <div class="brand" id="brandTrigger">
                <div class="logo" id="loginLogo">TA</div>
                <div>
                    <h1 id="loginTitle">Teaching Assistant Recruitment</h1>
                    <div class="sub" id="loginSub">Teaching Assistant · Sign in</div>
                </div>
            </div>

            <div class="tabs">
                <button class="tab active" type="button" data-role="TA">TA</button>
                <button class="tab" type="button" data-role="MO">MO</button>
            </div>

            <form id="loginForm">
                <input id="roleInput" name="role" type="hidden" value="TA">

                <div class="field">
                    <label for="username" id="loginIdentifierLabel">Username, email, or phone</label>
                    <input id="username" name="username" type="text" placeholder="Enter username, email, or phone" autocomplete="username">
                </div>

                <div class="field">
                    <label for="password">Password</label>
                    <input id="password" name="password" type="password" placeholder="Enter password" autocomplete="current-password">
                </div>

                <div class="actions">
                    <button class="btn" id="loginSubmit" type="submit">Sign in</button>
                    <a class="link" id="openRegister" href="javascript:void(0)">Sign up</a>
                </div>

                <div class="error" id="loginError" aria-live="polite"></div>
            </form>

            <div class="admin" id="adminPanel">
                <div class="field">
                    <label for="adminAccount">Admin username</label>
                    <input id="adminAccount" type="text" placeholder="Admin username">
                </div>
                <div class="field">
                    <label for="adminPassword">Admin password</label>
                    <input id="adminPassword" type="password" placeholder="Admin password">
                </div>
                <button class="btn alt" id="adminLogin" type="button">Admin Sign in</button>
                <div class="error" id="adminError" aria-live="polite"></div>
            </div>

            <div class="hint" id="adminUnlockHint" hidden aria-live="polite"></div>
        </section>

        <section class="card hidden" id="registerCard">
            <button class="theme-toggle theme-toggle-tag" id="registerThemeToggle" type="button" aria-label="Toggle theme" title="Toggle theme">
                <span class="theme-toggle-icon" aria-hidden="true">☾</span>
                <span class="theme-toggle-pulse" aria-hidden="true"></span>
            </button>

            <div class="brand">
                <div class="logo" id="registerLogo">TA</div>
                <div>
                    <h1 id="registerTitle">Sign up</h1>
                    <div class="sub" id="registerSub">Teaching Assistant</div>
                </div>
            </div>

            <div class="tabs" id="registerRoleTabs" role="tablist" aria-label="Registration account type">
                <button class="tab active" type="button" data-role="TA" role="tab" aria-selected="true">TA</button>
                <button class="tab" type="button" data-role="MO" role="tab" aria-selected="false">MO</button>
            </div>
            <p class="register-role-hint">Switching TA/MO only updates your ID—everything else stays.</p>

            <form id="registerForm" novalidate>
                <input id="registerRoleInput" name="role" type="hidden" value="TA">

                <div class="field-row">
                    <div class="field">
                        <label for="registerId" id="registerIdLabel">Auto-generated account ID</label>
                        <input id="registerId" name="id" type="text" readonly>
                    </div>
                    <div class="field">
                        <label for="registerName">Full name</label>
                        <input id="registerName" name="name" type="text" placeholder="Your name" autocomplete="name">
                    </div>
                </div>

                <div class="field">
                    <label for="registerUsername">Username (unique, used to sign in)</label>
                    <input id="registerUsername" name="username" type="text" placeholder="Choose a username" autocomplete="username">
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
                    <button class="btn alt" id="registerSubmit" type="submit">Sign up</button>
                    <a class="link" id="backToLogin" href="#">Back to Sign in</a>
                </div>

                <div class="error" id="registerError" aria-live="polite"></div>
            </form>
        </section>
    </div>
</div>

<div class="auth-modal-overlay hidden" id="registerConfirmModal" role="dialog" aria-modal="true" aria-labelledby="registerConfirmTitle" aria-hidden="true">
    <div class="auth-modal-panel">
        <h2 class="auth-modal-title" id="registerConfirmTitle">Confirm registration</h2>
        <p class="auth-modal-body" id="registerConfirmMessage"></p>
        <p class="auth-modal-note">Cancel keeps your entries.</p>
        <div class="auth-modal-actions">
            <button type="button" class="btn btn-modal-secondary" id="registerConfirmCancel">Cancel</button>
            <button type="button" class="btn" id="registerConfirmOk">Continue</button>
        </div>
    </div>
</div>

<script src="assets/common/js/index.js"></script>
</body>
</html>
