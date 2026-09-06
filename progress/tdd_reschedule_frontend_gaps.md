# Refuerzo acotado de comportamiento frontend13

Base local d8c4129, rama codex/reschedule-completion. Init vigente 6673/bc2678: 1495 tests frontend; no se repite init. Benchmark 83148 terminado: 1416 clasificaciones idénticas y concurrency8 adoptada. Este paquete no ejecuta mutación ni modifica su configuración o los informes originales. RuntimeError171/180 siguen registrados como errores, sin reclasificación.

Propiedad exclusiva temporal de cinco archivos de tests y esta bitácora. Ponytail full/Caveman lite; seis comportamientos prioritarios, uno por ciclo. Se reutilizan fixtures, controles accesibles y peticiones observables; no se alteran fuentes para fabricar un RED.

1. BlockConfirmation: se amplía el test de reintento existente para dos GET503 consecutivos y tercer intento manual. Conserva la confirmación histórica, comprueba carga/reintento y tres peticiones al estado correcto. Primera ejecución en curso. Objetivo candidato60; ningún Killed atribuido sin replay.

1. Inicialmente GREEN93d67c, 1 caso focal; ningún cambio de producción.
2. Recuperación de movimiento: POST503, GET503 mantiene incertidumbre sin reenvío; GET404 posterior habilita sólo la decisión manual. El segundo POST conserva cuerpo, headers, key y URL exactos. Inicialmente GREEN92aa22, 1 caso focal. Candidatos183/184/186, sin atribución de Killed.

3. Se amplía el caso de consentimiento existente a un intervalo real que cruza medianoche presupuestaria UTC: primer día sin exceso y segundo con exceso. Conserva bloqueo del POST antes de consentir, retirada de revisión al editar, reinicio del consentimiento y allowOverBudget=true en el POST. Inicialmente GREENe224b2 (1 caso); sin fuentes nuevas. Candidatos703/1113/1145, aún sin replay. No cubre la ausencia total de exceso necesaria para1115/1117.

4. ETag canónica seguida de garbage en GET de estado: inicialmente GREENa336b3. Cuerpo válido y única petición efectuada; se rechaza la respuesta. Candidato258 (regex sin ancla final, comprobado5475fb), no253 (guarda de tipo); sin replay.
5. Recuperación GET por key con after estructuralmente válido pero objetivo diferente: inicialmente GREEN35bf24. Se mantiene identidad y tiempo del fixture para aislar únicamente la incoherencia del objetivo respecto a la intención retenida. Candidatos394/396/397/417 según inventario, sin afirmar cobertura de todas las variantes de identidad ni de readBlockChange542/543. No producción modificada.

6. Se amplía la integración App/TaskBlocks existente: tras confirmar el movimiento desaparece la región Mover bloque y vuelve Planificar bloque. Se conservan los oráculos anteriores de confirmación histórica y estado vigente. Inicialmente GREENbe7584. Candidato1432 (retirada de selección); no1433, que corresponde a volver a la primera página.

## Entrega para revisión independiente

Seis comportamientos ejecutados uno por ciclo, todos inicialmente GREEN. Tres amplían pruebas existentes y tres son nuevas. No se alteró ninguna fuente, configuración, script, informe de mutación ni archivo backend. `progress/current.md` figura en el diff del árbol por trabajo del coordinador; no pertenece a esta autoría.

Regresión conjunta `8ff2d0`: **203/203 tests en cinco archivos**, cero fallos u omitidos. Formato focal `d8c30d` y Prettier/ESLint `5475fb` correctos; diffcheck `54c905` correcto. Los casos focales anteriores seleccionaban uno y omitían deliberadamente los otros tests del archivo; el foco conjunto los ejecutó todos.

Trazabilidad para el futuro replay, sólo candidatos y sin adjudicar resultados: `60,183,184,186,258,394,396,397,417,703,1113,1145,1432`. Conserva sin cambios la clasificación original y RuntimeError171/180. Esta lista no pretende resolver los190 huecos del inventario ni justificar equivalencias.

Hashes SHA-256 congelados (`54c905`):

- `block-confirmation.test.tsx`: `FFDBACD6BFC05CD2FD2E0989D3C400E3F6D8EC953E8147A8E38BE73B0FF16D04`.
- `change-submit.test.tsx`: `747762538BB03A45AE06BAC2C615D4239DE4C01F072D671C3AECDF7AD179A603`.
- `reschedule-api.test.ts`: `538C4AD5B4276AD385564B0BC66DC9AA4D3BA0AB036A4055ACE112121EDC9D4D`.
- `reschedule-block.test.tsx`: `C819523559AEBDD9CD47B93FC0A6D85BD45007E7AA746F883B767BCF6FE4E0BC`.
- `task-blocks.test.tsx`: `45872A72BF05BDADCD0CF9046851C025F8B06978F9538F66F8873CC3598A5D8B`.

Sin commits ni push. Se espera revisión antes de cualquier replay o ampliación.

TypeScript final sin emisión: 337d85 EXIT0. Diffcheck final42e805 correcto.
