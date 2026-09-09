// Guardas estáticas del tema: las hojas SCSS solo pueden pintar con tokens de
// los mixins de apariencia. El oráculo de color real es el E2E con axe
// (e2e/today-dark.spec.mjs); esto evita que vuelvan colores fijos.
import {
  mkdirSync,
  mkdtempSync,
  readdirSync,
  readFileSync,
  writeFileSync,
} from "node:fs";
import { tmpdir } from "node:os";
import { join, resolve } from "node:path";
import { expect, it } from "vitest";

const FIXED_COLOR = /#[0-9a-fA-F]{3,8}\b/g;
// import.meta.url apunta a http://localhost en jsdom; se lee desde el cwd (frontend/).
const read = (file: string) =>
  readFileSync(resolve(process.cwd(), "src", file), "utf8");
// Recursivo a propósito: una hoja escondida en una subcarpeta pintaría con
// literales sin que la guarda global se enterase. Devuelve rutas relativas a
// la raíz, con barras normales para que el mensaje de fallo sea legible.
const styleSheets = (root: string = resolve(process.cwd(), "src")): string[] =>
  readdirSync(root, { withFileTypes: true }).flatMap((entry) => {
    if (entry.isDirectory())
      return styleSheets(join(root, entry.name)).map(
        (nested) => `${entry.name}/${nested}`,
      );
    return entry.name.endsWith(".scss") ? [entry.name] : [];
  });

it("today.scss paints notice, summary and agenda cards only with theme tokens (audit #1-#4)", () => {
  expect(read("today.scss").match(FIXED_COLOR)).toBeNull();
});

it("history.scss gives select and date controls the same editable tokens as other forms (audit #6)", () => {
  const source = read("history.scss");
  for (const declaration of [
    "background: var(--editable);",
    "color: var(--ink);",
    "border: 1px solid var(--control-border);",
  ])
    expect(source).toContain(declaration);
});

const block = (source: string, selector: string) =>
  source.slice(source.indexOf(`${selector} {`)).split("}")[0];

it("styles.scss draws .empty-divider with the shared --line token (audit #7)", () => {
  expect(block(read("styles.scss"), ".empty-divider")).toContain(
    "background: var(--line);",
  );
});

const SEED_TOKENS = [
  "--seed-stem",
  "--seed-leaf",
  "--seed-leaf-alt",
  "--seed-soil",
];
// Los valores que el tema claro ha tenido siempre: la corrección #8 es deuda de
// tokens, no un rediseño, así que en claro nada puede cambiar.
const LIGHT_SEED = {
  "--seed-stem": "#7a9863",
  "--seed-leaf": "#a8bb88",
  "--seed-leaf-alt": "#7d9b61",
  "--seed-soil": "#b3c59e",
};
const DARK_PANEL = "#1f2937";
const VISIBLE_ON_PANEL = 3;
const NOT_GLARING_ON_PANEL = 6;

const region = (source: string, from: string, to: string) =>
  source.slice(source.indexOf(from), source.indexOf(to));
const mixin = (source: string, name: string) =>
  source.slice(source.indexOf(`@mixin ${name} {`)).split("}")[0];
const tokenValue = (mixinBody: string, token: string) =>
  mixinBody.match(new RegExp(`${token}:\\s*([^;]+);`))?.[1];

const channel = (value: number) => {
  const c = value / 255;
  return c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4;
};
const luminance = (color: string) =>
  [1, 3, 5]
    .map((start) => channel(parseInt(color.slice(start, start + 2), 16)))
    .reduce(
      (total, c, index) => total + [0.2126, 0.7152, 0.0722][index] * c,
      0,
    );
const contrast = (first: string, second: string) => {
  const [dark, light] = [luminance(first), luminance(second)].sort(
    (a, b) => a - b,
  );
  return (light + 0.05) / (dark + 0.05);
};

it("the seed art paints only with theme tokens (audit #8)", () => {
  const art = region(read("styles.scss"), ".seed-art {", ".empty-divider {");
  expect(art.match(FIXED_COLOR)).toBeNull();
  for (const token of SEED_TOKENS) expect(art).toContain(`var(${token})`);
});

it("both appearance mixins define every seed token (audit #8)", () => {
  const source = read("styles.scss");
  for (const name of ["light-appearance", "dark-appearance"])
    for (const token of SEED_TOKENS)
      expect(tokenValue(mixin(source, name), token)).toBeDefined();
});

it("the light seed tokens keep the historical greens so the light theme does not change (audit #8)", () => {
  const light = mixin(read("styles.scss"), "light-appearance");
  for (const [token, value] of Object.entries(LIGHT_SEED))
    expect(tokenValue(light, token)).toBe(value);
});

