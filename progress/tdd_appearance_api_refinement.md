# Refuerzo API20, sólo pruebas

Cuatro grupos autorizados, con una fila nueva y un foco cada vez. Diez ejemplos
concretos en total; no36 tests por número de residuos. Todos inicialmente GREEN:
la producción ya cumple estas guardas y no se inventa RED ni cambia el original.

| Grupo / ejemplo incorporado individualmente | EXIT0 inicial | Objetivo original |
| --- | --- | --- |
| Defaults: acento claro válido distinto |492e06|235|
| Defaults: acento oscuro válido distinto |0cdbd7|238|
| Revisión: unconfigured con ETag configurado |b3911b|243|
| Revisión: ETag configurado con sufijo |f6b9f8|284|
| HEX: color con sufijo |7e4263|73,78|
| HEX: color repetido que conserva canales iniciales |6bad51|77|
| Contraste: #0002D0 válido, canal bajo no cero |38b223|61|
|400: error de contraste real, sin signal opcional |76df09|120,174|
|400: lista mixta, entrada de campo ajeno |bad3f0|154,156,158,159,160,161,162,163,164|
|400: HTTP503 con body de validación400 |61ec05|115|

El rechazo de defaults usa colores accesibles para no depender de otro guard.
HEX conserva canales parseables para distinguir anclas, no NaN incidental.
Contraste comprueba aceptación mediante readAppearance público, sin exportar
helpers privados ni reproducir la fórmula. El problema auténtico exige la clase
AppearanceValidationError y mensaje de campo; los problemas no fiables deben
rechazar con la MISMA Response original, preservando clasificación de fallo.

Formato seguido de suite focal completa y ESLint del archivo: EXIT0 b2850b,
40/40 tests,1 suite. Logs `appearance_api_refinement_1.log` a `_10.log`,
`_final.log`, `_format.log` y `_lint.log`. No otra suite global ni Stryker.

Freeze de `frontend/src/appearance-api.test.ts`:
`0F7CED2888606A5A8EB21D1CA3F99FFF5E23090476B1B4FD900EC4D0812E62FF`.
`appearance_api_refinement_targets.json` contiene20 firmas originales exactas
(archivo, ID, rango1-based, mutador y reemplazo) para revisión de A/root.
Son objetivos propuestos, NO kills observados. Se mantienen pendientes los
otros residuos documentados; no se reclama cierre de fronteras58/86.

Original SHA56D3A57C…D60122 intacto. Única fuente de tests cambiada es la
asignada; no producción, configuración, raw ni umbral alterados. No commit.
