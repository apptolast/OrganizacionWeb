import fs from "node:fs";
import crypto from "node:crypto";
import ts from "../frontend/node_modules/typescript/lib/typescript.js";
const files = ["App.tsx", "workspace.tsx"];
const evidence = [];
for (const file of files) {
  const path = `frontend/src/${file}`;
  const text = fs.readFileSync(path, "utf8");
  const source = ts.createSourceFile(path, text, ts.ScriptTarget.Latest, true, ts.ScriptKind.TSX);
  if (source.parseDiagnostics.length) throw new Error(`Parse failed: ${path}`);
  const nodes = [];
  function add(node, label) {
    const start = source.getLineAndCharacterOfPosition(node.getStart(source));
    const end = source.getLineAndCharacterOfPosition(node.end);
    nodes.push({ label, kind: ts.SyntaxKind[node.kind], range: `${start.line + 1}:${start.character}-${end.line + 1}:${end.character}`, text: node.getText(source) });
  }
  function visit(node) {
    if (file === "App.tsx") {
      if (ts.isVariableDeclaration(node) && node.name.getText(source) === "weeklyReview") add(node, "weekly review route recognition");
      if (ts.isConditionalExpression(node) && node.condition.getText(source) === "weeklyReview") {
        add(node, "complete weekly review conditional");

      }
    } else if (ts.isJsxElement(node) && node.openingElement.tagName.getText(source) === "RouteLink" && node.getText(source).includes("/revision-semanal")) add(node, "weekly review link");
    ts.forEachChild(node, visit);
  }
  visit(source);
  evidence.push({path, sha256: crypto.createHash("sha256").update(text).digest("hex").toUpperCase(), nodes});
}
for (const file of ["weekly-review-api.ts", "weekly-review.tsx"]) {
 const path = `frontend/src/${file}`;
 const text = fs.readFileSync(path, "utf8");
 const source = ts.createSourceFile(path, text, ts.ScriptTarget.Latest, true, file.endsWith("tsx") ? ts.ScriptKind.TSX : ts.ScriptKind.TS);
 if(source.parseDiagnostics.length) throw new Error(`Parse failed: ${path}`);
 evidence.push({path,sha256:crypto.createHash("sha256").update(text).digest("hex").toUpperCase(),scope:"full"});
}
fs.writeFileSync("progress/weekly_review_mutation_scope_nodes.json", JSON.stringify(evidence, null, 2));
console.log(JSON.stringify(evidence, null, 2));
