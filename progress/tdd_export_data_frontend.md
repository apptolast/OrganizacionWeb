# TDD frontend 22: export_data

Checkout exclusivo OrganizacionWeb-export-data; contrato cc78396 aprobado.
Ponytail full/Caveman lite. Init verde proporcionado por root; no se repite.
Sin cambios a COMMON, V14, backend ni Git. Un caso por ciclo; no campaña global.

## Ciclos

1. @s1/@s24, transporte nominal de cuenta vacía: catorce colecciones y counts,
   owner Unicode, archivo con indentación/salto final y bytes exactos. RED
   0e5c2e por módulo ausente. Primer intento a15498 seguía fallando por igualdad
   de Uint8Array entre realms JSDOM/Node, sin diferencia de bytes. El oráculo
   compara ahora todos los bytes como arrays y conserva metadatos exactos;
   GREEN en export_frontend_01_verified.log. El log llamado 01_green conserva
   aquel intento fallido y no acredita un GREEN.
   Cliente deliberadamente parcial: todavía sin validaciones, límite de lectura,
   abortos ni UI. No se presenta este corte como frontera de confianza completa.

2–13. Rechazo de tamaño declarado excesivo, exceso de bytes durante lectura,
   longitud ausente, owner ajeno, envelope adicional, formato/versión ajenos,
   colección ausente, count incoherente, tipo MIME, gzip y ruta en filename.
   Cada caso tuvo RED real y mínimo GREEN antes del siguiente. Logs agrupados
   export_frontend_02_red/green.log hasta export_frontend_13_red/green.log;
   último resultado 13/13, confirmado en 33368b. Reader acotado y cancelación
   del exceso sin retener el lock. Se reutiliza exact; no cambia su fuente.
14–21. Correspondencia de filename con exportedAt, fecha inexistente, UTF-8
   inválido, BOM, Response HTTP rechazada, aborto previo, HTTP tardío y aborto
   durante stream. RED/GREEN en logs 14–21. Último GREEN 9cdc99, 21/21.
   instant se reutiliza sólo para el timestamp del envelope; sus microsegundos
   siguen textuales. La prueba21 primero cerraba el stream tras abortar;
   se ajustó para entregar el chunk y dejar abierto el transporte, permitiendo
   comprobar cancelación sin provocar un enqueue sobre stream ya cancelado.
22–31. GREEN inicial, declarado: 401 tardío no revoca sesión actual; truncamiento;
   aborto esperando datos; límite inclusivo de 33554432 bytes con long textual;
   longitud cero, filename* adicional, colección no array, count fraccionario,
   interrupción y JSON incompleto. Logs 22–31 initial_green, uno por ciclo.
   En23 se hizo explícito el contador final: antes JSON ya rechazaba el padding
   de un archivo truncado; no se presenta como RED. Regresión 31/31 34480c.
32. Revisión root: cabeceras rechazadas dejaban transporte abierto. RED 2b83cd
   con stream sin datos, cancel no llamado; GREEN 44ccde, 32/32. Cancelación
   sin pull/getReader. Cinco oráculos previos ahora observan ausencia de lectura
   y lock liberado: bodyUsed también cambia al cancelar y no era el hecho
   contractual. Response no200 conserva cuerpo intacto para el flujo existente.

## Checkpoint cliente

Dos fuentes únicamente: export-data-api.ts y export-data-api.test.ts. Comandos:
pnpm --dir frontend test src/export-data-api.test.ts; exec tsc --noEmit;
exec eslint de ambos archivos y exec prettier --write de ambos. Evidencia final
export_frontend_client_final_verified.log, client_types_final.log,
client_lint_final.log y client_format_final.log. El primer lint fcb759 detectó
dos escapes innecesarios en el fixture filename*, corregidos sin cambio lógico.
Manifiesto export_frontend_client_freeze.json. Sin suite global ni mutación.

## Ciclos UI y composición

