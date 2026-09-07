# Revisión de residuos Stryker20: appearance-api.ts

Lectura independiente del original SHA
`56D3A57C3393400EFDD98CA463CFB2B0C0E5274D4E878793FBA15EBB49D60122`.
Sólo `src/appearance-api.ts`:262 mutantes,226 Killed,36 Survived,0 NoCoverage.
No se alteran estados, denominadores ni el resultado global637/747.
No fuentes/tests editados, pruebas ejecutadas ni replay iniciado.

Dictamen: hay huecos de oráculo públicos concretos; no se demuestra un defecto
en el producto actual, cuyas guardas están presentes. Priorizar rechazo de
recursos incompatibles (@s18), una fórmula de contraste y la clasificación
de errores400. No convertir36 supervivientes en36 pruebas nuevas.

| IDs originales | Líneas | Clasificación y diferencia observable |
| --- | --- | --- |
|61|34|Hueco prioritario: división por12.92 pasa a multiplicación. El vector público válido #0002D0 ya usado en backend es candidato directo; no se ejecutó aquí ni se atribuye kill.|
|58|33|Siempre usa rama exponencial. Cambia luminancia de canales bajos; falta vector decisivo cerca del umbral. Limitación pendiente, no equivalente declarado.|
|59|33|Equivalente para HEX8bit válido: channel=byte/255 nunca es0.04045, pues requeriría byte10.31475. Permanece raw Survived.|
|73,77,78|40|Hueco de formato: retirar guard o anclas puede aceptar color con sufijo, o un prefijo que conserve los primeros canales parseables. Ejemplos candidatos #0000FFextra y #0000FF#0000FF; no ampliar gramática ni permitir CSS arbitrario.|
|86|45–47|Frontera >=4.5 pasa a >4.5. No hay valor exacto demostrado con esta paleta finita; conservar como limitación, sin equivalencia ni redondeo.|
|99|61|Mensaje base de AppearanceValidationError vacío. La UI discrimina clase y fields; no se observa requisito de ese texto interno. No merece un test por literal.|
|119|97|Equivalencia del fallback al fallar JSON: null/undefined son rechazados por exact y se conserva la respuesta HTTP original.|
|115|93|Procesa como candidato400 cualquier HTTP. Con HTTP503 y body con status400 válido podría fabricar errores de campo y permitir otra decisión en lugar de incertidumbre; candidato útil de clasificación.|
|120|98|Elimina optional chaining: saveAppearance sin signal puede fallar antes de clasificar400 válido. Puerto público admite signal omitido; UI normal sí lo suministra. Prioridad menor al caso400 integral.|
|142,145|102–103|Acepta code o status del problema incompatibles; no debe convertir respuesta inválida en errores de campo.|
|147,175|104,120|Retira typeof, pero trim sigue rechazando valores JSON no string mediante excepción. Equivalencia sólo respecto a rechazo/no aplicación, no identidad del error interno; no exigir prueba por TypeError.|
|150,178|105,121|Acepta title/message de espacios. Riesgo de error vacío/ilegible; candidato dentro del mismo bloque de problemas inválidos.|
|151,152|107|Lista vacía aceptada como AppearanceValidationError sin campos. Diferencia observable de clasificación, aunque ningún color se aplica.|
|154|108–122|every→some permite lista mixta con una entrada válida y otra inválida; hueco prioritario y representativo del bloque.|
|156,158,159,160,161,162,163,164|110–121|Desactiva/pasa a OR distintos prefijos de validación de entrada. Permite campo/código/shape inválidos o mensaje incompleto; agrupar con rechazo integral de lista mixta, no un test por operador.|
|171,172,174|115–118|Retira códigos REQUIRED/INVALID_TYPE/INSUFFICIENT_CONTRAST admitidos. Una respuesta400 legítima se trataría como fallo genérico; seleccionar nominal real de contraste y sólo añadir variantes si el contrato/oráculo lo requiere.|
|235,238|158–159|Unconfigured con otro acento accesible pasa. Hueco explícito @s18: no basta probar sólo theme DARK; usar azul/cian válidos para evitar que contraste o formato maten por otra razón.|
|243|161|Unconfigured con ETag configurado o ajeno pasa. Hueco explícito @s18; debe preservar snapshot previo al rechazar.|
|282|179|ETag ausente provoca TypeError en lugar del error propio, pero sigue rechazando y no aplica snapshot. No defecto visible probado ni prioridad por mensaje interno.|
|284|180|Quita ancla final del ETag configurado: admite token válido seguido de basura. Hueco comprobable del contrato de revisión fuerte exacta.|

Los36 IDs están enumerados una sola vez. Las equivalencias anteriores son
razonamientos acotados a entradas/efectos públicos; no reclasifican el XML/JSON.
Los riesgos del bloque400 importan porque AppearanceState diferencia
AppearanceValidationError de incertidumbre; un problema no fiable no debería
habilitar el mismo flujo que un400 auténtico.

Propuesta mínima para decisión de root: un ciclo de defaults/revisión exactos,
uno de gramática HEX, uno con el vector público de contraste y uno de400
integral (nominal contraste más lista mixta/estado incoherente según selección).
Mantener corpus/rangos/umbral y original intactos; no campaña por mero conteo.
Los demás residuos pueden permanecer documentados si no aparece diferencia
observable adicional. Esta revisión no revisa Provider/Form ni routing.
