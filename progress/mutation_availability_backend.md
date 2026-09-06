# Mutación backend de disponibilidad

Ponytail full y Caveman lite activos. La ejecución focal se autorizó después de la revisión independiente APPROVED y del init conjunto 8318: 984 pruebas backend, 841 frontend y lint global verdes. No se ha modificado producción tras ese corte.

Comando: `./gradlew.bat pitest -PmutationScope=availability`, Java 25. Ejecución 57648 EXIT 0. Informe: `backend/build/reports/pitest-availability/mutations.xml`. Los resultados y cualquier replay se registrarán por separado, conservando el original.

El alcance incluye Availability, AvailabilityRevision, ReadAvailability, SaveAvailability, AvailabilityController (incluido parsing), PostgresAvailabilityStore, JavaTimeZoneCatalog y ApiErrors. Este último incorpora handlers históricos; su presencia no convierte toda la cifra en lógica nueva. DTO/puertos sin lógica no se presentan como lógica comprobada. El perfil global también incluye los adaptadores y pruebas de disponibilidad. Umbral 80 conservado; sin exclusiones específicas de comportamiento nuevo.

## Resultado original verificado

XML original: 130 KILLED de 130 mutantes (100 %), cero SURVIVED, TIMED_OUT, NO_COVERAGE y demás estados. Cobertura de líneas instrumentadas 255/255. PIT informó 98 segundos: 32 segundos de cobertura y 65 de análisis. No hubo replay ni modificaciones posteriores de producción o tests. Copia exacta conservada en `.e2e-work/pit-availability-original.xml`.

| Clase | Mutantes KILLED |
| --- | ---: |
| com.apptolast.organization.adapter.config.JavaTimeZoneCatalog | 1 |
| com.apptolast.organization.adapter.http.ApiErrors | 14 |
| com.apptolast.organization.adapter.http.AvailabilityController | 44 |
| com.apptolast.organization.adapter.http.AvailabilityController$PreferenceResponse | 5 |
| com.apptolast.organization.adapter.persistence.PostgresAvailabilityStore | 17 |
| com.apptolast.organization.application.ReadAvailability | 2 |
| com.apptolast.organization.application.SaveAvailability | 14 |
| com.apptolast.organization.domain.Availability | 27 |
| com.apptolast.organization.domain.AvailabilityRevision | 6 |

La cifra incluye 14 mutantes de ApiErrors, donde conviven el handler nuevo y handlers históricos, y cinco de los accesores del record de respuesta. No se presentan esos accesores como reglas de negocio ni todos los handlers como lógica nueva. No hay supervivientes que clasificar ni timeouts contados como detección. El coordinador volvió a leer el XML original y confirmó independientemente los 130 KILLED y el desglose por clase (salida b810e1). Umbral 80 superado sin exclusiones nuevas. Gradle liberado; el cierre de feature continúa pendiente de integración, UX y mutación frontend.
