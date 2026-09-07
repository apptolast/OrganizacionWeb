# TDD frontend 20 — Apariencia

Contrato f4697a6, autorización de TDD 0202129 y gate previo init25384 EXIT0/14625f. No se repite init para empezar. Ownership sólo frontend/documentación propia; Java/HTTP pertenecen a otros autores. Ponytail full/Caveman lite: se reutilizan apiRequest, exact e instant, sin modificar sus cuerpos ni añadir dependencias.

## Cliente: un caso por ciclo

Logs `progress/appearance_client_<n>_red.log`, `_green.log` o `_initial_green.log`. Los casos que pasan inicialmente acreditan comportamiento heredado de ciclos anteriores; no se presenta RED ficticio. Cada fila se escribió y ejecutó antes de la siguiente.

| Ciclo | Frontera observable | Evidencia |
| --- | --- | --- |
| 1 | GET privado con defaults, ETag, no-store y signal | RED c0d7cc import inexistente; GREEN 7a88b4 |
| 2 | HTTP503 se conserva, no defaults | RED fd081f; GREEN 72474c |
| 3 | JSON con extra se rechaza | RED 79d136; GREEN final40956d |
| 4 | Ausencia debe contener defaults coherentes | RED d1bf45; GREEN04d264 |
| 5 | Configurada con versión BIGINT exacta textual | Inicialmente GREEN0cfcd3 |
| 6 | configured sólo booleano | REDdba2a7; GREEN643c60 |
| 7 | Enum tema cerrado | REDe1b31b; GREENc75b3a |
| 8 | Timestamp máximo microsegundos | RED1482a3; GREENd1a1b1 |
| 9 | ETag configurado con BIGINT acotado | REDa9bcdc; GREEN4589ee |
| 10 | Contraste claro aunque tema oscuro | RED76e730; GREENbc78e4 |
| 11 | Contraste oscuro aunque tema claro | RED9b63af; GREEN501ea1 |
| 12 | Color legible en blanco pero no en hover | Inicialmente GREENe501b3 |
| 13 | Contraste menor a4,5 no se redondea para aceptar | Inicialmente GREEN418568; fixture afinado GREEN371d93 |
| 14 | Acentos libres válidos azul/cian | Inicialmente GREENfca11c |
| 15 | HTTP401 tardío antes del observador | RED284a18; GREENc84a84 |
| 16 | JSON termina después de aborto | RED033f9a; GREEN4c2108 |
| 17 | PUT cerrado con If-Match y CSRF | RED3f1cb1; GREEN3eb379 |
| 18 | Confirmación válida pero tema distinto de intención | RED6871d2; GREEN782502 |
| 19–24 | Tag débil, timestamp ausencia/configurada, color no canónico/tipo incorrecto y campo ausente, uno a uno | Inicialmente GREEN670d3b/0da7a9/7f1b30/93aa20/462a30/71172f |
| 25–26 | Confirmación con otro acento claro/oscuro, separadamente | Inicialmente GREENcc2269/1b9438 |
| 27 | Intención capturada no cambia con mutación del argumento | Inicialmente GREENa6c634 |
| 28 | PUT no puede confirmar recurso no configurado | Inicialmente GREEN5fbd38 |
| 29 | La fila GET401 anterior se conserva y se añade PUT401 | Inicialmente GREEN8a8da8, dos filas parametrizadas |
| 30 | PUT412 conserva respuesta para recuperación manual | Inicialmente GREEN1cb62c |

Incidentes: en ciclo3 se pasó inicialmente un array a exact, cuya firma existente requiere cadena de claves. Fallo real keys.split preservado en `_3_green.log` (el nombre no acredita éxito); se corrigió sólo la llamada y el GREEN real es `_3_final_green.log`. Vitest devuelve su diagnóstico real antes de un mensaje PNPM accesorio “vitest not found” en algunas salidas fallidas; no se instaló ni cambió ninguna herramienta.

El primer fixture13 #777777 fallaba por contrastes adicionales y no aislaba redondeo. Se sustituyó antes del freeze por #645F61: su peor contraste sobre #D0DFC9 es aproximadamente4,499799974, que redondeado parecería4,50; la aceptación compara valor completo. Ambas ejecuciones inicialmente verdes se preservan. No se calcula la expectativa del test usando el helper de producción.

## Alcance y pendientes

Dos archivos nuevos: appearance-api.ts y appearance-api.test.ts. Los 30 casos no equivalen a los112 ejemplos Gherkin. exact/instant mantienen oráculos heredados de estructura/calendario y sólo se prueba conexión y fronteras relevantes de20, sin duplicar toda su matriz.

