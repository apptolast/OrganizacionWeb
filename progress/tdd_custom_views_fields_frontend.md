# TDD frontend21: vistas y campos personales

Contrato aprobado c91bb8b,42 escenarios/174 ejemplos léxicos; no equivale al
número de tests. Inicio sobre init21 verde comunicado por root. No gates globales
ni cambios backend. Cliente en construcción: estos ciclos no acreditan decoder
completo, escrituras ni UI. Ponytail full/Caveman lite; un caso por ciclo.

| Ciclo | Contrato/oráculo | RED real | GREEN focal | Cambio mínimo |
| --- | --- | --- | --- | --- |
| 1 | s1 GET PROJECT defaults, ruta/auth/no-store y ETag | b267c5, módulo ausente | 0e3507,1/1 | Transporte y snapshot nominal |
| 2 | s27 campo extra en respuesta cerrada | 9524c9, promesa resuelta | 3dbc05,2/2 | Reutilizar exact existente |
| 3 | s20 GET503 conserva Response, no defaults | a1bfb6, Error distinto | d8a945,3/3 | Rechazar status no200 antes JSON |
| 4 | s27 defaultsPROJECT con ETagTASK | b5aac0, promesa resuelta | log04_green,4/4 | Vínculo de ETag ausente al scope |

Logs progress/customization_frontend_0N_red.log y _green.log conservados.
Etiquetas iniciales de los ciclos2/3 corregidas documentalmente a s27/s20;
los oráculos no cambiaron. Pendientes: validación íntegra, guardas abort/401,
comandos y comparación de intención, valores/ETag compuesto, estado/UI.

