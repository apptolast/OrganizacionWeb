# TDD frontend23

Contrato aprobado en5feebd7; sección23 SHA959BE37196F6E07FFAF969C1824BB33C86DCAE3E6DBD4872DF15437BEDF49369. Se aplica Ponytail full/Caveman lite, un test por ciclo. Sólo PRIMARY import-data; no COMMON/V14 ni8080. Baseline oficial suministrado por root; no se repite global por ciclo.

Cada ciclo ejecuta `pnpm --dir frontend test -- src/import-data-api.test.ts`. Logs propios `progress/import_frontend_NNN_red.log` y `_green.log` conservados. Todos los RED siguientes fueron EXIT1 y su GREEN EXIT0, sin timeout/retries ni tests anticipados.

| Ciclo | Oráculo público y causa RED | GREEN focal |
| --- | --- | --- |
| 001 | @s18/@s34 File original, POST y CSRF existentes, preview nominal14 vacío; módulo ausente | 1/1 |
| 002 | @s18 envelope con campo adicional antes aceptado | 2/2 |
| 003 | @s18 preview de otro owner antes aceptado | 3/3 |
| 004 | @s18 hash de otros bytes antes aceptado | 4/4 |
| 005 | @s17 File32 MiB+1 antes llegaba a fetch; ahora rechazo previo a envío/lectura | 5/5 |
| 006 | @s18 byteLength discordante antes aceptado | 6/6 |
| 007 | @s40 señal ya abortada antes enviaba | 7/7 |
| 008 | @s40 aborto durante fetch antes consumía JSON | 8/8 |
| 009 | @s40 aborto durante JSON antes leía File | 9/9 |
| 010 | @s40 aborto durante lectura File antes invocaba Web Crypto | 10/10 |
| 011 | @s40 digest terminado después de aborto antes aceptado | 11/11 |

Refactor002: fixture con hash real para que envelope inválido no quede encubierto cuando se añade guardia de hash; resultado2/2 EXIT0 en `_002_refactor.log`. Tags iniciales corregidos a escenario18 (preview cerrado), sin cambiar oráculos.

El cliente aún es parcial: transporte/identidad/bytes/abortos, no validación completa de counts, formato, timestamps, runningSessions ni status/errores. Confirmación, recibos, intención persistida, UI y actualización de snapshots siguen pendientes. No se atribuye cierre de cliente, UI o feature a estos once tests. No global, mutación ni Git propios.

## Corte temprano de preview

Ciclos012–026, uno cada vez: formato, versión tipada, timestamp civil inválido, counts incompletos, colección extra de insertCounts, fracción coherente, suma discordante, límite total100001, runningSessions noarray, dos sesiones, campoextra, UUID inválido, timestamp sin microsegundos, aviso sin inserción y response503 no consumida como preview. Cada `_red.log` EXIT1 y `_green.log` EXIT0 con conteo creciente de12 a26; no se añadieron casos por lote.

Refactor final conserva guardias y expone ImportPreview tipado. El primer tsc encontró incompatibilidad sólo de declaraciones NodeFile/DOMFile (BYOB stream): la prueba usa File nativo de Node para arrayBuffer, ausente en JSDOM, con adaptación del constructor únicamente en tests y comentario explícito. No se cambió producto para acomodar ese fixture. Tipos finales, ESLint y Prettier EXIT0 en import_frontend_preview_checks.log; focal26/26 EXIT0 en import_frontend_preview_freeze.log. No build/global ni campañas.

Freeze temprano: dos fuentes del cliente/fixture, bitácora y dos logs finales inventariados en import_frontend_preview_freeze.json. Cubre transporte y decoder de vista previa, no confirmación/recibo/errorUI ni flujo de importación completo. Las familias de errores se entregan como Response al futuro consumidor para clasificación segura. Faltan positivos de frontera/sesión con datos, implementación de confirmación y recuperación, intención persistida y UI. No se afirma que los26 tests validen todos los174 ejemplos backend.

## Intención mínima y positivos del cliente

Ciclos027–036: lectura nominal; retirada por propietario distinto sin tocar otras claves; campos privados extra; JSON malformado; key inválida; digest mayúsculo; guardado nominal; almacenamiento que descarta silenciosamente; omisión de datos incidentales del caller; limpieza acotada. Cada ciclo tuvo RED EXIT1 y GREEN EXIT0 en sus logs numerados. Todavía son pruebas del módulo, no acreditan integración logout ni UI.

