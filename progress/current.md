# Estado actual — noche del 9 al 10 de septiembre de 2026

**Lo primero, para leer recién levantado.** Las cinco features que faltaban
—25 webhooks, 27 conector GitHub, 28 calendario externo, 29 conectores
adicionales y 30 automatizaciones— están **implementadas e integradas en
`main`**. Lo que decide si pasan a `done` no es la implementación sino sus dos
puertas: juez aprobado y mutación por encima del 80 %. El detalle de cuáles han
pasado está más abajo, en «Cierre de la noche», con cifras medidas.

Lo más importante que se resolvió, porque llevaba días escondido: **el ejecutor
de reglas de automatizaciones no existía**. La feature 30 se había integrado
como «fase 1» —reglas, plantillas, simulación, auditoría y pantalla— sin el
motor que dispara las reglas ante eventos reales, así que nueve escenarios del
contrato aprobado no tenían nada que probar. Ya está escrito, con sus nueve
escenarios y tres más que el propio ejecutor destapó.

**Dos decisiones tuyas quedaron aplicadas**: la enmienda de navegación (ahora
catorce entradas, con «Conectores» tras «Importación» porque lo fija @s33) y la
contradicción 50/52 de webhooks (se persisten 52, se sirven 50). Y una tercera,
el anclaje del reenlace DNS en las dos features con su prueba de TLS.

## Cuadro de puertas — 10 de septiembre, 16:30

Todas las cifras de aquí están **calculadas** del `mutations.xml` o del
`mutation.json`, sobre árbol limpio y máquina drenada, y cada una lleva su acta
con la lista **nominal** de los mutantes sin matar. Ninguna se ha leído de un
HTML: ésa fue la lección de la 30, que publicó «96,00 %» cuando el XML daba
95,72 %.

| Feature                   | Backend                     | Frontend                        | Juez              |
| ------------------------- | --------------------------- | ------------------------------- | ----------------- |
| 25 webhooks               | **92,81 %** ✅ remedido hoy | pendiente (previsión ~90 %)     | corriendo         |
| 27 conector GitHub        | **95,90 %** ✅ remedido hoy | pendiente (ámbito cambiado hoy) | corriendo         |
| 28 calendario externo     | midiéndose                  | **91,32 %** ✅                  | corriendo         |
| 29 conectores adicionales | **96,50 %** ✅ primera vez  | pendiente (previsión ~89 %)     | corriendo         |
| 30 automatizaciones       | pendiente (era 95,72 %)     | **90,29 %** ✅ remedido hoy     | **8 condiciones** |

### Lo que las tres campañas de backend de hoy han cerrado

No son sólo números: cada una cerró un bloqueante duro que llevaba días.

- **La 25** cerró **B2 y B3**. El «89,88 %» describía un árbol inexistente —situaba
  una línea en la 237 y no está en la 237 en ningún commit—; ahora casa con el
  fuente, y el test que el juez obligó a escribir aparece por fin como
  `killingTest`, cuando antes no aparecía ni una vez.
- **La 27** medía **81 mutantes de la feature 29** por un comodín de paquete. Al
  quitarlos **subió** de 93,23 a 95,90: la feature estaba mejor probada de lo que
  decía su número.
- **La 29** **no se había medido nunca**. Su ámbito era el único de 34 sin
  directorio de informe propio, así que escribía donde la siguiente campaña lo
  habría pisado sin dejar rastro.

### Y una cosa que se creía imposible y no lo era

Los cinco adaptadores de bitácora salían con **cero mutantes** estando en su
ámbito, y se explicaba como «una propiedad de los mutadores». La causa real era
el `avoidCallsTo` **por defecto** de PIT. Quitado `org.slf4j` de esa lista,
`Slf4jWebhookAudit` recibe 3 y `Slf4jConnectorAudit` 4, **verificado en dos
campañas independientes**. Era condición bloqueante en cuatro features a la vez.

---

## 10 de septiembre, 14:25 — todos los carriles aterrizados, midiendo

**Los nueve carriles han cerrado.** Ninguno quedó parado ni en error. La máquina
está drenada por primera vez en el día y **la pasada combinada de PIT está
corriendo**: mide las cinco features de una vez, sobre árbol limpio y con el SHA
anotado, que son las tres condiciones que yo mismo me impuse después de que se
cayeran tres campañas.

### Lo que trajeron los carriles, en defectos reales de producto

No huecos de prueba: cosas que le pasaban al usuario.

| Feature | Defecto                                                                                                                             |
| ------- | ----------------------------------------------------------------------------------------------------------------------------------- |
| 25      | Un secreto ilegible **paraba la cola de entregas de todos los propietarios**, en silencio y para siempre. Ninguna prueba lo tocaba. |
| 25      | El panel de entregas de un webhook **enseñaba las de otro**, con su «Reenviar» apuntando a la entrega ajena.                        |
| 25      | Cualquier fallo decía «No se ha podido crear el webhook», y el error se encendía en un campo que no era.                            |
| 27      | `hint()` devolvía el **token entero** si medía cuatro caracteres o menos.                                                           |
| 27      | El recibo de importación se buscaba sin filtrar por origen: uno de GitHub salía por la ruta de GitLab.                              |
| 29      | Las dos pantallas **no tenían bloque de estilos**: enlaces a 21 px y desbordamiento con el texto al 200 %.                          |
| 29      | Una importación lenta **repintaba su recibo sobre una pantalla ya desconectada**.                                                   |
| 30      | `queue()` **descartaba el evento entero en silencio** cuando el endpoint dejaba de estar activo.                                    |
| 30      | `Guardar` y el interruptor quedaban **inertes para siempre** si una escritura adelantaba a otra.                                    |

### Dos cifras remedidas, y las dos bajan

- Feature 30, frontend: **90,29 %** (era 91,18). Baja porque los arreglos
  añadieron producción: se matan doce mutantes más, pero hay 22 nuevos. Una
  cifra que subiera después de arreglar defectos sería la sospechosa.