Ciclos siguientes (todos s27, inicialmente RED):
- 5 defaults PROJECT alterados: fcc788 → 9d9e96,5/5.
- 6 ausencia con fecha guardada:597f6c → 26c6ff,6/6.
- 7 configured no booleano:0dabd4 → 410e5e,7/7.
- 8 fecha configurada inválida:a5d722 → f9515c,8/8; reutiliza instant.
- 9 versión no canónica:9e4a71 → log09_green,9/9.
Se corrigieron literales con encoding dañado al detectar root el problema;
UTF-8 explícito en las escrituras posteriores. No nuevo comportamiento por
ese ajuste. Cliente sigue parcial; aún sin UI ni gate global.
- 10 revisión mayor que BIGINT:d353e3 → 88dfd1,10/10.
- 11 metadato del ámbito ajeno:3521e5 → 366b3e,11/11.
- 12 metadato duplicado:db4a0b → 26ef88,12/12.
- 13 ausencia con definición:9baf63 → log13_green,13/13.
Todos s27 y RED antes de mínimo verde. Formato de los dos archivos con
Prettier instalado después del ciclo12; sin cambios heredados.
- 14 tipo de definición no permitido:87c016 → f015e2,14/14.
- 15 etiqueta vacía:05763d → 011562,15/15.
- 16 límite60 puntos Unicode con control positivo60/negativo61:d247d2 → 645eab,16/16.
- 17 etiqueta no canónica con U+0085 exterior:24b868 → log17_green,17/17.
- 18 surrogate aislado:f89e1a → fc711c,18/18.
- 19 NUL:f0700e → 2b9799,19/19.
- 20 identidad inválida de definición:750299 → 935799,20/20.
- 21 flag active no booleano:90643c → log21_green,21/21.
- 22 IDs duplicados:5394c7 → 45f0f5,22/22.
- 23 etiquetas duplicadas incluyendo inactiva:b41a20 → c97943,23/23.
- 24 máximo12 con control positivo12/negativo13:0ac1fb → 711bb1,24/24.
- 25 s37 JSON tardío tras abort:b8dc55 → 47de97,25/25.
- 26 s37 HTTP401 tardío:216afd → log26_green,26/26; observer sin invocación.
- Refactor tipado y extracción decode reutilizable:77d4b0,26/26 sin cambio de oráculos.
- 27 s2 PUT visibleFields nominal, orden/If-Match/CSRF:15eaf9 → 6acd29,27/27.
- 28 s28 confirmación incompatible y captura antes de await:8627c0 → log28_green,28/28.
- 29 s28 vista pierde definición:3d7af4 → 225003,29/29.
- 30 s28 ausencia no confirma guardado:059eab → 08dcbb,30/30.
- 31 s3 alta nominal normalizada:27d710 → 567ad6,31/31.
- 32 s28 alta con etiqueta distinta:db389d → log32_green,32/32.
- 33 s28 alta reutiliza ID previo:628431 → 7a809c,33/33; nuevo ID por diferencia, no por posición.
- 34 s28 alta tipo distinto:e50440 → f7e6c6,34/34.
- 35 s28 alta pierde inactiva:6eae2a → log35_green,35/35.
- 36 alta cambia vista:fd70fd → a275f2,36/36.
- 37 alta inactiva:e5c47c → 1c7761,37/37.
- 38 renombrar/desactivar nominal sin enviar type:f337bc → 109b3d,38/38.
- 39 actualización cambia otra definición:bc6951 → log39_green,39/39.
- 40 actualización cambia vista:2ea7e3 → b12b18,40/40.
ESLint inicial8155ba señaló escapes de comillas redundantes en tests y regex
con NUL; reemplazo equivalente por includes(String.fromCharCode(0)) y limpieza
de escapes. Formato+ESLint focal y regresión40/40 verdes9f667b. Sin masks.
- 41 s10 parcial GET de valores sin esquema: eae02e →155132,1/1 módulo valores.
- 42 s27 ETag de otra entidad:a05ec7 →b50f6e,2/2 valores. Aún parcial.
- 43 s34 problema400 coherente de etiqueta:1ab87e →3e952e,41/41 configuración.
- 44 s34 problema contradictorio:d02d22,42/42 inicialmenteGREEN; no producción.
- 45 s34 problema mixto con campo desconocido:a883b7,43/43 inicialmenteGREEN.
Refactor mínimo sameFields elimina tres comparaciones duplicadas. Configuración
final43/43, formato/ESLint/TypeScript EXIT0 03980e/94bdd9. Logs
customization_frontend_config_final/lint/types.log. Freeze selectivo dos archivos
de configuración en customization_frontend_config_freeze.json. Valores2/2
permanece parcial; continúo su cliente disjunto, después estado/UI. No se afirma
cobertura global ni contrato21 completo; lecturas/transporte y errores API no
sustituyen oráculos públicos de montaje/recuperación.
- 46 s27 values cerrado:51be26 →b7088a,3/3 valores.
- 47 s27 ETag compuesto malformado:23e600 →450d4b,4/4.
- 48 s10 tarea/esquema configurado/valor null:2f668d,5/5 inicialmenteGREEN.
- 49 s27 configured no booleano:f1047f →log49_green,6/6.
- 50 values ausencia con fecha:73f09d →bb4ba5,7/7.
- 51 fecha configurada inválida:0cfb87 →4db294,8/8.
- 52 revisiónvalues incoherente con configured:8bc3e4 →4618bf,9/9.
- 53 revisiónschema superior aBIGINT:fcdb03 →log53_green,10/10. Comparación
  decimal textual canónica, sin redondeo ni BigInt de texto ilimitado.
- 54 tipo de valor desconocido:f499b2 →6d6030,11/11.
- 55 ausencia con valor no null:1d626e →7abbd7,12/12.
- 56 campos activos sin esquema:817d1d →log56_green,13/13.
- 57 NUMBERstring no coercionado:892773 →2e80ca,14/14.
- 58 NUMBERfracción:7744c3 →ecb19c,15/15.
- 59 NUMBERlímite y cero con control positivo:52cc51 →log59_green,16/16.
Root aprobó configuración43 como parcial y commit709021f; fuente liberada.
Siguiente delta autorizado: coherencia revisión/fecha en confirmaciones y
fieldId objetivo previo, con RED individual; no atribuirlo al corte anterior.
- 60 s28 cambio confirmado con revisión anterior:607c7f →9a2913,44/44 config.
  Guardia compartida en tres comandos; conserva UUID y exige incremento1.
