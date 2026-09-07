# Revisión de contrato15 — pausa y reanudación

**Veredicto: APPROVED para destilación y posterior TDD**, sujeto a la autorización de fase del coordinador. No aprueba implementación, pruebas ejecutadas, mutación ni despliegue.

Corte congelado por autor: `features/pause_resume_session.feature`, SHA256 `15B7F783EB709F4BC0A4CD15E877BB74C095052548B409F2AB349E41AB1BBEBB`, comprobado45a798.39escenarios/@s1–@s39,105casos de contrato según inventario del autor; no son105pruebas ejecutadas. Contraste con sección15 finaleb1a9bb (`project-spec.md` SHAA3237BEA747529D47146CAE5F38DDB3C85BF0B009A5D189AF3249113CEF62A6E), continuidad14 y propuesta/planUX15.

## Contraste y límites

- @s1–9: recibo14 inmutable, lectura sin materialización, acumuladoµs y pausas excluidas, reloj/medianoche/DST, completed permitido y paused ocupa plaza. Coherente con alcance15 sin cierre/aviso/historial16–18.
- @s10–18: Work-Session-Revision propio sin comillas, identidad y revisión completas,428/400/412 y precedencias, problemas/queries heredados, replay antes de negocio y separación de keys. No exige ETag fuerte sobre snapshot variable.
- @s19–26: concurrencia por sesión, independencia de propietarios, rollback de escrituras/commit, snapshot previo coherente,503 de lectura/finalización, recibos tras reinicio y publicación durable. Dos sesiones no cerradas del mismo propietario no se fabrican: límite explícito aceptado por root; sesión sucesiva pertenece a16.
- @s27–35: DTO/state/snapshot/recibo cerrados, neto y revisión exactos superiores aNumber, acciones sólo desde state, consulta posterior distinta de confirmación, incertidumbre/reenvío manual/CSRF y salida sin revocación. Semántica de cabecera y Location heredada/referenciada, sin repetir matriz14 completa.
- @s36–39: privacidad por etapa real, foco/feedback,30criterios y límites físicos; GET antiguo del mismo contexto no revierte una pausa confirmada ni restaura la acción anterior. Se conservan tokens internos, lenguaje de estado/actualización y fin previsto fijo.

## Ajustes cerrados antes del dictamen

1. @s10: eliminadas las dos filas duplicadas, sin quitar familias de validación.
2. @s12: código heredado corregido a MALFORMED_JSON.
3. @s39: secuencia GETviejo→POSTconfirmado→GETactual→GETviejo tardío, complementaria a cambio de ruta de@s36.
4. @s36:401 diferido sólo antes de entregar Response; JSON diferido corresponde a200 y clasificación a error no401. Se verificó `api-client.ts`: el observer401 se ejecuta al recibir Response, por lo que la precisión evita un oráculo temporal imposible.

Sin hallazgos bloqueantes residuales en este corte. Revisión sólo documental/read-only: no suites, nueva implementación, Gherkin editado por el juez ni modificación de metadata. Se reutilizan contratos comunes sin contar sus escenarios como ejecuciones nuevas; TDD individual seguirá siendo exigible por cada caso que se implemente.
