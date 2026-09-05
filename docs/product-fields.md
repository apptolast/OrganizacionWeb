# Campos y recorrido del producto

Propuesta inicial del 5 de septiembre de 2026. Los campos avanzados se incorporan
por features independientes; este catálogo no significa que estén implementados.

## Proyecto

Nombre; descripción; resultado deseado; estado (idea, activo, pausado, terminado,
archivado); prioridad; color; icono; fecha objetivo opcional; presupuesto semanal;
enlaces a repositorios y recursos; etiquetas; campos personalizados.
Identificador, propietario, fecha de creación y cambios son datos de sistema.

## Tarea

Título; proyecto; sección; descripción; resultado observable; criterios de aceptación;
estimación en minutos; prioridad; fecha límite opcional; estado; subtareas; dependencias;
etiquetas; enlace externo. La planificación se registra en bloques distintos de la tarea.
Se evita sumar dos veces la estimación del padre y la de sus subtareas.

## Bloque de planificación

Tarea; objetivo de la sesión; fecha; hora de inicio y fin; zona horaria; descanso posterior;
recordatorio; recurrencia opcional; estado (previsto, realizado, movido, cancelado).
Moverlo no cambia retrospectivamente cuándo se trabajó. La duración estimada de una
tarea y la duración de un bloque no tienen por qué coincidir.

## Sesión real

Tarea; bloque de origen opcional; inicio; pausas; reanudaciones; cierre; duración activa;
nota de avance; siguiente paso; motivo de interrupción opcional. Una sesión cerrada
no completa automáticamente una tarea. Si se pierde conexión o se cierra la pestaña,
el tiempo posterior no se inventa como trabajo: se ofrece reconciliarlo al volver.

## Historial

Qué cambió; cuándo ocurrió; cuándo se registró; objeto afectado; origen (usuario o
conector); detalle del avance. Completar una tarea guarda completedAt; reabrirla
conserva el evento previo. Las correcciones del tiempo quedan identificadas.

## Preferencias

Zona horaria; inicio de semana; jornada por día; días de descanso; capacidad diaria;
duración de bloque y pausas; límite de proyectos activos; objetivo semanal; avisos;
tema; acento; densidad; tamaño de texto; orden y visibilidad de paneles.

## Primer recorrido visible

1. Entrar y ver un estado vacío útil, sin métricas de ejemplo que parezcan reales.
2. Crear un proyecto con nombre; los campos avanzados permanecen plegados.
3. Añadir una tarea que tenga un resultado pequeño y verificable.
4. Elegir un hueco con inicio y fin; mostrar la capacidad disponible del día.
5. Empezar la sesión desde Hoy, con el objetivo y hora de cierre visibles.
6. Cerrar, anotar avance y dejar el siguiente paso. Completar la tarea es otra acción.
7. Consultar en el historial el trabajo realizado y revisar la siguiente semana.

## Ejemplo de descomposición: web de Cenit Digital

Ejemplo propuesto, no datos importados de un repositorio ni tareas ya creadas.

| Sección | Feature acotada | Criterio observable |
| --- | --- | --- |
| Navegación | Menú móvil | Abre/cierra con botón y teclado; al cerrar restaura el foco |
| Inicio | Presentación principal | Expresa servicio, destinatario y acción de contacto; sin desbordamiento a 360 px |
| Servicios | Ficha de un servicio | Muestra resultado, alcance y acción de contacto |
| Proyectos | Tarjeta de caso | Título, imagen con alternativa y enlace accesible |
| Contacto | Validación de formulario | Señala campos inválidos, conserva datos y enfoca el primer error |
| Contacto | Envío confirmado | Muestra éxito solo tras respuesta del servidor y permite recuperar fallos |
| Accesibilidad | Recorrido con teclado | Toda acción alcanzable y foco visible en el orden de lectura |

Estos criterios se destilarán en escenarios concretos dentro de cada proyecto;
la herramienta facilitará guardarlos y trabajar por sesiones, sin imponer una
duración ficticia a una feature todavía no estimada.
