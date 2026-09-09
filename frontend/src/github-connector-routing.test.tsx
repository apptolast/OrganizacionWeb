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
  go("/integraciones");

  render(<App username="owner" />);

  const nav = screen.getByRole("navigation", { name: "Principal" });
  expect(
    screen.getByRole("link", { name: "Conector de GitHub" }),
  ).toHaveAttribute("href", "/integraciones/github");
  expect(
    within(nav).queryByRole("link", { name: "Conector de GitHub" }),
  ).toBeNull();
  expect(within(nav).getAllByRole("link")).toHaveLength(navLinkCount);
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

it("@s31 the connector page is not rendered without a signed-in person", () => {
  go("/integraciones/github");

  render(<App username={null} />);

  expect(
    screen.queryByRole("heading", { level: 1, name: "Conector de GitHub" }),
  ).toBeNull();
});

/** El menú tenía nueve entradas antes del conector y debe seguir teniéndolas. */
const navLinkCount = 9;
