# Revisión de wiring14

Aprobado para gates globales y mutación. Root revisó5889db y5d55af: tres factories registran una instancia concreta del Store y sus casos de uso. Dos contextos acotados heredados reciben únicamente dependencias de construcción; no se relajan aserciones ni se sustituyen puertos en la integración HTTP/PostgreSQL real.

La suite nueva verifica POST201/Location/DTO exacto y precisión temporal, persistencia atómica sesión/evento, conservación de contexto/planificación, lecturas ID/key/active sin escrituras y replay200 con filas idénticas. Los tres casos siguen TDD individual según tdd_start_work_wiring.md. No cambia Store/core/Controller ya revisados.

Root verificó los XML finales en68db19:3 integración real,49HTTP,16 wiring y7 configuración,75 total; cero fallos, errores u omitidos. Autor104b16 y Spotless focal real limpio, cuatro hashes en bitácora. El init global ya está en ejecución58795e/sesión46298; no se anticipa su resultado. Smoke y navegador pertenecen a otros paquetes y no se acreditan aquí.