037 acepta exactamente32 MiB de File y100000 proyectos en counts, archivo con registros generados y padding JSON válido;038 acepta warning con null legado;039 conserva timestamp histórico con6 microsegundos. Los tres fueron inicialmente GREEN, sin cambio productivo ni RED fabricado. Son validaciones del cliente con Response simulada; no acreditan importación backend de esos registros.

040: la hipótesis de que el regexp admitía LF final no se reprodujo: `_040_red.log` contiene realmente EXIT0/11 pruebas. Se conserva el nombre original del log y se clasifica como inicialmente GREEN, no RED. Se retiró la comprobación redundante de longitud añadida provisionalmente; sólo quedó refactor de retorno tipado de intent. Focal combinado40/40 EXIT0 en import_frontend_040_refactor.log. No hay un bug corregido atribuible a040.

La bitácora del freeze inicial se conserva exactamente en import_frontend_preview_tdd_snapshot.md; el manifiesto inicial mantiene su referencia histórica a la versión anterior de esta bitácora. Las dos fuentes liberadas por root se siguen desarrollando después de su revisión parcial, sin reescribir los resultados iniciales. Próximo: cliente de confirmación/recibo y después consumidores UI.

## Cliente de confirmación y consulta

041–054: confirmación nominal File/key/hash/CSRF; rechazo de bytes diferentes antes de POST; respuesta409 conservada; recibo con campo extra; key ajena; hash distinto; byteLength distinto; recordedAt inválido; counts incompletos; IMPORTED sin inserciones; suma100001; recuperación nominal sin File; longitud imposible en recuperación; key de ruta inválida. Cada ciclo tuvo RED EXIT1 y GREEN EXIT0, una prueba cada vez. Se extrajeron lectura/status/guardas y hash acotado comunes conservando los oráculos de preview;043_refactor EXIT0. Ningún cambio de api-client global.

055 (404 conservado) y056 (recuperación IMPORTED en fronteras32 MiB/100000) fueron inicialmente GREEN. Resultado56/56 en2 suites, EXIT0 en import_frontend_client_final.log. Tipos y ESLint EXIT0 antes del formato final; el primer check de Prettier falló únicamente en import-data-api.test.ts y se conserva en import_frontend_client_checks.log. Reaplicar Prettier a ese archivo y check de cuatro fuentes EXIT0, log import_frontend_client_format_fix.log. Cambio sólo de formato, no se repite la suite por ello.

Este corte expone ImportPreview, ImportReceipt e ImportIntent sin casts de datos recibidos ni parser de registros del archivo. Freeze: cuatro fuentes, tres logs finales y copia inmutable de bitácora, con import_frontend_client_freeze.json. No acredita integración global: errores de storage se entregan al futuro consumidor; sesión/ruta/UI, foco físico y refresh siguen en cola. No Java/proxy ni campañas propias.

## Vista nominal y recuperación inicial

057–064: selección sin envío; preview con segundo gesto; tabla de14 colecciones y metadatos; cambio deFile retira confirmación; guardado mínimo antesPOST y recibo confirmado; red incierta bloquea nueva importación; fallo removeItem no convierte recibo en fallo; recuperación deliberada tras reload sinFile. Cada caso se escribió, ejecutó RED EXIT1 y después GREEN EXIT0. Último focal8/8 en import_frontend_064_green.log.

WIP explícito: faltan control busy/cancelación/lifecycle, fallos de acceso/storage, errores HTTP visibles, foco, integración route/session/isPrivateRoute, callbacks de refresh y snapshots. Root recordó que /importacion debe conservarse tras login en isPrivateRoute; queda en cola con oráculo público. No se afirma freeze de vista ni integración logout. Cliente+intent revisados e integrados por commit autorizado cc6f91c, logs locales preservados según ignore existente.

## Integración y privacidad en curso

065–072: callback de refresh fallido no deshace recibo; bloqueo de duplicados en preview/confirmación;413 seguro; cambioowner aborta antes layout; recuperación abortada no limpia intención nueva; storage get/set inaccesible no envía. RED→GREEN individual, logs correspondientes.073 monta nav/ruta sin GET;074 login directo conserva /importacion;075 logout desde otra ruta retira metadata;076 corrección root UUID con letras mayúsculas (guardia local, helper heredado intacto);077 removeItem fallido no impide logout;078 refresh de cuenta distinta retira intención previa;079401 vigente retira antes de recheck;080 cancelar preview aborta, suelta File e ignora respuesta tardía. Todos RED EXIT1→GREEN EXIT0.

Último focal de vista: 17/17, EXIT 0 en import_frontend_080_green.log. Integración auth/App se verifica focalmente con selectorimport23; todavía pendiente regresión de esas suites completas al freeze. Falta tratamiento restante de errores/reenvío manual, foco físico y DOM, refresh appearance/customization y SCSS. Ningún global/mutación. Las fuentesUI están WIP, no son freeze final.

