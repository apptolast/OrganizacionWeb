# Revisión parcial — envío y recuperación de cambios

**CHANGES_REQUESTED**, lectura independiente de9fcd por root del componente, sus35 pruebas y bitácora. No autoría de producción/tests ni suite repetida. Corte examinado: fuente SHA256 `04D15F2DC099C38B6B3E4A34CE5CB5E3CC3A9A2692823F4E476135E600E37775`,35 pruebas verdes77a664.

## Cambio requerido

El estado busy sólo cambia aria-disabled en las acciones. Falta un anuncio de trabajo en curso durante enviar/comprobar, exigido por el estado Transmitiendo del diseño UI13 y feedback accesible de @s40. Incorporar un mensaje de estado sencillo mientras espera una promesa real del test, manteniendo foco y guarda de doble envío. No basta una assertion de className ni retirar los anuncios para simplificar selectores.

## Revisión del resto del corte

- @s33/@s34: clases inciertas conservan intención y sólo permiten comprobación. Ausencia comprobada habilita reenvío manual con mismo cuerpo, key e If-Match/Availability-Revision; no hay POST automático. El cliente API aprobado valida recibo antes de confirmar.
- @s35: errores reconocidos notifican al padre mediante onRejected;412 ofrece consulta deliberada. CSRF reutiliza useSession/apiRequest con renovación manual y reenvío separado. El host del test usa el hook real, no representa una nueva ejecución de SessionGate/App completo.
- @s36/@s38: recibo válido se comunica una vez; guarda síncrona de dobleclick y recuperación de foco sólo si no hay otro control elegido. Padre debe retirar el formulario y consultar vigencia por separado; esa composición tiene su propia revisión.
- @s37: cambio de identidad/revisión desmonta la instancia y aborta la petición. JSON de recibo, error definitivo y CSRF demorados después del cierre no disparan callbacks ni observer obsoletos. No se publica información ni se añade almacenamiento local.

La bitácora conserva la desviación real de granularidad: se introdujeron matrices de variantes simultáneamente en varios focos. No se presentan como ciclos individuales ni se recrea un RED retrospectivo. Existe evidencia de errores observados antes de sus mínimos cambios, y los oráculos posteriores inicialmente verdes se identifican como tales. La revisión y mutación pendientes deben valorar el código y sus pruebas reales; este documento no certifica una ejecución perfecta de TDD ni cierra feature13.

## Revisión del ajuste solicitado

**APPROVED para el componente**, tras lectura63c7f0. El único cambio productivo es un mensaje role=status condicionado por busy. El oráculo individual conserva POST y GET pendientes por separado, comprueba anuncio durante ambas esperas, retirada al terminar, foco y mensaje de incertidumbre. RED2bfef1 y36/36 GREEN133407; formato629722, lintaf06c9 y diffd919fa del autor. Hashes comprobados por root63c7f0: fuente `E12B6C1F5ABB4777976EF3792520D4365D53B8A4F5C8E3F06D1AE057A692FB02`, test `643384ED97528A17A1A98B204D5FA0692CE3FF9973F08BF1A6EB8B700828845F`.

No se detecta producción sin oráculo en el alcance revisado. Se acepta el ajuste funcional y se mantiene visible la desviación procesal documentada; no se altera retrospectivamente. No se repiten suites ni se extiende este dictamen a integración del padre, tipos globales, servidor, UX real o mutación todavía pendientes.
