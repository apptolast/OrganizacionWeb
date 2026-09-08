# Frontend brief — OrganizacionWeb

Generado el 8 de septiembre de 2026 por un agente Explore para orientar a los implementadores de 24–30. Verificar rutas antes de usarlas.

## 1. frontend/src layout
Flat, no subfolders. All files at frontend/src/.

Routing (no router library):
- `navigation.tsx` — `useRoute()` = `useSyncExternalStore(subscribe, () => location.pathname + location.search)`, subscribing to `popstate`; `RouteLink` intercepts plain left-clicks, does `history.pushState` + dispatches a synthetic `PopStateEvent`. Also exports `isProjectRoute`.
- `App.tsx` — routes are a single nested ternary chain over regex/equality tests on `route` (`/`, `/proyectos/nuevo`, `/revision-semanal`, `/historial`, `/disponibilidad`, `/apariencia`, `/exportacion`, `/importacion`, `/proyectos/:id/editar`, `/proyectos/:p/tareas/:t`, `/proyectos/:p/tareas/:t/sesiones/:s`, else 404). It also computes the `section` label string passed to `Workspace`. La feature 24 (rama `codex/integration-api`) añade `/integraciones`.
- `workspace.tsx` — sidebar shell + `<nav aria-label="Principal">` with hardcoded `RouteLink`s; active item via `aria-current={section === "X" ? "page" : undefined}`. Adding a page = add route branch + `section` label in App.tsx, add union member to `Workspace`'s `section` prop type, add a `RouteLink` in the nav.
- `main.tsx` — `createRoot` → `<StrictMode><SessionGate/></StrictMode>`, imports `./styles.scss`.
- `session-gate.tsx` — auth gate; when authenticated renders `<AppearanceProvider key={session.username}><App .../></AppearanceProvider>` plus CSRF-recovery banner.

API client:
- `api-client.ts` — `apiRequest(url, options)` wraps `fetch`; for non-GET it sets `X-CSRF-TOKEN` from a module-level token (`setCsrfToken`). `observeAccess(listener)` registers a 401/403 callback; 401 fires it, and 403 fires it only when `isCsrfFailure` sees a JSON body with `code === "CSRF_INVALID"`. Aborted signals suppress callbacks. No JSON parsing/throwing here.
- Per-feature `*-api.ts` modules do their own parsing and error mapping: they call `apiRequest`, `throw response` on unexpected status, validate the payload shape (`exact`/`instant` helpers exported from `schedule-block-api.ts`), and throw typed errors (e.g. `AppearanceValidationError` mapping RFC7807 `errors[]` → per-field messages). Modules: appearance-api, export-data-api, import-data-api, customization-api, custom-fields-api, today-api, history-api, weekly-review-api, projects-api, tasks-api, task-status-api, availability-api, reschedule-api, schedule-block-api, session-api, work-session*-api, read-projects-api, edit-project-api.

State/hooks:
- No Redux/Zustand/React Query. React 19 only (react ^19.2.8).
- Context providers: `appearance-state.tsx` (`AppearanceProvider` / `useAppearance`), `customization-state.ts` (`useCustomizationSession`).
- `use-*.ts` hooks colocated: use-session, use-create-project, use-edit-project, use-project-status, use-project-tasks, use-read-projects, use-work-session-decision.
- Strong convention: `AbortController` refs for in-flight read/write, `signal.throwIfAborted()`, `useLayoutEffect` focus management (focus `main h1` when the active element was dropped).

