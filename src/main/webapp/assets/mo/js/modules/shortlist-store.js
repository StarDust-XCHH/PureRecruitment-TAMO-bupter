(function () {
    'use strict';

    /** 与 applicants 模块一致：服务端短名单条目缓存（由 syncFromServer 填充） */
    var _items = [];

    function apiUrl(path) {
        var p = path.charAt(0) === '/' ? path : '/' + path;
        if (typeof window.moApiPath === 'function') {
            return window.moApiPath(p);
        }
        return '../../' + p.replace(/^\//, '');
    }

    function norm(s) {
        return String(s == null ? '' : s).trim();
    }

    function sameMo(a, b) {
        return norm(a).toLowerCase() === norm(b).toLowerCase();
    }

    function sameJobId(a, b) {
        return norm(a).toLowerCase() === norm(b).toLowerCase();
    }

    /**
     * MB-35：候选短名单由服务端 `mo-applicant-shortlist.json` 持久化；不修改 TA 申请状态。
     * 分桶与 API 以 {@code jobId} 为准（同课号多岗位）。
     */
    window.MoShortlistStore = {
        /**
         * 从服务器拉取当前 MO 的全部短名单行，写入内存缓存。
         */
        syncFromServer: async function (moId) {
            var m = norm(moId);
            if (!m) {
                _items = [];
                return;
            }
            var res = await fetch(apiUrl('/api/mo/applicants/shortlist') + '?moId=' + encodeURIComponent(m), {
                headers: { Accept: 'application/json' }
            });
            var payload = await res.json();
            if (!res.ok || payload.success === false) {
                _items = [];
                var msg = payload.message || payload.error || 'Shortlist sync failed';
                throw new Error(typeof msg === 'string' ? msg : 'Shortlist sync failed');
            }
            _items = Array.isArray(payload.items) ? payload.items.map(function (row) {
                return {
                    moId: row.moId,
                    jobId: row.jobId,
                    courseCode: row.courseCode,
                    applicationId: row.applicationId,
                    taId: row.taId,
                    name: row.name,
                    addedAt: row.addedAt
                };
            }) : [];
        },

        /** 某 MO 下、某 {@code jobId} 岗位上的短名单行 */
        listForJob: function (moId, jobId) {
            var m = norm(moId);
            var j = norm(jobId);
            if (!m || !j) return [];
            return _items.filter(function (row) {
                return sameMo(row.moId, m) && sameJobId(row.jobId, j);
            }).map(function (r) {
                return {
                    applicationId: r.applicationId,
                    jobId: r.jobId,
                    courseCode: r.courseCode,
                    taId: r.taId,
                    name: r.name,
                    addedAt: r.addedAt
                };
            });
        },

        /** 有短名单条目的岗位 {@code jobId} 列表（排序） */
        jobIdsWithEntries: function (moId) {
            var m = norm(moId);
            var seen = {};
            _items.forEach(function (row) {
                if (!sameMo(row.moId, m)) return;
                var jid = norm(row.jobId);
                if (jid) seen[jid] = true;
            });
            return Object.keys(seen).sort(function (a, b) {
                return a.localeCompare(b, undefined, { sensitivity: 'base' });
            });
        },

        totalCount: function (moId) {
            var m = norm(moId);
            if (!m) return 0;
            return _items.filter(function (row) {
                return sameMo(row.moId, m);
            }).length;
        },

        isShortlisted: function (moId, jobId, applicationId) {
            var aid = norm(applicationId);
            var j = norm(jobId);
            if (!aid || !j) return false;
            return window.MoShortlistStore.listForJob(moId, j).some(function (r) {
                return norm(r.applicationId) === aid;
            });
        },

        add: async function (moId, entry) {
            var m = norm(moId);
            var jid = norm(entry && entry.jobId);
            var aid = norm(entry && entry.applicationId);
            if (!m || !jid || !aid) return false;
            var res = await fetch(apiUrl('/api/mo/applicants/shortlist'), {
                method: 'POST',
                headers: { 'Content-Type': 'application/json;charset=UTF-8' },
                body: JSON.stringify({
                    moId: m,
                    jobId: jid,
                    applicationId: aid,
                    taId: norm(entry.taId),
                    name: norm(entry.name)
                })
            });
            var payload = await res.json();
            if (!res.ok || payload.success === false) {
                throw new Error(payload.message || 'Add to shortlist failed');
            }
            await window.MoShortlistStore.syncFromServer(m);
            return true;
        },

        remove: async function (moId, applicationId) {
            var m = norm(moId);
            var aid = norm(applicationId);
            if (!m || !aid) return false;
            var q =
                '?moId=' + encodeURIComponent(m)
                + '&applicationId=' + encodeURIComponent(aid);
            var res = await fetch(apiUrl('/api/mo/applicants/shortlist') + q, {
                method: 'DELETE',
                headers: { Accept: 'application/json' }
            });
            var payload = await res.json();
            if (!res.ok || payload.success === false) {
                throw new Error(payload.message || 'Remove from shortlist failed');
            }
            await window.MoShortlistStore.syncFromServer(m);
            return true;
        },

        /**
         * @returns {Promise<'added'|'removed'>}
         */
        toggle: async function (moId, entry) {
            var jid = norm(entry && entry.jobId);
            var aid = norm(entry && entry.applicationId);
            if (window.MoShortlistStore.isShortlisted(moId, jid, aid)) {
                await window.MoShortlistStore.remove(moId, aid);
                return 'removed';
            }
            await window.MoShortlistStore.add(moId, entry);
            return 'added';
        }
    };
})();
