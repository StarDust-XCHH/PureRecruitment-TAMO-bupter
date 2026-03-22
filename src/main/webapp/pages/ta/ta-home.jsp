<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="en" data-theme="dark">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>TA workspace</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/ta/css/ta-layout.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/ta/css/ta-components.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/ta/css/ta-dashboard-cards.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/ta/css/ta-profile.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/ta/css/ta-settings.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/ta/css/ta-jobs.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/ta/css/ta-status.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/ta/css/ta-modal.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/ta/css/ta-onboarding.css">
</head>
<body>
<div class="app" id="taApp">
    <aside class="sidebar">
        <%@ include file="partials/ta-layout-sidebar.jspf" %>
    </aside>

    <main class="main">
        <%@ include file="partials/ta-layout-topbar.jspf" %>
        <%@ include file="partials/ta-welcome-card.jspf" %>

        <section class="routes">
            <%@ include file="routes/ta-route-profile.jspf" %>
            <%@ include file="routes/ta-route-jobs.jspf" %>
            <%@ include file="routes/ta-route-status.jspf" %>
        </section>
    </main>
</div>

<%@ include file="partials/ta-modals.jspf" %>
<%@ include file="partials/ta-onboarding.jspf" %>

<script src="${pageContext.request.contextPath}/assets/ta/js/modules/settings.js"></script>
<script src="${pageContext.request.contextPath}/assets/ta/js/modules/route-nav.js"></script>
<script src="${pageContext.request.contextPath}/assets/ta/js/modules/modal.js"></script>
<script src="${pageContext.request.contextPath}/assets/ta/js/modules/profile.js"></script>
<script src="${pageContext.request.contextPath}/assets/ta/js/modules/password.js"></script>
<script src="${pageContext.request.contextPath}/assets/ta/js/modules/job-board.js"></script>
<script src="${pageContext.request.contextPath}/assets/ta/js/modules/dashboard-cards.js?v=20260322-en-hero"></script>
<script src="${pageContext.request.contextPath}/assets/ta/js/modules/status.js?v=20260322-en-status"></script>
<script src="${pageContext.request.contextPath}/assets/ta/js/modules/onboarding.js"></script>
<script src="${pageContext.request.contextPath}/assets/ta/js/ta-home.js"></script>
</body>
</html>
