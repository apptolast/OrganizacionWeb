// Recalcula los rangos linea:columna de las configuraciones de Stryker a partir
// del TEXTO que deben cubrir. Escribirlos a mano garantiza que se rompan cada vez
// que alguien anade o quita una linea por encima; derivarlos del texto no.
import { readFileSync, writeFileSync } from "node:fs";
const F =
  "C:/Users/vhurt/OneDrive/Escritorio/Proyectos/OrganizacionWeb/frontend/";

const rango = (fichero, desde, hasta) => {
  const texto = readFileSync(F + fichero, "utf8")
    .split("\r\n")
    .join("\n");
  const i = texto.indexOf(desde);
  if (i < 0)
    throw new Error(
      `inicio no hallado en ${fichero}: ${JSON.stringify(desde.slice(0, 40))}`,
    );
  const k = texto.indexOf(hasta, i);
  if (k < 0)
    throw new Error(
      `final no hallado en ${fichero}: ${JSON.stringify(hasta.slice(0, 40))}`,
    );
  const j = k + hasta.length;
  const coord = (pos) => {
    const antes = texto.slice(0, pos).split("\n");
    return [antes.length, antes[antes.length - 1].length];
  };
  const [l1, c1] = coord(i),
    [l2, c2] = coord(j);
  return `${fichero}:${l1}:${c1}-${l2}:${c2}`;
};

const PLAN = {
  "stryker.ics-calendar.config.json": [
    [
      "src/App.tsx",
      'calendar = route === "/calendario"',
      'calendar = route === "/calendario"',
    ],
    [
      "src/App.tsx",
      'calendar\n                  ? "Calendario"',
      '? "Calendario"',
    ],
    [
      "src/App.tsx",
      "calendar && username ? (",
      "<Calendar owner={username} />",
    ],
    [
      "src/workspace.tsx",
      '<RouteLink\n            href="/calendario"',
      "</RouteLink>",
    ],
  ],
  "stryker.external-calendar.config.json": [
    [
      "src/App.tsx",
      'externalCalendar = route === "/calendario-externo"',
      'externalCalendar = route === "/calendario-externo"',
    ],
    [
      "src/App.tsx",
      // El tramo se para en la RAMA de esta feature, no en el `: null` del final del
      // ternario: llegar hasta alli arrastraba 16 mutantes de otras diez features y
      // los puntuaba como si fueran de esta. Lo caza el juez de cierre de la 28.
      'externalCalendar\n            ? "Calendario externo"',
      '? "Calendario externo"',
    ],
    ["src/App.tsx", "externalCalendar && username ? (", "<ExternalCalendar />"],
    [
      "src/workspace.tsx",
      '<RouteLink\n            href="/calendario-externo"',
      "</RouteLink>",
    ],
  ],
  "stryker.appearance.config.json": [
    [
      "src/App.tsx",
      'appearance = route === "/apariencia"',
      'appearance = route === "/apariencia"',
    ],
    [
      "src/App.tsx",
      'appearance\n                      ? "Apariencia"',
      ": null",
    ],
    ["src/App.tsx", "appearance ? (", "<Appearance />"],
    [
      "src/workspace.tsx",
      '<RouteLink\n            href="/apariencia"',
      "</RouteLink>",
    ],
  ],
};

for (const [config, tramos] of Object.entries(PLAN)) {
  const d = JSON.parse(readFileSync(F + config, "utf8"));
  const nuevos = tramos.map(([f, a, b]) => rango(f, a, b));
  const enteros = d.mutate.filter((m) => !/:\d+:/.test(m));
  const otrosRangos = d.mutate.filter(
    (m) => /:\d+:/.test(m) && !/App\.tsx|workspace\.tsx/.test(m),
  );
  // se conserva el orden original: primero lo que ya estaba antes de los rangos
  const antes = d.mutate.filter((m) => !/:\d+:/.test(m));
  d.mutate = config.includes("appearance")
    ? [...antes, ...nuevos, ...otrosRangos]
    : config.includes("external")
      ? [...antes, ...nuevos]
      : [...nuevos, ...enteros];
  writeFileSync(F + config, JSON.stringify(d, null, 2) + "\n");
  console.log(config);
  for (const r of d.mutate) console.log("   ", r);
}
