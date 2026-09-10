# Veredicto de cierre — feature 29-additional-connectors (features/additional_connectors.feature)

**NO APROBADA** — 15 condiciones, 6 bloqueantes. 10 de septiembre de 2026.

Dos verificadores independientes (cierres declarados / lo que sigue bloqueando) más
síntesis. Sin ejecutar nada: la máquina estaba midiendo.

## Resumen

NO se cierra. Seis condiciones bloqueantes, y la mas grave es que la puerta de mutacion de frontend nunca se ha pasado: la unica medicion real que existe la he recomputado yo del informe y da 68,51 % (Killed 494 + Timeout 13 sobre 740), contra un break de 80 en frontend/stryker.additional-connectors.config.json y un threshold 0.8 en harness.config.json:22. Ese informe es de las 11:47 y gitlab-connector.tsx —el fichero que hunde la campana, 64,58 % con 97 supervivientes— cambio DESPUES tres veces (0bb9eb2c 13:26, 39641e53 14:16, b4de28a9 14:20). La prevision del 89 % descansa en un inventario de mutantes que ya no existe, y ademas no cuadra con sus propios artefactos: los ocho progress/verificacion_mutantes_*.json tienen 162 entradas sin un solo nombre repetido, 161 MUERE y 1 SOBREVIVE, mientras el informe publica 154 y luego 153 sin ensenar la resta. Esa discrepancia la doy por absorbida en la condicion 1: cuando se mida de verdad, la cifra a mano sobra.

Lo que SI he confirmado como solido, y es mucho: la campana de backend es honesta y recomputable. La conte yo sobre mutations.xml: 343 mutantes, 330 KILLED + 1 TIMED_OUT = 331, exactamente 96,50 %, con 8 SURVIVED y 4 NO_COVERAGE sobre 32 clases, y los doce sin matar coinciden clase, metodo, linea y mutador con la tabla del informe. B4, B6 (la mitad de ImportResponse), B7, B8, B9, C1, C2 y C5 son cierres reales.

Los tres bloqueantes de contrato los he verificado uno a uno contra el codigo y los tres se sostienen. B5: @s11 (feature:132,136) fija app.gitlab.api-base en https://gitlab.example.com/api/v4 y exige HTTP 200 con ese apiBase, y GitlabApiBaseTest, en el metodo etiquetado @s11 s11_refusesAnythingElseWhenTheBeanIsBuilt, lleva ese valor exacto entre los RECHAZADOS; GitlabApiBase.of solo admite gitlab.com oficial o loopback. La prueba afirma lo contrario de su escenario. B10: @s32 (feature:413) pide el identificador de correlacion de la peticion en los logs del fallo, y ConnectorAudit.java no admite parametro de correlacion en ninguno de sus cuatro metodos; el unico correlationId del repositorio lo acuna ApiErrors.java:127 en el manejador de 500, otro camino, y el oraculo que se cita como cierre afirma owner=OWNER, que es el propietario y no la peticion. B6 esta cerrado a medias: ConnectionResponse.lastActivityAt:291 sigue VIVO con NullReturnVals y ninguna prueba afirma por valor ese campo a traves de ese accesor (ConnectorCatalogApiTest:130 mide la fila del catalogo, otra clase; GitlabConnectorApiTest:169 afirma null, que el mutante satisface), de modo que @s9:105 y @s15:188 quedan sin oraculo. Y token_nonce simplemente no existe en V29__additional_connectors.sql:37 —grep del nombre en todo backend/src devuelve cero—, asi que @s9:107 no puede verificarse mientras el .feature diga eso.