SCSS & theming:
- Tokens live at the top of `styles.scss` (lines 1–49): `@mixin light-appearance` and `@mixin dark-appearance` defining `--canvas --panel --editable --sidebar --selection --hover --ink --muted --line --control-border --error --warning --success --brand --accent --on-accent --native-scheme`.
- Applied as: `:root, [data-theme="light"] { @include light-appearance; }`, `@media (prefers-color-scheme: dark) { :root:not([data-theme]) { @include dark-appearance; } }`, `[data-theme="dark"] { @include dark-appearance; }`. So: data-attribute wins, media query is the SYSTEM fallback.
- JS side (`appearance-state.tsx` lines 125–154): on snapshot change it matchMedia's `(prefers-color-scheme: dark)` only when `theme === "SYSTEM"`, then sets `document.documentElement.dataset.theme`, `style.colorScheme`, and `--accent` (accentDark vs accentLight). Re-applies on media `change` and on `visibilitychange`; cleanup deletes all three.
- Other stylesheets: today.scss (imported by today.tsx), history.scss (history.tsx), work-session.scss (work-session.tsx, work-session-reader.tsx). No CSS modules, no Tailwind.
- Appearance feature files: appearance.tsx, appearance-state.tsx, appearance-api.ts, appearance.test.tsx, appearance-api.test.ts; styles under `.appearance` in styles.scss; e2e appearance.spec.mjs, appearance-persistence.spec.mjs, appearance-ux-audit.spec.mjs; stryker.appearance.config.json + stryker.appearance-replay.config.json.

## 2. Complete feature: export_data
- Page/component: `export-data.tsx` (`ExportData({owner})` → inner `ExportPreparation`, object-URL download, focus mgmt, failure states "temporary" | "limit").
- API module: `export-data-api.ts` (`readExportData`).
- Unit tests: `export-data.test.tsx`, `export-data-api.test.ts`.
- SCSS: none dedicated — styled from styles.scss.
- Stryker: `frontend/stryker.export-data.config.json`.
- e2e: `e2e/export-data.spec.mjs`, `e2e/export-data-browser.spec.mjs`, cross-browser config `e2e/export-data-browser.config.mjs`.
- Wiring: App.tsx line 15 import, line 32 `const exportData = route === "/exportacion"`, section label "Exportación" (line 43), render branch `exportData && username ? <ExportData owner={username}/>` (line 70); nav entry in workspace.tsx lines 82–87 (`href="/exportacion"`), plus `"Exportación"` in the `section` union (line 17).

## 3. Test conventions
- Unit: Vitest (`vitest run` via frontend `pnpm test`) + React Testing Library + user-event + jest-dom. Config `frontend/vite.config.ts`: `include: ["src/**/*.test.{ts,tsx}"]`, `environment: "jsdom"`, `setupFiles: "./src/test-setup.ts"`, `restoreMocks: true`. Setup file only imports jest-dom and runs RTL `cleanup()` in `afterEach`.
- Naming: colocated `<feature>.test.tsx` / `<feature>-api.test.ts` next to source; mutation-targeted extras use `<feature>.mutation.test.tsx`. Shared fixtures: `today-fixture.ts`, `frontend/test-fixtures/customization.ts`.
- Tests carry scenario tags in titles, e.g. `it("@s24 follows a system color-scheme change …")`.
- a11y: no axe in unit tests — axe runs in Playwright via `@axe-core/playwright` `new AxeBuilder({page}).withTags(["wcag2a","wcag2aa","wcag21aa","wcag22aa","best-practice"])`, used in ~20 specs.
- Responsive: `page.setViewportSize` swept over breakpoint arrays including 320/768/1280/1440 (e.g. `e2e/appearance-ux-audit.spec.mjs:13-14`); convention `height: width === 768 ? 400 : 900` and a 320-px no-horizontal-scroll assertion, plus native-zoom-200% specs and text-200% specs.
- e2e harness: `pnpm test:e2e [spec]` → `scripts/e2e.mjs`. It mkdtemps `.e2e-work/run-*`, writes a `test.env`, `docker compose ... -p organizationweb-e2e-<pid> up --build -d --wait`, polls `/api/session`, then runs `pnpm exec playwright test <args>`; `finally` always runs `compose down --volumes --remove-orphans`. Desde el 8 de septiembre acepta `E2E_WEB_PORT` (por defecto 18080) para ejecutar varias pilas en paralelo.
- `playwright.config.mjs`: testDir ./e2e, fullyParallel false, workers 1, retries 0, 30 s timeout, baseURL `E2E_BASE_URL ?? http://127.0.0.1:18080`. Cross-browser via per-feature config files that spread the base and fan out chromium/firefox/webkit.
- Fixtures/cleanup: `e2e/support/authenticated-test.mjs` exports an extended `test` with an auto `authenticated` fixture that logs in via `scripts/session-client.mjs` and cleans the owner's rows before and after each test (`sql()` from `e2e/support/projects.mjs`). Other helpers: `e2e/support/backend.mjs` (`restartBackend`), blocks.mjs, tasks.mjs, today.mjs.
- Adding a spec: create `e2e/<feature>.spec.mjs`, import `{ test, expect }` from `./support/authenticated-test.mjs`, use relative paths against baseURL. `scripts/project.mjs` `lint` only syntax-checks a fixed list of .mjs files.