33–43. Un RED y mínimo GREEN por ciclo, logs export_frontend_33 a43:
entrada sin GET (bedb42/314de8); Blob y enlace nativo con bytes originales
(ca4640/ac5014); duplicados y feedback (cbf8da/51c01c); cancelar y HTTP tardío
(58c443/a79ee8);503 manual (a31f3a/a5dee4); desmontaje (551043/0a05ca);
revoke (45cf09/2b50b1); preparar de nuevo (a3f0ff/2e5328); identidad
(a1bb6a/42cdd0);413 explicado (c645a2/83b7ac); navegación (341cf0/599120).
No se usa RouteLink para descargar. El fichero sólo se crea tras validar bytes.

44. Composición SessionGate con identidad real. Los primeros logs red/green
fallaron por una ruta de fixture equivocada (/api/v1/auth/session). Corregida a
/api/session según session-api, se retiró la nueva prop para verificar RED real
f66108 (Página no encontrada), y se repuso: GREEN215c6b. Se preservan ambos
intentos originales sin atribuirles validación. No se altera la API de sesión.
45. Retorno tras login conserva /exportacion:3951b7/a9d351. Sólo nueva ruta
privada; no cambios de persistencia ni resets de formularios ajenos.
46–49. Foco al desaparecer Cancelar, desaparición por413, movimiento voluntario
seguido de blur, entrada por navegación. RED/GREEN respectivos c34b51/7ae8d7,
b6c8af/20f9de,76e61c/b3b788,a070df/379b7b.
50. Ana → logout → Bruno →401 tardío, composición real SessionGate/apiRequest:
GREEN inicial1b9294. No petición adicional ni revocación de Bruno.
51. Intento de simular blur automático falló en la precondición: JSDOM conserva
foco sobre el botón disabled al invocar blur. Logs51_red y51_green preservan
ese límite, no acreditan defecto del producto. Se retiró la modificación de foco
propuesta y el caso se ajustó a lo observable en DOM: conserva el control que
sigue enfocado, GREEN inicial ef66cf. El comportamiento físico queda para UX.
También se hace explícita la guarda local tras await del cliente antes de Blob;
el mismo controller/identidad protege finalmente el resultado y los errores.
52–55. GREEN inicial, uno por ciclo:401 vigente retira acceso (8c0afa), respuesta
incompatible no crea URL (12ea1b), reutilizar enlace conserva archivo sin GET
(62e8f6), JSON de error resuelto después de cancelar no restaura fallo (34adb6).
56. Revisión root corrigió el h1 contractual y ayuda:8a27ee/46bc9e. Nombre final
«Exportar mis datos»; enlace de navegación «Exportación». Se explica JSON
versionado, archivo personal e importación todavía no disponible.
57. Revisión root: cleanup pasivo dejaba ventana hasta layout del nuevo owner.
El caso de identidad previo ahora observa revoke desde un probe de layout:
RED36747d, GREENe05ed1. Cleanup de layout aborta y revoca antes del nuevo owner.

## Corte funcional para revisión

Cliente aprobado en832331f, sin cambios posteriores. UI22 +cliente32 y
regresión App/auth:143/143 (aa6cee y focal_verified). Tipos52570e y lint5ee54c
EXIT0; formato5d37ab. Se añadió después una guarda simétrica antes de consumir
el error tardío; focal_verified conserva143. Build frontend registrado en
export_frontend_ui_build.log. SCSS local usa tokens existentes, áreas44px,
wrap y separación; sólo revisión de código hasta UX real, sin afirmar geometría.
Manifiesto export_frontend_ui_freeze.json. Ninguna suite global/Java/mutación.

## Mapa y pendientes

