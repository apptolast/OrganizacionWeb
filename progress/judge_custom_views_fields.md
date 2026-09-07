# Revisión incremental de vistas y campos personales

Estado: **IN_PROGRESS**, sin aprobación de cierre de la feature 21.

## Cortes revisados

- Lectura hexagonal y PostgreSQL: puertos y modelos `9ef8a75`, adaptador,
  migración V20 y bean real `7b58da6`; GET HTTP integrado en `b1ed8e6`.
  La ejecución combinada de HTTP y arranque real quedó verde en `34a4d6`.
  Esto acredita la lectura inicial, no las escrituras o toda la integridad
  de filas seleccionadas.
- Helpers de dominio `CustomizationView` y `CustomFieldLabel`: `36ee10d`.
  Revisados contra los ámbitos, duplicados y Unicode White_Space del
  contrato. Sus dos hashes coinciden con el manifiesto entregado.
- Puertos reales de creación y edición de definiciones: `bdacad6`.
  Sus dos hashes se verificaron antes de transferirlos al agente HTTP
  mediante `9db2289`. No se añadieron beans ficticios.
- Cliente de configuración y 43 pruebas focales: `709021f`.
  Dos hashes coinciden con `customization_frontend_config_freeze.json`.
  Revisados DTO cerrado, identidad de ámbito, BIGINT, ausencia y defaults,
  etiquetas, tipos, conservación de definiciones y de la intención enviada.
  El helper existente `instant` ya limita fechas válidas y microsegundos.
  Se conserva la evidencia focal de formato, ESLint y TypeScript del autor;
  no equivale a una ejecución global del frontend.

## Hallazgos que deben resolverse antes del cierre

1. El cliente valida el contenido y la gramática del ETag, pero todavía debe
   comprobar que la confirmación respeta la revisión anterior: UUID estable,
   versión cero en primera escritura, incremento único en cambio real y
   revisión/fecha conservadas en no-op. Un cuerpo cambiado con el ETag
   anterior no puede presentarse como éxito. También debe impedir confirmar
   una edición cuyo campo no existía en la instantánea enviada. Corrección
   delegada al autor mediante TDD, sin bloquear su trabajo independiente de
   valores.
2. En la lectura provisional de `UpdateCustomField`, el reloj podía hacer
   retroceder `updatedAt`. El autor confirmó que su ciclo 29 ya reproduce
   ese caso en rojo (`d869dc`). La implementación no estaba congelada ni
   se había aprobado como completa.

## Puertas pendientes

Persistencia de comandos y valores, validación de datos guardados,
concurrencia y rollback; HTTP completo con los beans reales; estado y
formularios del navegador, recuperación y privacidad; revisión de código
final, pruebas integradas, mutación, matriz UX y aceptación tras despliegue.

La producción continúa en `ed00ad4`, con las funciones 1–20 aceptadas.
No se ha desplegado la feature 21. V14 heredada permanece fuera del trabajo.
