import { render, screen, within } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { App } from "./App";

function go(path: string) {
  window.history.pushState({}, "", path);
  window.dispatchEvent(new PopStateEvent("popstate"));
}

beforeEach(() => {
  vi.stubGlobal("fetch", () =>
    Promise.resolve(
      new Response(JSON.stringify({ items: [], nextCursor: null }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    ),
  );
});

afterEach(() => {
  vi.unstubAllGlobals();
  go("/");
});

describe("automations route", () => {
  it("@s37 heads /automatizaciones and marks its navigation entry as current", async () => {
    go("/automatizaciones");
    render(<App username="owner" />);
    expect(
      await screen.findByRole("heading", {
        level: 1,
        name: "Automatizaciones",
      }),
    ).toBeInTheDocument();
    const nav = screen.getByRole("navigation", { name: "Principal" });
    const entry = within(nav).getByRole("link", { name: "Automatizaciones" });
    expect(entry).toHaveAttribute("href", "/automatizaciones");
    expect(entry).toHaveAttribute("aria-current", "page");
  });

  it("@s37 adds the entry without displacing «Hoy» from the top of the navigation", () => {
    go("/");
    render(<App username="owner" />);
    const nav = screen.getByRole("navigation", { name: "Principal" });
    const links = within(nav).getAllByRole("link");
    expect(links[0]).toHaveTextContent("Hoy");
    expect(links.map((link) => link.getAttribute("href"))).toContain(
      "/automatizaciones",
    );
    expect(
      within(nav).getByRole("link", { name: "Automatizaciones" }),
    ).not.toHaveAttribute("aria-current");
  });

  it("@s37 keeps every other route untouched", () => {
    go("/exportacion");
    render(<App username="owner" />);
    const nav = screen.getByRole("navigation", { name: "Principal" });
    expect(
      within(nav).getByRole("link", { name: "Exportación" }),
    ).toHaveAttribute("aria-current", "page");
    expect(
      within(nav).getByRole("link", { name: "Automatizaciones" }),
    ).not.toHaveAttribute("aria-current");
  });
});
