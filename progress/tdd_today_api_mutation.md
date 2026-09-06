# Seguimiento TDD — API Hoy

Rol autor acotado a today-api.test.ts y esta bitácora sobre checkpoint5fe9afc, init15059verde. Ponytail full/Caveman lite; un comportamiento por ciclo, sin replay ni mutación manual. Producción API sólo ante defecto real comunicado primero; UI/config/tests ajenos quietos por esta autoría. Original521mutantes e inventario103 preservados; objetivoAPI35, sin excluir denominador.

1. Fecha array coercible JSON, único defecto sobre snapshot vacío válido (@s16/@s17, objetivo127). Prueba añadida; primer foco pendiente. No se atribuye RED de mutante aún no medido.

1 final: inicialmenteGREEN df71bd,1caso; producción intacta.
2. Fallback: diez vectores (dos fuentes × zoneId/budgetMinutes/remainingSeconds/excessSeconds/availabilityZoneId), cada uno parte de fallback válido y cambia un único campo. InicialmenteGREEN66a5c3,10casos; objetivos171–178/181/183/185/192, no cambio productivo.
3. IDs: duplicado noadyacente en intervalos9–10/10–11/11–12, resúmenes10800s/3600exceso correctos; misma grafía y mezcla de mayúsculas. InicialmenteGREENabe596,2casos; objetivos224/225NoCoverage/226. El rechazo ya no puede confundirse con orden de dos filas idénticas.
4. Orden válido: dos reservas separadas, orden temporal creciente y UUIDhex lexicográfico inverso/case mixto. InicialmenteGREEN746128,1caso; objetivos212/213/214, más protección contra ordenar sólo porUUID.
5. Empate manipulado negativo: root autoriza expresamente probar JSON con inicios empatados y UUIDcanónicos descendentes; se documenta que también solapan y jamás se presenta como agenda válida de servidor ni como matriz de defecto único. Resúmenes/candidatos consistentes con filas evitan rechazo espurio por esos campos. InicialmenteGREEN5ed4e7,1caso; objetivos211/217/218 y otros que debiliten guard de desempate. No se fabrica un positivo con solape para matar mutantes más estrictos.
6. Intersección cero: dos extremos (fin==dayStart e inicio==dayEnd), planned0/restante7200 y closing/next coherentes con filas. El único defecto de pertenencia es tocar sin intersectar. InicialmenteGREENbdd097,2casos; objetivos232/233. No producción adicional ni RED fingido; mutantes no ejecutados aún.

Total nuevo17casos, suiteAPI88 antes de formato/regresión final. Todos los ciclos fueron inicialmente verdes frente al programa vigente; no se atribuye ninguna muerte hasta replay independiente aprobado.

## Equivalencias propuestas al judge, individuales y sin descuento

- ID78 (`typeof zoneSource === "string"`→true): el includes inmediatamente siguiente acepta sólo uno de los tres strings por identidad estricta. Ningún otro tipoJSON puede satisfacerlo. Quitar esta comprobación no amplía aceptación ni cambia el canal de error.
- ID85 (guard inicial budget entero/null→true): AVAILABILITY vuelve a exigir integer(0,1440); ambas fuentes fallback exigen null. Esas ramas agotan zoneSource, validado antes. Mismo resultado para cualquier valor JSON.
- ID89 (guard inicial remaining entero/null→true): AVAILABILITY exige igualdad estricta con max(0,budget*60-planned), número entero no negativo dentro de límite seguro por validación previa; fallback exige null. Quitar guard inicial no acepta string, fracción, negativo u objeto.
- ID93 (guard inicial excess entero/null→true): misma exhaustividad, con max(0,planned-budget*60); planned es entero seguro y presupuesto0..1440, por lo que el valor esperado es entero no negativo seguro. Fallback exige null.
- ID216 (comparación UUID en empate >=→>): sólo cambia igualdad canónica. Ambas filas pasan al bucle Set, que rechaza segundo ID canónicamente igual. Para claves distintas > y >= coinciden. No depende de inventar un empate válido.
- ID222 (normalización del Set toLowerCase→toUpperCase): todos los IDs fueron validados UUIDhexASCII. Convertir ambos elementos a cualquiera de las dos cajas preserva exactamente las clases de igualdad. No modifica elID devuelto ni el orden, que usa otra expresión.
- ID253 (comparador de mayorfin→true): **sólo contextual** a reservas válidas sin solapes y ordenadas porinicio. En ese dominio, fines también aumentan y el máximo es el último. JSON arbitrario con solapes/contenimiento no cumple esta precondición; no se afirma equivalencia universal ni se fabrica un positivo solapado para matar elmutante.
- ID259 (>fin→>=fin): **sólo contextual** a la misma prohibición de solapes. Dos reservas positivas válidas no compartenfin. Además wholeInstant permite representaciones Z/.0Z: en JSON solapado, igual instante no asegura igual string y elegir el último podría alterar closingAt. Se deja esta limitación explícita al judge; no se descuenta del denominador.

Otros candidatos de empate más estricto (209/215/219) se remiten al judge contextual: no existe agenda válida con inicios empatados por prohibición de solapes. Los tests negativos sí pueden rechazar JSON manipulado con ties; no deben presentarlo como agenda válida para convertir rechazos más estrictos en defectos.

## Entrega para revisión

Freeze de today-api.test.ts; producción today-api.ts intacta. Seis comportamientos añadidos,17casos. Formato únicamente este archivo; suiteAPI completa **88/88 GREEN b85355**, ESLint focal también EXIT0; **tsc --noEmit y diff --check EXIT0 288a83**. Los skips de comandos -t anteriores son filtrado focal, no omitidos en la regresión final.

Objetivos observables cubiertos para medición posterior:127,171–178,181,183,185,192,211–214,217–218,224,225,226,232,233; losIDs demás mantienen análisis contextual anterior. La cobertura objetivo no es un resultado de mutación: no ejecuté replay, no atribuí kills ni excluí supervivientes. Revisor independiente debe aprobar tests y argumentos antes de campaña focal posterior. No cambios ajenos, commits ni done.
