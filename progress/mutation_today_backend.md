# Mutación backend — today

**Veredicto final: PASS. Score bruto80/80 =100 %; umbral80 %.** Mutation tester independiente de la autoría backend; no se editan fuentes/tests. Ponytail full/Caveman lite. Revisión backend APPROVED en review_today_backend.md y soporte aprobado en judge_today.md. Init35422 EXIT0/292711 previo a esta medición,1415 backend y1317 frontend; no se repite init ni campaña histórica.

## Corte y ejecución

- Checkpoint backend: `5cc80a99e3951a755e0e1331622113ed29e2e2c2` (HEAD comprobado4e6171). Frontend sigue en desarrollo/review independiente; no es target de esta campaña.
- Comando exacto: `node .harness/harness.mjs mutate today-backend`, lanzado557c74, sesión2316. El arnés llama scripts/project.mjs, target cerrado `pitest -PmutationScope=today`. El soporte scripts revisado aún está sin commit, SHA256 FC4B899640835EC66203EF876B159FC48295C443EEDA7A8F3467FAF39AEDC31A.
- Gradle scope SHA256 C4E700C780DCFC0CE057B5C4582EEE78CF40801E5B60DE67E7A3A1CB8B6973F5. Clases: TodayWindow*,TodayItem,ReadToday,TodayController,PostgresTodayQueries,ApplicationConfiguration. Cuatro suites: TodayWindowTest,TodayApiTest,ApplicationWiringTest,ProjectStateConfigurationTest. PIT informa7 unidades y4 clases de prueba al arrancar.
- Umbral bruto80,threads4,HTML/XML en backend/build/reports/pitest-today, FRECORD desactivado y exclusiones heredadas equals/hashCode/toString intactas. No se excluyen sobrevivientes para alterar el resultado.
- Hashes de producción observados: TodayWindow19597FD5F362B54BCE8BF9FD097A81DFE84F3BEE15C12216DFAD74F2B266292E; ReadTodayD32D0BD5DB6CFA725DA7E52B630064E800F142D8059457B01C1289A716789D65; PostgresTodayQueries1A77B524A9E168886A1D8294961B39652A0F48FACA1DC6EC769DF3168A50F313.

No se leerá XML parcial para presentar resultados definitivos; se adjuntará el inventario completo al terminar, incluyendo estado, línea, método, mutador e índice de cada superviviente/no cubierto. Fallos de herramienta y timeouts se distinguirán de Killed.

## Resultado final verificado

Arnés EXIT0, sesión2316 terminada56f8fa. PIT finalizó en50s (11s cobertura,38s análisis), Gradle BUILD SUCCESSFUL58s. Generados80 mutantes:80 KILLED,0 SURVIVED,0 NO_COVERAGE,0 TIMED_OUT,0 errores. Cobertura de líneas del scope136/136;352 ejecuciones de tests durante análisis. Estos datos pertenecen únicamente a la campaña today-backend de este corte.

XML final leído9469b5/9edace después del cierre, timestamp2026-09-06T14:57:17.5238003+02:00. SHA256 F569451961A2F66F34012093130DCCA2DA7196A20514FD190D5BA9EFAF0A5BCD. Archivos originales conservados en `backend/build/reports/pitest-today/mutations.xml` e `index.html`.

| Clase mutada | Generados | Killed |
| --- | ---: | ---: |
| ApplicationConfiguration | 21 | 21 |
| TodayController | 4 | 4 |
| PostgresTodayQueries | 5 | 5 |
| ReadToday | 2 | 2 |
| TodayItem | 3 | 3 |
| TodayWindow | 37 | 37 |
| TodayWindow.Agenda | 8 | 8 |
| Total | 80 | 80 |

Inventario de sobrevivientes/no cubiertos: vacío. No hay equivalencias a justificar, exclusiones nuevas ni tests propuestos para inflar un resultado. Avisos de Mockito/JDK/Spring presentes en consola no impidieron ejecución; no se confundieron con mutantes eliminados. `git diff --name-only -- backend` vacío tras medición confirma que no se editaron fuentes/tests/config del checkpoint. Frontend y E2E continúan sus puertas separadas; este PASS no declara done de feature12 ni aprobación del conjunto.
