import {
  render,
  screen,
  waitFor,
  act,
  fireEvent,
  within,
} from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { Appearance } from "./appearance";
import { AppearanceProvider, useAppearance } from "./appearance-state";
import userEvent from "@testing-library/user-event";
import { StrictMode } from "react";
import { App } from "./App";
import { SessionGate } from "./session-gate";
import { observeAccess } from "./api-client";

afterEach(() => {
  observeAccess();
  vi.unstubAllGlobals();
  window.history.replaceState(null, "", "/");
});
const stored = {
  configured: true,
  theme: "DARK",
  accentLight: "#0000FF",
  accentDark: "#00FFFF",
  updatedAt: "2026-09-07T18:00:00Z",
};
const tag = '"appearance:12345678-1234-1234-1234-123456789abc:0"';
it("@s21 provides separate native link and button samples without invoking application actions", async () => {
  const fetcher = vi
    .fn()
    .mockResolvedValue(Response.json(stored, { headers: { ETag: tag } }));
  vi.stubGlobal("fetch", fetcher);
  render(
    <AppearanceProvider>
      <Appearance />
    </AppearanceProvider>,
  );
  await screen.findByRole("radio", { name: "Oscuro" });
  for (const [name, theme] of [
    ["Vista previa clara", "light"],
    ["Vista previa oscura", "dark"],
  ]) {
    const sample = screen.getByRole("region", { name });
    expect(sample).toHaveAttribute("data-theme", theme);
    const link = within(sample).getByRole("link", {
      name: "Enlace de ejemplo",
    });
    await userEvent.click(link);
    await userEvent.click(
      within(sample).getByRole("button", { name: "Botón de ejemplo" }),
    );
  }
  expect(fetcher).toHaveBeenCalledTimes(1);
  expect(window.location.hash).toBe("");
});
it("@s28 keeps the write blocked across navigation until deliberate valid recovery", async () => {
  window.history.replaceState(null, "", "/apariencia");
  let reads = 0;
  let writes = 0;
  const fetcher = vi.fn(async (url: string, init?: RequestInit) => {
    if (url === "/api/v1/history")
      return Response.json({ items: [], nextCursor: null });
    if (url !== "/api/v1/me/appearance")
      throw new Error(`Unexpected URL ${url}`);
    if (init?.method === "PUT") {
      writes++;
      return writes === 1
        ? new Response(null, { status: 503 })
        : Response.json(
            { ...stored, theme: "LIGHT" },
            { headers: { ETag: tag.replace(":0", ":2") } },
          );
    }
    reads++;
    return Response.json(reads === 1 ? stored : { ...stored, theme: "LIGHT" }, {
      headers: { ETag: reads === 1 ? tag : tag.replace(":0", ":1") },
    });
  });
  vi.stubGlobal("fetch", fetcher);
  render(
    <AppearanceProvider>
      <App />
    </AppearanceProvider>,
  );
  await screen.findByRole("radio", { name: "Oscuro" });
  await userEvent.click(screen.getByRole("radio", { name: "Claro" }));
  await userEvent.click(
    screen.getByRole("button", { name: "Guardar apariencia" }),
  );
  await screen.findByText(/No podemos confirmar el guardado/);
  await userEvent.click(screen.getByRole("link", { name: "Historial" }));
  await screen.findByRole("heading", { name: "Historial", level: 1 });
  window.history.back();
  await screen.findByRole("heading", { name: "Apariencia", level: 1 });
  expect(
    screen.getByRole("button", { name: "Guardar apariencia" }),
  ).toHaveAttribute("aria-disabled", "true");
  await userEvent.click(
    screen.getByRole("button", { name: "Guardar apariencia" }),
  );
  expect(writes).toBe(1);
  expect(reads).toBe(1);
  await userEvent.click(
    screen.getByRole("button", { name: "Recargar versión guardada" }),
  );
  expect(await screen.findByRole("radio", { name: "Claro" })).toBeChecked();
  expect(reads).toBe(2);
  expect(writes).toBe(1);
  await userEvent.click(
    screen.getByRole("button", { name: "Guardar apariencia" }),
  );
  expect(await screen.findByText("Apariencia guardada.")).toBeVisible();
  expect(writes).toBe(2);
  const finalWrite = fetcher.mock.calls
    .filter(([, init]) => init?.method === "PUT")
    .at(-1)!;
  expect(new Headers(finalWrite[1]?.headers).get("If-Match")).toBe(
    tag.replace(":0", ":1"),
  );
});
it.each([
  "Cancelar cambios",
  "Restaurar valores predeterminados",
  "Guardar apariencia",
  "Recargar versión guardada",
])(
  "@s37 %s clears obsolete server errors together with the replaced draft",
  async (action) => {
    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockResolvedValueOnce(
          Response.json(stored, { headers: { ETag: tag } }),
        )
        .mockResolvedValueOnce(
          Response.json(
            {
              type: "urn:organization:problem:validation_error",
              title: "Revisa los campos",
              status: 400,
              code: "VALIDATION_ERROR",
              errors: ["theme", "accentLight", "accentDark"].map((field) => ({
                field,
                code: "INVALID_VALUE",
                message: `Revisa ${field}`,
              })),
            },
            { status: 400 },
          ),
        )
        .mockResolvedValueOnce(
          action === "Recargar versión guardada"
            ? new Response(null, { status: 503 })
            : Response.json(stored, { headers: { ETag: tag } }),
        )
        .mockResolvedValueOnce(
          Response.json(stored, { headers: { ETag: tag } }),
        ),
    );
    render(
      <AppearanceProvider>
        <Appearance />
      </AppearanceProvider>,
    );
    await screen.findByRole("radio", { name: "Oscuro" });
    await userEvent.click(
      screen.getByRole("button", { name: "Guardar apariencia" }),
    );
    expect(await screen.findAllByRole("alert")).toHaveLength(3);
    if (action === "Recargar versión guardada") {
      await userEvent.click(
        screen.getByRole("button", { name: "Guardar apariencia" }),
      );
      await screen.findByRole("button", { name: action });
    }
    await userEvent.click(screen.getByRole("button", { name: action }));
    if (action === "Guardar apariencia")
      expect(await screen.findByText("Apariencia guardada.")).toBeVisible();
    if (action === "Recargar versión guardada")
      await waitFor(() =>
        expect(
          screen.queryByRole("button", { name: action }),
        ).not.toBeInTheDocument(),
      );
    expect(screen.queryAllByRole("alert")).toHaveLength(0);
    expect(screen.getByRole("group", { name: "Tema" })).toHaveAttribute(
      "aria-invalid",
      "false",
    );
    expect(
      screen.getByRole("textbox", { name: "Color de acento claro" }),
    ).toHaveAttribute("aria-invalid", "false");
    expect(
      screen.getByRole("textbox", { name: "Color de acento oscuro" }),
    ).toHaveAttribute("aria-invalid", "false");
  },
);
it("@s30 invalidates a read started during a write when that write confirms", async () => {
  let confirm!: (response: Response) => void;
  let oldRead!: (response: Response) => void;
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(
      Response.json({ ...stored, theme: "LIGHT" }, { headers: { ETag: tag } }),
    )
    .mockReturnValueOnce(
      new Promise<Response>((resolve) => {
        confirm = resolve;
      }),
    )
    .mockReturnValueOnce(
      new Promise<Response>((resolve) => {
        oldRead = resolve;
      }),
    );
  function SharedConsumer() {
    const { snapshot, reload, save } = useAppearance();
    return (
      <>
        <output>{snapshot?.theme}</output>
        <button
          onClick={() => {
            void reload().catch(() => {});
          }}
        >
          Consultar
        </button>
        <button
          onClick={() => {
            void save({
              theme: "DARK",
              accentLight: stored.accentLight,
              accentDark: stored.accentDark,
            });
          }}
        >
          Guardar
        </button>
      </>
    );
  }
  vi.stubGlobal("fetch", fetcher);
  render(
    <AppearanceProvider>
      <SharedConsumer />
    </AppearanceProvider>,
  );
  await screen.findByText("LIGHT");
  await userEvent.click(screen.getByRole("button", { name: "Guardar" }));
  await userEvent.click(screen.getByRole("button", { name: "Consultar" }));
  await act(async () => {
    confirm(
      Response.json(stored, { headers: { ETag: tag.replace(":0", ":1") } }),
    );
  });
  await act(async () => {
    oldRead(
      Response.json({ ...stored, theme: "LIGHT" }, { headers: { ETag: tag } }),
    );
  });
  expect(screen.getByRole("status")).toHaveTextContent("DARK");
  expect(fetcher.mock.calls[2][1].signal.aborted).toBe(true);
});
it("@s26 announces an initial appearance failure while other functions remain usable", async () => {
  window.history.replaceState(null, "", "/proyectos/nuevo");
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(new Response(null, { status: 503 }))
    .mockResolvedValueOnce(Response.json(stored, { headers: { ETag: tag } }));
  vi.stubGlobal("fetch", fetcher);
  render(
    <AppearanceProvider>
      <App />
    </AppearanceProvider>,
  );
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "Usamos temporalmente valores seguros",
  );
  expect(
    screen.getByRole("textbox", { name: /Nombre del proyecto/ }),
  ).toBeEnabled();
  await userEvent.click(screen.getByRole("button", { name: "Reintentar" }));
  await waitFor(() =>
    expect(document.documentElement.style.colorScheme).toBe("dark"),
  );
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  expect(fetcher).toHaveBeenCalledTimes(2);
  expect(screen.getByRole("heading", { level: 1 })).toHaveFocus();
});
it("@s25 warns about leaving a draft and restores only confirmed values on Back", async () => {
  window.history.replaceState(null, "", "/apariencia");
  const fetcher = vi.fn(async (url: string) => {
    if (url === "/api/v1/me/appearance")
      return Response.json(stored, { headers: { ETag: tag } });
    if (url === "/api/v1/history")
      return Response.json({ items: [], nextCursor: null });
    throw new Error(`Unexpected URL ${url}`);
  });
  vi.stubGlobal("fetch", fetcher);
  render(
    <AppearanceProvider>
      <App />
    </AppearanceProvider>,
  );
  await screen.findByRole("radio", { name: "Oscuro" });
  await userEvent.click(screen.getByRole("radio", { name: "Claro" }));
  expect(
    screen.getByText(
      "Al salir de esta pantalla se descartan los cambios sin guardar.",
    ),
  ).toBeVisible();
  await userEvent.click(screen.getByRole("link", { name: "Historial" }));
  await screen.findByRole("heading", { name: "Historial", level: 1 });
  expect(document.documentElement.style.colorScheme).toBe("dark");
  window.history.back();
  expect(await screen.findByRole("radio", { name: "Oscuro" })).toBeChecked();
  expect(
    screen.queryByText("Tienes cambios sin guardar."),
  ).not.toBeInTheDocument();
  expect(
    fetcher.mock.calls.filter(([url]) => url === "/api/v1/me/appearance"),
  ).toHaveLength(1);
});
it.each([200, 401])(
  "@s30 does not let a preceding shared read replace a confirmed save: %s",
  async (oldStatus) => {
    let deliver!: (response: Response) => void;
    let confirm!: (response: Response) => void;
    const observer = vi.fn();
    observeAccess(observer);
    const fetcher = vi
      .fn()
      .mockResolvedValueOnce(
        Response.json(
          { ...stored, theme: "LIGHT" },
          { headers: { ETag: tag } },
        ),
      )
      .mockReturnValueOnce(
        new Promise<Response>((resolve) => {
          deliver = resolve;
        }),
      )
      .mockImplementationOnce(() =>
        oldStatus === 401
          ? new Promise<Response>((resolve) => {
              confirm = resolve;
            })
          : Promise.resolve(
              Response.json(stored, {
                headers: { ETag: tag.replace(":0", ":1") },
              }),
            ),
      );
    function SharedConsumer() {
      const { snapshot, reload, save } = useAppearance();
      return (
        <>
          <output>{snapshot?.theme}</output>
          <button
            onClick={() => {
              void reload().catch(() => {});
            }}
          >
            Consultar
          </button>
          <button
            onClick={() => {
              void save({
                theme: "DARK",
                accentLight: stored.accentLight,
                accentDark: stored.accentDark,
              });
            }}
          >
            Guardar
          </button>
        </>
      );
    }
    vi.stubGlobal("fetch", fetcher);
    render(
      <AppearanceProvider>
        <SharedConsumer />
      </AppearanceProvider>,
    );
    await screen.findByText("LIGHT");
    await userEvent.click(screen.getByRole("button", { name: "Consultar" }));
    await userEvent.click(screen.getByRole("button", { name: "Guardar" }));
    if (oldStatus === 401) {
      await act(async () => deliver(new Response(null, { status: 401 })));
      expect(observer).not.toHaveBeenCalled();
      expect(screen.getByRole("status")).toHaveTextContent("LIGHT");
      await act(async () =>
        confirm(
          Response.json(stored, { headers: { ETag: tag.replace(":0", ":1") } }),
        ),
      );
    }
    await screen.findByText("DARK");
    if (oldStatus === 200)
      await act(async () => {
        deliver(
          Response.json(
            { ...stored, theme: "LIGHT" },
            { headers: { ETag: tag } },
          ),
        );
      });
    expect(screen.getByRole("status")).toHaveTextContent("DARK");
    expect(document.documentElement.style.colorScheme).toBe("dark");
    expect(fetcher.mock.calls[1][1].signal.aborted).toBe(true);
    expect(observer).not.toHaveBeenCalled();
  },
);
it("@s27 associates a server theme error with its native group and clears it on correction", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValueOnce(Response.json(stored, { headers: { ETag: tag } }))
      .mockResolvedValueOnce(
        Response.json(
          {
            type: "urn:organization:problem:validation_error",
            title: "Revisa los campos",
            status: 400,
            code: "VALIDATION_ERROR",
            errors: [
              {
                field: "theme",
                code: "INVALID_VALUE",
                message: "Selecciona otro tema.",
              },
            ],
          },
          { status: 400 },
        ),
      ),
  );
  render(
    <AppearanceProvider>
      <Appearance />
    </AppearanceProvider>,
  );
  await screen.findByRole("radio", { name: "Oscuro" });
  await userEvent.click(
    screen.getByRole("button", { name: "Guardar apariencia" }),
  );
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "Selecciona otro tema.",
  );
  expect(screen.getByRole("group", { name: "Tema" })).toHaveAttribute(
    "aria-invalid",
    "true",
  );
  expect(
    screen.getByRole("group", { name: "Tema" }),
  ).toHaveAccessibleDescription("Selecciona otro tema.");
  await userEvent.click(screen.getByRole("radio", { name: "Claro" }));
  expect(screen.getByRole("group", { name: "Tema" })).toHaveAttribute(
    "aria-invalid",
    "false",
  );
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
});
it("@s33 does not reclaim focus after a deliberate move followed by blur during recovery", async () => {
  let deliver!: (response: Response) => void;
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(Response.json(stored, { headers: { ETag: tag } }))
    .mockResolvedValueOnce(new Response(null, { status: 503 }))
    .mockReturnValueOnce(
      new Promise<Response>((resolve) => {
        deliver = resolve;
      }),
    );
  vi.stubGlobal("fetch", fetcher);
  render(
    <AppearanceProvider>
      <Appearance />
    </AppearanceProvider>,
  );
  await screen.findByRole("radio", { name: "Oscuro" });
  await userEvent.click(
    screen.getByRole("button", { name: "Guardar apariencia" }),
  );
  await userEvent.click(
    await screen.findByRole("button", { name: "Recargar versión guardada" }),
  );
  const chosen = screen.getByRole("textbox", { name: "Color de acento claro" });
  chosen.focus();
  chosen.blur();
  expect(document.activeElement).toBe(document.body);
  await act(async () => {
    deliver(Response.json(stored, { headers: { ETag: tag } }));
  });
  expect(
    screen.queryByRole("button", { name: "Recargar versión guardada" }),
  ).not.toBeInTheDocument();
  expect(document.activeElement).toBe(document.body);
});
it.each([
  { method: "GET", status: 401, json: false },
  { method: "GET", status: 200, json: false },
  { method: "PUT", status: 401, json: false },
  { method: "PUT", status: 200, json: false },
  { method: "GET", status: 200, json: true },
  { method: "PUT", status: 200, json: true },
])(
  "@s31 ignores Ana's late $method HTTP $status (deferred JSON $json) after logout and Bruno's appearance has loaded",
  async ({ method, status, json }) => {
    window.history.replaceState(null, "", "/apariencia");
    let owner: string | null = "Ana";
    let deliver!: (response: Response) => void;
    const fetcher = vi.fn((url: string, init?: RequestInit) => {
      if (url === "/api/session/logout") {
        owner = null;
        return Promise.resolve(new Response(null, { status: 204 }));
      }
      if (url === "/api/session" && init?.method === "POST") {
        owner = "Bruno";
        return Promise.resolve(new Response(null, { status: 204 }));
      }
      if (url === "/api/session")
        return Promise.resolve(
          Response.json({
            authenticated: owner !== null,
            username: owner,
            csrfToken: "token",
            csrfHeaderName: "X-CSRF-TOKEN",
          }),
        );
      if (url === "/api/v1/me/appearance" && owner === "Ana") {
        if (method === "PUT" && init?.method !== "PUT")
          return Promise.resolve(
            Response.json(stored, { headers: { ETag: tag } }),
          );
        if (json) {
          const response = Response.json(stored, { headers: { ETag: tag } });
          vi.spyOn(response, "json").mockImplementation(
            () =>
              new Promise((resolve) => {
                deliver = (incoming) => {
                  void incoming.json().then(resolve);
                };
              }),
          );
          return Promise.resolve(response);
        }
        return new Promise<Response>((resolve) => {
          deliver = resolve;
        });
      }
      if (url === "/api/v1/me/appearance")
        return Promise.resolve(
          Response.json(
            { ...stored, theme: "LIGHT" },
            { headers: { ETag: tag } },
          ),
        );
      throw new Error(`Unexpected URL ${url}`);
    });
    vi.stubGlobal("fetch", fetcher);
    render(<SessionGate />);
    if (method === "PUT") {
      await screen.findByRole("radio", { name: "Oscuro" });
      await userEvent.click(
        screen.getByRole("button", { name: "Guardar apariencia" }),
      );
    } else await screen.findByText("Consultando apariencia…");
    await waitFor(() => expect(deliver).toBeTypeOf("function"));
    await userEvent.click(
      screen.getByRole("button", { name: "Cerrar sesión" }),
    );
    await userEvent.type(await screen.findByLabelText("Usuario"), "Bruno");
    await userEvent.type(screen.getByLabelText("Contraseña"), "secret");
    await userEvent.click(
      screen.getByRole("button", { name: "Iniciar sesión" }),
    );
    expect(await screen.findByRole("radio", { name: "Claro" })).toBeChecked();
    const sessionReads = fetcher.mock.calls.filter(
      ([url, init]) => url === "/api/session" && !init?.method,
    ).length;
    await act(async () => {
      deliver(
        status === 401
          ? new Response(null, { status: 401 })
          : Response.json(stored, { headers: { ETag: tag } }),
      );
    });
    expect(screen.getByRole("radio", { name: "Claro" })).toBeChecked();
    expect(document.documentElement.style.colorScheme).toBe("light");
    expect(
      fetcher.mock.calls.filter(
        ([url, init]) => url === "/api/session" && !init?.method,
      ),
    ).toHaveLength(sessionReads);
    expect(screen.queryByLabelText("Usuario")).not.toBeInTheDocument();
  },
);
it.each([
  {
    key: "accentLight",
    label: "Color de acento claro",
    replacement: "#244C3C",
    message: "Elige otro color claro.",
  },
  {
    key: "accentDark",
    label: "Color de acento oscuro",
    replacement: "#B7E4C7",
    message: "Elige otro color oscuro.",
  },
])(
  "@s27 shows a coherent server field rejection and allows correction without recovery: $key",
  async ({ key, label, replacement, message }) => {
    const fetcher = vi
      .fn()
      .mockResolvedValueOnce(Response.json(stored, { headers: { ETag: tag } }))
      .mockResolvedValueOnce(
        Response.json(
          {
            type: "urn:organization:problem:validation_error",
            title: "Revisa los campos",
            status: 400,
            code: "VALIDATION_ERROR",
            errors: [
              {
                field: key,
                code: "INVALID_VALUE",
                message,
              },
            ],
          },
          { status: 400 },
        ),
      );
    vi.stubGlobal("fetch", fetcher);
    render(
      <AppearanceProvider>
        <Appearance />
      </AppearanceProvider>,
    );
    await screen.findByRole("radio", { name: "Oscuro" });
    await userEvent.click(
      screen.getByRole("button", { name: "Guardar apariencia" }),
    );
    expect(await screen.findByRole("alert")).toHaveTextContent(message);
    const field = screen.getByRole("textbox", { name: label });
    expect(field).toHaveAttribute("aria-invalid", "true");
    expect(field).toHaveAccessibleDescription(message);
    expect(
      screen.queryByRole("button", { name: "Recargar versión guardada" }),
    ).not.toBeInTheDocument();
    fireEvent.change(field, { target: { value: replacement } });
    expect(field).toHaveAttribute("aria-invalid", "false");
    expect(
      screen.getByRole("button", { name: "Guardar apariencia" }),
    ).toHaveAttribute("aria-disabled", "false");
    expect(fetcher).toHaveBeenCalledTimes(2);
  },
);
it("@s32 does not carry confirmed appearance or drafts into a different authenticated owner", async () => {
  window.history.replaceState(null, "", "/apariencia");
  let owner = "first";
  const fetcher = vi.fn((url: string) => {
    if (url === "/api/session")
      return Promise.resolve(
        Response.json({
          authenticated: true,
          username: owner,
          csrfToken: owner,
          csrfHeaderName: "X-CSRF-TOKEN",
        }),
      );
    if (url === "/api/v1/me/appearance" && owner === "first")
      return Promise.resolve(Response.json(stored, { headers: { ETag: tag } }));
    if (url === "/api/v1/me/appearance") return new Promise<Response>(() => {});
    throw new Error(`Unexpected URL ${url}`);
  });
  vi.stubGlobal("fetch", fetcher);
  render(<SessionGate />);
  await screen.findByRole("radio", { name: "Oscuro" });
  fireEvent.change(
    screen.getByRole("textbox", { name: "Color de acento claro" }),
    { target: { value: "#244C3C" } },
  );
  owner = "second";
  await act(async () => {
    document.dispatchEvent(new Event("visibilitychange"));
  });
  await waitFor(() =>
    expect(
      fetcher.mock.calls.filter(([url]) => url === "/api/v1/me/appearance"),
    ).toHaveLength(2),
  );
  expect(
    screen.queryByRole("textbox", { name: "Color de acento claro" }),
  ).not.toBeInTheDocument();
  expect(document.documentElement.style.getPropertyValue("--accent")).toBe("");
  expect(screen.getByRole("status")).toHaveTextContent(
    "Consultando apariencia",
  );
});
it("@s20 returns to appearance after login without reading private preferences beforehand", async () => {
  window.history.replaceState(null, "", "/apariencia");
  let authenticated = false;
  const unexpected: string[] = [];
  const fetcher = vi.fn(async (url: string, init?: RequestInit) => {
    if (url === "/api/session" && init?.method === "POST") {
      authenticated = true;
      return new Response(null, { status: 204 });
    }
    if (url === "/api/session")
      return Response.json({
        authenticated,
        username: authenticated ? "owner" : null,
        csrfToken: "token",
        csrfHeaderName: "X-CSRF-TOKEN",
      });
    if (url === "/api/v1/me/appearance")
      return Response.json(stored, { headers: { ETag: tag } });
    unexpected.push(url);
    return new Response(null, { status: 404 });
  });
  vi.stubGlobal("fetch", fetcher);
  render(<SessionGate />);
  await screen.findByRole("button", { name: "Iniciar sesión" });
  expect(
    fetcher.mock.calls.filter(([url]) => url === "/api/v1/me/appearance"),
  ).toHaveLength(0);
  await userEvent.type(screen.getByLabelText("Usuario"), "owner");
  await userEvent.type(screen.getByLabelText("Contraseña"), "secret");
  await userEvent.click(screen.getByRole("button", { name: "Iniciar sesión" }));
  expect(
    await screen.findByRole("heading", { name: "Apariencia", level: 1 }),
  ).toBeVisible();
  expect(window.location.pathname).toBe("/apariencia");
  expect(unexpected).toEqual([]);
});
it("@s20 loads appearance once inside the authenticated session", async () => {
  window.history.replaceState(null, "", "/apariencia");
  const unexpected: string[] = [];
  const fetcher = vi.fn(async (url: string) => {
    if (url === "/api/session")
      return Response.json({
        authenticated: true,
        username: "owner",
        csrfToken: "token",
        csrfHeaderName: "X-CSRF-TOKEN",
      });
    if (url === "/api/v1/me/appearance")
      return Response.json(stored, { headers: { ETag: tag } });
    unexpected.push(url);
    return new Response(null, { status: 404 });
  });
  vi.stubGlobal("fetch", fetcher);
  render(<SessionGate />);
  expect(await screen.findByRole("radio", { name: "Oscuro" })).toBeChecked();
  await waitFor(() =>
    expect(document.documentElement.style.colorScheme).toBe("dark"),
  );
  expect(
    fetcher.mock.calls.filter(([url]) => url === "/api/v1/me/appearance"),
  ).toHaveLength(1);
  expect(unexpected).toEqual([]);
});
it("@s20 opens appearance through the principal navigation at its stable route", async () => {
  window.history.replaceState(null, "", "/proyectos/nuevo");
  const fetcher = vi
    .fn()
    .mockResolvedValue(Response.json(stored, { headers: { ETag: tag } }));
  vi.stubGlobal("fetch", fetcher);
  render(
    <AppearanceProvider>
      <App />
    </AppearanceProvider>,
  );
  const navigation = within(
    screen.getByRole("navigation", { name: "Principal" }),
  );
  expect(navigation.getAllByRole("link")[0]).toHaveAccessibleName("Hoy");
  // Later features append entries, so assert the tail order by name and not by
  // index: these keep their relative order however many entries exist.
  const names = navigation
    .getAllByRole("link")
    .map((link) => link.textContent?.replace(/[^\p{L}\s]/gu, "").trim() ?? "");
  const order = [
    "Apariencia",
    "Exportación",
    "Calendario",
    "Importación",
    "API para integraciones",
  ].map((name) => names.indexOf(name));
  expect(order.every((index) => index > 0)).toBe(true);
  expect(order).toEqual([...order].sort((left, right) => left - right));
  await userEvent.click(screen.getByRole("link", { name: "Apariencia" }));
  expect(window.location.pathname).toBe("/apariencia");
  expect(await screen.findByRole("radio", { name: "Oscuro" })).toBeChecked();
  expect(
    screen.getByRole("heading", { name: "Apariencia", level: 1 }),
  ).toHaveFocus();
  expect(screen.getByRole("link", { name: "Apariencia" })).toHaveAttribute(
    "aria-current",
    "page",
  );
  expect(fetcher).toHaveBeenCalledTimes(1);
});
it("@s21 synchronizes native color selectors with hexadecimal drafts", async () => {
  const fetcher = vi
    .fn()
    .mockResolvedValue(Response.json(stored, { headers: { ETag: tag } }));
  vi.stubGlobal("fetch", fetcher);
  render(
    <AppearanceProvider>
      <Appearance />
    </AppearanceProvider>,
  );
  await screen.findByRole("radio", { name: "Oscuro" });
  const light = screen.getByLabelText("Seleccionar acento claro");
  const dark = screen.getByLabelText("Seleccionar acento oscuro");
  expect(light).toHaveAttribute("type", "color");
  expect(dark).toHaveAttribute("type", "color");
  fireEvent.change(light, { target: { value: "#244c3c" } });
  expect(
    screen.getByRole("textbox", { name: "Color de acento claro" }),
  ).toHaveValue("#244c3c");
  fireEvent.change(
    screen.getByRole("textbox", { name: "Color de acento oscuro" }),
    { target: { value: "#B7E4C7" } },
  );
  expect(dark).toHaveValue("#b7e4c7");
  fireEvent.change(dark, { target: { value: "#ffffff" } });
  expect(
    screen.getByRole("region", { name: "Vista previa oscura" }),
  ).toHaveStyle({
    "--accent": "#FFFFFF",
  });
  fireEvent.change(dark, { target: { value: "#000000" } });
  expect(dark).toHaveValue("#000000");
  const darkField = screen.getByRole("textbox", {
    name: "Color de acento oscuro",
  });
  expect(darkField).toHaveValue("#000000");
  expect(darkField).toHaveAttribute("aria-invalid", "true");
  expect(darkField).toHaveAccessibleDescription(
    "Elige un color con más contraste. La muestra conserva el último color válido.",
  );
  expect(
    screen.getByRole("region", { name: "Vista previa oscura" }),
  ).toHaveStyle({
    "--accent": "#FFFFFF",
  });
  fireEvent.change(light, { target: { value: "#ffffff" } });
  expect(light).toHaveValue("#ffffff");
  expect(
    screen.getByRole("textbox", { name: "Color de acento claro" }),
  ).toHaveValue("#ffffff");
  expect(
    screen.getByRole("region", { name: "Vista previa clara" }),
  ).toHaveStyle({
    "--accent": "#244C3C",
  });
  expect(fetcher).toHaveBeenCalledTimes(1);
});
it("@s37 cancels a local draft back to confirmed values without an HTTP request", async () => {
  const fetcher = vi
    .fn()
    .mockResolvedValue(Response.json(stored, { headers: { ETag: tag } }));
  vi.stubGlobal("fetch", fetcher);
  render(
    <AppearanceProvider>
      <Appearance />
    </AppearanceProvider>,
  );
  await screen.findByRole("radio", { name: "Oscuro" });
  await userEvent.click(screen.getByRole("radio", { name: "Claro" }));
  fireEvent.change(
    screen.getByRole("textbox", { name: "Color de acento claro" }),
    { target: { value: "#FFFFFF" } },
  );
  await userEvent.click(
    screen.getByRole("button", { name: "Cancelar cambios" }),
  );
  expect(screen.getByRole("radio", { name: "Oscuro" })).toBeChecked();
  expect(
    screen.getByRole("textbox", { name: "Color de acento claro" }),
  ).toHaveValue("#0000FF");
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  expect(
    screen.queryByText("Tienes cambios sin guardar."),
  ).not.toBeInTheDocument();
  expect(
    screen
      .getByRole("region", { name: "Vista previa clara" })
      .style.getPropertyValue("--accent"),
  ).toBe("#0000FF");
  expect(
    screen.getByRole("button", { name: "Cancelar cambios" }),
  ).toHaveFocus();
  expect(fetcher).toHaveBeenCalledTimes(1);
});
it("@s22 prepares defaults locally and requires an explicit save", async () => {
  const fetcher = vi
    .fn()
    .mockResolvedValue(Response.json(stored, { headers: { ETag: tag } }));
  vi.stubGlobal("fetch", fetcher);
  render(
    <AppearanceProvider>
      <Appearance />
    </AppearanceProvider>,
  );
  await screen.findByRole("radio", { name: "Oscuro" });
  await userEvent.click(
    screen.getByRole("button", { name: "Restaurar valores predeterminados" }),
  );
  expect(screen.getByRole("radio", { name: "Sistema" })).toBeChecked();
  expect(
    screen.getByRole("textbox", { name: "Color de acento claro" }),
  ).toHaveValue("#244C3C");
  expect(
    screen.getByRole("textbox", { name: "Color de acento oscuro" }),
  ).toHaveValue("#B7E4C7");
  expect(
    screen
      .getByRole("region", { name: "Vista previa clara" })
      .style.getPropertyValue("--accent"),
  ).toBe("#244C3C");
  expect(screen.getByText("Tienes cambios sin guardar.")).toBeVisible();
  expect(document.documentElement.style.colorScheme).toBe("dark");
  expect(document.documentElement.style.getPropertyValue("--accent")).toBe(
    "#00FFFF",
  );
  expect(fetcher).toHaveBeenCalledTimes(1);
});
it("@s20 loads the own appearance through the application's StrictMode lifecycle", async () => {
  const fetcher = vi
    .fn()
    .mockImplementation(() =>
      Promise.resolve(Response.json(stored, { headers: { ETag: tag } })),
    );
  vi.stubGlobal("fetch", fetcher);
  render(
    <StrictMode>
      <AppearanceProvider>
        <Appearance />
      </AppearanceProvider>
    </StrictMode>,
  );
  expect(await screen.findByRole("radio", { name: "Oscuro" })).toBeChecked();
  expect(screen.queryByText("Consultando apariencia…")).not.toBeInTheDocument();
  expect(fetcher.mock.calls[0][1].signal.aborted).toBe(true);
  await waitFor(() =>
    expect(document.documentElement.style.colorScheme).toBe("dark"),
  );
});
it.each([
  ["503", () => new Response(null, { status: 503 })],
  [
    "invalid200",
    () => Response.json({ ...stored, extra: true }, { headers: { ETag: tag } }),
  ],
] as const)(
  "@s29 retains uncertainty after failed recovery and announces the next pending consultation: %s",
  async (_name, response) => {
    const fetcher = vi
      .fn()
      .mockResolvedValueOnce(Response.json(stored, { headers: { ETag: tag } }))
      .mockResolvedValueOnce(new Response(null, { status: 503 }))
      .mockImplementationOnce(response)
      .mockReturnValue(new Promise(() => {}));
    vi.stubGlobal("fetch", fetcher);
    render(
      <AppearanceProvider>
        <Appearance />
      </AppearanceProvider>,
    );
    await screen.findByRole("radio", { name: "Oscuro" });
    await userEvent.click(screen.getByRole("radio", { name: "Claro" }));
    await userEvent.click(
      screen.getByRole("button", { name: "Guardar apariencia" }),
    );
    await userEvent.click(
      await screen.findByRole("button", { name: "Recargar versión guardada" }),
    );
    expect(
      await screen.findByText(
        "No se pudo consultar la apariencia. Se conserva la última versión confirmada.",
      ),
    ).toBeVisible();
    expect(
      screen.queryByRole("button", { name: "Reintentar" }),
    ).not.toBeInTheDocument();
    expect(screen.getByRole("radio", { name: "Claro" })).toBeChecked();
    expect(
      screen.getByRole("button", { name: "Guardar apariencia" }),
    ).toHaveAttribute("aria-disabled", "true");
    await userEvent.dblClick(
      screen.getByRole("button", { name: "Recargar versión guardada" }),
    );
    expect(screen.getByRole("status")).toHaveTextContent(
      "Consultando versión guardada",
    );
    expect(fetcher).toHaveBeenCalledTimes(4);
    expect(document.documentElement.style.colorScheme).toBe("dark");
  },
);
it.each([
  [
    "503",
    () =>
      Promise.resolve(
        Response.json(
          {
            type: "urn:organization:problem:storage_unavailable",
            title: "No disponible",
            status: 503,
            code: "STORAGE_UNAVAILABLE",
          },
          { status: 503 },
        ),
      ),
  ],
  [
    "412",
    () =>
      Promise.resolve(
        Response.json(
          {
            type: "urn:organization:problem:appearance_conflict",
            title: "Conflicto",
            status: 412,
            code: "APPEARANCE_CONFLICT",
          },
          { status: 412 },
        ),
      ),
  ],
  ["network", () => Promise.reject(new TypeError("Network unavailable"))],
  [
    "invalid200",
    () =>
      Promise.resolve(
        Response.json(
          { ...stored, theme: "LIGHT", extra: true },
          { headers: { ETag: tag } },
        ),
      ),
  ],
  [
    "contradictory",
    () =>
      Promise.resolve(
        Response.json(
          {
            type: "urn:organization:problem:storage_unavailable",
            title: "Rechazo",
            status: 400,
            code: "VALIDATION_ERROR",
            errors: [
              {
                field: "theme",
                code: "INVALID_VALUE",
                message: "Tema rechazado",
              },
            ],
          },
          { status: 400 },
        ),
      ),
  ],
] as const)(
  "@s28 @s29 recovers an uncertain PUT by reading saved values before another manual decision: %s",
  async (_name, outcome) => {
    const fetcher = vi
      .fn()
      .mockResolvedValueOnce(Response.json(stored, { headers: { ETag: tag } }))
      .mockImplementationOnce(outcome)
      .mockResolvedValueOnce(
        Response.json(
          { ...stored, theme: "SYSTEM" },
          { headers: { ETag: tag.replace(":0", ":1") } },
        ),
      );
    vi.stubGlobal("fetch", fetcher);
    render(
      <AppearanceProvider>
        <Appearance />
      </AppearanceProvider>,
    );
    await screen.findByRole("radio", { name: "Oscuro" });
    await userEvent.click(screen.getByRole("radio", { name: "Claro" }));
    await userEvent.click(
      screen.getByRole("button", { name: "Guardar apariencia" }),
    );
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "No podemos confirmar el guardado",
    );
    expect(document.documentElement.style.colorScheme).toBe("dark");
    expect(screen.getByRole("radio", { name: "Claro" })).toBeChecked();
    await userEvent.click(
      screen.getByRole("button", { name: "Guardar apariencia" }),
    );
    expect(fetcher).toHaveBeenCalledTimes(2);
    await userEvent.click(
      screen.getByRole("button", { name: "Recargar versión guardada" }),
    );
    expect(await screen.findByRole("radio", { name: "Sistema" })).toBeChecked();
    expect(fetcher).toHaveBeenCalledTimes(3);
    expect(fetcher.mock.calls[2][1].method).toBeUndefined();
    expect(screen.queryByText("Apariencia guardada.")).not.toBeInTheDocument();
    expect(
      screen.getByRole("heading", { name: "Apariencia", level: 1 }),
    ).toHaveFocus();
  },
);
it("@s23 saves one explicit draft and applies it only after confirmation", async () => {
  let deliver!: (response: Response) => void;
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(Response.json(stored, { headers: { ETag: tag } }))
    .mockReturnValueOnce(
      new Promise<Response>((resolve) => {
        deliver = resolve;
      }),
    );
  vi.stubGlobal("fetch", fetcher);
  render(
    <AppearanceProvider>
      <Appearance />
    </AppearanceProvider>,
  );
  await screen.findByRole("radio", { name: "Oscuro" });
  await userEvent.click(screen.getByRole("radio", { name: "Claro" }));
  await userEvent.dblClick(
    screen.getByRole("button", { name: "Guardar apariencia" }),
  );
  expect(fetcher).toHaveBeenCalledTimes(2);
  expect(JSON.parse(fetcher.mock.calls[1][1].body)).toEqual({
    theme: "LIGHT",
    accentLight: "#0000FF",
    accentDark: "#00FFFF",
  });
  expect(screen.getByRole("status")).toHaveTextContent("Guardando apariencia");
  expect(document.documentElement.style.colorScheme).toBe("dark");
  expect(screen.getByRole("radio", { name: "Oscuro" })).toBeDisabled();
  expect(screen.getByLabelText("Seleccionar acento claro")).toBeDisabled();
  expect(screen.getByLabelText("Seleccionar acento oscuro")).toBeDisabled();
  expect(
    screen.getByRole("textbox", { name: "Color de acento claro" }),
  ).toHaveAttribute("readonly");
  expect(
    screen.getByRole("textbox", { name: "Color de acento oscuro" }),
  ).toHaveAttribute("readonly");
  expect(
    screen.getByRole("button", { name: "Cancelar cambios" }),
  ).toBeDisabled();
  expect(
    screen.getByRole("button", { name: "Restaurar valores predeterminados" }),
  ).toBeDisabled();
  await act(async () =>
    deliver(
      Response.json(
        { ...stored, theme: "LIGHT" },
        { headers: { ETag: tag.replace(":0", ":1") } },
      ),
    ),
  );
  expect(document.documentElement.style.colorScheme).toBe("light");
  expect(screen.getByRole("status")).toHaveTextContent("Apariencia guardada");
  expect(
    screen.getByRole("button", { name: "Guardar apariencia" }),
  ).toHaveFocus();
  await userEvent.click(screen.getByRole("radio", { name: "Oscuro" }));
  expect(screen.queryByText("Apariencia guardada.")).not.toBeInTheDocument();
  expect(screen.getByText("Tienes cambios sin guardar.")).toBeVisible();
});
it("@s27 retains an invalid color with an accessible error and the last safe preview", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(Response.json(stored, { headers: { ETag: tag } })),
  );
  render(
    <AppearanceProvider>
      <Appearance />
    </AppearanceProvider>,
  );
  const field = await screen.findByRole("textbox", {
    name: "Color de acento claro",
  });
  fireEvent.change(field, { target: { value: "#FFFFFF" } });
  expect(field).toHaveValue("#FFFFFF");
  expect(field).toHaveAttribute("aria-invalid", "true");
  expect(field).toHaveAccessibleDescription(
    "Elige un color con más contraste. La muestra conserva el último color válido.",
  );
  expect(
    screen
      .getByRole("region", { name: "Vista previa clara" })
      .style.getPropertyValue("--accent"),
  ).toBe("#0000FF");
});
it("@s21 edits a local preview without applying or saving the draft globally", async () => {
  const fetcher = vi
    .fn()
    .mockResolvedValue(Response.json(stored, { headers: { ETag: tag } }));
  vi.stubGlobal("fetch", fetcher);
  render(
    <AppearanceProvider>
      <Appearance />
    </AppearanceProvider>,
  );
  await screen.findByRole("radio", { name: "Oscuro" });
  await userEvent.click(screen.getByRole("radio", { name: "Claro" }));
  fireEvent.change(
    screen.getByRole("textbox", { name: "Color de acento claro" }),
    { target: { value: "#244C3C" } },
  );
  expect(
    screen
      .getByRole("region", { name: "Vista previa clara" })
      .style.getPropertyValue("--accent"),
  ).toBe("#244C3C");
  expect(screen.getByText("Tienes cambios sin guardar.")).toBeVisible();
  expect(document.documentElement.style.colorScheme).toBe("dark");
  expect(document.documentElement.style.getPropertyValue("--accent")).toBe(
    "#00FFFF",
  );
  expect(fetcher).toHaveBeenCalledTimes(1);
});
it("@s24 refreshes the system scheme after suspension without polling or writing", async () => {
  const media = Object.assign(new EventTarget(), { matches: false });
  vi.stubGlobal("matchMedia", vi.fn().mockReturnValue(media));
  const fetcher = vi
    .fn()
    .mockResolvedValue(
      Response.json({ ...stored, theme: "SYSTEM" }, { headers: { ETag: tag } }),
    );
  vi.stubGlobal("fetch", fetcher);
  render(
    <AppearanceProvider>
      <Appearance />
    </AppearanceProvider>,
  );
  await screen.findByRole("radio", { name: "Sistema" });
  await waitFor(() =>
    expect(document.documentElement.style.colorScheme).toBe("light"),
  );
  await act(async () => {
    media.matches = true;
    document.dispatchEvent(new Event("visibilitychange"));
  });
  expect(document.documentElement.style.colorScheme).toBe("dark");
  expect(fetcher).toHaveBeenCalledTimes(1);
});
it("@s24 follows a system color-scheme change without persisting another preference", async () => {
  const media = Object.assign(new EventTarget(), { matches: true });
  vi.stubGlobal(
    "matchMedia",
    vi.fn((query: string) =>
      query === "(prefers-color-scheme: dark)"
        ? media
        : Object.assign(new EventTarget(), { matches: false }),
    ),
  );
  const fetcher = vi
    .fn()
    .mockResolvedValue(
      Response.json({ ...stored, theme: "SYSTEM" }, { headers: { ETag: tag } }),
    );
  vi.stubGlobal("fetch", fetcher);
  render(
    <AppearanceProvider>
      <Appearance />
    </AppearanceProvider>,
  );
  await screen.findByRole("radio", { name: "Sistema" });
  expect(document.documentElement.style.colorScheme).toBe("dark");
  await act(async () => {
    media.matches = false;
    media.dispatchEvent(new Event("change"));
  });
  expect(document.documentElement.style.colorScheme).toBe("light");
  expect(document.documentElement.style.getPropertyValue("--accent")).toBe(
    "#0000FF",
  );
  expect(fetcher).toHaveBeenCalledTimes(1);
});
it.each(["DARK", "SYSTEM"])(
  "@s32 removes private theme overrides when the authenticated boundary is removed: %s",
  async (theme) => {
    const media = Object.assign(new EventTarget(), { matches: true });
    vi.stubGlobal("matchMedia", vi.fn().mockReturnValue(media));
    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockResolvedValue(
          Response.json({ ...stored, theme }, { headers: { ETag: tag } }),
        ),
    );
    const view = render(
      <AppearanceProvider>
        <Appearance />
      </AppearanceProvider>,
    );
    await screen.findByRole("radio", {
      name: theme === "DARK" ? "Oscuro" : "Sistema",
    });
    view.unmount();
    expect(document.documentElement.dataset.theme).toBeUndefined();
    expect(document.documentElement.style.colorScheme).toBe("");
    expect(document.documentElement.style.getPropertyValue("--accent")).toBe(
      "",
    );
    for (const [target, event] of [
      [media, "change"],
      [document, "visibilitychange"],
    ] as const) {
      await act(async () => target.dispatchEvent(new Event(event)));
      expect(document.documentElement.dataset.theme).toBeUndefined();
      expect(document.documentElement.style.colorScheme).toBe("");
      expect(document.documentElement.style.getPropertyValue("--accent")).toBe(
        "",
      );
    }
  },
);
it("@s23 applies the confirmed dark scheme and accent to the document", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(Response.json(stored, { headers: { ETag: tag } })),
  );
  render(
    <AppearanceProvider>
      <Appearance />
    </AppearanceProvider>,
  );
  await waitFor(() =>
    expect(document.documentElement.style.colorScheme).toBe("dark"),
  );
  expect(document.documentElement.style.getPropertyValue("--accent")).toBe(
    "#00FFFF",
  );
});
it("@s26 retries a failed initial read manually without an endless loading state", async () => {
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(new Response(null, { status: 503 }))
    .mockResolvedValueOnce(Response.json(stored, { headers: { ETag: tag } }));
  vi.stubGlobal("fetch", fetcher);
  render(
    <AppearanceProvider>
      <Appearance />
    </AppearanceProvider>,
  );
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "No se pudo cargar la apariencia",
  );
  expect(screen.queryByText("Consultando apariencia…")).not.toBeInTheDocument();
  await userEvent.click(screen.getByRole("button", { name: "Reintentar" }));
  expect(await screen.findByRole("radio", { name: "Oscuro" })).toBeChecked();
  expect(fetcher).toHaveBeenCalledTimes(2);
});
it("@s26 announces the initial appearance read without presenting defaults as saved", () => {
  vi.stubGlobal("fetch", vi.fn().mockReturnValue(new Promise(() => {})));
  render(
    <AppearanceProvider>
      <Appearance />
    </AppearanceProvider>,
  );
  expect(screen.getByRole("status")).toHaveTextContent(
    "Consultando apariencia",
  );
  expect(
    screen.queryByRole("radio", { name: "Sistema" }),
  ).not.toBeInTheDocument();
});
it("@s20 loads the private appearance into native labelled controls", async () => {
  const fetcher = vi
    .fn()
    .mockResolvedValue(Response.json(stored, { headers: { ETag: tag } }));
  vi.stubGlobal("fetch", fetcher);
  render(
    <AppearanceProvider>
      <Appearance />
    </AppearanceProvider>,
  );
  expect(await screen.findByRole("radio", { name: "Oscuro" })).toBeChecked();
  expect(
    screen.getByRole("textbox", { name: "Color de acento claro" }),
  ).toHaveValue("#0000FF");
  expect(
    screen.getByRole("textbox", { name: "Color de acento oscuro" }),
  ).toHaveValue("#00FFFF");
  expect(
    screen.getByRole("heading", { level: 1, name: "Apariencia" }),
  ).toHaveFocus();
  expect(fetcher).toHaveBeenCalledTimes(1);
});