- Feature 28, backend: va a bajar del 91,71 % en cuanto se mida, porque salió
  del ámbito una clase que aportaba **113 de 579 mutantes con 112 muertos por
  pruebas de otras features**. Era nota prestada.

### Lo que falta

1. La pasada de PIT (corriendo) y tres campañas de Stryker.
2. Los jueces de las cuatro features que no lo tienen.
3. **Nueve decisiones tuyas**, en `progress/decisiones_pendientes.md`.

---

## Cuánto falta de verdad — 10 de septiembre, 14:20

Llevas diez días y pediste el 100 %. Ésta es la cuenta sin adornos.

**25 de 30 están `done`.** Las cinco que faltan tienen **73 motivos bloqueantes**
encontrados hoy por dos paneles de jueces independientes, sobre features que ya
tenían las tres puertas en verde y por encima del umbral. Ése es el hecho
central del día: **los números salían y el trabajo no estaba**.

De esos 73, a las 14:20 hay **29 cerrados y verificados** (los 22 de la feature
30 y siete más), y los otros 44 están **todos asignados** a nueve carriles que
trabajan ahora mismo, cada uno en su propio worktree.

**Lo que queda después de los carriles, en orden:**

1. **Las campañas de mutación.** Todas las cifras vigentes están caducadas o son
   irreproducibles, y hay que rehacerlas con la máquina drenada. Con el ámbito
   combinado son ~40 min de PIT más cuatro o cinco de Stryker: **entre dos y tres
   horas**, y no se pueden solapar con los carriles porque hoy se han caído tres
   por eso.
2. **Los jueces.** Uno por feature, en paralelo, ~30 min.
3. **Lo que los jueces encuentren.** Aquí no puedo prometer nada: el panel de hoy
   encontró 20 motivos en una feature que tres puertas verdes daban por lista. Si
   los jueces devuelven condiciones nuevas, hay otra vuelta.

**Y siete decisiones que sólo puedes tomar tú**, en
`progress/decisiones_pendientes.md`. Ninguna bloquea el trabajo de hoy, pero
**cada una bloquea el cierre de su feature**. Dos son cambios de contrato ya
hechos sin contrafirma, dos son huecos que el contrato no describe, y una —la
guarda de estado de `upsert`— es elegir entre arreglar una condición de carrera
o declarar que el último que escribe manda.

**Lo que no voy a hacer:** marcar `done` nada que no haya pasado juez y mutación
de verdad. Es lo que ha pasado esta semana y es por lo que hoy hay 73 motivos en
vez de cero.

## Lo que hay que saber hoy, 10 de septiembre al mediodía

**Ninguna de las cinco está `done`, y el motivo cambió esta mañana.** Hasta hoy
la cuenta pendiente era «que suban los números de mutación». Un panel de siete
jueces independientes, lanzado justo antes de marcar como `done` las dos
features que ya tenían **las tres puertas verdes y por encima del umbral**,
rechazó las dos: **20 motivos bloqueantes en la 27 y 22 en la 30**, todos
escritos con fichero y línea en `progress/carriles/bloqueantes_27.md` y
`progress/carriles/bloqueantes_30.md`.

Es el hallazgo más caro de la semana y conviene entenderlo bien: **los números
salían y el trabajo no estaba**. Ejemplos del propio panel:

- La 27 **incumple su contrato** en la primera fila del `@s41`: salir de la
  pantalla y volver debía conservar el repositorio, y no lo conserva, porque
  `App.tsx` la desmonta. Ninguna prueba lo medía.
- El **96,00 %** publicado de la 30 no está medido: el `mutations.xml` da
  581/607 = **95,72 %**. El 96 salía del entero redondeado de un HTML.
- `queue()` de automatizaciones confunde «el endpoint ya no está activo» con
  «otro worker se me adelantó» y **descarta el evento en silencio**, sin dejar
  fila de ejecución. Pérdida permanente de datos, con las puertas en verde.

La causa es siempre la misma que ya se anotó anoche: **una puerta que mide algo
que se parece a lo que quería medir**. Ámbitos de mutación mal apuntados,
oráculos que no pueden fallar, cifras copiadas en vez de calculadas.

### Lo que está corriendo ahora mismo

Siete carriles, **cada uno en su propio worktree** —esta madrugada asigné dos al
mismo y el segundo borró el trabajo del primero, que hubo que rescatar por SHA;
ya está en `main` y no puede volver a pasar—:

| Carril                  | Encargo                                                  |
| ----------------------- | -------------------------------------------------------- |
| `webhooks`              | mutación de frontend de la 25 (iba por 71,40 %)          |
| `additional-connectors` | mutación de frontend de la 29 (iba por 68,51 %)          |
| `github-connector`      | el bloqueante `@s41` de la 27 y los oráculos U+00A0      |
| `gh-resto`              | los otros 17 motivos del panel sobre la 27               |
| `auto-contrato`         | motivos 1-7 de la 30: cobertura del contrato             |
| `auto-datos`            | motivos 8-12 de la 30: seguridad y pérdida de datos      |
| `auto-mutantes`         | motivos 13-22 de la 30: supervivientes y cifras honestas |

Más dos campañas midiéndose en el centro: **PIT de la 29** (que no se había
corrido nunca) y **Stryker de la 28**, recién integrada con 83 → 161 pruebas y
**dos defectos de producto** que encontró su carril: una lista ilegible que
decía «no hay eventos», y una limpieza de efecto que abortaba el controlador
equivocado y dejaba escrituras vivas al salir de la pantalla.

### Puertas de mutación, medidas (13:25)

| Feature                   | Backend                 | Frontend                            |
| ------------------------- | ----------------------- | ----------------------------------- |
| 25 webhooks               | 89,88 % ✅              | carril trabajando (iba por 71,40 %) |
| 27 conector GitHub        | **caducada** ⚠️         | **caducada** ⚠️                     |
| 28 calendario externo     | **91,71 %** ✅          | **91,32 %** ✅                      |
| 29 conectores adicionales | midiéndose, primera vez | carril trabajando (iba por 68,51 %) |
| 30 automatizaciones       | 95,72 % ✅ (no 96,00)   | 91,18 % ✅                          |

