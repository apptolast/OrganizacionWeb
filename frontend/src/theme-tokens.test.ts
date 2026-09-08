// Guardas estáticas del tema: las hojas SCSS solo pueden pintar con tokens de
// los mixins de apariencia. El oráculo de color real es el E2E con axe
// (e2e/today-dark.spec.mjs); esto evita que vuelvan colores fijos.
import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { expect, it } from "vitest";

const FIXED_COLOR = /#[0-9a-fA-F]{3,8}\b/g;
// import.meta.url apunta a http://localhost en jsdom; se lee desde el cwd (frontend/).
const read = (file: string) =>
  readFileSync(resolve(process.cwd(), "src", file), "utf8");

it("today.scss paints notice, summary and agenda cards only with theme tokens (audit #1-#4)", () => {
  expect(read("today.scss").match(FIXED_COLOR)).toBeNull();
});
