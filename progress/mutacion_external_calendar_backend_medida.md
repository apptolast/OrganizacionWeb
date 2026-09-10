# Mutación de backend de la feature 28 — remedida, 10 de septiembre de 2026

**93.58 %** (437/467), calculado del `mutations.xml`.

Sobre el SHA `eb55beed`, árbol limpio, máquina drenada.

## Predije que bajaría y subió. Me equivoqué, y por un buen motivo

Al sacar `ApplicationConfiguration` del ámbito —aportaba **113 de 579 mutantes**
con 112 muertos por pruebas de **otras** features— di por hecho que la puntuación
caería, porque era nota prestada. Cayó el denominador, sí, pero **subió la nota**:

| | antes | ahora |
|---|---|---|
| Puntuación | 91,71 % | **93.58 %** |
| Mutantes | 579, con 113 ajenos | 467, todos propios |
| `ApplicationConfiguration` | 113 mutantes | **0** |

Sube porque los oráculos que escribió el carril mataron supervivientes de verdad.
O sea: el cabotaje **no estaba inflando** la nota, estaba **tapando** dónde faltaba
trabajo. Que es peor, y es justo lo que denunciaba el bloqueante B4.

## Cierra B4: ninguna capa queda bajo el listón

El juez fijó **80 % en cada capa**, no sólo en el global. La capa
`adapter.feed` iba al **70,2 % (33/47)** y el global de 91,71 % lo escondía.

| Capa | antes | ahora |
|---|---|---|
| `adapter.feed` | 70,2 % (33/47) | **85.1 % (40/47)** ✅ |

El carril había previsto exactamente **40/47 = 85,1 %** con sus oráculos escritos y
verificados a mano, sin poder lanzar campaña. La campaña le da la razón al decimal.

## Todas las capas, de peor a mejor

| Capa | Puntuación | Mutantes |
|---|---|---|
| `adapter.feed` | 85.1 % ✅ | 40/47 |
| `adapter.net` | 86.4 % ✅ | 19/22 |
| `application` | 89.7 % ✅ | 78/87 |
| `domain` | 95.6 % ✅ | 174/182 |
| `adapter.http` | 96.6 % ✅ | 56/58 |
| `adapter.connectors` | 96.7 % ✅ | 29/30 |
| `adapter.persistence` | 100.0 % ✅ | 39/39 |
| `adapter.logging` | 100.0 % ✅ | 2/2 |

## Tercera confirmación del arreglo del `avoidCallsTo`

`Slf4jExternalCalendarAudit` pasa de **cero** mutantes a **2**, los
2 muertos. Es la tercera feature independiente donde se confirma que la causa
no era «una propiedad de los mutadores» sino una opción por defecto de PIT.

## Los 30 sin matar, nominalmente

| Clase | Método | Línea | Mutador | Estado |
|---|---|---|---|---|
| `AnchoredConnection` | `literal` | 48 | ConditionalsBoundaryMutator | SURVIVED |
| `AnchoredConnection` | `authority` | 57 | ConditionalsBoundaryMutator | SURVIVED |
| `AnchoredConnection` | `isAddressLiteral` | 76 | ConditionalsBoundaryMutator | SURVIVED |
| `ConnectorKeyRing` | `malformed` | 55 | NegateConditionalsMutator | SURVIVED |
| `ConnectorsGate` | `doFilterInternal` | 54 | VoidMethodCallMutator | SURVIVED |
| `ExternalCalendarController` | `single` | 163 | EmptyObjectReturnValsMutator | NO_COVERAGE |
| `HttpCalendarFeed` | `download` | 159 | NegateConditionalsMutator | SURVIVED |
| `HttpCalendarFeed` | `download` | 159 | VoidMethodCallMutator | NO_COVERAGE |
| `HttpCalendarFeed` | `read` | 202 | ConditionalsBoundaryMutator | SURVIVED |
| `HttpCalendarFeed` | `read` | 203 | NullReturnValsMutator | NO_COVERAGE |
| `HttpCalendarFeed` | `read` | 207 | NullReturnValsMutator | NO_COVERAGE |
| `HttpCalendarFeed` | `read` | 210 | NegateConditionalsMutator | SURVIVED |
| `HttpCalendarFeed` | `expired` | 218 | ConditionalsBoundaryMutator | SURVIVED |
| `IcsFeed` | `summary` | 121 | MathMutator | SURVIVED |
| `IcsFeed$Property` | `parameters` | 144 | EmptyObjectReturnValsMutator | NO_COVERAGE |
| `IcsFeed$Property` | `of` | 148 | ConditionalsBoundaryMutator | SURVIVED |
| `IcsFeed$Property` | `of` | 148 | ConditionalsBoundaryMutator | SURVIVED |
| `IcsFeed$Property` | `of` | 150 | NegateConditionalsMutator | NO_COVERAGE |
| `IcsFeed$Property` | `of` | 153 | ConditionalsBoundaryMutator | SURVIVED |
| `IcsFeed$Property` | `of` | 158 | ConditionalsBoundaryMutator | SURVIVED |
| `PublicAddressPolicy` | `unmap` | 55 | NegateConditionalsMutator | SURVIVED |
| `PublicAddressPolicy` | `unmap` | 60 | NullReturnValsMutator | NO_COVERAGE |
| `PublicAddressPolicy` | `unmap` | 63 | NullReturnValsMutator | NO_COVERAGE |
| `PublicAddressPolicy$Cidr` | `network` | 67 | NullReturnValsMutator | NO_COVERAGE |
| `PublicAddressPolicy$Cidr` | `prefixLength` | 67 | PrimitiveReturnsMutator | NO_COVERAGE |
| `PublicAddressPolicy$Cidr` | `of` | 70 | NullReturnValsMutator | SURVIVED |
| `PublicAddressPolicy$Cidr` | `contains` | 78 | BooleanTrueReturnValsMutator | NO_COVERAGE |
| `SaveExternalCalendar` | `reject` | 56 | NegateConditionalsMutator | SURVIVED |
| `SyncExternalCalendar` | `execute` | 64 | MathMutator | SURVIVED |
| `SyncSummary` | `&lt;init&gt;` | 17 | ConditionalsBoundaryMutator | SURVIVED |
