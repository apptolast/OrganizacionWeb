# Reparto de la noche del 9 al 10 de septiembre de 2026 — objetivo: las cinco al 100 %

Instrucción del usuario: «o las terminas esta noche o nunca», «todas el 100 %».

Vale lo de `progress/carriles/REGLAS.md`, y **además** estas cinco reglas nuevas,
que salen de las diez roturas que costó la integración de esta tarde.

## 1. Números de migración, repartidos por adelantado

Dos carriles crearon `V29` a la vez y Flyway abortó, arrastrando 965 fallos en
cascada. `git` no lo vio, porque son ficheros con nombres distintos: la colisión
vive en el espacio de nombres de Flyway.

Reservados, y **nadie usa uno que no sea el suyo**:

| Carril | Números |
|---|---|
| 25 webhooks | V31, V32 |
| 27 conector GitHub | V33 |
| 28 calendario externo | V34 |
| 29 conectores adicionales | V35, V36 |
| 30 automatizaciones | V37, V38, V39 |

Ocupados hasta hoy: hasta la `V30` inclusive. Antes de crear una, comprueba el
directorio `backend/src/main/resources/db/migration/`.

## 2. Nada de constantes que dependan de algo que crece

Las cinco roturas de esta tarde eran la misma: una constante escrita a mano que
caducó. Lista de tablas en un `TRUNCATE`, número de migración, `nth(-3)` en la
navegación, un presupuesto de doce pulsaciones de `Tab`, un ancho de ventana que
daba por hecha una pantalla.

Si vas a escribir un número o una lista, pregúntate qué la hace caducar. Si la
respuesta es «que alguien añada una ruta, una tabla o una feature», **derívala**:
`CASCADE`, contar los elementos enfocables, filtrar por la pantalla real,
afirmar la lista entera en vez de posiciones.

## 3. Ficheros compartidos, con dueño único

Para que no haya conflictos semánticos como el de `openEditor`/`openDenseScreen`,
que fusionó limpio y dejó un `ReferenceError`:

| Fichero | Dueño esta noche |
|---|---|
| `scripts/project.mjs` | orquestador |
| `scripts/project.test.mjs` | orquestador |
| `project-spec.md`, `feature_list.json` | orquestador |
| `docker-compose.yml`, `scripts/e2e.mjs` | orquestador |
| `frontend/src/App.tsx`, `workspace.tsx`, `navigation*` | orquestador |
| `e2e/automations-ux.spec.mjs`, `automations*.mjs` | carril 30 |
| `e2e/webhooks*.mjs` | carril 25 |
| `e2e/external-calendar*.mjs` | carril 28 |
| `e2e/github-connector*.mjs` | carril 27 |
| `frontend/src/github-connector*` | carril 27 |

Si necesitas tocar uno que no es tuyo, **pídemelo**; no lo hagas y avises después.

## 4. Nada de campañas de mutación en los carriles

Stryker tarda de 10 a 17 minutos y PIT más de 20, y levanta decenas de
contenedores. Cinco carriles corriéndolas a la vez es exactamente lo que colapsó
la máquina el 8 de septiembre.

**Las corre el orquestador, en serie, según vayáis terminando.** Vosotros dejad
los tests escritos y verdes, y anotad en la bitácora qué mutantes esperáis matar
para que la campaña se pueda contrastar con vuestra previsión.

## 5. Ejecutar, no razonar

Hoy el dictamen se equivocó en un oráculo que proponía, y solo se descubrió
rompiendo la producción a mano y viendo que la prueba **seguía verde**. Y una
prueba de E2E llevaba semanas siendo verde por suerte de carga.

Cada hallazgo se cierra con el rojo demostrado, y la evidencia del rojo va a la
bitácora: qué rompiste y qué mensaje dio. Si al ejecutar te contradice lo que te
he dicho yo, **gana la ejecución**: dímelo y lo corrijo.
