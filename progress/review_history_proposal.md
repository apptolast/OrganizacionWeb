# Revisión de propuesta18 — root

APPROVED para normativa y destilación, sujeto a ratificación del delta UI de B. Propuesta F207B83746AF453EC1B6B09C75BB7BBFAFB735C06121D96033886CDF9C8BE478 revisada6303bb/9addbd. Cinco fuentes durables, lectura por propietario/snapshot de página, orden total e inserciones tardías explícitos. DTO existentes y ausencia de agregados evitan duplicar hechos o convertir planificación en trabajo.

El delta fija from>to en campo to/INVALID_VALUE, taskId sin projectId en taskId/INVALID_VALUE, sintaxis de cursor antes de propiedad y vínculo semántico después. Permite escenarios de precedencia sin asumir autorización desde un cursor editable. Fechas inclusivas UTC evitan reinterpretar atribución histórica; no se promete snapshot entre solicitudes ni causalidad en empates de microsegundo.

Los enlaces desde proyecto/tarea hacen el filtro utilizable sin buscar previamente una fila ni introducir UUID. El cliente comprueba filtros, forma, orden, unicidad e identidades presentes sin consultas por fila. Corrección de lectura de root: BlockResponse sí contiene projectId, confirmado9addbd; TaskHistory4 carece de contexto. La propuesta corregida no suprime ni inventa esos campos.

No se ha escrito producción18 ni se aprueban pruebas no ejecutadas. Sin estadísticas19, cambios históricos, secuencias globales ni nuevos consumidores. Después de normativa/Gherkin y revisión se mantiene autorización global del usuario para TDD, sin repetir aprobación humana.