## Recuperación y foco del consumidor

081–083: el 404 conserva incertidumbre y exige reselección; el reenvío deliberado conserva clave y bytes; un archivo distinto no se envía y explica cómo corregirlo. RED y GREEN individuales preservados. 084–085 refuerzan casos existentes para retirar éxito y error obsoletos al cambiar archivo. 086 añade aviso de sesión histórica. Último resultado de ese tramo: 21/21 en import_frontend_086_green.log.

087 muestra cantidades confirmadas por las 14 colecciones y recordedAt en recuperación sin File. 088 enfoca el encabezado inicial y conserva el destino del enlace de salto. Ambos RED y GREEN reales. 089 tuvo primero un fallo de precondición del test: JSDOM no retira foco al llamar blur sobre un botón disabled. Los logs 089_red y 089_green conservan ese fallo de montaje, no prueban un defecto de navegador. Se retiró esa simulación y se deshizo el cambio provisional; 089_actual_red acredita el foco perdido cuando desaparece realmente el botón tras el recibo. El mínimo de captura/restauración pasa en 089_actual_green. 090 conserva ese oráculo y añade movimiento voluntario a otro control seguido de blur: RED y GREEN, sin recuperar foco indebidamente.

Último resultado: 25/25 en import_frontend_090_green.log. El comportamiento físico de disabled sigue pendiente de navegador; no se atribuye esa evidencia a JSDOM. Tipos EXIT 0 en import_frontend_ui_types_green.log después de precisar username no nulo en la integración de sesión. Faltan actualización de snapshots afectados, estados HTTP restantes y validación UX/SCSS; no hay freeze funcional final ni campaña global.

## Actualización de snapshots y rechazos

091–092: App y AppearanceProvider reales actualizan apariencia sólo después del recibo; una incertidumbre previa permanece bloqueada. 093–095: el hook de personalización y sus controles reales invalidan la vista limpia de forma perezosa, conservan borrador dirty y recuperan manualmente sin descartarlo; una incertidumbre previa no provoca GET ni queda liberada. 094_green conserva un intento fallido: abrir Personalizar ya crea un borrador igual a la vista, y el consumidor existente oculta controles cuando hay fallo. La corrección distingue cambios efectivos y el oráculo comprueba la conservación después de la recuperación explícita; 094_final_green contiene el cierre.

096–098: valores PROJECT/TASK importados se releen al volver al detalle; un borrador local se mantiene y bloquea guardar hasta recuperación. 096_red falló por esperar Guardar campos cuando el DTO vacío legítimamente no lo muestra. La fixture se corrigió a un campo activo con valor null, conservando ese resultado; 096_actual_red demuestra cache sin invalidar y 096_green su corrección. 099 evita refrescar apariencia mientras su PUT sigue pendiente. 100 acredita que importar únicamente definiciones también invalida los campos previamente vacíos. Son controles reales con transporte simulado, no E2E backend.

101–103: mensajes seguros para preview 400/409, además de 413; una primera confirmación rechazada por conflicto permite Elegir otra copia mediante gesto explícito, sin POST automático. 104: GET de configuración retenido antes del recibo se aborta; al liberar su respuesta vieja no vuelve a sustituir la vista aceptada. Cada ciclo tiene RED EXIT 1 y GREEN EXIT 0 salvo los intentos de fixture descritos. Últimos resultados por archivo: vista 31/31 (103_green) y refresh 8/8 (104_green). Continúan pendientes los demás bordes de refresh, errores y UX; ninguna campaña global ni mutación.

## Corte UI y refresh para revisión

105 aborta un GET de valores anterior al recibo y cambia la generación para que el consumidor aún montado consulte el estado nuevo; la respuesta vieja no repuebla cache. 106 conecta el mecanismo a App: Proyectos, Importación y vuelta a Proyectos con transporte real del cliente y respuestas simuladas. 107–108 y 110 explican 400 IMPORT_INVALID_REQUEST, 412 IMPORT_FILE_CHANGED y 409 IMPORT_KEY_REUSED de primera confirmación y permiten corregir manualmente. 109 refuerza el reenvío rechazado: conserva la incertidumbre anterior y no ofrece retirar su identidad; inicialmente GREEN, sin cambios productivos ni RED fabricado.