- 61 primera alta con versión distinta0:53c52a →b26370,45/45.
- 62 no-op cambia revisión:708f04 →log62_green,46/46.
- 63 no-op cambia fecha:baed0d →031c19,47/47 configuración.
- 64 retroceso de1microsegundo:dc472a →0446eb,48/48; reutiliza microseconds.
- 65 fieldId no existe en snapshot previo:0b00fb →log65_green,49/49; no HTTP.
- 66 control positivo no-op BIGINTmáximo:85b791,50/50 inicialmenteGREEN.
- Extracción helpers label/id/type sin alterar contratos,50/50 refactor53d60f.
- 67 values etiqueta inválida:6f2255 →log67_green,17/17; reutiliza helper de configuración.
- 68 identidadvalues inválida:273972 →2bce8e,18/18.
- 69 IDsvalues duplicados:3551e2 →96d989,19/19.
- 70 etiquetasvalues duplicadas:21cd83 →log70_green,20/20.
- 71 BOOLEAN false real/string:8b2bde →a9ab7a,21/21.
- 72 DATE bisiesto válido/fecha imposible:63734a →bce5ce,22/22.
- 73 TEXT conserva espacios/saltos pero vacío debe ser null:cdef52 →log73_green,23/23.
- 74 TEXTlímite1000 puntos con positivo y negativo:f0bb34 →717af1,24/24.
- 75 surrogate aislado en TEXT:fd3f74 →557193,25/25.
- 76 NUL en TEXT:4162ab →log76_green,26/26.
- 77 valuesmáximo12:74fe0e →f2d13a,27/27.
- 78 HTTP503 conservaResponse:51929a →c868ab,28/28.
- 79 HTTP401 tardío abortado:87a2ee →log79_green,29/29.
- 80 s37 JSONvalues tardío:e1e8e2 →cf9853,30/30.
- Refactor decoder tipado y foco conjunto80/80:cdf35c.
- 81 s11 PUT conjunto nominal y TEXTvacío/null/NUMBER0:3f0070 →log81_green,31/31 valores.
- 82 values capturados antes de await:8f2033 →fca177,32/32.
- 83 ausencia no confirma PUTnull:6d85dc →69d869,33/33.
- 84 revisiónschema ajena a intención:1cb7a1 →log84_green,34/34.
- 85 metadata de valor cambia sin schema:7eba4c →9f54e0,35/35.
- Refactor guardia revisión compartida sin ampliar objeto:4dad67,50/50 configuración.
- 86 values cambiados con revisión anterior:dcdaaf →log86_green,36/36 valores.
  Reutiliza guardia de revisión/fecha y conserva schema mediante comparación previa.
- 87 control positivo no-opvalues, orden enviado distinto:315b8d,37/37 inicialmenteGREEN.
- Extracción decodificación problem400 compartida:c9f75c,50/50 configuración.
- 88 error400indexado conserva orden enviado:b55f2b →log88_green,38/38 valores.

## Corte cliente completo para revisión

Configuración50 + valores38 =88/88 verdes aece31. Formato, ESLint focal y
TypeScript terminan EXIT0 9e69bc. Logs customization_frontend_clients_final,
_clients_lint y_clients_types. Cuatro archivos en
customization_frontend_clients_freeze.json. Comparadores compartidos preservan
ETag compuesto/esquema, intención capturada y revisión/fecha; índice de errores
corresponde al array enviado. No GET global, estado o UI montados todavía.
Pendientes de evidencia21: estado por sesión/recurso, controles y composición,
privacidad entre rutas/cuentas, UX física y gates integrados, no acreditados por
estos88 tests. Reutilización exact/instant/microseconds/apiRequest mantiene
contratos heredados y no altera cuerpos de esos helpers.
- 89 primercontrolUI/contexto nuevo, no montajeApp aún:70cce7 →4ef01c,1/1.
- 90 dos consumidores de scope comparten GET: primerRED80b65c incluía mockResponse
  reutilizado; corregido fixture para Response nueva por llamada, REDcontrolado
  8faa92 exclusivamente cuenta2vs1. GREENlog90_green,2/2. No error productivo
  atribuido a consumir dos veces una Response simulada.

