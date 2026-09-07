# Revisión independiente HTTP20

Veredicto: **APPROVED para el paquete HTTP**, con los límites de integración indicados abajo. No se han encontrado defectos P1/P2 accionables en las dos fuentes revisadas. No es el cierre completo de la feature20.

Lectura independiente de AppearanceController.java, AppearanceApiTest.java, sección20 normativa, bitácora y manifiesto. Se contrastaron seis hashes: todos coinciden (`ef4b95`). XML preservados: 55 Appearance y 151 Availability, 206 en total; cero fallos, errores u omisiones. Se acredita la ejecución del autor EXIT0 `0a6e89`; esta revisión no ejecutó suites ni modificó producción/tests.

## Comprobaciones

- Controller líneas46–73: GET deriva owner sólo de Principal; query prohibida precede lectura. Cuerpo cerrado de cinco campos, defaults sólo para Optional.empty. DTO y ETag configurados proceden del mismo objeto, sin exponer owner/UUID/versión en JSON. Los oráculos s1/s2 comprueban forma y campos; versión máxima permanece en cabecera como decimal y timestamp conserva microsegundos.
- PUT líneas85–121: query antes de If-Match, después parser JSON estricto, forma, extras ordenados y validación completa de theme/light/dark. Se reutilizan validadores reales del dominio; no se duplica la fórmula de contraste. Casos mixtos de s10/s11 distinguen theme inválido antes de light null y contraste light antes de tipo dark. Todos verifican ausencia de interacción con los puertos ante rechazo.
- Precondición líneas131–147: un único valor, fuerte, UUID minúsculo de forma canónica y decimal sin signo/ceros iniciales, máximo Long. Se preservan valores sin el recorte observado con List<String>; pruebas de espacios, repetición idéntica, lista, recurso ajeno y overflow. Ausencia428 precede cuerpo inválido. La identidad/revisión completa llega al caso de uso; conflicto412 tiene handler local y no filtra datos.
- JSON: raíz null/array/escalar produce INVALID_TYPE; ausencia/vacío/malformado, duplicados y tokens concatenados producen MALFORMED_JSON mediante handler local o ApiErrors existente. Desconocidos se ordenan antes de valores requeridos.
- Seguridad real del slice y configuración compartida: autenticación, CSRF, origen y415 preceden al handler; GET nominal no necesita CSRF. No se añade ruta para consultar otro owner. Cache-Control no-store comprobado en defaults, PUT y fallos; DTO PUT no emite Location.
- Fallos: ApiErrors traduce StorageUnavailableException a503 sin causa privada ni ETag/defaults. La corrupción de fila se valida en PostgresAppearanceStore y se envuelve antes de retornar al puerto; el controller no necesita deserializar o revalidar un record arbitrario. La prueba HTTP acredita la traducción del fallo, no simula una corrupción SQL como si fuera PG real.

## Límites y evidencia compuesta

Los puertos read/save están simulados en HTTP. La exclusión por owner, commit, no-op, reloj, reinicio, corrupción durable y migración pertenecen al paquete core/PG de A; no se atribuyen a estos 55 casos. La lectura puntual del mapper confirma la cadena corrupción→StorageUnavailable→503, pero esta revisión no vuelve a ejecutar los tests PG ni reemplaza su revisión independiente. Seguridad, negociación y serialización sí atraviesan Spring MVC real del slice. Navegador, E2E, mutación y gate integrado siguen separados.

Hashes de fuentes aprobadas:

- AppearanceController.java: `581AB8FB1913E8C6E381EA55ABC5F24FFC0E16291AB973C21B90AEB33EC81670`.
- AppearanceApiTest.java: `D9B19CB450C8CABD1F3FD90323B2216C40A03BF2F6E499B9503C3B354827DEC9`.

Ponytail full: DTO explícito y validadores compartidos evitan duplicación de dominio. Caveman lite: alcance y límites se conservan sin exigir matrices repetidas de disponibilidad ni nuevos oráculos por formalismo.
