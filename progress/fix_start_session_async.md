# Corrección de sincronización del test de recuperación de inicio

7 de septiembre de 2026. Alcance: únicamente `frontend/src/work-session.test.tsx`, caso existente `@s32 @s33 keeps an uncertain intention and confirms it only through its key`. Ponytail full y Caveman lite; sin cambio productivo ni contrato nuevo.

## Causa y RED real

El init coordinado 70041 falló con 1898/1899 pruebas frontend verdes: la aserción esperaba cinco llamadas y observó tres. Evidencia: `weekly_review_init.log`, líneas 353–369, salida c001af. El foco original aislado pasó inicialmente (298f27, `start_session_async_initial.log`); se registra la fluctuación, sin fabricar otro RED mediante un scheduler artificial.

Los escenarios 32 y 33 de `features/start_work_session.feature` exigen conservar K/25 durante incertidumbre y confirmar únicamente el recibo compatible recuperado por K. El texto «Sesión iniciada» acredita el render de ese recibo; no acredita que los efectos de los paneles recién montados ya hayan iniciado GET state y GET end-time. Las tres llamadas observadas eran consulta activa, POST incierto y recuperación por key. La comprobación inmediata de cinco llamadas competía con esos efectos.

## Cambio mínimo aprobado

Se sustituye una sola línea por `await waitFor(() => expect(fetcher).toHaveBeenCalledTimes(5))`, usando el plazo predeterminado existente. Se conservan todas las aserciones de duración retenida, ausencia de reenvío, key, ruta state y única consulta end-time. No se añaden timeouts, reintentos de ejecución, mocks de componentes ni cambios de producción.

## Verificación y freeze

- WorkSession completo: 47/47, EXIT 0 (33aefe), `start_session_async_focal.log`.
- Frontend completo, una ejecución: 1899/1899 en 40 archivos, EXIT 0; duración 30,00 s. `start_session_async_full.log`.
- Lint completo (ESLint y Prettier): EXIT 0; `start_session_async_lint.log`. Cierre conjunto 0ebee3.
- 97 archivos de `frontend/src` verificados antes/después, cero diferencias: `start_session_async_before.json` y `start_session_async_after.json`. Estos manifiestos cubren fuentes y tests, no configuración fuera de src.
- SHA256 del test final: `FA20D076E1944903559CC1407A65B176C9FF1BEECD0098154C948A21F4B47ED9`.

Paquete congelado para revisión/commit del coordinador: este informe y el único test modificado. Logs y manifiestos preservados como evidencia local. No se ejecutaron build, Java, E2E ni mutación; no hay proceso de B activo.
