# Replay dirigido20: dictamen

EXIT0 22a6d3,5m24.79 mutantes:72 Killed,7 Survived;0 NoCoverage,Timeout,
RuntimeError,CompileError o Error.72/79=91.1392405%, umbral80 conservado.
Cobertura inicial811 tests GREEN,3m2; ocho workers y configuración aprobada.

Raw independiente `appearance_stryker_replay_original/mutation.json` SHA
`61EAB93975D8795DD2C479C7A0B21850FAA8EC2EB16C13339DA423A546029A91`.
Original637/747 intacto SHA56D3A57C…D60122; no se agregan numeradores ni se
reclasifican resultados anteriores.120 entradas antes/después idénticas8b6815.

Las31 firmas pedidas aparecen exactamente una vez por archivo/rango/mutador/
reemplazo:30Killed y1Survived. Los48 extras también están inventariados:
42Killed y6Survived. `appearance_stryker_replay_inventory.json` contiene79 filas
con ID original/replay, estados originales/finales, rol objetivo/extra y firma
completa. Análisis reproducible c410b6; ningún ID nuevo se usó como identidad.

## Siete residuos finales

| Original → replay | Archivo y rango original1-based | Residuo y límite |
| --- | --- | --- |
|73→1, objetivo|appearance-api.ts40:7–40:32|Sólo typeof pasa a false; regex y cálculo permanecen. Para strings HEX el typeof es redundante y estos vectores no lo distinguen. No prueba equivalencia universal para valores JSON ajenos; no se repite campaña por este candidato.|
|171→29, extra|appearance-api.ts115:13–115:23|REQUIRED vacío. Falta nominal específico de ese código; contraste174 síKilled. Implementación actual conserva el código correcto.|
|172→30, extra|appearance-api.ts116:13–116:27|INVALID_TYPE vacío. Mismo límite específico; no confundir cobertura de INSUFFICIENT_CONTRAST con todos los códigos.|
|175→33, extra|appearance-api.ts120:11–120:44|Retira typeof de message; trim posterior aún rechaza mensajes JSON no string. No diferencia visible de aplicación demostrada; identidad del error interno puede variar.|
|178→36, extra|appearance-api.ts121:11–121:31|Retira trim: mensaje sólo espacios se aceptaría. Hueco de oráculo de problema corrupto; guard actual presente, sin defecto actual observado.|
|618→70, extra|appearance.tsx193:19–193:36|Regex de valor del picker pierde ancla inicial. Falta vector con prefijo en borrador; no desactiva validación de contraste/guardado.|
|619→71, extra|appearance.tsx193:19–193:36|Regex pierde ancla final. Falta vector con sufijo en borrador; afecta selección de valor nativo del picker, no demuestra aplicación global insegura.|

Los dos últimos son extras del rango del objetivo620, que síKilled. No se
declaran equivalentes ni se equipara el comportamiento del input color nativo
al decoder API. Se conservan como límites de prueba de entradas inválidas.

Dictamen acotado: refuerzos acreditan30 objetivos y mantienen42 controles
heredados del rango; no apareció defecto productivo observable que justifique
otra edición o campaña. Aceptar estos siete límites documentados, sin nuevas
pruebas para elevar un porcentaje ya superior al umbral. La revisión funcional
y CI siguen siendo gates independientes. No procesos Stryker propios activos.
