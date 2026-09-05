# edit_project — mutación frontend

La ejecución completa del alcance editado27599 terminó exit0: **209/255 (81,96 %)**. Hubo45 supervivientes y1 mutante sin cobertura; cero timeout y errores. Esta es la puntuación de la ejecución completa conservada. Los replays posteriores no se suman para fabricar otra puntuación global.

## Alcance exacto

frontend/stryker.edit-project.config.json instrumenta src/read-projects-api.ts, src/edit-project-api.ts y src/use-edit-project.ts. Incluye toda la validación compartida de detalle, no sólo las líneas extraídas. Stryker10, Vitest4.1.10, coverageAnalysis perTest, dos workers, umbral80 sin reducción. Informes originales en frontend/reports/mutation-edit-project/mutation.json y mutation.html.

| Archivo | Eliminados / total | Supervivientes | Sin cobertura |
| --- | --- | --- | --- |
| edit-project-api.ts | 31/33 | 2 | 0 |
| read-projects-api.ts | 88/95 | 7 | 0 |
| use-edit-project.ts | 90/127 | 36 | 1 |

La configuración global añade los dos archivos nuevos para futuras ejecuciones. En este corte no se repite la mutación de creación/navegación intactas. JSX y SCSS tienen pruebas de comportamiento, navegador y revisión visual; no se les atribuye la puntuación de estos tres archivos. El foco inicial añadido después del arranque de Stryker sólo toca JSX fuera del alcance y tiene RED/GREEN propio. Las fuentes instrumentadas permanecen iguales.

## Refuerzos y replays

Se añadieron seis casos observables sin modificar producción: segundo guardado usa ETag confirmado y cancela submit nativo; error anterior desaparece al reintentar; problemas400 con null, número, errors no array o entradas incompatibles conservan borrador sin excepciones. Las pruebas focalizadas pasan. Init raíz8183 ejecutó después **122 tests frontend completos**, lint y240 backend verdes.

Replay44798: **36/42 (85,71 %)**, exit0,6 supervivientes, cero sin cobertura/timeout/errores. Alcance limitado a use-edit-project.ts líneas56,66,78 y82–97; configuración temporal .stryker-tmp/edit-project-survivors.json. Informes separados en reports/mutation-edit-project-targeted/. El replay elimina15 huecos de los supervivientes originales.

El superviviente original224 necesita además un cuerpo400 numérico: eliminar typeof body permite que el operador in lance una excepción. Se añadió el ejemplo42, verde focalizado e incluido en init122. Replay43711 exclusivo de línea86 terminó exit0: **3/3** eliminados, cero supervivientes/sin cobertura/timeout/errores. Es una reproducción puntual, no una puntuación global nueva. Sus informes separados viven en reports/mutation-edit-project-primitive/.

## Revisión individual de los45 supervivientes originales

IDs y líneas corresponden al informe completo. La equivalencia se justifica respecto al recorrido real de App, que remonta Editor por ruta, y a cuerpos JSON del contrato. No implica que dos expresiones sean idénticas para cualquier llamador u objeto JavaScript arbitrario.

