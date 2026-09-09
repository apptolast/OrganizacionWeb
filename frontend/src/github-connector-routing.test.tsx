import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { render, screen, within } from "@testing-library/react";
import { App } from "./App";

function go(path: string) {
  window.history.pushState({}, "", path);
  window.dispatchEvent(new PopStateEvent("popstate"));
}

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

it("@s36 renders the connector page at /integraciones/github", async () => {
  go("/integraciones/github");

  render(<App username="owner" />);

  expect(
    await screen.findByRole("heading", {
      level: 1,
      name: "Conector de GitHub",
    }),
  ).toBeInTheDocument();
});

it("@s42 the integrations index links the connector without a new menu entry", async () => {
  // El menú de referencia es el que se ve en una ruta ajena al conector. Se compara contra él en
  // lugar de contra un número fijo: otros carriles añaden sus propias entradas, y lo que el
  // contrato exige es que el conector no añada ninguna, no que el menú tenga un tamaño concreto.
  go("/");
  const before = render(<App username="owner" />);
  const menuElsewhere = navHrefs();
  before.unmount();

  go("/integraciones");
  render(<App username="owner" />);

  expect(navHrefs()).toEqual(menuElsewhere);
  expect(navHrefs()).not.toContain("/integraciones/github");
  expect(
    screen.getByRole("link", { name: "Conector de GitHub" }),
  ).toHaveAttribute("href", "/integraciones/github");
  expect(
    within(screen.getByRole("navigation", { name: "Principal" })).queryByRole(
      "link",
      { name: "Conector de GitHub" },
    ),
  ).toBeNull();
});

it("@s42 the connector page itself adds no menu entry either", () => {
  go("/");
  const before = render(<App username="owner" />);
  const menuElsewhere = navHrefs();
  before.unmount();

  go("/integraciones/github");
  render(<App username="owner" />);

  expect(navHrefs()).toEqual(menuElsewhere);
});

it("@s42 the integrations index also links the credentials page of the API", async () => {
  go("/integraciones");

  render(<App username="owner" />);

  // Dentro del contenido, no en el menú, que ya tenía su propio enlace a la API.
  const main = screen.getByRole("main");
  expect(
    within(main).getByRole("link", { name: "API para integraciones" }),
  ).toHaveAttribute("href", "/integraciones/api");
  expect(
    within(main).getByRole("link", { name: "Conector de GitHub" }),
  ).toHaveAttribute("href", "/integraciones/github");
});

it("@s42 el índice de integraciones es enfocable por programa, como el resto del contenido", () => {
  go("/integraciones");

  render(<App username="owner" />);

  // El enlace de salto lleva el foco aquí: sin tabindex negativo el navegador no lo aceptaría.
  expect(screen.getByRole("main")).toHaveAttribute("tabindex", "-1");
});

it("@s31 the connector page is not rendered without a signed-in person", () => {
  go("/integraciones/github");

  render(<App username={null} />);

  expect(
    screen.queryByRole("heading", { level: 1, name: "Conector de GitHub" }),
  ).toBeNull();
});

/** Los destinos del menú principal, en orden, tal y como se ven en la pantalla actual. */
function navHrefs() {
  return within(screen.getByRole("navigation", { name: "Principal" }))
    .getAllByRole("link")
    .map((link) => link.getAttribute("href"));
}
