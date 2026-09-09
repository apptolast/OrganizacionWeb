import { render, screen } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { App } from "./App";

afterEach(() => {
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
  window.history.replaceState(null, "", "/");
});

function stubSession() {
  vi.stubGlobal(
    "fetch",
    vi.fn(async (url: RequestInfo | URL) => {
      if (String(url) === "/api/v1/me/webhooks") return Response.json({ items: [] });
      if (String(url) === "/api/v1/me/appearance")
        return Response.json(
          {
            configured: false,
            theme: "SYSTEM",
            accentLight: "#244C3C",
            accentDark: "#B7E4C7",
            updatedAt: null,
          },
          { headers: { ETag: '"appearance:unconfigured"' } },
        );
      return Response.json({ items: [], nextCursor: null });
    }),
  );
}

it("@s36 /webhooks shows the Webhooks view under its own heading", async () => {
  window.history.replaceState(null, "", "/webhooks");
  stubSession();

  render(<App username="Ana" />);

  expect(
    await screen.findByRole("heading", { level: 1, name: "Webhooks" }),
  ).toBeVisible();
});

it("@s36 the navigation entry sits right after API para integraciones and Hoy keeps its place", async () => {
  window.history.replaceState(null, "", "/webhooks");
  stubSession();

  render(<App username="Ana" />);
  await screen.findByRole("heading", { level: 1, name: "Webhooks" });

  const links = [
    ...screen.getByRole("navigation", { name: "Principal" }).querySelectorAll("a"),
  ];
  // Decorative markers are aria-hidden, so strip them to compare what a person hears.
  const names = links.map((link) => link.textContent?.replace(/[^\p{L}\s]/gu, "").trim());
  expect(names[0]).toBe("Hoy");
  const api = names.findIndex((name) => name === "API para integraciones");
  expect(api).toBeGreaterThan(0);
  expect(names[api + 1]).toBe("Webhooks");
  expect(links[api + 1]).toHaveAttribute("aria-current", "page");
});

it("@s36 another route does not mark Webhooks as the current page", async () => {
  window.history.replaceState(null, "", "/apariencia");
  stubSession();

  render(<App username="Ana" />);

  const webhooks = [
    ...screen.getByRole("navigation", { name: "Principal" }).querySelectorAll("a"),
  ].find((link) => link.textContent?.trim() === "Webhooks");
  expect(webhooks).toBeDefined();
  expect(webhooks).not.toHaveAttribute("aria-current");
});
