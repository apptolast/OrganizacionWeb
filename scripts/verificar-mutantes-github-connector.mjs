// Verifica a mano que los oráculos nuevos discriminan: aplica cada mutante superviviente
// al fichero de producción, ejecuta la suite del componente y anota qué pruebas caen.
// No modifica nada de forma permanente: restaura el fichero tras cada pasada.
//
// Uso: node scripts/verificar-mutantes-github-connector.mjs
import { execFileSync } from "node:child_process";
import { readFileSync, writeFileSync } from "node:fs";

const SOURCE = "frontend/src/github-connector.tsx";
const original = readFileSync(SOURCE, "utf8");

/** [nombre, texto exacto a buscar, texto de reemplazo] */
const MUTANTS = [
  [
    "41 StringLiteral GITHUB_UNAVAILABLE",
    `  GITHUB_UNAVAILABLE: "GitHub no responde. Inténtalo más tarde",`,
    `  GITHUB_UNAVAILABLE: "",`,
  ],
  [
    "44 StringLiteral GITHUB_REPOSITORY_UNAVAILABLE",
    `    "El repositorio no está disponible con ese token",`,
    `    "",`,
  ],
  [
    "46 StringLiteral RESOURCE_NOT_FOUND",
    `  RESOURCE_NOT_FOUND: "El proyecto ya no está disponible",`,
    `  RESOURCE_NOT_FOUND: "",`,
  ],
  [
    "47 StringLiteral VALIDATION_ERROR",
    `  VALIDATION_ERROR: "Revisa el repositorio y el token",`,
    `  VALIDATION_ERROR: "",`,
  ],
  [
    "60 BooleanLiteral loading inicial",
    `  const [loading, setLoading] = useState(true);`,
    `  const [loading, setLoading] = useState(false);`,
  ],
  [
    "66 ArrayDeclaration projects inicial",
    `useState<ProjectSummary[]>([]);`,
    `useState<ProjectSummary[]>(["Stryker was here"]);`,
  ],
  [
    "80 BooleanLiteral focusDisconnect inicial",
    `  const focusDisconnect = useRef(false);`,
    `  const focusDisconnect = useRef(true);`,
  ],
  [
    "132 ConditionalExpression disabled -> false",
    `    if (disabled) return;`,
    `    if (false) return;`,
  ],
  [
    "153 ConditionalExpression !confirming -> false",
    `    if (!confirming) return;`,
    `    if (false) return;`,
  ],
  [
    "155 ConditionalExpression key !== Escape -> false",
    `      if (event.key !== "Escape") return;`,
    `      if (false) return;`,
  ],
  [
    "156 CallExpression preventDefault -> ;",
    `      if (event.key !== "Escape") return;\n      event.preventDefault();`,
    `      if (event.key !== "Escape") return;`,
  ],
  [
    "160 ArrowFunction cleanup -> undefined",
    `    return () => document.removeEventListener("keydown", onKeyDown);`,
    `    return () => undefined;`,
  ],
  [
    "160 StringLiteral keydown -> ''",
    `document.removeEventListener("keydown", onKeyDown)`,
    `document.removeEventListener("", onKeyDown)`,
  ],
  [
    "207 BooleanLiteral setConnecting(false) -> true",
    `        setConnecting(false);`,
    `        setConnecting(true);`,
  ],
  [
    "214 ConditionalExpression guarda import -> false",
    `    if (importing || !selected) return;`,
    `    if (false) return;`,
  ],
  [
    "214 LogicalOperator || -> &&",
    `    if (importing || !selected) return;`,
    `    if (importing && !selected) return;`,
  ],
  [
    "218 CallExpression setImportError(null) -> ;",
    `    setImporting(true);\n    setImportError(null);\n    setSummary(null);`,
    `    setImporting(true);\n    setSummary(null);`,
  ],
  [
    "219 CallExpression setSummary(null) -> ;",
    `    setImporting(true);\n    setImportError(null);\n    setSummary(null);`,
    `    setImporting(true);\n    setImportError(null);`,
  ],
  [
    "230 ConditionalExpression STORAGE_UNAVAILABLE -> true",
    `      if (failure.code === "STORAGE_UNAVAILABLE")`,
    `      if (true)`,
  ],
  [
    "235 LogicalOperator ?? -> &&",
    `          failed: failure.failed ?? 0,`,
    `          failed: failure.failed && 0,`,
  ],
  [
    "236 BooleanLiteral truncated -> true",
    `          truncated: false,`,
    `          truncated: true,`,
  ],
  [
    "258 ConditionalExpression !live -> false (desconexión)",
    `      await disconnectGithub(controller.signal);\n      if (!live(controller)) return;`,
    `      await disconnectGithub(controller.signal);\n      if (false) return;`,
  ],
  [
    "260 CallExpression setSummary(null) -> ; (desconexión)",
    `      setConnection(null);\n      setSummary(null);\n      setImportError(null);`,
    `      setConnection(null);\n      setImportError(null);`,
  ],
  [
    "261 CallExpression setImportError(null) -> ; (desconexión)",
    `      setConnection(null);\n      setSummary(null);\n      setImportError(null);`,
    `      setConnection(null);\n      setSummary(null);`,
  ],
  [
    "266 ConditionalExpression live -> true",
    `      if (live(controller))\n        setImportError(`,
    `      if (true)\n        setImportError(`,
  ],
  [
    "271 ConditionalExpression -> true",
    `      if (mounted.current && pending.current === controller)`,
    `      if (true)`,
  ],
  [
    "271 LogicalOperator && -> ||",
    `      if (mounted.current && pending.current === controller)`,
    `      if (mounted.current || pending.current === controller)`,
  ],
  [
    "271 EqualityOperator === -> !==",
    `      if (mounted.current && pending.current === controller)`,
    `      if (mounted.current && pending.current !== controller)`,
  ],
  [
    "284 UnaryOperator tabIndex main",
    `    <main id="proyectos" className="github-connector" tabIndex={-1}>`,
    `    <main id="proyectos" className="github-connector" tabIndex={+1}>`,
  ],
  [
    "285 UnaryOperator tabIndex h1",
    `      <h1 ref={heading} tabIndex={-1}>`,
    `      <h1 ref={heading} tabIndex={+1}>`,
  ],
  [
    "405 StringLiteral alert -> ''",
    `              role={connectError ? "alert" : undefined}`,
    `              role={connectError ? "" : undefined}`,
  ],
  [
    "407 StringLiteral '' -> Stryker",
    `              {connectError ? importMessage(connectError) : ""}`,
    `              {connectError ? importMessage(connectError) : "Stryker was here!"}`,
  ],
  [
    "416 ConditionalExpression -> true",
    `      {importError && importError.code !== "GITHUB_TOKEN_REJECTED" ? (`,
    `      {importError && true ? (`,
  ],
  [
    "416 StringLiteral -> ''",
    `      {importError && importError.code !== "GITHUB_TOKEN_REJECTED" ? (`,
    `      {importError && importError.code !== "" ? (`,
  ],
  [
    "419 ConditionalExpression entera -> true",
    `          {importError.code === "CONNECTION_INVALID" && !reconnecting ? (`,
    `          {true ? (`,
  ],
  [
    "419 ConditionalExpression !reconnecting -> true",
    `          {importError.code === "CONNECTION_INVALID" && !reconnecting ? (`,
    `          {importError.code === "CONNECTION_INVALID" && true ? (`,
  ],
  [
    "419 LogicalOperator && -> ||",
    `          {importError.code === "CONNECTION_INVALID" && !reconnecting ? (`,
    `          {importError.code === "CONNECTION_INVALID" || !reconnecting ? (`,
  ],
  [
    "424 ConditionalExpression -> true",
    `          {importError.code === "IMPORT_IN_PROGRESS" ? (`,
    `          {true ? (`,
  ],
  [
    "456 StringLiteral '' -> Stryker",
    `            : ""}`,
    `            : "Stryker was here!"}`,
  ],
  [
    "474 ConditionalExpression -> true",
    `  return last && last.status !== "running" ? fromReceipt(last) : null;`,
    `  return true ? fromReceipt(last) : null;`,
  ],
  [
    "474 StringLiteral running -> ''",
    `  return last && last.status !== "running" ? fromReceipt(last) : null;`,
    `  return last && last.status !== "" ? fromReceipt(last) : null;`,
  ],
  [
    "478 OptionalChaining ?. -> .",
    `  return projects.find((project) => project.id === id)?.name ?? id;`,
    `  return projects.find((project) => project.id === id).name ?? id;`,
  ],
  [
    "478 ConditionalExpression -> true",
    `  return projects.find((project) => project.id === id)?.name ?? id;`,
    `  return projects.find((project) => true)?.name ?? id;`,
  ],
];