PUT captura tres primitivas antes del await; el decoder es compartido con GET y comprueba configured/defaults, campos, tema, acentos/contraste, timestamp y ETag. La clasificación detallada de problem+json y estado de recuperación se completará con los ciclos UI; actualmente los errores HTTP se conservan como Response sin inventar códigos. Estado compartido, navegación, formulario, roles de color y tokens SCSS, recuperación/foco/privacidad, pruebas reales y campañas todavía pendientes. No se anuncia cierre de feature20 ni se cambian fixtures globales.

## Estado compartido y formulario: ciclos 31–45 (en curso)

Cada fila se ejecutó antes de pasar a la siguiente. Logs `appearance_ui_N_red.log` y `appearance_ui_N_green.log` en esta carpeta; los identificadores son salidas del runner, no commits.

| Ciclo | Oráculo público | RED | GREEN |
|---|---|---|---|
| 31 | Consulta y controles nativos nominales | 086827 | 2c02a1 |
| 32 | Espera sin formulario antes de respuesta | bcfa06 | 84e27f |
| 33 | Error inicial y reintento explícito | e08d19 | 12a3f6 |
| 34 | Tema confirmado aplicado al documento | 16d596 | 60285b |
| 35 | Retirada de valores privados al desmontar | d17654 | ca59a1 |
| 36 | Sistema sigue cambio de preferencia sin PUT | b789d2 | 7dc354 |
| 37 | Relectura del sistema al recuperar visibilidad | 415712 | 89f4fb |
| 38 | Borrador y vista previa locales | 76e0d9 | 15d996 |
| 39 | Color inválido, descripción accesible y última muestra válida | 6a61aa | 5fdd4a |
| 40 | PUT único y aplicación sólo tras confirmación | 18aff9 | d55a77 |
| 41 | Incertidumbre y recuperación manual mediante GET | d18287 | f488c8 |
| 42 | Recuperación fallida seguida de consulta pendiente sin duplicado | a24e9b | 451118 |
| 43 | Carga real bajo StrictMode del punto de entrada | ee0ed7 | 9ccc1c |
| 44 | Refuerzo del caso 42: conserva versión confirmada, no anuncia defaults ni ofrece segundo reintento ajeno al borrador | f1c2cd | d939bc |
| 45 | Restaurar prepara defaults sin PUT ni cambio global | 291af3 | f7836b |

El ciclo 42 terminó con 12 pruebas verdes; 451118 es la lectura posterior del log conservado, pues la salida inicial excedió el contexto de la herramienta. El ciclo 43 acreditó un problema real de limpieza: el ref de una consulta abortada impedía la segunda ejecución del efecto en StrictMode. Se libera al desmontar; el finally antiguo conserva su guardia de identidad. El ciclo 39 exporta únicamente `isAppearanceAccent` para reutilizar el contraste del cliente en la vista previa; no relaja el decoder canónico.

Corte actual: 14 pruebas UI verdes, 30 del cliente previamente revisadas. Sin freeze final: quedan controles de cancelación/selector, clasificación de errores, coordinación de lecturas/escrituras, montaje autenticado y tokens/UX. No se ha ejecutado gate global ni campaña.

### Ciclos 46–50: controles y montaje nominal

| Ciclo | Oráculo | RED | GREEN |
|---|---|---|---|
| 46 | Cancelar restaura campos/muestra confirmados sin HTTP y conserva foco | cff2d3 | 37fcaa |
| 47 | Selectores color nativos sincronizados con hexadecimal | 5e3a0d | b45d3b |
| 48 | Enlace Principal, ruta exacta, aria-current y foco h1 | 610cc1 | f50cc5 |
| 49 | Provider dentro de sesión autenticada, una consulta propia | c90db1 | 97414b |
| 50 | Retorno a /apariencia tras login, ninguna consulta privada anónima | 9cfbc7 | 3f1ae5 |

El cliente se reutiliza sin cambios adicionales desde el helper del ciclo 39. Formato local aplicado; types detectó una opción `exact` no admitida por ByRoleOptions (efc57b), retirada sin cambiar semántica de la consulta por nombre literal; types posteriores verdes 44b2f8. El Provider se monta en SessionGate autenticado, por lo que los tests que montan App directamente no reciben consultas nuevas artificialmente. Se inicia regresión focal de autenticación para detectar mocks afectados por la nueva ruta; no se ocultan peticiones desconocidas globalmente.

### Ciclos 51–52 y regresión de autenticación

