# Estado actual — API para integraciones24

Las funcionalidades1–23 están desplegadas y aceptadas en https://organizacion.apptolast.com. Importación23 conserva producto4c74e183 y aplicación/infraestructura ya fusionadas. El cierre documental PR28 se fusionó en4c7d558 tras CI34231982918 SUCCESS (26m38,161E2E y2omisiones documentadas). InfraPR39 se fusionó en9dbc08e. No hubo otro despliegue por documentación o corrección del test heredado.

Feature24 está in_progress con42escenarios A155A48E aprobados por root bajo autorización global; propuesta y revisión independientes versionadas. Se precisó en de9b062 que microsegundos admite0–6decimales UTC y que el primer revokedAt válido puede preceder createdAt si retrocede el reloj. No hay nuevas puertas humanas rutinarias. 25–30 no están implementadas; la consulta opcional al usuario sobre proveedores de calendario/tareas sigue pendiente y no bloquea24.

## Trabajo paralelo y revisión

A desarrolla aplicación/dominio/PG/V22 en este checkout. B desarrolla cliente/intención/interfaz en archivos separados aquí. C desarrolla HTTP/seguridad/OpenAPI en work/OrganizacionWeb-integration-http; integra commits de A aprobados. Root coordina/revisa/integra/despliega, sin escribir producto ni tests. Ponytail full/Caveman lite vigentes.

Cortes revisados en review_integration_api_checkpoints.md: emisión nominal f485486 y creación durable c58bad5 (18fuentes+51evidencias,29tests). Incluyen replay/cupo/locks/rollback/wiring; A precisa dos oráculos de carrera/caducidad y continúa lectura/revocación/autenticación/cuotas. Cliente segundo corte32tests (26cliente+4intención+2UI nominal) aprobado tras corregir fechas con RED019/020; vista y su integración siguen en desarrollo. HTTP estructural16tests y corte creación/resolver24tests revisados: fronteraJSON4096, errores reales/no-store, resolver de sesión estándar todavía sin cadenaBearer integrada. No atribuir cierre funcional a esos cortes.

Baseline oficial corregido2bb20674 EXIT0: Node70 y frontend2424 ejecutados; Java UP-TO-DATE reutiliza3215pruebas/135suites ejecutadas originalmente. Root verificó5artefactos y843entradas idénticas. C inició checkout separado con install/init0 nuevos, logs originales preservados; no se atribuye su conteoJava desde XML reemplazado por focales. Git45e1edd une main4c7d558 con árbolidéntico al padre24: sólo ancestry, sin sobrescribir fuentes de agentes.

## Próximas puertas

Completar las tres fronteras, revisar/integrar, fijar corte, ejecutar regresión integrada, E2E/UX y mutación sobre todo lo tocado. Verificar rollback aditivo y export/import sin credenciales antes de construir/publicar imágenes24 y hacer aceptación HTTPS. No hay despliegue24 todavía. Plan operativo externo integration24-release-checklist.md. Evitar globales durante ciclosRED ajenos y repeticiones de gates válidos por docs.

COMMON/V14 protegido: no leer/hash/copiar/restaurar/modificar;8080protegido/18080reservado. No borrar artefactos ajenos, forzarGit ni parar procesos no identificados. Los originales23/PIT y campaña alternativa abortada siguen separados. Credenciales privadas permanecen fueraGit/logs/chat; escrituraQA de24 usa infraestructura efímera. El PC no se mantiene activo mediante automatizaciones ni otra sesión.


## Avance posterior — 16:30 Madrid

Gestión backend 17e9b3b revisada: 41 pruebas y 54 artefactos verificados. HTTP eb3cb38 integrado como ee4f275; rama publicada hasta 36912bf. Autenticación y cuota nominal cefd1c9 revisadas: 67 pruebas, 75 artefactos verificados y puertos liberados para C. El fallo de listado 503 quedó corregido. A completa concurrencia, atomicidad, invariantes y dos fixtures de wiring heredados.

B incorporó la ruta privada y navegación, recuperación por id, revocación incierta por credencial y tolerancia a almacenamiento inaccesible. Sigue cerrando UI/foco/SCSS y navegador; aún no hay freeze final. C integra los puertos reales y termina seguridad/OpenAPI; el mapa independiente de respuestas está en el artefacto externo integration24-openapi-source-map.md. A posee build.gradle.kts, B configuración Stryker y C scripts del arnés para las campañas de integración. No se ha lanzado una campaña ni suite global sobre ciclos activos.


## Avance posterior — 17:03 Madrid

Backend final propio 8d7d8b2 revisado: 122 pruebas/9 XML y 87 hashes verificados. Frontend 82a45c2 fijado, evidencia original en ZIP; revisados recuperación, privacidad, foco y guardas. Dispatcher integrado d153fbe. A revisa HTTP/Bearer parcial de C; C completa wiring real y OpenAPI; B cierra scope Stryker, rangos históricos y evidencia responsive. Campañas, regresión integrada, E2E real, rollback de imágenes y despliegue24 todavía pendientes.
