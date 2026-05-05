(function () {
    'use strict';

    var host = null;
    var hideTimer = null;
    var currentEl = null;

    function ensureHost() {
        if (host && host.parentNode) return host;
        host = document.createElement('div');
        host.id = 'moToastHost';
        host.className = 'mo-toast-host';
        host.setAttribute('aria-live', 'polite');
        (document.body || document.documentElement).appendChild(host);
        return host;
    }

    function hideToast(el, skipAnim) {
        if (!el) return;
        clearTimeout(hideTimer);
        hideTimer = null;
        function removeNode() {
            if (el.parentNode) el.parentNode.removeChild(el);
            if (currentEl === el) currentEl = null;
        }
        if (skipAnim) {
            removeNode();
            return;
        }
        el.classList.remove('mo-toast--in');
        el.classList.add('mo-toast--leave');
        setTimeout(removeNode, 320);
    }

    /**
     * 顶部居中轻提示：不挡下层点击（宿主 pointer-events: none；条本身可点撤回）。
     * @param {Object} opts
     * @param {string} opts.message
     * @param {'success'|'info'} [opts.type]
     * @param {{ label?: string, action: () => (void|Promise<void>) }} [opts.undo]
     * @param {number} [opts.durationMs] 默认 5000
     */
    function show(opts) {
        opts = opts || {};
        var type = opts.type === 'info' ? 'info' : 'success';
        var message = opts.message != null ? String(opts.message) : '';
        var undo = opts.undo;
        var durationMs = typeof opts.durationMs === 'number' && opts.durationMs > 0 ? opts.durationMs : 5000;

        ensureHost();
        if (currentEl) {
            hideToast(currentEl, true);
        }

        var el = document.createElement('div');
        el.className = 'mo-toast mo-toast--' + type;
        el.setAttribute('role', 'status');

        var icon = document.createElement('span');
        icon.className = 'mo-toast__icon';
        icon.setAttribute('aria-hidden', 'true');
        icon.textContent = type === 'info' ? '!' : '✓';

        var body = document.createElement('div');
        body.className = 'mo-toast__body';

        var text = document.createElement('span');
        text.className = 'mo-toast__text';
        text.textContent = message;
        body.appendChild(text);

        if (undo && typeof undo.action === 'function') {
            var undoBtn = document.createElement('button');
            undoBtn.type = 'button';
            undoBtn.className = 'mo-toast__undo';
            undoBtn.textContent = undo.label != null ? String(undo.label) : 'Undo';
            undoBtn.addEventListener('click', function (e) {
                e.preventDefault();
                e.stopPropagation();
                clearTimeout(hideTimer);
                hideTimer = null;
                Promise.resolve()
                    .then(function () { return undo.action(); })
                    .then(function () { hideToast(el, false); })
                    .catch(function (err) {
                        var msg = err && err.message ? String(err.message) : '';
                        if (msg) window.alert(msg);
                        hideTimer = setTimeout(function () { hideToast(el, false); }, durationMs);
                    });
            });
            body.appendChild(undoBtn);
        }

        el.appendChild(icon);
        el.appendChild(body);
        host.appendChild(el);
        currentEl = el;

        requestAnimationFrame(function () {
            el.classList.add('mo-toast--in');
        });

        if (!undo) {
            hideTimer = setTimeout(function () {
                hideToast(el, false);
            }, durationMs);
        } else {
            hideTimer = setTimeout(function () {
                hideToast(el, false);
            }, durationMs);
        }
    }

    function dismiss() {
        if (currentEl) hideToast(currentEl, false);
    }

    window.MoToast = { show: show, dismiss: dismiss };
})();
