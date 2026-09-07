# Precisión de la última semana pública (@s6)

Lectura Java 25.0.1, misma versión principal del toolchain, 2e8dff y bd204c. JavaTimeZoneCatalog usa ZoneId.getAvailableZoneIds más UTC; UTC ya pertenece a los 604 IDs observados.

El último lunes cuya semana termina dentro de 9999 es 9999-12-20. El domingo es 9999-12-26 y el extremo civil exclusivo 9999-12-27. Enumerando atStartOfDay de ese extremo en las 604 zonas del catálogo actual, el mínimo UTC es 9999-12-26T10:00:00Z y el máximo 9999-12-27T12:00:00Z. Ambos están dentro del rango público. El extremo exclusivo inferior de la primera semana empieza el 0001-01-08 civil, lejos del límite inferior incluso con los offsets admitidos.

No hay una entrada pública válida que alcance la fila de extremo exclusivo superior fuera del rango con ese catálogo y semana completa. La comprobación de frontera inicial UTC sí es alcanzable (0001-01-01 en Etc/GMT-14) y permanece. No se fabrica una semana incompleta para acreditar la fila imposible ni se cambia protección existente.

Propuesta de delta único en features/weekly_review.feature, manteniendo 34 escenarios y 100 ejemplos:

```diff
-      | el límiteUTC exclusivo de semana no es representable públicamente | 409 WEEKLY_REVIEW_TIME_OUT_OF_RANGE  |
+      | sin date, serverNow=9999-12-31T12:00:00Z y zonaUTC produce una semana cuyo domingo sale de9999 | 409 WEEKLY_REVIEW_TIME_OUT_OF_RANGE |
```

El nuevo ejemplo corresponde a ReadWeeklyReviewTest.s6_defaultWeekWhoseSundayExceedsYearRangeIsATemporalConflict: RED 09ff9f, GREEN 02389a y regresiones posteriores. Es distinto al date explícito inválido, que produce 400 antes de consultar. La propuesta inicial se conservó para revisión; la aprobación y aplicación posteriores se registran a continuación.

Root aprobó y se aplicó exclusivamente la fila indicada. Nuevo SHA256: B7BFCB35E5105D4DB531BF3E480596E7CBDEFBF974BDB5F8E40A1D3508068FDA. Se conservan34 escenarios y100 ejemplos; la normativa general y la protección temporal permanecen intactas.