- 91 guardar selección explícita: RED log91_red (1 fallo/2 verdes), GREEN5bc256,3/3.
- 92 restaurar PROJECT sólo borrador:269fd3 →91c24c,4/4.
- 93 controles/defaults TASK específicos:0c4f31 →e3f67e,5/5.
- 94 cancelar y reabrir guardado:661a7a → log94_green. Sin escrituras por restaurar/cancelar.
- 95 orden DOM y envío mediante Subir: f73dc5 → b56dac,7/7.
- 96 consulta fallida, base provisional y recarga manual:7111f7 →8713c6,8/8. RED incluía rechazo no gestionado, eliminado en GREEN.
- 97 incertidumbre compartida tras salir/volver:95d326 →caec82,9/9. Sólo GET manual válido desbloquea.
- 98 guardado pendiente y doble pulsación:0696f0 →7ab9d9,10/10; controles congelados durante envío.
- 99 recuperación pendiente no restaura borrador antiguo:3cdc39 →d0b48c,11/11.
- 100 sesión desmontada antes de HTTP401 tardío:de1237 →ea1638,12/12; aborto antes del observador de acceso.

- 101 rechazo 400 corregible conserva borrador: ce8fc3 → 52a264, 13/13.
- 102 creación tipada: 3461fd; primer intento de GREEN tuvo duplicación de identificador durante extracción del comando compartido (7d5a73), sin tests ejecutados. Corregido, 91a079 GREEN 14/14.
- 103 etiqueta vacía con error accesible local: 41f027 → 54da48, 15/15.
- 104 renombrar/desactivar sin cambiar tipo: fb296d → a2f191, 16/16.
- 105 lectura de valores BOOLEAN false: a7470d (módulo ausente) → 4a05bb, 1/1.
- 106 guardado booleano explícito: a5ab5b → ebaa93, 2/2.
- 107 NUMBER cero y entero negativo: 9b0bc1 → 9cfedf, 3/3.
- 108 intención fraccionaria larga antes de conversión: cc0d89 → e2db79, 4/4. RED incluía confirmación simulada incompatible tras envío indebido; GREEN no envía petición.
- 109 rango NUMBER antes de petición: f6f5f3 → log109_green, 5/5. Gramática decimal entera, finito y rango comprobados antes de construir JSON; no parser JSON global ni dependencia nueva.

- 110 texto multilínea conserva espacios: 8852ef → a85683, 6/6 valores.
- 111 fecha civil con control nativo y sin conversión de zona: 3d1442 → f0f0b6, 7/7 valores.
- 112 escritura de configuración abortada antes de 401 tardío: 60cd44 → ce3f65, 17/17 configuración UI. GET y PUT tienen evidencia distinta; POST comparte el ejecutor pero aún no se atribuye una simulación separada.
- 113 borrador de vista por ámbito sobrevive a navegación interna: 356ece → 51d088, 18/18 configuración UI.

Pendientes antes del freeze UI: valores (borradores, incertidumbre, privacidad, validación y errores indexados), gestión completa de definiciones, invalidación entre lectores/escrituras, montaje en proyectos/tareas y foco/SCSS. Este WIP no es una aprobación ni evidencia de recorrido completo.

- 114 guardado de valores incierto sobrevive a navegación: 16e433 → d68c2d, 8/8 valores.
- 115 corrección de oráculo según @s33: fallo inicial usa Reintentar, no Recargar guardado. RED 295c46 → e85433, 18/18 configuración UI. La prueba de borrador entre rutas no se atribuye ya a @s33.
- 116 fallo inicial de valores no publica vacío confirmado: aac156 → a1dea6, 9/9 valores.
- 117 error values[1].value enlazado al orden enviado, conserva todos los borradores: 365b84 → b04d87, 10/10 valores. Etiqueta y descripción accesible separadas.
- 118 borrador de valores por recurso durante navegación: f8c65f → 8b67ef, 11/11 valores.
- 119 Vaciar sólo un campo y conservar false en el conjunto enviado: 661649 → 0626b3, 12/12 valores.
- 120 Cancelar restaura snapshot sin escritura: 0a3331 → 962041, 13/13 valores.
- 121 cambio de proyecto aborta GET anterior antes del observador 401: 2ba17b → f26aeb, 14/14 valores.
- 122 volver tras retirar una consulta inicial pendiente permite consultar de nuevo: 95752d → 7f89ec, 15/15 valores.