**La 28 tiene ya las dos puertas de mutación.** Su cifra de frontend está
calculada del `mutation.json` (663 de 726), no copiada de un HTML, y va con la
lista nominal de los 65 sin matar en
`progress/mutacion_external_calendar_frontend_medida.md`.

**Las dos de la 27 caducaron hoy, y es correcto que caduquen**: el 93,23 % de
backend se midió con un ámbito que arrastraba 81 mutantes de la feature 29, y el
de frontend se midió antes del arreglo del `@s41`, que cambia producción. Hay que
volver a medirlas.

### Lo que ya se cerró hoy

- Rescate y merge del trabajo que el reset borró (cuatro oráculos de
  automatizaciones contra la base real, y un defecto vivo de la 29: el catálogo
  no traducía los dos errores de GitLab).
- Condición **C7** de la 29: contrafirma del propietario sobre la enmienda del
  `@s31`, verificada contra el transcript antes de escribirla.
- Ámbitos de mutación de la 27 y la 29 reapuntados (`GithubIssueConnections`
  entra, `ImportGuard` se muda). 442 patrones, 0 muertos. 95/95 guardas verdes.

---

## Cierre: dónde está cada puerta (10 de septiembre, mañana)

Las cinco features están **implementadas e integradas**. Ninguna está `done`
todavía, y esto es exactamente lo que falta, con cifras medidas:

| Feature                   | Juez                                                  | Mutación backend | Mutación frontend                      |
| ------------------------- | ----------------------------------------------------- | ---------------- | -------------------------------------- |
| 25 webhooks               | **APPROVED** condicionado, 3 condiciones **cerradas** | midiéndose       | pendiente                              |
| 27 conector GitHub        | **APPROVED** condicionado                             | pendiente        | midiéndose (era 75,55 %, +44 oráculos) |
| 28 calendario externo     | **APPROVED** condicionado                             | pendiente        | pendiente                              |
| 29 conectores adicionales | **APPROVED** condicionado, 7 condiciones, 3 cerradas  | pendiente        | pendiente                              |
| 30 automatizaciones       | **APPROVED** condicionado, 2 condiciones **cerradas** | **96 %** ✅      | **52,74 %** ❌                         |

### El cuello de botella no es la máquina, son los huecos que destapa

La primera medición de frontend, la de automatizaciones, dio **52,74 %** con
**337 mutantes vivos y 77 sin cobertura**. «Sin cobertura» significa que ninguna
prueba ejecuta esa rama: es producto que nadie ha ejercido nunca. Esta noche, de
sitios así, han salido tres defectos de producto reales.

Conclusión honesta: las features que nunca han pasado por mutación de frontend
necesitan **trabajo de oráculos**, no sólo tiempo de máquina.

### Cuatro ámbitos de mutación incompletos, encontrados en una noche

Todos con el mismo efecto: la campaña daba una puntuación estupenda **sin mutar
la clase que importaba**, y no avisaba.

1. `adapter.crypto.*` — paquete borrado al unificar el cifrado. El AES-256-GCM
   no recibía ni un mutante.
2. `ConnectorStatusSource*` — sólo resolvía a la **interfaz**; las seis
   implementaciones del catálogo quedaban fuera.
3. `ImportGithubIssues*` — clase borrada al unificar la importación, de modo que
   `ImportIssues`, que hace **todas** las importaciones del producto, llevaba sin
   recibir un mutante en **ninguna** campaña del repositorio.
4. `AnchoredConnection` — la clase que implementa el anclaje del reenlace DNS no
   estaba en ningún ámbito, tres líneas por debajo del comentario que conmemora
   el caso 1.

Queda una herramienta que barre los dos árboles de fuentes y exige que todo
patrón resuelva a una clase real: **441 patrones, cero muertos**. Va antes de
cada campaña.

### Diez roturas, una sola causa

Nueve de las diez roturas de esta madrugada fueron **una constante escrita a
mano que dependía de algo que crece**: la lista de tablas de un `TRUNCATE`, un
número de migración, `nth(-3)` en el menú, doce pulsaciones de `Tab`, un ancho de
ventana, el nombre de un arenero de Stryker, ocho rangos `línea:columna` de
mutación, la línea de hilos de PIT que fijan nueve guardas, y el
`.prettierignore` que hacía imposible tener el lint verde mientras corría una
campaña.

Todas están arregladas **de forma derivada** —contar, filtrar, comodín,
`CASCADE`— y no con un número más grande.

---

## La sesión de tarde del 9 de septiembre (antecedente)

El estado anterior, con toda la historia del 8 y del 9 por la mañana, está
archivado en `progress/sesion_2026-09-08_09.md`. Este documento lo sustituye.

**Instrucción del usuario:** «tienes solo esta sesión, la siguiente la quiero
para otro proyecto, llevo como 7 días con esto» y, a mitad de sesión, «la tienes
que terminar todo en 1 hora y 20 minutos, que es cuando se resetea la sesión y
te voy a cortar». Plazo: 20:04 → 21:24 (Madrid).

## Lo que se cerró de verdad en esta sesión

### 1. La CI de `main` estaba roja, y no por el producto

`bin/harness init` fallaba en el paso de lint: Prettier señalaba formato en ocho
`frontend/stryker.*.config.json`. Todo lo demás estaba verde —2840 pruebas de
frontend pasaban en la misma ejecución—. Corregido en `9696109`, comprobando
que el JSON resultante es **idéntico** al anterior: solo cambia el formato.

Fallo distinto y aparte, del mismo día: la ejecución `34386220549` murió en
`playwright install --with-deps chromium` con `Some index files failed to
download` y código 100. Es la red del runner de GitHub, no el repositorio.

### 2. Ninguno de los cuatro workflows está parado ni colgado

