# Importación23: revisión previa de invariantes

Propuesta de decisiones, no contrato final ni aprobación de implementación.
Lectura exclusiva del checkout limpio OrganizacionWeb-export22-closure,
HEAD42639f73fa78201b952135330e07d928d78e029d. Se consultaron sección22,
exportador/recibos publicados, migraciones V1–V20 y writers existentes.
V14 se leyó únicamente en este checkout publicado, nunca en COMMON.
No se ejecutaron tests, mutaciones ni operaciones remotas.

## Fusión mínima conservadora

Mantener el formato cerrado de exportación22 y sus catorce colecciones.
Ausente significa insertar; existente e íntegramente igual significa no-op;
cualquier diferencia o colisión significa409 para toda la operación. No
actualizar automáticamente registros porque el archivo tenga mayor versión.
Conservar siempre las filas del destino que no están en el archivo.

La igualdad debe comparar la representación durable tipada, incluidos IDs,
versiones, timestamps, nulls, recibos, espacios y Unicode. No comparar bytes
JSON, orden de propiedades o espacio de indentación. Sí importa el orden de
visibleFields y customFields; los pares values se identifican por fieldId.
No normalizar texto con constructores de comandos, convertir longs a double,
recalcular TZDB ni completar nulls históricos. Reutilizar las validaciones
de fidelidad22 donde sea posible; las reglas de edición actual no sustituyen
al contrato de historia válida.

La unidad de conflicto es el registro durable completo. Por ejemplo, una
customization del mismo scope con otra definición o vista no admite unión
parcial de arrays:409. Un values distinto no admite completar sólo sus claves.
La fusión conserva registros omitidos, no mezcla campos de registros distintos.
Esto vuelve idempotente repetir el mismo archivo mientras el destino no cambie.
Un archivo antiguo contra datos posteriormente editados produce conflicto.

## Identidades, claves alternativas y privacidad

El Principal fija el propietario. La alternativa mínima es exigir owner del
archivo igual a esa identidad; remapear propietarios o UUID sería otro alcance.
Los UUID primarios son globales: una colisión ajena debe fallar sin revelar el
propietario ni contenido existente. No adoptar una fila ajena aunque sus otros
campos coincidan. Las referencias se validan por dueño y contexto propio.

No basta comprobar id. Restricciones publicadas relevantes:

- V10/V19: una disponibilidad/apariencia por owner.
- V20: customization por owner/scope; valores por owner/recurso, además de id.
- V11/V12: requestKey por tarea; V13 añade block/version y contexto compuesto.
- V15/V16: requestKey de inicio y cambio de sesión por owner en sus respectivas
  tablas; una sesión abierta running o paused por owner.
- V16: intervalos por sessionId/revision; V17: un CLOSE por sesión.
- V9: historial por taskId/taskVersion; FK de subtarea y contexto de tarea.

Una clave alternativa ocupada por otro registro es conflicto, no ON CONFLICT
DO NOTHING indiscriminado. Preservar los requestKeys y recibos importados es
necesario para que el replay de comandos existentes no repita una acción
histórica. No colapsar claves entre tablas que hoy tienen ámbitos distintos.

El archivo22 es autocontenido: referencias y recibos deben resolver dentro de
él. Validar además que su unión con el destino cumple restricciones vigentes.
No convertir referencias faltantes en búsquedas que filtren información ajena.
Los errores de conflicto deben evitar IDs, nombres o payloads del otro dueño.

## Preferencias, actividad e historia

Array vacío de preferencia significa ausencia en el archivo, no borrar destino
ni fabricar defaults. Conservar definiciones inactivas y sus valores, y distinguir
valor ausente de null, NUMBER0 de BOOLEANfalse. Definiciones y valores importados
mantienen versión y updatedAt; no llamar Save como una edición nueva.

Conservar proyecto/tarea terminados, intervalos, proyecciones y recibos cerrados
13/15–17, incluidas intención y resolución de offsets separadas. No reejecutar
Move/Close/Extend para reconstruirlos ni regenerar historia faltante legada.

Decisión necesaria: importación integral incluye sesiones abiertas. Insertar un
running con runningSince histórico conserva el dato; las lecturas actuales
pueden mostrar tiempo transcurrido desde ese instante. No pausarlo, cerrarlo ni
mover runningSince silenciosamente. Si ya existe otra running/paused,409 por la
restricción V16. Explicar esta consecuencia antes de confirmar es más pequeño
que inventar un modo de transformación temporal.

