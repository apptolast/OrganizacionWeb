# Replay mínimo de dos oráculos15

Preparación de lectura; no campaña ni modificación de runner/config/producción. Stryker instalado10.0.0 admite `run [configFile]` (README36 y stryker-cli.js) y rangos `archivo:startLine[:startColumn]-endLine[:endColumn]`. project-reader.js193–207 convierte líneas a base0 y conserva columnas base0; usar líneas completas evita trasladar erróneamente las columnas1 del reporte.

Propuesta mínima: crear al freeze una configuración JSON local derivada exactamente de la15 aprobada, cambiando únicamente mutate, rutas de reportes y temporal propio. No nuevo target ni modificación permanente del dispatcher. Ejecución propuesta: `pnpm --dir frontend exec stryker run stryker.pause-resume-session.replay.config.json`, sólo tras autorización root y revisión de los dos tests.

- `src/work-session-state.tsx:84-85`: objetivos originales401/409/410, autorización de reenvío tras comprobación.
- `src/work-session-state-api.ts:195-196`: objetivos266/267/269, validación de acción/recibo de K.

Las seis firmas y ubicaciones fueron leídas del JSON original697cce. Se volverán a contrastar con hashes/posición al freeze; no seleccionar por número regenerado. Estos rangos pueden producir mutantes adicionales: se inventariarán aparte y se exigirá correspondencia de las seis firmas, sin confundir total generado con número de objetivos.

Salidas propuestas `reports/mutation-pause-resume-session/replay.json` y `replay.html`; temporal `.stryker-tmp-pause-resume-session-replay`. Mantener ignorePatterns protegido exacto, break80,8 workers, perTest, Vitest y restantes opciones. El informe original861/JSON0F0F92…DBA246 permanece intacto. Si el foco diagnóstico queda bajo80 se conservará EXIT/score, sin rebajar puerta ni convertirlo en campaña global sustitutiva.

Incidente de preparación ee2272: PowerShell rechazó una tubería colocada directamente tras foreach; ninguna edición o ejecución Stryker. La lectura corregida697cce recuperó las firmas. No requiere nuevo andamiaje ni cambios de producto.