| Archivo / ID / línea | Resultado y explicación |
| --- | --- |
| edit-project-api /26 /28 | Equivalente: mensaje privado de Error vacío; UI muestra el mismo fallo propio sin usar ese mensaje. |
| edit-project-api /33 /31 | Equivalente: mismo caso al rechazar DTO incompatible. |
| read-projects-api /36 /11 | Equivalente: isRecord true; validaciones restantes rechazan primitivas o producen excepción que el hook convierte en fallo seguro. |
| read-projects-api /38 /11 | Equivalente: OR admite null/primitivas pero las comprobaciones posteriores impiden datos falsos. |
| read-projects-api /39 /11 | Equivalente: retirar typeof termina en rechazo o excepción capturada con el mismo estado. |
| read-projects-api /42 /11 | Equivalente: retirar guard null produce el mismo fallo seguro en validaciones posteriores. |
| read-projects-api /114 /48 | Equivalente para JSON: items.every valida arrays; otros valores JSON no proporcionan un método ejecutable y fallan. |
| read-projects-api /115 /48 | Equivalente: el prefijo OR mantiene every y comprobaciones posteriores; mismo rechazo seguro. |
| read-projects-api /130 /55 | Equivalente: sentinel de Error no se muestra ni clasifica por mensaje. |
| use-edit-project /135 /14 | Equivalente: entrada ficticia en fields no coincide con name/description ni encuentra control que enfocar. |
| use-edit-project /143 /19 | Equivalente: ETag inicial nunca se envía; no hay formulario hasta GET válido que lo sustituye. |
| use-edit-project /156 /27 | Equivalente: loadFailure inicial se reinicia al iniciar GET; antes no hay borrador ni fallo que mostrar. |
| use-edit-project /157 /30 | Equivalente: loading inicial false se establece true en efecto; sin draft no existe submit utilizable y sigue el anuncio de carga. |
| use-edit-project /163 /34 | Equivalente: loadFailure true tras GET no cambia UI sin failure; un fallo de GET ya establece true y PUT lo reinicia. |
| use-edit-project /177 /48 | Equivalente en App: aborto ocurre al desmontar o durante StrictMode inicial sin draft. Cambiar loading de la instancia obsoleta no habilita un formulario con datos viejos. |
| use-edit-project /186 /56 | Hueco real eliminado en replay: sin preventDefault se permite navegación nativa adicional. La prueba comprueba cancelación del evento. |
| use-edit-project /195 /64 | Equivalente: placeholder al limpiar fields no coincide con controles ni errores visibles. |
| use-edit-project /198 /66 | Hueco real eliminado en replay: error antiguo persistía durante reintento y tras éxito. |
| use-edit-project /202 /76 | Equivalente observable en App: PUT sólo se aborta al desmontar/cambiar ruta; actualización posterior pertenece a instancia desmontada. Guard conservado defensivamente. |
| use-edit-project /204 /78 | Hueco real eliminado en replay: segundo PUT debe enviar el ETag recibido en la confirmación anterior. |
| use-edit-project /209 /81 | Equivalente observable: rechazo de PUT abortado sólo podría actualizar instancia desmontada; no sustituye otra ruta. |
| use-edit-project /212 /82 | Equivalente para problemas del contrato: errors sólo pertenece a400; parsear además otros Response no genera errores de campo ni cambia su clasificación. |
| use-edit-project /213 /82 | Equivalente bajo el mismo contrato: los problemas no400 carecen de errors de validación; parsearlos no cambia el resultado visible. |
| use-edit-project /216 /83 | Equivalente: null y undefined tras JSON malformado no pasan la guarda y conservan el mismo fallo400 genérico. |
| use-edit-project /219 /85 | Hueco real eliminado en replay: prefijo OR permite errors no array y provoca excepción de flatMap. |
| use-edit-project /220 /85 | Hueco real eliminado en replay: suprime guardas del cuerpo y permite acceder a null. |
| use-edit-project /221 /85 | Hueco real eliminado en replay: OR permite acceder a cuerpo null. |
| use-edit-project /222 /85 | Hueco real eliminado en replay: elimina prefijo de seguridad para null. |
| use-edit-project /223 /85 | Hueco real eliminado en replay: OR con typeof object admite null y falla después. |
| use-edit-project /224 /86 | Hueco real: retirar typeof permite operador in sobre42. Ejemplo añadido; eliminado en replay43711. |
| use-edit-project /230 /92 | Hueco real eliminado en replay: entradas null dejan de filtrarse y provocan excepción. |
| use-edit-project /232 /92 | Hueco real eliminado en replay: OR evalúa entry.field sobre null. |
| use-edit-project /233 /92 | Hueco real eliminado en replay: retirar prefijo permite acceso inseguro a entrada null. |
| use-edit-project /234 /92 | Hueco real eliminado en replay: OR permite operador in sobre entrada incompatible. |
| use-edit-project /235 /92 | Hueco real eliminado en replay: prefijo true permite in sobre null/number. |
| use-edit-project /236 /92 | Hueco real eliminado en replay: OR admite entrada null y falla en in. |
| use-edit-project /237 /93 | Hueco real eliminado en replay: sin typeof la entrada42 alcanza el operador in. |
| use-edit-project /241 /95 | Equivalente en formulario actual: campos desconocidos admitidos no coinciden con name/description ni controles enfocados; no se muestran mensajes del servidor. |
| use-edit-project /253 /103 | Equivalente observable: panel401/404 retira siempre formulario y no permite retry; navegación remonta instancia limpia. La limpieza explícita de estado se conserva como defensa. |
| use-edit-project /256 /104 | Equivalente observable por la misma retirada de formulario y remontaje al navegar; no se puede reenviar el draft oculto desde la UI. |
| use-edit-project /257 /104 | Equivalente observable para401 por el mismo aislamiento de la pantalla de acceso. |
| use-edit-project /259 /104 | Equivalente observable para404 por el mismo aislamiento de la pantalla no encontrada. |
| use-edit-project /261 /105 | Equivalente observable: borrar el bloque de limpieza no permite mostrar/reintentar datos tras401/404. Se conserva por higiene del estado. |
| use-edit-project /262 /106 | Equivalente observable: draft retenido internamente no se renderiza ni reutiliza tras pérdida de acceso; navegación crea instancia nueva. No se promete borrado físico de memoria JS. |
| use-edit-project /264 /107 | Equivalente: ETag ficticio tras401/404 jamás se envía; esos estados no ofrecen guardar y otra ruta remonta el editor. |

## El mutante originalmente sin cobertura

ID250, use-edit-project.ts línea97: cambia el array vacío de filtrado por un placeholder. La suite inicial no ejercitaba entradas individuales malformadas en errors: era un hueco de evidencia y no se ocultó en el porcentaje. Los ejemplos nuevos con null/42/objeto vacío/campo desconocido recorren esa rama. Replay44798 lo clasifica Survived con cobertura, no NoCoverage. Es equivalente en este formulario: el placeholder no coincide con name/description ni con controles nombrados, por lo que no produce errores ni foco visible. El replay completo conserva cero NoCoverage.

No se recortaron fuentes ni se redujo el umbral. No se persigue eliminar equivalentes ni se atribuye100 % al frontend.

Cierre: los16 huecos observables originales quedan eliminados en replays; los29 supervivientes restantes y el mutante originalmente sin cobertura tienen justificación de equivalencia contextual arriba. Lint final posterior a los seis refuerzos verde. La puntuación completa se conserva en209/255 (81,96 %).

