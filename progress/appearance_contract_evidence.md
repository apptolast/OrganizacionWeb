# Apariencia20: trazabilidad contractual

Corte documental final de evidencias locales; CI del último corte pendiente. **37 escenarios y112 ejemplos expandidos**
del Gherkin no son37/112 tests ejecutables. No se declara done. Las filas
separan evidencia ejecutada, compuesta y pendiente; los futuros cambios de
UI no quedan acreditados automáticamente por los cortes anteriores.

## Fuentes de evidencia

- Backend: tdd_appearance_backend.md, appearance_backend_final_manifest.json
  y seis XML70/70; review_appearance_backend_final.md. Core/PG reales y
  wiring con adaptador real/JDBC simulado, claramente separados.
- HTTP: tdd_appearance_http.md, appearance_http_freeze.json y
  review_appearance_http.md;55 propios+151 regresión Availability=206 verdes.
  Slices con seguridad Spring real y puertos simulados.
- Cliente/UI: tdd_appearance_frontend.md, appearance_client_freeze.json,
  appearance_shared_freeze.json y appearance_shared_recovery_freeze.json.
  review_appearance_shared_state.md conserva hallazgoP2 y ratificación.
  Corte previo: appearance_frontend_text200_freeze.json, API/UI 78/78
  (30 cliente y48 UI), EXIT0 58caef; global 2046/44, EXIT0 2b6247.
  Después de los refuerzos: API 40/40 por C; UI/auth 124/124 por B
  (50 UI y74 auth), EXIT0 890d69, appearance_ui_refinement_focal_final.log.
  Regresión final **2058 tests /44 archivos**, EXIT0 91f538,
  appearance_refined_global_test.log;119 entradas antes/después idénticas
  (373ea2). El primer lint sólo detectó formato en appearance-api.test.ts;
  corregido sin lógica, lint completo EXIT0 66161f. El freeze final es
  appearance_refined_final_freeze.json; no se atribuye otra ejecución global
  al cambio exclusivo de formato. Build final anterior EXIT0 1eb847 sobre
  la misma producción/CSS; no se extrapola la conjunta anterior558.
- E2E API/PG: tdd_appearance_persistence_e2e.md y
  appearance_persistence_e2e.log,1/1 inicialmenteGREENe5b0cc. Transporte
  cortado tras PUT200 real, reinicioAPI y otro contexto autenticado.
- PIT: mutation_appearance_backend.md y
  mutation_appearance_backend_replay.md. Original153K/6S/159=96,2264%;
  replay13K/2S/15=86,6667%, separados. No NC, timeout ni error en ambos.
 391 inputs originales y392 replay intactos respectivamente. Boundary
  byte0,04045 se conservaS con explicación de dominio; boundary4,5 sigueS
  sin equivalencia demostrada. No se suman replays al score original.

- Stryker: mutation_appearance_frontend.md y review_appearance_stryker_replay.md.
  Original **637 Killed /747:85,27 %**,102 Survived y8 NoCoverage;
  cero timeout/errores. Raw original SHA56D3A57C…D60122 conservado.
  Replay dirigido independiente EXIT0 22a6d3: **72 Killed /79:91,14 %**,
  7 Survived y cero NC/timeout/errores. De31 firmas objetivo,30 Killed y1
  Survived;48 extras:42 Killed y6 Survived. Las120 entradas permanecieron
  idénticas y root contrastó las79 firmas (3e0af6). No se suman resultados.
- UX: tdd_appearance_ux_audit.md y review_appearance_ux_audit.md,
  tres motores terminados y zoom nativo Chromium; detalle y límites en s34.

## Mapa37

