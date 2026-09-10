# Plan de campañas de mutación — 10 de septiembre de 2026

## Por qué existe este fichero

Hoy se han caído **tres campañas**: dos por memoria y una por carga (dos pruebas
de 32 MiB a través de nginx no pasaron sin mutación, con ocho carriles y 31
contenedores compitiendo). Y el panel encontró que **una cifra publicada como
medida describe un árbol que no existe en ningún commit**.

Las dos cosas tienen la misma causa: se midió cuando no se podía medir. Así que
las campañas dejan de lanzarse a mano según hace falta y pasan a tener orden,
condiciones de arranque y acta.

## Condiciones de arranque — las tres, o no se lanza

1. **Máquina drenada.** Cero carriles trabajando. Se comprueba con
   `docker ps -q | wc -l` por debajo de 5 y sin `gradlew` ni `vitest` vivos.
   Ésta es la que mató la campaña de la 29.
2. **Árbol limpio y `build/` borrado.** `git status` limpio y `rm -rf backend/build` antes de
   PIT, para que no reutilice clases compiladas viejas. Ésta es la que produjo
   el informe irreproducible de la 25: el XML sitúa `elapsedMillis` en la línea
   237 y no está en 237 en **ningún** commit.
3. **SHA anotado.** Se apunta el commit exacto antes de arrancar y se escribe en
   el acta. «Sobre `main`» no vale: `main` se mueve.

## Verificación obligatoria antes de publicar cifra

- **Calcular del XML o del JSON**, nunca leer el entero del `index.html`. La
  feature 30 publicó «96,00 %» y el XML daba **95,72 %**.
- **Comprobar que las líneas del XML casan con el fuente** del SHA anotado. Es
  la comprobación que habría cazado el informe de la 25 el día que se escribió.
- **Comprobar que los tests que se escribieron para matar aparecen como
  `killingTest`.** Si un test existe y no aparece, la campaña es de otro árbol.
- **Comprobar que los cinco adaptadores de bitácora ya reciben mutantes.** Hoy
  se quitó `org.slf4j` del `avoidCallsTo`, que era la causa de que
  `Slf4jWebhookAudit`, `Slf4jConnectorAudit`, `Slf4jExternalCalendarAudit` y
  `Slf4jAutomationAudit` salieran con cero. **Si siguen a cero, el arreglo no
  funcionó** y hay que decirlo en vez de dar la condición por cerrada.
- **Barrer patrones muertos**: ninguna clase del ámbito puede salir con cero
  mutantes sin explicación. Hoy han aparecido seis ámbitos mal apuntados.

## El atajo combinado NO existe. Probado cuatro veces, muerto cuatro veces

**Tachado el 10 de septiembre a las 14:45.** Lo que sigue en esta sección era el
plan de medir las cinco features en una pasada con el ámbito `noche_cinco`. **No
funciona en esta máquina**, y ya estaba escrito que no funcionaba:
`backend/build.gradle.kts` lo dice desde anoche —«probado con cuatro hilos y con
dos, y el sistema lo mató por memoria las dos veces»— y aun así se volvió a
intentar. Tercer y cuarto intento, muertos igual: el de las 14:22 llegó a superar
la fase de cobertura, creó las 195 unidades de mutación, subió a 62 contenedores
de Testcontainers y el sistema lo mató a los 23 minutos.

**La lección, que es la misma de todo el día:** había un hallazgo escrito, con su
razón, y se ignoró porque el atajo era atractivo. Exactamente lo que produjo los
siete ámbitos mal apuntados y la cifra irreproducible.

**Las campañas van UNA POR FEATURE.** Está probado que caben: la de la 28 dio
91,71 % y la de la 27 dio 93,23 % en esta misma máquina. Son ~35 min cada una y
no hay forma de bajar de ahí.

<details>
<summary>El razonamiento del atajo, conservado por si alguna vez hay máquina</summary>

## Un atajo legítimo para las cinco de PIT

PIT gasta **~17 minutos calculando cobertura** sea cual sea el ámbito, porque
`targetTests` es la suite entera. Ese coste es el mismo para una feature que para
cinco. Ya existe el ámbito **`noche_cinco`** (`backend/build.gradle.kts:54`) que
une los cinco conjuntos **por referencia**, así que hereda automáticamente las
correcciones de ámbito de hoy.

La puntuación **por feature** sale después, del XML, que trae el resultado clase
a clase: son los mismos mutantes y las mismas pruebas que en las campañas
separadas, no una medida distinta. Eso convierte **cinco campañas de PIT en
una**.

Ya murió dos veces por memoria, pero las dos con carriles trabajando encima. Se
intenta primero combinada y, si vuelve a caer, se baja a una por feature. La
diferencia son unas tres horas.

</details>

## Lo que NO se va a hacer, y por qué

**No se van a excluir pruebas del cálculo de cobertura.** La tentación es
concreta: `ImportSocketTest` tardó **68,2 s** y es la que tumbó la campaña de la
29 al no pasar sin mutación; excluirla sería más rápido y más robusto, y además
sería *conservador* —quitar una prueba sólo puede bajar la puntuación, nunca
subirla—, porque una prueba de límites de nginx no puede matar un mutante de
webhooks.

Aun así no se hace. Falló **por carga**, con nueve carriles encima, y en máquina
drenada pasa. Excluirla trataría el síntoma y, sobre todo, cambiaría el
instrumento a mitad de la medición, que es exactamente la clase de decisión que
ha producido hoy seis ámbitos mal apuntados y una cifra irreproducible. Si vuelve
a caer **con la máquina drenada**, entonces sí hay un problema de estabilidad de
esa prueba, y se arregla en la prueba, no en el ámbito.

## La cola, por orden

Diez campañas. A ~30 min cada una son unas **cinco horas** de máquina drenada, y
conviene decirlo en vez de fingir que caben en un hueco.

| # | Campaña | Por qué hay que rehacerla |
|---|---|---|
| 1 | PIT webhooks (25) | El informe vigente es irreproducible: el bloqueante B2. Es la peor de todas y va primera. |
| 2 | PIT github_connector (27) | El ámbito cambió: el comodín arrastraba 81 mutantes de la feature 29. El 93,23 % caducó. |
| 3 | PIT additional_connectors (29) | Nunca ha llegado a producir `mutations.xml`. Ahora ya tiene directorio propio. |
| 4 | PIT automations (30) | Verificar el 95,72 % después del trabajo de los tres carriles. |
| 5 | PIT external_calendar (28) | 91,71 % medido, pero antes del trabajo pendiente de sus bloqueantes. |
| 6 | Stryker webhooks (25) | Iba por 71,40 %, en rojo. Depende del carril. |
| 7 | Stryker github-connector (27) | Producción cambiada por el arreglo del `@s41`. Y falta `github-connector-draft.ts` en la lista a mutar. |
| 8 | Stryker additional-connectors (29) | Iba por 68,51 %, en rojo. Depende del carril. |
| 9 | Stryker automations (30) | 91,18 %, a rehacer tras los tres carriles. |
| 10 | Stryker external-calendar (28) | 91,32 % medido hoy; sólo si los bloqueantes cambian producción. |

## Un arreglo pendiente antes de la número 7

`frontend/stryker.github-connector.config.json` muta tres ficheros y **no
incluye `src/github-connector-draft.ts`**, que es producción nueva del arreglo
del `@s41`. Si se mide sin añadirlo, se repite exactamente el defecto que el
panel lleva todo el día cazando: producción de la feature fuera del ámbito que
la puntúa. Hay una guarda en `scripts/project.test.mjs:1898` que enumera la
lista; hay que actualizarla en el mismo commit.
