# Destilación de la API para integraciones

Fuente: sección 24 de `project-spec.md` fijada por `e1f980701a72c54f079be7498969264a54f78888`, con detalle normativo en la propuesta aprobada SHA256 `B948AB34566EA6CE1EED299B5CD7DF1DF42B3E22C56E13EA38F680A25CAE349B`. Se conserva la distinción ratificada por root: secreto transitorio en memoria para mostrar 201; prohibido persistirlo en sessionStorage/localStorage, archivos o logs.

`features/integration_api.feature`: 42 tags estables, 42 escenarios y un When por escenario. Agrupa fronteras homogéneas mediante Examples; ninguna instrucción aparece después de Examples. El código de negocio de 1–23 se referencia expresamente y no se replica como una segunda matriz de miles de casos.

- s1–17: emisión única, forma y tamaños, caducidad/cupo, replay, consultas, revocación y COMMIT incierto.
- s18–25: separación cookie/Bearer, bootstrap, allowlist completa de 18 operaciones, precedencia y preservación de contratos de escritura.
- s26–31: límites inclusivos, carrera de cuota entre credenciales/owners, ventana UTC, fallo cerrado, revocación y caducidad.
- s32: documento OpenAPI exacto, privado y sin cuota.
- s33–42: recuperación por id, errores definitivos, memoria/almacenamiento, identidad tardía, clipboard, revocación incierta, UX30 y exclusión de secretos/credenciales en copias.

Preparación documental aislada mientras 23 completa su cierre. Sin producto, pruebas, feature_list ni cambios de estado; sin commit de estos dos archivos hasta revisión B/root. No se repite init para documentos ni se atribuye aceptación de UI por este contrato.

Precisión de revisión B/root: s35 bloquea también crear manualmente otra identidad mientras el intento permanece incierto, incluidos 404 y reload; conserva comprobación y reenvío deliberados con la misma id. Sin escenario ni matriz nuevos.