@s1/@s24: bytes nominales y frontera máxima. @s25: variantes de transporte y
envelope cubiertas; @s28: Response preservada; @s29: abortos/401 tardío cliente.
@s2–21/@s33: servidor/contrato HTTP son responsabilidad de A/C; el cliente no
reinterpreta registros de negocio. @s22–29: composición, descarga y lifecycle
acreditados en DOM; @s30 memoria local por diseño (refs, Blob/objectURL y cleanup,
sin llamadas a almacenamiento web), pendiente observación E2E. @s31 foco DOM
acreditado, comportamiento físico de disabled pendiente. @s32 UX real pendiente.
Todavía no hay E2E/UX ni mutación frontend22 ni gate global de este corte.

## Browser, scope and final regression checkpoint

Physical focus RED 8844ac; same keyboard preparation GREEN c5643f after restoring the connected initiating control only while focus remains on BODY. Voluntary focus movements remain covered in three engines. The prepared native download uses existing primary-link styling; prepare again is secondary, reviewed visually. UX matrix and explicit simulated API limits: ux_export_data.md.

Dispatcher target RED 570e12 / GREEN 79ec59; missing config RED 8b7082 / GREEN df6a20. Full harness 63/63 after AST remapping historical Appearance nodes and retaining exact startsWith assertions. Default Workspace is a reviewed superset. No historical campaign executed.

Global initial 2294/2295 failed only the legacy last-navigation expectation; contract 22 adds Exportación after Apariencia. Authorized fixture correction preserves Hoy first and remaining assertions. Final global 2295/2295 in 56 files (cbb659), 170 inputs unchanged through test/lint/build. Initial lint failed only formatting export-data.test.tsx; Prettier correction changes no oracle and is listed separately in export_frontend_final_freeze.json. Build/tsc EXIT0. No repeated global for formatting alone.

## Original mutation and integrated E2E preparation

Original campaign EXIT0 d6af28, 7 min 27 s: 261 Killed / 60 Survived / 4 NoCoverage / 1 Timeout, 326 total. Conservative 261/326=80.06134969%, no reclassification. 170 inputs unchanged. Full inventory, raw JSON/HTML, before/after and analysis preserved in export_stryker_original_freeze.json. No replay or refinement executed.

First integrated E2E in e2e/export-data.spec.mjs is prepared, syntax and format checked only (fbb46c). It uses real HTTP login and project/task writes, a foreign-owner SQL fixture, byte-exact native downloads and owner-scoped cleanup. No server started and no test result claimed until the coordinated fixed backend stack is available.

## Post-campaign refinement authorized by root

Original residue 300 motivated one public overlap oracle in the existing cancellation test. A is cancelled, B begins, then A resolves; B remains busy and cancelable, its signal remains current and no old Blob/third GET appears. B then completes normally. All earlier cancellation assertions remain. Initially GREEN f79d9b; no fake RED or production edit. Focal client+view and formatting/lint logs: export_overlap_focal_final.log, export_overlap_lint.log, export_overlap_format_check.log. Original campaign untouched and no replay executed.

Integrated E2E header comparison now accepts UTF-8 case variations using the same anchored semantic media-type check as the client. C already observed uppercase UTF-8 on the real server. This E2E has not yet run, so this is fixture preparation, not a claimed RED/GREEN integration result. Freeze export_overlap_freeze.json records both test hashes and the explicit post-campaign delta.

## First integrated browser run on fixed product

Detached checkout work/OrganizacionWeb-export-e2e from ee4ca8d, dependencies installed from frozen locks, 18080 confirmed free. Existing scripts/e2e.mjs owns creation and cleanup. First run RED 422af3: raw text assertion received aria-hidden icon plus Hoy; accessible name was correct. Corrected only first/last navigation assertions to toHaveAccessibleName, preserving order. All 1913 versioned inputs unchanged during that original run.

Same real nominal GREEN 1/1 e52761 / EXIT0 2cfea5 after transfer of that test only. Product remains ee4ca8d. HTTP-created project and task, explicit foreign-owner PG fixture, 14 collection counts, owner exclusion, 1332 original bytes equal both native downloads, one export GET and no view writes. Own rows/event payloads unchanged. Leaving revokes the Blob URL, verified by its fetch rejection. Runner removed containers/network/volume; port 18080 free (72e40b).