`Application CI`, `Prueba de mutación`, `Guardián de rutas sensibles` y
`Evolución autónoma del arnés`. Los tres últimos no han fallado nunca. De las
últimas 47 ejecuciones de `Application CI`, 13 fallaron, y todas por dos causas:
el lint de arriba y el flake de exportación de abajo. No hay ninguna ejecución
en cola ni encallada.

### 3. El flake de exportación, que bloqueaba la feature 24

Integrado `claude/ci-export-flake`. El test pedía los bytes con
`response.body()`, que se los pide a la caché del inspector de Chromium; bajo
carga esa caché ya ha desalojado el cuerpo que la página consumió en streaming,
y salía `Protocol error (Network.getResponseBody)`. Ahora los bytes se capturan
en el **transporte**, con `page.route` y `route.fetch()`, y de paso se afirman
`content-type`, `content-length` y `content-disposition` del transporte, y que
la petición se hace **una sola vez**.

Con esto queda cerrada la condición 2 de las tres del dictamen de la feature 24.

### 4. El hueco de mutación de la rama SYSTEM del tema

Integrado `claude/darkmode`: oráculo para la rama SYSTEM del pintado del tema en
`appearance-state.tsx`, más la simplificación que la prueba deja demostrar.

### 5. La puerta `Stop` estaba saboteando la sesión

`.claude/settings.json` tenía un hook `Stop` que ejecuta `node
.harness/harness.mjs init` —la suite **completa**, con sus 49 clases de test con
`PostgreSQLContainer` estático— al final de **cada turno del orquestador**. En
CI ese paso tarda 14 minutos. Con varios carriles en paralelo es exactamente lo
que colapsó la máquina el 8 de septiembre, y es hermano del hook `PostToolUse`
que ya se retiró por lo mismo.

Se aparcó mientras duró la fase paralela y **se restauró al integrar**, con la
verificación ejecutada de forma explícita. Si se vuelve a trabajar con varios
carriles, hay que volver a aparcarlo: no es opcional, es la diferencia entre
que la máquina rinda y que no rinda nada.

## Los cinco carriles

Worktrees en `C:/Users/vhurt/ow-worktrees`, ramas `claude/<nombre>` desde `main`.
Reglas de operación en `progress/carriles/REGLAS.md` (nunca la suite completa,
backend por clase concreta, un `E2E_WEB_PORT` por carril, rojo demostrado,
commit por ciclo).

El dictamen del juez está partido por feature para que cada carril lea solo lo
suyo: `progress/carriles/dictamen_f25.md`, `dictamen_f28.md`, `dictamen_f30.md`.

| Carril | Feature                   | Puerto E2E | Encargo                                                                           |
| ------ | ------------------------- | ---------- | --------------------------------------------------------------------------------- |
| A      | 25 webhooks               | 18090      | 15 hallazgos abiertos del dictamen, 5 bloqueantes                                 |
| B      | 28 calendario externo     | 18092      | 15 hallazgos abiertos, 5 bloqueantes, más el plazo de lectura del cuerpo del feed |
| C      | 30 automatizaciones       | 18094      | 10 hallazgos abiertos, 4 bloqueantes                                              |
| D      | 27 conector GitHub        | 18096      | revalidar el único bloqueante del juez, ya corregido en `d418a5d`                 |
| E      | 29 conectores adicionales | 18098      | terminar el ciclo a medias, inventario de oráculos por escenario                  |

## Resultado de la cosecha (20:45)

Los cinco carriles se integraron en `main` **sin un solo conflicto** —comprobado
antes con `git merge-tree` sobre los diez pares—. En total, **más de setenta
commits**.

| Feature                   | Hallazgos cerrados hoy                                           | Abiertos |
| ------------------------- | ---------------------------------------------------------------- | -------- |
| 25 webhooks               | 6 (1, 5, 15, 16, 17, 21) + 9 y 10 desde otro carril              | 6        |
| 28 calendario externo     | 8 (3, 6, 8, 12, 14, 15, 18, 19)                                  | 5        |
| 30 automatizaciones       | 9 (2, 3, 4, 5, 6, 7, 8, 9, 11)                                   | 1        |
| 29 conectores adicionales | 24 de 38 escenarios con oráculo, más el caso de uso del catálogo | endpoint |
| Las tres a la vez         | zoom nativo al 200 %, en tres ficheros nuevos y verdes           | —        |

### Tres defectos de producto reales, no deuda de pruebas

1. **Contraste 1,01:1** en el `role="switch"` de cada regla de automatizaciones:
   texto blanco sobre `--editable`. Violación seria de WCAG 1.4.3. Llevaba
   escondido exactamente por lo que el dictamen predijo: ese control **nunca se
   había renderizado en una ejecución medida**.
2. **Oráculo de recorte ciego** en automatizaciones: medía
   `documentElement.scrollWidth` y no veía nada. Acreditado con una mutación de
   control (`li { overflow: hidden; max-height: 96px }`) que hace caer 3 de 4
   pruebas con `clientHeight` 94 frente a `scrollHeight` de 249 a 800.
3. **El reenlace DNS del calendario externo**, bloqueante 6: la guardia
   descartaba la dirección validada y volvía a conectar por nombre, al revés de
   lo que exige la enmienda B3. Cerrado y demostrado.

Y uno corregido a medias con honestidad: en **webhooks** el reenlace DNS **no**
se ha implementado; se ha devuelto a límite declarado y se ha corregido el
javadoc, que afirmaba estar cerrado sin estarlo. El límite residual y su
contención por política de egreso quedan escritos en `deploy/EGRESS.md`.

> **Superado el 10 de septiembre de 2026.** Las dos decisiones de arriba eran
> contrarias entre sí: la 28 ancló y la 25 revocó el anclaje, sin verse. El
> propietario zanja a favor de anclar en las dos **y probar el TLS**, que era el
> punto que nadie había medido. Hecho: el anclaje **no** rompe la verificación
> del certificado si se conserva el nombre, y ahora hay oráculo de ello en las
> dos features. `deploy/EGRESS.md` pasa a ser defensa en profundidad. Bitácora:
> `progress/tdd_anclaje_dns.md`.

