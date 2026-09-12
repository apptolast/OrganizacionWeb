import { render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { AppearanceProvider, useAppearance } from "./appearance-state";

const LIGHT_CANVAS = "#f8f9f5";
const DARK_CANVAS = "#111827";
const snapshot = (theme: string) =>
  Response.json(
    {
      configured: true,
      theme,
      accentLight: "#244C3C",
      accentDark: "#B7E4C7",
      updatedAt: "2026-09-08T10:00:00Z",
    },
    {
      headers: {
        ETag: '"appearance:12345678-1234-1234-1234-123456789abc:0"',
      },
    },
  );
let meta: HTMLMetaElement;
let sheet: HTMLStyleElement;
beforeEach(() => {
  sheet = document.createElement("style");
  sheet.textContent = `:root{--canvas:${LIGHT_CANVAS}}[data-theme="dark"]{--canvas:${DARK_CANVAS}}`;
  meta = document.createElement("meta");
  meta.name = "theme-color";
  meta.content = "#000000";
  document.head.append(sheet, meta);
});
afterEach(() => {
  sheet.remove();
  meta.remove();
  vi.unstubAllGlobals();
});

it("index.html declares a theme-color per color scheme with the real --canvas values (audit #5)", () => {
  const html = readFileSync(resolve(process.cwd(), "index.html"), "utf8");
  const metas = [...html.matchAll(/<meta\s+name="theme-color"([^>]*)>/g)].map(
    ([, attributes]) => attributes.replace(/\s+/g, " ").trim(),
  );
  expect(metas).toEqual([
    `content="${LIGHT_CANVAS}" media="(prefers-color-scheme: light)" /`,
    `content="${DARK_CANVAS}" media="(prefers-color-scheme: dark)" /`,
  ]);
});

it("applying DARK paints theme-color with the computed dark canvas (audit #5)", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation(() => snapshot("DARK")),
  );
  render(<AppearanceProvider>{null}</AppearanceProvider>);
  await waitFor(() =>
    expect(document.documentElement.dataset.theme).toBe("dark"),
  );
  expect(meta.content).toBe(DARK_CANVAS);
});

it("applying LIGHT paints theme-color with the computed light canvas (audit #5)", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation(() => snapshot("LIGHT")),
  );
  render(<AppearanceProvider>{null}</AppearanceProvider>);
  await waitFor(() =>
    expect(document.documentElement.dataset.theme).toBe("light"),
  );
  expect(meta.content).toBe(LIGHT_CANVAS);
});

// SYSTEM es el valor por defecto: la rama por la que pasa casi todo el mundo.
const SYSTEM_DARK_QUERY = "(prefers-color-scheme: dark)";
const systemPrefers = (dark: boolean) => {
  const media = Object.assign(new EventTarget(), { matches: dark });
  const matchMedia = vi.fn().mockReturnValue(media);
  vi.stubGlobal("matchMedia", matchMedia);
  return matchMedia;
};

it.each([
  { os: "dark", dark: true, theme: "dark", canvas: DARK_CANVAS },
  { os: "light", dark: false, theme: "light", canvas: LIGHT_CANVAS },
])(
  "SYSTEM follows a $os operating system for canvas, data-theme and theme-color (@s24)",
  async ({ dark, theme, canvas }) => {
    const matchMedia = systemPrefers(dark);
    vi.stubGlobal(
      "fetch",
      vi.fn().mockImplementation(() => snapshot("SYSTEM")),
    );
    render(<AppearanceProvider>{null}</AppearanceProvider>);
    await waitFor(() =>
      expect(document.documentElement.dataset.theme).toBe(theme),
    );
    expect(matchMedia).toHaveBeenCalledWith(SYSTEM_DARK_QUERY);
    expect(document.documentElement.style.colorScheme).toBe(theme);
    expect(
      getComputedStyle(document.documentElement)
        .getPropertyValue("--canvas")
        .trim(),
    ).toBe(canvas);
    expect(meta.content).toBe(canvas);
  },
);

it.each([
  { preference: "DARK", theme: "dark" },
  { preference: "LIGHT", theme: "light" },
])(
  "an explicit $preference preference never consults the operating system (@s24)",
  async ({ preference, theme }) => {
    const matchMedia = systemPrefers(preference === "LIGHT");
    vi.stubGlobal(
      "fetch",
      vi.fn().mockImplementation(() => snapshot(preference)),
    );
    render(<AppearanceProvider>{null}</AppearanceProvider>);
    await waitFor(() =>
      expect(document.documentElement.dataset.theme).toBe(theme),
    );
    expect(matchMedia).not.toHaveBeenCalled();
  },
);

it("leaving the session returns theme-color to the system canvas (@s32)", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation(() => snapshot("DARK")),
  );
  const { unmount } = render(<AppearanceProvider>{null}</AppearanceProvider>);
  await waitFor(() => expect(meta.content).toBe(DARK_CANVAS));
  unmount();
  expect(document.documentElement.dataset.theme).toBeUndefined();
  expect(meta.content).toBe(LIGHT_CANVAS);
});

// Carrera del run 34610657835 (Stryker 10/12): el formulario ya mostraba la
// apariencia confirmada y el documento seguía sin tema, porque el tema se
// aplicaba en un efecto pasivo que React agenda para una tarea posterior.
// El MutationObserver dispara justo tras el commit, antes de esa tarea.
function ConfirmedTheme() {
  const { snapshot } = useAppearance();
  return snapshot ? (
    <p data-testid="confirmed-theme">{snapshot.theme}</p>
  ) : null;
}
it.each([
  { preference: "DARK", theme: "dark", canvas: DARK_CANVAS },
  { preference: "LIGHT", theme: "light", canvas: LIGHT_CANVAS },
])(
  "the document already wears $preference in the same commit that shows it",
  async ({ preference, theme, canvas }) => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockImplementation(() => snapshot(preference)),
    );
    let painted:
      { theme?: string; colorScheme: string; meta: string } | undefined;
    const observer = new MutationObserver(() => {
      if (!painted && document.querySelector('[data-testid="confirmed-theme"]'))
        painted = {
          theme: document.documentElement.dataset.theme,
          colorScheme: document.documentElement.style.colorScheme,
          meta: meta.content,
        };
    });
    observer.observe(document.body, { childList: true, subtree: true });
    try {
      render(
        <AppearanceProvider>
          <ConfirmedTheme />
        </AppearanceProvider>,
      );
      await screen.findByTestId("confirmed-theme");
    } finally {
      observer.disconnect();
    }
    expect(painted).toEqual({ theme, colorScheme: theme, meta: canvas });
  },
);
