# Revisión independiente de UI21

## Dictamen funcional JS final

**Pendiente físico posterior al dictamen estático:** Chromium real detectó @s39 recuperación de valores sin restitución de foco. El diagnóstico c29fee (E2E EXIT1 14c758, snapshot final sin cambios) observa Enter/click en Recargar guardado y focusout automático al quedar disabled, todavía conectado, con activeElement BODY. No hay movimiento voluntario/focusin; onBlurCapture elimina initiator por isConnected y ya no restaura al desaparecer el botón. B/root informados; no se aprueba cierre físico de foco hasta el delta y la repetición del mismo caso. El test unitario159 no acreditaba esta conducta del navegador; el caso162 de foco voluntario debe conservarse.

**Resuelto por164:** ambos TSX usan matches(':disabled') efectivo para conservar el iniciador ante blur automático, manteniendo abandono por foco voluntario. B acredita55/55incl162 (081fab). Copia exacta3734C3…D5C5/40D34D…910A y mismo E2E Chromium GREENaccb98: encabezadofocused con outline3px/top300.25, feedback3.5ms y una sola escritura real.578inputs antes/después sin cambios903426. El pendiente anterior queda cerrado en este corte; modalidades/motores restantes siguen independientes.

**APROBADO en código/oráculos JS, con UX física y gates globales separados.** Corte `customization_frontend_final_freeze.json`, SHA256 `535514DF930DE15FC706DE783D8BFD4DBBE9AB4986505BC1BD4486C4949AB571`;14/14 hashes contrastados y copiados al aislado1126a1. Pruebas151/151 de cinco suites y tipos EXIT0 678435; formato/lint25640d, log final leído. No se ejecutaron suites de revisión.

Los pendientes de recuperación GETschema quedan cerrados por acceptConfig compartido y comparación con valores ya conocidos. Los oráculos145/147 cubren recuperación tras412 y primera configuración posterior a detalle directo. La revisión de estado independiente de root documenta además ACK/GET retrasados resueltos por154/155; no se vuelven a declarar hallazgos abiertos aquí. La aceptación de valores compara identidad/versión de esquema antes de publicar; schema nuevo puede retirar PUT pendiente y conservar incertidumbre para GET manual.

Se revisaron los deltas144–162: TEXT largo/NUL/surrogate, DATE con extremos nativos, duplicados/12 incluyendo inactivos, Cancelar edición y eliminación de mensajes anteriores. Foco: región lógica/encabezado tabIndex-1, captura sólo iniciador enfocado, restaura únicamente si desaparece y body conserva foco, abandona restauración tras foco voluntario. Las pruebas unitarias no sustituyen el recorrido físico que C ejecuta aparte. Cancelar edición precede a Crear/Guardar en el fieldset, compatible con el CSS revisado por root.

El texto y mapas anteriores describen la evolución y límites de los cortes previos. No invalidan este dictamen final JS, pero tampoco se convierten retrospectivamente en evidencia del corte final. CSS es autoría C revisada por root; no se autoaprueba en este informe. Matriz/modalidades físicas siguen en el aislado `progress/ux_customization.md`, sin atribución de dispositivos reales o comprensión humana.

Estado: revisión preliminar; **sin aprobación final**. B todavía termina guardas, montaje y estilos. Lectura de COMMON; no modificaciones de producto/tests ni ejecuciones o stack por esta revisión. Los clientes revisados previamente y los resultados unitarios comunicados por B no sustituyen un freeze final. No se fija un hash de una fuente que sigue cambiando.

Actualización del corte parcial: **APROBADO únicamente para integración nominal y primer E2E**, no para cierre UI/UX. Manifiesto `customization_frontend_ui_partial_freeze.json` SHA256 `8A29A26666C06D93CFB011B883DD04D3280A822117410C77BD777BF381A67586`; los14 hashes se verificaron contra las fuentes quietas. Log `customization_ui_partial_tests_final.log`:142/142 en5 suites; B registra tipos EXIT0 d0bcab y lint EXIT0 085b35. Se leyeron los deltas y sus oráculos, sin ejecutar suites de revisión.