No bloqueo de infraestructura. Montaje y resto de guardas siguen pendientes; no se ha ejecutado ninguna campaña o regresión global.

- 123 PUT de valores abortado al retirar sesión: f0d303 → c39af2, 16/16 valores.
- 124 espera y exclusión de doble envío/edición: 6faea9 → e6423a, 17/17 valores.
- 125 primer montaje App/Proyectos con metadatos vacíos y sin N+1: 464faf → d48f0c, 1/1 integración.
- 126 orden de fechas PROJECT en UTC: 8cf6df → 72e478, 2/2 integración.
- 127 TASK compartido en proyecto conserva borrador de creación: d43c89 → feddc9, 3/3 integración.
- 128 fechas TASK con orden y UTC: f715f0 → 7789f8, 4/4 integración.
- 129 campos personales en detalle de proyecto: 1f8f7d → 567923, 5/5 integración. Fixture anterior añade exclusivamente GET propio de valores vacío confirmado para el nuevo montaje, conservando unexpected=[].
- 130 detalle tarea y subtareas: c8daf3 → e3c2b2, 6/6 integración. Sólo GET de valores del detalle; criterio de negocio permanece aunque se oculta en la lista.
- 131 lectura anterior de configuración no restaura confirmación: 1f91ca → 70727c, 19/19 configuración UI.
- 132 lectura anterior de valores no restaura confirmación: 3977c0 → 30926c, 18/18 valores.
- 133 incertidumbre después de retirar otra lectura no provoca GET al volver: 1f4e53 → 919fea, 19/19 valores.
- 134 recuperación manual pendiente anuncia espera y no duplica GET: 195f7a → b1e260, 20/20 valores.
- 135 404 vigente de escritura retira campos y avisa al contenedor, manteniendo lectura antigua descartada: b3e385 → e6a7a2, 21/21 valores.

- 136 retirada del contenedor de proyecto ante 404: RED controlado a8017c → GREEN 335d2d, 7/7 integración. El primer RED dd31e6 también detectó una precondición incompleta del fixture: ahora espera la carga de Personalizar vista antes de contar consultas, conservando los oráculos.
- 137 retirada del contenedor de tarea ante 404: b42b54 → 47d2eb, 8/8 integración.
- 138 recuperación manual con 404 retira el recurso y comunica al contenedor: 9ce4a4 → 4ff73c, 22/22 valores; elimina el rechazo sin consumidor demostrado en RED.
- 139 cambio local de esquema invalida valores limpios al volver: fee2bf → 8706ec, 20/20 configuración UI.
- 140 variante con borrador conserva valores y exige recuperación manual: 61f133 → 4dc6c9, 43/43 configuración y valores.
- 141 consumidor limpio todavía montado consulta la nueva generación: 6695a1 → e7f157 (EXIT e73a7c), 22/22 configuración UI.
- 142 error 400 general de values anuncia explicación y conserva borrador: 5a6527 → b5d332, 23/23 valores.
- 143 edición iniciada durante PUT del esquema también se conserva: 8410ab → f59b04, 23/23 configuración UI. La confirmación consulta el borrador vigente, no el capturado antes del await.

Corte parcial para revisión: clientes, retirada de recursos, integración y coherencia del esquema. Permanecen pendientes validaciones locales y acabado de formularios, mensajes/foco y SCSS. No representa cierre de la interfaz ni validación física; no se han ejecutado gates globales.

