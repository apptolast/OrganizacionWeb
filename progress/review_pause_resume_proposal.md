# Revisión de la propuesta de pausa y reanudación

APPROVED para destilación Gherkin bajo la autorización global vigente. Root leyó íntegramente la sección 15 (dea54c) y comprobó las tres precisiones posteriores (3e024a). La propuesta UX mantiene los mismos límites y debe usar los nombres y formas del contrato normativo.

Se conserva el DTO de inicio y se añade estado actual separado, evitando modificar recibos históricos al pausar. Una pausa mantiene la única plaza del propietario; las acciones siguen disponibles después de completar el contexto. El neto proviene de intervalos y reloj del servidor, con precisión exacta, sin desplazar el fin previsto ni atribuir trabajo cerrado. La consulta muestra una instantánea explícita, sin temporizador local ficticio.

La revisión corrigió tres ambigüedades: identidad del ETag comprobada después de propiedad y antes de replay; invariantes y límites temporales del estado/neto; orden de lectura coherente seguido de una única captura de reloj, con errores y queries definidos. Las decisiones son implementación concreta del alcance aprobado, no una nueva puerta de permiso.

Pendiente: Gherkin por un autor distinto, revisión del contrato y TDD. No autoriza anticipar producción, pausa automática, cierre oculto, controles de administración, historial global o cambio de la duración prevista. Las convenciones comunes se referencian sin duplicar todas las variantes de 14; las diferencias nuevas necesitan evidencia propia.

Corrección posterior aprobada antes de TDD (69e0e3): se usa `Work-Session-Revision`, un token propio de revisión, en lugar de un ETag fuerte constante. El cuerpo del snapshot cambia con serverNow y netMicroseconds aun sin una transición; un validador fuerte debe cambiar con los datos de representación según [RFC 9110 §8.8.1](https://www.rfc-editor.org/rfc/rfc9110.html#section-8.8.1). La corrección preserva identidad, precedencia y códigos de error sin añadir rutas ni cambiar feature 14. El autor Gherkin ha recibido el contrato actualizado.