Los dos hallazgos siguientes quedan cerrados en este corte: generación por recurso provoca nueva lectura del consumidor limpio incluso montado; dirty conserva snapshot/borrador y bloquea guardar hasta recuperación manual válida. Los cuatro casos de esquema prueban limpio/dirty, retorno, montado y edición iniciada durante PUT. La incertidumbre mantiene su guarda previa; no se afirma que esos cuatro casos inyecten además un PUT incierto antes del cambio de esquema. El error400 general values ahora se anuncia con role alert y conserva borrador/posibilidad de corregir. La recuperación404 propaga retirada al padre y elimina los controles; callback ref actualizado por efecto conserva el último consumidor. No se encontró otro bloqueo concreto en este alcance parcial.

Siguen pendientes expresamente validaciones locales, acabado de formularios, mensajes/foco y SCSS. Los hallazgos originales se conservan a continuación como trazabilidad, no como defectos aún abiertos. Root autorizó copiar este snapshot al aislado para el primer E2E; ello no acredita controles físicos ni los treinta principios de la tabla.

**Nuevo pendiente concreto comunicado por root después de la copia:** loadConfig/reloadConfig acepta un esquema nuevo obtenido por GET sin aplicar la invalidación que sí realiza changeConfig. Recorrido: detalle cacheado, otra pestaña cambia esquema, PUT de configuración recibe412, Recargar guardado obtiene esquema nuevo y regreso al detalle. Los valores podrían conservar el esquema anterior. B tiene asignada la invalidación compartida al aceptar GET/PUT, preservando dirty/uncertain. El primer E2E nominal puede continuar, pero este corte no se aprueba para recuperación completa hasta revisar ese delta y su oráculo.

## Hallazgos concretos

1. **Esquema conocido obsoleto.** useCustomizationSession.changeConfig publicaba configs actualizado, pero conservaba values/valueReads de la revisión anterior. Recorrido: abrir detalle y cargar valores; volver a gestión; renombrar/desactivar una definición con200; volver al detalle. La promesa cacheada evita GET y permite volver a presentar el campo/etiqueta anterior. B lo confirmó; root ratificó la corrección. Una instantánea limpia puede invalidarse y releerse al entrar. Con borrador o incertidumbre debe conservarlos, avisar y exigir recuperación manual, sin auto-liberar el bloqueo ni descartar lo escrito. Pendiente revisar el oráculo y delta final.

2. **Error de conjunto no anunciado.** custom-fields-api admite un problem400 válido con field=values, pero el catch leído de CustomFieldsPanel sólo copiaba mensajes con values[i].fieldId/value. El error general dejaba el mapa vacío y terminaba sin explicación. Comunicado a B; pendiente ratificación/corrección y evidencia. Los índices fuera de rango sí se rechazan en el decoder; no se reporta ese defecto inexistente.

Pendientes ya declarados por B, no hallazgos nuevos: retirada del padre tras404 de TaskReader,404 de recarga manual, foco, validación local TEXT/DATE y límite de definiciones, mensajes obsoletos y estilos. No se presupone que estén cerrados por cambios observados durante una lectura concurrente.

## Aspectos inspeccionados y límites

