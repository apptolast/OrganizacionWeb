# Preparación PIT de historial18

Estado: alcance comprobado; campaña no iniciada. Se espera autorización root tras init/build. No se ejecutó Gradle durante esta preparación.

Los 12 patrones de historyClasses cubren exactamente los 12 archivos Java de producción modificados frente a origin/main en el corte registrado en history_pit_scope_check.json. Incluyen ApplicationConfiguration compartida, los tres adaptadores con comodín para clases anidadas y los ocho tipos de aplicación. Las interfaces pueden no producir mutantes; no se presupone un inventario antes de medir.

El arnés despacha history-backend a pitest con -PmutationScope=history. Candidatos: todos com.apptolast.organization.*; cuatro workers, umbral 80, XML/HTML separados en backend/build/reports/pitest-history. Se conservan -FRECORD, exclusión explícita equals/hashCode/toString y timeout constante heredado de 15000 ms. No se añadió filtro, mutador ni exclusión. La puntuación estricta se calculará con KILLED separado de supervivientes, NO_COVERAGE y errores/timeout; todavía no hay resultado.

history_pit_before_hashes.json conserva todos los archivos backend versionados (producción, pruebas, recursos SQL, wrapper/configuración y archivos auxiliares), dispatcher y su test, configuración y motor del arnés. Es un conjunto conservador mayor que las clases mutadas: no describe cobertura obtenida. No incluye salidas de build ni fuentes frontend. Antes de iniciar se comprobará que los hashes siguen iguales; cualquier cambio se declarará, sin sobrescribir silenciosamente esta captura.

No falta ningún archivo productivo del diff en los patrones. No se encontró un bloqueo de configuración. Los gates globales siguen bajo control de root.