function failedTests(output) {
  return [...output.matchAll(/^\s+×\s+(.+?)\s+\d+ms$/gm)].map(
    (match) => match[1],
  );
}

const results = [];
for (const [name, search, replacement] of MUTANTS) {
  const occurrences = original.split(search).length - 1;
  if (occurrences !== 1) {
    results.push({
      name,
      verdict: `ANCLA AMBIGUA (${occurrences} apariciones)`,
      tests: [],
    });
    continue;
  }
  writeFileSync(SOURCE, original.replace(search, replacement));
  let output = "";
  try {
    output = execFileSync(
      "pnpm",
      [
        "--dir",
        "frontend",
        "exec",
        "vitest",
        "run",
        "src/github-connector.test.tsx",
      ],
      { encoding: "utf8", shell: true },
    );
  } catch (error) {
    output = `${error.stdout ?? ""}${error.stderr ?? ""}`;
  }
  const tests = failedTests(output);
  results.push({
    name,
    verdict: tests.length > 0 ? "MUERE" : "SOBREVIVE",
    tests,
  });
  console.log(
    `${tests.length > 0 ? "MUERE   " : "SOBREVIVE"}  ${name}  ->  ${tests.join(" | ") || "(nadie falla)"}`,
  );
}

writeFileSync(SOURCE, original);
const dead = results.filter((result) => result.verdict === "MUERE").length;
console.log(`\nTOTAL: ${dead} mueren de ${results.length}`);
writeFileSync(
  "progress/verificacion_mutantes_github_connector.json",
  `${JSON.stringify(results, null, 2)}\n`,
);
