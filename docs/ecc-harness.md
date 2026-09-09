# ECC junto al arnés SSD de este repositorio

El 9 de septiembre de 2026 el usuario instaló ECC 2.2.1 como plugin `ecc@ecc` en ámbito de usuario, con perfil de hooks `standard`, y pidió usarlo en este proyecto. Este documento fija cómo convive con el arnés propio y qué está realmente activo.

## Qué es cada cosa

El arnés de este repositorio define el **proceso**: conversación, Gherkin, puerta humana, TDD estricto, revisión independiente y prueba de mutación sobre el umbral. Está en `AGENTS.md`, `docs/workflow.md` y `.claude/agents/`. Es la autoridad sobre cuándo una feature puede declararse terminada.

ECC aporta **catálogo y técnica**: 286 skills y 68 agentes con listas de comprobación por stack. No define cuándo algo está hecho.

Se componen así: ECC entra dentro de las fases del arnés propio, nunca las sustituye. Un dictamen de ECC no reemplaza al `judge`, y ninguna lista de comprobación de ECC exime de la mutación por encima de 0,8.

## Qué está activo

El plugin vive en `~/.claude/plugins/cache/ecc/ecc/2.2.1/`. Se cargó en la sesión en curso sin reiniciar: los 286 skills quedaron disponibles con el prefijo `ecc:`, junto con las herramientas MCP de Chrome DevTools que trae el plugin. No hizo falta abortar los seis carriles de implementación ni la revisión de cierre que estaban en marcha.

El perfil de hooks elegido es `standard` y queda registrado en `pluginConfigs` de la configuración de usuario. Está operativo: en la primera edición de este mismo documento, la guardia GateGuard interceptó la escritura y exigió declarar qué archivos dependen de él, qué API pública toca, qué datos maneja y cuál era la instrucción del usuario. La escritura solo se permitió tras responder. Conviene contar con esa fricción en cada primer toque de un archivo.

Las reglas de ECC no se instalan con el plugin. Copiarlas a `~/.claude/rules/ecc/` las volvería de carga permanente en todos los proyectos del usuario, así que no se ha hecho sin decisión suya. `rules/common` más `rules/java` o `rules/typescript` serían las candidatas.

## Skills de ECC que aplican a este stack

| Skill | Dónde se usa aquí |
| --- | --- |
| `springboot-security` | Canal Bearer, cifrado de secretos, validación de entrada, limitación de tasa, cabeceras |
| `springboot-tdd`, `springboot-verification` | Ciclos de los carriles de backend y su evidencia de cierre |
| `springboot-patterns`, `java-coding-standards` | Revisión del código de adaptadores y casos de uso |
| `react-testing`, `react-patterns`, `frontend-a11y` | Vistas nuevas y su evidencia de accesibilidad |
| `security-review` | Lista genérica de secretos, entrada, autorización y terceros |
| `verification-loop` | Cierre por evidencia, junto a `docs/verification.md` |

Los agentes `java-reviewer`, `typescript-reviewer`, `react-reviewer` y `security-reviewer` de ECC sirven como guion de revisión; los ejecutan los agentes de este repositorio hasta que el registro de ECC esté cargado.

## Dónde ha aportado ya

Las features 25, 27 y 28 manejan secretos cifrados, firmas HMAC y peticiones salientes hacia direcciones que elige el usuario. El juez de la feature 24 ya encontró ahí un oráculo ausente en la comprobación de permisos del canal Bearer. Esa superficie recibe una revisión de seguridad guiada por `springboot-security` además de la puerta habitual.

## Límite honesto

Instalar ECC no mejora nada por sí solo. Lo que vale es la lista de comprobación aplicada a código concreto y verificada con pruebas. Ninguna afirmación de seguridad de este proyecto se apoya en tener el plugin instalado.
