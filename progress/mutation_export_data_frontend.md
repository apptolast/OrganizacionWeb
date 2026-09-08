# Mutación frontend de exportación 22

Campaña original única: `node scripts/project.mjs mutate export_data-frontend`, EXIT 0, 7 min 27 s. Mismo universo aprobado: dos módulos completos y seis nodos AST de integración, 8 workers, perTest, suites completas configuradas, umbral 80, sin exclusiones nuevas ni incremental. Stryker instrumentó seis archivos; SessionGate no generó mutantes para el atributo username. Dry-run seleccionó 919 pruebas por cobertura.

326 mutantes: **261 Killed, 60 Survived, 4 NoCoverage, 1 Timeout, 0 errores**. Score del motor 80,37%; conservador **261 / 326 = 80,06134969%**, supera 80 sin contar Timeout como Killed. No se reclasifica ningún estado.

Raw original: `export_stryker_original/mutation.json`, SHA256 `9082109b1571c1bcbb1a9c78641d1c085fb9fff6b4b46d79fc5d29a0592f1db5`. HTML original, log, configuración y before/after preservados. Las 170 entradas permanecen idénticas. Inventario completo con identidad, mutador, ubicación, texto y replacement: `export_stryker_inventory.json`; subconjunto residual: `export_stryker_residues.json`. Columnas del reporte son 1-based.

| Archivo | Killed | Survived | NoCoverage | Timeout |
| --- | ---: | ---: | ---: | ---: |
| export-data-api.ts | 116 | 32 | 3 | 1 |
| export-data.tsx | 96 | 28 | 1 | 0 |
| App.tsx | 41 | 0 | 0 | 0 |
| use-session.ts | 3 | 0 | 0 | 0 |
| workspace.tsx | 5 | 0 | 0 | 0 |

## Clasificación completa de residuos

### API: encabezados válidos y límites léxicos

IDs: 56, 57, 59, 60, 73, 75, 76, 101, 104, 105. Hueco observable. La suite prueba HTML/gzip/longitud cero, pero no Content-Encoding: identity válido, variaciones permitidas de espacios/caso ni prefijos/sufijos de un tipo aparentemente válido. Una longitud no canónica matemáticamente igual también puede eludir la intención léxica. Proponer variantes pequeñas del transporte nominal; no matriz general de HTTP.

### API: counts cerrado

IDs: 172, 174. Hueco contractual concreto: añadir una propiedad extra a counts con las catorce cantidades coherentes. Hoy el producto lo rechaza; el mutante 172 lo acepta. El error 174 está sin cobertura por faltar ese rechazo, no por falta de integración.

### API: mensajes internos y fallbacks

IDs: 64, 81, 66, 78, 95, 113, 132, 144, 169, 191, 201. Los textos internos no se presentan al usuario: la vista usa un mensaje propio. Fallback de cabecera ausente sigue siendo incompatible aunque cambie la cadena. No exigir literal interno para matar mutantes; no implica equivalencia de cualquier texto visible.

### API: guardias complementarias

IDs: 82, 83, 90, 92, 127, 130, 141, 182, 204. 82/83 siguen rechazados por name[0] !== disposition; 92 duplica el patrón completo para valores nativos de Headers (normaliza espacio exterior); 90 también rechaza la falta de match aunque mediante TypeError. 127/130 alcanzan Uint8Array.set, que lanza RangeError antes de devolver bytes; cancelación permanece. 141 deja ceros en el buffer truncado y JSON.parse rechaza esos bytes. 182 está respaldado por instant y correspondencia exacta del nombre. 204 sólo cambia el error interno cuando body es null, sin generar archivo. Equivalencia limitada al rechazo/bytes públicos, no a diagnósticos internos.

### API: lifecycle listener

IDs: 117, 118, 138. AbortSignal sólo emite abort una vez; once true/false no cambia ese evento. 138 deja el listener hasta abort/GC y conserva una referencia al reader; la posterior cancelación rechazada se captura. Límite de limpieza, no defecto de datos acreditado. No proponer tests internos de add/remove sólo para el porcentaje.

### UI: foco físico y foco conservado

