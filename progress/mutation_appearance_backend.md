# Mutación original de apariencia

Corte backend aislado 5f0b0aa, integrado por root como e3df0e1. No contiene
frontend final de apariencia y no se atribuyen gates de UI a este árbol.

Gate previo: gradlew test spotlessCheck --no-daemon -Dorg.gradle.jvmargs=-Xmx768m,
EXIT0 2a36eb, 3m12s. XML preservados en appearance_backend_gate_xml:
2384 tests en102 suites, cero fallos/errores/skips. Inventario por suite en
appearance_backend_gate_results.json.391 entradas previas intactas8dfc22.

Campaña ORIGINAL: node .harness/harness.mjs mutate appearance-backend.
Proceso67153, log appearance_pit_original.log. Se conservan once patrones,
todos los candidatos JUnit, cuatro workers, umbral80, heap y límites actuales.
Sin replay ni refuerzo durante campaña. Resultado final y análisis debajo.

## Resultado final original

EXIT0 b159ac; BUILD SUCCESSFUL en7m43s (PIT7m34s: cobertura177s y
mutación4m37s).159 mutantes:153 KILLED,6 SURVIVED,0 NO_COVERAGE,
0 TIMED_OUT y0 errores. Denominador estricto159;153/159=96.2264150943396%.
Cobertura de líneas247/247;381 clases examinadas y1199 ejecuciones de tests.
No se recalifican supervivientes ni se mezclan futuros replays.

Raw original copiado: appearance_pit_original/mutations.xml,
SHA293593AC0AFB54D77BDDC2B624AC92503C904764E970BF598998BB466E2EDA5E.
Inventario de seis firmas en appearance_pit_residuals.json.391 entradas
before/after idénticas (a150fd), manifiestos appearance_pit_before_hashes.json
y appearance_pit_after_hashes.json. Gate y raw se preservan antes de refuerzos.

## Residuos y dictamen

- AppearanceValues.channel:62, boundary index20: compara byte/255 con0.04045.
  Ningún byte entero coincide con10.31475; cambiar <= por < no cambia esa
  rama en el dominio hexadecimal de ocho bits. Se conserva SURVIVED original.
- AppearanceValues.channel:62, Math index23: división por12.92 sustituida
  por multiplicación. Hay diferencia pública: #0002D0 válido como acento claro
  tiene mínimo7.856465610995792; conmutante4.4878507918199295, rechaza.
- AppearanceValues.contrast:53, Math index16: +0.05 sustituido por -0.05.
  #0033FF válido tiene mínimo5.168912080757051; con numerador mutado,
  4.483400842930037. Son cálculos independientes para orientar el oráculo,
  no se atribuye una ejecución del mutante ni un kill todavía.
- AppearanceValues.color:40, boundary index44: rechazo <4.5 pasa a <=4.5.
  No se dispone de vector público que alcance exactamente4.5 en las seis
  superficies. No se inventa fixture ni se afirma equivalencia demostrada.
- PostgresAppearanceStore.lambda$select$0:45, boundary index88, y
  SaveAppearance.timestamp:66, boundary index23: segunda comparación >9999.
  Hay pruebas fuera de rango y año0001, pero falta aceptación pública de9999.
  Un único recorrido real de alta/lectura PostgreSQL puede cubrir ambos.

Gate de mutación supera80 con holgura. Root autoriza después de este resultado
sólo tres refuerzos concretos: dos colores válidos y año9999. Se registrarán
como inicialmente GREEN si la implementación actual los satisface. No cambios
productivos ni de alcance; replay separado requiere revisión posterior.