- 51 (@s32): cambio de usuario autenticado no hereda tema ni borrador. RED 1f521f, GREEN 69b413 (20 UI). SessionGate identifica el Provider por el usuario de la sesión ya validada; no se crean DTO ni almacenamiento adicionales.
- Regresión autenticación: RED real ec5f6f, 27 fallos/47 pases por la nueva consulta que consumía respuestas secuenciales de otras operaciones. Root autorizó fixture local por ruta y método exactos. `mockSessionTraffic` responde sólo GET `/api/v1/me/appearance` con defaults válidos, delegando cualquier otra ruta/método al mock anterior. Se conservaron todos los oráculos e índices de tráfico sesión/negocio. GREEN a16d56, 74/74. No se reemplazan Provider ni apiRequest.
- 52 (@s27): 400 coherente de campo, aria-invalid/descripcion y corrección sin recuperación incierta. RED 838aea, GREEN 004951 (51 cliente+UI). Se añadió error tipado tras validar forma/status/type/code y entradas conocidas; la lectura JSON comprueba aborto antes de clasificar. Los demás errores siguen preservados y no se convierten en confirmación. Formato y types verdes 1372dd.

Continúan pendientes los demás campos de error, recuperación CSRF y composición de respuestas tardías entre sesiones; la aprobación previa del cliente no se extrapola a estas ramas nuevas.

### Ciclos 53–59: privacidad, errores y foco

- 53 (@s31): GET de Ana pendiente, logout/login de Bruno, HTTP 401 antiguo después. Inicialmente GREEN 45f0af; se conserva acceso/tema de Bruno y no aumenta la lectura de sesión.
- 54 (@s27): segunda fila del mismo oráculo de campo, ahora accentDark. RED 7b4424 → GREEN 808acc, sin duplicar el fixture de accentLight.
- 55 (@s33): refuerzo del caso de recuperación existente, foco h1 al desaparecer su iniciador. RED d65789. Primera regresión cff51e detectó una aserción del ciclo 43 antes del efecto visual StrictMode; se espera ese efecto concreto con waitFor predeterminado, sin alterar producción por timing. GREEN final c7d2a9.
- 56 (@s33): foco movido deliberadamente a otro campo y después body mientras espera. RED b4bbcc → GREEN d3e08b. Listener focusin retira la intención de recuperar foco.
- 57: error del servidor asociado al grupo nativo Tema, corregible al cambiar selección. RED 36c5ed → GREEN 412aa5.
- 58 (@s31): fila GET HTTP 200 antiguo tras sesión posterior, inicialmente GREEN 444143. Mantiene el mismo flujo real de SessionGate.
- 59 (@s30): consumidor del estado compartido consulta mientras conserva snapshot, confirma PUT y recibe después el GET anterior. RED 4e4d9e → GREEN d76966 (27 UI). La lectura se aborta al comenzar PUT; no se restaura estado viejo. El consumidor es un arnés de prueba del Provider, no un control añadido al producto.

Pendientes de cierre: JSON diferido/PUT entre sesiones, navegación Back y algunos estados; vista previa completa y tokens físicos. No se declara freeze ni cumplimiento UX global.

### Ciclos 60–72 y revisión del estado compartido

| Ciclo | Oráculo | Resultado |
|---|---|---|
| 60 | Historial/Back descarta borrador con advertencia, conserva tema y no repite GET | RED 4f8bd6 → GREEN b0225a |
| 61 | Fallo inicial visible también en otra función utilizable | RED 0cf412 → GREEN afbf15 |
| 62 | PUT HTTP401 antiguo tras Ana/logout/Bruno | inicialmente GREEN 3005ee; primera selección 36943a no ejecutó tests y no cuenta como evidencia |
| 63 | PUT HTTP200 antiguo en el mismo recorrido | inicialmente GREEN 229cde |
| 64 | GET con JSON200 diferido entre propietarios | inicialmente GREEN d19c1a |
| 65 | PUT con JSON200 diferido entre propietarios | inicialmente GREEN fd48af, seis filas acumuladas |
| 66 | GET iniciado durante PUT tampoco restaura revisión antigua después de confirmar | RED 92ee4c → GREEN 17079d (log f17076, 34 UI) |
| 67 | Reintento global devuelve foco al encabezado al desaparecer su control | RED ee1405 → GREEN b4b256 |
| 68 | Refuerzo de guardado: editar después retira el éxito obsoleto | RED d1b617 → GREEN 3e7500 |
| 69 | Mismo caso: controles de edición congelados durante PUT, Guardar conserva foco | RED 40268d → GREEN 61f260 |
| 70 | Cancelar elimina errores del servidor del borrador reemplazado | RED de05cb → GREEN c39fb3 |
| 71 | Segunda fila: Restaurar elimina esos errores | RED b2a107 → GREEN 1757fa |
| 72 | Incertidumbre sobrevive Historial/Back; no PUT/GET automático; GET manual habilita nueva decisión con su revisión | RED dafc8b → GREEN 0f21d1, 37 UI |