La regresión focal inicial de nueve suites dio 301/302: el único fallo fue el oráculo histórico de Apariencia penúltima, desplazado por Importación. Root autorizó conservar nombres y orden con Apariencia antepenúltima, Exportación penúltima e Importación última; 111 cerró 50/50 de Apariencia. 112 prueba que, al terminar un recibo retenido, se llama al callback de refresh vigente, no al cierre de un render anterior. 113 demuestra el foco perdido al cancelar una preview ya preparada; el layout de restauración ahora atiende también ese commit sin transición busy. Los dos ciclos tienen RED y GREEN reales.

Corte funcional para revisión: import_frontend_ui_final_focal.log, 304/304 en nueve suites, EXIT 0. Tipos y ESLint EXIT 0 en import_frontend_ui_final_checks.log. Formato aplicado en import_frontend_ui_final_format.log; las fuentes ampliadas ya se formatearon en los cortes anteriores. No es una regresión frontend global ni mutación. El snapshot de esta bitácora y las huellas quedan separados para que la continuación no reescriba evidencia.

Pendientes de cierre: revisión independiente de UI/refresh; SCSS local y navegación física/UX con los 30 principios y motores; integración backend/E2E cuando esté disponible; scope y mutación después de freeze aprobado. La evidencia DOM no acredita blur automático de disabled en navegador ni contraste/reflow. El mecanismo de refresh conserva borradores e incertidumbre y retira lecturas afectadas; la revisión debe contrastar también operaciones concurrentes y callbacks de sesión. No hay nuevas dependencias, proveedor global ni consultas globales de importación. No se ha actualizado export-data.tsx: su ayuda histórica que dice que importar no está disponible necesita alineación de copy antes de publicación, coordinada dentro del cierre23.

## Correcciones de revisión y cierre de navegador

El párrafo anterior describe el checkpoint histórico 304/304, no el estado final. Root revisó ese corte y señaló la herencia indebida de missingReceipt entre intenciones. 114 reproduce 404, reenvío confirmado y nueva intención rechazada: RED y GREEN 38/38. Se retira missingReceipt al empezar una intención nueva y al aceptar recibo; corregir un rechazo queda bloqueado mientras hay petición activa. Se conserva el 404 de la misma clave durante reenvío incierto. 115 demuestra que una recuperación confirmada retiraba el bloqueo pero dejaba un error anterior: RED y GREEN 38/38, con limpieza del error sólo al aceptar recibo.

116 exige coherencia HTTP/status/type/code antes de ofrecer descartar una intención definitivamente rechazada: HTTP409 con body.status200 sigue incierto. RED y GREEN 39/39. Los fixtures positivos ahora expresan el problema canónico completo. 117 añade type contradictorio al mismo oráculo; inicialmente GREEN, 2 ejemplos seleccionados y 38 omitidos. No se atribuye un resultado 40/40 a esa selección. Root revisó y aprobó funcionalmente estos deltas.

Navegador con API explícitamente simulada: import_browser_initial.log conserva RED real del selector File de 24px. El mínimo SCSS local amplía su área a44px y separa acciones, adapta tablas y usa tokens claros/oscuros existentes; import_browser_nominal_green.log acredita el primer GREEN. Cancelar/413, respuesta tardía, foco movido voluntariamente y confirmación separada se probaron físicamente. La matriz Chromium pasa276 medidas en23anchos,6estados y2temas; 12axe sin violaciones. La primera prueba de texto200 sólo cambió la raíz y no amplió tipografía fija enpx: import_browser_modalities_initial.log es fallo de instrumento, no defecto productivo. Se miden tamaños computados, se duplican exactamente y se conserva el resultado GREEN en modalities_second.

Firefox pasó4/4; WebKit pasó3/4 inicialmente y falló únicamente porque su interceptor devuelve null para el cuerpo File. Se conserva import_browser_other_engines.log. La comprobación de bytes transportados permanece íntegra en Chromium/Firefox; WebKit conserva método, cabeceras, ambos gestos y resultado, con límite de observabilidad explícito. Su nominal corregido pasa1/1 en import_browser_webkit_nominal.log. Zoom nativo Chromium2, DPRdoble y320CSS pasan en import_browser_native_zoom.log; compositor visible inspeccionado sin reescalar imágenes. La matriz30filas y límites están en review_import_data_browser.md.

Se alinea la ayuda histórica de Exportación con el recorrido disponible de Importación mediante cambio de texto y expectativa existente, sin navegación ni lógica nueva. Formato final aplicado. Scope aprobado por root: tres módulos completos más trece nodos AST de integración, sin exclusiones nuevas, ocho workers y umbral80. La regresión final y campaña se registrarán con sus resultados reales; E2E backend sigue siendo una evidencia separada pendiente.
