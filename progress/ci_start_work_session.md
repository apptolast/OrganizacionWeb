# CI14 — PR12

Seguimiento read-only de [PR12](https://github.com/apptolast/OrganizacionWeb/pull/12) y [Application CI34068314181](https://github.com/apptolast/OrganizacionWeb/actions/runs/34068314181), evento pull_request, head `3930123fa4504d085fb82195e82587fd1c6c0de2`.

Primera consulta62f360: ejecución iniciada2026-09-06T23:57:33Z, en curso en harness init, instalación y preparación verdes. PR OPEN y head coincidente0eac02. Seguimiento mediante `gh run watch 34068314181 --interval 60 --exit-status`, sin cancelación, push, merge ni modificación de producto. Resultado terminal pendiente; no se atribuye éxito anticipado. Sólo se recogerán diagnósticos concretos, sin publicar logs completos con posibles datos sensibles.

## Resultado terminal

**SUCCESS**, verificado87b6e8/e70fdb. `gh run watch` terminó EXIT0; trabajo verify11min26s, ejecución cerrada2026-09-07T00:09:02Z. Head exacto `3930123fa4504d085fb82195e82587fd1c6c0de2`, sin cambio durante seguimiento.

- Harness init: backend BUILD SUCCESSFUL;29 pruebas de scripts,29PASS/0FAIL; frontend1585PASS en31 archivos.
- Build: correcto.
- E2E Linux bajo xvfb:104PASS,4,2min.
- Publisher smoke: correcto, incluida comprobación real14@s25/@s26 de respuesta perdida, recuperación owner/key, Rabbit retry/publicación y reinicio sin outbox publicado. No se confunde con mutación.
- Todos los pasos de cierre finalizaron correctamente.

Conteos extraídos mediante filtros acotados del log (e70fdb/c55f54), sin copiar logs completos. No hay fallos accionables de producto. Única anotación informativa de Actions: `gradle/actions/setup-gradle@v4` y `pnpm/action-setup@v4` apuntan aNode20 y el runner los ejecuta conNode24; no bloqueó esta ejecución. Su mantenimiento pertenece al arnés, fuera de esta tarea read-only.

Consulta terminal de PR12: sigue OPEN, sin mergeCommit, head coincidente (e70fdb). No existe todavía una fusión que requiera seguir CI de main en este corte. No se canceló, publicó, fusionó ni modificó código. Este resultado certifica la ejecución indicada, no marca feature14done ni sustituye PIT/Stryker o el dictamen final.

## Seguimiento de main después de squash

PR12 fusionada por el coordinador mediante squash `ae861102ff0507e15617ccb1ff830d6e5babcc50`. Nueva [Application CI34068993847](https://github.com/apptolast/OrganizacionWeb/actions/runs/34068993847), evento push a main; head exacto verificado93a385. Primer estado observado c73dcd: init y build verdes, E2E en curso. Seguimiento read-only con `gh run watch 34068993847 --interval 90 --exit-status`, backoff respecto a PR; resultado terminal pendiente. No se extrapola el SUCCESS de PR a este nuevo trabajo.

Main terminal **SUCCESS** f153c6/44b840, watch EXIT0. Head exacto `ae861102ff0507e15617ccb1ff830d6e5babcc50`; ejecución2026-09-07T00:11:10Z–00:25:09Z, verify13min56s. Init/backend/build verdes;29 scripts sin fallos,1585 frontend en31archivos,104E2E(5,3min) y publisher real14@s25/@s26 correctos. Todos los pasos de cierre verdes. Misma advertencia informativa Node20→24 de las acciones externas, sin fallo accionable. Estos conteos proceden del log filtrado de main, no se reutilizan por inferencia desde PR. Seguimiento completado; sin Git mutations ni código editado.
