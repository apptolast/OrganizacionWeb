# Propuesta 22: exportación privada y portable

Propuesta para revisión, sin Gherkin ni implementación. Se aplica la autorización
global vigente; no hay una decisión que necesite nuevos datos del usuario.
Ponytail full y Caveman lite: un archivo JSON y una lectura transaccional,
sin conectores, trabajos en segundo plano ni dependencias nuevas.

## Alcance comprobado

El roadmap original sólo compromete «Descarga versionada de datos propios».
No contiene una aprobación específica de CSV, hojas de cálculo o ZIP. Se propone
JSON UTF-8 v1 porque conserva relaciones, tipos, notas y precisión. CSV perdería
estructura o exigiría varios archivos y convenciones adicionales; ICS pertenece
a la funcionalidad 26. Importar/restaurar corresponde a 23 y no se promete aquí.

La inspección de migraciones V1–V20 confirma proyectos, tareas y subtareas,
historial de estado de tareas, disponibilidad, planificación original y sus
proyecciones/recibos, sesiones e intervalos/recibos, apariencia y personalización.
No existe un historial independiente de todas las ediciones de proyectos:
no se fabricará a partir de outbox. Hoy, Historial y Revisión semanal son lecturas
derivadas; sus fuentes durables se incluyen una sola vez.

Puntos especialmente relevantes:

- `V20__customization.sql` guarda valores de campos inactivos que GET21 omite.
  Una exportación completa debe consultar su representación durable, con todas
  las definiciones y referencias, sin componer las páginas de la UI.
- `V12__block_changes.sql` separa reserva original, proyección y recibos.
  Exportar sólo la proyección perdería historia; exportar sólo el original
  perdería la situación actual.
- `V14__work_sessions.sql`, V16 y V18 conservan intervalos cerrados y una cola
  runningSince. No se incorpora tiempo adicional hasta el reloj de exportación.
  Los null históricos se preservan; una sesión cerrada legada no adquiere un
  cierre ni unas notas inventadas.
- `PostgresWorkSessionStore`, `WorkSessionTransitionReceipt` y los contratos
  15–17 definen recibos discriminados. Se exportan como datos tipados, nunca
  como JSONB crudo que pueda añadir campos internos accidentalmente.
- Spring Session y outbox son infraestructura, no contenido portable de la
  organización personal. Se excluyen cookies, contraseñas, CSRF y credenciales.

## Decisiones propuestas

GET `/api/v1/me/export`, autenticado y sin parámetros, devuelve un único JSON
adjunto después de completar una lectura PostgreSQL repeatable-read/read-only.
No se genera archivo persistente, evento ni trabajo asíncrono. La fecha identifica
la preparación, sin prometer que equivalga al tiempo de commit de cada fila.

Límite propuesto para revisión: 100 000 registros y 32 MiB de JSON UTF-8 sin
comprimir, inclusivos; exceso produce 413 `EXPORT_TOO_LARGE`, nunca truncamiento.
Es una frontera explícita del primer formato, no una capacidad medida ni una
promesa de que un archivo pequeño termine en un tiempo fijo. Si el uso real la
supera, el siguiente corte deberá diseñar descarga grande con snapshot estable;
no se inventa ahora paginación que mezcle momentos distintos.

Tras revisión de root, el límite exige lectura/validación por lotes o cursor y
serialización con presupuesto de memoria: no materializar primero todas las
filas. El buffer privado de salida se limita durante su construcción y se
descarta al fallar. El envío 200 empieza sólo al completar la validación.
Content-Length exacto y representación sin compresión permiten detectar cuerpos
truncados; el cliente compara bytes recibidos, valida UTF-8/JSON y nunca ofrece
un Blob parcial o vacío. La sección 22 precisa Accept, cuerpo GET, HEAD405,
Content-Disposition seguro y rechazo de formatos/codificaciones no admitidos.

La vista `/exportacion`, al final de navegación Principal, ofrece «Preparar
exportación» y, después de recibir el archivo completo, «Descargar archivo JSON».
El segundo gesto evita depender de una descarga automática tras una espera de
red. Se anuncia archivo preparado; no se afirma que el navegador lo haya guardado.
Cancelar/navegar/salir retira bytes y URL local; una respuesta tardía no descarga
ni afecta a la sesión siguiente.

La sección 22 de project-spec.md fija el esquema cerrado, orden, identidad,
precisión, límites y errores para revisión. No cambia el estado de la feature.

## Revisión que falta antes de destilar

Root debe contrastar la frontera 32 MiB/100 000 y la lista exacta de datos con el
adaptador PostgreSQL. Revisar que las representaciones de recibos mantengan
discriminadores y precisión sin copiar infraestructura. Comprobar límites de
memoria y rechazo antes de enviar 200, así como propiedad en relaciones anidadas.
Son decisiones técnicas de este contrato bajo autorización existente; no se
requiere preguntar al usuario por formatos que no solicitó ni inventar un
conector. Toda evidencia de aceptación, UX, rendimiento y mutación queda pendiente.

## Corrección de procedencia V14

La consulta inicial 874059 leyó por error de alcance el archivo V14 protegido
de COMMON, dentro de un lote de migraciones. Fue sólo lectura: no se editó,
copió ni utilizó para ejecutar pruebas. No se presenta esa lectura como evidencia
de un blob publicado ni se vuelve a consultar el archivo protegido.

El contraste documental válido posterior es 907679, mediante
`git show 5a5464c:backend/src/main/resources/db/migration/V14__work_sessions.sql`.
La descripción de sesiones iniciales se sostiene en ese blob publicado y en los
contratos existentes. La auditoría de las catorce colecciones corresponde a C
en el checkout limpio; no se atribuye al WIP protegido de COMMON.

## Precisión del recibo histórico de replanificación

La auditoría independiente de C confirma las catorce colecciones y detecta una
ambigüedad entre el DTO HTTP de 13 y su recibo durable. Se corrige en sección 22:
BlockChangeReceipt conserva version, siete campos; before/after son PlannedBlock
de seis campos, con request de siete y time de cinco. Los offsets opcionales de
intención y los offsets resueltos siguen separados, junto con locales, zona y
allowOverBudget. after es null sólo para CANCELLED; no se reutiliza el DTO público
revision/BlockResponse de nueve campos que perdería datos históricos.

Fuentes contrastadas por lectura en el checkout limpio
`work/OrganizacionWeb-export-data`, herramienta 00c0ff: BlockChangeReceipt.java,
PlannedBlock.java, BlockRequest.java y ResolvedBlockTime.java. No se vuelve a leer
V14 ni otro archivo protegido de COMMON. Esta precisión modifica sólo el esquema
documental del export, sin producción, pruebas ni implementación de importación.

## Decisión del coordinador

Contrato técnico aprobado para destilación bajo la autorización global del usuario. Root revisó el alcance y los límites; C contrastó las catorce colecciones contra el checkout publicado5a5464c. Se corrigió la única ambigüedad detectada: recibo histórico de bloque, manteniendo request y time con ambos pares de offsets. Root verificó que la sección22 es la única adición a project-spec (263 líneas antes de aprobación), sin cambiar contratos anteriores. Memoria acotada y contrato de transferencia completo quedan explícitos. No hay implementación ni pruebas22 todavía.
