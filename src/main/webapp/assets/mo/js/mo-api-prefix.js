(function (w) {
    'use strict';
    var raw = typeof w.__APP_CONTEXT_PATH__ === 'string' ? w.__APP_CONTEXT_PATH__ : '';
    var ctx = raw && raw !== '/' ? raw.replace(/\/$/, '') : '';
    /**
     * @param {string} path 必须以 / 开头，如 /api/mo/jobs
     */
    w.moApiPath = function (path) {
        var p = path || '';
        if (p.charAt(0) !== '/') {
            p = '/' + p;
        }
        return ctx + p;
    };
}(window));
