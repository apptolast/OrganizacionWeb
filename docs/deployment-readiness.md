# Preparación del despliegue del MVP

Revisión documental del 6 de septiembre de 2026. El repositorio de infraestructura local y el HEAD remoto coinciden en `c8bd39f0da15de0364372c79d56ba47fab354e46`, comprobado mediante GitHub en 61e702. No se ha conectado al servidor ni comprobado su estado vivo; los datos operativos publicados proceden de julio y agosto.

## Integración necesaria

OrganizacionWeb construye la aplicación. DockerSwarmInfrastrcture conserva la configuración reproducible del servidor: catálogo, recursos, redes, secrets, imágenes, rutas y backups. El despliegue debe incorporar el nuevo servicio a ese contrato y seguir sus validaciones; una imagen local o el CI de la aplicación no acreditan ese trabajo.

| Área | Evidencia existente | Trabajo antes de producción |
| --- | --- | --- |
| Catálogo | `docs/SERVICE_CATALOG.md` lista diez servicios, sin OrganizacionWeb | Incorporar web/API y dependencias propias, hostname y datos persistentes |
| Capacidad | `docs/CAPACITY.md` deja 45 MiB en el presupuesto agregado de límites; medición de host del 26 de julio | Medir recursos actuales y dimensionar API/BD/broker; revisar presupuesto, sin confundirlo con RAM libre |
| Acceso e identidad | `CLAUDE.md` define administración remota y secrets externos | Confirmar acceso operativo, proveedor de identidad y configuración concreta de la aplicación |
| DNS/HTTPS | La documentación advierte que el root Terraform DNS necesita adopción del estado real antes de aplicar | Usar la vía de infraestructura revisada para el hostname definitivo y verificar certificado/ruta |
| Datos y recuperación | El estado documental describe gates externos de respaldo aún abiertos; no prueba su situación actual | Comprobar destino externo y procedimiento de respaldo/restauración del nuevo servicio |
| Publicación | CI de aplicación verde sobre `ae364e5`; sin despliegue productivo acreditado | Imágenes versionadas, configuración revisada, comprobación de salud y recorrido persistente tras reinicio |

Fuentes versionadas: [instrucciones y estado de gates](https://github.com/apptolast/DockerSwarmInfrastrcture/blob/c8bd39f0da15de0364372c79d56ba47fab354e46/CLAUDE.md), [capacidad](https://github.com/apptolast/DockerSwarmInfrastrcture/blob/c8bd39f0da15de0364372c79d56ba47fab354e46/docs/CAPACITY.md), [catálogo](https://github.com/apptolast/DockerSwarmInfrastrcture/blob/c8bd39f0da15de0364372c79d56ba47fab354e46/docs/SERVICE_CATALOG.md). `CLAUDE.md` señala expresamente partes obsoletas de otros documentos: no se toman como inventario actual del servidor.

## Efecto sobre la estimación

Las 4–8 horas de validación y despliegue del plan MVP suponen acceso y configuración disponibles, capacidad suficiente y una vía de infraestructura utilizable. Si comprobar el servidor exige ampliar recursos, resolver adopción DNS o preparar respaldo externo, se estimará ese trabajo concreto antes de fijar fecha de entrega. No se presenta ninguna de esas necesidades documentales como un fallo vivo confirmado ni se cambia infraestructura durante esta revisión.
