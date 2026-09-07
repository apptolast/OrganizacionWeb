# Dos precondiciones asíncronas de tests de sesión

7 de septiembre de 2026, 15:20–15:23 Europe/Madrid. Arreglo acotado solicitado por root tras init aislado de C. Producción sin cambios; dos tests existentes conservan todas sus aserciones. Decoder19 detenido en su ciclo10 GREEN.

## RED real y causas

`../OrganizacionWeb-weekly-http/progress/weekly_http_init.log`, líneas 408–526: 1897/1899 verdes, dos fallos. El snapshot productivo es el mismo que pasó init común 62482; no se atribuye una regresión productiva a estos resultados fluctuantes.

1. `work-session-end.test.tsx`, @s35: faltaba «Fin acordado actual». Tras recibo EXTEND confirmado, `settle` inicia nueva generación y GET E; el aviso viejo puede desaparecer mientras la consulta está pendiente. Esperar sólo su ausencia no acredita el nuevo snapshot. La norma17 @s35 exige retirar el aviso cuando E confirma U. Ahora se espera el texto positivo con `findByText` antes de comprobar ausencia, conservando conteo3 y body/headers. El original aislado pasó inicialmente (aa7cfe, `session_effects_end_initial.log`); RED del init se conserva sin fabricar otro.
2. `work-session.test.tsx`, @s36: cinco llamadas esperadas frente a tres observadas. Redescubrir GET active y mostrar los hechos de la sesión no implica que los efectos S/E del hijo hayan empezado. Se espera el conteo5 mediante `waitFor` predeterminado; permanecen rutas y todos los oráculos de ausencia de recuperación por key. El original aislado pasó inicialmente (720144, `session_effects_remount_initial.log`). Norma14 @s36 exige redescubrimiento, sin prometer sincronía de efectos.

No aumentan timeouts, no se repiten campañas, no se retiran assertions ni se selecciona una alerta arbitraria. No se modificaron otros casos o producción.

## GREEN y freeze

- End completo: 33/33 EXIT0 bf1b7d, `session_effects_end_green.log`.
- WorkSession completo: 47/47 EXIT0 22b14a, `session_effects_remount_green.log`.
- Frontend completo una vez: 1909/1909 en 41 archivos, 22,74 s, EXIT0. Incluye los 1899 heredados y los 10 del decoder19 WIP congelado; no se confunden con nuevos casos del arreglo.
- ESLint + Prettier completos: EXIT0. Cierre 4c32c0; logs `session_effects_full.log` y `session_effects_lint.log`.
- 99 archivos `frontend/src` (97 anteriores y dos WIP19) idénticos antes/después: `session_effects_before.json` y `session_effects_after.json`. No incluye config fuera de src. Formato focal previo documentado en `session_effects_format.log`; formateó también los dos WIP19 sin cambio lógico para permitir lint global.

SHA256 final WorkSession test: `0483EB2CCEDA6A2B7270CE5EB0FFECBE5CE6A70E2D3A1E58453B15DDAFD5DB64`.
SHA256 final End test: `5B0810DF835050B48500A2A6BDBBC7A979BBB14BE9C2640EE6D52604DB77275C`.

Paquete selectivo para root: esos dos tests y este informe. Manifiestos/logs conservados; WIP19 fuera del arreglo y de su commit selectivo. No se ejecutaron build, Java, E2E ni mutación; ningún proceso B queda activo. Tras revisión se retoma decoder19, sin nuevos arreglos heredados no asignados.
