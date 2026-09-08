# Conectar una aplicación a tu organización

Disponible en la entrega 24, actualmente pendiente de desplegar. La API permite
consultar proyectos, tareas, agenda e historial, crear proyectos y tareas raíz,
y editar proyectos. El documento OpenAPI enumera las 18 operaciones admitidas.

## Crear una credencial

1. Inicia sesión y abre **API para integraciones** en el menú.
2. Escribe un nombre que te ayude a identificar el cliente.
3. Marca los permisos que necesita. Leer y escribir son permisos independientes.
4. Elige una caducidad de 7, 30 o 90 días y pulsa **Crear**.
5. Copia el secreto al gestor de secretos de tu cliente antes de cerrar el panel.

El secreto se muestra una sola vez. El servidor conserva su verificador, por
lo que no puede volver a mostrarlo. Puedes mantener hasta diez credenciales
válidas simultáneamente; las caducadas o revocadas liberan una plaza.

| Permiso            | Uso                                               |
| ------------------ | ------------------------------------------------- |
| Leer proyectos     | Listado y detalle de proyectos propios            |
| Escribir proyectos | Crear y editar proyectos propios                  |
| Leer tareas        | Tareas, estado, jerarquía y subtareas propias     |
| Escribir tareas    | Crear tareas raíz en un proyecto propio           |
| Leer agenda        | Hoy, bloques propios y su estado                  |
| Leer historial     | Historial, revisión semanal e historial de tareas |

## Hacer una petición

Envía el secreto en la cabecera HTTP `Authorization`, con el esquema
`Bearer` y un espacio antes del valor. No lo incluyas en la URL, en código
publicado ni en los logs de tu cliente.

Este ejemplo usa una variable de entorno que tu cliente debe proporcionar:

```bash
printf 'Authorization: Bearer %s\n' "$ORGANIZATIONWEB_API_TOKEN" | \
  curl --fail-with-body --header @- \
    https://organizacion.apptolast.com/api/v1/projects
```

El contrato OpenAPI privado se obtiene en
`/api/v1/integration-openapi.json`, con una credencial válida o con la sesión
web. Su consulta no consume cuota. La edición de proyectos conserva
`ETag`/`If-Match`; consulta el contrato antes de implementar escrituras.

| Respuesta | Qué revisar                                                                 |
| --------- | --------------------------------------------------------------------------- |
| 401       | Secreto inválido, credencial caducada/revocada o identidad deshabilitada    |
| 403       | Permiso insuficiente, operación no admitida u origen de escritura rechazado |
| 429       | Cuota agotada; espera los segundos indicados en `Retry-After`               |
| 503       | Almacenamiento temporalmente inaccesible; no presupongas confirmación       |

La cuota es de 60 peticiones por credencial y 120 por propietario en cada
minuto UTC. Una petición admitida que falle una regla de negocio también
consume cuota. El uso normal de la web con sesión conserva su canal propio.

## Recuperar una creación incierta

Si se pierde la respuesta, usa **Comprobar creación**. La web conserva sólo
el propietario y el identificador del intento en esta pestaña; nunca guarda
el secreto en el almacenamiento del navegador. Una comprobación puede
confirmar la credencial, pero no recuperar un secreto que no llegaste a ver.
En ese caso, revócala y crea otra de forma deliberada.

Un resultado ausente no demuestra que la petición original nunca se confirmará.
Sigue las opciones de recuperación del mismo intento que ofrece la pantalla;
no generes credenciales nuevas automáticamente para sortear la incertidumbre.
Tampoco crees otra manualmente hasta resolver el intento pendiente.

## Revocar el acceso

Busca la credencial por nombre, abre su revocación y confirma. Desde la
revocación confirmada, las nuevas peticiones con ese secreto se rechazan.
Una petición que ya se había admitido puede terminar. Si se pierde la respuesta
de revocación, comprueba el estado desde la propia pantalla antes de repetir.

Las credenciales y sus cuotas no forman parte del archivo de exportación de
datos personales. Esta API no instala conectores de GitHub, calendarios ni
webhooks: esas funciones tienen sus entregas y configuración propias.
