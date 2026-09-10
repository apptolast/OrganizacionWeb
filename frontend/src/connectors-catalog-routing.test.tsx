import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { render, screen, within } from "@testing-library/react";
import { App } from "./App";

/**
 * Condición C1 del juez de cierre de la feature 29: las dos rutas del catálogo se
 * cablearon en `App.tsx` sin ningún oráculo. Se podían borrar las cuatro líneas y la
 * suite seguía verde, que es la definición de una prueba que falta.
 *
 * Aquí se afirman las tres cosas que el cableado promete: que cada ruta pinta su
 * pantalla, que la entrada de menú existe y apunta al catálogo, y que ocupa la
 * posición que fija @s33 —después de «Importación»—, comparada por nombre contra la
 * lista entera y no por índice, porque un índice caduca en cuanto otra feature añade
 * su ruta.
 */

function go(path: string) {
  window.history.pushState({}, "", path);
  window.dispatchEvent(new PopStateEvent("popstate"));
}

const navNames = () =>
  within(screen.getByRole("navigation", { name: "Principal" }))
    .getAllByRole("link")
    .map((link) => link.textContent?.replace(/[^\p{L}\s]/gu, "").trim() ?? "");

beforeEach(() => {
  vi.stubGlobal(
    "fetch",
    vi.fn((url: string) =>
      Promise.resolve(
        url.startsWith("/api/v1/projects")
          ? Response.json({ items: [], nextCursor: null })
          : new Response(null, { status: 404 }),
      ),
    ),
  );
});

afterEach(() => {
  vi.unstubAllGlobals();
  go("/");
});

it("@s33 /conectores pinta el catálogo bajo su propio encabezado", async () => {
  go("/conectores");

  render(<App username="owner" />);

  expect(
    await screen.findByRole("heading", { level: 1, name: "Conectores" }),
  ).toBeInTheDocument();
});

it("@s8 /conectores/gitlab pinta la pantalla del conector de GitLab", async () => {
  go("/conectores/gitlab");

  render(<App username="owner" />);

  expect(
    await screen.findByRole("heading", {
      level: 1,
      name: "Conector de GitLab",
    }),
  ).toBeInTheDocument();
});

it("@s33 la entrada del menú lleva al catálogo y va detrás de «Importación»", async () => {
  go("/");

  render(<App username="owner" />);

  const entrada = within(
    screen.getByRole("navigation", { name: "Principal" }),
  ).getByRole("link", { name: "Conectores" });
  expect(entrada).toHaveAttribute("href", "/conectores");

  // Por posición relativa y no por índice: lo que el contrato fija es que vaya
  // inmediatamente después de «Importación», no que el menú tenga catorce entradas.
  const nombres = navNames();
  expect(nombres[nombres.indexOf("Importación") + 1]).toBe("Conectores");
});

it("@s33 el catálogo se marca como sección actual sólo cuando se está en él", async () => {
  go("/conectores");
  const dentro = render(<App username="owner" />);
  expect(
    within(screen.getByRole("navigation", { name: "Principal" })).getByRole(
      "link",
      { name: "Conectores" },
    ),
  ).toHaveAttribute("aria-current", "page");
  dentro.unmount();

  go("/");
  render(<App username="owner" />);
  expect(
    within(screen.getByRole("navigation", { name: "Principal" })).getByRole(
      "link",
      { name: "Conectores" },
    ),
  ).not.toHaveAttribute("aria-current");
});

it("@s8 la pantalla de GitLab también marca «Conectores» como sección actual", async () => {
  go("/conectores/gitlab");

  render(<App username="owner" />);

  await screen.findByRole("heading", { level: 1, name: "Conector de GitLab" });
  expect(
    within(screen.getByRole("navigation", { name: "Principal" })).getByRole(
      "link",
      { name: "Conectores" },
    ),
  ).toHaveAttribute("aria-current", "page");
});
