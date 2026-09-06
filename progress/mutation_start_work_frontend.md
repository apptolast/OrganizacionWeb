# Mutación frontend14 — campaña inicial

**Veredicto de umbral: PASS.** 452/533 = 84,8030%; umbral80. EXIT0. Quedan81Survived pendientes de dictamen; no se excluye ninguno del score bruto.

Autorización coordinador tras revisión `bc9b16`; producto aprobado y congelado `46913a7`, diez hashes revisados `d16bca`. Configuración focal API/UI completas y composición TaskReader124–135, umbral80, perTest, concurrency8. Rol mutation_tester leído `2a3ec4`, protocolo `238007`.

Comando: `node .harness/harness.mjs mutate start_work_session-frontend`, sesión42307, arranque `3d11d8`. Before88archivos registrado a2026-09-06T22:59:27.5935190Z en `mutation_start_work_frontend_before.json`; incluye todas las fuentes/pruebas frontend y configuraciones/ejecutores conocidos. Log íntegro `mutation_start_work_frontend.log`.

Instrumentación observada `f871f9`: tres archivos,533mutantes. Este número no es resultado ni acredita que se hayan ejecutado todos. Al terminar se registrarán conteos por estado, score bruto, EXIT, duración, hashes después y cada residual. Ningún RuntimeError/Timeout se atribuirá a Killed. No se editarán fuentes/pruebas para corregir mutantes durante esta medición.

No se ejecuta el destino backend, cuyo selector espera todavía inventario final y freeze. No hay declaración de cierre de14 ni de UX navegador.

Baseline GREEN1de735: Stryker informa679pruebas en2m41s (net81,261s, overhead80,719s). Es la selección de la herramienta, no una regresión global adicional. Muestra01:04:34 local a3507a: los ocho workers acumulan105–277sCPU y mantienen actividad; elapsed5m07 desde before. No se infiere porcentaje de avance sin reporte de mutantes.

Incidente comunicado por root durante la campaña: merge de squash main861cdc8 produjo add/add temporales en API/test y bitácora (c4c5ae); root restauró la versión congelada inmediatamente. Verificación independiente14a871:10hashes de producto idénticos; merge03778ed con diff vacío. Instrumentación y baseline de esta campaña habían terminado antes sobre la copia de Stryker. No se interrumpe ni repite; se verificarán los88hashes finales. El conflicto temporal no se atribuye a comportamiento del producto.

## Resultado final

Stryker terminó c3875b a2026-09-07T01:23:56+02:00, duración reportada24m08s. Desde captura before hasta fin del reporter:24m28s aproximadamente (incluye arranque del arnés). Promedio28,91tests por mutante.

| Archivo | Total | Killed | Survived | NoCoverage | RuntimeError | Timeout |
| --- | --- | --- | --- | --- | --- | --- |
| work-session-api.ts | 211 | 184 | 27 | 0 | 0 | 0 |
| work-session.tsx | 315 | 264 | 51 | 0 | 0 | 0 |
| task-reader.tsx | 7 | 4 | 3 | 0 | 0 | 0 |
| Total | 533 | 452 | 81 | 0 | 0 | 0 |

After446566:88archivos,0diferencias; mutation_start_work_frontend_after.json conserva hashes individuales. Freeze de campaña liberado al coordinador inmediatamente al terminar. JSON original E88C2A6C45207C40D68267162F320E84B9A769CD581F69B21896C8B1260764FF; HTML6986FF220F6144DF010D12D1924F9EE563A1AED25946C7ACE5E4B98241E152D5, ambos en frontend/reports/mutation-start-work-session/. No se sobrescribieron con un replay.

## Inventario residual y trabajo para revisión

mutation_start_work_frontend_inventory.json conserva81identidades con ubicación original, mutador, texto y reemplazo. Las columnas del informe Stryker son1-based: extracción de texto usa columna-1, conservando negaciones. La tabla siguiente propone el oráculo o análisis necesario, no afirma que cada superviviente sea un defecto ni que se haya probado una equivalencia. Ninguno se descuenta de la medición.