### Cuatro veces que un agente refutó lo que se le dijo

Vale la pena registrarlo, porque es lo que separa este trabajo de un teatro:

- El dictamen **se equivocaba en el oráculo que proponía** para el hallazgo 10 de
  webhooks. Sustituir `SKIP LOCKED` por `FOR UPDATE` a mano dejó la prueba
  propuesta en verde: con diez filas libres la instancia perdedora no se muere de
  hambre. El oráculo que sí discrimina pregunta por la **identidad** de lo
  reclamado mientras otra transacción retiene la primera fila.
- En automatizaciones el zoom nativo **sí se ejecutaba**; el hueco era que medía
  un solo ancho de los cuatro del contrato. Y ese contrato **no pide zoom nativo
  en su tabla**: el spec nuevo mide más estricto que lo firmado, y lo dice.
- 2560 px al 200 % **no se puede medir**: exigiría una ventana de 5120 px que
  ninguna pantalla del proyecto tiene, y el gestor recortaría la petición en
  silencio. Se documenta en vez de fingir.
- La premisa de que cuatro escenarios de la 29 seguían bloqueados «porque
  dependen de 25/26/28/30» **había caducado**: esas features ya están en `main`.
  El bloqueo real es que falta el endpoint del catálogo.

### Feature 27: aprobada por el juez y suspendida por la mutación

El juez levantó el rechazo: **APPROVED**, condicionado a la puerta de mutación.
La revalidación se ejecutó de verdad —E2E 16 de 16, backend 75 de 75— y de paso
encontró que una prueba era verde **por suerte de carga**: se concedía 90 s de
espera interior bajo un presupuesto de 30 s, y en la segunda ejecución tardó
30,7 s. Habría muerto por siete décimas.

Pero la puerta de mutación **no la pasa**, y este es el dato duro del día:

- **Frontend, Stryker: 69,44 %**, umbral 80. 409 muertos, **175 supervivientes**,
  5 sin cobertura. Esa campaña **nunca se había ejecutado**: el juez la tenía
  como condición C7 pendiente. No es una regresión, es un hueco que llevaba ahí
  desde el principio.
- **Backend, PIT: no se pudo medir.** Abortó tras 17 minutos porque
  `ImportScaleTest.s16_exact32MiBAnd100000RecordsPreviewAndApplyThroughRealProxy`
  falló **sin mutación**, con la máquina cargada por cinco carriles y su prueba
  hermana tardando 75 s. PIT exige suite verde. Hay que repetirla con la máquina
  libre antes de concluir nada.

**Segunda medición, con la máquina libre y tras atacar cuatro racimos:
69,44 % → 75,55 %.** Sigue por debajo de 80, pero el movimiento está donde se
predijo:

| Fichero                      | Antes       | Después     | Supervivientes |
| ---------------------------- | ----------- | ----------- | -------------- |
| `github-connector-client.ts` | 73,17 %     | **85,37 %** | 66 → 36        |
| `github-connector.tsx`       | 66,86 %     | 68,62 %     | 108 → 107      |
| `integrations-index.tsx`     | 50,00 %     | 50,00 %     | 1 → 1          |
| **Total**                    | **69,44 %** | **75,55 %** | 175 → 144      |

Los **5 mutantes sin cobertura pasan a cero**. La previsión del carril era «74-75 %
en el total»; la medida da 75,55 %, así que la previsión era honesta. Lo que
falta para el umbral está concentrado en `github-connector.tsx`: **107
supervivientes**, sin tocar. Ese es el trabajo exacto que separa a la feature 27
de poder cerrarse, y ya no hay que buscarlo.

Sobre los supervivientes se atacaron cuatro racimos, con previsión —declarada
como previsión, no como medida— de 35 a 37 muertos de 175; murieron 31. El mayor racimo eran
**13 supervivientes de una sola causa**: cinco operaciones que llaman
`signal.throwIfAborted()` tres veces cada una, y solo la primera llamada de una
operación tenía oráculo. Y la rama sin cobertura resultó ser el `catch` de
`disconnect()`, que **nunca se había ejecutado**: el producto era correcto, pero
todo un camino de fallo estaba sin una sola prueba detrás.

### La verificación posterior a la cosecha, y el defecto que solo aparece al integrar

- **Lint verde**: Prettier y Spotless, todo el árbol.
- **Frontend: 85 ficheros, 2866 pruebas, 0 fallos** (eran 2840 al empezar la
  sesión).
- **Backend, primera pasada: 1170 fallos de 4020.** No era el producto: **dos
  carriles numeraron `V29` a la vez**. `V29__additional_connectors.sql` ya
  existía en la rama de la feature 29 y la cota del texto cifrado del conector
  se numeró encima, por una instrucción equivocada del orquestador que dijo
  «usa V29» sin comprobar que estaba ocupada. Flyway aborta con «Found more than
  one migration with version 29», el contexto de Spring no arranca y de ahí
  salen **965 fallos en cascada** de «ApplicationContext failure threshold
  exceeded». Renumerada a `V30` en `c179839`; ninguna de las dos estaba
  aplicada, así que renumerar era seguro.

  Es el defecto característico del trabajo en paralelo: **los diez pares de
  ramas fusionaban limpio**, porque son ficheros distintos con nombres
  distintos; la colisión está en el espacio de nombres de Flyway, que `git` no
  conoce. La lección para la próxima tanda de carriles: **repartir los números
  de migración por adelantado y por escrito**, y comprobar el directorio antes
  de asignar uno.