DESCARTO cuatro motivos, y me parece bien que los verificadores los trajeran para que alguien los cerrara. (1) El partial="true" del XML no es defecto: los cinco informes de PIT del repositorio abren igual, es como PIT emite una campana con targetClasses acotado; la premisa de B2 era falsa. (2) Que App.tsx quede fuera del ambito de Stryker de la 29 no es condicion: el juez lo eximio expresamente, C1 le puso oraculo (connectors-catalog-routing.test.tsx) y todo ambito acotado excluye ficheros compartidos; exigirlo arrastraria produccion de otras features al denominador. (3) Es FALSO que current.md siga listando C7 como pendiente: current.md:212-214 lo pone bajo «Lo que ya se cerro hoy», verificado contra el transcript. Solo sobrevive la mitad buena del hallazgo —no hay artefacto firmado—, y por eso queda como condicion menor. (4) Rebajo B11: no es cierto que produccion CONTRADIGA la fila 2 de @s24; esa fila dice «429 sin Retry-After -> 60» y HttpGitlabIssueSource:178-181 devuelve exactamente DEFAULT_RETRY_SECONDS cuando no hay ratelimit-reset. Lo real, y lo unico que conservo, es que la rama del reset esta en NO_COVERAGE con dos mutantes: produccion sin ninguna prueba.

Ninguna comprobacion de esta revision ha ejecutado nada: hay una campana de PIT viva en la maquina. Todo sale de lectura de ficheros y git de solo lectura. Las condiciones marcadas «campana» son precisamente las que quedan PENDIENTES DE EJECUCION.

---

## Condiciones

| # | Condición | Bloq. | De quién | Min. |
|---|---|---|---|---|
| 1 | La puerta de mutacion de frontend no se ha pasado nunca: la unica medicion real es 68,51 %, por debajo del break de 80, y esta caducada porque gitlab- | **SÍ** | campana | 45 |
| 2 | B5 sigue ABIERTO: @s11 exige que https://gitlab.example.com/api/v4 sea aceptada y devuelva 200, y la prueba etiquetada @s11 la lista entre las RECHAZA | **SÍ** | propietario | 30 |
| 3 | B10 sigue ABIERTO y es hueco de implementacion: @s32 (feature:413) pide el identificador de correlacion de la peticion en los logs del fallo y Connect | **SÍ** | carril | 60 |
| 4 | B6 se declaro CERRADO y solo se cerro la mitad. GitlabConnectorController$ConnectionResponse.lastActivityAt:291 sigue vivo con NullReturnVals: el acce | **SÍ** | carril | 30 |
| 5 | @s9:107 nombra una columna token_nonce de 12 bytes que NO existe: V29__additional_connectors.sql:37 declara solo token_ciphertext, con el nonce embebi | **SÍ** | propietario | 20 |
| 6 | La puerta de cierre del arnes no se ha ejecutado nunca sobre este arbol; el CP6 del propio juez sigue sin marcar. Sin ella no hay init verificado ni m | **SÍ** | campana | 40 |
| 7 | Las cuatro decisiones de contrato abiertas —B5, B10, B11 y D-2— viven solo en la bitacora del carril. progress/decisiones_pendientes.md tiene nueve se | no | orquestador | 25 |
| 8 | El informe de mutacion de backend no hace el contraste que el juez declaro innegociable: lista los 12 sin matar nominalmente y no explica ninguno, ni  | no | orquestador | 35 |
| 9 | Los dos VoidMethodCall vivos sobre rejectQuery(parameters) en GitlabConnectorController.delete:92 y startImport:105 borran la guarda de parametros de  | no | carril | 30 |
| 10 | @s15:187 («existen exactamente 2 enlaces ... con url igual a web_url de cada issue») se declara cubierta y ninguna prueba lee esas columnas contra la  | no | carril | 25 |
| 11 | B11 sigue PARCIAL: la rama ratelimit-reset de HttpGitlabIssueSource:180-181 esta en NO_COVERAGE con dos mutantes. No contradice la fila 2 de @s24 —sin | no | carril | 30 |
| 12 | La bitacora de a11y publica una cifra que ya no se recomputa contra el codigo: dice «los tres anchos» y «6 mediciones bajo zoom nativo», y el spec en  | no | orquestador | 25 |
| 13 | Toda la acreditacion del rojo de C2 vive fuera del repositorio: .e2e-work esta en .gitignore:60 y los nueve volcados solo existen en C:/Users/vhurt/ow | no | orquestador | 15 |
| 14 | C7: la contrafirma de la enmienda de @s31 la escribio el mismo carril que hizo la enmienda. current.md la da por cerrada «verificada contra el transcr | no | propietario | 10 |
| 15 | D-2: el componente Receipt de gitlab-connector.tsx pinta cuatro cifras y la bandera de truncado y nunca el status del recibo, asi que un recibo runnin | no | propietario | 20 |

