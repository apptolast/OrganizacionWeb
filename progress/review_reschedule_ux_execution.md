# Revisión de la evidencia UX de Replanificar

Aprobación parcial de geometría, ampliación y controles condicionales; no cierre
global de UX ni de la funcionalidad.

La inspección independiente 003f50 confirmó 124 mediciones en 31 anchuras,
cuatro pruebas con altura 400 y cuatro informes axe sin infracciones. Se
inspeccionaron capturas nominales móviles y de escritorio. Texto al 200 % añade
12 mediciones y cuatro informes axe; zoom nativo verifica getZoom=2 y DPR de
1,5 a 3, con ventana real ajustada a 320 píxeles CSS. Root comprobó las fuentes
y hashes 8dfcc9 y leyó implementación y evidencia 824563/7062d3. No se presenta
reducir el viewport como zoom nativo.

El skiplink sin foco está fuera del viewport: y=-100, altura45, bottom=-55.
Su aparición en capturas fullPage no constituye un defecto visual demostrado.

El recorrido condicional inicialmente verde acredita errores reales DST,
selección de ambas ocurrencias, revisión de 90 minutos con exceso explícito y
confirmación mediante Tab, Space y Enter. Root revisó fuente y geometría en
cb0ebd/7062d3. El checkbox interno mide 22 píxeles, pero su label asociado es
el objetivo clicable y supera 44 píxeles en ambas dimensiones. No requiere una
corrección SCSS por medir sólo la caja interna.

Se conserva el dictamen de los 30 principios en review_reschedule_ux_principles.md.
Sus dos mejoras P3 son de claridad: unidades mixtas y referencia sin etiqueta.
Quedan pendientes otros motores y evidencia de feedback con respuesta retenida.
No hay prueba con dispositivos físicos ni lectores de pantalla reales; axe y
geometría no sustituyen esas observaciones ni estudios de comprensión humana.

Actualización posterior: Firefox y WebKit pasan nominal y condicional, dos
recorridos por motor, con fuentes sin adaptar. La revisión independiente9855ec
comprobó los informes axe y el feedback; 584eb7 revisó íntegro el test nuevo.
Con preview real retenido, el anuncio aparece a los 2,5 ms; tras503 controlado,
el reintento devuelve200 real, conserva foco/borrador y no crea recibos. El
resultado mide feedback DOM en Chromium, no respuesta del servidor ni tiempo
de anuncio de un lector de pantalla. La medida no promete idéntica latencia
en todo dispositivo. Se aprueba este paquete de pruebas para integración;
resta ejecutarlo en el corte común y cerrar la revisión general de la feature.