it("the dark seed tokens stay visible on the dark panel without glaring (audit #8)", () => {
  const dark = mixin(read("styles.scss"), "dark-appearance");
  for (const token of SEED_TOKENS) {
    const value = tokenValue(dark, token)!;
    expect(value).toMatch(/^#[0-9a-f]{6}$/);
    const ratio = contrast(value, DARK_PANEL);
    expect(ratio).toBeGreaterThanOrEqual(VISIBLE_ON_PANEL);
    expect(ratio).toBeLessThanOrEqual(NOT_GLARING_ON_PANEL);
  }
});

it("the dark seed tokens keep the shading order of the light art (audit #8)", () => {
  const source = read("styles.scss");
  const order = (name: string) =>
    [...SEED_TOKENS]
      .sort(
        (first, second) =>
          luminance(tokenValue(mixin(source, name), first)!) -
          luminance(tokenValue(mixin(source, name), second)!),
      )
      .join(" ");
  expect(order("dark-appearance")).toBe(order("light-appearance"));
});

// Regla de fondo: los mixins de apariencia son el único sitio donde un color
// puede ser literal; el resto de las hojas pinta siempre con var(--token). Sin
// esta guarda global cualquier hoja nueva puede repetir el blanco fijo de Hoy.
const APPEARANCE_MIXINS = /@mixin (?:light|dark)-appearance \{[^}]*\}/g;
const COLOR_DECLARATION =
  /(?:^|[\s;{])(color|background|background-color|border|border-color|border-top|border-bottom|border-left|border-right|border-inline|border-inline-start|border-inline-end|border-block|outline|outline-color|box-shadow|text-shadow|fill|stroke|caret-color|accent-color|text-decoration-color|column-rule)\s*:\s*([^;{}]+)/g;
const LITERAL_COLOR =
  /#[0-9a-fA-F]{3,8}\b|\brgba?\(|\bhsla?\(|\b(?:white|black|red|blue|green|gray|grey|silver|whitesmoke|gainsboro|ivory|snow|beige|linen|lightgray|lightgrey|azure|mintcream|honeydew)\b/;
// Una sombra de negro puro no aporta color: se ve igual en los dos temas. La
// exención se decide capa por capa, nunca sobre la declaración entera.
const NEUTRAL_BLACK = /rgb\(0 0 0 \/ \d+%\)/g;
const SHADOW_LAYER_SEPARATOR = /,(?![^(]*\))/;
const SHADOW_PROPERTY = /shadow$/;

const isNeutralShadow = (value: string) =>
  value
    .split(SHADOW_LAYER_SEPARATOR)
    .every((layer) => !LITERAL_COLOR.test(layer.replace(NEUTRAL_BLACK, "")));

const literalColorsOutsideTokens = (source: string) =>
  [...source.replace(APPEARANCE_MIXINS, "").matchAll(COLOR_DECLARATION)]
    .map(([, property, value]) => [property, value.trim()] as const)
    .filter(
      ([property, value]) =>
        LITERAL_COLOR.test(value) &&
        !(SHADOW_PROPERTY.test(property) && isNeutralShadow(value)),
    )
    .map(([property, value]) => `${property}: ${value}`);

it("the literal-color scan catches the fixed white that broke Hoy in dark (audit #1)", () => {
  expect(
    literalColorsOutsideTokens(".today-summary { background: #fff; }"),
  ).toEqual(["background: #fff"]);
});

// El juez encontró que la excepción de sombra neutra eximía la declaración
// entera si *terminaba* en una capa neutra, así que una capa clara colada
// delante atravesaba las dos guardas.
const GLARING_SHADOW =
  "box-shadow: 0 0 8px #ffffff, 0 5px 18px rgb(0 0 0 / 6%)";

it("the literal-color scan catches a light shadow layer hidden before a neutral one (juez #2)", () => {
  expect(literalColorsOutsideTokens(`.card { ${GLARING_SHADOW}; }`)).toEqual([
    GLARING_SHADOW,
  ]);
});

it("a shadow is neutral only when every one of its layers is (juez #2)", () => {
  expect(isNeutralShadow("0 5px 18px rgb(0 0 0 / 6%)")).toBe(true);
  expect(isNeutralShadow("0 0 8px #ffffff, 0 5px 18px rgb(0 0 0 / 6%)")).toBe(
    false,
  );
  expect(isNeutralShadow("0 5px 18px rgb(0 0 0 / 6%), 0 0 8px #ffffff")).toBe(
    false,
  );
});

// Una hoja en subcarpeta se escapaba del barrido entero: readdirSync no es
// recursivo. Hoy no existe ninguna, pero la guarda promete cubrirlas.
it("the stylesheet scan reaches sheets nested in subdirectories (juez #3)", () => {
  const root = mkdtempSync(join(tmpdir(), "scss-scan-"));
  mkdirSync(join(root, "partials", "deep"), { recursive: true });
  writeFileSync(join(root, "top.scss"), "");
  writeFileSync(join(root, "partials", "mid.scss"), "");
  writeFileSync(join(root, "partials", "deep", "low.scss"), "");
  writeFileSync(join(root, "partials", "notes.md"), "");
  expect(styleSheets(root).sort()).toEqual([
    "partials/deep/low.scss",
    "partials/mid.scss",
    "top.scss",
  ]);
});

it("no stylesheet declares a fixed color outside the appearance mixins (audit #1-#8)", () => {
  const sheets = styleSheets();
  expect(sheets).toContain("today.scss");
  expect(sheets).toContain("styles.scss");
  expect(
    sheets.flatMap((sheet) =>
      literalColorsOutsideTokens(read(sheet)).map(
        (declaration) => `${sheet} -> ${declaration}`,
      ),
    ),
  ).toEqual([]);
});

it("card shadows are neutral black so no fixed green survives in dark (audit #8)", () => {
  const shadows = [...read("styles.scss").matchAll(/box-shadow:\s*([^;]+);/g)]
    .map(([, value]) => value)
    .filter((value) => value !== "none");
  expect(shadows.length).toBeGreaterThan(0);
  expect(shadows.filter((shadow) => !isNeutralShadow(shadow))).toEqual([]);
});
