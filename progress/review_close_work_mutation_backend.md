# PIT16: medición y revisión independiente de residuos

**Gate numérico PASS: 516/520 = 99,23076923076923 %, umbral80 conservado.** XML completo:516 KILLED,2 SURVIVED y2 NO_COVERAGE. TIMED_OUT, NON_VIABLE, MEMORY_ERROR, RUN_ERROR, STARTED y NOT_STARTED:0. El denominador incluye los cuatro residuos; ninguno se reclasifica como detectado. Lectura final5c6780. El log declara BUILD SUCCESSFUL en35m28s (PIT35m21s, análisis32m44s, cobertura2m36s). El EXIT de la sesión30914 pertenece a root: no fue accesible desde este agente y no se inventa.

Original: `backend/build/reports/pitest-close-work-session/mutations.xml`. Copia preservada: `progress/close_work_pit_final/mutations.xml`. Ambos SHA256 **9CF3C8A44FB24E8EC1991BFDAE532D7E42B13A8D778D23CE663057E44787ED75**. Los321 paths del manifest previo se volvieron a calcular en `close_work_pit_after_hashes.json`: cero cambios. Inventario literal de residuos en `close_work_pit_residuals.json`. Informes15 intactos; sin otra campaña ni edición de código/configuración.

Scope leído:15 patrones de clases de16 y dependencias compartidas, incluidos núcleo, Notes/State/Receipt/Closure/Closed/Transition, lecturas, Store, Controller, Outbox/Rabbit y wiring. Selección JUnit completa del paquete, cuatro workers y umbral80. El log informa313 tests examinados,3322 ejecuciones y782/792 líneas cubiertas. Esos recuentos no se presentan como tests nuevos ni cobertura contractual total.

## Cuatro residuos

1. **SURVIVED — OutboxMessage.validationCode:173**, ConditionalsBoundaryMutator, índice725/bloque115. Cambia `LocalDate.parse(date).getYear() < 1` por `<= 1`: rechaza un workDate válido del año0001. Es un hueco contractual de prueba, no equivalencia ni fallo de la fuente actual. Las pruebas positivas alcanzan9999 y las negativas0000/10000, pero falta esta frontera positiva. Propuesta mínima enviada a root: un evento CLOSE válido con workDate0001 que se publique; inicialmente puede ser GREEN. No se ha escrito ni ejecutado aún y no se atribuye detección futura.
2. **SURVIVED — WorkSessionStateController.note:248**, EmptyObjectReturnValsMutator, índice17/bloque4. Cambia el retorno null de nota ausente/JSONnull por cadena vacía. Redundancia contextual demostrable: el método privado sólo alimenta inmediatamente WorkSessionCloseNotes, cuyo constructor normaliza null a cadena vacía antes del puerto/intención. Ambas variantes conservan exactamente la misma nota, validación y replay. No exige un test que contradiga la normalización contractual; el estado medido permanece SURVIVED.
3. **NO_COVERAGE — PostgresWorkSessionStore.lambda$commit$0:136**, NullReturnValsMutator, índice37/bloque3: retorno exterior exitoso de recuperación tras colisión.
4. **NO_COVERAGE — PostgresWorkSessionStore.lambda$commit$2:142**, NullReturnValsMutator, índice32/bloque5: confirmación replay dentro de la misma recuperación tardía.

Los dos NC corresponden al éxito con intención idéntica después del rollback por colisión owner/key. Una petición normal de la misma sesión adquiere FOR UPDATE antes del lookup: la segunda observa el recibo ya confirmado y no alcanza naturalmente el INSERT perdedor. La colisión controlada del test16 alcanza UNIQUE real, verifica dos transacciones y rollback, pero termina en conflicto de intención entre A cerrada/B abierta. Ausencia de ganador, conflicto y replay normal/misma-key concurrente están cubiertos. No se fuerza una pareja imposible de sesiones abiertas ni se declara equivalencia universal de estos retornos; el riesgo residual y ambos NC se conservan explícitos.

Conclusión: campaña válida y puerta superada sobre fuentes idénticas. Recomiendo únicamente el oráculo positivo de año0001 antes del cierre documental; root decide su ejecución y cualquier replay focal. Ningún motivo observado para repetir PIT completo o perseguir el100 %.

Confirmación posterior de root: sesión30914 terminó EXIT0, evidencia ce3127. No se repitió la campaña.

Seguimiento autorizado, separado de la medición original: el único refuerzo de año0001 pasó192 pruebas del publicador y luego el replay clase/boundary terminó EXIT0 e01e1a,7/7 KILLED. La firma original173/725/115 fue detectada por el nuevo test, comprobada en5a44fd. El XML520 y sus estados originales siguen intactos. Evidencia completa en `tdd_close_publisher_year_one.md`; no se proyecta el porcentaje focal sobre el gate completo ni se reclasifican HTTP/NC.
