# Mutación — publish_outbox

Ejecución final backend 74662: `gradlew test pitest bootJar` tras spotlessApply, exit 0. Informe generado: `backend/build/reports/pitest/mutations.xml` y `backend/build/reports/pitest/index.html` (artefactos ignorados, regenerables).

PIT 1.22.0: **90/90 KILLED, 0 SURVIVED, 0 NO_COVERAGE**, 105/105 líneas. El denominador incluye clases previas de create_project y nuevas clases publish_outbox en `com.apptolast.organization.domain.*` y `com.apptolast.organization.application.*`; el desglose es 54 mutantes nuevos de publish_outbox y 36 de create_project, todos eliminados. Tests objetivo en esos mismos paquetes. Umbral configurado 80 %; resultado 100 %.

Filtro FRECORD desactivado para que PIT examine los constructores escritos a mano de records. Exclusiones de métodos: equals, hashCode, toString generados por compilador. No se excluyen validadores, getters de negocio ni lógica de publicación. Ningún superviviente necesita justificación. La primera ejecución 86/90 reveló carencias de aserción de JSON, versión, frontera de 120 Unicode y timestamp parseable discordante; fueron cerradas con comportamiento observable, no excepciones al umbral.

Los adaptadores JDBC/Rabbit/configuración/logging están fuera del alcance PIT configurado. No se les atribuye el 100 %. Su evidencia son tests reales PostgreSQL/Rabbit/HTTP, pruebas del scheduler, privacidad y los cuatro mutantes semánticos del adaptador Rabbit ejecutados en copia aislada, todos detectados, documentados en `progress/tdd_publish_outbox_broker.md`. El mapa completo de escenarios está en `progress/tdd_publish_outbox.md`.

Verificación final local del coordinador 6887, exit 0: PIT vuelve a dar 90/90 y Stryker 143/148 (96,62 %). Los cinco supervivientes frontend son los del baseline create_project, sin cambios en esta feature; justificación en `progress/mutation_create_project.md`. No hay nuevos supervivientes ni exclusiones añadidas. El juez emitió APPROVED; CI remoto `33993262637` todavía en curso al registrar estos resultados.

Confirmación remota posterior: Application CI 33993262637 completado SUCCESS para 1a3737758c655462fc3814f6af8d0f87138eb1a8, incluidas verificación y mutación. Feature cerrada tras esta confirmación; enlace: https://github.com/apptolast/OrganizacionWeb/actions/runs/33993262637.
