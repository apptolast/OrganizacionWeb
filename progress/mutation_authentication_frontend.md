# authentication — mutación frontend

Ejecución 62167 finalizada: 302/355 detectados (85,07 %), EXIT 0. Stryker 10 instrumentó 355 mutantes con dos workers, Vitest 4.1.10, coverageAnalysis perTest y umbral 80 sin reducción. Baseline completo: 241 pruebas y lint verdes; build verde.

Configuración reproducible: `frontend/stryker.authentication.config.json`. Informes separados: `frontend/reports/mutation-authentication/mutation.json` y `.html`.

Alcance exacto: `src/api-client.ts`, `src/session-api.ts` y `src/use-session.ts` íntegros; `src/session-gate.tsx:22:0-31:100` cubre foco y condición de acceso privado; `src/use-create-project.ts:10:0-11:100` y `:40:0-46:100` cubren cancelación; `src/projects-api.ts:43:0-43:100` cubre propagación de señal. read-projects-api/edit-project-api sólo sustituyen el identificador fetch por el cliente compartido, sin nueva rama propia; su integración queda comprobada por tests y navegador. No se repite la mutación de validadores históricos intactos.

El JSX restante, estilos SCSS y composición de main/App/workspace tienen pruebas de comportamiento y navegador; no se les atribuye esta puntuación. La configuración global `stryker.config.json` incorpora los tres módulos nuevos y la lógica del gate para verificaciones futuras. No se excluyeron mutantes del alcance ni se rebajó el umbral.

La clasificación completa se conserva a continuación. Los replays son ejecuciones independientes y no cambian la puntuación original.

## Resultado completo y revisión

La ejecución 62167 terminó con EXIT 0 en 11 min 15 s: **302/355 detectados (85,07 %)**. Se observaron 53 supervivientes, 0 sin cobertura, 0 timeouts y 0 errores. El umbral 80 está superado. La puntuación pertenece exclusivamente a esa campaña; los replays no se sumarán para inventar una segunda puntuación global.

Se añadieron 19 regresiones y se reforzaron aserciones existentes sin modificar producción. Ejecución 79967: **260/260 pruebas completas** y lint verdes. Una aserción adicional en el ejemplo inicial observa el DOM del primer commit React, para no confundir un error transitorio antes del efecto con equivalencia; su prueba focal también pasó. El total continúa siendo 260.

### Supervivientes originales

IDs del JSON original. Las equivalencias describen el flujo público actual, no llamadas arbitrarias a hooks internos.

