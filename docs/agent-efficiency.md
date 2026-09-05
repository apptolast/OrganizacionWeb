# Ponytail y Caveman en OrganizationWeb

El usuario pidió el 6 de septiembre de 2026 que el coordinador y todos los agentes usen ambas herramientas activamente. Se instalan sus skills de instrucciones en `.agents/skills/`, dentro del repositorio, para que el flujo sea reproducible y compartido.

| Skill | Fuente fijada | Modo |
| --- | --- | --- |
| Ponytail | DietrichGebert/ponytail, commit `974d940a1c5344210874150b98ff0d2c861fab6a`, `skills/ponytail` | full |
| Caveman | JuliusBrussee/caveman, commit `5184b3d11ac6a1acb7d44b9bfaa31698157cff97`, `skills/caveman` | lite |

Las copias de `SKILL.md` se conservan sin modificar, con sus licencias MIT. La instalación utiliza el helper de skill-installer con referencias fijadas. No instala el proxy de Caveman, hooks globales, paquetes ejecutables de terceros ni cambios en las credenciales o el tráfico del asistente. La variante de skill funciona leyendo las instrucciones; no necesita esos componentes.

## Uso activo por todos los agentes

- Leer ambas skills al recibir la tarea. El coordinador incluye su aplicación en cada delegación y revisión.
- Ponytail: comprender el flujo afectado; reutilizar código existente, biblioteca estándar, controles nativos o dependencias instaladas antes de añadir código. Evitar capas especulativas y cambios ajenos al contrato.
- Caveman lite: mensajes breves, frases completas y una idea por frase. Conservar nombres técnicos, cantidades, negaciones, errores y comandos exactos. Los documentos, comentarios y commits mantienen prosa normal.
- En cada revisión, identificar complejidad innecesaria y comprobar que la brevedad no elimina información necesaria. No añadir un informe separado por cada decisión trivial.

## Precedencia y límites

Los requisitos explícitos del usuario y las instrucciones del entorno prevalecen. Se mantienen React/SCSS, arquitectura hexagonal con sus puertos, EDA, SDD, TDD, umbral de mutación y revisión independiente. Una interfaz exigida por la arquitectura no es una abstracción no solicitada. El código conserva su formato legible y los formateadores del repositorio.

No se omiten validaciones, controles de acceso, manejo de pérdida de datos, accesibilidad ni funciones aprobadas. La brevedad no elimina las actualizaciones de progreso exigidas por el entorno. No se atribuye un porcentaje de ahorro a este proyecto a partir de los benchmarks de los autores.

Las skills quedan disponibles para descubrimiento en turnos posteriores; en la sesión actual se cargan explícitamente en el coordinador y cada agente. Esta activación por instrucciones no se presenta como instalación de un plugin con hooks o de un proxy.
