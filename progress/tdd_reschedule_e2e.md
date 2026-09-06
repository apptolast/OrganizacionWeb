# E2E13 — recorrido real en árbol aislado

Autoría E2E independiente de producción. Árbol `OrganizacionWeb-reschedule-e2e`, rama `codex/reschedule-e2e`, corte inicial limpio `407a534`. Frontend integrado; núcleo backend13 en otra pista Codex, pendiente de integración coordinada por root. Claude Code detenido. No copiar WIP, simular respuestas felices ni cerrar feature13 por esta preparación.

Ponytail full/Caveman lite. Contrato `reschedule.feature` aprobado, 41 escenarios; matriz UX30 de `docs/ux-requirements.md` vigente. Root corrigió la ruta sugerida: usar `e2e/reschedule.spec.mjs` y helpers `.mjs` descubiertos por el runner, sin configuración nueva. Puerto18080 reservado durante cada stack.

## Baseline y reutilización

- Dependencias raíz `a3b080` y frontend `af1302`: `pnpm install --frozen-lockfile`, EXIT0, sin cambios de lock. Advertencia habitual de script opcional `@parcel/watcher` no ejecutado; no se cambia política.
- Init del árbol dedicado sesión54324: **EXIT0 f4721d**, lint GREEN,18scripts,1495frontend/28archivos; XML backend4afecf confirma1444tests, cero failures/errors/skipped. Es baseline de407a534, no del backend posterior aún en desarrollo.
- Extracción mínima autorizada por root: `configure` y `openEditor` desde schedule-block a `support/blocks.mjs`, conservando lógica y oráculos. Antes del nuevo RED13 se ejecutará un recorrido11 que usa ambos.
- Autenticación, CSRF, proyectos, tareas y SQL se reutilizan desde helpers existentes. Los once TRUNCATE heredados ya contienen ambas tablas13; el fixture nuevo mantendrá esa lista explícita sin CASCADE.

## Primer ciclo previsto

Un único recorrido UI de creación, movimiento, cancelación e historial. Comprueba respuestas reales y hechos persistidos: identidad estable, creación original inmutable, proyección vigente, recibos históricos y eventos por cambio efectivo. Ningún resultado previsto equivale a prueba ejecutada. Si el endpoint aún falta, registrar el RED concreto y esperar integración; no añadir una batería mientras el primer ciclo permanezca rojo.

## Extracción y primer RED ejecutados

Extracción pura de dos helpers y su constante de días, con la misma ruta de tarea. Diff ec490e/4afecf conserva todas las aserciones del test11. `node scripts/e2e.mjs e2e/schedule-block.spec.mjs:63`: **1/1 PASS, EXIT0 551ece**, stack19064 retirado. El caso ejecuta configure/openEditor, creación y replay reales. Los imports compartidos no duplican login, CSRF ni resolución temporal.

Sólo después de ese GREEN se añade un test en `e2e/reschedule.spec.mjs`. `node scripts/e2e.mjs e2e/reschedule.spec.mjs`: **1FAIL, EXIT1 b2b65e**. El navegador configura disponibilidad, crea proyecto/tarea y bloque real, abre Mover bloque y rellena destino12:00–13:00. `POST B/{id}/reschedule/preview` responde500 `INTERNAL_ERROR`, frente a200 requerido, en línea72. No es un fallo de sintaxis, selector, TRUNCATE ni contenedor. No se alcanza confirmación, cancelación ni historial: esos oráculos permanecen preparados, no verificados. Backend autor confirma que407a534 carece de las rutas Move que su WIP ya prueba; la integración compete a root, sin copia manual de fuentes.

Tras cotejar sección13/662 se retiran dos expectativas no alcanzadas que exigían ETag en POST de recibos: el contrato exige revision textual dentro del cuerpo y Location; el ETag de header se promete en estado/preview. Se conservan esas expectativas contractuales. No se atribuye otro RED ni GREEN a esa corrección de test.

Prettier focal, node --check y diffcheck **eb8466 EXIT0**. Stack63272 retirado por runner, puerto18080 libre. Sin mocks de éxito, cambios de producto/configuración ni nueva suite mientras este ciclo siga rojo. Siguiente acción: integrar corte backend autorizado y repetir este único recorrido; corregir sólo errores demostrados del test o informar defectos de producto a sus autores.

## Alcance UX pendiente