it("@s36 uses confirmed SYSTEM light accent when matchMedia is unavailable without writing", async () => {
  vi.stubGlobal("matchMedia", undefined);
  const fetcher = vi
    .fn()
    .mockResolvedValue(
      Response.json({ ...stored, theme: "SYSTEM" }, { headers: { ETag: tag } }),
    );
  vi.stubGlobal("fetch", fetcher);
  render(
    <AppearanceProvider>
      <Appearance />
    </AppearanceProvider>,
  );
  await waitFor(() => {
    expect(screen.getByRole("radio", { name: "Sistema" })).toBeChecked();
    expect(document.documentElement.dataset.theme).toBe("light");
  });
  expect(document.documentElement.style.colorScheme).toBe("light");
  expect(document.documentElement.style.getPropertyValue("--accent")).toBe(
    stored.accentLight,
  );
  expect(fetcher).toHaveBeenCalledTimes(1);
  expect(fetcher.mock.calls[0][0]).toBe("/api/v1/me/appearance");
  expect(fetcher.mock.calls[0][1].method).toBeUndefined();
});

it.each([
  ["LIGHT", "Claro", "light", stored.accentLight],
  ["DARK", "Oscuro", "dark", stored.accentDark],
])(
  "@s24 fixed %s ignores operating-system scheme changes without persisting",
  async (theme, label, effective, accent) => {
    const media = Object.assign(new EventTarget(), { matches: false });
    vi.stubGlobal("matchMedia", vi.fn().mockReturnValue(media));
    const fetcher = vi
      .fn()
      .mockResolvedValue(
        Response.json({ ...stored, theme }, { headers: { ETag: tag } }),
      );
    vi.stubGlobal("fetch", fetcher);
    render(
      <AppearanceProvider>
        <Appearance />
      </AppearanceProvider>,
    );
    await waitFor(() =>
      expect(screen.getByRole("radio", { name: label })).toBeChecked(),
    );
    await waitFor(() =>
      expect(document.documentElement.style.colorScheme).toBe(effective),
    );
    await act(async () => {
      media.matches = true;
      media.dispatchEvent(new Event("change"));
      document.dispatchEvent(new Event("visibilitychange"));
    });
    expect(document.documentElement.dataset.theme).toBe(effective);
    expect(document.documentElement.style.colorScheme).toBe(effective);
    expect(document.documentElement.style.getPropertyValue("--accent")).toBe(
      accent,
    );
    expect(screen.getByRole("radio", { name: label })).toBeChecked();
    expect(fetcher).toHaveBeenCalledTimes(1);
    expect(fetcher.mock.calls[0][0]).toBe("/api/v1/me/appearance");
    expect(fetcher.mock.calls[0][1].method).toBeUndefined();
  },
);
