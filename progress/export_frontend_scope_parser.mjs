import fs from "node:fs";
import crypto from "node:crypto";
import ts from "../frontend/node_modules/typescript/lib/typescript.js";
const files = [
  "App.tsx",
  "workspace.tsx",
  "session-gate.tsx",
  "use-session.ts",
  "export-data-api.ts",
  "export-data.tsx",
];
const evidence = [];
for (const file of files) {
  const path = "frontend/src/" + file;
  const text = fs.readFileSync(path, "utf8");
  const source = ts.createSourceFile(
    path,
    text,
    ts.ScriptTarget.Latest,
    true,
    file.endsWith("tsx") ? ts.ScriptKind.TSX : ts.ScriptKind.TS,
  );
  if (source.parseDiagnostics.length) throw new Error("Parse failure " + path);
  const nodes = [];
  function add(node) {
    const a = source.getLineAndCharacterOfPosition(node.getStart(source)),
      b = source.getLineAndCharacterOfPosition(node.end);
    nodes.push({
      kind: ts.SyntaxKind[node.kind],
      range:
        a.line + 1 + ":" + a.character + "-" + (b.line + 1) + ":" + b.character,
      text: node.getText(source),
    });
  }
  function visit(node) {
    if (file === "App.tsx") {
      if (
        ts.isVariableDeclaration(node) &&
        node.name.getText(source) === "exportData"
      )
        add(node);
      if (
        ts.isConditionalExpression(node) &&
        ["exportData", "exportData && username"].includes(
          node.condition.getText(source),
        )
      )
        add(node);
    }
    if (
      file === "workspace.tsx" &&
      ts.isJsxElement(node) &&
      node.openingElement.tagName.getText(source) === "RouteLink" &&
      node.getText(source).includes('href="/exportacion"')
    )
      add(node);
    if (
      file === "session-gate.tsx" &&
      ts.isJsxAttribute(node) &&
      node.name.getText(source) === "username"
    )
      add(node);
    if (
      file === "use-session.ts" &&
      ts.isBinaryExpression(node) &&
      node.getText(source) === 'path === "/exportacion"'
    )
      add(node);
    ts.forEachChild(node, visit);
  }
  if (!file.startsWith("export-data")) visit(source);
  evidence.push({
    path,
    sha256: crypto
      .createHash("sha256")
      .update(text)
      .digest("hex")
      .toUpperCase(),
    scope: file.startsWith("export-data") ? "full" : "nodes",
    nodes,
  });
}
if (evidence.flatMap((e) => e.nodes).length !== 6)
  throw new Error("Expected six complete integration nodes");
fs.writeFileSync(
  "progress/export_frontend_scope_nodes.json",
  JSON.stringify(evidence, null, 2),
);
console.log(
  JSON.stringify(
    evidence.map((e) => ({
      path: e.path,
      scope: e.scope,
      ranges: e.nodes.map((n) => n.range),
    })),
    null,
    2,
  ),
);