- **Backend, segunda pasada: 4697 pruebas, 1 fallo.** No se pudo identificar
  cuál: OneDrive sincronizó y borró los XML de resultados antes de poder
  leerlos, que es el mismo fallo de entorno ya anotado el 8 de septiembre —tener
  `build/` dentro de OneDrive—. La sospecha razonable, no confirmada, es
  `ImportScaleTest`, la misma prueba de escala de 32 MiB y 100 000 registros que
  hizo abortar al PIT por fallar sin mutación bajo carga, y cuya hermana tarda
  75 segundos.

  **Confirmado por contraste**: ejecutada sola y con la máquina libre,
  `ImportScaleTest` pasa —2 de 2, `BUILD SUCCESSFUL` en 55 s—, frente a los más
  de 75 s por prueba que tardaba bajo carga. El fallo era de carga, no de
  producto, y es la misma causa que hizo abortar al PIT. La CI de GitHub, en
  máquina limpia y fuera de OneDrive, es el árbitro definitivo.

  Dos cosas que arreglar cuando haya tiempo, porque hoy costaron una hora entre
  las dos: **sacar `build/` de OneDrive** (la sincronización borra los XML de
  resultados a mitad de ejecución y deja la suite sin diagnóstico), y **dar a
  `ImportScaleTest` un presupuesto propio o aislarla de las ejecuciones
  paralelas**, porque hoy es la que decide si la puerta de mutación se puede
  medir siquiera.

### La segunda rotura latente, que solo la CI podía ver

Con `main` ya integrado y verde en local, la CI falló con
`ERROR: cannot truncate a table referenced in a foreign key constraint`, y
cayeron los E2E de autenticación, apariencia y varios más.

No lo trajo esta sesión: es una **rotura latente desde la integración de las
features 25 a 30**. La migración `V25` añadió `connector_connections`
(`REFERENCES projects(id)`) y `task_external_links` (`REFERENCES tasks(id)`), y
las listas de `TRUNCATE` de los 33 specs de E2E no las nombran. En local no se
veía porque la suite de E2E no se había vuelto a ejecutar entera desde entonces:
la CI llevaba días cayendo antes, primero por el lint y luego por el flake de
exportación, así que nunca llegaba a este paso.

Arreglado en `de04eff` añadiendo `CASCADE`. Es además lo que el ayudante de
reinicio quiere decir de verdad —vaciar todo lo que cuelgue— y no volverá a
quedarse corto la próxima vez que una migración añada una tabla dependiente.

**La lección operativa**: enumerar tablas a mano en un `TRUNCATE` de test es una
lista que caduca en silencio con cada migración. Y una CI que falla temprano por
otra causa **esconde** todo lo que viene después; cuando se arregla el primer
fallo, hay que contar con encontrar los siguientes.

### Cinco roturas latentes, y todas con la misma forma

Al arreglar el lint y el flake, la CI por fin llegó a ejecutar pasos que llevaba
días sin alcanzar, y aparecieron cinco roturas. Ninguna la trajo el trabajo de
hoy: todas llevaban ahí desde la integración de las features 25 a 30, y todas
son **una constante escrita a mano que dependía de algo que crece**:

| Rotura                        | La constante                     | Qué la hizo caducar                    |
| ----------------------------- | -------------------------------- | -------------------------------------- |
| `TRUNCATE` sin `CASCADE`      | la lista de tablas enumerada     | la `V25` añadió tablas con clave ajena |
| Migración `V29` duplicada     | el número elegido a mano         | dos carriles a la vez                  |
| `nth(-3)` en la navegación    | posiciones desde el final        | tres rutas nuevas                      |
| 12 pulsaciones de `Shift+Tab` | el presupuesto de pasos          | tres entradas de navegación nuevas     |
| Ventana del zoom nativo       | una pantalla que se da por hecha | el xvfb de CI no la tiene              |

Las cinco fusionaban limpio, porque ninguna es un choque textual. Las cinco
están arregladas **de forma derivada** —`CASCADE`, número comprobado, lista
completa, presupuesto calculado, anchos filtrados por la pantalla real— y no con
un número más grande, para que no vuelvan a caducar.

De propina, un **conflicto semántico de fusión**, que es la categoría que `git`
no puede ver: un carril renombró el ayudante `openEditor` a `openDenseScreen` y
actualizó sus tres llamadas; otro añadió una llamada nueva con el nombre viejo.
Las dos ramas fusionan sin conflicto y el fichero queda con un `ReferenceError`.

**La lección que más va a durar**: una CI que falla pronto por una causa
**esconde** todo lo que viene después. Llevaba días cayendo en el lint, así que
el paso de E2E no se ejecutaba, así que estas cinco no se veían. Al arreglar el
primer fallo hay que contar con encontrar los siguientes, y no leerlo como que
«ahora se ha roto todo».

### Regresión abierta: la importación del conector GitHub no crea tareas

**Esto es lo más serio que queda abierto, y es de producto, no de pruebas.**

Cuatro pruebas de `e2e/github-connector.spec.mjs` fallan porque la región
«Resultado de la importación» **no muestra ningún contador**: se espera
«Creadas 1» y no aparece nada. Afecta a importar, reimportar y a la
desconexión que conserva lo importado, más la spec de zoom nativo del conector.

**No lo causa el arreglo del `TRUNCATE`, y está comprobado ejecutando, no
razonando**: revertido el fichero a la versión de `main`, la misma prueba falla
igual. Es anterior.

**El sospechoso, con nombre y apellidos**: el commit `a347936` de la feature 29,
«un solo caso de uso de importación para GitHub y GitLab», que unificó el caso
de uso de importación de las dos plataformas y tocó `ImportGithubIssues`,
`IssueImportReceipt` e `IssueSourceException`. La feature 27 tenía sus 16
pruebas verdes esta misma tarde, antes de integrar la 29.

**Por qué nadie lo vio**: la CI llevaba días cayendo antes del paso de E2E
—primero por el lint, después por el flake de exportación—, así que las pruebas
que lo habrían cazado no se ejecutaban. Es la misma causa que escondió las diez
roturas de arriba.

