# read_projects — frontend

Autorización persistente del usuario confirmada por coordinador; init37769 exit0 antes de producción. Ownership frontend exclusivamente, React/pnpm/SCSS sin Tailwind. Se conserva `/` como formulario de creación; nuevas rutas `/proyectos` y detalle. Contrato32 escenarios/50casos y docs/ux-requirements.md leídos.

## Ciclos observados

1. @s14: RED App no tiene enlace a proyecto persistente; GREEN lectura GET real mediante fetch, lista semántica y enlaces por id. Credenciales same-origin y cache no-store.
2. @s16: RED no existe status accesible y se renderiza lista antes de respuesta; GREEN estado inicial sin items con Cargando proyectos inmediato. Un primer comando -t sin comillas perdió argumento por PowerShell; no se cuenta como RED funcional.
3. @s15: RED vacío no orienta; GREEN mensaje exacto y Crear proyecto enlazado al formulario existente.
4. @s17: RED red/503/500 no presenta error y red rechaza sin captura; GREEN mensaje propio sin detalles del servidor y Reintentar misma página. Seis casos focused verdes.

5. @s18: RED no existe Más antiguos; GREEN URL con cursor opaco, nueva página, foco h1 y regreso al inicio mediante History API sin dependencia de router.
6. @s19: RED página posterior vacía pierde regreso y dice incorrectamente que no hay proyectos; GREEN distingue final de página y conserva inicio.
7. @s20/@s30: RED detalle directo no se muestra; GREEN nombre/descripcion como texto, fechas time/datetime en UTC y regreso, sin ownerId visible.
8. @s32: RED anuncio plural incorrecto para detalle y falta regreso durante espera; GREEN anuncio singular inmediato y enlace.
9. @s31: RED error detalle usa mensaje lista y pierde regreso; GREEN red/503/500 con retry mismo recurso.
10. @s21: RED404 se presenta genérico; GREEN Proyecto no encontrado y regreso sin datos ficticios.
11. @s27: RED401 genérico; GREEN autenticación requerida sin datos previos tanto lista como detalle.
12. @s22: RED petición antigua sin signal abortado; GREEN AbortController por lectura y cleanup al cambiar ruta.
13. @s22 StrictMode: RED respuesta vieja reemplaza vigente durante repetición de efectos; GREEN comprobar signal antes de publicar éxito/fallo.
14. Respuestas incompatibles: RED7/8 variantes causan espera infinita, contenido inválido o error de render; GREEN validación de DTO antes de actualizar estado, incluida identidad de detalle y ambas fechas. Un fallo TypeScript de estrechamiento de ownerId se corrigió antes del primer build verde comunicado a integración.
15. Navegación desde creación: se actualiza expectativa del enlace Proyectos al nuevo recorrido; RED href antiguo, GREEN RouteLink /proyectos. El resto de creación conserva contrato.
16. Respuestas fuera de contrato: RED201 y nextCursor vacío aceptados; GREEN200 exacto y cursor no vacío o null.

Refactor en verde: Workspace compartido, hook useReadProjects y adaptador read-projects-api separados del JSX, navegación pequeña con History API. Tests de regresión para modifiers del enlace, rechazo cancelado y ausencia de local/sessionStorage pasan; no se inventa rojo porque reutilizan lógica ya implementada.

Diseño: lista semántica de tarjetas con nombre íntegro/estado/fecha, detalle de descripción completa, jerarquía y paleta coherentes con creación, SCSS con wrap de palabras largas, controles44px, estados vacíos/cargando/error. Matriz responsive/axe/navegación queda a cargo de integración y revisión visual; no se atribuye cumplimiento universal por JSDOM.

Verificación frontend24834 exit0: lint,73 tests completos y build. Mutación45125 en curso,297 mutantes en projects-api/use-create-project anteriores y read-projects-api/use-read-projects/navigation nuevos. No se baja umbral80 ni se excluye la nueva lógica. Los38 tests del corte anterior permanecen, salvo href Proyectos que ahora apunta al recorrido aprobado.

## Trazabilidad frontend

- @s14: recuperación por URL/lista con GET no-store; persistencia real reforzada en E2E de integración.
- @s15/@s16: vacío confirmado y anuncio de espera inmediato.
- @s17/@s18/@s19: errores/retry, URL de cursor, foco, fin de página y retorno.
- @s20/@s30: detalle directo, literalHTML/Unicode, fechas time en UTC, regreso; E2E verifica reload.
- @s21/@s27:404/401 sin conservar datos anteriores ni afirmar borrado.
- @s22: aborto en cambio de ruta y guards de éxito/rechazo bajo StrictMode.
- @s23/@s24/@s25/@s26: semántica/controles SCSS; integración ejecuta matriz anchos/teclado/táctil y zoom nativo. Ver matriz completa de30 principios en progress/ux_read_projects.md, con autoría y límites explícitos.
- @s29: cache no-store y ausencia de escrituras localStorage/sessionStorage.
- @s31/@s32: error de detalle red/503/500 recuperable y espera singular inmediata.
- @s1–@s13/@s28: contrato servidor, propiedad backend; UI no sustituye evidencia de autorización/SQL/cursores.
