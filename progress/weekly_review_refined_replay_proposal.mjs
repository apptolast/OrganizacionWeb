import fs from "node:fs";
import crypto from "node:crypto";
import ts from "../frontend/node_modules/typescript/lib/typescript.js";
const ids = [388,389,392,393,394,395,396,397,399,409,410,413,695,155,156,159,163,271,292].map(String);
const inventory = JSON.parse(fs.readFileSync("progress/weekly_review_stryker_inventory.json", "utf8"));
const ranges = [
  ["src/weekly-review.tsx", "37:13-37:52"],
  ["src/weekly-review.tsx", "39:3-45:10"],
  ["src/weekly-review.tsx", "51:7-53:47"],
  ["src/weekly-review.tsx", "283:30-283:50"],
  ["src/weekly-review-api.ts", "93:7-93:65"],
  ["src/weekly-review-api.ts", "138:6-142:6"],
  ["src/weekly-review-api.ts", "147:31-147:70"],
];
const nodes = ranges.map(([file, range]) => {
  const text = fs.readFileSync(`frontend/${file}`, "utf8");
  const source = ts.createSourceFile(file,text,ts.ScriptTarget.Latest,true,file.endsWith("tsx") ? ts.ScriptKind.TSX : ts.ScriptKind.TS);
  if (source.parseDiagnostics.length) throw new Error(`Parse failure ${file}`);
  const [sl,sc,el,ec] = range.split(/[:-]/).map(Number);
  const start = source.getPositionOfLineAndCharacter(sl-1,sc-1);
  const end = source.getPositionOfLineAndCharacter(el-1,ec-1);
  let matching;
  function visit(node) {
    if (node.getStart(source) === start && node.end === end) matching = node;
    ts.forEachChild(node, visit);
  }
  visit(source);
  if (!matching) throw new Error(`No exact node ${file}:${range}`);
  return {file,range:`${sl}:${sc-1}-${el}:${ec-1}`,kind:ts.SyntaxKind[matching.kind],text:matching.getText(source)};
});
const files = ["frontend/src/weekly-review.tsx","frontend/src/weekly-review-api.ts","frontend/src/weekly-review.test.tsx","frontend/src/weekly-review-api.test.ts","frontend/src/styles.scss","e2e/weekly-review-ux.spec.mjs","frontend/stryker.weekly-review.config.json"];
const hashes = files.map(path => ({path,sha256:crypto.createHash("sha256").update(fs.readFileSync(path)).digest("hex").toUpperCase()}));
const signatures = inventory.residuals.filter(m => ids.includes(m.id));
if (signatures.length !== ids.length) throw new Error("Missing original signatures");
fs.writeFileSync("progress/weekly_review_refined_replay_proposal.json", JSON.stringify({originalRawSha256:inventory.rawSha256,hashes,nodes,signatures},null,2)+"\n");
console.log(`${signatures.length} original signatures; ${nodes.length} exact AST nodes; ${hashes.length} hashes`);
