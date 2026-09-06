vi.mock("./project-tasks", () => ({ ProjectTasks: () => null }));
import { render, screen, act, fireEvent } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { App } from "./App";
import userEvent from "@testing-library/user-event";
import { StrictMode } from "react";
import { RouteLink } from "./navigation";
const summary = {
  id: "6c5dbd10-9ad5-4000-8000-000000000001",
  name: "Zenit Digital",
  status: "idea",
  createdAt: "2026-09-05T12:00:00Z",
  updatedAt: "2026-09-05T12:00:00Z",
};
it("@s15 orienta solo el vacío confirmado y enlaza el formulario existente", async () => {
  window.history.replaceState(null, "", "/proyectos");
  vi.spyOn(globalThis, "fetch").mockResolvedValue(
    Response.json({ items: [], nextCursor: null }),
  );
  render(<App />);
  expect(await screen.findByText("Todavía no tienes proyectos")).toBeVisible();
  expect(screen.getByRole("link", { name: "Crear proyecto" })).toHaveAttribute(
    "href",
    "/proyectos/nuevo",
  );
  expect(screen.queryByRole("status")).not.toBeInTheDocument();
});
it.each([
  ["/proyectos", null],
  ["/proyectos", { items: "invalid", nextCursor: null }],
  [
    "/proyectos",
    { items: [{ ...summary, createdAt: "invalid" }], nextCursor: null },
  ],
  ["/proyectos", { items: [{ ...summary, id: 42 }], nextCursor: null }],
  ["/proyectos", { items: [summary], nextCursor: 7 }],
  [
    `/proyectos/${summary.id}`,
    { ...summary, ownerId: "owner", description: "", updatedAt: "invalid" },
  ],
  [
    `/proyectos/${summary.id}`,
    { ...summary, ownerId: "owner", description: 42 },
  ],
  [
    `/proyectos/${summary.id}`,
    { ...summary, ownerId: "owner", description: "", id: "otra-ruta" },
  ],
])(
  "una respuesta200 incompatible en %s no causa falso vacío, detalle ni excepción",
  async (route, data) => {
    window.history.replaceState(null, "", route);
    vi.spyOn(globalThis, "fetch").mockResolvedValue(Response.json(data));
    render(<App />);
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "No hemos podido cargar",
    );
    expect(screen.queryByRole("list")).not.toBeInTheDocument();
    expect(
      screen.queryByRole("heading", { name: summary.name }),
    ).not.toBeInTheDocument();
  },
);
it("@s22 ignora abortos y datos obsoletos durante la repetición de efectos StrictMode", async () => {
  window.history.replaceState(null, "", "/proyectos");
  let complete!: (response: Response) => void;
  vi.spyOn(globalThis, "fetch")
    .mockImplementationOnce(
      () =>
        new Promise((resolve) => {
          complete = resolve;
        }),
    )
    .mockResolvedValueOnce(
      Response.json({ items: [summary], nextCursor: null }),
    );
  render(
    <StrictMode>
      <App />
    </StrictMode>,
  );
  await screen.findByRole("link", { name: summary.name });
  await act(async () =>
    complete(
      Response.json({
        items: [{ ...summary, name: "Obsoleto" }],
        nextCursor: null,
      }),
    ),
  );
  expect(screen.getByRole("link", { name: summary.name })).toBeVisible();
  expect(screen.queryByText("Obsoleto")).not.toBeInTheDocument();
});
it("@s22 cancela lecturas previas y una respuesta tardía no sustituye la ruta actual", async () => {
  window.history.replaceState(null, "", "/proyectos");
  let complete!: (response: Response) => void;
  const request = vi
    .spyOn(globalThis, "fetch")
    .mockImplementationOnce(
      () =>
        new Promise((resolve) => {
          complete = resolve;
        }),
    )
    .mockResolvedValueOnce(
      Response.json({
        ...summary,
        ownerId: "owner",
        description: "Detalle actual",
      }),
    );
  render(<App />);
  act(() => {
    window.history.pushState(null, "", `/proyectos/${summary.id}`);
    window.dispatchEvent(new PopStateEvent("popstate"));
  });
  expect(await screen.findByText("Detalle actual")).toBeVisible();
  expect(request.mock.calls[0][1]?.signal?.aborted).toBe(true);
  await act(async () =>
    complete(
      Response.json({
        items: [{ ...summary, name: "Obsoleto" }],
        nextCursor: null,
      }),
    ),
  );
  expect(screen.getByText("Detalle actual")).toBeVisible();
  expect(screen.queryByText("Obsoleto")).not.toBeInTheDocument();
});
it.each(["/proyectos?cursor=older", `/proyectos/${summary.id}`])(
  "@s27 retira datos anteriores ante401 en %s",
  async (route) => {
    window.history.replaceState(null, "", "/proyectos");
    vi.spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(
        Response.json({ items: [summary], nextCursor: null }),
      )
      .mockResolvedValueOnce(
        Response.json({ code: "UNAUTHENTICATED" }, { status: 401 }),
      );
    render(<App />);
    await screen.findByRole("link", { name: summary.name });
    act(() => {
      window.history.pushState(null, "", route);
      window.dispatchEvent(new PopStateEvent("popstate"));
    });
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Autenticación requerida",
    );
    expect(screen.queryByText(summary.name)).not.toBeInTheDocument();
    expect(
      screen.queryByText("Todavía no tienes proyectos"),
    ).not.toBeInTheDocument();
  },
);
it("@s21 muestra detalle no encontrado y regreso sin proyecto ficticio", async () => {
  window.history.replaceState(null, "", `/proyectos/${summary.id}`);
  vi.spyOn(globalThis, "fetch").mockResolvedValue(
    Response.json(
      { code: "PROJECT_NOT_FOUND", title: "Proyecto no encontrado" },
      { status: 404 },
    ),
  );
  render(<App />);
  expect(
    await screen.findByRole("heading", { name: "Proyecto no encontrado" }),
  ).toBeVisible();
  expect(
    screen.getByRole("link", { name: "Volver a proyectos" }),
  ).toHaveAttribute("href", "/proyectos");
  expect(
    screen.queryByRole("button", { name: "Reintentar" }),
  ).not.toBeInTheDocument();
});
it.each(["network", 503, 500])(
  "@s31 presenta detalle fallido %s y reintenta su misma URL",
  async (failure) => {
    window.history.replaceState(null, "", `/proyectos/${summary.id}`);
    const request = vi.spyOn(globalThis, "fetch");
    if (failure === "network")
      request.mockRejectedValueOnce(new Error("private"));
    else
      request.mockResolvedValueOnce(
        Response.json({ title: "private" }, { status: Number(failure) }),
      );
    request.mockResolvedValueOnce(
      Response.json({
        ...summary,
        ownerId: "owner",
        description: "Descripción recuperada",
      }),
    );
    render(<App />);
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "No hemos podido cargar el proyecto.",
    );
    expect(
      screen.getByRole("link", { name: "Volver a proyectos" }),
    ).toBeVisible();
    await userEvent.click(screen.getByRole("button", { name: "Reintentar" }));
    expect(await screen.findByText("Descripción recuperada")).toBeVisible();
    expect(request).toHaveBeenLastCalledWith(
      `/api/v1/projects/${summary.id}`,
      expect.any(Object),
    );
  },
);
it("@s32 anuncia espera de detalle inmediato y ofrece regreso mientras carga", () => {
  window.history.replaceState(null, "", `/proyectos/${summary.id}`);
  vi.spyOn(globalThis, "fetch").mockImplementation(() => new Promise(() => {}));
  render(<App />);
  expect(screen.getByRole("status")).toHaveTextContent(/^Cargando proyecto$/);
  expect(
    screen.getByRole("link", { name: "Volver a proyectos" }),
  ).toBeVisible();
});
it("@s20 @s30 abre detalle directo, conserva texto y fecha semántica con zona", async () => {
  window.history.replaceState(null, "", `/proyectos/${summary.id}`);
  const detail = {
    ...summary,
    ownerId: "private-owner",
    name: "<b>Idea 🌿</b>",
    description: "<script>private()</script>\nSegundo paso",
  };
  const request = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValue(Response.json(detail));
  const { container } = render(<App />);
  expect(
    await screen.findByRole("heading", { level: 1, name: detail.name }),
  ).toBeVisible();
  expect(screen.getByText(/<script>private\(\)<\/script>/)).toHaveTextContent(
    "Segundo paso",
  );
  expect(container.querySelector("script, b")).toBeNull();
  expect(
    container.querySelectorAll(`time[datetime="${detail.createdAt}"]`),
  ).toHaveLength(2);
  expect(screen.getAllByText(/UTC/).length).toBeGreaterThan(0);
  expect(
    screen.getByRole("link", { name: "Volver a proyectos" }),
  ).toHaveAttribute("href", "/proyectos");
  expect(screen.queryByText("private-owner")).not.toBeInTheDocument();
  expect(request).toHaveBeenCalledWith(
    `/api/v1/projects/${summary.id}`,
    expect.any(Object),
  );
});
it("@s19 conserva regreso al inicio también en una página antigua vacía", async () => {
  window.history.replaceState(null, "", "/proyectos?cursor=last");
  vi.spyOn(globalThis, "fetch").mockResolvedValue(
    Response.json({ items: [], nextCursor: null }),
  );
  render(<App />);
  expect(
    await screen.findByRole("link", { name: "Volver al inicio" }),
  ).toHaveAttribute("href", "/proyectos");
  expect(
    screen.queryByRole("link", { name: "Más antiguos" }),
  ).not.toBeInTheDocument();
  expect(
    screen.queryByText("Todavía no tienes proyectos"),
  ).not.toBeInTheDocument();
});
it("@s18 pagina mediante URL opaca, enfoca el título y permite regresar al inicio", async () => {
  window.history.replaceState(null, "", "/proyectos");
  const request = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(
      Response.json({ items: [summary], nextCursor: "opaque_cursor-2" }),
    )
    .mockResolvedValueOnce(
      Response.json({
        items: [{ ...summary, name: "Anterior" }],
        nextCursor: null,
      }),
    )
    .mockResolvedValueOnce(
      Response.json({ items: [summary], nextCursor: null }),
    );
  render(<App />);
  await userEvent.click(
    await screen.findByRole("link", { name: "Más antiguos" }),
  );
  expect(window.location.pathname + window.location.search).toBe(
    "/proyectos?cursor=opaque_cursor-2",
  );
  expect(await screen.findByRole("link", { name: "Anterior" })).toBeVisible();
  expect(
    screen.getByRole("heading", { level: 1, name: "Proyectos" }),
  ).toHaveFocus();
  expect(request).toHaveBeenLastCalledWith(
    "/api/v1/projects?cursor=opaque_cursor-2",
    expect.any(Object),
  );
  expect(
    screen.queryByRole("link", { name: "Más antiguos" }),
  ).not.toBeInTheDocument();
  await userEvent.click(screen.getByRole("link", { name: "Volver al inicio" }));
  expect(await screen.findByRole("link", { name: summary.name })).toBeVisible();
  expect(window.location.search).toBe("");
});
it.each(["network", 503, 500])(
  "@s17 recupera una lista fallida %s sin falso vacío ni detalles internos",
  async (failure) => {
    window.history.replaceState(null, "", "/proyectos");
    const request = vi.spyOn(globalThis, "fetch");
    if (failure === "network")
      request.mockRejectedValueOnce(new Error("private-network"));
    else
      request.mockResolvedValueOnce(
        Response.json({ title: "private-sql" }, { status: Number(failure) }),
      );
    request.mockResolvedValueOnce(
      Response.json({ items: [summary], nextCursor: null }),
    );
    render(<App />);
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "No hemos podido cargar los proyectos.",
    );
    expect(screen.queryByText(/private/)).not.toBeInTheDocument();
    expect(
      screen.queryByText("Todavía no tienes proyectos"),
    ).not.toBeInTheDocument();
    await userEvent.click(screen.getByRole("button", { name: "Reintentar" }));
    expect(
      await screen.findByRole("link", { name: summary.name }),
    ).toBeVisible();
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
    expect(request).toHaveBeenCalledTimes(2);
  },
);
afterEach(() => window.history.replaceState(null, "", "/"));
it("navegación cliente cancela el salto nativo para no provocar una segunda carga", () => {
  render(<RouteLink href="/proyectos">Abrir proyectos</RouteLink>);
  expect(fireEvent.click(screen.getByRole("link"))).toBe(false);
  expect(window.location.pathname).toBe("/proyectos");
});
it("la navegación libera su suscripción al desmontar la aplicación", () => {
  const subscriptions = new Set<EventListenerOrEventListenerObject>();
  const add = window.addEventListener.bind(window);
  const remove = window.removeEventListener.bind(window);
  vi.spyOn(window, "addEventListener").mockImplementation(
    (type, listener, options) => {
      if (type === "popstate") subscriptions.add(listener);
      add(type, listener, options);
    },
  );
  vi.spyOn(window, "removeEventListener").mockImplementation(
    (type, listener, options) => {
      if (type === "popstate") subscriptions.delete(listener);
      remove(type, listener, options);
    },
  );
  const { unmount } = render(<App />);
  expect(subscriptions.size).toBe(1);
  unmount();
  expect(subscriptions.size).toBe(0);
});
it.each([
  { ctrlKey: true },
  { metaKey: true },
  { shiftKey: true },
  { altKey: true },
  { button: 1 },
])("@s24 conserva navegación nativa modificada %j", (options) => {
  render(<RouteLink href="/proyectos">Abrir proyectos</RouteLink>);
  expect(fireEvent.click(screen.getByRole("link"), options)).toBe(true);
  expect(window.location.pathname).toBe("/");
});
it("@s22 un rechazo tardío de un efecto cancelado no oculta la respuesta vigente", async () => {
  window.history.replaceState(null, "", "/proyectos");
  let reject!: (reason: unknown) => void;
  vi.spyOn(globalThis, "fetch")
    .mockImplementationOnce(
      () =>
        new Promise((_resolve, fail) => {
          reject = fail;
        }),
    )
    .mockResolvedValueOnce(
      Response.json({ items: [summary], nextCursor: null }),
    );
  render(
    <StrictMode>
      <App />
    </StrictMode>,
  );
  await screen.findByRole("link", { name: summary.name });
  await act(async () => reject(new DOMException("Aborted", "AbortError")));
  expect(screen.getByRole("link", { name: summary.name })).toBeVisible();
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
});
it("@s29 la lectura no copia datos a localStorage ni sessionStorage", async () => {
  window.history.replaceState(null, "", "/proyectos");
  const persist = vi.spyOn(Storage.prototype, "setItem");
  vi.spyOn(globalThis, "fetch").mockResolvedValue(
    Response.json({ items: [summary], nextCursor: null }),
  );
  render(<App />);
  await screen.findByRole("link", { name: summary.name });
  expect(persist).not.toHaveBeenCalled();
});
it("@s14 obtiene proyectos persistentes al abrir su URL y presenta semántica de lista", async () => {
  window.history.replaceState(null, "", "/proyectos");
  const request = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValue(Response.json({ items: [summary], nextCursor: null }));
  render(<App />);
  expect(
    await screen.findByRole("link", { name: "Zenit Digital" }),
  ).toHaveAttribute("href", `/proyectos/${summary.id}`);
  expect(
    screen.getByRole("list", { name: "Proyectos guardados" }),
  ).toBeVisible();
  expect(screen.getByText("Idea")).toBeVisible();
  expect(request).toHaveBeenCalledWith(
    "/api/v1/projects",
    expect.objectContaining({ credentials: "same-origin", cache: "no-store" }),
  );
});
it("@s16 anuncia espera inmediata sin presentar un vacío ficticio", () => {
  window.history.replaceState(null, "", "/proyectos");
  vi.spyOn(globalThis, "fetch").mockImplementation(() => new Promise(() => {}));
  render(<App />);
  expect(screen.getByRole("status")).toHaveTextContent("Cargando proyectos");
  expect(screen.queryByRole("list")).not.toBeInTheDocument();
  expect(
    screen.queryByText("Todavía no tienes proyectos"),
  ).not.toBeInTheDocument();
});