La prueba aún no acredita UX global. Tras cerrar este ciclo, el recorrido responsive debe medir320–2560 y bordes con inputs/selects/buttons del editor, revisión y cancelación; foco al retirar control; carga/error/historial; texto200%, zoom nativo real, axe y tres motores. Las30filas se evaluarán individualmente: observación de jerarquía/lectura y grupos; preservación de contexto y recuperación; controles nativos44px; representación de tiempo planificado sin trabajo inventado; feedback temprano y cierre honesto. Dispositivos físicos, teclado virtual real y comprensión humana siguen pendientes de evidencia específica, nunca PASS automático por emulación. Este párrafo sólo planifica, no sustituye esa futura matriz ni añade tests por adelantado.

## Matriz UX30 aplicada a reprogramación

**Toda la evidencia de esta matriz permanece pendiente para feature13.** El primer RED sólo alcanzó apertura y entrada de destino; no se heredan PASS de Hoy ni se declara facilidad de uso humana por inspección automática. La futura ejecución se añade caso a caso después del primer GREEN.

| ID / principio | Aplicación concreta al panel13 | Evidencia que falta | Estado |
| --- | --- | --- | --- |
| U01 Atención selectiva | Objetivo de sólo lectura y acción de revisión reconocibles; aviso de cierre secundario. | Capturas de editor/revisión/error a320/1440 y lectura de jerarquía. | Pendiente |
| U02 Carga cognitiva | Un editor abierto y sólo destino necesario; cancelar no solicita horas. | Recorrido abrir/mover/cerrar/cancelar sin formularios simultáneos. | Pendiente |
| U03 Estética-usabilidad | Campos y errores coherentes con disponibilidad y creación de bloques. | Capturas con Unicode/texto largo, contraste y recuperación real. | Pendiente |
| U04 Posición en serie | Orden objetivo, inicio/fin/zona, revisión y confirmación permanece comprensible. | Secuencia Tab y orden visual en ancho reducido y escritorio. | Pendiente |
| U05 Tendencia a la meta | Movimiento/cancelación no acredita trabajo ni completa tarea. | SQL de estado pending y ausencia de contador de trabajo inventado tras cambio. | Pendiente |
| U06 Von Restorff | Rechazo, incertidumbre y éxito histórico tienen texto además de color. | Roles alert/status y capturas de estados diferenciados. | Pendiente |
| U07 Zeigarnik | Una operación incierta conserva identificación; cerrar explica que no revoca envío. | ACK perdido real, recuperación por key y aviso antes de salir. | Pendiente |
| U08 Fluir | Se puede abandonar edición sin imponer continuidad; una espera no bloquea navegación segura. | Salida deliberada durante espera y preservación de privacidad. No aplica iniciar/pausar sesión de trabajo, fuera de13. | Pendiente |
| U09 Fragmentación | Editor, revisión, confirmación histórica y estado actual forman unidades distintas. | Regiones/encabezados en DOM y lectura de capturas con ambas representaciones. | Pendiente |
| U10 Memoria de trabajo | Conservar destino ante rechazo; mostrar antes/después y presupuesto junto a decisión. | Error real tras preview y corrección sin reintroducir objetivo/horas. | Pendiente |
| U11 Navaja de Occam | Usar un panel inline; no añadir calendario, modal ni consultas de estado por fila. | Peticiones de red al abrir acciones/historial y controles necesarios. | Pendiente |
| U12 Conectividad uniforme | Identidad estable une reserva y sus hechos; no sugerir relaciones o dependencias nuevas. | Mismo blockId, historial propio y composición sin conexiones decorativas ambiguas. | Pendiente |
| U13 Fitts | Acciones y campos operables con áreas de44×44px, sin superposición. | Geometría de buttons/inputs/selects y etiquetas pertinentes; clic táctil sin hover. | Pendiente |
| U14 Hick | Revisar precede confirmar; consentimiento sólo aparece cuando hay exceso. | Controles visibles antes/después de preview y presupuesto consumido real. | Pendiente |
| U15 Jakob | Inputs locales y select de zona siguen convenciones; enlaces y salida mantienen foco. | Teclado, Enter, Tab, nombres accesibles y retorno tras retirar control. | Pendiente |
| U16 Semejanza | Mismo significado visual para recuperar estado y errores en panel/confirmación/historial. | Comparación de estados y estilos de controles equivalentes. | Pendiente |
| U17 Miller | Campos agrupados por destino, revisión por antes/después; sin umbral arbitrario de opciones. | Agrupación semántica/visual y lectura de contexto largo; comprensión humana aparte. | Pendiente |
| U18 Parkinson | El destino tiene principio/fin explícitos y nunca se extiende solo. | Recibo con intervalo exacto y ninguna escritura al revisar/cerrar. No aplica timer de trabajo. | Pendiente |
| U19 Postel | Validación acordada conserva Unicode; no coacciona revisiones o ambigüedad temporal. | Destino UTC y posterior caso DST con offsets explícitos; error seguro y datos conservados. | Pendiente |
| U20 Proximidad | Ayuda/error junto al campo y asociación programática correspondiente. | label, aria-describedby/invalid, recorte y separación en320px. | Pendiente |
| U21 Prägnanz | Planned/cancelled e histórico/actual se expresan con palabras; no iconos solos. | Textos visibles y árbol accesible después de cancelar y consultar hecho previo. | Pendiente |
| U22 Región común | Un contenedor por edición, revisión y entrada histórica. | Regiones reales y geometría con filas largas; sin mezcla de confirmación y vigencia. | Pendiente |
| U23 Tesler | Mostrar presupuesto y ocurrencias comprensibles; esconder stack/SQL y claves internas. | Rechazo de presupuesto/DST y error seguro, sin detalles técnicos expuestos. | Pendiente |
| U24 Modelo mental | Cambia una reserva, no tarea, creación original ni tiempo trabajado. | SQL/recibo/consulta original y etiquetas históricas frente a estado cancelado. | Pendiente |
| U25 Usuario activo | Vacío y primera acción explican qué hacer; historial se abre deliberadamente. | Estado sin cambios y después de cancelación, sin instrucciones externas necesarias. | Pendiente |
| U26 Pareto | Priorizar mover/cancelar desde la reserva sin perder recuperación e historial. | Recorrido frecuente directo y accesibilidad de alternativas; no porcentajes de uso supuestos. | Pendiente |
| U27 Fin de pico | Confirmación cierta y cierre honesto incluso si no se puede consultar vigencia. | Recibo visible, error de estado separado, foco y recuperación sin otro POST. | Pendiente |
| U28 Sesgo cognitivo | Presupuesto representa tiempo planificado; exceso exige decisión neutral, sin culpa. | Texto de revisión/error, consentimiento deliberado y ningún indicador de productividad. | Pendiente |
| U29 Sobrecarga de opciones | Un editor y revelación progresiva de offsets/consentimiento; cerrar descarta borrador local. | No controles simultáneos de acciones incompatibles; reinicio limpio al cambiar contexto. | Pendiente |
| U30 Doherty | Feedback de revisión/envío aparece temprano sin fingir éxito mientras la red espera. | MutationObserver+performance.now, feedback<400ms y espera real retenida tras route.fetch. | Pendiente |

