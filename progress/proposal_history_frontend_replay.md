# Propuesta de replay frontend18 tras doce refuerzos

Revisión requerida antes de crear configuración o ejecutar. Fuentes iguales al raw original; sólo history-api.test.ts y history.test.tsx cambiaron. Regresión95/95 y formato/lint/types verdes. Manifest history_frontend_refinement_freeze.json identifica el corte.

`history_frontend_replay_proposal.json` contiene SHA originales,17firmas y nueve rangos parser exactos (a716b8). Objetivos:12supervivientes de forma/discriminación API y cinco de UI (298S,302NC,547S,692NC,696NC). No se pronostica su nuevo estado.

La configuración futura debe heredar8/perTest/todas suites/mutadores80 y protección; salidas/temp exclusivos mutation-history-replay y .stryker-tmp-history-replay. Sin overwrite del raw original21F58F…65FF5 ni reutilización de IDglobal como identidad: mapear file, ubicación, mutador y reemplazo. Extras que genere el rango se reportan aparte. Before/after nuevos, todos estados y EXIT preservados. No nuevo target/backend/Gradle necesario si root prefiere ejecutar configuración explícita después de revisar.

Los tres RuntimeError originales siguenError; no forman parte del objetivo ni se les atribuye detección. Global original84,15% y History78,33% permanecen históricos; un replay no sustituye esa campaña completa. Los demás riesgos están inventariados en mutation_history_frontend.md y no se abre otra matriz por porcentaje.
