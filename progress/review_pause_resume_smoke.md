# Revisión del smoke de pausa y reanudación

APPROVED por root para integrar el recorrido real de @s25/@s26. No es el cierre de la feature ni sustituye la validación del navegador o las campañas de mutación.

Revisión de las 350 líneas de diff en 426e0a: el relay compartido sólo descarta la respuesta después de observar el 201 del backend. La extracción conserva el recorrido anterior de inicio. Las consultas y aserciones observan PostgreSQL, RabbitMQ y la API reales: dos transiciones, eventos originales con doce campos, recuperación por clave y recibo histórico distinto del estado actual. El reinicio exige un StartedAt nuevo del backend y conserva identidad, StartedAt y montajes de PostgreSQL. La retirada se limita a los eventos publicados del escenario; no se usa el stack existente del puerto8080.

Primera ejecución inicialmente verde, sin RED inventado: autor0886c3 EXIT0. Root verificó once PASS en fb747b y 1b737a, incluidos los dos recorridos de inicio y pausa/reanudación. Los manifiestos anterior y posterior contienen los mismos283 archivos (26fadf) y hash24EE30D0A4949486F89B81E8ACE0588911EA4FD07D64D770204D0100C1D8CF43. Script E626D9C5FC6A1FCD4334EF58AC5C6CE81DA0E456D16540865E21C8AC1C298AB9; log A02A97A5504B59A1C586445136336AE4F11B20E91A7AE304E11CE0A212B59A97.

La imagen usada precede al arreglo posterior de feedback del frontend. El backend permanece idéntico; esta evidencia acredita el recorrido HTTP y la publicación, sin atribuirle la interfaz corregida. La evidencia visual se genera con su propia imagen actual. No se solicita repetir este smoke sin una modificación o hallazgo que lo justifique.