La revisión compartida de A identificó el ciclo 72: la bandera estaba en Form y se perdía al navegar. Se trasladó al Provider; el formulario consume ese estado. Una validación de campo coherente sigue siendo corregible, sin convertirla en incertidumbre. Freeze selectivo para re-review: `appearance_shared_recovery_freeze.json` (Provider, formulario y test), tipos/lint/formato b655c3/c01910. No es cierre visual ni de feature.

Validación del checkpoint previo: 153 pruebas cliente/UI/auth/App c579d8; types encontró estrechamiento TypeScript a never después de limpiar ref y await (37e0b7), resuelto compartiendo la retirada de lectura sin cambiar su semántica. Revalidación 34 UI 6c4409, types d64e18, lint/formato 6a5b4f. Lint inicial 012b30/93cb0b exigió separar reinicio manual de carga inicial y expresar efectos asíncronos mediante la misma cadena Promise usada por otras lecturas; sin disables de reglas ni temporizadores añadidos.

A ejecutó después sus ocho suites legacy con appearance/auth/App: 558/558 GREEN 5fee26. Sus tres fixtures corregidas permanecen fuera de la propiedad B. Las nuevas filas 70–72 son posteriores y cuentan en las 37 UI, sin extrapolar aquella conjunta.

### Ciclo 73 y primer recorrido físico

73 (@s21): muestras independientes con enlace y botón nativos que no disparan acciones ni navegación, RED b13ddc → GREEN 3fb30a. Fuente/test vuelven a quedar disponibles tras la revisión P2; ocho fuentes heredadas no se reabren sin necesidad.

`e2e/appearance.spec.mjs` es un único caso real de guardar DARK y recargar. C lo ejecutó sin editar: RED 1a40f9 confirma PUT200 y data-theme dark pero canvas claro rgb(248,249,245), sidebar claro rgb(238,241,233). Evidencia inicial `.e2e-work/appearance-real/organizationweb-e2e-17452/nominal`; copia preservada por C. Después de tokens SCSS, mismo caso GREEN 3f3c9e, 1/1 en 2,7 s y 390 entradas idénticas durante ese pase; recarga también comprobada. No atribuye todavía geometría/axe/30 principios.

Se reutilizan variables SCSS y seis superficies exactas por tema. Colores semánticos/texto son fijos; acentos sólo afectan acciones/enlaces/indicadores/foco. Tarjetas que contienen acciones usan superficies neutrales definidas, sin transparencias del acento. La marca y el texto de estado no dependen del acento arbitrario. Defaults SYSTEM antes de snapshot se resuelven por media CSS; con snapshot explícito la preferencia de sistema la coordina Provider.

Luminancia calculada con la misma fórmula normativa, mínimos contra las seis superficies (esto es cálculo de paleta, no medición física): claro texto 8,8825, secundario 5,8689, error 6,1013, aviso 5,9601, éxito 5,9248, borde de control 3,4932; oscuro 8,0268 / 5,9295 / 5,5658 / 6,6187 / 6,7209 / 3,8237. Texto blanco sobre acento claro y negro sobre acento oscuro son los máximos para todos los colores admisibles: la restricción de contraste con las seis superficies limita luminancia de acentos claros por debajo del punto de igualdad blanco/negro, y los oscuros por encima. No se admite aplicar esa simplificación a otra paleta sin volver a comprobarla.

Build frontend tras tokens f5f3d8 EXIT0 (72 módulos, Vite343 ms); formato válido. Primer intento de formato del E2E desde raíz no encontró prettier, se usó la versión ya instalada de frontend y pasó f2f486; no se instalaron herramientas.

Pendientes reales: geometría/espaciado físicos, contraste de roles en pantallas representativas y matriz completa de temas/estados/motores; algunas filas UI de errores y foco aún por explicitar. Sin campaña ni gate global propio.

### Continuación de UX y oráculos de sistema

A añadió únicamente @s36 (matchMedia ausente) y las dos filas @s24 LIGHT/DARK frente a cambios del sistema. Fueron inicialmente GREEN; conjunto 41/41, EXIT0 d3d742, ESLint/formato válidos. Fuente de aplicación intacta. Evidencias: appearance_s36_initial.log, appearance_s24_fixed_initial.log y appearance_s24_s36_final.log. Snapshot del test al devolverlo: 0BB1B2C42A8FFCFD598D91BBC1C3255E1B649A63E1C904B72E7D02BB8D297173.