## Atomicidad y concurrencia

Validar el archivo sin escrituras de negocio; ejecutar comprobación del destino
e inserciones en una única transacción. Un fallo tardío, conflicto único/FK o
commit fallido revierte todo, incluidas preferencias e historia. Nada de commit
por colección. El resultado sólo confirma éxito después del commit.

La vista previa no reserva el destino. Al aplicar, repetir la comparación sobre
estado transaccional actual: no confiar en counts o hash del plan enviados por
el navegador. Un escritor intermedio puede convertir un plan válido en409.
Respuesta perdida deja resultado incierto; repetir el mismo archivo o consultar
puede acreditar estado durable, pero no atribuir causalidad a aquella petición.

No basta añadir un advisory lock usado sólo por import. Writers actuales usan
READ_COMMITTED/locks de fila, CAS y claves diferentes; customization toma
`customization:owner:scope` (PostgresCustomizationStore:54), sesiones bloquean
filas y blocks también disponibilidad. Un SERIALIZABLE sólo en import tampoco
sustituye una coordinación demostrada con todos los writers existentes.

Dos opciones a decidir antes del código: coordinación compartida adoptada por
los writers, o bloqueo transaccional de las tablas afectadas que realmente
intercepte sus INSERT/UPDATE/DELETE, con orden fijo y espera acotada. La segunda
reduce cambios de producto, pero bloquea escrituras de otras cuentas y puede
entrar en deadlock con transacciones ya iniciadas: debe abortar íntegramente,
no convertirse en bypass o espera indefinida. No impongo un mecanismo aquí;
la prueba decisiva es una carrera con writers reales y sin pérdida de cambios.

No bloquear tablas antes de recibir y validar el cuerpo completo acotado.
No introducir locks de sesión que puedan sobrevivir al rollback.

## Eventos y vista previa

El archivo excluye outbox/publicación. Importar hechos históricos no debe
republicarlos como eventos nuevos ni importar delivery status. La opción mínima
es cero outbox de importación; si se necesitara un evento nuevo de importación,
habría que justificarlo separadamente. No exige recrear los eventos originales.

Vista previa muestra counts de inserciones/iguales/conflictos, sin persistir
filas, recibos, staging durable ni preferencias. Un archivo en memoria o una
segunda subida acotada evita introducir almacén de archivos y tokens de plan.
Confirmar aplica exactamente el archivo revisado por la UI, pero el servidor
lo vuelve a validar; un hash aportado por el cliente no es autorización.

## Frontera JSON/HTTP y memoria

Reutilizar límites inclusivos22:32MiB UTF-8 sin comprimir y100000 registros
exteriores, con presupuesto también para estructura anidada y staging. Contar
bytes al leer, incluso con Content-Length ausente o falso; rechazar compresión
no soportada antes de expandir datos. No materializar primero un árbol ilimitado.
JSON cerrado, claves duplicadas rechazadas, fin de documento único, tipos exactos,
longs textuales y números personales integrales sin redondeo. Acotar profundidad
según el esquema existente, sin diseñar un parser general nuevo.

Proponer POST autenticado para preview/apply con política CSRF y Origin actual,
negociación validada antes del comando y respuestas no-store/problem+json sin
payload privado. Distinguir archivo inválido, conflicto409, exceso413 y fallo
real503; no etiquetar corrupción del destino como archivo inválido. Los códigos
y rutas definitivos corresponden a la propuesta de B, no a esta auditoría.

Abortar la UI no promete revertir un commit que ya terminó. Descartar archivo,
resumen y errores al salir/cambiar sesión; ignorar respuestas tardías de otro
contexto. No persistir archivos en storage del navegador ni ejecutar su texto
como HTML. La preview no dispara import por abrir la pantalla.

## Prioridades para cerrar la propuesta

Fijar propietario sin remapeo, igualdad durable por registro y claves alternas;
semántica de sesiones abiertas; coordinación real con writers; preview no
vinculante y recuperación incierta; límites de recepción y cero republicación.
Son decisiones derivadas del esquema/exportador actual. No se requieren modos
replace, resolución manual campo a campo, nuevos formatos o matrices adicionales.

## Mapa mínimo de reutilización para A

Raíz Java: backend/src/main/java/com/apptolast/organization/. Nombres siguientes
son clases reales; posibles puertos nuevos son propuestas, no firmas fijadas.

