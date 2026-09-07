# Aclaración del escenario de seguridad de historial

Dictamen: **APPROVED**, bajo la autorización global vigente. La revisión de SecurityConfiguration (d1fa57), el oráculo HTTP existente y el diff de dos líneas (de6f8a) muestran que GET autenticado no exige CSRF. El ejemplo anterior de un supuesto 403 no identificaba una condición realizable con esa política. Se sustituye por query inválida sin token CSRF: 400 query INVALID_VALUE y ninguna llamada a aplicación. No se modifica autenticación, OriginGuard, CSRF de escrituras ni producción.

Se conservan 39 escenarios y 142 ejemplos: cambia una fila, sin añadir ni retirar ejemplos. El comportamiento ya queda acreditado por s12_unknownQueryIsRejectedBeforeUnavailableStorage (036f10); no se presenta como nuevo RED ni como un 403 probado. Las otras precedencias de cursor y contexto permanecen intactas.

Hashes después de la aclaración: features/history.feature C9AB1D0486E98FB3F7B868EB5074DFD8C1EEE2EADC463B47C9032749F87B5D0D; project-spec.md 269E971B83E632F218DB5944E8693CA42159B75DCD11B984270351D158CF18D7. Los hashes anteriores describen sus cortes históricos y no se reescriben.
