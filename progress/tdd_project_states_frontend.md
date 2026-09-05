# project_states — TDD frontend

Se reutiliza el baseline raíz 8183 por coordinación: 240 pruebas backend y 122 frontend verdes. Se leyeron el contrato aprobado features/project_states.feature y su propuesta. Ponytail full y Caveman lite permanecen activos. El alcance propio es frontend y estas bitácoras; la matriz UX pertenece a integración.

## Ciclos observados

1. @s14, lista: RED al rechazar active/paused/completed o mostrarlos como Idea. GREEN con una tabla tipada de cuatro estados, validación y etiqueta real.
2. @s14, detalle y edición: RED porque el detalle fijaba Idea. GREEN con etiqueta real; la edición acepta los cuatro estados mediante el validador compartido y sigue enviando sólo nombre y descripción.
3. @s15, acciones: RED porque faltaba la región de estado. GREEN con tabla cerrada de acciones permitidas por origen; no se presenta un selector de destinos arbitrarios.
4. @s1/@s15, transiciones: RED porque las acciones no enviaban ni confirmaban. GREEN con PUT status e If-Match del mismo GET de detalle mostrado. Sus siete ejemplos exigen exactamente dos solicitudes, sin refresco silencioso previo. La lectura devuelve un snapshot interno {project, etag}; la API pública conserva siete campos.
5. @s15, espera: RED porque un doble clic disparaba otra solicitud y faltaba el anuncio. GREEN con acciones bloqueadas y Cambiando estado antes de recibir respuesta; el estado anterior se conserva hasta confirmar.
6. @s15, precondición: RED porque las acciones estaban activas sin ETag fuerte. GREEN al bloquearlas y ofrecer recarga deliberada; se reutiliza la validación del adaptador de edición.
7. @s9/@s15, conflicto: RED por falta de captura de HTTP 412 y recuperación. GREEN al conservar estado, mostrar versión más reciente y ofrecer Recargar versión guardada. El siguiente cambio usa el nuevo ETag obtenido sólo tras esa decisión.
8. @s4/@s8/@s15, capacidad: RED sin explicación del límite. GREEN con activeCount y limit recibidos del servidor, incluidos conteos superiores a un límite reducido, y enlace Elegir qué pausar. No se pausa ningún otro proyecto ni se reintenta automáticamente.
9. @s15, fallos: RED sin feedback para HTTP 400/409/503/500 o red. GREEN con mensajes propios, estado anterior y reintento explícito. El error anterior desaparece al intentar otra acción y sólo se confirma éxito tras respuesta válida.
10. @s15, pérdida de acceso: RED conservando detalle tras HTTP 401/404. GREEN al retirar datos del lector y reutilizar sus pantallas privadas de recuperación.
11. @s15, cancelación: RED porque PUT continuaba sin abortar al salir. GREEN con AbortController de escritura y guards de éxito/error. Una respuesta antigua no reemplaza otra ruta.
12. @s16, foco: RED al desplazarlo hacia h1 tras éxito o dejarlo en body tras error. GREEN al devolverlo al botón existente o al encabezado de Estado del proyecto si el botón desapareció; el foco que el usuario movió a otro control se conserva. El lector mantiene foco inicial sin imponerlo de nuevo en cada confirmación.
13. @s15, confirmación incompatible: RED al anunciar éxito cuando HTTP 200 devolvía un destino diferente. GREEN al validar destino confirmado en el adaptador compartido, conservar representación anterior y explicar incertidumbre.

El refactor en verde separó el hook useProjectStatus de su presentación. Se reutilizan adaptador de edición, validador de detalle, controles y SCSS. No se añaden dependencias, consultas silenciosas, preferencias ni pantallas nuevas. El snapshot interno mantiene cuerpo y ETag de la misma respuesta.

Las regresiones sobre lógica ya verde comprueban problemas de capacidad incompatibles (tipos, enteros, límites inválidos y código ajeno), segundo cambio con ETag confirmado y ausencia de almacenamiento persistente. No se inventa un RED para estas comprobaciones.

## Verificación y estado

La ejecución 41113 terminó verde: lint, **171 pruebas completas** y build de TypeScript/Vite. Incluye las 122 anteriores y 49 nuevas. Se entregó el build estable a integración para recorridos reales y matriz UX. Fuentes congeladas salvo hallazgo del juez o navegador.

Stryker 85794 está en curso con 312 mutantes. El alcance exacto son project-status.ts, read-projects-api.ts, edit-project-api.ts, use-read-projects.ts y use-project-status.ts. La configuración dedicada conserva el umbral 80 y los informes separados; la configuración global añade los dos archivos nuevos para futuras ejecuciones. No se repite la lógica histórica intacta.

## Trazabilidad

- @s1: siete transiciones por acciones reales, PUT exacto y confirmación con ETag del único GET.
- @s2/@s3: tabla de acciones restringida; el rechazo de transiciones no permitidas y el no-op completo corresponden al servidor.
- @s4/@s8: límite con conteos del servidor y navegación para decidir qué pausar.
- @s5/@s6/@s7: capacidad transaccional, aislamiento y configuración pertenecen al backend; la web no inventa límites ni ejecuta pausas ajenas.
- @s9: conflicto y recarga deliberada; integración verifica la concurrencia real entre texto y estado.
- @s10: fronteras de API a cargo del backend; web bloquea ETag incompatible y retira datos ante HTTP 401/404.
- @s11/@s12/@s13: transacción, outbox y RabbitMQ a cargo del backend e integración.
- @s14: estados reales en lista/detalle; la edición de texto los acepta y conserva sin enviar status.
- @s15: espera, doble envío, éxito, errores, capacidad, conflicto, invalidación de acceso, cancelación, respuestas incompatibles y almacenamiento.
- @s16: tres casos de foco, etiquetas y controles SCSS; la matriz completa de 30 principios y pruebas de navegador corresponde a progress/ux_project_states.md, propiedad de integración.
- @s17: la UI no muestra mensajes internos del servidor; usa mensajes propios y conserva cache no-store en lecturas y cambios. La seguridad HTTP se verifica en backend.

## Cierre de mutación y regresiones focalizadas

La ejecución completa 85794 terminó con 284/312 mutantes detectados (91,03 %), 28 supervivientes y ningún caso sin cobertura. La revisión encontró cinco huecos de pruebas: estado array, capacidad obsoleta, respuesta HTTP incompatible con límite, enlace privado tras pérdida de acceso y acciones de un snapshot durante recarga. Se añadieron cinco ejemplos sin modificar producción. Ejecución 45001: 54 pruebas focalizadas verdes. Ejecución 28182: lint y las 176 pruebas frontend completas verdes. El replay de esas líneas valida los fallos contra mutaciones reales; no se inventa un RED de producción ni se repite la campaña global. La clasificación individual y el resultado separado se conservan en `progress/mutation_project_states_frontend.md`.

Replay válido 53750: 14/14 mutantes detectados, EXIT 0, 43 segundos. Detecta los seis supervivientes originales correspondientes a los cinco huecos de pruebas. Los otros 22 supervivientes están justificados individualmente; la puntuación global conservada sigue siendo 284/312. Fuentes y pruebas liberadas para revisión independiente. Ponytail full y Caveman lite aplicados; no se añadieron dependencias ni se modificó producción durante esta revisión.
