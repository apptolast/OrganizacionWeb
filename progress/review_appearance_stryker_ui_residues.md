# Residuos Stryker20: estado compartido y formulario

Revisión sólo lectura del original, sin pruebas, cambios ni replay.
Raw frontend/reports/mutation-appearance/mutation.json y copia preservada
progress/appearance_stryker_original/mutation.json tienen el mismo SHA
56D3A57C3393400EFDD98CA463CFB2B0C0E5274D4E878793FBA15EBB49D60122 (dc58fc).
State:103K/28S/6NC; formulario177K/34S/2NC. Este informe clasifica exactamente
62S+8NC, no modifica sus estados ni suma equivalencias al score85,27%.
Las líneas se refieren al source incluido en ese JSON.

## Refuerzos mínimos propuestos, pendientes de autorización

1. **Privacidad después de desmontar,441/443.** Reutilizar el caso de retirar
   overrides: cargar SYSTEM, desmontar y emitir tanto change del MediaQueryList
   antiguo como visibilitychange. Comprobar después de cada evento que no
   reaparecen tema/acento privados. Hoy el caso usa DARK y sólo mira la retirada
   inmediata; las seis filas Ana/Bruno comprueban HTTP/JSON, no estas escuchas.
2. **Lectura anterior mientras PUT sigue pendiente,352.** Reforzar el caso
   compartido de GET anterior: mantener PUT diferido, entregar HTTP401 antiguo
   antes de confirmar y comprobar que no revoca acceso ni restaura datos.
   El caso actual entrega200 después del PUT; retireRead al confirmar oculta
   la falta de invalidación al comenzar. Es contrato de coordinación ya probado
   mediante consumidor público del contexto; no añadir un proveedor alternativo.
3. **Selector oscuro y error accesible,623/620/633, y527.** Ampliar el caso de
   selectores existente: cambiar el oscuro a otro color válido y comprobar la
   muestra; después elegir #000000 sin contraste, conservar hex/picker, muestra
   segura, aria-invalid y descripción enlazada al error. Hoy se prueba inválido
   local sólo claro; el oscuro se edita por hexadecimal válido o error servidor.
4. **SYSTEM consulta correcta,412.** En el caso existente hacer que el stub de
   matchMedia distinga su query: sólo '(prefers-color-scheme: dark)' devuelve
   la coincidencia oscura. Verificar el tema visible. El mock actual devuelve
   el mismo objeto para cualquier cadena y deja pasar query vacía.

No se propone una prueba por mutante. Son oráculos inicialmente GREEN esperables
sobre producto correcto; sólo un RED real justificaría producción. Otros huecos
menores quedan inventariados abajo, sin asumir equivalencia ni perseguir100%.

## appearance-state.tsx: todos los S/NC

