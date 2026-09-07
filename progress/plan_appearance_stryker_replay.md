# Único replay dirigido20: preparado, no ejecutado

Prevalidación551cfe mediante parser instalado Stryker10:31 firmas originales
exactas,21 rangos mínimos contenidos en el alcance original, freeze119 intacto.
Original SHA56D3A57C…D60122 conservado; fuentes de los cuatro archivos comparadas
con source del informe. La configuración sólo cambia mutate y destinos de
JSON/HTML/temp frente a stryker.appearance.config.json. Mantiene Vitest y su
vite.config.ts, candidatos de pruebas, perTest,8 workers,umbral80, mutadores y
tiempos por defecto; ignorePatterns protegido sin cambios.

Objetivos:20 firmas del paquete API más352/412/441/443/527/620/623/633/642/705
y707 como efecto adicional pedido. Estado ORIGINAL de esos31:29Survived y
2NoCoverage. El manifiesto preserva ID,archivo,rango1-based,mutador,reemplazo
y estado; el ID nuevo del replay no se usa como identidad.

El selector convierte columnas1→0 en ambos extremos y conserva líneas1-based.
ProjectReader.filterMutatePattern instalado produjo las coordenadas esperadas
para los21 selectores. Se eliminan sólo rangos contenidos/repetidos, sin sacar
firmas. La selección por rango puede generar extras: el inventario ORIGINAL
contenido arroja79 firmas (42K/35S/2NC), no se promete que sea el total final.
Todos los extras se enumerarán y evaluarán; no se afirmará que sólo31 mutantes
se ejecutaron ni se sumará resultado al original637/747.

Precisión al revisar la firma73: su rango40:7–40:32 sólo sustituye la condición
typeof por false, NO toda la guardia HEX como se resumió por línea anteriormente.
La regex aún rechaza cadenas mal formadas. Conservarlo como objetivo propuesto
con resultado abierto, sin prometer kill por los dos vectores HEX. Esta precisión
no altera producción, tests ni la firma raw; si sobrevive se analizará ese límite.

FreezeB:2058/44 GREEN91f538; formatoAPI único posterior y lintglobal EXIT0
066161f. Hash API testF0A6047E…2818C. La prevalidación verifica cada una de las
119 entradas del manifest final, no sólo las tres pruebas reforzadas.

Archivos:
- frontend/stryker.appearance-replay.config.json SHA
  F2CE7FA83B55CCCEEFA0FDCC186FB0FA23E82613C0DE56DFC623B49F63F9637F.
- progress/appearance_stryker_replay_targets.json SHA
  B221E373E8E0107D6C3BDAC5F5D3B7547D495827CBD791673DE8B1BFC2B26358.
- progress/prepare_appearance_stryker_replay.mjs: preparación reproducible,
  sólo parser/lectura/validación/generación, no ejecuta Stryker.

Comando propuesto tras revisión root:
`pnpm --dir frontend exec stryker run stryker.appearance-replay.config.json`.
Destinos dedicados reports/mutation-appearance-replay y
.stryker-tmp-appearance-replay. Antes: capturar119 inputs más config dirigida;
después: comparación y copia de raw a progress/appearance_stryker_replay_original.
Conservar EXIT propio, JSON original del replay/SHA, todos los estados y la
presencia exacta de31 firmas; informar ausencia/error sin retry automático.
No nueva campaña global, tests, exclusiones ni rebaja de umbral.