| Tag | Conducta | Evidencia y estado |
| --- | --- | --- |
| s1 | Defaults sin insertar | Ejecutado: ReadAppearanceTest, PG ausencia y AppearanceApiTest.s1; cliente defaults exactos. |
| s2 | Tres valores durables | Compuesto ejecutado: Save/PG nominal, HTTP DTO+principal+tag, cliente colores arbitrarios; E2E guarda DARK. |
| s3 | No-op canónico | Ejecutado: Save no-op sin Clock y PG trigger que rechaza UPDATE; HTTP tag máximo. |
| s4 | Revisión obsoleta aunque intención satisfecha | Ejecutado: Save.s4; prioridad revisión antes de no-op. |
| s5 | Carreras misma revisión | Ejecutado: PG alta concurrente y actualización esperando lock real; un éxito y un conflicto, sin ganador prefijado. |
| s6 | Tag ajeno no selecciona owner | Compuesto ejecutado: Save identidad/propiedad, PG owner aislado y HTTP412 sin datos. |
| s7 | Fallo antes del commit | Ejecutado: PG supresión INSERT/UPDATE y fallo diferido de commit; HTTP503. |
| s8 | ACK perdido y reinicio | E2E real1/1: route.fetch obtiene200, abort al navegador, APIreiniciada/PGconservado, login en contexto nuevo y mismo DTO/ETag. No se atribuye sólo a reconstrucción Store. |
| s9 | Sin cambios de trabajo ni eventos | E2E compara proyecto/tarea/reserva/sesióncerrada/intervalos/recibos/disponibilidad/outbox e historiaHTTP antes/después; PG ausenciaoutbox y upgrade aditivo. |
| s10 | Forma y orden de campos | Ejecutado: Values/Save y14 variantesHTTP de campos, raíz/extras y mezclas con precedencia. |
| s11 | Contraste en ambos temas | Ejecutado: dominio y cliente, superficies completas y vector4,499799974; refuerzos PIT de canal/offset yaK. No se presume accesibilidad visual por fórmula. |
| s12 | If-Match cerrado | Ejecutado: HTTP ausente428, listas/repetidos/débil/UUID/canonical/BIGINT; identidad completa llega al puerto. |
| s13 | Query precede header/body | Ejecutado: HTTP GET/PUT con query repetida y body malformado; no interacción del puerto. |
| s14 | JSON estricto | Ejecutado: HTTP ausente/vacío/duplicados/concatenado y raíz no objeto; reutiliza ApiErrors. |
| s15 | Seguridad/negociación | Ejecutado: HTTP401/403origen/CSRF/415 antes de handler; GET autenticado sin CSRF. |
| s16 | Fallo no es ausencia | Compuesto ejecutado: PG tabla no disponible, HTTP503 sin defaults/ETag/detalles y cliente error conservado. |
| s17 | Versión máxima | Ejecutado: Save máximo real503 y no-op máximo válido; HTTP máximo textual, sin númeroJSON. |
| s18 | Decoder cerrado | Ejecutado: appearance-api.test.ts, claves/boolean/theme/colores/contraste/timestamp/ETag/long. Refuerzos API40/40 y Stryker original/replay terminados; el objetivo73 (typeof del HEX) sigue Survived, sin equivalencia universal demostrada. |
| s19 | Confirmación corresponde a intención | Ejecutado: cliente captura valores antes de await y rechaza cambio de cada campo o configuredfalse. |
| s20 | Navegación/retorno login | UI ejecutada ciclos48–50 y82: `opens appearance through the principal navigation at its stable route`, carga única/StrictMode y retorno tras login. Hoy permanece primero y Apariencia al final; sin lectura privada anónima. |
| s21 | Borrador/muestra locales | UI `edits a local preview`, `provides separate native link and button samples` y `synchronizes native color selectors`: ciclo74 RED540306/GREENef7017 conserva hex/picker incluso con contraste inválido, sin aplicar ese color a la muestra ni globalmente. |
| s22 | Restaurar defaults no escribe | UI ejecutada ciclo45 y guardas posteriores: intención local hasta Guardar. |
| s23 | Confirmación global y un PUT | UI `saves one explicit draft and applies it only after confirmation`: dobleclic, controles no editables durante PUT, foco y éxito retirado al volver a editar. Ciclo80 retira errores obsoletos tras guardar. C: teclado real, feedback5,1ms con PUT retenido y foco conservado tras200, GREENc47fbb. |
| s24 | SYSTEM y medio | Ejecutado: SYSTEM evento/visibilitychange y dos filasLIGHT/DARK fijas inicialmenteGREEN; regresión41/41 d3d742. Sin PUT adicional. Refuerzo del mock con query realista inicialmente GREEN6ff053; medios reales emulados acreditados en tres motores. |
| s25 | Rutas conservan confirmado, descartan borrador | UI ciclo60 ejecutado, Historial/Back y ningún GET extra. |
| s26 | Fallo inicial permite continuar | UI ciclos61 y reintento: otra función utilizable, defaults provisionales no guardados y carga/error honestos. |
| s27 | Error accesible, muestra segura | UI `retains an invalid color`, error de grupo theme y dos filas de error de campo light/dark; aria-invalid/descripción y corrección. Ciclos70/71/80/81 retiran errores de borradores reemplazados por Cancelar, Restaurar, guardado válido o recuperación válida. |
| s28 | Incertidumbre bloquea escritura | Cinco filas reales de `recovers an uncertain PUT ... manual decision`:503 coherente,412 coherente, red,200 con campo adicional y code/type contradictorio. Ciclos75–78 inicialmente GREEN (8801b0/fd7627/90f45b/116d83), incluidos en78focal/2046global. Ciclo72 `keeps the write blocked across navigation` acredita Provider y ausencia de GET/PUT automático al volver. |
| s29 | GET manual recupera | El mismo caso confirma lectura200 válida, reemplazo de borrador y ningún falso éxito/recibo. `retains uncertainty after failed recovery ...` tiene filas503 y200 inválido (ciclo79 inicialmente GREEN7c8eae), conserva borrador/bloqueo y anuncia siguiente consulta. Ciclo81 REDbdc345/GREEN7cca13 elimina errores400 obsoletos sólo después del GET válido. |
| s30 | Lectura antigua no pisa PUT | UI ciclos59/66 ejecutados: GET anterior y GET iniciado durante PUT abortados ante confirmación. Refuerzo57d42c conserva el200 antiguo y añade401 antiguo antes de confirmar PUT: no revoca acceso ni aplica preferencias prematuramente. |
| s31 | Respuestas privadas obsoletas | Cliente `discards a late HTTP401` GET/PUT y JSON abortado; UI seis filas `ignores Ana's late ... after logout and Bruno's appearance has loaded`, con HTTP401/200 y JSON diferido, ciclos53/58/62–65. Incluidas en78focal/2046global. |
| s32 | Logout retira preferencias | UI ejecutada: desmontajeProvider/overrides y cambio propietario sin herencia de snapshot/borrador. Seguridad de sesión real en fixture. Refuerzo0daf01 añade SYSTEM a DARK y dispara eventos de medio/visibilidad tras desmontaje sin restaurar overrides. |
| s33 | Foco deliberado | `recovers an uncertain PUT ...` comprueba encabezado al desaparecer el iniciador (ciclo55); `does not reclaim focus after a deliberate move followed by blur during recovery` comprueba body después de otra elección (ciclo56), ambos ejecutados en78/2046. La segunda fila contractual (otro control conserva foco hasta el final) no tiene aserción final independiente: no se deduce de la fila con blur. Se conserva como límite de evidencia; los refuerzos finales no añaden esa aserción. |
| s34 | Acceso/contraste/responsive | Auditoría final en Chromium6/6 (d9bbbb), Firefox4/4 (c780df) y WebKit4/4 (64eacf). Por motor:124 medidas de geometría,24 de texto200 y6 de medios;14 axe completos y1 sin color-contrast exclusivamente en forced-colors, todos sin violaciones dentro de ese alcance. Feedback4,5/17/8ms respectivamente. Zoom nativo Chromium2×:1/1 (0b30aa), geometría y un axe completo; captura viewport válida, no el fullPage recortado. Treinta principios documentados, con límites físicos y de evaluación humana; véanse ambos informes UX. |
| s35 | Reloj/corrupción503 | Ejecutado: Save años0/10000/relojfallido/no-op sinClock; PG contraste/rango/metadata corruptos mediante fixture controlado. Replay acredita año9999 real. |
| s36 | SYSTEM sin matchMedia | Ejecutado: matchMedia undefined, SYSTEM permanece seleccionado, light/accentLight confirmados, sóloGET; inicialmenteGREEN841ba3. |
| s37 | Cancelar sin HTTP | `cancels a local draft back to confirmed values without an HTTP request`, ciclo46; mantiene foco en Cancelar y ningún HTTP. Las filas `clears obsolete server errors together with the replaced draft` conservan esa conducta y retiran errores obsoletos (ciclos70/71). |

