import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { App } from "./App";

beforeEach(() => {
  window.history.replaceState(null, "", "/");
  vi.stubGlobal(
    "fetch",
    vi.fn(async (url: string) =>
      String(url).startsWith("/api/v1/me/external-calendar")
        ? Response.json({ configured: false, subscription: null })
        : Response.json({}, { status: 500 }),
    ),
  );
});
afterEach(() => {
  vi.unstubAllGlobals();
  window.history.replaceState(null, "", "/");
});

it("@s37 se llega a /calendario-externo desde el enlace de navegación", async () => {
  render(<App username="Ana" />);
  const link = screen.getByRole("link", { name: "Calendario externo" });
  await userEvent.setup().click(link);
  expect(window.location.pathname).toBe("/calendario-externo");
  expect(link).toHaveAttribute("aria-current", "page");
  expect(
    await screen.findByRole("heading", {
      level: 1,
      name: "Calendario externo",
    }),
  ).toBeVisible();
});

it("@s37 una URL directa autenticada abre la misma vista y marca la navegación", async () => {
  window.history.replaceState(null, "", "/calendario-externo");
  render(<App username="Ana" />);
  expect(
    await screen.findByRole("heading", {
      level: 1,
      name: "Calendario externo",
    }),
  ).toBeVisible();
  expect(
    screen.getByRole("link", { name: "Calendario externo" }),
  ).toHaveAttribute("aria-current", "page");
  expect(screen.getByRole("navigation", { name: "Principal" })).toBeVisible();
});

it("@s37 la miga de pan del encabezado nombra la sección", async () => {
  window.history.replaceState(null, "", "/calendario-externo");
  render(<App username="Ana" />);
  await screen.findByRole("heading", { level: 1, name: "Calendario externo" });
  expect(screen.getByRole("banner")).toHaveTextContent("Calendario externo");
});

it("@s37 sin sesión no se monta la vista privada", () => {
  window.history.replaceState(null, "", "/calendario-externo");
  render(<App />);
  expect(
    screen.queryByRole("heading", { level: 1, name: "Calendario externo" }),
  ).not.toBeInTheDocument();
});
