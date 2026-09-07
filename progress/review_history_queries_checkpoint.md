# Revisión del checkpoint de consulta de historial

Dictamen: **APPROVED para este checkpoint**, con la transacción, integridad y wiring todavía pendientes.

Root revisó aplicación, SQL y las dos suites (abf416, 086588). Los cuatro hashes coinciden; los XML preservados contienen 21 pruebas PostgreSQL y 3 de aplicación, sin fallos, errores u omisiones (f1b654). El foco del autor terminó EXIT0 01ce2f después del formato. No se extrapola este resultado a toda la funcionalidad.

La consulta une las cinco fuentes durables y obtiene las etiquetas actuales mediante contexto propio. Las fuentes de sesiones exigen además su propietario. No usa outbox ni proyecciones como hechos, no añade escrituras y aplica límite SQL de 21. La aplicación entrega sólo veinte y construye el cursor desde el último hecho entregado, conservando upper en continuaciones.

La propiedad del contexto explícito precede a la vinculación semántica del cursor. SQL usa orden descendente por instante, rango de fuente y UUID; Java compara UUID canónico textual para mantener ese orden sin interpretación de entero firmado. La continuación es estricta y no exige existencia de la posición. Los filtros de día usan UTC explícito y no añaden un día al máximo representable. Las pruebas contrastan empates, categorías, contexto vacío propio frente a hechos ajenos, errores de vínculo y medianoche inclusive.

Pendientes antes de aprobación final: transacción RR/read-only que incluya propiedad y consulta, traducción de fallos de consulta y cierre, integridad de recibos seleccionados, commits tardíos y actualización de etiquetas, extremos de calendario reales en PostgreSQL, variantes restantes y aislamiento de las cinco fuentes. El único ejemplo de sesión ajena no acredita por sí solo todos esos caminos. También falta wiring para incorporar el controlador HTTP.

No hay cambio de interfaces ni nuevo índice. Se conserva V14 sin incluir su marca de metadatos. Los planes de validación y mutación siguen vigentes; aún no procede iniciar campañas globales.
