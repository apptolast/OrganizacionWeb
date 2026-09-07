# Apariencia20: trazabilidad contractual

Corte documental provisional. **37 escenarios y112 ejemplos expandidos**
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
  Corte final: appearance_frontend_text200_freeze.json. API/UI 78/78
  (30 cliente y48 UI), EXIT0 58caef, appearance_ui_final_focal.log.
  Regresión completa2046/44, EXIT0 2b6247; appearance_frontend_gate.md
  conserva119 hashes y la distinción entre CSS preliminar y final.
  Lint/build final EXIT0 1eb847; no se extrapola la conjunta anterior558.
- E2E API/PG: tdd_appearance_persistence_e2e.md y
  appearance_persistence_e2e.log,1/1 inicialmenteGREENe5b0cc. Transporte
  cortado tras PUT200 real, reinicioAPI y otro contexto autenticado.
- PIT: mutation_appearance_backend.md y
  mutation_appearance_backend_replay.md. Original153K/6S/159=96,2264%;
  replay13K/2S/15=86,6667%, separados. No NC, timeout ni error en ambos.
 391 inputs originales y392 replay intactos respectivamente. Boundary
  byte0,04045 se conservaS con explicación de dominio; boundary4,5 sigueS
  sin equivalencia demostrada. No se suman replays al score original.

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
| s18 | Decoder cerrado | Ejecutado: appearance-api.test.ts, claves/boolean/theme/colores/contraste/timestamp/ETag/long. Stryker todavía pendiente. |
| s19 | Confirmación corresponde a intención | Ejecutado: cliente captura valores antes de await y rechaza cambio de cada campo o configuredfalse. |
| s20 | Navegación/retorno login | UI ejecutada ciclos48–50 y82: `opens appearance through the principal navigation at its stable route`, carga única/StrictMode y retorno tras login. Hoy permanece primero y Apariencia al final; sin lectura privada anónima. |
| s21 | Borrador/muestra locales | UI `edits a local preview`, `provides separate native link and button samples` y `synchronizes native color selectors`: ciclo74 RED540306/GREENef7017 conserva hex/picker incluso con contraste inválido, sin aplicar ese color a la muestra ni globalmente. |
| s22 | Restaurar defaults no escribe | UI ejecutada ciclo45 y guardas posteriores: intención local hasta Guardar. |
| s23 | Confirmación global y un PUT | UI `saves one explicit draft and applies it only after confirmation`: dobleclic, controles no editables durante PUT, foco y éxito retirado al volver a editar. Ciclo80 retira errores obsoletos tras guardar. C: teclado real, feedback5,1ms con PUT retenido y foco conservado tras200, GREENc47fbb. |
| s24 | SYSTEM y medio | Ejecutado: SYSTEM evento/visibilitychange y dos filasLIGHT/DARK fijas inicialmenteGREEN; regresión41/41 d3d742. Sin PUT adicional. |
| s25 | Rutas conservan confirmado, descartan borrador | UI ciclo60 ejecutado, Historial/Back y ningún GET extra. |
| s26 | Fallo inicial permite continuar | UI ciclos61 y reintento: otra función utilizable, defaults provisionales no guardados y carga/error honestos. |
| s27 | Error accesible, muestra segura | UI `retains an invalid color`, error de grupo theme y dos filas de error de campo light/dark; aria-invalid/descripción y corrección. Ciclos70/71/80/81 retiran errores de borradores reemplazados por Cancelar, Restaurar, guardado válido o recuperación válida. |
| s28 | Incertidumbre bloquea escritura | Cinco filas reales de `recovers an uncertain PUT ... manual decision`:503 coherente,412 coherente, red,200 con campo adicional y code/type contradictorio. Ciclos75–78 inicialmente GREEN (8801b0/fd7627/90f45b/116d83), incluidos en78focal/2046global. Ciclo72 `keeps the write blocked across navigation` acredita Provider y ausencia de GET/PUT automático al volver. |
| s29 | GET manual recupera | El mismo caso confirma lectura200 válida, reemplazo de borrador y ningún falso éxito/recibo. `retains uncertainty after failed recovery ...` tiene filas503 y200 inválido (ciclo79 inicialmente GREEN7c8eae), conserva borrador/bloqueo y anuncia siguiente consulta. Ciclo81 REDbdc345/GREEN7cca13 elimina errores400 obsoletos sólo después del GET válido. |
| s30 | Lectura antigua no pisa PUT | UI ciclos59/66 ejecutados: GET anterior y GET iniciado durante PUT abortados ante confirmación. |
| s31 | Respuestas privadas obsoletas | Cliente `discards a late HTTP401` GET/PUT y JSON abortado; UI seis filas `ignores Ana's late ... after logout and Bruno's appearance has loaded`, con HTTP401/200 y JSON diferido, ciclos53/58/62–65. Incluidas en78focal/2046global. |
| s32 | Logout retira preferencias | UI ejecutada: desmontajeProvider/overrides y cambio propietario sin herencia de snapshot/borrador. Seguridad de sesión real en fixture. |
| s33 | Foco deliberado | `recovers an uncertain PUT ...` comprueba encabezado al desaparecer el iniciador (ciclo55); `does not reclaim focus after a deliberate move followed by blur during recovery` comprueba body después de otra elección (ciclo56), ambos ejecutados en78/2046. La segunda fila contractual (otro control conserva foco hasta el final) no tiene aserción final independiente: no se deduce de la fila con blur. No se añaden tests durante freeze. |
| s34 | Acceso/contraste/responsive | UX aún parcial, ver tdd_appearance_ux_audit.md. Nominal oscuro/recarga1/1 GREEN3f3c9e; geometría124 medidas/4axe0 GREENa05964; teclado/feedback GREENc47fbb. Texto200 RED371ff8 y mínimo CSS, después24medidas/8axe0 GREEN380077 en2temas×4rutas×3anchos,347inputs iguales. Modalidades, zoom nativo, motores y cierre30principios siguen en auditoría C; no se atribuye cobertura universal. |
| s35 | Reloj/corrupción503 | Ejecutado: Save años0/10000/relojfallido/no-op sinClock; PG contraste/rango/metadata corruptos mediante fixture controlado. Replay acredita año9999 real. |
| s36 | SYSTEM sin matchMedia | Ejecutado: matchMedia undefined, SYSTEM permanece seleccionado, light/accentLight confirmados, sóloGET; inicialmenteGREEN841ba3. |
| s37 | Cancelar sin HTTP | `cancels a local draft back to confirmed values without an HTTP request`, ciclo46; mantiene foco en Cancelar y ningún HTTP. Las filas `clears obsolete server errors together with the replaced draft` conservan esa conducta y retiran errores obsoletos (ciclos70/71). |

## Gates y límites vigentes

Backend, HTTP, PIT original/replay y E2E de persistencia están acreditados
con sus límites anteriores. Stryker20 sólo tiene soporte/config revisados,
Node55/55; **resultado de campaña pendiente**. A ha iniciado la original
autorizada (sesión79876,7fuentes/8workers); el inventario provisional no
es un resultado final. Regresión frontend2046/44 y foco78/78
acreditados. Lint/build final EXIT0 1eb847 sobre CSS54BB0E…56DD6;119
entradas finales idénticas. UX final, gate integrado completo y CI del corte
completo20 siguen separados. CI/DNS de otra corrección operativa no los acredita.

Las variantes pendientes de s28/s29 y los huecos s24/s36 están resueltos
con referencias anteriores. s33 conserva el límite concreto de la fila de
foco sin blur; no se inventa evidencia ni se abren pruebas por reflejo.
C tiene ownership de auditoríaUX y A del gate/preflight; no se duplican.

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
corte final. La auditoríaUX y Stryker permanecen separados.
