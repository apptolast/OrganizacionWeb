# Convenciones de código

> Homogeneidad extrema. La IA predice mejor cuando el repositorio se parece a
> sí mismo en todas partes. Define aquí las convenciones de TU stack; los
> agentes las siguen al pie de la letra.

## Principios (agnósticos)

- **Un formateador y un linter fijos**, declarados en `harness.config.json`
  (`commands.lint`). El código se entrega formateado; el linter no da avisos.
- **Nombres consistentes** por tipo (módulos, tipos, funciones, constantes,
  privados). Elige una convención por categoría y no la mezcles.
- **Estructura de archivo homogénea**: mismo orden de imports, mismas
  cabeceras, mismos patrones de export.
- **Manejo de errores uniforme**: un tipo/base de error de dominio; la capa
  de interfaz captura, informa por el canal de error y sale con código != 0.
  Nunca propagar stack traces crudos al usuario.
- **Comentarios: solo el *por qué* no obvio.** Los nombres hacen el resto. Sin
  comentarios decorativos ni obviedades.
- **Tests co-locados o en `tests/`**, uno por módulo, con nombres
  descriptivos que digan qué comportamiento verifican.

## Ejemplos concretos (referencia)

| Stack     | Formato/Lint         | Tests            | Ver                              |
| --------- | -------------------- | ---------------- | -------------------------------- |
| Python    | PEP 8, líneas ≤ 100  | `unittest`       | `examples/python-notes-cli`      |
| Node/TS   | ESLint + Prettier    | `node --test`    | `examples/node-notes-cli`        |
| Go        | `gofmt` + `go vet`   | `go test ./...`  | `examples/go-notes-cli`          |
| Rust      | `cargo fmt` + clippy | `cargo test`     | `examples/rust-notes-cli`        |
| Node/TS (web) | ESLint 9 flat + Prettier | Vitest + TL | El repo WebEmpresa que inspiró la plantilla |

## Convenciones de OrganizationWeb

- **Lenguajes:** Java 25 para backend; TypeScript 6 y React 19 para frontend.
  Gradle Kotlin DSL configura el build. Sass/SCSS es la autoría de estilos, sin Tailwind.
- **Formato:** Google Java Format 1.31 mediante Spotless en Java; Prettier en frontend.
  Se respetan las decisiones de esos formateadores, sin ajustes manuales de alineación.
- **Lint:** SpotlessCheck y ESLint con TypeScript/React Hooks; comprobación TypeScript
  durante build. Los comandos raíz coordinan ambos subproyectos.
- **Nombres:** tipos Java/TS PascalCase; variables/métodos camelCase; constantes
  SCREAMING_SNAKE_CASE cuando corresponda; columnas SQL snake_case y JSON camelCase.
- **Imports:** Java ordenado por Google Java Format. TypeScript separa imports de tipos
  mediante `import type`; importación directa del módulo usado, sin barrels innecesarios.
- **Errores:** validaciones del dominio con errores por campo; el adaptador HTTP traduce
  a application/problem+json con códigos estables y mensajes españoles. Sin stack traces
  o credenciales en la respuesta. El cliente distingue error confirmado e incertidumbre.
- **Tests:** nombres trazables a tags Gherkin; PostgreSQL efímero real en integración.
  No incluir datos de usuarios, secretos o credenciales productivas en fixtures.
- **Documentación:** estado implementado y propuesto separados; evidencia y límites
  en progress, sin declarar done antes de revisión y mutación.
