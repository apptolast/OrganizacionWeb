# Revisión independiente HTTP16

APPROVED para integrar el adaptador, no para cerrar16. Root6554c1/a3248e revisó controller y25 pruebas nuevas, contrastó el manifest y los XML:25 CloseWorkSessionApiTest +48 WorkSessionStateApiTest +49 WorkSessionApiTest =122, sin fallos, errores ni omisiones. Spotless final5744e9 acreditado por autor; no se repiten suites desde el juez.

POSTclose reutiliza seguridad y precedencia de query/UUID/key/token/JSON. Acepta sólo los dos campos de notas, comprueba su tipo antes del valor de dominio real y transmite identidad completa del token e intención normalizada. La respuesta discrimina DTO6 de PAUSE/RESUME y DTO7 de CLOSE, con revisión y trabajo como strings, sin añadir closure:null a los contratos anteriores. GETclosure delega al puerto real por propietario/sesión; no consulta previamente el estado ni inventa ausencia a partir de un fallo. C/K comparten la representación histórica.

Los oráculos nominales comparan shape y valores explícitos; los problemas comparan cuerpo cerrado, status y no-store. Unicode/normalización atraviesan el constructor real probado por A. Se conservan Location en POST/replay y ausencia en GET. El incidente CSRF fue una preparación incorrecta del test, corregida sin fingir un RED productivo.

La evidencia HTTP acredita serialización, delegación, validación y mapping de errores. Propiedad efectiva, transacciones, replay con notas, carreras y recuperación tras reinicio se validan en PostgreSQL/smoke; no los demuestran los mocks. No se afirma mutación, UI ni despliegue desde este paquete. Se integran sólo dos Java propios y documentación; las dependencias nominales de A ya existen en COMMON.
