# Revisión independiente del contrato 14

**Dictamen: APPROVED para destilación contractual, sujeto a la aprobación final de root antes de TDD.** No se encontraron contradicciones bloqueantes frente a la sección 14 de `project-spec.md` y `review_start_work_spec.md`. Esta revisión no aprueba implementación, cobertura ejecutada, mutación ni despliegue.

Se leyó el contrato completo: 42 escenarios y 123 casos, contados desde sus escenarios simples y filas de Examples. No se ejecutaron suites ni se modificaron fuentes, tests, contrato o metadatos. Aplicados Ponytail full y Caveman lite.

## Comprobaciones materiales

- **Duración y precisión:** cabecera y @s1–6, @s13 y @s30 conservan entrada explícita 1–1440, captura única truncada a microsegundos y suma de segundos reales. El rechazo de una desviación de un microsegundo evita reducir el contrato cliente a milisegundos. Los cambios DST no alteran la duración; reloj/fin fuera de rango tienen conflicto propio, no 503 ni error del campo editable.
- **Precedencia y acceso:** cabecera y @s7–15 mantienen seguridad, query, IDs/key, estructura y contexto antes de replay; replay antes de activa, completed y nueva captura. Las referencias a 11 @s19/@s23/@s62 existen. La herencia de @s23 se limita expresamente a las reglas citadas: 14 excluye Availability-Revision e If-Match, por lo que no importa su 428.
- **Unicidad por propietario:** @s15–18 distinguen intención normalizada, espacio independiente de 11/13, misma key frente a distinta intención y competición entre proyectos sin disponibilidad. La activa de otra tarea es válida en GET A y no se confunde con confirmación del POST abierto (@s23, @s29, @s42). La resolución tras transacción abortada y las restricciones PostgreSQL quedan normadas en la especificación; no se necesita convertir esos mecanismos en escenarios adicionales.
- **Atomicidad y recuperación:** @s19–25 conservan ambos órdenes de completed, supresión de escrituras, error de finalización y ausencia sólo comprobada. El recibo permanece después de reiniciar API y retirar outbox. La finalización transaccional de @s20/@s24 no equivale al cierre de trabajo, excluido de 14. La lectura no materializa ni escribe; el comando no altera planificación ni estados.
- **Evento:** @s26–27 conservan los once campos, identidad independiente, instante original, novena ruta y ocho rutas anteriores. Zona histórica no exige catálogo; confirm incierto permite redelivery y no promete entrega exactamente una vez.
- **Interfaz y privacidad:** @s28–42 separan carga, ausencia, error, activa y recibo confirmado. POST incompatible e idempotency conflict conservan intención; GET 404 sólo permite decisión manual con la misma key. CSRF renovado no reenvía automáticamente. Las guardas después de await y el 401 antes de entregar Response evitan una ventana de prueba artificial. El foco sólo se recupera si quedó sin destino, sin robarlo a otro control.
- **Límites 15–18:** cabecera, @s28, @s36–37 y especificación excluyen pausa/cierre, ampliación, aviso, neto e historial global. Llegar al fin previsto no termina ni acredita trabajo. La habilitación habitual queda pendiente del ciclo 14–16. @s41 exige evidencia delimitada de UX, sin declarar dispositivos o estudios no realizados.

No se solicita ampliar combinaciones ni añadir infraestructura. Durante TDD se deberán materializar los mecanismos normativos de persistencia y las referencias heredadas en pruebas relevantes; este dictamen no los considera ya probados.

## Corte leído

SHA256:

- `project-spec.md`: `379F98742CFA471B85812529A764A5A6B67387535B0D83B6BBCCF7FB01AA3579`.
- `features/start_work_session.feature`: `BA10E213B555E650F999E230B934317E7CD33D6CBD3F746B00440F66EB7CABB1`.
- `progress/review_start_work_spec.md`: `D2F663429C57251F97D33DEF167D5EE5FAD3678E97FF82D5E016A036FE8CE2B2`.

Lecturas principales: `054bdb`, `673bc7`; referencias, hashes y conteo: `b24911`, `b62905`.