## 4. Stryker
Full config `frontend/stryker.export-data.config.json`:
```json
{
  "$schema": "./node_modules/@stryker-mutator/core/schema/stryker-schema.json",
  "testRunner": "vitest",
  "plugins": ["@stryker-mutator/vitest-runner"],
  "ignorePatterns": [".stryker-tmp-availability-replay"],
  "mutate": [
    "src/App.tsx:29:8-29:45", "src/App.tsx:37:8-51:28", "src/App.tsx:54:7-97:7",
    "src/workspace.tsx:81:10-86:22", "src/session-gate.tsx:44:10-44:37",
    "src/use-session.ts:190:6-190:29", "src/export-data-api.ts", "src/export-data.tsx"
  ],
  "reporters": ["clear-text", "json", "html", "progress-append-only"],
  "thresholds": { "high": 90, "low": 80, "break": 80 },
  "concurrency": 8,
  "coverageAnalysis": "perTest",
  "vitest": { "configFile": "vite.config.ts" },
  "jsonReporter": { "fileName": "reports/mutation-export-data/mutation.json" },
  "htmlReporter": { "fileName": "reports/mutation-export-data/mutation.html" },
  "tempDirName": ".stryker-tmp-export-data"
}
```
Note the line:col range syntax used to scope shared files (App.tsx/workspace.tsx) to the feature's own branches.

Mapping in `scripts/project.mjs` (`createProject`): `main(args)` takes `[task, target]`; a non-empty target is rejected unless task === "mutate" AND target is in the whitelist array (top of `createProject`). Each frontend target is an `if (task === "mutate" && target === "<x>-frontend")` block running `pnpm --dir frontend exec stryker run stryker.<feature>.config.json`. Backend targets call `gradlew pitest -PmutationScope=<scope>`. To add a target: (a) add the string to the whitelist, (b) add an `if` block, (c) add `frontend/stryker.<feature>.config.json`, (d) add the case to `scripts/project.test.mjs`.

## 5. Dark mode
Files defining dark colors:
- `styles.scss` lines 20–37 (`@mixin dark-appearance`), applied at lines 43–49.
- `appearance-api.ts` lines 22–29 `darkSurfaces` (contrast validation reference set, mirrors the SCSS dark tokens) and lines 14–21 `lightSurfaces`; defaults `#244C3C` / `#B7E4C7`.
- `appearance.tsx` renders two static preview sections with literal `data-theme="light"` and `data-theme="dark"`.
Mechanism: `data-theme` attribute on `<html>` set from the persisted preference; `prefers-color-scheme: dark` only applies to `:root:not([data-theme])`. Runtime accent injected as inline `--accent` on `<html>`.

Known gaps (hardcoded colors not using tokens), pendientes de verificación visual:
- `styles.scss` — 6 occurrences outside the token mixins: line 646 `#2b3f2410`, 856 `#7a9863`, 867 `#a8bb88`, 877 `#7d9b61`, 886 `#b3c59e`, 891 `#cad9bb`, plus shadows at 1192 `#2b3f2407` and 1482 `#23392f08`. The 856–891 greens are the "seed art" decoration.
- `today.scss` — 6 occurrences (lines 21 `#b8c8b8`, 23 `#f0f3eb`, 30 `#e0e5dc`, 32 `#fff`, 47 `#fff`, 48 `#e0e5dc`). Light-only values with no dark counterpart — the clearest dark-mode gap.
- `history.scss` — 0. `work-session.scss` — 0.
- `frontend/index.html` line 6: `<meta name="theme-color" content="#f8f9f5">` is light-only (no dark `media` variant).
