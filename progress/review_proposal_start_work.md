# Revisión de la propuesta de inicio de trabajo

El coordinador acepta las decisiones de producto propuestas para la futura
destilación de feature14: duración relativa elegida explícitamente, fin previsto
fijo, tarea como contexto y zona histórica con fallback UTC. Evitan repetir la
resolución de una hora civil ambigua y mantienen separadas planificación y
actividad real. Una sola sesión activa por propietario y recibo durable son
requisitos de integridad, no personalización futura.

La representación del inicio permanece inmutable. La consulta de sesión activa
debe evolucionar explícitamente en15 sin reinterpretar el recibo como estado
actual. El orden de locks deberá concretarse contra las transiciones existentes,
sin mutex global entre propietarios ni nuevas tablas redundantes por anticipación.

La habilitación para uso habitual se revisará con el ciclo completo de inicio,
pausa y cierre (14–16). No se desplegará14 sola como temporizador utilizable sin
salida, ni se inventará cierre automático o reset administrativo para suplir16.
El aviso deliberado al alcanzar el fin previsto sigue reservado a17.

Este dictamen permite preparar el contrato cuando cierre13. No convierte el
documento en Gherkin aprobado, no cambia estados y no autoriza implementación
simultánea de otra feature. La aprobación global del usuario permanece vigente;
no es necesaria otra confirmación humana para estas decisiones coherentes con
su alcance.