## Gates y límites vigentes

Backend, HTTP, PIT original/replay y E2E de persistencia están acreditados
con sus límites anteriores. Frontend final2058/44 y lint completos verdes;
build final de la misma producción acreditado. Stryker original y replay
están terminados y conservados por separado. El replay deja siete residuos:
objetivo73 (typeof HEX), extras171/172 (códigos de error),175 (tipo de mensaje),
178 (mensaje sólo espacios) y618/619 (anclas del valor del picker). No se
reclasifican como muertos ni se declaran equivalentes de forma general;
el dictamen del replay documenta el alcance concreto y no demuestra un
fallo de la producción actual.

La CI anterior acabó **141/143**, con dos fallos de fixtures E2E: captura
fullPage redundante en Linux y lista de peticiones de Historial que no
contemplaba GET appearance. Correcciones aisladas en **1f36315**, sin cambio
productivo. La nueva **CI34154520811 sigue pendiente** en este corte; los
gates locales y de navegadores no se presentan como su resultado.

s28/s29 y los huecos s24/s36 están resueltos con referencias anteriores.
s33 conserva el límite concreto de la fila de foco sin blur. UX final
cubre tres motores, pero los tamaños/medios son emulados: no hubo móvil o
tablet físico, teclado virtual, lector de pantalla real ni estudio con
participantes; zoom nativo sólo Chromium. Forced-colors no acredita una
medición numérica de contraste de píxeles. La matriz de treinta principios
no es una certificación universal. Este documento no cierra la feature ni
anuncia disponibilidad desplegada.

## Cierre de los dos huecos cedidos por B

A recibió ownership temporal de appearance.test.tsx, sin producción.
s36 inicialmenteGREEN841ba3:1pasó/38no seleccionados; s24LIGHT/DARK
inicialmenteGREEN:2pasaron/39no seleccionados, log
appearance_s24_fixed_initial.log. No se inventa RED; la implementación ya
cumple. Prettier/ESLint y regresión completa EXIT0d3d742:41/41, cero skips.
Log appearance_s24_s36_final.log. SHA del test al devolverlo a B:
0BB1B2C42A8FFCFD598D91BBC1C3255E1B649A63E1C904B72E7D02BB8D297173.
Sólo dos declaraciones nuevas (una parametrizada en dos filas), sin matriz
adicional. La nota previa de huecos s24/s36 queda resuelta por esta evidencia;
Las referencias de recuperación y foco se han consolidado arriba con el
corte final. La auditoría UX y Stryker finalizados se registran por separado arriba.