Original and second logs/contexts, byte metadata and hashes are in export_e2e_nominal_freeze.json. Only the E2E fixture differs from the baseline; no production edit. This does not attribute final backend guard/performance validation to the nominal cutoff.

## Official complete E2E on final backend cutoff

Detached e9350e9, locks identical and no reinstall. Existing scripts/e2e.mjs executed all157 cases. Original EXIT1 (307fd3):154PASS,2FAIL,1SKIP in24.2min. Both failures are inherited responsive matrices timing out at180000ms in page.setViewportSize: end-time-notification-ux:330 and pause-resume-session:278. No failed business assertion. All export cases executed in this run pass; native zoom is explicitly restricted to the named Chromium project and has separate prior evidence.

All1924 versioned inputs unchanged after run (c6cdce). Own stack52200/network/volume removed,18080free. Logs, contexts, byte metadata and hashes preserved in export_e2e_global_freeze.json. No source/config/test changes, timeout increases or automatic replay. Await resource release before authorized same-code directed replay; it cannot retroactively make the original global GREEN.

## Directed same-code replay after resource review

Root changed sequence based on measured available CPU/RAM after global init/build ended, while one PIT minion remained; no claim of an idle machine. Same e9350e9 and1924 inputs, no changes to config/timeouts/oracles. Directed runner selected only end-time:330 and pause:278. EXIT1 ebd275: end-time GREEN in2.8min with155 measurements; pause timed out180s again at setViewportSize after139 measurements, query-error through701px. No geometry assertion failed.

Static diagnostic counts: completed end-time executes60,047 pair expects plus17,236 box expects; partial pause executes50,463 pair expects plus15,260 box expects. This suggests runner assertion overhead, but no phase timing was recorded and causality is not yet established. No timeout increase or further retry. Proposal to root: measure phases or aggregate the same violation predicates into one empty-list assertion per width, preserving every comparison and evidence.

Before/after1924 identical, stack3840 removed and18080free. Directed original log/context/geometry preserved independently in export_e2e_directed_freeze.json. This does not change the original global result.

## Geometric assertion instrumentation, authorized bounded repair

Root reviewed both inspectors and authorized node:assert/strict for their purely synchronous numeric/overlap checks. Only six calls per inspector and one import changed. Positive <=/>= predicates preserve NaN rejection; all values/tolerances/messages, nested pairs,31widths,5states, async locator/poll assertions, font closeTo, screenshots, axe and180s timeout remain. Equivalence evidence covers66 numeric combinations and2booleans (the first console label incorrectly said198; JSON count is66).

Original global and same-code directed failures remain preserved. After this isolated instrumentation change, both matrices pass in8.3s each,2/2GREEN18.7s total (1df4e6/663dcb), each155measurements. This controlled delta supports Playwright step instrumentation overhead as the source of the time budget exhaustion; no product performance fix is claimed. No further replay or timeout change. Before/after1924 inputs identical through repaired run; only the two authorized E2E files differ from e9350e9. Stack41992/network/volume removed; evidence freeze export_geometry_assert_freeze.json. Files await root review before transfer to primary.

## Final complete official E2E gate

Root authorized one complete rerun after PIT ended. Isolated product/E2E/runner/Docker files were verified equal to38f1229; no additional edit or reinstall. Official scripts/e2e.mjs completed EXIT0 (589cf8):156PASS,0FAIL,1SKIP of157 cases in18.2min. The skip is the already documented named-Chromium native zoom case with separate successful visible evidence.

Both repaired matrices pass in the full sequence (end-time10.3s, pause10.4s), preserving180s timeout and all checks. Exportation real and simulated cases pass.1924 inputs unchanged (60cf02). Stack17444/network/volume removed;18080free. Compact log/before/after/exit manifest: export_e2e_final_global_freeze.json. Earlier failed runs remain preserved separately; no geometries duplicated and no automatic replay.