Verificación del corte parcial: 142/142 en cinco suites, tipos y formato verdes. Lint detectó escritura del ref de callback durante render y una variable de fixture sin uso; refactor mínimo a efecto y retirada de la variable, sin desactivar reglas. Lint final 085b35 EXIT0; suites y tipos repetidos tras el delta (logs customization_ui_partial_*_final.log). Manifiesto: customization_frontend_ui_partial_freeze.json, 14 archivos. No validación global ni física atribuida.

- 144 TEXT de más de 1000 puntos de código: RED 982b10 (envío indebido terminaba en incertidumbre) y GREEN 742961, 24/24 valores. Mensaje local asociado al campo, borrador intacto, ninguna escritura.
- 145 recuperación manual de configuración con esquema diferente: RED b601e9, GREEN fbd4c4, 24/24 configuración UI. Una variante del fixture de esquema simula PUT412 y GET manual válido; ahora GET y escritura comparten acceptConfig, comparan ETag y conservan las protecciones dirty/uncertain. Formato/lint 63253c EXIT0. El manifiesto parcial anterior se conserva como snapshot histórico para el E2E aislado; no describe estos deltas posteriores.

- 146 TEXT con NUL: RED 99051c y GREEN c63e81, 25/25 valores, reutilizando el oráculo de validación local.
- 147 primera configuración después de URL directa con valores cacheados de otro esquema: RED af23ee y GREEN cbcc78, 25/25 configuración. Compara la revisión de esquema de valores aceptados; no invalida lecturas nominales sin snapshot ni revisiones iguales. Lint/formato b935f3.
- 148 TEXT con surrogate aislado: RED 0bea94 y GREEN 5faeb8, 26/26 valores. Regex Unicode no rechaza pares válidos.
- 149 límite superior civil del control DATE: RED cb2ceb y GREEN fb77f3, 26/26 valores. min/max nativos impiden enviar año 10000; se conserva el oráculo previo de fecha bisiesta y payload exacto.
- 150 recuperación de configuración conserva borrador hasta GET válido: RED 90c1fc y GREEN 707ffa, 25/25 configuración. Consumidor del hook observa el borrador durante espera; el borrado se realiza al aceptar recuperación válida.
- 151 etiqueta duplicada incluyendo campo inactivo: RED funcional 6cff6f y GREEN 81006a, 26/26 configuración. Dos intentos previos conservaron error sintáctico del test, no se cuentan como evidencia funcional.
- 152 límite de doce campos incluyendo inactivos mantiene edición: RED funcional 722547 y GREEN 5d7253 (log completo), 27/27 configuración. Primer intento sintáctico preservado sin atribución funcional.

- 153 cancelar edición de definición restaura formulario nuevo sin petición: RED 157577 y GREEN 3f5e8e, 27/27 configuración; se conserva el envío previo de renombre/desactivación.
- 154 ACK de valores anterior a esquema ya observado: RED a685a6 y GREEN 623451, 28/28 configuración. Aceptar esquema distinto retira escritura pendiente y conserva incertidumbre/borrador para recuperación manual.
- 155 GET inicial retenido con schema anterior al config observado: RED 37f366 y GREEN 542d50, 29/29 configuración. Guardia común de llegada de valores, usada en lectura y escritura; compara revisiones del mismo agregado, acepta revisiones más nuevas y no inicia consultas automáticas. Formato/lint 143b2d EXIT0.

- 156 foco tras desaparecer Guardar vista: RED 3ad67d y GREEN 48d233, 29/29 configuración. Región lógica enfocable; sólo captura iniciador realmente enfocado y no sustituye foco ajeno.
- 157 editar/cancelar valores limpia confirmación y errores anteriores: RED 515784 y GREEN 6a38fe, 26/26 valores.
- 158 abrir nuevo borrador de vista limpia confirmación anterior: RED 1af74c y GREEN 0c6c9f, 29/29 configuración.
- 159 recuperación de valores devuelve foco al encabezado si desaparece el iniciador: RED ada5ba y GREEN 37be01, 26/26 valores.
- 160 editar nueva definición limpia confirmación anterior: RED 5008c6 y GREEN 9bb13b, 29/29 configuración.
- 161 anuncio común de escritura dice Guardando personalización (también se usa para campos): RED f6f93e y GREEN 0735e6, 29/29 configuración; conserva oráculos de espera/exclusión.
- 162 refuerzo de foco voluntario: el primer intento falló porque blur() de un botón deshabilitado no mueve el foco en este entorno. Fixture corregido: el usuario mueve foco a otro control habilitado y después a body; inicialmente GREEN cbffda, sin cambio productivo. Conserva todas las aserciones previas de guardado y espera.
- Ayuda de desactivación añadida por instrucción explícita root como copy reversible, sin nuevo ciclo costoso. Un intento anterior de prueba de esa ayuda coincidió con el fixture162 todavía fallido y no se cuenta como evidencia funcional; fue retirado antes de revalidar162. Texto explica ocultar, conservar valores y reactivar.