- El estado se aloja en App, montado bajo la sesión/AppearanceProvider con identidad de cuenta; separa configuración PROJECT/TASK y valores projectId/taskId. No realiza GET por el mero montaje global del hook.
- Los consumidores visibles inician sus lecturas; hay deduplicación por scope/recurso. Las listas mapean metadatos según el orden configurado sin GET de valores por fila. No se añade ruta global.
- Escrituras inciertas se guardan en estado de sesión y no se liberan al volver; se necesita distinguir esta garantía del defecto de invalidación de esquema indicado arriba.
- Lecturas de valores tienen señales y retiro por componente; las respuestas de decoder se comprueban después de leer el body. La revisión final debe contrastar abortos/401 tardíos y cierres de sesión con los tests congelados, sin inferirlos sólo de una rama del hook.
- Los formularios de negocio se mantienen en sus componentes y la personalización se añade como hermana. La conservación real de sus borradores y del foco requiere pruebas de montaje/teclado; no basta con leer JSX.
- Los clientes validan representaciones cerradas, revisiones e intención de confirmación. La UI no debe transformar un GET de recuperación en recibo atribuible a un comando perdido.

## Las treinta filas UX: alcance de esta revisión

Todas quedan pendientes de evidencia final de UI21 salvo las no aplicables indicadas. Lectura estática no acredita geometría, contraste, latencia ni comprensión humana.

| Principio | Aplicación y evidencia todavía necesaria |
| --- | --- |
| Atención selectiva | Jerarquía de Guardar vista/Gestionar/Guardar campos en capturas y estados |
| Carga cognitiva | Separación de formularios y contexto PROJECT/TASK; recorrido real pendiente |
| Estética-usabilidad | Estilos finales, legibilidad de éxito/error y ambos temas pendientes |
| Posición en serie | Orden visual/DOM configurado y recorrido de teclado pendientes |
| Tendencia a la meta | No introduce cálculo de avance; no aplicable a metadatos21 |
| Von Restorff | Aviso incierto y acción de recuperación distinguibles sin depender del color |
| Zeigarnik | Conservar borradores ante errores/navegación; verificar freeze |
| Fluir | No cambia duración/acciones de sesión; preservar borradores ajenos en montaje |
| Fragmentación | Agrupación contextual de vista, definiciones y valores |
| Memoria de trabajo | Borrador/error recuperable; hallazgos1/2 pendientes |
| Navaja de Occam | Controles nativos y sin ruta global; comprobar recorrido completo |
| Conectividad uniforme | No añade relaciones visuales nuevas; no aplicable salvo agrupación semántica |
| Fitts | Medir44×44 y separación; pendiente stack final |
| Hick | Revelación de opciones avanzadas y prioridad de acción |
| Jakob | Checkbox/select/date, Cancelar y enlaces convencionales; teclado/foco pendientes |
| Semejanza | Consistencia entre ambos scopes y detalles |
| Miller | Grupos por función, no límite cognitivo inventado de siete |
| Parkinson | No cambia planificación ni fin de sesión; no aplicable al cambio21 |
| Postel | Unicode/tipos/null/rangos y mensajes locales; tests finales pendientes |
| Proximidad | Etiqueta/ayuda/error asociado; error general señalado en hallazgo2 |
| Prägnanz | Estados explícitos: provisional, guardando, incierto, desactivado |
| Región común | Formularios separados y contenedores funcionales |
| Tesler | No mostrar revisiones/IDs técnicos como decisiones del usuario |
| Modelo mental | Campos personales separados de hechos/progreso; no reabrir entidades |
| Usuario activo | Vacío y orientación para crear definiciones sin manual |
| Pareto | Defaults útiles y personalización contextual; sin porcentajes de uso supuestos |
| Fin de pico | Confirmación cierta y recuperación manual, sin falsa atribución del GET |
| Sesgo cognitivo | Sin nuevas métricas ni presión; opciones neutrales conservadas |
| Sobrecarga de opciones | Máximo12 explicado, categorías y restauración sólo de vista |
| Doherty | Medir feedback<400ms y espera honesta; aún sin medición |

Pendiente tras freeze: anchos y breakpoints,320px, texto/zoom real200%, contenido largo/12 campos, tres motores, temas/forced-colors/reduced-motion y foco. Dispositivos físicos, teclado virtual y evaluación humana se declararán con sus límites reales; no se extrapolará desde emulación o axe.
