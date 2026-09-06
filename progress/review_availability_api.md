# Revisión independiente del cliente availability

Veredicto: APPROVED para `availability-api.ts` y sus pruebas. Esta revisión no cierra la interfaz, el backend ni la feature 10; tampoco autoriza todavía su mutación conjunta.

El coordinador leyó el contrato aprobado, ambos archivos completos y `api-client.ts`. Reejecución independiente: `pnpm exec vitest run src/availability-api.test.ts`, salida b75bd2, EXIT 0, 147/147 pruebas el 6 de septiembre a las 06:14:11 de Madrid. La ejecución focal tarda 1,82 segundos y no sustituye la futura regresión global del código congelado.

Las lecturas distinguen ausencia confirmada de error. Exigen HTTP 200, estructura cerrada, siete enteros dentro de límites, fecha UTC válida y ETag coherente con configured. La versión BIGINT se valida sin convertirla a Number. Se conserva la zona histórica textual; la pertenencia actual pertenece al catálogo y al guardado del servidor. El catálogo conserva aliases, exige orden estricto, unicidad y UTC, sin usar Intl como autoridad.

El guardado copia la intención antes de esperar y confirma únicamente un snapshot configurado válido con la misma zona y los siete valores enviados. Una mutación posterior del objeto del caller no cambia la comparación. HTTP inesperado, JSON inválido y fallo de red propagan error sin repetir escrituras. Los tres endpoints transmiten AbortSignal y reutilizan el cliente común de sesión/CSRF; las pruebas distinguen 401 vigente de respuesta antigua cancelada.

Ponytail aplicado: sin dependencias nuevas, almacenamiento en navegador, reintentos automáticos ni abstracción de catálogo cliente. Los predicados privados validan esta frontera sin acoplarla a módulos de tareas. La revisión no identifica un hallazgo de producción. La mutación posterior podrá detectar huecos adicionales en las aserciones y deberá revisarlos antes del cierre.

## Revisión independiente de mutantes originales

El coordinador leyó `mutation-original.json` y contrastó fuente y reemplazos: availability-api contiene 237 Killed y siete Survived. Los resultados siguientes corresponden al original, no a una nueva campaña.

| ID | Dictamen y evidencia lógica |
| --- | --- |
| 31 | Hueco real de pruebas: eliminar configured===true permite aceptar configured=false acompañado de todos los demás datos y ETag configurados válidos. Se solicita al autor el caso adversarial y replay; la producción original conserva la guarda correcta. |
| 80 | Equivalente en esta frontera JSON: retirar typeof object de isRecord no permite una respuesta válida; null y arrays siguen excluidos y los primitivos restantes no satisfacen los campos nombrados y sus validaciones posteriores. No hay funciones ni prototipos personalizados provenientes de JSON. |
| 103 | Equivalente: Number.isInteger ya rechaza valores cuyo tipo no es number. |
| 148 | Equivalente en el resultado público de rechazo: sin typeof string, un valor JSON no textual no completa la comparación de fecha y replace; aunque una coerción pasase la expresión regular, la operación posterior falla. El contrato no diferencia el tipo interno del error. |
| 151 | Equivalente: retirar el ancla inicial no permite aceptar prefijos; Date.parse debe funcionar y la comparación posterior exige el texto canónico completo, sin prefijo. |
| 152 | Equivalente: retirar el ancla final no permite aceptar sufijos; la comparación canónica posterior conserva y rechaza ese contenido adicional. |
| 174 | Equivalente: la validación previa exige el único Z al final; retirarle el ancla a replace no cambia el segmento reemplazado para ninguna entrada que alcance esa rama. |

Las seis equivalencias no se presentan como mutantes detectados ni alteran el score original. El caso 31 sigue pendiente de su evidencia de refuerzo y replay antes del cierre.

Refuerzo posterior revisado: API 148/148 y ESLint verdes según bitácora del autor. El coordinador leyó `api-replay.json`: 3/3 Killed, y la variante original 31 corresponde al ID 0 por expresión y ubicación; killedBy 569 identifica el nuevo caso contradictorio. Producción conservada con SHA256 idéntico antes/después, registrado en `tdd_availability_api.md`. Se cierra ese hueco de pruebas; no se recalcula retrospectivamente el score original.