## El trabajo, una por una

### 1. La puerta de mutacion de frontend no se ha pasado nunca: la unica medicion real es 68,51 %, por debajo del break de 80, y esta caducada porque gitlab-connector.tsx cambio tres veces despues del informe. La prevision del 89 % no es una cifra.

- **Bloqueante:** sí · **De:** campana · **Estimación:** 45 min

Relanzar la campana sobre HEAD y publicar la cifra con su marca de tiempo: bin\harness.ps1 mutate additional_connectors-frontend (equivale a pnpm --dir frontend exec stryker run stryker.additional-connectors.config.json). Reescribir la seccion «Recuento y prevision» de progress/mutacion_additional_connectors_frontend.md con el numero medido y retirar el recuento a mano de 154/153, que no reconcilia con sus propios artefactos (162 entradas, 161 MUERE). Si cae por debajo de 80, lo que falte pasa a ser trabajo de carril: escribir los oraculos que maten a los supervivientes que queden.

### 2. B5 sigue ABIERTO: @s11 exige que https://gitlab.example.com/api/v4 sea aceptada y devuelva 200, y la prueba etiquetada @s11 la lista entre las RECHAZADAS. El escenario, tal y como esta escrito, no puede pasar.

- **Bloqueante:** sí · **De:** propietario · **Estimación:** 30 min

Decidir cual de las dos orillas se mueve: o se enmienda @s11 y @s9 para que el Given nombre una base de bucle local (que es lo que GitlabApiBase.of admite y lo que el servidor falso puede servir), o se amplia GitlabApiBase con una lista blanca de instancia autoalojada configurada. Registrar la decision en progress/decisiones_pendientes.md con contrafirma, como se hizo con @s31, y despues ajustar feature y prueba.

### 3. B10 sigue ABIERTO y es hueco de implementacion: @s32 (feature:413) pide el identificador de correlacion de la peticion en los logs del fallo y ConnectorAudit no admite ninguno. El oraculo que se da por cierre afirma owner=OWNER, que es otro valor.

- **Bloqueante:** sí · **De:** carril · **Estimación:** 60 min

Meter el identificador de correlacion de la peticion en las firmas de ConnectorAudit (connectionRefused e importFailed como minimo), acunarlo donde ya se acuna en ApiErrors.java:127 o propagarlo desde el filtro, y ampliar GitlabTokenConfinementTest para afirmar que la linea del fallo lleva CONNECTION_INVALID Y ese identificador. Alternativa legitima: que el propietario enmiende @s32 y retire la clausula, tambien con contrafirma.

### 4. B6 se declaro CERRADO y solo se cerro la mitad. GitlabConnectorController$ConnectionResponse.lastActivityAt:291 sigue vivo con NullReturnVals: el accesor puede devolver null y la suite entera sigue verde, de modo que @s9:105 («lastActivityAt igual al instante de la conexion») y @s15:188 no tienen oraculo por valor.

- **Bloqueante:** sí · **De:** carril · **Estimación:** 30 min

Ampliar el oraculo de frontera en GitlabConnectorApiTest para afirmar $.lastActivityAt POR VALOR contra el instante del reloj fijo en la conexion, y contra finishedAt del recibo en la ruta de @s15, igual que se hizo con ImportResponse. Comprobar despues que el mutante muere volviendo a correr el ambito de backend.

