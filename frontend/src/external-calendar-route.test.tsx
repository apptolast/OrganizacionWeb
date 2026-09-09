import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { App } from "./App";
import { SessionGate } from "./session-gate";

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

const subscription = {
  id: "11111111-2222-3333-4444-555555555555",
  label: "Trabajo",
  urlHost: "calendar.google.com",
  urlTail: ".ics",
  lastAttemptAt: "2030-01-07T11:00:00Z",
  lastSyncAt: "2030-01-07T11:00:00Z",
  lastStatus: "OK",
  lastError: null,
  snapshotZoneId: "Europe/Madrid",
  imported: 12,
  skippedRecurring: 3,
  skippedCancelled: 1,
  skippedInvalid: 0,
  truncated: false,
  updatedAt: "2030-01-07T11:00:00Z",
};

/**
 * @s39 fila 4 y @s37 «retorno tras iniciar sesión». Se prueba sobre SessionGate y no sobre
 * ExternalCalendar aislado porque la conducta que fija la fila es de la sesión, no de la vista:
 * /calendario-externo vuelve a «/» al reabrir sesión exactamente porque NO figura en la lista
 * blanca `isPrivateRoute` de use-session.ts. Añadirlo allí —como la feature 24 hizo con
 * /integraciones/api— dejaría la ruta viva y hoy no lo notaría ninguna prueba.
 */
function sessionServer() {
  let authenticated = true;
  return vi.fn(async (url: string, options: RequestInit = {}) => {
    const address = String(url);
    if (address === "/api/session/logout") {
      authenticated = false;
      return new Response(null, { status: 204 });
    }
    if (address === "/api/session" && options.method === "POST") {
      authenticated = true;
      return new Response(null, { status: 204 });
    }
    if (address === "/api/session")
      return Response.json({
        authenticated,
        username: authenticated ? "Ana" : null,
        csrfToken: "session-csrf",
        csrfHeaderName: "X-CSRF-TOKEN",
      });
    if (address === "/api/v1/me/appearance")
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
    if (address.startsWith("/api/v1/me/external-calendar/events"))
      return Response.json({
        configured: true,
        lastSyncAt: "2030-01-07T11:00:00Z",
        lastStatus: "OK",
        items: [
          {
            uid: "u1",
            summary: "Reunión",
            startAt: "2030-01-07T08:00:00Z",
            endAt: "2030-01-07T09:00:00Z",
            allDay: false,
          },
        ],
      });
    if (address.startsWith("/api/v1/me/external-calendar"))
      return Response.json({ configured: true, subscription });
    return Response.json({}, { status: 500 });
  });
}

it("@s39 al cerrar sesión desaparecen los datos y el retorno tras iniciar sesión reinicia la ruta a / (@s37)", async () => {
  window.history.replaceState(null, "", "/calendario-externo");
  vi.stubGlobal("fetch", sessionServer());
  const user = userEvent.setup();
  render(<SessionGate />);

  // Estado de partida: host, contadores y lista visibles en /calendario-externo.
  expect(await screen.findByText("calendar.google.com")).toBeInTheDocument();
  expect(
    screen.getByText(
      "12 eventos, 3 recurrentes no incluidos, 1 cancelado, 0 inválidos",
    ),
  ).toBeInTheDocument();
  expect(await screen.findByText("Reunión")).toBeInTheDocument();
  // Un borrador a medio escribir, que la fila prohíbe conservar.
  await user.type(
    screen.getByLabelText("Dirección secreta iCal"),
    "https://calendar.google.com/borrador.ics",
  );

  await user.click(screen.getByRole("button", { name: "Cerrar sesión" }));

  expect(await screen.findByLabelText("Usuario")).toBeVisible();
  expect(screen.queryByText("calendar.google.com")).not.toBeInTheDocument();
  expect(screen.queryByText(".ics")).not.toBeInTheDocument();
  expect(screen.queryByText(/12 eventos/)).not.toBeInTheDocument();
  expect(screen.queryByText("Reunión")).not.toBeInTheDocument();
  expect(document.body.textContent).not.toContain("borrador.ics");

  await user.type(screen.getByLabelText("Usuario"), "Ana");
  await user.type(screen.getByLabelText("Contraseña"), "not-a-real-password");
  await user.click(screen.getByRole("button", { name: "Iniciar sesión" }));

  await screen.findByRole("navigation", { name: "Principal" });
  expect(window.location.pathname).toBe("/");
  expect(
    screen.queryByRole("heading", { level: 1, name: "Calendario externo" }),
  ).not.toBeInTheDocument();

  // Y al volver a entrar, el borrador no reaparece: el campo está vacío.
  await user.click(screen.getByRole("link", { name: "Calendario externo" }));
  expect(await screen.findByLabelText("Dirección secreta iCal")).toHaveValue(
    "",
  );
});
