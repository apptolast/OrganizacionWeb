import fs from "node:fs";
import crypto from "node:crypto";
import ts from "../frontend/node_modules/typescript/lib/typescript.js";
const files = [
  "import-data-api.ts",
  "import-data-intent.ts",
  "import-data.tsx",
  "App.tsx",
  "workspace.tsx",
  "use-session.ts",
  "appearance-state.tsx",
  "customization-state.ts",
];
const evidence = files.map((file) => {
  const path = "frontend/src/" + file;
  const text = fs.readFileSync(path, "utf8");
  const source = ts.createSourceFile(
    path,
    text,
    ts.ScriptTarget.Latest,
    true,
    file.endsWith("tsx") ? ts.ScriptKind.TSX : ts.ScriptKind.TS,
  );
  if (source.parseDiagnostics.length) throw new Error("Invalid AST " + path);
  const nodes = [];
  function add(node) {
    const a = source.getLineAndCharacterOfPosition(node.getStart(source));
    const b = source.getLineAndCharacterOfPosition(node.end);
    nodes.push({
      kind: ts.SyntaxKind[node.kind],
      range: `${a.line + 1}:${a.character}-${b.line + 1}:${b.character}`,
      text: node.getText(source),
    });
  }
  function visit(node) {
    const body = node.getText(source);
    if (
      file === "App.tsx" &&
      ((ts.isVariableDeclaration(node) &&
        ["importData", "appearanceState"].includes(
          node.name.getText(source),
        )) ||
        (ts.isConditionalExpression(node) &&
          ["importData", "importData && username"].includes(
            node.condition.getText(source),
          )))
    )
      add(node);
    if (
      file === "workspace.tsx" &&
      ts.isJsxElement(node) &&
      node.openingElement.tagName.getText(source) === "RouteLink" &&
      body.includes('href="/importacion"')
    )
      add(node);
    if (file === "use-session.ts") {
      if (
        ts.isFunctionDeclaration(node) &&
        node.name?.text === "retireImportRecovery"
      )
        add(node);
      if (
        ts.isIfStatement(node) &&
        node.expression.getText(source) ===
          "next.authenticated && next.username !== null"
      )
        add(node);
      if (
        ts.isExpressionStatement(node) &&
        body === "retireImportRecovery();"
      ) {
        if (!ts.isIfStatement(node.parent)) add(node);
      }
      if (ts.isBinaryExpression(node) && body === 'path === "/importacion"')
        add(node);
    }
    if (
      ["appearance-state.tsx", "customization-state.ts"].includes(file) &&
      ts.isPropertyAssignment(node) &&
      node.name.getText(source) === "refreshAfterImport"
    )
      add(node);
    ts.forEachChild(node, visit);
  }
  const full = file.startsWith("import-data");
  if (!full) visit(source);
  return {
    path,
    sha256: crypto.createHash("sha256").update(text).digest("hex"),
    scope: full ? "full" : "nodes",
    nodes,
  };
});
const config = JSON.parse(
  fs.readFileSync("frontend/stryker.export-data.config.json", "utf8"),
);
config.mutate = evidence.flatMap((e) =>
  e.scope === "full"
    ? [e.path.replace("frontend/", "")]
    : e.nodes.map((n) => e.path.replace("frontend/", "") + ":" + n.range),
);
config.jsonReporter.fileName = "reports/mutation-import-data/mutation.json";
config.htmlReporter.fileName = "reports/mutation-import-data/mutation.html";
config.tempDirName = ".stryker-tmp-import-data";
if (process.argv.includes("--verify")) {
  const actual = JSON.parse(fs.readFileSync("frontend/stryker.import-data.config.json", "utf8"));
  if (JSON.stringify(actual) !== JSON.stringify(config)) throw new Error("Selected configuration differs from expected scope/settings");
} else fs.writeFileSync("frontend/stryker.import-data.config.json", JSON.stringify(config, null, 2) + "\n");
fs.writeFileSync(
  "progress/import_frontend_scope_nodes.json",
  JSON.stringify(evidence, null, 2) + "\n",
);
console.log(JSON.stringify(config.mutate, null, 2));
