# Revisión independiente de tooling — publish_outbox

Revisor: coordinador raíz, sin autoría de scripts, Compose, CI ni tests del smoke. El responsable del smoke revisará por separado el backend que no escribió; no se atribuye una revisión independiente a su propio tooling.

## Revisión de código

- Proyecto Compose aleatorio exclusivo y puerto web efímero en loopback. Broker/probe sin puertos externos. Contexto Docker observado: desktop-linux.
- Credenciales sintéticas aleatorias. El archivo de entorno enumera únicamente claves del fixture; se corrigió la copia inicial por patrón que podía incluir variables heredadas innecesarias. No se imprimen salidas Docker con posibles secretos.
- SQL limita el identificador a caracteres de UUID antes de interpolarlo; el usuario de DB se pasa como argumento. Management recibe programa Node por stdin, sin construir comandos shell a partir de credenciales.
- Limpieza por nombre de proyecto exclusivo, sin operaciones sobre servicios existentes. Eliminación del scratch después de comprobar ruta absoluta descendiente de .e2e-work.
- Imágenes y configuración coherentes con el stack. Base mantiene publicador deshabilitado; override lo habilita sin hacer depender el arranque API de RabbitMQ healthy.
- Lectura RabbitMQ con ack_requeue_true exclusivamente en el fixture, sin consumidor de producto. Comprobaciones del contenido fuera de los reintentos de readiness; espera por identidad de eventos esperados.
- Reinicio del broker con backend detenido: la prueba no puede pasar porque el publicador haya recreado topología o reenviado mensajes durante la comprobación. Filas SQL completas permanecen iguales.
- Comando test:publisher incluido en CI además del arnés, build y ocho E2E base. El runner tiene límites de tiempo y salida no-cero ante fallo de aserción o limpieza.

## Evidencia y estado

El responsable comunicó ejecución completa8008 exit0, con los tres bloques: publicación; caída/API201/recuperación; reinicio conservando mensajes y topología. El coordinador revisó el código final de cada bloque y la bitácora. La repetición conjunta sobre el árbol final y GitHub Actions se registrará en el cierre; esta revisión no sustituye esos comandos.

No hay bloqueos de tooling en el árbol revisado. No se ha desplegado en el servidor. El smoke no demuestra por sí solo cobertura de los23 escenarios ni una auditoría UX ampliada: el mapa global y el juez de producción completan esa evidencia.

Verificación final del coordinador sobre código congelado: sesión49506 exit0; ocho E2E base verdes (28,3s), después los tres bloques de test:publisher verdes. Se construyeron y ejecutaron imágenes reales y se limpiaron ambos stacks propios. Sin cambios funcionales después de esa ejecución.
