# MO client scripts

- **`mo-home.js`** — Bootstraps `window.MOApp`: registers modules under `MOApp.modules` and runs them once in order (`settings` → `routeNav` → `modal` → `jobBoard` → `applicants` → `dashboard` → `profile`). Uses a guard flag to avoid double init.

Feature code lives in **`modules/`**; this file only wires the app shell.