### 5. @s9:107 nombra una columna token_nonce de 12 bytes que NO existe: V29__additional_connectors.sql:37 declara solo token_ciphertext, con el nonce embebido, y el nombre no aparece en ningun sitio de backend/src. La clausula no puede tener oraculo mientras el .feature diga eso.

- **Bloqueante:** sí · **De:** propietario · **Estimación:** 20 min

Resolver la seccion 2 de progress/decisiones_pendientes.md, que ya esta bien planteada: enmendar el Then de @s9 para que describa el nonce embebido en token_ciphertext, con la contrafirma del propietario en el propio .feature. Es la unica de las cuatro decisiones abiertas que ya esta donde el propietario mira.

### 6. La puerta de cierre del arnes no se ha ejecutado nunca sobre este arbol; el CP6 del propio juez sigue sin marcar. Sin ella no hay init verificado ni mutacion agregada sobre HEAD.

- **Bloqueante:** sí · **De:** campana · **Estimación:** 40 min

bin\harness.ps1 verify sobre HEAD, una vez cerradas las condiciones de codigo, y pegar la salida en progress/. Ojo a la maquina: hay una campana de PIT viva, hay que esperar a que termine.

### 7. Las cuatro decisiones de contrato abiertas —B5, B10, B11 y D-2— viven solo en la bitacora del carril. progress/decisiones_pendientes.md tiene nueve secciones y solo la 2 es de la feature 29. El propietario no las ve donde mira.

- **Bloqueante:** no · **De:** orquestador · **Estimación:** 25 min

Anadir a progress/decisiones_pendientes.md una seccion por cada una, con el mismo formato que las nueve existentes: que pasa, la cita exacta del .feature y del codigo, y la pregunta cerrada. Sin esto, las condiciones de propietario de esta lista no pueden ni empezar.

### 8. El informe de mutacion de backend no hace el contraste que el juez declaro innegociable: lista los 12 sin matar nominalmente y no explica ninguno, ni dice que la prevision del juez salio desmentida (acepto de antemano supervivientes en ConnectorRow y ApiCredentialStatusSource, y salen 8/8 y 11/11). Ademas la tabla «las tres clases que ahora si se miden» cambia en silencio las tres que B2 nombraba.

- **Bloqueante:** no · **De:** orquestador · **Estimación:** 35 min

Reescribir progress/mutacion_additional_connectors_backend_medida.md: una linea por superviviente explicando por que sobrevive o que oraculo le falta, el contraste explicito contra la seccion «Supervivientes que acepto de antemano» del juez, y una nota diciendo que ConnectorsDisabledException y GitlabUnavailableException estan declaradas en build.gradle.kts:96-101 pero no aparecen en el XML por ser clases de cuerpo vacio, que es legitimo y hoy no esta dicho en ningun sitio.

### 9. Los dos VoidMethodCall vivos sobre rejectQuery(parameters) en GitlabConnectorController.delete:92 y startImport:105 borran la guarda de parametros de consulta del DELETE y del POST /imports y la suite entera sigue verde. El unico oraculo, s31_aqueryStringOnAWriteRouteIsRefusedBeforeReachingTheUseCase, prueba solo el PUT, y @s31 no tiene ninguna fila de query string en sus ocho ejemplos: es produccion que ningun test exige.

- **Bloqueante:** no · **De:** carril · **Estimación:** 30 min

Ampliar esa prueba a las tres rutas de escritura (o parametrizarla) para que un DELETE y un POST /imports con parametros de consulta se rechacen antes de llegar al caso de uso, y anadir la fila correspondiente a los Examples de @s31 para que la guarda tenga respaldo en el contrato.

### 10. @s15:187 («existen exactamente 2 enlaces ... con url igual a web_url de cada issue») se declara cubierta y ninguna prueba lee esas columnas contra la tabla real: GitlabConnectorPersistenceTest solo hace count("task_external_links") en :188, :214 y :291. Su gemelo de GitHub si lo hace (GithubConnectorPersistenceTest:289, SELECT source, external_id, url, task_id).

