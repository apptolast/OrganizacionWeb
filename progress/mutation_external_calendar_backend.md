# Mutación de backend — feature 28, calendario externo

**95.29 %** (445/467), calculado del `mutations.xml`.

Sobre el SHA `354f36dc`, árbol limpio y máquina drenada, **después** de retirar del
producto las features 27, 29 y 30.

## Todas las capas pasan el listón de 80 %

El veredicto fijó **80 % en cada capa**, no sólo en el global, precisamente porque un
global alto puede esconder una capa hundida. Ninguna queda por debajo:

| Capa                  | Puntuación | Mutantes |
| --------------------- | ---------- | -------- |
| `adapter.feed`        | 85.1 % ✅  | 40/47    |
| `adapter.net`         | 86.4 % ✅  | 19/22    |
| `application`         | 92.0 % ✅  | 80/87    |
| `domain`              | 97.8 % ✅  | 178/182  |
| `adapter.http`        | 98.3 % ✅  | 57/58    |
| `adapter.persistence` | 100.0 % ✅ | 39/39    |
| `adapter.connectors`  | 100.0 % ✅ | 30/30    |
| `adapter.logging`     | 100.0 % ✅ | 2/2      |

## Sube de 93,58 % a 95.29 %

Y sube por trabajo real, no por relajar nada: el ámbito no ganó ni una clase, y de hecho
la medición anterior ya se había hecho **sin** `ApplicationConfiguration`, que aportaba
113 de 579 mutantes con 112 muertos por pruebas de **otras** features.

`adapter.logging` aparece con 2 mutantes y los dos muertos: es la cuarta confirmación de
que quitar `org.slf4j` del `avoidCallsTo` funcionó. Antes esa capa no existía en el
informe porque su única clase salía con cero.

## Los 22 sin matar, nominalmente

| Clase                        | Método             | Línea | Mutador                      | Estado      |
| ---------------------------- | ------------------ | ----- | ---------------------------- | ----------- |
| `AnchoredConnection`         | `literal`          | 48    | ConditionalsBoundaryMutator  | SURVIVED    |
| `AnchoredConnection`         | `authority`        | 57    | ConditionalsBoundaryMutator  | SURVIVED    |
| `AnchoredConnection`         | `isAddressLiteral` | 76    | ConditionalsBoundaryMutator  | SURVIVED    |
| `ExternalCalendarController` | `single`           | 163   | EmptyObjectReturnValsMutator | NO_COVERAGE |
| `HttpCalendarFeed`           | `download`         | 159   | NegateConditionalsMutator    | SURVIVED    |
| `HttpCalendarFeed`           | `download`         | 159   | VoidMethodCallMutator        | NO_COVERAGE |
| `HttpCalendarFeed`           | `read`             | 202   | ConditionalsBoundaryMutator  | SURVIVED    |
| `HttpCalendarFeed`           | `read`             | 203   | NullReturnValsMutator        | NO_COVERAGE |
| `HttpCalendarFeed`           | `read`             | 207   | NullReturnValsMutator        | NO_COVERAGE |
| `HttpCalendarFeed`           | `read`             | 210   | NegateConditionalsMutator    | SURVIVED    |
| `HttpCalendarFeed`           | `expired`          | 218   | ConditionalsBoundaryMutator  | SURVIVED    |
| `IcsFeed$Property`           | `parameters`       | 144   | EmptyObjectReturnValsMutator | NO_COVERAGE |
| `IcsFeed$Property`           | `of`               | 148   | ConditionalsBoundaryMutator  | SURVIVED    |
| `IcsFeed$Property`           | `of`               | 153   | ConditionalsBoundaryMutator  | SURVIVED    |
| `IcsFeed$Property`           | `of`               | 158   | ConditionalsBoundaryMutator  | SURVIVED    |
| `PublicAddressPolicy`        | `unmap`            | 55    | NegateConditionalsMutator    | SURVIVED    |
| `PublicAddressPolicy`        | `unmap`            | 60    | NullReturnValsMutator        | NO_COVERAGE |
| `PublicAddressPolicy`        | `unmap`            | 63    | NullReturnValsMutator        | NO_COVERAGE |
| `PublicAddressPolicy$Cidr`   | `network`          | 67    | NullReturnValsMutator        | NO_COVERAGE |
| `PublicAddressPolicy$Cidr`   | `prefixLength`     | 67    | PrimitiveReturnsMutator      | NO_COVERAGE |
| `PublicAddressPolicy$Cidr`   | `of`               | 70    | NullReturnValsMutator        | SURVIVED    |
| `PublicAddressPolicy$Cidr`   | `contains`         | 78    | BooleanTrueReturnValsMutator | NO_COVERAGE |

---

## `DeleteExternalCalendar` sale con cero mutantes, y por qué no es un problema

Se deja escrito porque si no, el próximo juez lo vuelve a abrir — y con razón,
porque «una clase del ámbito con cero mutantes» es señal de alarma en este
repositorio: así se descubrieron los cinco adaptadores de bitácora que estaban
exentos sin que nadie lo supiera.

**Aquí la causa es distinta y es benigna.** El cuerpo del caso de uso es una sola
llamada **no-void cuyo valor se descarta**, y ningún mutador por defecto de PIT
toca esa forma: no hay condicional que negar, ni frontera que mover, ni retorno
que sustituir. No es que la clase esté exenta como pasaba con `org.slf4j`; es que
no tiene superficie que mutar.

**No viola la condición C3 del veredicto**, porque esa condición enumera las
clases que *tienen* que recibir mutantes y ésta no está en la lista.

La diferencia con el caso de los adaptadores de bitácora, que sí era un defecto:
allí la clase **tenía** código mutable y una opción de configuración lo estaba
suprimiendo. Aquí no hay nada que suprimir.
