# Revisión de huecos de mutación frontend13

Estado: revisión parcial; no aprobación final ni nuevas pruebas ejecutadas. Fuentes congeladas mientras se compara concurrencia. Base: informe original de ejecución18743, inventario de190 entradas en `reschedule_frontend_mutation_gaps.json`.178 Survived,10 NoCoverage y2 RuntimeError no equivalen automáticamente a defectos de producto ni a equivalencias demostradas.

## Casos de comportamiento prioritarios para el autor de tests

Ejecutar un caso por ciclo y registrar si ya es GREEN. Los IDs identifican la campaña original; repetir por firma/ubicación si cambian tras editar tests. No alterar producción para fabricar un RED. No afirmar que un caso mata los IDs previstos hasta observar el replay.

| Frontera | Oráculo observable | IDs candidatos |
| --- | --- | --- |
| ETag | Rechazar etiqueta válida seguida de caracteres adicionales y array JSON que contenga una etiqueta válida (RegExp.exec lo puede convertir a string si falta la guarda de tipo) |253,258|
| Recibo de movimiento | Rechazar snapshot after con identidad u objetivo ajenos aunque su estructura sea válida; repetir por recuperación GET |394,396,397,417,542,543|
| Historia | Rechazar21 elementos; aceptar20 terminales; rechazar modificación de createdAt; rechazar respuesta no200 aunque el cuerpo parezca válido |442,443,531,539|
| Historia temporal | Ordenar instantes dentro del mismo segundo con fracciones de longitud distinta y fracción ausente |582–591|
| Problemas HTTP | Errores propios13 con título vacío, code no textual o status incoherente deben producir null sin lanzar una excepción al interpretar el problema |573,577,580|
| Recuperación incierta | Tras GET503 o fallo de red, mantener incertidumbre y no habilitar reenvío hasta GET404 explícito |183,184,186|
| Reintentos manuales | Dos fallos consecutivos y un tercer intento real en confirmación, estado e historia |60,633,649,1228|
| Acciones de lista | Proyecto completado oculta Mover; editor activo oculta acciones incompatibles; confirmar cierra editor y vuelve a primera página |1378,1380,1388,1389,1422,1424,1432,1433,1452,1453|
| Confirmación y creación | Tras confirmar cambio y crear otro bloque, retirar confirmación anterior |1485|
| Presupuesto de movimiento | Días con exceso y sin exceso mezclados: exigir consentimiento; ningún exceso: no mostrarlo; comprobar POST con consentimiento explícito |703,1113,1115,1117,1145|
| Errores DST | Editar inicio elimina sólo errores de inicio/local offset y conserva los de fin; espejo para fin; cambiar zona conserva errores locales no relacionados |897,901,902,926,928,933–935,958,960,965–967|
| Selección DST | Elegir ocurrencia elimina sólo el error de ese selector y mantiene asociación aria de los demás |1054,1057,1061,1063,1075,1077,1096|
| Rechazo de commit DST | VALIDATION_ERROR con validOffsets tras POST permite elegir las ocurrencias recibidas conservando las otras opciones |1138,1140,1141|
| Estado actualizado | Fallo de consulta termina indicador de carga; no borra acceso por503; éxito cancelled muestra estado y bloquea revisión |764,768,784,793,878–880|
| Revisión y privacidad |503 de preview no invoca onAccessFailure404 ni crea conflicto de versión; una nueva revisión elimina rechazo previo |820,832,838|
| Foco y navegación | Encabezados con tabIndex=-1, foco externo conservado y foco de recuperación al desaparecer control activo |47,48,80,121,628,652,657,658,668,669,1221|

Lectura exacta de fuente y ubicaciones del informe: c9f35d/b2728e. Aclaración:1140/1141 afectan `setOffsetChoices` del rechazo de commit; no son el onChange del consentimiento.

## Candidatos a equivalencia: falta prueba individual

- Contadores opacos +1→-1 (61,634,650,1229,1327,1431) parecen conservar el único uso observable, disparar recarga. Revisar todos los consumidores y límites antes de aceptar.
- Dependencias constantes de efecto []→[cadena constante] (98,678,684,737,1255) parecen conservar montaje/desmontaje; documentar por ID.
- Guardas de aborto anteriores a un decoder con segunda guarda posterior pueden ser redundantes; no confundirlas con las posteriores, que protegen privacidad y respuestas tardías. Ejemplos de guardas distintas en panel:609/611,781/783,829/831. No aceptar el grupo completo.
- Referencias DOM sin optional chaining pueden ser equivalentes si el punto de ejecución garantiza montaje; demostrar por llamada y no mediante suposición general.
- Textos internos de Error no presentados por la UI pueden ser equivalentes bajo el contrato; probar que no son parte de diagnóstico contractual antes de clasificar.
- Normalización UUID y controles de duplicados/orden se solapan parcialmente. El parser admite mayúsculas: comprobar datos mixtos antes de aceptar mutantes de comparación.

## Límites pendientes

Esta tabla prioriza pruebas; no clasifica exhaustivamente las190 entradas ni sustituye el judge. RuntimeError171/180 son fallos del adaptador al representar una excepción, conservados como errores sin adjudicarles Killed o equivalencia. Las pruebas E2E previstas aún no vuelven GREEN el escenario13 y tampoco sustituyen los oráculos de esta campaña Vitest.

## Pruebas de equivalencia acotadas por lectura

No se cambian los estados ni el score del informe original. Estas conclusiones se limitan a las fuentes congeladas y quedan disponibles para revisión final.

- API481, comparación de ID `<=`→`<`: sólo difieren para IDs iguales tras toLowerCase. Antes de alcanzar esa comparación se rechaza la página entera si Set(ids.toLowerCase()).size difiere de su longitud. La igualdad entre dos IDs de la página nunca alcanza esta rama; para desigualdad ambos operadores coinciden. La guarda de duplicados permanece en este mutante individual.
- API462, `toLowerCase`→`toUpperCase` al construir el Set de IDs: cada ID pasa primero el validador UUID ASCII hexadecimal insensible a mayúsculas. Ambas normalizaciones inducen exactamente las mismas clases de igualdad sobre ese alfabeto. Esto no justifica mutantes483/484 de comparación asimétrica entre dos IDs.
- API555, callback de `.json().catch` devuelve undefined en lugar de null: ambos fallan inmediatamente `exact(value,...)` y `readChangeError` devuelve null sin observar otras propiedades. El fallo de JSON permanece capturado. No afecta al objeto Response original, que se clona.
- Efectos98,678,684,737,1255, array vacío→array de una cadena literal constante: la lista mantiene longitud y mismo valor primitivo durante todas las renderizaciones de cada montaje. React compara cada dependencia con Object.is, por lo que sigue ejecutándose sólo al montar y limpiándose al desmontar, incluidos ciclos adicionales de StrictMode. No incluye efectos cuyas dependencias se eliminan o cambian de identidad.

Lectura de API e invariantes compartidas7607ab/9931de/1b6d96; comprobación de efectos contra fuentes congeladas29c0c0/c9f35d. Falta replay de cualquier nueva prueba de comportamiento; no se usa esta lectura para reclasificar RuntimeError.