- **Bloqueante:** no · **De:** carril · **Estimación:** 25 min

Copiar el patron del gemelo de GitHub: en GitlabConnectorPersistenceTest, SELECT source, external_id, url, task_id y afirmar los dos external_id y que cada url es el web_url del issue que le toca. Cubre de paso la columna que PostgresImportedTaskCommit.java:121 escribe y que hoy no afirma nadie en la 29.

### 11. B11 sigue PARCIAL: la rama ratelimit-reset de HttpGitlabIssueSource:180-181 esta en NO_COVERAGE con dos mutantes. No contradice la fila 2 de @s24 —sin cabecera de reset produccion devuelve 60, que es lo que la fila pide—, pero es una rama de produccion que ninguna prueba toca y que decide el plazo de reintento.

- **Bloqueante:** no · **De:** carril · **Estimación:** 30 min

O anadir a @s24 una cuarta fila que fije la conducta con RateLimit-Reset presente y su prueba, o que el propietario declare la rama fuera de contrato y se retire de produccion. Cualquiera de las dos mata los dos mutantes.

### 12. La bitacora de a11y publica una cifra que ya no se recomputa contra el codigo: dice «los tres anchos» y «6 mediciones bajo zoom nativo», y el spec en main se titula «en los anchos que caben» y filtra fits = WIDTHS.filter(w => w*2 + chromeWidth <= availWidth), asi que en el xvfb de 1280 px de CI solo cabe 320. Ademas la linea 275 compara contra el mismo fits que el test acaba de calcular, de modo que esa asercion no puede fallar.

- **Bloqueante:** no · **De:** orquestador · **Estimación:** 25 min

Enmendar progress/a11y_conectores_29.md para decir cuantos anchos se miden de verdad en CI y anadirlo al apartado 4 de limites; y en e2e/additional-connectors-native-zoom.spec.mjs:275, afirmar ademas que los omitidos quedan escritos con su motivo, que es lo unico que hoy queda sin diente.

### 13. Toda la acreditacion del rojo de C2 vive fuera del repositorio: .e2e-work esta en .gitignore:60 y los nueve volcados solo existen en C:/Users/vhurt/ow-worktrees/conectores-a11y. Si ese worktree se poda, la prueba del rojo desaparece y nadie podra recomputarla.

- **Bloqueante:** no · **De:** orquestador · **Estimación:** 15 min

Copiar los once ficheros de .e2e-work/a11y-log/ y los dos evidence.json a un directorio versionado bajo progress/evidencia_a11y_29/, y corregir las citas de la bitacora para que apunten alli.

### 14. C7: la contrafirma de la enmienda de @s31 la escribio el mismo carril que hizo la enmienda. current.md la da por cerrada «verificada contra el transcript» —no esta listada como pendiente, al contrario de lo que dice uno de los informes—, pero no hay artefacto firmado por el propietario sobre un cambio de conducta esperada (401 -> 403).

- **Bloqueante:** no · **De:** propietario · **Estimación:** 10 min

Que el propietario confirme la enmienda de features/additional_connectors.feature:388-396 en progress/decisiones_pendientes.md, en una linea, para que la puerta de aprobacion humana sobre el .feature quede documentada fuera del carril que la escribio.

### 15. D-2: el componente Receipt de gitlab-connector.tsx pinta cuatro cifras y la bandera de truncado y nunca el status del recibo, asi que un recibo running se presenta identico a una importacion terminada sin resultados. El contrato no cubre el hueco: @s35 (feature:446-447) modela la importacion como sincrona.

- **Bloqueante:** no · **De:** propietario · **Estimación:** 20 min

Decidir si la 29 muestra el estado del recibo. Si si, anadir la clausula al Then de @s35 y pintarlo; si no, dejarlo escrito como limite conocido y quitar la exportacion sin uso de readGitlabImport.

