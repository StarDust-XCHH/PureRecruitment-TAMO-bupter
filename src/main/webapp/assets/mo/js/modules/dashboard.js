(function () {
    'use strict';

    const moApp = window.MOApp = window.MOApp || {};
    const modules = moApp.modules = moApp.modules || {};

    modules.dashboard = function initDashboard(app) {
        const dashOpenJobs = document.getElementById('dashOpenJobs');
        const dashCandidates = document.getElementById('dashCandidates');
        const dashAccepted = document.getElementById('dashAccepted');
        const dashPending = document.getElementById('dashPending');

        let latestApplicants = [];

        function render() {
            const jobs = typeof app.getJobs === 'function' ? app.getJobs() : [];
            const applicants = Array.isArray(latestApplicants) ? latestApplicants : [];
            const accepted = applicants.filter(function (item) { return item.status === 'Hired'; }).length;
            const pending = applicants.filter(function (item) { return item.status !== 'Hired' && item.status !== 'Not selected'; }).length;

            dashOpenJobs.textContent = String(jobs.length);
            dashCandidates.textContent = String(applicants.length);
            dashAccepted.textContent = String(accepted);
            dashPending.textContent = String(pending);
        }

        app.onApplicantsLoaded = function (items) {
            latestApplicants = items || [];
            render();
        };
        app.loadDashboard = render;

        render();
    };
})();
