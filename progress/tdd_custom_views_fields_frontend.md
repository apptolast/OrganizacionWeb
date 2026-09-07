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
