# edit_project — TDD frontend

Baseline raíz91741 reutilizado por coordinación: init verde, sin cambios de producción desde ese baseline. Contrato aprobado features/edit_project.feature y propuesta leídos; Ponytail full y Caveman lite activos. Alcance exclusivo frontend, sin cambios de metadatos ni commits.

1. @s17 precarga: RED no existen campos en ruta editar. GREEN GET real del detalle, campos controlados y Cancelar al detalle.
2. @s17 guardado: RED no anuncia éxito ni envía PUT. GREEN PUT name/description con If-Match exacto, respuesta y ETag nuevos conservados. API todavía se endurece en ciclos posteriores.
3. @s17 espera/doble envío: RED no hay status y segundo submit dispara otra petición. GREEN Guardando cambios inmediato, campos readOnly y botón disabled; guard evita doble submit. Tres casos verdes.

Estado actual: implementación entregada a integración y juez; suite completa y build verdes. Mutación y matriz de navegador en curso; no se declara cierre final.

4. @s18: RED400/503/500 reemplazan campos por problema y red queda sin capturar. GREEN estados propios sin detalles internos, borrador exacto y reintento deliberado. Cuatro ejemplos de un comportamiento.
5. @s19: RED412 genérico sin recarga. GREEN mensaje de versión reciente y acción deliberada; GET actualiza campos y ETag usados en siguiente PUT.
6. @s20: RED401/404 conserva formulario. GREEN retira campos y acciones de guardar, explica acceso y enlaza Proyectos.
7. @s20/@s23 carga: RED no anuncia espera ni captura fallo inicial. GREEN espera inmediata, errores seguros y retry de carga;401/404 mantienen recuperación privada.
8. @s23 ETag: RED acepta ETag ausente, débil, comodín, sin comillas o múltiple. GREEN exige ETag fuerte entre comillas, opaco, sin depender de UUID/versión interna.
9. @s23 DTO: RED acepta identidad ajena, fecha inválida, ownerId numérico y null. GREEN reutiliza validador de detalle extraído de lectura;64 casos lectura+edición verdes tras extracción.
10. @s23 StrictMode: RED éxito viejo reemplaza datos y rechazo viejo presenta error. GREEN guards de abort en ambos resultados.
11. @s19: RED guardar activo durante GET de recarga. GREEN loading bloquea controles/submit, anuncia carga y mantiene borrador si GET falla.
12. @s18/@s22: RED campos400 sin asociación/foco. GREEN allowlist name/description, mensajes propios, aria-invalid/describedby y foco al primer campo. No presenta mensajes internos del problema.
13. @s23: RED PUT no cancelado al cambiar ruta. GREEN AbortController de escritura se cancela al desmontar; respuesta vieja no reemplaza formulario de otra ruta.
14. @s17: RED detalle sin enlace Editar proyecto y confirmación permanece al volver a escribir. GREEN enlace real, ruta directa y retirada de confirmación al cambiar borrador.
15. @s22 foco: primer ensayo blur sobre botón disabled en JSDOM no reproducía pérdida. Corregido montaje con body temporalmente enfocable; RED foco no vuelve. GREEN restaura control solo si foco permanece en body. Caso adicional conserva foco movido a textarea.
16. @s19: RED red durante GET de recarga confundida con guardado incierto. GREEN mensaje específico de carga y recarga deliberada disponible; conserva borrador.

Refactor en verde: hook useEditProject separa comportamiento de JSX; validador isProjectDetail reutilizado desde lectura. Se conserva estructura de creación, colores SCSS, campos y ayudas, errores junto a controles. Status de guardado vive fuera del form aria-busy para no retrasar su anuncio. No se añadieron dependencias.

Regresiones añadidas sobre lógica ya verde: PUT sin ETag/identidad incorrecta/JSON inválido/status201 no confirma; Unicode y descripción literal intactos; no storage y foco movido no robado. No se inventa RED para esas comprobaciones.

Verificación21877 exit0: lint,115 tests completos (78 anteriores +37 edición), build tsc/Vite. Fuentes estables entregadas a integración. Stryker27599 en curso255 mutantes: read-projects-api.ts, edit-project-api.ts y use-edit-project.ts. Configuración dedicada stryker.edit-project.config.json conserva umbral80 y reportes separados; configuración global añade ambos archivos nuevos para ejecuciones futuras. No se ejecuta de nuevo toda la mutación histórica.

## Trazabilidad frontend

- @s17: precarga/PUT/ETag, doble envío, confirmación, enlace de detalle y Cancelar.
- @s18: cuatro fallos de guardado, validación asociada, respuestas incompatibles sin falso éxito.
- @s19:412 conserva borrador, no retry automático, recarga deliberada usa nuevo ETag, bloqueo durante carga y recuperación de carga fallida.
- @s20:401/404 tanto carga como guardado retiran contenido y ofrecen navegación.
- @s21: GET/PUT no-store y credenciales same-origin; no almacenamiento persistente en navegador.
- @s22: campos etiquetados, errores asociados/foco, retorno de foco y Unicode; matriz navegador a cargo de integración, ver progress/ux_edit_project.md.
- @s23: carga/retry, ETag y DTO inválidos, abort/StrictMode y cambio de ruta durante PUT.
- @s7: Unicode y marcado literal, sin HTML interpretado.
- @s1–@s16/@s24: transacción, autorización, concurrencia, eventos y errores de API son propiedad backend; frontend no sustituye sus pruebas. @s1/@s2/@s7 además se ejercitan por UI/E2E.

17. Hallazgo del juez@s22: enlace Editar desaparece y foco acaba en body. RED reproducido en test de navegación; GREEN h1 recibe foco sólo al montar Editor. Test focalizado y build posteriores verdes. Es cambio de JSX fuera de los tres archivos instrumentados por Stryker27599; sus fuentes siguen iguales. Total declarado116, ejecución completa previa115 y caso posterior focalizado, sin atribuir nueva suite completa.


## Cierre frontend para revisión

Mutación completa27599 exit0:209/255 (81,96 %),45 supervivientes y1 sin cobertura, cero timeout/errores. Replay44798 limitado a42 mutantes elimina36; último replay43711 de línea86 elimina3/3. No se suman ejecuciones ni se cambia el denominador global. Seis nuevos casos de regresión eliminan16 huecos observables iniciales; los29 supervivientes restantes y el NoCoverage inicial se explican individualmente en progress/mutation_edit_project_frontend.md. El NoCoverage original250/línea97 ahora queda cubierto y resulta equivalente en el formulario actual.

Init raíz8183 confirma122 pruebas frontend completas y lint verdes (240 backend también verdes). Lint posterior al último ejemplo42 vuelve a pasar. Producción y tests congelados; no quedan implementaciones pendientes del autor. El juez y el coordinador conservan el cierre global. La matriz final de navegador/30 principios pertenece a integración en progress/ux_edit_project.md; sus límites de dispositivo físico y comprensión humana se mantienen.