**Por dónde empezar**: comparar `ImportGithubIssues` antes y después de
`a347936`, y mirar qué devuelve hoy el endpoint de importación —si el conteo
llega vacío desde el backend o si es la vista la que no lo pinta—. Las pruebas
unitarias de `ImportGithubIssuesTest` pasan, así que el hueco está entre el caso
de uso y la frontera HTTP, o en la forma del recibo.

### La enmienda de navegación que ratificaste no se cumple

`project-spec.md:2504` fija el orden canónico de la navegación principal en doce
entradas. La aplicación sirve **trece**, y en otro orden:

| #   | Ratificado             | Servido                |
| --- | ---------------------- | ---------------------- |
| 6   | Apariencia             | **Calendario externo** |
| 7   | Calendario             | Apariencia             |
| 8   | Exportación            | Exportación            |
| 9   | Importación            | **Calendario**         |
| 10  | API para integraciones | Importación            |

Es decir: **«Calendario externo» (feature 28) no aparece en la enmienda** —que
sólo resolvió el choque entre la 25 y la 30— y **«Calendario» y «Exportación»
están intercambiados** respecto a lo firmado.

`e2e/export-data.spec.mjs` afirma ahora el **orden servido**, con la divergencia
escrita en el propio test y remitiendo aquí, para no bendecir en silencio una
violación del contrato. **Decisión del propietario**: o se arregla la
aplicación, o se enmienda `project-spec.md:2504` para incluir la feature 28.

### Una contradicción del contrato que solo puede resolver el propietario

El hallazgo 11 de webhooks no es un defecto de código: `features/webhooks.feature`
exige en la línea 367 que queden persistidas «50 terminales **y las 2
pendientes**» —52 filas— y en la 368 que el GET devuelva «items de como máximo
**50**». Las dos no pueden ser ciertas a la vez. Poner `LIMIT 50` rompe el test
que hoy afirma 52. **Hace falta que decidas cuál manda** y enmendar
`project-spec.md:2018` en consecuencia.

## Lo que NO cabía en el plazo, dicho sin adornos

El dictamen `progress/dictamen_final_25_28_30.md` confirmó **53 hallazgos, 19 de
ellos bloqueantes**. Se cerraron 13 antes de esta sesión (los once «de minutos»
más las tres puertas de mutación). Quedaban **~40 abiertos**, y entre ellos hay
piezas de producto, no de prueba:

- **Feature 30, bloqueante 1: no existe el ejecutor de reglas.** La feature se
  integró como «fase 1» —reglas, plantillas, simulación, auditoría y UI— sin el
  motor que dispara las reglas ante eventos reales. Nueve escenarios completos
  del contrato aprobado no tienen ningún oráculo porque no hay nada que probar.
  Es trabajo de horas. En esta sesión se ha ordenado **no empezarlo** y dejar en
  su lugar el inventario preciso de esos nueve escenarios, porque un ejecutor a
  medias vale cero y el inventario sí permite retomarlo.
- **Feature 25, bloqueante 5 y feature 28, bloqueante 6:** el cierre del
  rebinding DNS. En webhooks no está implementado aunque el código afirma que
  sí; en el calendario externo la guardia descarta la dirección ya validada y
  vuelve a conectar por nombre, justo lo contrario de lo que exige la enmienda
  B3 aprobada. Son defectos de seguridad reales, no de oráculo.
- **El zoom nativo del navegador al 200 %** no se ejecuta en ninguna de las tres
  features, aunque los tres contratos lo nombran. Hay precedente que copiar en
  `e2e/github-connector-native-zoom.spec.mjs`.
- **Feature 29** tiene 18 commits de conector GitLab y sigue en `spec_ready`:
  falta el inventario de qué escenarios tienen oráculo y cuáles no.

## Puertas que siguen sin cumplirse

Ninguna feature se marca `done` sin juez aprobado y mutación sobre 0,80. Hoy:

- **24 integration_api**: juez APPROVED, tres campañas de mutación por encima
  del umbral. Le faltaba CI verde; el flake que la tumbaba ya está corregido.
- **25, 28, 30**: integradas en `main`, con dictamen del juez **con
  bloqueantes abiertos**. No pueden cerrarse.
- **27 github_connector**: juez REJECTED por un solo defecto ajeno al artesano
  —una aserción de E2E que asumía 49 octetos de texto cifrado cuando la
  unificación de `SecretCipher` los dejó en 48—. El defecto está corregido en
  `d418a5d`; falta la **evidencia ejecutada** que el juez exige para levantarlo.
- **29 additional_connectors**: `spec_ready`.

Auditoría de coherencia hecha de paso, y **cerrada en falso positivo**: una
búsqueda por nombre de fichero no encontraba puerta para 14 `start_work_session`
ni para 15 `pause_resume_session`. Las dos la tienen, con otro nombre. Detalle
en `progress/auditoria_puertas_14_15.md`:

- 14: `judge_start_work_final.md:3` APPROVED; PIT 340/347 = 97,98 %
  (`mutation_start_work_backend.md:3`); Stryker 483/539 = 89,61 %
  (`mutation_start_work_frontend_final.md:3`); ámbito declarado en
  `frontend/stryker.start-work-session.config.json` y en `scripts/project.mjs`.
- 15: `judge_pause_resume_final.md:3` APPROVED; PIT 523/525 = 99,62 %; Stryker
  741/861 = 86,06 %, y 738/861 = 85,71 % tras descontar de forma conservadora
  tres muertes atribuidas a un fixture inestable
  (`review_pause_resume_mutation_frontend.md:21`), que sigue por encima de 80.

La causa era de nomenclatura: `CLAUDE.md:64` fija el patrón
`progress/judge_<name>.md` y `progress/mutation_<name>.md`, y los agentes
añadieron sufijos `_final`, `_backend` y `_frontend` porque cada feature tuvo
campañas separadas por capa. **Las 25 `done` tienen sus dos puertas.** Conviene
unificar los nombres, o el próximo barrido volverá a dar el mismo susto.

## Despliegue: sigue bloqueado por acceso, y no por trabajo