### Reutilización exacta para la fase UX posterior

- Ya compartidos: `support/authenticated-test.mjs` (sesión/request), `support/projects.mjs` (create/sql aislado), `support/tasks.mjs` (saveTask) y `support/blocks.mjs` (configure/openEditor extraídos). No duplicar autenticación, CSRF ni convertir fechas mediante otro resolvedor.
- Patrón local `inspect(state)` en `today.spec.mjs:213`: usa getBoundingClientRect, scrollWidth, áreas44px,31anchos y axe. **No es export compartido ni se ejecutó para13.** Si se extrae posteriormente, hacerlo en GREEN y sólo por reutilización efectiva; el selector de Hoy no incluye inputs/selects, por lo que deberá cubrir los controles nativos del panel y las áreas de etiquetas que correspondan.
- Matriz de anchos existente:320,359,360,361,390,419,420,421,480,599,600,601,699,700,701,768,820,999,1000,1001,1024,1099,1100,1101,1280,1440,1599,1600,1601,1920,2560; altura reducida768×400. Revisar además bordes de cualquier breakpoint efectivo del panel. Reflow, texto200% y zoom real son verificaciones distintas.
- `today-native-zoom.spec.mjs:79` aporta patrón de extensión Chromium `tabs.setZoom/getZoom`, sesión headful y comprobación geométrica; no se importa su test ni se hereda su resultado. Firefox/WebKit se ejecutan con CLI existente cuando corresponda, sin configuración nueva ni emulación presentada como dispositivo físico.
- `schedule-block.spec.mjs` conserva la recuperación tras ACK real perdido y reinicio de backend, con invariantes de PostgreSQL. No usar `seedAgenda` para mover mediante UPDATE: el nuevo cambio debe entrar por endpoint13 real.

Límites explícitos: axe no certifica contraste o comprensión en todos los estados; una captura no mide foco; viewport emulado no acredita teclado virtual, áreas seguras ni lector de pantalla real. La revisión humana de los30principios y la evidencia física se marcan según lo que se observe, sin convertir limitaciones en PASS.
