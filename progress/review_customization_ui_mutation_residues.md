# Revisión independiente de residuos UI21

Original intacto: customization_stryker_original/mutation.json, SHA256
3dc6d9b548b532389660a53b9915b620683c552032fef8e3510a10afae997942.
Lectura sin producción/tests/replay. Firmas completas (archivo, rango,
mutador, replacement, texto original y razones completas) en
customization_ui_mutation_review_signatures.json. Columnas del informe
empiezan en1; no son directamente columnas0 de un selector Stryker.

El original sigue bajo80:1391K/413S/11NC/9RuntimeError,1824total.
El alcance leído contiene161 supervivientes:66 customization,69 custom-fields,
10 project-reader y16 project-tasks. task-reader tiene3Killed, sin residuos.
App no aparece en files del informe: no significa que sus nodos esténKilled;
las integraciones declaradas pueden no producir mutantes. No se modifica
el alcance para mejorar artificialmente el resultado.

## Prioridades de refuerzo de comportamiento

1. **Aceptar extremos públicos en controles (alta).** custom-fields518,
   rango140:18–140:41 EqualityOperator reemplaza >1000 por >=1000;
   544,151:19–151:54 reemplaza >1000000000 por >=1000000000.
   Probar envío y confirmación de TEXT exactamente1000 puntos, con astrales,
   y NUMBER±1000000000. El rechazo del valor exterior no acredita aceptar
   el extremo.538 (149:19–149:34 Regex) exige signo obligatorio; incluir
   entero positivo sin signo.564 (166:25–166:56 ConditionalExpression true)
   justifica false editado explícitamente, no sólo false sin tocar.

2. **Contenido temporal correcto en metadatos (alta).** PROJECT1877–1883 y
   TASK1921–1927/1933/1937–1941. El fixture de integración usa createdAt y
   updatedAt iguales: puede detectar orden pero no sustitución de dato.
   Usar instantes distintos y verificar etiqueta, datetime y texto público
   UTC con formato esperado;1933 elimina opciones Intl completas. No limitar
   el oráculo a contar dos time. Defaults provisionales:1874/1875 y1905/1906
   requieren GET pendiente/fallido con metadatos por defecto visibles, sin
   convertir esa presentación en configuración confirmada.

3. **Reordenación y edición reversibles (media).** customization1697/1699
   (122:42–122:66 y122:52–122:66) eliminan el filtro al desmarcar;
   1720/1724 (152:35–152:61 y153:34–153:47) bloquean/eliminan Bajar.
   Complementar el Subir existente: desmarcar un campo entre varios y bajar
   el primero, afirmar orden DOM y cuerpo real. Definiciones1763–1767,
   1817/1819 y1846/1848–1850 afectan carga/reset de label/type/active/error.
   Editar definición inactiva noTEXT, guardar o cancelar y crear otra:
   afirmar intención enviada y borrador resultante. Evitar un test por setter.

4. **Recuperación y privacidad en distintas respuestas (alta).**
   custom-fields433–441 (lectura),454–460 (recarga),573–579 (escritura)
   sobreviven al ampliar guardias401/404. Un503 o error de transporte debe
   conservar contexto/borrador y ofrecer recuperación, sin llamar retirada
   privada;401 vigente debe retirar, también al cambiar callback mientras
   la respuesta está retenida (415/416). No invocar handlers directamente:
   renderizar padre observable y resolver fetch retenido. No confundir
   sus guardas con el estado de sesión que revisa A.

5. **Foco voluntario en valores y limpieza de mensajes (media).**
   custom-fields392–399 eliminan listener focusin;469/471/473–480 alteran
   blur, y customization1660–1671 la guarda hermana. La unidad162 de vista
   no demuestra el consumidor de valores: recuperar con petición retenida,
   mover foco a un control habilitado y luego body, resolver y verificar
   que no roba foco. El caso positivo disabled164 y UX reales ya existen,
   no repetir esa evidencia por sí sola. Para424/425/502/503/601, usar
   error/éxito seguido de nueva edición o recurso y afirmar borrador/mensaje
   correctos; no inspeccionar estado interno.

6. **Etiquetas válidas y error en índice10 (media).** customization1776–1780
   y1798–1804 alteran recorte White_Space;1793/1794 cambian excluir la propia
   definición. Probar label con varios espacios Unicode en extremos e
   internos, límite60 y renombre propio sin falso duplicado; verificar
   resultado normalizado sin perder espacios internos. custom-fields595
   (183:21–183:62 Regex) sólo captura un dígito:12campos con error válido
   values[10].value debe asociarlo al undécimo control, conservar todos.
   No basta el caso indexado1 ya existente.

No se garantiza que cada grupo mate todos sus IDs: son hipótesis observables
para revisar y ejecutar individualmente. No se propone cobertura por conteo
ni una matriz masiva. Fronteras y contenido son prioridad antes de strings
puramente espaciales (1883/1927/1941) o defensas sobre datos imposibles desde
los decoders (1702/1754/1789/1859/1873/1903 OptionalChaining). Estas últimas
son candidatas a límite/equivalencia contextual, no equivalencias probadas
ni reclasificaciones. Foco cleanup1598–1600/400–402 merece analizar desmontaje
antes de una prueba artificial de listeners.

## Los nueve RuntimeError conservan su estado

Las nueve razones originales son exactamente iguales (una variante):
«Test runner crashed. Tried twice to restart it without any luck» seguido de
«TypeError: Cannot convert object to primitive value». La traza llega a
@stryker-mutator/util errorToString, String, Array.map y VitestTestRunner.run.
Esto demuestra fallo al comunicar errores del runner; no permite conocer
por sí solo el error original del mutante ni convertirlo enKilled.

- Estado1410 (262:29–266:6 BlockStatement{}),1413 (267:34–271:6{}),
  1416 (272:22–275:7 ArrowFunctionundefined),1420 (276:21–276:66undefined):
  mutan actualizadores de retirada de valores/borrador/error/reading.
  Devolverundefined en setters puede causar estado inválido; es una inferencia
  del código, no la causa original recuperada. A revisa su comportamiento.
- customization1672 (79:34–79:45 StringLiteral vacío) y custom-fields481
  (90:34–90:45 mismo mutador) cambian matches(':disabled') a matches('').
  Selector CSS vacío puede lanzar; motivo plausible, distinto de la traza
  de serialización realmente almacenada. No son equivalentes.
- custom-fields583 (178:19–178:64 ConditionalExpression true),592
  (182:33–184:25 OptionalChaining sin ?. tras match) y597
  (185:23–185:42 ConditionalExpression true) fuerzan tratamiento de error
  indexado incluso para error general/otro tipo. Posibles accesos aerrors,
  matchnull o sent[NaN], sin adjudicar cuál ocurrió. Casos400global y fallos
  noValidation ya relevantes; no quitar esas guardas ni excluir mutantes.

Conservar razones/raw y medir cualquier diagnóstico posterior por separado.
No solicitar repetir automáticamente nueve mutantes como si fueran flaky.
La UX aprobada y sus límites humanos/WindowsWebKit permanecen válidos; esta
campaña Vitest mide sensibilidad de sus oráculos, no sustituye navegador.