- domain/AppearanceValues, CustomizationView y CustomFieldLabel contienen
  reglas puras reutilizables. Atención: también normalizan. Validar y comprobar
  igualdad con lo recibido cuando el contrato exige fidelidad; no sustituir el
  dato histórico por su versión canónica de edición. Los records Appearance,
  Customization, CustomFieldDefinition/Type y SessionStart sirven como tipos
  puros donde sus campos coincidan, sin añadir JsonNode ni ResultSet al dominio.
  CustomFieldValuesCollection es un record, no un validador completo.
- adapter/persistence/ExportReceiptWriter ya comprueba estructura/contexto de
  los recibos13/15–17, snapshots, createdAt compartido, offsets y nulls legados.
  Es el punto principal para evitar una segunda implementación de esas reglas.
  Su entrada actual es JSONB durable con longs numéricos; export22 los convierte
  a strings. Por tanto NO aceptar directamente el JSON del archivo con ese
  método ni persistirlo sin adaptación. Un decoder adaptador puede convertir
  sólo los longs conocidos, exactos y acotados a su forma durable y reutilizar
  el writer con salida acotada. Si ello duplica árboles/salida o mezcla errores,
  extraer sólo el pequeño validador realmente común, manteniendo los dos lados
  de Jackson dentro de adapter. Decidirlo con el primer caso, no refactorizar
  todo22 antes de que import lo necesite.
- adapter/persistence/PostgresExportDataQueries contiene las catorce
  proyecciones SQL cerradas y validaciones por colección. Es mapa de referencia
  de campos, propiedad y nulls; no es un repositorio genérico de importación.
  Sus métodos son privados y escriben al JsonGenerator, por lo que exponerlos
  todos o invertir SQL mecánicamente acoplaría lectura y escritura. Evitar
  duplicar ese exportador para comparar destino: considerar reutilizar su
  representación canónica sólo si participa en LA MISMA transacción de import
  y mantiene el presupuesto, sin segundo snapshot ni documento entero extra.
  Si no encaja, comparación tipada específica en el adaptador import, con campos
  explícitos, es preferible a un motor de mapeo genérico de catorce tablas.
- adapter/persistence/ExportJsonWriter fija envelope/lista de colecciones y
  serialización canónica; ExportBuffer demuestra salida acotada. El buffer no
  valida cuerpos entrantes y PreparedExport no es un modelo de importación.
  Mantener read-side22 intacto salvo extracción pequeña justificada por uso real.

Puertos: seguir application/ExportDataUseCase y ExportDataQueries como patrón
de entrada/orquestación y salida transaccional. Para import basta un caso de uso
con intención explícita preview/apply y un puerto que compare/aplique atómicamente;
no catorce casos de uso por tabla. El documento validado y resumen pueden ser
records puros; si el transporte necesita InputStream, es JDK y debe tener
ownership/cierre claros. Jackson/JDBC y conversiones JSONB permanecen en adapters.
Los tipos y firmas finales los define A cuando tenga el primer bundle compilable.
No reutilizar Create/Save/Close para insertar historia: normalizan, cambian
versiones/relojes y pueden emitir outbox que aquí no corresponde.

HTTP: adapter/http/ExportDataController y ExportHeadersFilter aportan patrones
de Principal, preparación antes de responder, negociación y cabeceras tras
reset de sesión. Reutilizar política de seguridad/advice existente y parsing
exacto de custom-fields para números; no ampliar el filtro export a otra ruta
por nombre. Import es POST: oráculos reales CSRF/Origin y negociación antes de
escritura, siguiendo CustomizationController, sin un mapper global diferente.

UI: frontend/src/api-client.ts centraliza sesión/CSRF; customization-api.ts
muestra comandos y clasificación de conflictos. export-data.tsx demuestra
AbortController, identidad de operación, cleanup por owner/desmontaje, foco y
bloqueo de doble acción; export-data-api.ts muestra lectura de bytes limitada.
Su decoder valida el envelope de descarga, no la integridad completa de catorce
colecciones para autorización. Reutilizar los helpers públicos exact/instant
que ya importa de schedule-block-api, sin convertir la UI en segundo backend.
La preview del servidor es la autoridad para relaciones y conflictos.

Riesgo principal: copiar catorce mapeos más recibos en servidor y navegador da
varias definiciones divergentes del mismo formato. La alternativa mínima es
servidor autoritativo, validadores puros existentes, recibos compartidos en el
adaptador cuando sea viable y pruebas de round-trip por hechos ya disponibles;
no una capa nueva de serialización universal ni cambios preventivos al export22.