| IDs / archivo | Clasificación y fundamento |
| --- | --- |
| 5, 7 / api-client | Hueco real: un cuerpo CSRF_INVALID en HTTP 503 no debe provocar renovación. Regresión añadida y replay focal. |
| 10 / api-client | Equivalente: JSON inválido produce undefined en vez de null; ambos se rechazan por el mismo guard sin notificar. |
| 52, 53 / api-client | Huecos reales: señal y listener son opcionales. Regresiones con CSRF válido sin signal y con/sin listener; replay focal. |
| 84, 85, 87 / session-api | Equivalentes: primitivas fallan en claves/campos posteriores; null provoca error controlado. No se acepta otra sesión JSON. |
| 94, 102 / session-api | Huecos reales de tipo: authenticated truthy/falsy no booleano y username numérico. Se exige rechazo con error seguro; replay focal. |
| 121 / session-api | Equivalente: cambia el texto del error interno que el gate no muestra. |
| 129 / session-gate | Equivalente: todas las ramas del gate y las vistas privadas montan main con h1 antes del efecto de layout. No existe una rama admitida sin encabezado. |
| 132 / session-gate | Hueco real: tabindex positivo altera el orden de teclado aunque permita focus. La prueba exige -1; replay focal. |
| 151 / use-create-project | Equivalente: dependencias vacías sustituidas por una constante estable conservan montaje y cleanup. |
| 154 / use-create-project | Equivalente en el árbol actual: el cliente ya evita la notificación global de una señal abortada; la respuesta restante sólo actualiza el componente de creación desmontado. |
| 158 / use-session | Hueco real de primer commit: iniciar con failure=true puede mostrar error transitorio antes de refresh. Se observa el DOM inicial mediante Profiler, sin leer estado interno; replay de un mutante. |
| 159, 160, 162, 256 / use-session | Huecos reales: campos y error inicial deben estar vacíos, y un nuevo envío no presenta un error inventado. Aserciones de DOM y replay focal. |
| 164 / use-session | Equivalente: recoveryFailed se restablece síncronamente al arrancar refresh antes de que exista sesión o aviso CSRF que pueda mostrarlo. |
| 171, 197 / use-session | Huecos reales: el primer aviso CSRF no es un fallo de renovación; un GET fallido sí debe anunciarlo conservando borrador. Recorrido añadido y replay focal. |
| 181 / use-session | Equivalente: modifica el argumento histórico de título de replaceState, ignorado por la API History del navegador. No modifica ruta ni estado. |
| 184 / use-session | Equivalente para los retornos admitidos: la normalización de una URL ajena ocurre antes de montar App. Los enlaces internos generan rutas válidas; si la ruta ya es raíz, el snapshot de useRoute no cambia. Alterar el nombre del evento no cambia el retorno público de este contrato. |
| 191 / use-session | Hueco real: rechazo tardío del GET cancelado podía ocultar una sesión nueva. Regresión de StrictMode y replay focal. |
| 200, 217, 243 / use-session | Equivalentes de comportamiento público: tras fallo, revocación o cleanup no hay vista privada que pueda escribir; las peticiones existentes se abortan. Antes de cualquier nuevo montaje privado, un GET válido sustituye el token. El valor residual queda sólo en memoria inaccesible al recorrido, sin persistencia ni envío nuevo. Se conserva la limpieza de producción. |
| 209, 211 / use-session | Huecos reales: revocar durante login debe vaciar contraseña y habilitar un nuevo acceso. La prueba exige ambos estados antes de escribir otra contraseña; replay focal. |
| 242, 245, 328 / use-session | Equivalentes: al ejecutar cleanup ya se crearon el controlador del GET y BroadcastChannel; al publicar logout el canal existe y la operación no está abortada. Quitar el encadenamiento opcional no altera esas invariantes. |
| 244 / use-session | Equivalente para el lifecycle actual: todas las peticiones del árbol desmontado se abortan y el cliente ignora sus respuestas. Un gate nuevo registra su listener antes de montar vistas. El listener residual no recibe una operación pública vigente; se conserva su cleanup en producción. |
| 248 / use-session | Equivalente: una dependencia constante conserva el efecto único y su cleanup. |
| 250 / use-session | Hueco real: sin preventDefault el formulario ejecutaría navegación nativa. La prueba verifica que el evento DOM cancelable queda cancelado; replay focal. |
| 274, 332 / use-session | Equivalentes: quitar el primer guard de catch sólo permite parsear el problema; el segundo guard tras await impide toda modificación cuando está abortado. |
| 297 / use-session | Equivalente en la interfaz: al iniciar logout desaparece el botón y la vista privada; no queda control que invoque de nuevo ese handler mientras closing es true. |
| 314, 320 / use-session | Huecos reales: logout401 con GET fallido conserva el token para reintentar; si GET sigue autenticado tampoco reabre datos. Ambos recorridos y replay focal. |
| 327 / use-session | Hueco real: después de logout204, un GET abortado por una comprobación de visibilidad no puede reabrir la sesión anterior mientras el segundo GET sigue pendiente. Prueba de entrelazado y replay focal. |
| 340 / use-session | Hueco real: el finally de un logout cancelado no debe retirar la espera de un segundo cierre. Prueba con dos operaciones separadas por revocación y replay focal. |
| 351, 356, 357, 358, 359, 372, 373 / use-session | Huecos reales: cursor vacío/repetido, parámetros extra y prefijo/sufijo de una ruta válida deben descartarse. Regresiones y replay focal. |
| 369, 371 / use-session | Equivalentes: rechazar la raíz válida provoca replaceState a la misma raíz, con estado ya nulo y snapshot de ruta idéntico. No cambia el contexto ni produce navegación externa. |

### Replay selectivo

`stryker.authentication-replay.config.json` reproduce 79 mutantes en las líneas afectadas de api-client, session-api, session-gate y use-session. Informes separados en `reports/mutation-authentication-replay/`. Ejecución 14627: **79/79 detectados**, EXIT 0, 0 supervivientes/sin cobertura/timeouts/errores, en 2 min 38 s. Detecta los 28 supervivientes originales incluidos en esas líneas. El mutante 158 se comprueba aparte mediante `stryker.authentication-first-commit.config.json`, una única línea, sin repetir la campaña global; ejecución 94937 terminada: **1/1 detectado**, EXIT 0, sin supervivientes, en 10 segundos. El baseline de ese replay ejecutó los 73 casos de autenticación, incluida la aserción del primer commit.



Cierre de revisión: 29 de los 53 supervivientes originales quedan detectados por los replays separados; los otros 24 tienen equivalencia justificada y revisión independiente de la raíz. El emparejamiento del primer replay usa archivo, localización, mutador y replacement, no coincidencia de IDs entre ejecuciones. **El único score de campaña completa sigue siendo 302/355 (85,07 %)**. No se modificó producción durante el refuerzo de pruebas; no se repitió la campaña global.