| IDs (estado) | Mutador / línea | Clasificación y fundamento |
| --- | --- | --- |
|307S;308S,309S,310S;311NC,314NC;313NC,316NC|ObjectLiteral/BooleanLiteral/BlockStatement/StringLiteral,27–36|Contexto por defecto sin Provider: no hay consumidor de producción fuera del Provider autenticado. Ramas de defensa sin cobertura; no añadir una pantalla inexistente para ejecutarlas. No equivalencia universal de la API exportada.|
|320S|BooleanLiteral,42|reading inicial false no cambia el loading de Appearance sin snapshot, que depende de snapshot/failed. Antes de poder recuperar ya hay GET completo. Redundancia observable del montaje actual.|
|321S|BooleanLiteral,43|uncertain inicial true se limpia por primer GET válido antes de montar Form; sin snapshot no existe PUT. Redundancia en el recorrido actual.|
|333S|ConditionalExpression,56|Quita guardia body del foco del reintento global. Posible robo de foco si otro consumidor enfoca durante el render que retira el botón; no hay recorrido concreto acreditado en montaje actual. Límite de composición, no equivalencia general.|
|338S|ConditionalExpression,58|Quita guardia de existencia h1. Las rutas actuales renderizan main/h1 cuando desaparece el reintento; defensa para composición sin encabezado, sin nueva ruta que la demande.|
|341S|UnaryOperator,59|tabIndex -1 a+1 agrega parada/orden positivo de Tab tras reintentar. Diferenciable; el test sólo comprueba foco. P2: assert tabIndex del encabezado en el caso existente, sin otra fixture.|
|345S,346S,347S,348S|ConditionalExpression/LogicalOperator,66|Debilitan guardia Provider antes de PUT. Form sólo existe con snapshot y ya bloquea sending/uncertain; sobreviven por protección del consumidor actual. No equivalentes para llamadas libres a useAppearance; no inventar consumidores sólo para score.|
|351NC|StringLiteral,67|Mensaje de rechazo interno de esa guardia, no mostrado por Form. No prioridad observable de usuario.|
|352S|CallExpression,68|Elimina retireRead al comenzar PUT. Hueco de orden temporal descrito en prioridad2. Retirada al confirmar no protege un401 que termina antes.|
|354S,379S|CallExpression,77/100|Suprimen guards después de await: cliente ya protege HTTP/JSON y callbacks desmontados no vuelven a pintar un Provider muerto. Existe una frontera microtask adicional, pero no declaro equivalencia general; no se ha demostrado un oráculo diferente a los seis escenarios privados ya ejecutados.|
|366S|ConditionalExpression,89|Finalmente limpia writeRequest sin identidad. Bajo guardia vigente sólo hay una escritura y cleanup aborta al desmontar; un Provider nuevo tiene otra ref. Redundancia bajo ese invariante, no bajo dos mutaciones simultáneas.|
|373S,396S,453S|ArrayDeclaration,92/115/157|[] por constante o [load] por[]: deps constantes y load estable por useCallback[]. Mismo ciclo de vida en esta fuente, equivalencia local.|
|376S|ConditionalExpression,94|Retira rechazo de lectura concurrente. UI oculta reintento inicial al iniciar y Form bloquea reading; protección redundante para usuario actual, pero no para clientes libres del contexto.|
|377NC|StringLiteral,95|Mensaje de consulta concurrente rechazado, no mostrado al usuario. No test de texto interno.|
|385S|ConditionalExpression,106|Un aborto pasa a anunciar failed. Diferenciable en consumidor que expone fallo durante la carrera GET/PUT; los tests compartidos existentes sólo observan tema. P2: comprobar ausencia de error obsoleto en el mismo recorrido de prioridad2 si se compone Appearance, sin crear otra matriz.|
|390S|ConditionalExpression,110|Un finally viejo puede limpiar ref/reading de lectura nueva. Las lecturas públicas se serializan; hueco de composición en carreras de callback, no equivalencia absoluta ni defecto observado del original.|
|407S|ConditionalExpression,124|Obtiene MediaQueryList incluso en tema fijo; la fórmula posterior sigue ignorándolo en LIGHT/DARK. Añade escucha innecesaria sin cambio de tema/PUT; redundancia observable, no mismo coste de recursos.|
|412S|StringLiteral,125|Query de medio vacía; navegador real no sigue dark como contrato. Prioridad4; mock actual no distingue query.|
|422S|ConditionalExpression,130|SYSTEM pasa a true, pero media sólo existe para SYSTEM. LIGHT sigue sin coincidencia y DARK ya resuelve por primera rama. Equivalencia local por condición de construcción de media.|
|441S,443S|StringLiteral,144/145|Evento de removeEventListener vacío: callbacks antiguos sobreviven y pueden volver a escribir estilos tras logout/desmontaje. Prioridad1; no se confunde con los401 ya cubiertos.|

## appearance.tsx: todos los S/NC