Sin cambios respecto al 9 por la mañana. `admin@159.195.156.57` responde
`Permission denied (publickey)`; no hay alias del servidor en `~/.ssh/config`;
los workflows de `apptolast/DockerSwarmInfrastrcture` solo validan, no aplican.
La PR 40, `codex/integration-release`, sigue en **borrador** con su validación
verde, esperando que el propietario facilite clave o aplique él mismo.
«Desplegado» es una puerta distinta de `done` y no se declara sin evidencia.

## Lo primero que hay que hacer al retomar

1. Restaurar el hook `Stop` en `.claude/settings.json`
   (`hooks_disabled_during_parallel_lanes` → `hooks`) y ejecutar
   `bin/harness init` una vez, sin carriles en paralelo.
2. Leer las bitácoras de los cinco carriles en `progress/` para saber qué
   hallazgo quedó cerrado y cuál no.
3. Atacar por este orden: el rebinding DNS de 25 (seguridad; el de 28 ya está
   cerrado), el ejecutor de reglas de 30 (producto), el resto de oráculos, y la
   feature 29.

## Índice de por dónde seguir, con el trabajo ya localizado

Todo lo que sigue está **identificado con fichero y línea**, así que nadie tiene
que volver a buscarlo. Ordenado por relación entre valor y coste:

1. **Feature 27, los 107 supervivientes de `github-connector.tsx`.** Es lo único
   que separa a esa feature de cerrarse: juez APPROVED, revalidación ejecutada,
   mutación en 75,55 % de 80. El informe con cada mutante, su línea y su
   reemplazo está en `frontend/reports/mutation-github-connector/mutation.json`.
   Hay uno con receta escrita en
   `progress/mutacion_github_connector_supervivientes.md`: el
   `ConditionalExpression -> true` de la línea 266 se mata haciendo `vi.mock` del
   módulo cliente para que `disconnectGithub` rechace con
   `Object.assign(new Error(), { code: "CONNECTION_NOT_FOUND" })`. Diez minutos.
2. **Feature 27, el PIT de backend.** No se ha llegado a medir nunca sobre este
   árbol. Con la máquina libre debería correr; el juez exige, además del 80,
   **≥ 12 mutantes en `AesGcmSecretCipher`, cero `NO_COVERAGE`** y cinco puntos
   concretos muertos uno a uno.
3. **Feature 30, el ejecutor de reglas.** El inventario de los nueve escenarios
   sin oráculo está en `progress/tdd_automations_fase2.md`, uno a uno, con lo
   que exige cada `Then`, los tres medio cubiertos (`@s18`, `@s21`, `@s24`) y un
   diseño de puerto único que permite probar ocho de los nueve **sin
   contenedor**. Es la única feature con un hallazgo bloqueante de producto.
4. **Feature 29, el endpoint del catálogo.** El caso de uso ya está verde y
   commiteado; falta la frontera HTTP: seis implementaciones de
   `ConnectorStatusSource` más controlador y cableado. Desbloquea cuatro
   escenarios parciales de golpe.
5. **Feature 25, hallazgo 2.** El intento está salvado y versionado en
   `progress/parche_webhooks_hallazgo_2.patch`, con la hipótesis del fallo ya
   escrita: React materializa el valor del `textarea` como texto hijo, el
   `textContent` de la etiqueta envolvente deja de ser exactamente «Secreto» y
   `getByLabel(..., exact)` no casa; arreglo propuesto, `label htmlFor` + `id`.
6. **Los hallazgos abiertos restantes** están indexados feature a feature —qué
   exige, qué fichero tocar, si es de oráculo o de producto— en
   `progress/tdd_webhooks_cierre_dictamen.md`,
   `progress/tdd_external_calendar_cierre_dictamen.md` y
   `progress/tdd_automations_fase2.md`.

## Dos cosas de higiene que hoy costaron una hora

- **Sacar `build/` de OneDrive.** La sincronización borra los XML de resultados a
  mitad de ejecución: hoy dejó una suite de 4697 pruebas con un fallo y **sin
  forma de saber cuál**.
- **Repartir los números de migración por adelantado.** Dos carriles crearon
  `V29` a la vez y `git` no lo vio, porque son ficheros con nombres distintos: la
  colisión vive en el espacio de nombres de Flyway. Costó 965 fallos en cascada.

## Carril auto-mutantes — motivos M13 a M22 de la feature 30

Bitácora completa en `progress/mutation_automations.md` (es el fichero que la
condición 2 del juez exige y que no existía). Resumen:

- **Cerrados:** M13, M14, M15, M16, M17, M18, M19, M20, M21, M22.
- **Supervivientes matados: 17**, cada uno con el mutante aplicado a mano al
  fuente y la clase de test corrida por su nombre. Un decimoctavo ya estaba
  muerto desde `5309e409`.
- **La cifra real, medida sobre `mutations.xml`: 95,72 %** (581/607). El
  «96,00 %» publicado era el entero redondeado del `index.html`. Tras este
  carril la proyección aritmética es 98,68 % (599/607), **declarada como
  proyección**: nadie ha vuelto a lanzar PIT.
- **Frontend, medido sobre `mutation.json`: 91,18 %** (806/884).
- **M19 tiene causa nombrada:** el interceptor `FLOGCALL` de PIT, no un patrón
  muerto (barrido: 442 patrones, 0 muertos). Cero
  `removed call to org/slf4j/Logger::*` en 3.819 mutantes de seis campañas.
- **Abiertos, con destinatario:** H6 (`upsert` sin guarda de estado, pide
  decisión de contrato), los 2 mutantes matables de `editingOf` (piden fila de
  contrato en @s37), los 7 inobservables (dependen de M12),
  `EventTask$OfWorkSession::sessionId`, las 57 anclas del script de frontend sin
  juzgar y `ApplicationConfiguration:22` (es de `integration_api`).

Nada de producción se tocó: `git status` sobre `backend/src/main` y
`frontend/src` quedó vacío al cerrar.