IDs: 215, 230, 234, 235, 310. 235 no cubierto y 234 superviviente corresponden al control conectado desenfocado automáticamente al disabled. El navegador real acreditó la corrección, pero no corre dentro de Vitest. Reforzar el caso existente con simulación explícita del blur automático, conservando la variante de foco retenido y el movimiento voluntario. 215 necesita que el propio iniciador reciba focusin durante la espera sin cancelar su intención; no confundir con otro control. 230 se solapa con la guarda de intención. 310 altera tabIndex -1 por +1: foco programático sigue, pero entra en orden Tab; la evidencia de teclado externa no equivale a cobertura Stryker.

### UI: efectos constantes y referencias presentes

IDs: 210, 211, 222, 236, 245, 307. 211/222/245 sustituyen [] por una dependencia string constante y no cambian la frecuencia del efecto. 210/236: el heading está montado cuando se ejecuta el layout. 307: Cancelar sólo está presente mientras busy y pending existe; no expone otra transición pública dentro del evento. No crear oráculos de internals.

### UI: limpieza y guardias respaldadas

IDs: 220, 221, 242, 248, 258, 259, 260, 269, 270, 271. 220/221 pueden dejar listeners de instancias retiradas, aunque sólo modifican su ref inaccesible y no el estado actual; límite de recursos. 242 revoca null antes de disponer de URL, tolerado por plataforma, sin bytes expuestos. 248 se respalda en botón disabled y React vacía sus cambios entre eventos. 258–260 y 269–271 tienen protección previa del cliente y abortos de la identidad; no justificar mocks que omitan ese cliente únicamente para matar defensas redundantes. Se conserva cobertura de late401 real en composición.

### UI: recuperación y superposición de intenciones

IDs: 255, 300. Huecos observables diferenciables. 255 mantiene alerta vieja al iniciar un nuevo intento: reforzar el retry existente con alerta ausente durante respuesta retenida. 300 deja que finally de una preparación cancelada quite busy/pending de la siguiente: cancelar A, iniciar B, resolver A tarde y exigir B aún pendiente, sin tercer envío ni archivo viejo. Este caso no requiere cambiar producción si el guard actual cumple.

### UI: clasificación de 413

IDs: 275, 276, 278, 290, 294. 294 clasifica cualquier code de una respuesta 413 como límite; probar 413 con código incompatible y esperar error recuperable, nunca Blob. 275/276 pueden leer JSON de otro estado: un 503 con EXPORT_TOO_LARGE no debe convertirse en límite ni consumir cuerpo innecesariamente. 290 permite usar in sobre primitivas: variante 413 JSON primitivo demuestra fallback seguro. 278 null/undefined son falsy bajo el mismo guard, sin diferencia visible.

### UI: jerarquía visual

IDs: 317. Eliminar secondary-link altera la jerarquía de Preparar de nuevo. Verificado en captura con estilos reales; no está dentro de la suite Vitest y no se declara equivalente. Límite explícito de campaña JS.

### Tiempo agotado original

IDs: 122. BlockStatement vacía el cuerpo de while(true). Stryker informa Hit limit reached (5001/5000): bucle sin progreso introducido por el mutante. Se conserva Timeout, no Killed. El score conservador excluye esta detección.

## Propuesta mínima para revisión, sin ejecutar

No se acredita un defecto productivo nuevo. El producto actual contiene las guardas; quedan oportunidades de reforzar sus oráculos públicos. Prioridad: (1) counts con propiedad extra; (2) Content-Encoding identity válido y un encabezado incompatible que conserve apariencia nominal; (3) retry con alerta retirada y respuesta retenida; (4) cancelar A, iniciar B y resolver A tarde sin liberar B; (5) foco conectado tras blur automático, sin perder el caso de movimiento voluntario; (6) 413 con código incompatible y cuerpo primitivo, conservando recuperación segura. Reutilizar casos existentes cuando baste una aserción; inicialmente GREEN si ya cumplen. No se solicita una matriz ni 100%. No refuerzos o replay iniciados: root decide tras revisión.

La campaña no valida E2E de backend ni snapshot SQL. UX con API simulada y captura nativa de compositor quedan documentadas por separado en ux_export_data.md.
