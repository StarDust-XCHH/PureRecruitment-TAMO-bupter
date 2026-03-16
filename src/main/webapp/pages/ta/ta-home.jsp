<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>TA 首页</title>
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
        }

        * { box-sizing: border-box; }

        body {
            margin: 0;
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 24px;
            font-family: Segoe UI, PingFang SC, Microsoft YaHei, sans-serif;
            background: var(--bg-gradient);
            color: var(--text);
        }

        .panel {
            width: min(720px, 96vw);
            border-radius: 24px;
            padding: 32px;
            background: var(--card);
            border: 1px solid var(--card-border);
            backdrop-filter: blur(14px);
            box-shadow: 0 18px 40px rgba(3, 6, 20, 0.45);
        }

        .title {
            margin: 0 0 10px;
            font-size: 28px;
        }

        .sub {
            color: var(--muted);
            margin-bottom: 24px;
        }

        .grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 16px;
        }

        .item {
            padding: 16px;
            border-radius: 16px;
            background: rgba(255,255,255,0.06);
            border: 1px solid rgba(255,255,255,0.1);
        }

        :root[data-theme='light'] .item {
            background: rgba(15,25,50,0.04);
            border-color: rgba(15,20,35,0.08);
        }

        .label {
            font-size: 12px;
            color: var(--muted);
            margin-bottom: 8px;
        }

        .value {
            font-size: 16px;
            font-weight: 600;
            word-break: break-word;
        }
    </style>
</head>
<body>
<div class="panel">
    <h1 class="title">TA 首页占位页</h1>
    <div class="sub">登录或注册成功后将跳转到此页面，便于验证本次认证移植链路。</div>
    <div class="grid" id="userGrid"></div>
</div>
<script>
    (function () {
        const root = document.documentElement;
        const theme = localStorage.getItem('ta-theme');
        if (theme === 'light') {
            root.setAttribute('data-theme', 'light');
        }

        const raw = sessionStorage.getItem('ta-user') || localStorage.getItem('ta-user');
        const grid = document.getElementById('userGrid');
        let user = null;

        try {
            user = raw ? JSON.parse(raw) : null;
        } catch (error) {
            user = null;
        }

        const entries = user ? Object.entries(user) : [['state', '未检测到 ta-user 登录态']];
        grid.innerHTML = entries.map(function (entry) {
            return '<div class="item"><div class="label">' + entry[0] + '</div><div class="value">' + String(entry[1]) + '</div></div>';
        }).join('');
    })();
</script>
</body>
</html>
