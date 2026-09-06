# Hoy: soporte de selección de mutación

Contrato a127747. Soporte autorizado por root, sin ejecutar PIT ni Stryker. Ponytail full y Caveman lite. No modifica producción de aplicación ni configuración backend.

## Ciclos del arnés

1. Test «today backend scope runs only its fixed PIT target»: RED ad6183, `Invalid target: today-backend`. Se añadió el nombre al conjunto cerrado y la única llamada `pitest --no-daemon -PmutationScope=today`. GREEN07891c, 1/1.
2. Test «today frontend scope runs only its fixed Stryker configuration»: RED3ec039, `Invalid target: today-frontend`. Se añadió el nombre cerrado y ejecución fija de `stryker.today.config.json` mediante pnpm. GREEN5068ca, 1/1.
3. Refuerzo de matrices existentes para rechazo de targets con argumentos/operación incorrecta y paso intacto de ambos nombres por CLI. Inicialmente GREEN; no se atribuye RED a estas filas.

Regresión scripts/project.test.mjs: 12/12 GREEN62d123. El runner inyectado sólo captura argumentos: no lanza Gradle, Docker ni Stryker. Se mantienen los tres targets schedule_block previos, ausencia/target vacío con ambas suites completas, rechazo antes de subprocess y validación de argumentos adicionales. `node --check scripts/project.mjs` y diff --check focal EXIT0 en la misma salida.

## Pendiente antes de ejecutar

La ruta frontend ya se selecciona, pero su configuración todavía no existe: depende del corte estable de la UI. Debe incluir today-api.ts, today.tsx y únicamente regiones compartidas realmente modificadas; actualizar también el scope por defecto tras inspeccionar los desplazamientos, sin incorporar CreateProjectScreen intacto. Mantener umbral80, perTest, concurrency2, ignorePatterns exacto del destino protegido e informes separados en reports/mutation-today. Backend build.gradle pertenece al autor backend y espera sus nombres finales. Este soporte parcial no certifica que una campaña sea ejecutable ni autoriza mutación antes del juez.

## Configuración sobre primer corte estable de UI

La pendiente anterior se resolvió tras build c9040d del autor: test de configuración RED66b491 por archivo ausente, GREEN8d6cd4 tras crear configuración y manifest. Regresión final13/13 GREEN0560e0, formato focal7c935f y diff --check sin errores. Configuración today: API/UI completas, App10–49 (CreateProjectScreen50+ intacta), Workspace27–54 y85, href ProjectReader61/157, condiciones use-session29/189. Columnas finales comprobadas contra fuente ae141a; no se inventan longitudes. Default conserva targets previos y sustituye regiones desplazadas de App/Workspace, añade Today y href; use-session ya completo. Exports/formato de schedule-block-api no añaden comportamiento y su archivo completo permanece en default. Navigation sigue intacto.

`today_frontend_mutation_scope.json` registra hashes de bytes del corte y rangos, sin resultados de mutación. El posterior fix de frontera de medianoche anunciado por root requiere actualizar el hash de Today antes del juez; no se ejecuta una campaña con ese manifest desactualizado.
