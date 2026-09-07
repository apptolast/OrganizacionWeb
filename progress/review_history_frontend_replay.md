# Replay dirigido frontend18 — cierre

EXIT0 b57d3d. Configuración exclusiva stryker.history-replay.config.json, nueve rangos aprobados,8workers/perTest/todas suites/80 y protección intacta. Instrumentó33mutantes; terminó en1min42 con32Killed/1Survived/0NoCoverage/0RuntimeError/0Timeout. Score dirigido96,97%; no sustituye el original84,15%.

Mapping por file/location/mutador/replacement verificado f11ea2: **17/17 firmas objetivo Killed**, no por coincidencia de IDs. `history_frontend_replay_mapping.json` SHA E264C3243AC61A48440C21BAC223D26D17D787F6598C0657C87D3AE698AC4CC2.

| Original | Replay | Resultado |
|---|---|---|
|76|0|Killed|
|77|1|Killed|
|124|4|Killed|
|125|5|Killed|
|136|8|Killed|
|137|9|Killed|
|138|10|Killed|
|150|14|Killed|
|151|15|Killed|
|166|18|Killed|
|167|19|Killed|
|168|20|Killed|
|298|24|Killed|
|302|28|Killed|
|547|29|Killed|
|692|31|Killed|
|696|32|Killed|

Extras:15Killed (originales78,123,126,127,139,140,149,152,153,169,170,297,299,300,301), unSurvived original548→replay30. Este último cambia setRefresh(value+1) por value-1: cada reintento sigue teniendo un valor diferente, disparando la misma lectura; no se usa orden ni valor positivo del contador. Se conserva rawSurvived, sin refuerzo espejo ni nueva campaña.

Before137 y after137 idénticos,0mismatches b6c3e0. AfterSHA E0CD91A8045FCAF76979C5CDA3546EA1E94FDBCA90C99A62BB2EFB23DE940D85. Raw replay `history_stryker_replay_raw.json` SHA4038BB17D5BBE2CB2037C1CADDBC9366C596CC48F7B23A3EA05EC4E9887DE727. Original21F58FA90232A5C85FB5A78AE889F75FC718FB2739D150684867655E6BC65FF5 verificado sin cambios; sus3RuntimeError438/442/443 siguen siendo errores históricos, fuera de este objetivo.

No cambios de producción/tests/config durante replay, sin Gradle ni repetición de campaña completa. .gitignore no pertenece al manifiesto; root puede agregar patrones de evidencias después del after. No se declara18done ni aprobación de otros gates. Paquete congelado para revisión final.
