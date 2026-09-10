# Puertas de mutación de las cinco features — 10 de septiembre de 2026

Todas las cifras están **medidas**, no declaradas, y salen del `mutations.xml` o
del `mutation.json` de cada campaña. Umbral del proyecto: **80 %**
(`harness.config.json`).

## Backend (PIT)

| Feature | Puntuación | Muertos / total | Estado |
|---|---|---|---|
| 30 automatizaciones | **96,00 %** | 581 / 607 | ✅ |
| 28 calendario externo | **91,71 %** | 531 / 579 | ✅ |
| 25 webhooks | **89,88 %** | 382 / 425 | ✅ |
| 27 conector GitHub | midiéndose | | |
| 29 conectores adicionales | pendiente | | |

### Las clases que los jueces exigían, verificadas una a una

No basta el porcentaje: un ámbito mal apuntado da buena puntuación sin mutar la
clase que importa. Por eso cada campaña se comprueba clase a clase.

**Feature 30** — `ExecuteAutomations` 48/51 (el juez pedía ≥ 90 %),
`ApplicationConfiguration` 112/113, `PostgresAutomationWork` 33/38,
`AutomationCursor` 8/9, `AutomationSchedule` 1/1, `AutomationConfiguration` 0/1
→ su único superviviente se mató después, y era el que dejaba el motor sin
arrancar. `Slf4jAutomationAudit` no recibe mutantes, y está comprobado que
**ningún adaptador de bitácora los recibe en ninguna campaña del repositorio**:
es una propiedad de los mutadores, no un hueco.

**Feature 28** — `HttpCalendarFeed` 33/47, `AesGcmSecretCipher` 16/17,
`ConnectorKeyRing` 12/13, `AnchoredConnection` 18/21,
`PostgresExternalCalendarStore` 37/39, `ExternalCalendarController` 42/45,
`ConnectorsGate` 12/13. Las siete reciben mutantes.

**Feature 25** — `AnchoredConnection` 18/21. Esa clase recibía **cero** mutantes
hasta esta mañana porque no estaba en ningún ámbito.

## Frontend (Stryker)

| Feature | Puntuación | Vivos | Sin cobertura | Estado |
|---|---|---|---|---|
| 27 conector GitHub | **85,06 %** | 88 | **0** | ✅ |
| 25 webhooks | 71,40 % | 151 | 22 | carril trabajando |
| 30 automatizaciones | 52,74 % | 337 | 77 | carril trabajando |
| 28 calendario externo | pendiente | | | |
| 29 conectores adicionales | midiéndose | | | |

**Feature 27, las tres condiciones del juez, no sólo el porcentaje**: 589
mutantes generados, ningún módulo a cero, y los 13 mutantes del escuchador de
Escape (`github-connector.tsx:149-159`) **todos muertos**. Cero mutantes sin
cobertura en los tres módulos: no queda una sola rama del producto sin ejercer.

## Lo que estas cifras enseñan

**El backend está sano y el frontend no**, y no es casualidad: el backend lleva
meses pasando campañas y el frontend de estas features **nunca había pasado
ninguna**. La única que aprueba —la 27, con 85,06 %— es precisamente la que ya
había tenido dos rondas de trabajo sobre supervivientes: venía de 69,44 %.

## Cuatro ámbitos incompletos, encontrados en una sola noche

Todos con el mismo efecto: puntuación estupenda **sin mutar la clase que
importaba**, y sin avisar.

1. `adapter.crypto.*` — paquete borrado; el AES-256-GCM sin un solo mutante.
2. `ConnectorStatusSource*` — sólo la interfaz; las seis implementaciones fuera.
3. `ImportGithubIssues*` — clase borrada, dejando `ImportIssues` —que hace todas
   las importaciones del producto— sin mutantes en **ninguna** campaña.
4. `AnchoredConnection` — el anclaje del reenlace DNS, en ningún ámbito.

Queda `scratchpad/patrones-muertos.mjs`, que barre los dos árboles de fuentes y
exige que todo patrón resuelva a una clase real: **441 patrones, cero muertos**.
Va antes de cada campaña.