Cierre funcional JS para revisión: formato/lint 25640d EXIT0; pruebas focales y tipos en customization_frontend_final_tests.log / customization_frontend_final_types.log. CSS21 y E2E/UX físicos son propiedad de C y permanecen como gate independiente; no se atribuye cierre de feature ni gates globales a este corte.

Regresión global autorizada después del freeze: EXIT1 41fc1a, 2084/2209 pasan y 125 fallan en nueve suites heredadas (log customization_frontend_global.log). Las cinco suites21 conservan151/151. Diagnóstico inicial comunicado a root antes de editar: fixtures secuenciales reciben GET21 perezosos que antes no existían; no se atribuye automáticamente cada fallo a fixture sin revisar. Build frontend 9e1628 EXIT0; lint global en customization_frontend_global_lint.log. No campaña de mutación iniciada.

Scope Stryker21: cinco módulos completos y once nodos AST completos de cuatro archivos de integración, inventario customization_frontend_mutation_nodes.json. Líneas 1-based, columnas 0-based, final exclusivo; configuración mantiene8/perTest/todas suites/80/ignores y salidas independientes. Default conserva lo previo y añade comportamiento nuevo; project-tasks/task-reader ya estaban completos en default. Pendiente revisión root antes de campaña.

Corrección física de foco164: C observó en Chromium blur automático del botón deshabilitado aún conectado, sin focusin voluntario (diagnóstico14c758). Oráculo DOM reforzado con focusOut del iniciador disabled: RED66c5eb. Dos intentos intermedios no aplicaron el reemplazo de código y continuaron RED; no se cuentan como GREEN. Corrección final común a ambos consumidores usa matches(':disabled'), incluyendo fieldset, sin eliminar listener de foco voluntario. GREEN081fab,55/55; C repite navegador sobre hashes actuales. Manifiesto actualizado separado: customization_frontend_focus_final_freeze.json.

Fixtures heredados: helper local test-fixtures/customization.ts intercepta sólo GET21 canónicos y delega todo otro tráfico al mock de negocio. Nueve suites459/459 GREENe158ef, sin suprimir oráculos. Un único ajuste de precondición @s15 espera Personalizar vista antes del assert global de ausencia de status. Regresión reparada2209/2209,49suites, EXIT0acf19a;122inputs antes/después idénticos d00cdf. Build437fbfEXIT0. Lint detectó dos imports vi sin uso tras sustituir los spies; retirados sin cambios de comportamiento.

Precisión de revisión A: helper exige coincidencia completa también frente a LF final (el ancla $ permite LF en JavaScript). Cambio posterior al pase2209 sólo en helper; comprobación ejecutada sobre función transpilada acredita GET válidos, LF y POST delegados, log customization_legacy_exact_routes.log. No se reatribuye el hash del helper posterior al pase anterior. Manifiesto final de diez archivos customization_legacy_freeze_final.json; no repetición global automática por esta precisión y retirada de imports.

Lint final e976a9 EXIT0: la revisión posterior sólo encontró formato de stryker.config.json tras sustituir rangosApp por archivo completo, conforme instrucción root (mismo universo ejecutable). Prettier aplicado sin tocar opciones; resultado final en customization_frontend_lint_closed.log. Config focal21 conserva cinco módulos y once nodos completos; defaultApp completo evita deriva de coordenadas. No campaña iniciada por B.
