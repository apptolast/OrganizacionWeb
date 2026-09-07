# Plan de entrega del MVP

Actualizado el 7 de septiembre de 2026. Objetivo: terminar hoy. Estimación revisada tras finalizar las campañas originales de historial y la regresión general: **2–4 horas efectivas para el software**, más **4–8 horas para el despliegue**, condicionado a acceso, dominio y capacidad del servidor. Confianza media-baja en software y baja en despliegue. No es una garantía ni una predicción de consumo de cuota.

## Estado comprobado

El MVP comprende las funcionalidades 1–18: acceso privado, proyectos y tareas, disponibilidad, planificación y replanificación, Hoy, sesiones con pausa, reanudación y cierre, aviso de fin, ampliación e historial. Conserva React, TypeScript, pnpm y SCSS; Java y Spring Boot con arquitectura hexagonal y EDA; PostgreSQL y RabbitMQ en un monorepo.

Las funcionalidades 1–17 están cerradas técnicamente y fusionadas. La PR 16 incorporó la funcionalidad 17 en `56b91bee09d332eda27a016eea34d20f639992ab`. Su CI previa, `34100084803`, terminó correctamente y se verificó que el árbol del squash era idéntico. La CI posterior de main, `34101887939`, también terminó correctamente sobre ese mismo commit (verificación f8c94b; watch12926 EXIT0, 93b922).

La validación local de la funcionalidad 17 incluye 2.076 pruebas Java, 1.802 frontend, 40 del arnés, build, 121 E2E y 13 comprobaciones del publisher. La revisión UX documenta 515 mediciones, 35 análisis axe sin incidencias y pruebas en tres motores, con límites explícitos. PIT obtuvo 616/620; Stryker, 1.684/1.976 (85,22 %), con cuatro errores fuera de ese denominador. Los replays dirigidos se registran por separado. Véase `progress/judge_end_time_notification_final.md`.

La funcionalidad18, Historial, tiene su implementación revisada e integrada; sigue en validación con tres agentes. El contrato aprobado contiene39escenarios y142ejemplos, que no equivalen a pruebas ejecutadas. La regresión general ha pasado con2.211Java,1.887frontend y47Node. El frontend se volvió a validar tras corregir el foco de navegación; build y formato de los estilos finales también pasan. Hay cuatro recorridos E2E focales y un primer UX con155mediciones y5axe sin incidencias. PIT y Stryker originales terminaron y superan el umbral: 263/284 y 621/738 respectivamente; Stryker conserva además tres RuntimeError fuera de su denominador oficial. Se refuerzan casos concretos encontrados por mutación, sin repetir campañas completas. Faltan los replays dirigidos, completar la evidencia UX, el E2E global del corte final y CI. Estos avances no acreditan todavía el cierre de18 ni el despliegue.

## Trabajo pendiente

| Hito | Resultado verificable | Horas efectivas |
| --- | --- | ---: |
| Historial | Refuerzos acotados de pruebas, replay dirigido y cierre UX | 0,5–1 |
| Integración final | Revisión, recorrido conjunto y CI del corte final | 1–2 |
| Margen de correcciones | Defectos concretos de integración, pruebas o herramientas | 0,5–1 |
| Software restante | Ejecución coordinada, no horas-persona | **2–4** |
| Despliegue condicionado | Swarm, HTTPS, persistencia, reinicio, respaldo, restauración y reversión | **4–8 adicionales** |

La revisión se basa en hitos ya comprobados: implementación integrada, regresión y compilación verdes, publicador verificado y campañas originales terminadas. No resulta de restar tiempo transcurrido ni dividir horas entre agentes. Este rango sustituye las 5–9 horas estimadas al cierre de la funcionalidad 17. Backend, interfaz e integración HTTP avanzan con propietarios de archivos y entregas revisables. Los nuevos módulos reciben validación completa; el alcance de mutación de las integraciones reutilizadas se determina por el cambio real.

## Dependencias del despliegue

Todavía no hay despliegue productivo acreditado. El acceso SSH y el hostname definitivo ya están preguntados; el último intento SSH fue rechazado por autenticación de clave pública. Las esperas externas no tienen una duración conocida. El margen histórico de 45 MiB corresponde a límites presupuestados, no a memoria libre actual.

Existen Dockerfiles, Compose y un overlay de RabbitMQ. Falta publicar imágenes, integrarlas en el catálogo de Swarm y verificar recursos actuales, DNS, HTTPS, secrets, persistencia y recuperación. La ruta `/healthz` de Nginx por sí sola no acredita disponibilidad de API, base de datos o broker. Véanse `docs/deployment-readiness.md` y `progress/review_mvp_deployment_handoff.md`.

## Aceptación y alcance posterior

La entrega requiere recorridos reales contra API y base de datos, conservación tras reinicio, privacidad, recuperación sin duplicados y distinción entre planificación y trabajo real. El despliegue exige además comprobar HTTPS, acceso y recuperación en el servidor. Las pruebas no garantizan ausencia absoluta de errores ni certificación universal de las treinta leyes de UX.

Las funcionalidades 19–30 siguen autorizadas para después del MVP: revisión semanal, personalización, vistas y campos, importación y exportación, API de integración, webhooks, calendarios, GitHub, conectores y automatizaciones. Necesitan sus propios contratos. El inventario de proveedores debe acotarse antes de estimar el proyecto completo.