| ID | Archivo:línea | Original → mutación | Análisis u oráculo pendiente |
| --- | --- | --- | --- |
| 14 | src/work-session-api.ts:29 | () => null → () => undefined | Parser: fallo JSON y problemas con código/type/status contradictorios; revisar redundancias antes de añadir casos. |
| 29 | src/work-session-api.ts:32 | value.code === "WORK_SESSION_ALREADY_ACTIVE" → true | Parser: fallo JSON y problemas con código/type/status contradictorios; revisar redundancias antes de añadir casos. |
| 32 | src/work-session-api.ts:33 | value.type === "urn:organization:problem:work_session_already_active" → true | Parser: fallo JSON y problemas con código/type/status contradictorios; revisar redundancias antes de añadir casos. |
| 37 | src/work-session-api.ts:35 | error.status === 409 → true | Parser: fallo JSON y problemas con código/type/status contradictorios; revisar redundancias antes de añadir casos. |
| 35 | src/work-session-api.ts:34 | value.status === 409 → true | Parser: fallo JSON y problemas con código/type/status contradictorios; revisar redundancias antes de añadir casos. |
| 54 | src/work-session-api.ts:50 | value.type !== "urn:organization:problem:" + value.code.toLowerCase() → false | Parser: fallo JSON y problemas con código/type/status contradictorios; revisar redundancias antes de añadir casos. |
| 61 | src/work-session-api.ts:53 | (value.code === "WORK_SESSION_NOT_FOUND" && error.status === 404) &#124;&#124; ⏎       (value.code === "WORK_SESSION_TIME_OUT_OF_RANGE" && error.status === 409) → true | Parser: fallo JSON y problemas con código/type/status contradictorios; revisar redundancias antes de añadir casos. |
| 58 | src/work-session-api.ts:51 | value.status !== error.status → false | Parser: fallo JSON y problemas con código/type/status contradictorios; revisar redundancias antes de añadir casos. |
| 72 | src/work-session-api.ts:54 | value.code === "WORK_SESSION_TIME_OUT_OF_RANGE" && error.status === 409 → value.code === "WORK_SESSION_TIME_OUT_OF_RANGE" &#124;&#124; error.status === 409 | Parser: fallo JSON y problemas con código/type/status contradictorios; revisar redundancias antes de añadir casos. |
| 65 | src/work-session-api.ts:53 | value.code === "WORK_SESSION_NOT_FOUND" && error.status === 404 → value.code === "WORK_SESSION_NOT_FOUND" &#124;&#124; error.status === 404 | Parser: fallo JSON y problemas con código/type/status contradictorios; revisar redundancias antes de añadir casos. |
| 66 | src/work-session-api.ts:53 | value.code === "WORK_SESSION_NOT_FOUND" → true | Parser: fallo JSON y problemas con código/type/status contradictorios; revisar redundancias antes de añadir casos. |
| 73 | src/work-session-api.ts:54 | value.code === "WORK_SESSION_TIME_OUT_OF_RANGE" → true | Parser: fallo JSON y problemas con código/type/status contradictorios; revisar redundancias antes de añadir casos. |
| 69 | src/work-session-api.ts:53 | error.status === 404 → true | Parser: fallo JSON y problemas con código/type/status contradictorios; revisar redundancias antes de añadir casos. |
| 76 | src/work-session-api.ts:54 | error.status === 409 → true | Parser: fallo JSON y problemas con código/type/status contradictorios; revisar redundancias antes de añadir casos. |
| 141 | src/work-session-api.ts:134 | value.plannedMinutes !== plannedMinutes → false | POST de duración distinta pero internamente coherente debe permanecer incierto. |
| 174 | src/work-session-api.ts:159 | start !== null && ⏎     end !== null → true | Revisar rechazo por null frente excepción de BigInt: efecto en consumidores reales, sin confundir clase de error con confirmación. |
| 175 | src/work-session-api.ts:159 | start !== null && ⏎     end !== null → start !== null &#124;&#124; end !== null | Revisar rechazo por null frente excepción de BigInt: efecto en consumidores reales, sin confundir clase de error con confirmación. |
| 176 | src/work-session-api.ts:159 | start !== null → true | Revisar rechazo por null frente excepción de BigInt: efecto en consumidores reales, sin confundir clase de error con confirmación. |
| 178 | src/work-session-api.ts:160 | end !== null → true | Revisar rechazo por null frente excepción de BigInt: efecto en consumidores reales, sin confundir clase de error con confirmación. |
| 188 | src/work-session-api.ts:167 | /(?:\.\d+)?Z$/ → /(?:\.\d+)?Z/ | Exactitud temporal: fracciones con distinta representación y recibos incoherentes; anclas redundantes sólo tras validar instant. Revisar equivalencia del signo en una diferencia de instantes. |
| 189 | src/work-session-api.ts:167 | /(?:\.\d+)?Z$/ → /(?:\.\d+)Z$/ | Exactitud temporal: fracciones con distinta representación y recibos incoherentes; anclas redundantes sólo tras validar instant. Revisar equivalencia del signo en una diferencia de instantes. |
| 190 | src/work-session-api.ts:167 | /(?:\.\d+)?Z$/ → /(?:\.\d)?Z$/ | Exactitud temporal: fracciones con distinta representación y recibos incoherentes; anclas redundantes sólo tras validar instant. Revisar equivalencia del signo en una diferencia de instantes. |
| 191 | src/work-session-api.ts:167 | /(?:\.\d+)?Z$/ → /(?:\.\D+)?Z$/ | Exactitud temporal: fracciones con distinta representación y recibos incoherentes; anclas redundantes sólo tras validar instant. Revisar equivalencia del signo en una diferencia de instantes. |
| 192 | src/work-session-api.ts:167 | "Z" → "" | Exactitud temporal: fracciones con distinta representación y recibos incoherentes; anclas redundantes sólo tras validar instant. Revisar equivalencia del signo en una diferencia de instantes. |
| 195 | src/work-session-api.ts:168 | /\.(\d+)Z$/ → /\.(\d+)Z/ | Exactitud temporal: fracciones con distinta representación y recibos incoherentes; anclas redundantes sólo tras validar instant. Revisar equivalencia del signo en una diferencia de instantes. |
| 199 | src/work-session-api.ts:169 | BigInt(wholeMilliseconds) * 1000n + BigInt(fraction.padEnd(6, "0")) → BigInt(wholeMilliseconds) * 1000n - BigInt(fraction.padEnd(6, "0")) | Exactitud temporal: fracciones con distinta representación y recibos incoherentes; anclas redundantes sólo tras validar instant. Revisar equivalencia del signo en una diferencia de instantes. |
| 201 | src/work-session-api.ts:169 | "0" → "" | Exactitud temporal: fracciones con distinta representación y recibos incoherentes; anclas redundantes sólo tras validar instant. Revisar equivalencia del signo en una diferencia de instantes. |
| 6 | src/task-reader.tsx:131 | snapshot?.project → snapshot.project | Composición: proyecto recargando/fallido con snapshot anterior; no habilitar inicio hasta contexto vigente. Optional requiere justificar invariante. |
| 3 | src/task-reader.tsx:130 | !projectLoading && !projectFailure → !projectLoading &#124;&#124; !projectFailure | Composición: proyecto recargando/fallido con snapshot anterior; no habilitar inicio hasta contexto vigente. Optional requiere justificar invariante. |
| 1 | src/task-reader.tsx:130 | !projectLoading && !projectFailure → true | Composición: proyecto recargando/fallido con snapshot anterior; no habilitar inicio hasta contexto vigente. Optional requiere justificar invariante. |
| 233 | src/work-session.tsx:29 | heading.current?.focus → heading.current.focus | Estado inicial, elegibilidad tarea/proyecto independientes y refs montadas: separar oráculo observable de invariante interna. |
| 236 | src/work-session.tsx:36 | "" → "Stryker was here!" | Estado inicial, elegibilidad tarea/proyecto independientes y refs montadas: separar oráculo observable de invariante interna. |
| 238 | src/work-session.tsx:38 | false → true | Estado inicial, elegibilidad tarea/proyecto independientes y refs montadas: separar oráculo observable de invariante interna. |
| 240 | src/work-session.tsx:41 | false → true | Estado inicial, elegibilidad tarea/proyecto independientes y refs montadas: separar oráculo observable de invariante interna. |
| 249 | src/work-session.tsx:48 | active === null → true | Estado inicial, elegibilidad tarea/proyecto independientes y refs montadas: separar oráculo observable de invariante interna. |
| 257 | src/work-session.tsx:50 | props.taskStatus === "pending" && ⏎     props.projectStatus !== undefined → props.taskStatus === "pending" &#124;&#124; props.projectStatus !== undefined | Estado inicial, elegibilidad tarea/proyecto independientes y refs montadas: separar oráculo observable de invariante interna. |
| 258 | src/work-session.tsx:50 | props.taskStatus === "pending" → true | Estado inicial, elegibilidad tarea/proyecto independientes y refs montadas: separar oráculo observable de invariante interna. |
| 261 | src/work-session.tsx:51 | props.projectStatus !== undefined → true | Estado inicial, elegibilidad tarea/proyecto independientes y refs montadas: separar oráculo observable de invariante interna. |
| 294 | src/work-session.tsx:62 | Number(minutes) < 1 → Number(minutes) <= 1 | Duración mínima1/máxima1440 y exceso; ausencia de errores viejos después de corregir. |
| 296 | src/work-session.tsx:63 | Number(minutes) > 1440 → false | Duración mínima1/máxima1440 y exceso; ausencia de errores viejos después de corregir. |
| 297 | src/work-session.tsx:63 | Number(minutes) > 1440 → Number(minutes) >= 1440 | Duración mínima1/máxima1440 y exceso; ausencia de errores viejos después de corregir. |
| 305 | src/work-session.tsx:69 | "" → "Stryker was here!" | Duración mínima1/máxima1440 y exceso; ausencia de errores viejos después de corregir. |
| 313 | src/work-session.tsx:83 | controller.signal.aborted → false | Confirmación tardía frente cleanup y lookup pendiente: sin carga/error/uncertidumbre residual; revisar efectos locales desmontados. |
| 273 | src/work-session.tsx:54 | [] → ["Stryker was here"] | Estado inicial, elegibilidad tarea/proyecto independientes y refs montadas: separar oráculo observable de invariante interna. |
| 314 | src/work-session.tsx:84 | lookup.current?.abort → lookup.current.abort | Confirmación tardía frente cleanup y lookup pendiente: sin carga/error/uncertidumbre residual; revisar efectos locales desmontados. |
| 316 | src/work-session.tsx:85 | false → true | Confirmación tardía frente cleanup y lookup pendiente: sin carga/error/uncertidumbre residual; revisar efectos locales desmontados. |
| 318 | src/work-session.tsx:86 | false → true | Confirmación tardía frente cleanup y lookup pendiente: sin carga/error/uncertidumbre residual; revisar efectos locales desmontados. |
| 322 | src/work-session.tsx:89 | false → true | Confirmación tardía frente cleanup y lookup pendiente: sin carga/error/uncertidumbre residual; revisar efectos locales desmontados. |
| 367 | src/work-session.tsx:108 | problem.code === "WORK_SESSION_ALREADY_ACTIVE" → false | Rechazo activo retira ausencia anterior; validación sin campo coincidente y nuevo ciclo incierto no hereda permiso de reenvío. |
| 369 | src/work-session.tsx:108 | "WORK_SESSION_ALREADY_ACTIVE" → "" | Rechazo activo retira ausencia anterior; validación sin campo coincidente y nuevo ciclo incierto no hereda permiso de reenvío. |
| 370 | src/work-session.tsx:109 | setActive(undefined); → ; | Rechazo activo retira ausencia anterior; validación sin campo coincidente y nuevo ciclo incierto no hereda permiso de reenvío. |
| 375 | src/work-session.tsx:113 | false → true | Rechazo activo retira ausencia anterior; validación sin campo coincidente y nuevo ciclo incierto no hereda permiso de reenvío. |
| 384 | src/work-session.tsx:118 | problem.errors.find((error) => error.field === "plannedMinutes") ⏎             ?.message → problem.errors.find(error => error.field === "plannedMinutes").message | Rechazo activo retira ausencia anterior; validación sin campo coincidente y nuevo ciclo incierto no hereda permiso de reenvío. |
| 386 | src/work-session.tsx:118 | error.field === "plannedMinutes" → true | Rechazo activo retira ausencia anterior; validación sin campo coincidente y nuevo ciclo incierto no hereda permiso de reenvío. |
| 393 | src/work-session.tsx:123 | false → true | Rechazo activo retira ausencia anterior; validación sin campo coincidente y nuevo ciclo incierto no hereda permiso de reenvío. |
| 411 | src/work-session.tsx:132 | !controller.signal.aborted && command.current === controller → true | Cleanup/finally y401 tardío con padre vivo; justificar identidad de comando antes de tratar una guarda como redundante. |
| 413 | src/work-session.tsx:132 | !controller.signal.aborted && command.current === controller → !controller.signal.aborted &#124;&#124; command.current === controller | Cleanup/finally y401 tardío con padre vivo; justificar identidad de comando antes de tratar una guarda como redundante. |
| 415 | src/work-session.tsx:132 | command.current === controller → true | Cleanup/finally y401 tardío con padre vivo; justificar identidad de comando antes de tratar una guarda como redundante. |
| 429 | src/work-session.tsx:146 | controller.signal.aborted → false | Cleanup/finally y401 tardío con padre vivo; justificar identidad de comando antes de tratar una guarda como redundante. |
| 467 | src/work-session.tsx:182 | false → true | Región nombrada, errores retirados al actualizar y al menos dos actualizaciones independientes; signo del contador requiere análisis contextual. |
| 469 | src/work-session.tsx:183 | (value) => value + 1 → () => undefined | Región nombrada, errores retirados al actualizar y al menos dos actualizaciones independientes; signo del contador requiere análisis contextual. |
| 470 | src/work-session.tsx:183 | value + 1 → value - 1 | Región nombrada, errores retirados al actualizar y al menos dos actualizaciones independientes; signo del contador requiere análisis contextual. |
| 446 | src/work-session.tsx:159 | &#96;${id}-heading&#96; → &#96;&#96; | Región nombrada, errores retirados al actualizar y al menos dos actualizaciones independientes; signo del contador requiere análisis contextual. |
| 447 | src/work-session.tsx:161 | &#96;${id}-heading&#96; → &#96;&#96; | Región nombrada, errores retirados al actualizar y al menos dos actualizaciones independientes; signo del contador requiere análisis contextual. |
| 494 | src/work-session.tsx:201 | !eligible && !uncertain && ( ⏎             <p> ⏎               Para iniciar trabajo necesitamos confirmar una tarea pendiente y ⏎               un proyecto no completado. ⏎             </p> ⏎           ) → true | Ayuda de inelegibilidad en estados conocidos/desconocidos; submit Enter durante incertidumbre y preventDefault observable. |
| 495 | src/work-session.tsx:201 | !eligible && !uncertain && ( ⏎             <p> ⏎               Para iniciar trabajo necesitamos confirmar una tarea pendiente y ⏎               un proyecto no completado. ⏎             </p> ⏎           ) → false | Ayuda de inelegibilidad en estados conocidos/desconocidos; submit Enter durante incertidumbre y preventDefault observable. |
| 502 | src/work-session.tsx:213 | event.preventDefault(); → ; | Ayuda de inelegibilidad en estados conocidos/desconocidos; submit Enter durante incertidumbre y preventDefault observable. |
| 500 | src/work-session.tsx:201 | !uncertain → uncertain | Ayuda de inelegibilidad en estados conocidos/desconocidos; submit Enter durante incertidumbre y preventDefault observable. |
| 504 | src/work-session.tsx:214 | !uncertain → true | Ayuda de inelegibilidad en estados conocidos/desconocidos; submit Enter durante incertidumbre y preventDefault observable. |
| 496 | src/work-session.tsx:201 | !eligible && !uncertain && ( ⏎             <p> ⏎               Para iniciar trabajo necesitamos confirmar una tarea pendiente y ⏎               un proyecto no completado. ⏎             </p> ⏎           ) → !eligible && !uncertain &#124;&#124; <p> ⏎               Para iniciar trabajo necesitamos confirmar una tarea pendiente y ⏎               un proyecto no completado. ⏎             </p> | Ayuda de inelegibilidad en estados conocidos/desconocidos; submit Enter durante incertidumbre y preventDefault observable. |
| 497 | src/work-session.tsx:201 | !eligible && !uncertain → true | Ayuda de inelegibilidad en estados conocidos/desconocidos; submit Enter durante incertidumbre y preventDefault observable. |
| 498 | src/work-session.tsx:201 | !eligible && !uncertain → !eligible &#124;&#124; !uncertain | Ayuda de inelegibilidad en estados conocidos/desconocidos; submit Enter durante incertidumbre y preventDefault observable. |
| 499 | src/work-session.tsx:201 | !eligible → eligible | Ayuda de inelegibilidad en estados conocidos/desconocidos; submit Enter durante incertidumbre y preventDefault observable. |
| 541 | src/work-session.tsx:297 | false → true | Presentación histórica: UTC de respaldo sólo si Intl falla, locale/fecha/hora completas y separación legible. No asumir equivalencia de strings por ser UI. |
| 543 | src/work-session.tsx:300 | "es-ES" → "" | Presentación histórica: UTC de respaldo sólo si Intl falla, locale/fecha/hora completas y separación legible. No asumir equivalencia de strings por ser UI. |
| 545 | src/work-session.tsx:302 | "long" → "" | Presentación histórica: UTC de respaldo sólo si Intl falla, locale/fecha/hora completas y separación legible. No asumir equivalencia de strings por ser UI. |
| 546 | src/work-session.tsx:303 | "long" → "" | Presentación histórica: UTC de respaldo sólo si Intl falla, locale/fecha/hora completas y separación legible. No asumir equivalencia de strings por ser UI. |
| 554 | src/work-session.tsx:316 | " " → "" | Presentación histórica: UTC de respaldo sólo si Intl falla, locale/fecha/hora completas y separación legible. No asumir equivalencia de strings por ser UI. |
| 555 | src/work-session.tsx:323 | " " → "" | Presentación histórica: UTC de respaldo sólo si Intl falla, locale/fecha/hora completas y separación legible. No asumir equivalencia de strings por ser UI. |
| 532 | src/work-session.tsx:269 | busy &#124;&#124; uncertain → true | Aviso de salida sólo después de transmitir; no advertir revocación antes de iniciar. |
| 440 | src/work-session.tsx:152 | !controller.signal.aborted → true | Cleanup/finally y401 tardío con padre vivo; justificar identidad de comando antes de tratar una guarda como redundante. |
