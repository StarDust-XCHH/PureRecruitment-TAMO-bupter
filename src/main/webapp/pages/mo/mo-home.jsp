<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="en" data-theme="dark">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>MO Workspace</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/ta/css/ta-layout.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/ta/css/ta-components.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/ta/css/ta-dashboard-cards.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/ta/css/ta-jobs.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/ta/css/ta-modal.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/mo/css/mo-home.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/ta/css/ta-settings.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/ta/css/ta-profile.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/ta/css/ta-onboarding.css">
</head>
<body>
<div class="app" id="moApp">
    <aside class="sidebar">
        <%@ include file="partials/mo-layout-sidebar.jspf" %>
    </aside>
    <main class="main">
        <%@ include file="partials/mo-layout-topbar.jspf" %>
        <%@ include file="partials/mo-welcome-card.jspf" %>

        <section class="routes" aria-label="MO workspace views" data-i18n-aria-label="routes.regionAria" data-i18n-aria-label-zh="MO 工作台主内容" data-i18n-aria-label-en="MO workspace main content">
            <%@ include file="routes/mo-route-dashboard.jspf" %>
            <%@ include file="routes/mo-route-jobs.jspf" %>
            <%@ include file="routes/mo-route-applicants.jspf" %>
            <%@ include file="routes/mo-route-shortlist.jspf" %>
        </section>
    </main>
</div>

<%@ include file="partials/mo-modals.jspf" %>
<%@ include file="partials/mo-onboarding.jspf" %>

<script>window.__APP_CONTEXT_PATH__ = '${pageContext.request.contextPath}';</script>
<script src="${pageContext.request.contextPath}/assets/mo/js/mo-api-prefix.js"></script>
<script src="${pageContext.request.contextPath}/assets/mo/js/modules/mo-toast.js"></script>
<script src="${pageContext.request.contextPath}/assets/mo/js/modules/settings.js"></script>
<script src="${pageContext.request.contextPath}/assets/mo/js/modules/route-nav.js"></script>
<script src="${pageContext.request.contextPath}/assets/mo/js/modules/modal.js"></script>
<script src="${pageContext.request.contextPath}/assets/mo/js/modules/job-board.js"></script>
<script src="${pageContext.request.contextPath}/assets/mo/js/modules/shortlist-store.js"></script>
<script src="${pageContext.request.contextPath}/assets/mo/js/modules/applicants.js"></script>
<script src="${pageContext.request.contextPath}/assets/mo/js/modules/dashboard.js"></script>
<script src="${pageContext.request.contextPath}/assets/mo/js/modules/profile.js"></script>
<script src="${pageContext.request.contextPath}/assets/mo/js/modules/onboarding.js"></script>
<script src="${pageContext.request.contextPath}/assets/mo/js/mo-home.js"></script>
</body>
</html>