| IDs (estado) | Mutador / línea | Clasificación y fundamento |
| --- | --- | --- |
|465S,511S|OptionalChaining,21/66|h1 existe incondicionalmente en esos layout effects del árbol actual. Equivalencia local quitar optional, no promesa sobre un ref externo arbitrario.|
|466S,497S|ArrayDeclaration,22/60|[] por array constante: mismos renders del efecto, equivalencia local.|
|467S|UnaryOperator,25|h1 tabIndex+1 altera teclado aunque conserve foco. P2 junto341: aserción del atributo en caso existente, no nuevo recorrido.|
|490S|ConditionalExpression,55|Limpia iniciador incluso si vuelve a recibir foco él mismo. Volver al iniciador después de mover/blur puede diferir; las filas actuales no vuelven a enfocarlo. P2 de foco, no equivalencia.|
|495S,496S|ArrowFunction/StringLiteral,59|No retiran focusin al desmontar. Fuga de escucha retenida; sólo modifica una ref privada ya sin efecto render, no repinta datos. Límite de recursos, distinto de441/443 que sí escriben documento.|
|505S,507S,509S|ConditionalExpression/LogicalOperator,65|Debilitan guardia de foco. Cuando uncertain se limpia, botón desaparece; un focusin deliberado previo limpia la ref. Bajo ambas condiciones vigentes la guardia es redundante. No basta alegar falta de fila 'otro control' para afirmar que estos mutantes la rompen: esa fila seguiría protegida por listener.|
|524S,587S|ObjectLiteral,85/143|Editar un campo borra todos los errores de servidor en vez de sólo el editado. Hueco real, menor: conservar errores de otro campo al corregir uno; puede reforzarse el fixture400 con tres errores existente si root lo prioriza.|
|519S,532S|StringLiteral,82/86|DARK por cadena vacía: helper usa LIGHT ? superficiesLight : superficiesDark; resultado actual idéntico para cualquier valor distinto deLIGHT. Equivalencia local del helper, no relajar tipos de entrada.|
|527S|ConditionalExpression,86|Selecciona validaciónLIGHT para editar ambos campos: acento oscuro válido no actualiza muestra. Incluido prioridad3.|
|543S,545S|ConditionalExpression,91/92|No detecta borrador cambiado sólo en acento claro/oscuro. Falta advertencia al abandonar, aunque sí conserva edición/guardado. P2: assert aviso en caso local que sólo edita color.|
|548S|CallExpression,97|Quita preventDefault: en navegador el submit nativo puede navegar/revelar campos por query. No equivalente; nominal E2E real exige éxito visible y recarga, pero E2E no participa en Stryker. JSDOM no ejecuta navegación nativa. No se propone suite nueva sólo para reproducir limitación del motor.|
|564S|CallExpression,104|No copia confirmación canónica al draft. Con intención comparada sólo difieren mayúsculas del color; formulario podría seguir mostrando borrador cambiado tras éxito. P2 de normalización, sin pérdida de escritura.|
|567S,690S|ObjectLiteral,106–109/280–283|Vacía preview tras guardar/recuperar; se pierde acento propio de cada muestra y puede heredarse el global. Hueco observable de muestras: reforzar asserts del caso de recuperación con colores nuevos si root lo prioriza. No equivale a validar sólo inputs.|
|591S,617S|MethodExpression,161–164/193–196|Uppercase en input type=color: navegador normaliza valor a lowercase; mismo color para hex válido. Equivalencia del control nativo, no de texto serializado de atributo.|
|592S,593S,618S,619S|Regex,161/193|Sin ancla acepta prefijo/sufijo basura para picker; navegador lo normaliza a negro en vez del valor seguro anterior. Hex/error/preview continúan protegidos. P2 de coherencia del selector, no pérdida de datos.|
|620S|Regex,193|Acepta un dígito en vez de seis: picker oscuro no representa hex válido sin contraste y conserva seguro. Incluido prioridad3.|
|623NC|StringLiteral,197|Evento del selector oscuro escribe campo vacío; no hay interacción nativa oscura en los tests actuales. Prioridad3, sin declarar NC muerto.|
|604S,630S|ConditionalExpression,176/208|aria-describedby siempre apunta al error, también sin nodo de error. Diferenciable referencia ARIA obsoleta, pero no cambia texto visible ni error válido. P2, no equivalencia.|
|633S|BooleanLiteral,208|Invierte darkValid en condición de descripción: error oscuro local no queda descrito. Prioridad3, integridad accesible.|
|642NC|StringLiteral,216|Mensaje de contraste oscuro local desaparece. Mismo hueco de prioridad3; servidor usa mensaje distinto y no ejecutaba esa rama.|
|667S|StringLiteral,238|Restaurar omite color oscuro de la muestra. P2: comprobar muestra oscura en caso Restaurar existente; campos y guardado ya cubiertos.|
|685S|ConditionalExpression,274|Quita bloqueo local de segunda consulta, pero Provider rechaza readRequest activo antes de HTTP. Mismo número de solicitudes y estado visible en UI actual. Redundancia de capas, no equivalencia de la función aislada.|

## Límites del dictamen

No defecto de producción nueva demostrado en esta lectura. Las prioridades
son huecos diferenciables del conjunto de oráculos actual. La fila de foco
'sigue en otro control' aún carece de aserción final independiente, pero los
residuos505/507/509 conservan la otra defensa: no prometer que añadirla mata
esos IDs. Los refuerzos deben conservar también movimiento seguido de body.
No se reejecutó Stryker, no se cambió su configuración ni se reclasificó el raw.

## Complemento solicitado por root: aviso CSRF705

SessionGate, StringLiteral705S enlínea36, cambia role="alert" porcadena vacía
cuando csrfExpired. Texto/botón siguen visibles, pero se pierde anuncio
accesible. El caso heredado `@s17 CSRF inválido recupera token por decisión y
conserva borrador sin repetir POST` (authentication.test.tsx) espera botón y
borrador; puede reforzarse con una aserción del alert y su mensaje antes de
pulsar Recuperar acceso, conservando todas las aserciones anteriores. Es un
quinto refuerzo mínimo, no nueva fixture ni cambio de producto. Su revisión
original de routing pertenece a root; no se suma705 al inventario70 de B.
