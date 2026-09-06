# Diagnóstico independiente de RuntimeError — disponibilidad

**Resultado: ambos mutantes provocan un error real con Vitest normal y salida 1. No se reclasifican los RuntimeError originales como Killed.**

Se leyeron las identidades del informe original `frontend/reports/mutation-availability/mutation-original.json`. El fallo Stryker registrado ocurre al serializar errores en `errorToString`: «Cannot convert object to primitive value». El diagnóstico siguiente usa Vitest 4.1.10 directamente, sin el adaptador Stryker y sin modificar manejadores de errores.

| Mutante original | Identidad original | Resultado independiente |
| --- | --- | --- |
| 431 | availability.tsx, línea 157 columnas 10–25; OptionalChaining, reemplazo `zones.includes` en la condición de envío | Dos casos de catálogo fallido ejecutados; 2 tests pasan, pero Vitest detecta **2 rechazos no controlados**, `TypeError: Cannot read properties of undefined (reading 'includes')`, y termina con **exit 1**. |
| 446 | availability.tsx, línea 169 columnas 42–66; StringLiteral, reemplazo del selector de primer día inválido por plantilla vacía | Cuatro casos de presupuesto inválido ejecutados; 4 tests pasan, pero Vitest detecta **4 rechazos no controlados**, `SyntaxError: Invalid selector`, y termina con **exit 1**. |

La distinción importa: no hubo fallo de una aserción de esos tests, ni un resultado Killed emitido por Stryker. La política normal de Vitest rechaza las excepciones asíncronas que cada variante introduce. Esto aporta evidencia independiente de comportamiento inválido y explica que el runner original tuviera errores que serializar; no demuestra por sí solo la causa interna exacta de ese fallo de serialización.

## Aislamiento y comandos

Helper propio `.e2e-work/availability-runtime-diagnostics.mjs`: copia exclusivamente src y configuración de frontend a tres subdirectorios distintos bajo una carpeta propia `frontend/.stryker-tmp/runtime-diagnostic-*`. Las variantes se aplican por coincidencia única de la expresión pertinente, conservando la fuente final restante. En la fuente final esas expresiones están en las líneas 159 y 171; se preserva la identidad original por fragmento y mutador, sin confundir el desplazamiento de líneas con otro mutante. No se modifica la fuente viva, el perfil global ni las pruebas del autor. Las dependencias existentes se resuelven desde frontend/node_modules; no se instala otro paquete ni se cambia un manejador global.

Cada copia ejecuta Node con `frontend/node_modules/vitest/vitest.mjs run src/availability.test.tsx --config vite.config.ts --testNamePattern <patrón> --reporter=verbose`. Baseline usa las dos familias de pruebas: **6 pasan, 74 omitidas por el filtro, exit 0**. Mutante 431 ejecuta `recupera catálogo fallido`; mutante 446 ejecuta `rechaza presupuesto incompleto`. Los omitidos son selección focal explícita, no cobertura pretendida del resto.

Ejecución final del helper: salida 960618, exit 0 porque verifica que el baseline termina 0 y cada variante termina 1. Logs completos propios en `.e2e-work/availability-runtime-{baseline,431,446}.log`; resumen estructurado en `progress/evidence_availability_runtime_mutants.json`. El primer intento del helper se detuvo antes de aplicar 431 al encontrar dos coincidencias de zones?.includes; se precisó el contexto de la condición de envío. Fue una guarda de aislamiento, no un resultado del mutante.

SHA256 de availability.tsx antes y después, idéntico:
`042c68f09aba85b895e0d39a91646e6dffc141ff2c78e2e30cbfc9f4d7f62afa`.

No se ejecutó Stryker adicional. Los dos RuntimeError originales permanecen intactos; esta evidencia no cambia su denominador ni el score del informe original o del replay del autor. No se tocaron los temporales heredados bloqueados ni se eliminaron sus directorios.
