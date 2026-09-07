# Inventario de residuos original Stryker19

Identificación conservada por archivo, ID, ubicación, mutador y replacement. Clasificación de lectura, no replay ni cambio del estado original. Fuente completa/contexto/statusReason en weekly_review_stryker_inventory.json.

| ID | Archivo | Estado | Línea:columna | Mutador | Replacement | Familia / límite |
| --- | --- | --- | --- | --- | --- | --- |
| 0 | src/App.tsx | Timeout | 15:24 | Regex | /\/revision-semanal(?:\?[^#]*)?$/ | T: presupuesto de ejecución agotado; causa interna no registrada |
| 1 | src/App.tsx | Timeout | 15:24 | Regex | /^\/revision-semanal(?:\?[^#]*)?/ | T: presupuesto de ejecución agotado; causa interna no registrada |
| 9 | src/App.tsx | Timeout | 33:36 | StringLiteral | "" | T: presupuesto de ejecución agotado; causa interna no registrada |
| 97 | src/weekly-review-api.ts | Survived | 73:7 | ConditionalExpression | false | API: coherencia de zona fallback (diferenciable) |
| 99 | src/weekly-review-api.ts | Survived | 73:28 | StringLiteral | "" | API: coherencia de zona fallback (diferenciable) |
| 155 | src/weekly-review-api.ts | Survived | 93:7 | ConditionalExpression | false | API: lunes civil; fixture actual contradice también secuencia |
| 156 | src/weekly-review-api.ts | Survived | 93:7 | LogicalOperator | monday === null && ((monday + 3n) % 7n + 7n) % 7n !== 0n | API: lunes civil; fixture actual contradice también secuencia |
| 157 | src/weekly-review-api.ts | Survived | 93:7 | ConditionalExpression | false | API: lunes civil; fixture actual contradice también secuencia |
| 159 | src/weekly-review-api.ts | Survived | 93:26 | ConditionalExpression | false | API: lunes civil; fixture actual contradice también secuencia |
| 162 | src/weekly-review-api.ts | Survived | 93:27 | ArithmeticOperator | (monday + 3n) % 7n - 7n | API: módulo ±7 conserva comprobación de resto cero |
| 163 | src/weekly-review-api.ts | Survived | 93:28 | ArithmeticOperator | (monday + 3n) * 7n | API: lunes civil; fixture actual contradice también secuencia |
| 170 | src/weekly-review-api.ts | Survived | 96:5 | ConditionalExpression | false | API: domingo final (diferenciable) |
| 185 | src/weekly-review-api.ts | Survived | 103:5 | ConditionalExpression | false | API: guardas null/continuidad redundantes entre extremos; no equivalencia global certificada |
| 186 | src/weekly-review-api.ts | Survived | 103:5 | LogicalOperator | start === null && end === null | API: guardas null/continuidad redundantes entre extremos; no equivalencia global certificada |
| 187 | src/weekly-review-api.ts | Survived | 103:5 | ConditionalExpression | false | API: guardas null/continuidad redundantes entre extremos; no equivalencia global certificada |
| 189 | src/weekly-review-api.ts | Survived | 104:5 | ConditionalExpression | false | API: guardas null/continuidad redundantes entre extremos; no equivalencia global certificada |
| 201 | src/weekly-review-api.ts | Survived | 109:9 | ConditionalExpression | true | API: guardas null/continuidad redundantes entre extremos; no equivalencia global certificada |
| 202 | src/weekly-review-api.ts | Survived | 109:9 | LogicalOperator | left !== null \|\| right !== null | API: guardas null/continuidad redundantes entre extremos; no equivalencia global certificada |
| 203 | src/weekly-review-api.ts | Survived | 109:9 | ConditionalExpression | true | API: guardas null/continuidad redundantes entre extremos; no equivalencia global certificada |
| 205 | src/weekly-review-api.ts | Survived | 110:9 | ConditionalExpression | true | API: guardas null/continuidad redundantes entre extremos; no equivalencia global certificada |
| 216 | src/weekly-review-api.ts | Survived | 113:10 | ConditionalExpression | true | API: guardas null/continuidad redundantes entre extremos; no equivalencia global certificada |
| 232 | src/weekly-review-api.ts | Survived | 121:7 | ConditionalExpression | false | API: fecha solicitada anterior/inválida/domingo (diferenciable) |
| 233 | src/weekly-review-api.ts | Survived | 121:7 | LogicalOperator | requestedDay === null && requestedDay < monday | API: fecha solicitada anterior/inválida/domingo (diferenciable) |
| 234 | src/weekly-review-api.ts | Survived | 121:7 | ConditionalExpression | false | API: fecha solicitada anterior/inválida/domingo (diferenciable) |
| 236 | src/weekly-review-api.ts | Survived | 122:7 | ConditionalExpression | false | API: fecha solicitada anterior/inválida/domingo (diferenciable) |
| 240 | src/weekly-review-api.ts | Survived | 123:7 | EqualityOperator | requestedDay >= monday + 6n | API: fecha solicitada anterior/inválida/domingo (diferenciable) |
| 252 | src/weekly-review-api.ts | Survived | 128:30 | ConditionalExpression | false | API: límite inferior de serverNow (diferenciable) |
| 253 | src/weekly-review-api.ts | Survived | 128:30 | EqualityOperator | now <= start | API: límite inferior de serverNow (diferenciable) |
| 271 | src/weekly-review-api.ts | Survived | 138:6 | MethodExpression | days.some(day => day.capacityMicroseconds === null === (totals.capacityMicroseconds === null)) | API: capacidad mezclada null/conocida (diferenciable) |
| 292 | src/weekly-review-api.ts | Survived | 147:31 | ArithmeticOperator | sum - BigInt(day.capacityMicroseconds!) | API: suma positiva de capacidad (diferenciable) |
| 302 | src/weekly-review-api.ts | Survived | 156:5 | LogicalOperator | typeof value === "string" \|\| value.length <= 19 | API: tipo/longitud antes de BigInt; coerción o límites (diferenciable o guardas solapadas) |
| 303 | src/weekly-review-api.ts | Survived | 156:5 | ConditionalExpression | true | API: tipo/longitud antes de BigInt; coerción o límites (diferenciable o guardas solapadas) |
| 306 | src/weekly-review-api.ts | Survived | 157:5 | ConditionalExpression | true | API: tipo/longitud antes de BigInt; coerción o límites (diferenciable o guardas solapadas) |
| 316 | src/weekly-review-api.ts | Survived | 159:6 | ConditionalExpression | false | API: tipo/longitud antes de BigInt; coerción o límites (diferenciable o guardas solapadas) |
| 335 | src/weekly-review-api.ts | Survived | 171:7 | ConditionalExpression | false | API: léxico civil y validador microseconds delegado; oráculo aislado pendiente |
| 336 | src/weekly-review-api.ts | Survived | 171:7 | LogicalOperator | typeof value !== "string" && !/^(?!0000)\d{4}-\d{2}-\d{2}$/.test(value) | API: léxico civil y validador microseconds delegado; oráculo aislado pendiente |
| 337 | src/weekly-review-api.ts | Survived | 171:7 | ConditionalExpression | false | API: léxico civil y validador microseconds delegado; oráculo aislado pendiente |
| 341 | src/weekly-review-api.ts | Survived | 171:37 | Regex | /(?!0000)\d{4}-\d{2}-\d{2}$/ | API: léxico civil y validador microseconds delegado; oráculo aislado pendiente |
| 342 | src/weekly-review-api.ts | Survived | 171:37 | Regex | /^(?!0000)\d{4}-\d{2}-\d{2}/ | API: léxico civil y validador microseconds delegado; oráculo aislado pendiente |
| 352 | src/weekly-review-api.ts | Survived | 174:10 | ConditionalExpression | false | API: léxico civil y validador microseconds delegado; oráculo aislado pendiente |
| 368 | src/weekly-review.tsx | Survived | 28:5 | CallExpression | ; | UI: error anterior enmascarado por route/refresh y limpieza al cambiar selección |
| 388 | src/weekly-review.tsx | Survived | 37:13 | ArrowFunction | () => undefined | UI: aborto de catálogo al desmontar (privacidad diferenciable) |
| 389 | src/weekly-review.tsx | Survived | 37:19 | ArrowFunction | () => undefined | UI: aborto de catálogo al desmontar (privacidad diferenciable) |
| 391 | src/weekly-review.tsx | Survived | 37:54 | ArrayDeclaration | ["Stryker was here"] | UI: dependencia literal constante equivale a lista estable |
| 392 | src/weekly-review.tsx | Survived | 39:3 | CallExpression | ; | UI: foco movido y limpieza del listener (diferenciable/límite de recurso) |
| 393 | src/weekly-review.tsx | Survived | 39:19 | BlockStatement | {} | UI: foco movido y limpieza del listener (diferenciable/límite de recurso) |
| 394 | src/weekly-review.tsx | Survived | 40:42 | BlockStatement | {} | UI: foco movido y limpieza del listener (diferenciable/límite de recurso) |
| 395 | src/weekly-review.tsx | Survived | 41:11 | ConditionalExpression | true | UI: foco movido y limpieza del listener (diferenciable/límite de recurso) |
| 396 | src/weekly-review.tsx | Survived | 41:11 | ConditionalExpression | false | UI: foco movido y limpieza del listener (diferenciable/límite de recurso) |
| 397 | src/weekly-review.tsx | Survived | 41:11 | EqualityOperator | event.target === initiator.current | UI: foco movido y limpieza del listener (diferenciable/límite de recurso) |
| 399 | src/weekly-review.tsx | Survived | 43:31 | StringLiteral | "" | UI: foco movido y limpieza del listener (diferenciable/límite de recurso) |
| 400 | src/weekly-review.tsx | Survived | 44:12 | ArrowFunction | () => undefined | UI: foco movido y limpieza del listener (diferenciable/límite de recurso) |
| 401 | src/weekly-review.tsx | Survived | 44:47 | StringLiteral | "" | UI: foco movido y limpieza del listener (diferenciable/límite de recurso) |
| 402 | src/weekly-review.tsx | Survived | 45:6 | ArrayDeclaration | ["Stryker was here"] | UI: dependencia literal constante |
| 406 | src/weekly-review.tsx | Survived | 47:9 | ConditionalExpression | false | UI: oportunidad/condiciones de restitución de foco (diferenciable) |
| 409 | src/weekly-review.tsx | Survived | 51:7 | LogicalOperator | control && !control.isConnected \|\| document.activeElement === document.body | UI: oportunidad/condiciones de restitución de foco (diferenciable) |
| 410 | src/weekly-review.tsx | Survived | 51:7 | ConditionalExpression | true | UI: oportunidad/condiciones de restitución de foco (diferenciable) |
| 413 | src/weekly-review.tsx | Survived | 53:7 | ConditionalExpression | true | UI: oportunidad/condiciones de restitución de foco (diferenciable) |
| 415 | src/weekly-review.tsx | Survived | 55:7 | OptionalChaining | heading.current.focus | UI: h1 montado al ejecutar efecto, optional guard defensiva |
| 419 | src/weekly-review.tsx | Survived | 59:5 | OptionalChaining | heading.current.focus | UI: h1 montado al ejecutar efecto, optional guard defensiva |
| 420 | src/weekly-review.tsx | Survived | 60:6 | ArrayDeclaration | ["Stryker was here"] | UI: dependencia literal constante |
| 426 | src/weekly-review.tsx | Survived | 68:13 | ConditionalExpression | true | UI: respuesta obsoleta/clasificación400/409; guardas parcialmente solapadas, no todas equivalentes |
| 432 | src/weekly-review.tsx | Survived | 71:13 | ConditionalExpression | false | UI: respuesta obsoleta/clasificación400/409; guardas parcialmente solapadas, no todas equivalentes |
| 433 | src/weekly-review.tsx | Survived | 74:11 | ConditionalExpression | true | UI: respuesta obsoleta/clasificación400/409; guardas parcialmente solapadas, no todas equivalentes |
| 435 | src/weekly-review.tsx | Survived | 74:11 | LogicalOperator | status === 400 \|\| status === 409 \|\| error instanceof Response | UI: respuesta obsoleta/clasificación400/409; guardas parcialmente solapadas, no todas equivalentes |
| 436 | src/weekly-review.tsx | Survived | 74:12 | ConditionalExpression | true | UI: respuesta obsoleta/clasificación400/409; guardas parcialmente solapadas, no todas equivalentes |
| 442 | src/weekly-review.tsx | Survived | 75:40 | ArrowFunction | () => undefined | UI: respuesta obsoleta/clasificación400/409; guardas parcialmente solapadas, no todas equivalentes |
| 444 | src/weekly-review.tsx | Survived | 77:13 | ConditionalExpression | false | UI: respuesta obsoleta/clasificación400/409; guardas parcialmente solapadas, no todas equivalentes |
| 456 | src/weekly-review.tsx | Survived | 80:11 | ConditionalExpression | true | UI: respuesta obsoleta/clasificación400/409; guardas parcialmente solapadas, no todas equivalentes |
| 460 | src/weekly-review.tsx | Survived | 82:11 | ConditionalExpression | true | UI: respuesta obsoleta/clasificación400/409; guardas parcialmente solapadas, no todas equivalentes |
| 468 | src/weekly-review.tsx | Survived | 86:17 | ConditionalExpression | true | UI: respuesta obsoleta/clasificación400/409; guardas parcialmente solapadas, no todas equivalentes |
| 469 | src/weekly-review.tsx | Survived | 86:17 | LogicalOperator | entry && typeof entry === "object" \|\| entry.code === "INVALID_VALUE" | UI: respuesta obsoleta/clasificación400/409; guardas parcialmente solapadas, no todas equivalentes |
| 470 | src/weekly-review.tsx | Survived | 86:17 | ConditionalExpression | true | UI: respuesta obsoleta/clasificación400/409; guardas parcialmente solapadas, no todas equivalentes |
| 471 | src/weekly-review.tsx | Survived | 86:17 | LogicalOperator | entry \|\| typeof entry === "object" | UI: respuesta obsoleta/clasificación400/409; guardas parcialmente solapadas, no todas equivalentes |
| 472 | src/weekly-review.tsx | Survived | 87:17 | ConditionalExpression | true | UI: respuesta obsoleta/clasificación400/409; guardas parcialmente solapadas, no todas equivalentes |
| 475 | src/weekly-review.tsx | Survived | 88:17 | ConditionalExpression | true | UI: respuesta obsoleta/clasificación400/409; guardas parcialmente solapadas, no todas equivalentes |
| 499 | src/weekly-review.tsx | Survived | 100:13 | LogicalOperator | status === 409 \|\| body | UI: respuesta obsoleta/clasificación400/409; guardas parcialmente solapadas, no todas equivalentes |
| 500 | src/weekly-review.tsx | Survived | 100:13 | ConditionalExpression | true | UI: respuesta obsoleta/clasificación400/409; guardas parcialmente solapadas, no todas equivalentes |
| 502 | src/weekly-review.tsx | Survived | 102:13 | ConditionalExpression | true | UI: respuesta obsoleta/clasificación400/409; guardas parcialmente solapadas, no todas equivalentes |
| 506 | src/weekly-review.tsx | Survived | 104:13 | ConditionalExpression | true | UI: respuesta obsoleta/clasificación400/409; guardas parcialmente solapadas, no todas equivalentes |
| 511 | src/weekly-review.tsx | Survived | 109:13 | ConditionalExpression | true | UI: respuesta obsoleta/clasificación400/409; guardas parcialmente solapadas, no todas equivalentes |
| 519 | src/weekly-review.tsx | Survived | 114:9 | ConditionalExpression | false | UI: catálogo pendiente/reintento/aborto y anuncios (diferenciable) |
| 520 | src/weekly-review.tsx | Survived | 114:9 | LogicalOperator | zones && zoneLookup.current | UI: catálogo pendiente/reintento/aborto y anuncios (diferenciable) |
| 522 | src/weekly-review.tsx | Survived | 117:20 | BooleanLiteral | true | UI: catálogo pendiente/reintento/aborto y anuncios (diferenciable) |
| 527 | src/weekly-review.tsx | Survived | 121:13 | ConditionalExpression | true | UI: catálogo pendiente/reintento/aborto y anuncios (diferenciable) |
| 532 | src/weekly-review.tsx | Survived | 124:13 | ConditionalExpression | true | UI: catálogo pendiente/reintento/aborto y anuncios (diferenciable) |
| 537 | src/weekly-review.tsx | Survived | 127:13 | ConditionalExpression | true | UI: catálogo pendiente/reintento/aborto y anuncios (diferenciable) |
| 540 | src/weekly-review.tsx | Survived | 128:13 | BooleanLiteral | controller.signal.aborted | UI: catálogo pendiente/reintento/aborto y anuncios (diferenciable) |
| 541 | src/weekly-review.tsx | Survived | 128:13 | ConditionalExpression | true | UI: catálogo pendiente/reintento/aborto y anuncios (diferenciable) |
| 542 | src/weekly-review.tsx | Survived | 128:13 | ConditionalExpression | false | UI: catálogo pendiente/reintento/aborto y anuncios (diferenciable) |
| 544 | src/weekly-review.tsx | Survived | 128:56 | BooleanLiteral | true | UI: catálogo pendiente/reintento/aborto y anuncios (diferenciable) |
| 559 | src/weekly-review.tsx | Survived | 139:71 | UnaryOperator | +1 | UI: tabIndex positivo cambia orden teclado (diferenciable) |
| 560 | src/weekly-review.tsx | Survived | 140:37 | UnaryOperator | +1 | UI: tabIndex positivo cambia orden teclado (diferenciable) |
| 561 | src/weekly-review.tsx | Survived | 153:17 | UnaryOperator | +1 | UI: tabIndex positivo cambia orden teclado (diferenciable) |
| 564 | src/weekly-review.tsx | Survived | 156:11 | ConditionalExpression | false | UI: captura de enlaces/modificadores/destino y foco (diferenciable o máscara por RouteLink) |
| 565 | src/weekly-review.tsx | Survived | 156:11 | LogicalOperator | (event.button !== 0 \|\| event.ctrlKey \|\| event.metaKey \|\| event.shiftKey) && event.altKey | UI: captura de enlaces/modificadores/destino y foco (diferenciable o máscara por RouteLink) |
| 566 | src/weekly-review.tsx | Survived | 156:11 | ConditionalExpression | false | UI: captura de enlaces/modificadores/destino y foco (diferenciable o máscara por RouteLink) |
| 567 | src/weekly-review.tsx | Survived | 156:11 | LogicalOperator | (event.button !== 0 \|\| event.ctrlKey \|\| event.metaKey) && event.shiftKey | UI: captura de enlaces/modificadores/destino y foco (diferenciable o máscara por RouteLink) |
| 568 | src/weekly-review.tsx | Survived | 156:11 | ConditionalExpression | false | UI: captura de enlaces/modificadores/destino y foco (diferenciable o máscara por RouteLink) |
| 569 | src/weekly-review.tsx | Survived | 156:11 | LogicalOperator | (event.button !== 0 \|\| event.ctrlKey) && event.metaKey | UI: captura de enlaces/modificadores/destino y foco (diferenciable o máscara por RouteLink) |
| 570 | src/weekly-review.tsx | Survived | 156:11 | ConditionalExpression | false | UI: captura de enlaces/modificadores/destino y foco (diferenciable o máscara por RouteLink) |
| 571 | src/weekly-review.tsx | Survived | 156:11 | LogicalOperator | event.button !== 0 && event.ctrlKey | UI: captura de enlaces/modificadores/destino y foco (diferenciable o máscara por RouteLink) |
| 572 | src/weekly-review.tsx | Survived | 156:11 | ConditionalExpression | false | UI: captura de enlaces/modificadores/destino y foco (diferenciable o máscara por RouteLink) |
| 575 | src/weekly-review.tsx | RuntimeError | 165:22 | OptionalChaining | link.getAttribute | E: adaptador Vitest/Stryker no serializa excepción de optional chaining |
| 577 | src/weekly-review.tsx | Survived | 167:11 | ConditionalExpression | true | UI: captura de enlaces/modificadores/destino y foco (diferenciable o máscara por RouteLink) |
| 579 | src/weekly-review.tsx | Survived | 167:11 | LogicalOperator | link?.contains(document.activeElement) \|\| href === "/revision-semanal" \|\| href?.startsWith("/revision-semanal?") | UI: captura de enlaces/modificadores/destino y foco (diferenciable o máscara por RouteLink) |
| 580 | src/weekly-review.tsx | RuntimeError | 167:11 | OptionalChaining | link.contains | E: adaptador Vitest/Stryker no serializa excepción de optional chaining |
| 581 | src/weekly-review.tsx | Survived | 168:12 | ConditionalExpression | true | UI: captura de enlaces/modificadores/destino y foco (diferenciable o máscara por RouteLink) |
| 586 | src/weekly-review.tsx | Survived | 169:13 | MethodExpression | href?.endsWith("/revision-semanal?") | UI: captura de enlaces/modificadores/destino y foco (diferenciable o máscara por RouteLink) |
| 587 | src/weekly-review.tsx | Survived | 169:13 | OptionalChaining | href.startsWith | UI: captura de enlaces/modificadores/destino y foco (diferenciable o máscara por RouteLink) |
| 588 | src/weekly-review.tsx | Survived | 169:30 | StringLiteral | "" | UI: captura de enlaces/modificadores/destino y foco (diferenciable o máscara por RouteLink) |
| 589 | src/weekly-review.tsx | Survived | 174:35 | UnaryOperator | +1 | UI: tabIndex positivo cambia orden teclado (diferenciable) |
| 591 | src/weekly-review.tsx | Survived | 181:11 | CallExpression | ; | UI: submit nativo y captura de foco (diferenciable) |
| 592 | src/weekly-review.tsx | Survived | 183:13 | ConditionalExpression | true | UI: submit nativo y captura de foco (diferenciable) |
| 594 | src/weekly-review.tsx | Survived | 183:13 | LogicalOperator | document.activeElement instanceof HTMLElement \|\| event.currentTarget.contains(document.activeElement) | UI: submit nativo y captura de foco (diferenciable) |
| 597 | src/weekly-review.tsx | NoCoverage | 189:51 | StringLiteral | "Stryker was here!" | NC: fallback FormData null; controles nativos presentes en recorrido público |
| 604 | src/weekly-review.tsx | NoCoverage | 191:53 | StringLiteral | "Stryker was here!" | NC: fallback FormData null; controles nativos presentes en recorrido público |
| 610 | src/weekly-review.tsx | Survived | 195:13 | StringLiteral | "Stryker was here!" | UI: título pushState ignorado por navegador actual |
| 626 | src/weekly-review.tsx | Survived | 214:71 | StringLiteral | "Stryker was here!" | UI: ayudas accesibles/aria-invalid/opción de zona/catálogo (diferenciable) |
| 635 | src/weekly-review.tsx | Survived | 226:65 | BooleanLiteral | true | UI: ayudas accesibles/aria-invalid/opción de zona/catálogo (diferenciable) |
| 642 | src/weekly-review.tsx | Survived | 235:73 | StringLiteral | "Stryker was here!" | UI: ayudas accesibles/aria-invalid/opción de zona/catálogo (diferenciable) |
| 646 | src/weekly-review.tsx | Survived | 240:14 | ConditionalExpression | true | UI: ayudas accesibles/aria-invalid/opción de zona/catálogo (diferenciable) |
| 647 | src/weekly-review.tsx | Survived | 240:14 | LogicalOperator | filters.get("zoneId") \|\| !zones?.includes(filters.get("zoneId")!) | UI: ayudas accesibles/aria-invalid/opción de zona/catálogo (diferenciable) |
| 651 | src/weekly-review.tsx | Survived | 241:44 | StringLiteral | "" | UI: ayudas accesibles/aria-invalid/opción de zona/catálogo (diferenciable) |
| 652 | src/weekly-review.tsx | Survived | 242:44 | StringLiteral | "" | UI: ayudas accesibles/aria-invalid/opción de zona/catálogo (diferenciable) |
| 653 | src/weekly-review.tsx | Survived | 243:32 | StringLiteral | "" | UI: ayudas accesibles/aria-invalid/opción de zona/catálogo (diferenciable) |
| 662 | src/weekly-review.tsx | Survived | 255:58 | StringLiteral | "" | UI: ayudas accesibles/aria-invalid/opción de zona/catálogo (diferenciable) |
| 681 | src/weekly-review.tsx | Survived | 275:62 | StringLiteral | "" | UI: mensajes/datos anteriores/tiempos/presupuesto/resto (diferenciable; textos no equivalentes) |
| 689 | src/weekly-review.tsx | Survived | 280:21 | ConditionalExpression | true | UI: guardas de retry/foco (diferenciable o control desaparecido) |
| 692 | src/weekly-review.tsx | Survived | 281:23 | ConditionalExpression | true | UI: guardas de retry/foco (diferenciable o control desaparecido) |
| 695 | src/weekly-review.tsx | Survived | 283:30 | ArrowFunction | () => undefined | UI: segundo reintento/actualización no cambia undefined (diferenciable) |
| 696 | src/weekly-review.tsx | Survived | 283:41 | ArithmeticOperator | value - 1 | UI: contador decreciente también invalida generaciones; no se muestra |
| 699 | src/weekly-review.tsx | Survived | 297:13 | StringLiteral | "Stryker was here!" | UI: mensajes/datos anteriores/tiempos/presupuesto/resto (diferenciable; textos no equivalentes) |
| 708 | src/weekly-review.tsx | Survived | 305:40 | ArrowFunction | () => undefined | UI: segundo reintento/actualización no cambia undefined (diferenciable) |
| 709 | src/weekly-review.tsx | Survived | 305:51 | ArithmeticOperator | value - 1 | UI: contador decreciente también invalida generaciones; no se muestra |
| 713 | src/weekly-review.tsx | Survived | 311:15 | ConditionalExpression | true | UI: mensajes/datos anteriores/tiempos/presupuesto/resto (diferenciable; textos no equivalentes) |
| 716 | src/weekly-review.tsx | Survived | 311:79 | StringLiteral | "" | UI: mensajes/datos anteriores/tiempos/presupuesto/resto (diferenciable; textos no equivalentes) |
| 722 | src/weekly-review.tsx | Survived | 350:12 | ConditionalExpression | true | UI: mensajes/datos anteriores/tiempos/presupuesto/resto (diferenciable; textos no equivalentes) |
| 724 | src/weekly-review.tsx | Survived | 350:50 | StringLiteral | "" | UI: mensajes/datos anteriores/tiempos/presupuesto/resto (diferenciable; textos no equivalentes) |
| 731 | src/weekly-review.tsx | Survived | 361:28 | StringLiteral | "" | UI: mensajes/datos anteriores/tiempos/presupuesto/resto (diferenciable; textos no equivalentes) |
| 738 | src/weekly-review.tsx | Survived | 370:55 | StringLiteral | "" | UI: mensajes/datos anteriores/tiempos/presupuesto/resto (diferenciable; textos no equivalentes) |
| 739 | src/weekly-review.tsx | Survived | 383:16 | ConditionalExpression | true | UI: mensajes/datos anteriores/tiempos/presupuesto/resto (diferenciable; textos no equivalentes) |
| 742 | src/weekly-review.tsx | Survived | 384:19 | StringLiteral | "" | UI: mensajes/datos anteriores/tiempos/presupuesto/resto (diferenciable; textos no equivalentes) |
| 744 | src/weekly-review.tsx | Survived | 395:39 | StringLiteral | "" | UI: mensajes/datos anteriores/tiempos/presupuesto/resto (diferenciable; textos no equivalentes) |
| 745 | src/weekly-review.tsx | Survived | 396:20 | ConditionalExpression | true | UI: mensajes/datos anteriores/tiempos/presupuesto/resto (diferenciable; textos no equivalentes) |
| 748 | src/weekly-review.tsx | Survived | 397:23 | StringLiteral | "" | UI: mensajes/datos anteriores/tiempos/presupuesto/resto (diferenciable; textos no equivalentes) |
| 752 | src/weekly-review.tsx | Survived | 400:18 | ConditionalExpression | true | UI: mensajes/datos anteriores/tiempos/presupuesto/resto (diferenciable; textos no equivalentes) |
| 767 | src/weekly-review.tsx | Survived | 421:10 | Regex | /(?!0000)\d{4}-\d{2}-\d{2}$/ | UI: anclas sobre salida ISO canónica de Date; extremos cubiertos |
| 768 | src/weekly-review.tsx | Survived | 421:10 | Regex | /^(?!0000)\d{4}-\d{2}-\d{2}/ | UI: anclas sobre salida ISO canónica de Date; extremos cubiertos |
| 787 | src/weekly-review.tsx | Survived | 432:5 | MethodExpression | [hours ? `${hours} h` : "", minutes ? `${minutes} min` : "", remaining ? `${seconds(String(remaining))} s` : ""] | UI: formato de duración/0 min/espacios (diferenciable) |
| 796 | src/weekly-review.tsx | Survived | 438:21 | StringLiteral | "" | UI: formato de duración/0 min/espacios (diferenciable) |
