# Contrato semanal 19: entrega del autor

Freeze documental: 34 escenarios etiquetados @s1–34 (17 simples y 17 esquemas; 100 ejemplos expandidos). Este recuento estructural no equivale a pruebas ejecutadas ni acredita implementación.

Se incorporaron las precisiones ratificadas: borradores sin GET y formulario Mostrar semana; URL/Back/recarga y Esta semana conservando zona; errores de campo 400 corregibles y límite temporal 409 recuperable; enlace existente /proyectos. El cliente verifica rango long antes de BigInt, fecha civil solicitada y pertenencia de serverNow a la semana sólo cuando date se omite. No recalcula fronteras con TZDB.

Los contextos auxiliares de @s11 y @s14 quedan explícitos: otra sesión acaba en la frontera inicial; la semana futura se consulta como segundo paso del mismo escenario. No hay nueva familia funcional. Se corrigió espacio tras palabras clave Gherkin.

Normativa conserva captura única de reloj tras establecer snapshot, intervalos efectivos en lugar de workDate, capacidad actual sin métrica de pausas y representación UTC con precisión de microsegundos sin exigir seis decimales.

SHA256 features/weekly_review.feature: 637B6BE613EF6B81623801403A922BF0E8EA26821D51950C43092C9206827375.
SHA256 project-spec.md: 3665DCD1259A70D420D2A3124231E265B42DAC9A37C4B7EBCB87278C9215AC68.

Entrega del autor lista para ratificación independiente de root; sin código, suites, cambios de estado ni Git. No se afirma validación con parser Gherkin ni resultados ejecutables.
