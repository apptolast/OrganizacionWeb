# Revisión de la estimación del MVP

**30–60 horas es defendible sólo como hipótesis provisional de planificación, no como previsión validada ni calendario fiable.**
El desglose suma correctamente 28–56 horas y el redondeo no introduce precisión aparente.
El alcance 1–18 concuerda con el roadmap; 19–30 quedan fuera de esta primera entrega, sin cancelarse.
El estado actual no permite descontar 14: sólo hay núcleo/PG nominal y cliente parcial, sin endpoints, UI ni gates integrados propios.
Las partidas reconocen margen y despliegue pendiente; no hay contradicción material entre plan y current.

Faltan supuestos concretos para sostener el rango con mayor confianza:
- Cuánto trabajo queda en 14 y cuánto tardan sus revisiones, integración y validación completa, incluidas repeticiones por fallos.
- Contratos acotados de 15–18: intervalos netos, cierre, aviso y filtros todavía no permiten estimar su coste detallado.
- Medición de esfuerzo efectivo por hito comparable; los recuentos de tests no son una medida de productividad ni avance.
- Capacidad real, acceso, dominio, HTTPS y respaldo/restauración del servidor; los 45 MiB históricos no acreditan recursos actuales.
- Definición uniforme de «horas efectivas»: horas de agente acumuladas o tiempo transcurrido con ejecución disponible, y tratamiento de esperas de CI/mutación/revisión.

El desglose sirve para ordenar trabajo y explicitar incertidumbre, pero no identifica dependencias, disponibilidad o camino crítico suficiente para convertirlo en días.
La disciplina de una feature a la vez limita el paralelismo entre 14–18; no puede dividirse el total automáticamente por el número de agentes.
Revisar el rango al cerrar el primer ciclo integrado de sesiones y al comprobar el servidor, como prevé el plan; antes no prometer fecha ni cuota suficiente.
Lectura documental `163d80`; sin suites, cambios del plan, Git, fechas inventadas ni porcentaje completado. Ponytail full y Caveman lite.