C reprodujo geometría RED 72fda6 a320: labels de radio21px, selectores/hex27px y enlaces de muestra21px; sin overflow. Captura/JSON preservados en progress/appearance_ux_red_geometry. Se añadieron solamente reglas .appearance para áreas44px, separación de formulario y agrupación de muestras con los tokens existentes; formato c55d1c. La repetición física está pendiente y no se presenta como GREEN.

74 (@s21/@s27): refuerzo del mismo caso de sincronización del selector. Un color de formato válido pero contraste insuficiente debe seguir representando el borrador en ambos controles, mientras la muestra conserva el seguro. RED540306/bf71a3: el picker devuelve #244c3c tras seleccionar #ffffff. No se ha modificado aún producción; se espera COPYDONE del build geométrico de C.

### Remate del formulario y freeze para gate

74 GREEN ef7017: el selector representa el hex del borrador cuando su formato es válido; la muestra sólo adopta colores que cumplen contraste. Para un texto parcial no representable por input color se conserva el último valor seguro del selector.

75–78 completan una fila por ciclo del mismo caso @s28/@s29: 412 coherente (8801b0), error de red (fd7627), 200 con campo adicional (90f45b) y problema code/type contradictorio (116d83), todos inicialmente GREEN. La fila503 existente ahora lleva problema coherente; se conserva el mismo recorrido de bloqueo, GET deliberado y foco. 79 añade a la recuperación existente la fila200 inválida, inicialmente GREEN7c8eae, conservando borrador y bloqueo.

80 y81 refuerzan el caso existente de errores obsoletos: tras un rechazo400 y posterior guardado válido, o tras400/503 y recuperación válida, persistían los tres errores de campo junto al estado confirmado. RED4f2b1c/1c104d y bdc345/e0048c; dos reinicios locales de fieldErrors al aceptar snapshot solucionan la raíz. GREEN51ebf0 y7cca13. No cambian Provider ni API.

Revisión visual root: Guardar conserva la única acción primaria; Restaurar, Cancelar y consulta manual usan superficie neutral/texto acento, sin cambiar tamaño/foco. Se mueve Apariencia al final de Principal para conservar Hoy primero; refuerzo del caso de navegación existente RED4a889e y GREEN881a70. El nodo Link es el mismo; el selector AST de mutación debe recalcular su localización.

Geometría anterior al ajuste secundario: C informó124 medidas (31anchos por2temas por2estados),4axe sin violaciones, GREENa05964/347entradas idénticas. Teclado/feedback sobre fix74: GREENc47fbb, Tab hastaGuardar,290×45px, outline3px/offset4, feedback5,1ms conPUTretenido y foco conservado tras200. Próximas mediciones de C capturan la jerarquía nueva; no se extrapola ese resultado a todo el sistema.

Formato final de cuatro archivos0b8434. Se ceden fuentes/tests quietos a A para frontend global/lint/build; B no ejecuta un global duplicado. C conserva propiedad del runnerUX y scripts propios. No campaña de mutación iniciada por B.

### Texto ampliado: ajuste local posterior al gate preliminar

C reprodujo RED371ff8 al ampliar texto200% a320CSS: scrollWidth408, inputs/botones393px y encabezado fuera del contorno. Evidencia progress/appearance_ux_red_text200. Se esperó explícitamente la frontera de A: frontend2046/44GREEN2b6247, lint/buildaf1482 e119hashespreliminaresidénticos688755.

Tras esa frontera, root autorizó sólo CSS. Se elimina el mínimo intrínseco de las columnas del formulario y muestras (minmax(0,1fr)), se permite encoger inputs y cortar palabras largas dentro de Apariencia. Cuatro declaraciones; sin cambio de color, 44px, TS ni tests. Formato5ecdc6, SCSS SHA54BB0E4339CCBA5FD54451A4F39495C13049D046552969EF0A776BA9D0756DD6. C repetirá el mismo recorrido; resultado todavía pendiente. El manifiesto frontend anterior se conserva como snapshot previo al deltaCSS, no se sobrescribe.

Mismo caso texto200 GREEN380077:24 medidas (2temas,4rutas,3anchos) y8axe sin violaciones;347 entradas idénticasada021. Incluye Apariencia, sesión real, Historial y revisión semanal. Ningún ajuste adicional necesario. A recibe la frontera para lint/build del CSS final; no se repiten2046 tests por este cambio exclusivamente visual. C continúa modalidades/zoom/motores sobre ese mismo producto.
