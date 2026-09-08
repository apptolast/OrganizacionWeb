# Estado actual — API para integraciones (24)

Las features 1–23 están desplegadas en https://organizacion.apptolast.com. Feature 24 sigue in_progress con 42 escenarios aprobados. Features 25–30 pendientes. Autorización global vigente; Ponytail full/Caveman lite.

## Validación confirmada

Las tres campañas finales pasan con killed/total estricto: núcleo 180/188 (95,7447 %), HTTP 114/118 (96,6102 %) y frontend 836/997 (83,8516 %). Se conservan todos los estados residuales, originales y límites en los informes finales y ZIPs portables. El primer Stryker falló con 780/997 y queda preservado; el correctivo mantiene las mismas 997 identidades y configuración. No hay reclasificación.

Backend, HTTP, wiring, contrato OpenAPI y navegación real cuentan con evidencia revisada. Init aislado: Node72, frontend2492 y Java3446; los 15 refuerzos frontend posteriores tienen foco83/83. CI integrada final todavía pendiente.

Imágenes fe6f87f publicadas por digest, linux/amd64 y revisión comprobadas, 580 entradas de construcción sin cambios. Ensayo API24→23→24 PASS: 20 tablas y dos hashes de esquema preservados; límites explícitos en integration24_image_rollback_original.json.

Copia real V21 de las 16:22 UTC restaurada completamente en PostgreSQL aislado: 19 tablas restauradas, 17 tablas de negocio/Flyway y esquema coinciden con observación live separada. Sin datos de autenticación en comparación ni restauración de ACL. Recursos efímeros retirados. Evidencia en integration24_backup_restore_original.json y comparison.json.

Infraestructura candidata 1e873f0: bootstrap, validate, lint y check pasan; CI de PR40 verde. No se ha aplicado la entrega24. Se conserva el falso positivo original de Gitleaks y la excepción exacta revisada al digest público, con controles negativos.

## Trabajo activo

CI 34248649295 falló seis casos E2E: una expectativa Basic anterior al contrato Bearer, cuatro recorridos de teclado con presupuesto fijo insuficiente y una aserción de ancho con texto200 %. Las correcciones de autenticación y teclado pasan junto al caso de texto (6/6 en Windows); texto200 también pasa en Linux oficial. Su fallo original permanece sin causa demostrada: se añade diagnóstico al mismo oráculo, sin ocultar overflow ni ampliar límites. Pendiente revisión final y una CI integrada sobre el conjunto.

Después de CI verde: merge, aplicación oficial y aceptación HTTPS sin escrituras de negocio, y cierre24. Ninguna prueba local aislada sustituye esas puertas.

## Límites operativos

COMMON/V14 protegido, excluido antes de nuevas lecturas de contenido. Puertos8080/18080/18081 reservados. No alterar procesos, stacks ni datos ajenos. Credenciales fuera de Git/logs/chat. QA de escritura sólo efímero. Evidencia UX automatizada no equivale a estudio humano ni garantía universal.
