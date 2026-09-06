import { SessionGate } from "./session-gate";
import { StrictMode } from "react";
import userEvent from "@testing-library/user-event";
import {
  render,
  screen,
  fireEvent,
  within,
  act,
  waitFor,
} from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { App } from "./App";
const api = "/api/v1/me/availability";
const absent = {
  configured: false,
  zoneId: null,
  dailyMinutes: null,
  updatedAt: null,
};
const absentTag = '"availability:unconfigured"';
afterEach(() => {
  window.history.replaceState(null, "", "/");
  vi.unstubAllGlobals();
});
it.each([
  [200, false],
  [503, false],
  [200, true],
  [503, true],
])(
  "@s39 Enter desde presupuesto recupera origen o conserva foco elegido: %s %s",
  async (status, moved) => {
    window.history.replaceState(null, "", "/disponibilidad");
    let finish!: (value: Response) => void;
    let body!: string;
    fixture((url, options) => {
      if (url === api && options?.method === "PUT") {
        body = String(options.body);
        return new Promise<Response>((resolve) => {
          finish = resolve;
        });
      }
    });
    render(<App />);
    await screen.findByText("Sin configurar");
    const user = userEvent.setup();
    await user.selectOptions(screen.getByLabelText("Zona horaria"), "UTC");
    const input = screen.getByLabelText("Lunes · minutos");
    await user.clear(input);
    await user.type(input, "77");
    await user.keyboard("{Enter}");
    expect(screen.getByText("Guardando disponibilidad")).toBeVisible();
    document.body.tabIndex = -1;
    document.body.focus();
    document.body.removeAttribute("tabindex");
    const cancel = screen.getByRole("link", {
      name: "Cancelar y volver a Proyectos",
    });
    if (moved) cancel.focus();
    await act(async () =>
      finish(
        Number(status) === 200
          ? Response.json(
              {
                configured: true,
                ...JSON.parse(body),
                updatedAt: "2026-09-06T12:00:00Z",
              },
              {
                headers: {
                  ETag: '"availability:6c5dbd10-9ad5-4000-8000-000000000001:0"',
                },
              },
            )
          : Response.json({}, { status: 503 }),
      ),
    );
    expect(moved ? cancel : input).toHaveFocus();
  },
);
it.each(["preferences", "zones"])(
  "@s30 @s34 permite varios reintentos reales de %s sin mantener el error anterior",
  async (source) => {
    window.history.replaceState(null, "", "/disponibilidad");
    const target = source === "preferences" ? api : `${api}/zones`;
    let reads = 0;
    let finish!: (value: Response) => void;
    fixture((url) =>
      url === target
        ? ++reads === 1
          ? Response.json({}, { status: 503 })
          : new Promise<Response>((resolve) => {
              finish = resolve;
            })
        : undefined,
    );
    render(<App />);
    const name =
      source === "preferences"
        ? "Reintentar disponibilidad"
        : "Reintentar zonas";
    fireEvent.click(await screen.findByRole("button", { name }));
    expect(screen.queryByRole("button", { name })).not.toBeInTheDocument();
    expect(
      screen.getByText(
        source === "preferences"
          ? "Consultando disponibilidad"
          : "Consultando zonas",
      ),
    ).toHaveAttribute("role", "status");
    await act(async () => finish(Response.json({}, { status: 503 })));
    fireEvent.click(await screen.findByRole("button", { name }));
    await waitFor(() => expect(reads).toBe(3));
    await act(async () =>
      finish(
        source === "preferences"
          ? Response.json(absent, { headers: { ETag: absentTag } })
          : Response.json({ items: ["UTC"] }),
      ),
    );
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "Guardar disponibilidad" }),
    ).toBeEnabled();
  },
);

it.each(["success", "reject"])(
  "@s37 el catálogo antiguo %s no reemplaza el catálogo vigente ni el borrador",
  async (outcome) => {
    window.history.replaceState(null, "", "/disponibilidad");
    let reads = 0;
    let resolve!: (value: Response) => void;
    let reject!: (error: Error) => void;
    fixture((url) =>
      url === `${api}/zones` && ++reads === 1
        ? new Promise<Response>((ok, no) => {
            resolve = ok;
            reject = no;
          })
        : undefined,
    );
    render(
      <StrictMode>
        <App />
      </StrictMode>,
    );
    await screen.findByText("Sin configurar");
    fireEvent.change(screen.getByLabelText("Zona horaria"), {
      target: { value: "CET" },
    });
    fireEvent.change(screen.getByLabelText("Lunes · minutos"), {
      target: { value: "35" },
    });
    await act(async () => {
      if (outcome === "reject") reject(new TypeError("offline"));
      else resolve(Response.json({ items: ["UTC"] }));
    });
    expect(screen.getByLabelText("Zona horaria")).toHaveValue("CET");
    expect(screen.getByRole("option", { name: "CET" })).toBeEnabled();
    expect(screen.getByLabelText("Lunes · minutos")).toHaveValue(35);
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  },
);

it.each(["success", "reject"])(
  "@s37 GET inicial antiguo %s no termina una recuperación que aún espera",
  async (outcome) => {
    window.history.replaceState(null, "", "/disponibilidad");
    let reads = 0;
    let resolveOld!: (value: Response) => void;
    let rejectOld!: (error: Error) => void;
    let finishCurrent!: (value: Response) => void;
    fixture((url, options) => {
      if (url !== api) return;
      if (options?.method === "PUT") return Response.json({}, { status: 412 });
      if (++reads === 1)
        return new Promise<Response>((ok, no) => {
          resolveOld = ok;
          rejectOld = no;
        });
      if (reads === 3)
        return new Promise<Response>((ok) => {
          finishCurrent = ok;
        });
    });
    render(
      <StrictMode>
        <App />
      </StrictMode>,
    );
    await screen.findByText("Sin configurar");
    fireEvent.change(screen.getByLabelText("Zona horaria"), {
      target: { value: "UTC" },
    });
    fireEvent.change(screen.getByLabelText("Lunes · minutos"), {
      target: { value: "35" },
    });
    fireEvent.click(
      screen.getByRole("button", { name: "Guardar disponibilidad" }),
    );
    fireEvent.click(
      await screen.findByRole("button", { name: "Recargar versión guardada" }),
    );
    await act(async () => {
      if (outcome === "reject") rejectOld(new TypeError("offline"));
      else resolveOld(Response.json(absent, { headers: { ETag: absentTag } }));
    });
    expect(screen.getByLabelText("Lunes · minutos")).toHaveValue(35);
    expect(screen.getByLabelText("Lunes · minutos")).toBeDisabled();
    expect(screen.getByText("Consultando disponibilidad")).toBeVisible();
    expect(
      screen.queryByText(
        "No se pudo recargar la disponibilidad. Tu borrador se conserva.",
      ),
    ).not.toBeInTheDocument();
    await act(async () =>
      finishCurrent(Response.json(absent, { headers: { ETag: absentTag } })),
    );
    expect(screen.getByLabelText("Lunes · minutos")).toHaveValue(0);
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  },
);
it("@s29 @s31 la carga inicial no anuncia error, CSRF ni guardado ficticio", async () => {
  window.history.replaceState(null, "", "/disponibilidad");
  let finish!: (value: Response) => void;
  fixture((url) =>
    url === api
      ? new Promise<Response>((resolve) => {
          finish = resolve;
        })
      : undefined,
  );
  render(<App />);
  expect(screen.getByText("Consultando disponibilidad")).toHaveAttribute(
    "role",
    "status",
  );
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  expect(screen.queryByText("Disponibilidad guardada")).not.toBeInTheDocument();
  await act(async () =>
    finish(Response.json(absent, { headers: { ETag: absentTag } })),
  );
  expect(screen.getByText("Sin configurar")).toBeVisible();
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  expect(screen.queryByText("Disponibilidad guardada")).not.toBeInTheDocument();
});

it("@s31 el segundo guardado usa la revisión confirmada y retira la sugerencia inicial", async () => {
  window.history.replaceState(null, "", "/disponibilidad");
  const original = new Intl.DateTimeFormat().resolvedOptions();
  vi.spyOn(Intl.DateTimeFormat.prototype, "resolvedOptions").mockReturnValue({
    ...original,
    timeZone: "UTC",
  });
  let writes = 0;
  const fetcher = fixture((url, options) =>
    url === api && options?.method === "PUT"
      ? Response.json(
          {
            configured: true,
            ...JSON.parse(String(options.body)),
            updatedAt: "2026-09-06T12:00:00Z",
          },
          {
            headers: {
              ETag: `"availability:6c5dbd10-9ad5-4000-8000-000000000001:${writes++}"`,
            },
          },
        )
      : undefined,
  );
  render(<App />);
  await screen.findByText("Sin configurar");
  expect(await screen.findByText("Sugerencia sin guardar")).toBeVisible();
  fireEvent.change(screen.getByLabelText("Lunes · minutos"), {
    target: { value: "35" },
  });
  fireEvent.click(
    screen.getByRole("button", { name: "Guardar disponibilidad" }),
  );
  await screen.findByText("Disponibilidad guardada");
  expect(screen.getByText("Disponibilidad configurada")).toBeVisible();
  expect(screen.queryByText("Sugerencia sin guardar")).not.toBeInTheDocument();
  fireEvent.change(screen.getByLabelText("Lunes · minutos"), {
    target: { value: "40" },
  });
  fireEvent.click(
    screen.getByRole("button", { name: "Guardar disponibilidad" }),
  );
  await screen.findByText("Disponibilidad guardada");
  const puts = fetcher.mock.calls.filter(
    ([, options]) => options?.method === "PUT",
  );
  expect(new Headers(puts[1][1]?.headers).get("If-Match")).toBe(
    '"availability:6c5dbd10-9ad5-4000-8000-000000000001:0"',
  );
  expect(JSON.parse(String(puts[1][1]?.body)).dailyMinutes.MONDAY).toBe(40);
});

it("@s31 el submit nativo queda cancelado para evitar navegación del formulario", async () => {
  window.history.replaceState(null, "", "/disponibilidad");
  fixture(() => undefined);
  render(<App />);
  await screen.findByText("Sin configurar");
  const form = screen
    .getByRole("button", { name: "Guardar disponibilidad" })
    .closest("form")!;
  const submit = new Event("submit", { bubbles: true, cancelable: true });
  fireEvent(form, submit);
  expect(submit.defaultPrevented).toBe(true);
});

it.each([
  [400, null],
  [400, "x"],
  [400, {}],
  [400, { errors: null }],
  [400, { errors: { field: "zoneId", message: "Revisa" } }],
  [400, { errors: [null] }],
  [400, { errors: ["x"] }],
  [400, { errors: [{ message: "Revisa" }] }],
  [400, { errors: [{ field: "other", message: "Revisa" }] }],
  [400, { errors: [{ field: "zoneId" }] }],
  [400, { errors: [{ field: "dailyMinutes.MONDAY", message: 42 }] }],
  [400, { errors: [{ field: "zoneId", message: { text: "Revisa" } }] }],
  [503, { errors: [{ field: "dailyMinutes.MONDAY", message: "Revisa" }] }],
])(
  "@s32 un error HTTP no reconocido conserva recuperación segura: %s %j",
  async (status, body) => {
    window.history.replaceState(null, "", "/disponibilidad");
    const fetcher = fixture((url, options) =>
      url === api && options?.method === "PUT"
        ? Response.json(body, { status: Number(status) })
        : undefined,
    );
    render(<App />);
    await screen.findByText("Sin configurar");
    fireEvent.change(screen.getByLabelText("Zona horaria"), {
      target: { value: "UTC" },
    });
    fireEvent.change(screen.getByLabelText("Lunes · minutos"), {
      target: { value: "35" },
    });
    fireEvent.click(
      screen.getByRole("button", { name: "Guardar disponibilidad" }),
    );
    expect(
      await screen.findByRole("button", { name: "Recargar versión guardada" }),
    ).toBeVisible();
    expect(screen.getByRole("alert")).toHaveTextContent(
      "No podemos confirmar el guardado",
    );
    expect(
      screen.getByRole("button", { name: "Guardar disponibilidad" }),
    ).toBeDisabled();
    expect(screen.getByLabelText("Lunes · minutos")).toHaveValue(35);
    expect(screen.getByLabelText("Lunes · minutos")).not.toHaveAttribute(
      "aria-invalid",
    );
    expect(
      fetcher.mock.calls.filter(([, options]) => options?.method === "PUT"),
    ).toHaveLength(1);
  },
);
it.each(["", " \n\t "])(
  "@s32 un mensaje de campo vacío produce un aviso útil: %j",
  async (message) => {
    window.history.replaceState(null, "", "/disponibilidad");
    const fetcher = fixture((url, options) =>
      url === api && options?.method === "PUT"
        ? Response.json(
            { errors: [{ field: "dailyMinutes.MONDAY", message }] },
            { status: 400 },
          )
        : undefined,
    );
    render(<App />);
    await screen.findByText("Sin configurar");
    fireEvent.change(screen.getByLabelText("Zona horaria"), {
      target: { value: "UTC" },
    });
    fireEvent.change(screen.getByLabelText("Lunes · minutos"), {
      target: { value: "35" },
    });
    fireEvent.click(
      screen.getByRole("button", { name: "Guardar disponibilidad" }),
    );
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "No podemos confirmar el guardado",
    );
    expect(screen.getByLabelText("Lunes · minutos")).toHaveValue(35);
    expect(
      screen.getByRole("button", { name: "Guardar disponibilidad" }),
    ).toBeDisabled();
    expect(
      fetcher.mock.calls.filter(([, options]) => options?.method === "PUT"),
    ).toHaveLength(1);
  },
);
it("@s35 cancelar un borrador sin enviarlo vuelve a Proyectos y no lo conserva", async () => {
  window.history.replaceState(null, "", "/disponibilidad");
  const fetcher = fixture(() => undefined);
  render(<App />);
  await screen.findByText("Sin configurar");
  fireEvent.change(screen.getByLabelText("Lunes · minutos"), {
    target: { value: "35" },
  });
  fireEvent.click(
    screen.getByRole("link", { name: "Cancelar y volver a Proyectos" }),
  );
  await screen.findByRole("heading", { name: "Proyectos" });
  expect(window.location.pathname).toBe("/proyectos");
  fireEvent.click(screen.getByRole("link", { name: "Disponibilidad" }));
  await screen.findByText("Sin configurar");
  expect(screen.getByLabelText("Lunes · minutos")).toHaveValue(0);
  expect(
    fetcher.mock.calls.filter(([, options]) => options?.method === "PUT"),
  ).toHaveLength(0);
});
it.each([false, true])(
  "@s39 un error de servidor respeta foco elegido o enfoca el campo: %s",
  async (moved) => {
    window.history.replaceState(null, "", "/disponibilidad");
    let finish!: (value: Response) => void;
    fixture((url, options) =>
      url === api && options?.method === "PUT"
        ? new Promise<Response>((resolve) => {
            finish = resolve;
          })
        : undefined,
    );
    render(<App />);
    await screen.findByText("Sin configurar");
    fireEvent.change(screen.getByLabelText("Zona horaria"), {
      target: { value: "UTC" },
    });
    const save = screen.getByRole("button", { name: "Guardar disponibilidad" });
    save.focus();
    fireEvent.click(save);
    const cancel = screen.getByRole("link", {
      name: "Cancelar y volver a Proyectos",
    });
    document.body.tabIndex = -1;
    document.body.focus();
    document.body.removeAttribute("tabindex");
    if (moved) cancel.focus();
    await act(async () =>
      finish(
        Response.json(
          {
            errors: [
              {
                field: "dailyMinutes.MONDAY",
                message: "Presupuesto no válido",
              },
            ],
          },
          { status: 400 },
        ),
      ),
    );
    expect(screen.getByText("Presupuesto no válido")).toBeVisible();
    expect(
      moved ? cancel : screen.getByLabelText("Lunes · minutos"),
    ).toHaveFocus();
  },
);
it.each([
  [200, false],
  [200, true],
  [503, false],
  [503, true],
])(
  "@s39 al resolver guardado restaura un destino utilizable sin robar foco: %s %s",
  async (status, moved) => {
    window.history.replaceState(null, "", "/disponibilidad");
    let finish!: (value: Response) => void;
    let body!: string;
    fixture((url, options) => {
      if (url === api && options?.method === "PUT") {
        body = String(options.body);
        return new Promise<Response>((resolve) => {
          finish = resolve;
        });
      }
    });
    render(<App />);
    await screen.findByText("Sin configurar");
    fireEvent.change(screen.getByLabelText("Zona horaria"), {
      target: { value: "UTC" },
    });
    const save = screen.getByRole("button", { name: "Guardar disponibilidad" });
    save.focus();
    fireEvent.click(save);
    document.body.tabIndex = -1;
    document.body.focus();
    document.body.removeAttribute("tabindex");
    const cancel = screen.getByRole("link", {
      name: "Cancelar y volver a Proyectos",
    });
    if (moved) cancel.focus();
    await act(async () =>
      finish(
        Number(status) === 503
          ? Response.json({}, { status: 503 })
          : Response.json(
              {
                configured: true,
                ...JSON.parse(body),
                updatedAt: "2026-09-06T12:00:00Z",
              },
              {
                headers: {
                  ETag: '"availability:6c5dbd10-9ad5-4000-8000-000000000001:0"',
                },
              },
            ),
      ),
    );
    expect(
      moved
        ? cancel
        : Number(status) === 503
          ? screen.getByRole("heading", { name: "Disponibilidad" })
          : save,
    ).toHaveFocus();
  },
);
it.each([
  "/disponibilidad-extra",
  "/prefijo/disponibilidad",
  "/disponibilidad/",
  "/disponibilidad?x=1",
])("@s47 login no acepta una variante de la ruta exacta: %s", async (route) => {
  window.history.replaceState(null, "", route);
  let authenticated = false;
  const fetcher = fixture((url, options) => {
    if (url !== "/api/session") return;
    if (options?.method === "POST") {
      authenticated = true;
      return new Response(null, { status: 204 });
    }
    return Response.json({
      authenticated,
      username: authenticated ? "Pablo" : null,
      csrfToken: "token",
      csrfHeaderName: "X-CSRF-TOKEN",
    });
  });
  render(<SessionGate />);
  fireEvent.click(
    await screen.findByRole("button", { name: "Iniciar sesión" }),
  );
  await screen.findByLabelText(/Nombre del proyecto/);
  expect(window.location.pathname + window.location.search).toBe("/");
  expect(
    screen.queryByRole("heading", { name: "Disponibilidad" }),
  ).not.toBeInTheDocument();
  expect(fetcher.mock.calls.some(([url]) => String(url).startsWith(api))).toBe(
    false,
  );
});
it.each(["success", "reject", "401"])(
  "@s36 @s37 cierre en otra pestaña retira borrador e ignora PUT viejo: %s",
  async (outcome) => {
    const channel = {
      onmessage: null as ((event: MessageEvent) => void) | null,
      postMessage: vi.fn(),
      close: vi.fn(),
    };
    vi.stubGlobal(
      "BroadcastChannel",
      vi.fn(function () {
        return channel;
      }),
    );
    window.history.replaceState(null, "", "/disponibilidad");
    let authenticated = true;
    let resolve!: (value: Response) => void;
    let reject!: (reason: Error) => void;
    const fetcher = fixture((url, options) => {
      if (url === "/api/session")
        return Response.json({
          authenticated,
          username: authenticated ? "Pablo" : null,
          csrfToken: "token",
          csrfHeaderName: "X-CSRF-TOKEN",
        });
      if (url === api && options?.method === "PUT")
        return new Promise<Response>((ok, no) => {
          resolve = ok;
          reject = no;
        });
    });
    render(<SessionGate />);
    await screen.findByText("Sin configurar");
    fireEvent.change(screen.getByLabelText("Zona horaria"), {
      target: { value: "UTC" },
    });
    fireEvent.change(screen.getByLabelText("Lunes · minutos"), {
      target: { value: "35" },
    });
    fireEvent.click(
      screen.getByRole("button", { name: "Guardar disponibilidad" }),
    );
    authenticated = false;
    act(() =>
      channel.onmessage?.(new MessageEvent("message", { data: "logout" })),
    );
    await screen.findByLabelText("Contraseña");
    await act(async () => {
      if (outcome === "reject") reject(new TypeError("offline"));
      else
        resolve(
          Response.json(
            {
              configured: true,
              zoneId: "UTC",
              dailyMinutes: {
                MONDAY: 35,
                TUESDAY: 0,
                WEDNESDAY: 0,
                THURSDAY: 0,
                FRIDAY: 0,
                SATURDAY: 0,
                SUNDAY: 0,
              },
              updatedAt: "2026-09-06T12:00:00Z",
            },
            {
              status: outcome === "401" ? 401 : 200,
              headers: {
                ETag: '"availability:6c5dbd10-9ad5-4000-8000-000000000001:0"',
              },
            },
          ),
        );
    });
    expect(screen.getByLabelText("Contraseña")).toBeVisible();
    expect(screen.queryByLabelText("Zona horaria")).not.toBeInTheDocument();
    expect(screen.queryByRole("spinbutton")).not.toBeInTheDocument();
    expect(
      screen.queryByText("Disponibilidad guardada"),
    ).not.toBeInTheDocument();
    expect(
      fetcher.mock.calls.filter(([url]) => String(url) === "/api/session"),
    ).toHaveLength(2);
    vi.unstubAllGlobals();
  },
);
it.each([
  ["preferences", false],
  ["zones", false],
  ["preferences", true],
  ["zones", true],
])(
  "@s39 reintentar %s devuelve contexto sin robar foco elegido: %s",
  async (source, moved) => {
    window.history.replaceState(null, "", "/disponibilidad");
    const target = source === "preferences" ? api : `${api}/zones`;
    let reads = 0;
    let finish!: (value: Response) => void;
    fixture((url) =>
      url === target
        ? ++reads === 1
          ? Response.json({}, { status: 503 })
          : new Promise<Response>((resolve) => {
              finish = resolve;
            })
        : undefined,
    );
    render(<App />);
    const retry = await screen.findByRole("button", {
      name:
        source === "preferences"
          ? "Reintentar disponibilidad"
          : "Reintentar zonas",
    });
    retry.focus();
    fireEvent.click(retry);
    const cancel = screen.getByRole("link", {
      name: "Cancelar y volver a Proyectos",
    });
    if (moved) cancel.focus();
    await act(async () =>
      finish(
        source === "preferences"
          ? Response.json(absent, { headers: { ETag: absentTag } })
          : Response.json({ items: ["UTC"] }),
      ),
    );
    expect(
      moved ? cancel : screen.getByRole("heading", { name: "Disponibilidad" }),
    ).toHaveFocus();
  },
);
it("@s24 @s32 CSRF se recupera deliberadamente sin repetir PUT ni perder el borrador", async () => {
  window.history.replaceState(null, "", "/disponibilidad");
  let sessions = 0;
  let writes = 0;
  const fetcher = fixture((url, options) => {
    if (url === "/api/session")
      return Response.json({
        authenticated: true,
        username: "Pablo",
        csrfToken: ++sessions === 1 ? "old-token" : "new-token",
        csrfHeaderName: "X-CSRF-TOKEN",
      });
    if (url === api && options?.method === "PUT") {
      if (++writes === 1)
        return Response.json({ code: "CSRF_INVALID" }, { status: 403 });
      return Response.json(
        {
          configured: true,
          ...JSON.parse(String(options.body)),
          updatedAt: "2026-09-06T12:00:00Z",
        },
        {
          headers: {
            ETag: '"availability:6c5dbd10-9ad5-4000-8000-000000000001:0"',
          },
        },
      );
    }
  });
  render(<SessionGate />);
  await screen.findByText("Sin configurar");
  fireEvent.change(screen.getByLabelText("Zona horaria"), {
    target: { value: "UTC" },
  });
  fireEvent.change(screen.getByLabelText("Lunes · minutos"), {
    target: { value: "35" },
  });
  fireEvent.click(
    screen.getByRole("button", { name: "Guardar disponibilidad" }),
  );
  const recover = await screen.findByRole("button", {
    name: "Recuperar acceso",
  });
  expect(
    within(screen.getByRole("main")).getByText(
      /La protección de sesión rechazó el guardado/,
    ),
  ).toHaveAttribute("role", "alert");
  fireEvent.click(recover);
  await waitFor(() => expect(recover).not.toBeVisible());
  expect(screen.getByLabelText("Lunes · minutos")).toHaveValue(35);
  expect(
    screen.getByRole("button", { name: "Guardar disponibilidad" }),
  ).toBeEnabled();
  expect(
    fetcher.mock.calls.filter(([, options]) => options?.method === "PUT"),
  ).toHaveLength(1);
  expect(
    new Headers(
      fetcher.mock.calls.find(([, options]) => options?.method === "PUT")![1]
        ?.headers,
    ).get("X-CSRF-TOKEN"),
  ).toBe("old-token");
  fireEvent.click(
    screen.getByRole("button", { name: "Guardar disponibilidad" }),
  );
  expect(await screen.findByText("Disponibilidad guardada")).toBeVisible();
  const puts = fetcher.mock.calls.filter(
    ([, options]) => options?.method === "PUT",
  );
  expect(
    screen.queryByText(/La protección de sesión rechazó el guardado/),
  ).not.toBeInTheDocument();
  expect(puts).toHaveLength(2);
  expect(puts[1][1]?.body).toBe(puts[0][1]?.body);
  expect(new Headers(puts[1][1]?.headers).get("X-CSRF-TOKEN")).toBe(
    "new-token",
  );
  expect(new Headers(puts[1][1]?.headers).get("If-Match")).toBe(absentTag);
  expect(
    fetcher.mock.calls.filter(
      ([url, options]) => String(url) === api && options?.method !== "PUT",
    ),
  ).toHaveLength(1);
});
it.each(["success", "reject", "401"])(
  "@s37 GET antiguo %s no reemplaza un PUT nuevo en StrictMode",
  async (outcome) => {
    window.history.replaceState(null, "", "/disponibilidad");
    let reads = 0;
    let resolve!: (value: Response) => void;
    let reject!: (reason: Error) => void;
    const fetcher = fixture((url, options) => {
      if (url !== api) return;
      if (options?.method === "PUT")
        return Response.json(
          {
            configured: true,
            ...JSON.parse(String(options.body)),
            updatedAt: "2026-09-06T12:00:00Z",
          },
          {
            headers: {
              ETag: '"availability:6c5dbd10-9ad5-4000-8000-000000000001:0"',
            },
          },
        );
      if (++reads === 1)
        return new Promise<Response>((ok, no) => {
          resolve = ok;
          reject = no;
        });
    });
    render(
      <StrictMode>
        <App />
      </StrictMode>,
    );
    await screen.findByText("Sin configurar");
    fireEvent.change(screen.getByLabelText("Zona horaria"), {
      target: { value: "UTC" },
    });
    fireEvent.change(screen.getByLabelText("Lunes · minutos"), {
      target: { value: "35" },
    });
    fireEvent.click(
      screen.getByRole("button", { name: "Guardar disponibilidad" }),
    );
    await screen.findByText("Disponibilidad guardada");
    await act(async () => {
      if (outcome === "reject") reject(new TypeError("offline"));
      else
        resolve(
          Response.json(absent, {
            status: outcome === "401" ? 401 : 200,
            headers: { ETag: absentTag },
          }),
        );
    });
    expect(screen.getByText("Disponibilidad guardada")).toBeVisible();
    expect(screen.getByLabelText("Lunes · minutos")).toHaveValue(35);
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
    expect(
      fetcher.mock.calls.find(([url]) => String(url) === api)![1]?.signal
        ?.aborted,
    ).toBe(true);
  },
);

it.each([api, `${api}/zones`])(
  "@s37 GET 401 tardío de %s no consulta otra vez la sesión tras navegar",
  async (target) => {
    window.history.replaceState(null, "", "/disponibilidad");
    let finish!: (value: Response) => void;
    const fetcher = fixture((url) =>
      url === target
        ? new Promise<Response>((resolve) => {
            finish = resolve;
          })
        : undefined,
    );
    render(<SessionGate />);
    await screen.findByRole("heading", { name: "Disponibilidad" });
    fireEvent.click(
      screen.getByRole("link", { name: "Cancelar y volver a Proyectos" }),
    );
    await screen.findByRole("heading", { name: "Proyectos" });
    await act(async () =>
      finish(Response.json({ code: "UNAUTHENTICATED" }, { status: 401 })),
    );
    expect(screen.getByRole("heading", { name: "Proyectos" })).toBeVisible();
    expect(
      fetcher.mock.calls.filter(([url]) => String(url) === "/api/session"),
    ).toHaveLength(1);
  },
);
it.each(["preferences", "zones", "write"])(
  "@s36 un 401 de %s retira disponibilidad y catálogo mediante SessionGate",
  async (source) => {
    window.history.replaceState(null, "", "/disponibilidad");
    let authenticated = true;
    let finish!: (value: Response) => void;
    fixture((url, options) => {
      if (url === "/api/session")
        return Response.json({
          authenticated,
          username: authenticated ? "Pablo" : null,
          csrfToken: "token",
          csrfHeaderName: "X-CSRF-TOKEN",
        });
      if (
        (source === "preferences" && url === api) ||
        (source === "zones" && url === `${api}/zones`) ||
        (source === "write" && url === api && options?.method === "PUT")
      )
        return new Promise<Response>((resolve) => {
          finish = resolve;
        });
    });
    render(<SessionGate />);
    if (source === "write") {
      await screen.findByText("Sin configurar");
      fireEvent.change(screen.getByLabelText("Zona horaria"), {
        target: { value: "UTC" },
      });
      fireEvent.change(screen.getByLabelText("Lunes · minutos"), {
        target: { value: "35" },
      });
      fireEvent.click(
        screen.getByRole("button", { name: "Guardar disponibilidad" }),
      );
    } else await waitFor(() => expect(finish).toBeTypeOf("function"));
    authenticated = false;
    await act(async () =>
      finish(Response.json({ code: "UNAUTHENTICATED" }, { status: 401 })),
    );
    expect(await screen.findByLabelText("Contraseña")).toBeVisible();
    expect(screen.queryByRole("spinbutton")).not.toBeInTheDocument();
    expect(screen.queryByLabelText("Zona horaria")).not.toBeInTheDocument();
    expect(
      screen.queryByText("Disponibilidad guardada"),
    ).not.toBeInTheDocument();
  },
);
it("@s31 @s40 guarda un no-op confirmado y retira el anuncio cuando cambia el borrador", async () => {
  window.history.replaceState(null, "", "/disponibilidad");
  const dailyMinutes = {
    MONDAY: 60,
    TUESDAY: 0,
    WEDNESDAY: 0,
    THURSDAY: 0,
    FRIDAY: 0,
    SATURDAY: 0,
    SUNDAY: 0,
  };
  const data = {
    configured: true,
    zoneId: "UTC",
    dailyMinutes,
    updatedAt: "2026-09-06T12:00:00Z",
  };
  const etag = '"availability:6c5dbd10-9ad5-4000-8000-000000000001:0"';
  const fetcher = fixture((url) =>
    url === api ? Response.json(data, { headers: { ETag: etag } }) : undefined,
  );
  render(<App />);
  await screen.findByText("Disponibilidad configurada");
  fireEvent.click(
    screen.getByRole("button", { name: "Guardar disponibilidad" }),
  );
  expect(await screen.findByText("Disponibilidad guardada")).toBeVisible();
  fireEvent.change(screen.getByLabelText("Lunes · minutos"), {
    target: { value: "61" },
  });
  expect(screen.queryByText("Disponibilidad guardada")).not.toBeInTheDocument();
  fireEvent.change(screen.getByLabelText("Lunes · minutos"), {
    target: { value: "60" },
  });
  fireEvent.click(
    screen.getByRole("button", { name: "Guardar disponibilidad" }),
  );
  await screen.findByText("Disponibilidad guardada");
  fireEvent.change(screen.getByLabelText("Zona horaria"), {
    target: { value: "CET" },
  });
  expect(screen.queryByText("Disponibilidad guardada")).not.toBeInTheDocument();
  expect(
    new Headers(
      fetcher.mock.calls.filter(
        ([, options]) => options?.method === "PUT",
      )[1][1]?.headers,
    ).get("If-Match"),
  ).toBe(etag);
});
it("@s35 @s37 cancelar retira el borrador y un PUT 401 tardío no invalida la vista actual", async () => {
  window.history.replaceState(null, "", "/disponibilidad");
  let finish!: (value: Response) => void;
  const fetcher = fixture((url, options) =>
    url === api && options?.method === "PUT"
      ? new Promise<Response>((resolve) => {
          finish = resolve;
        })
      : undefined,
  );
  render(<SessionGate />);
  await screen.findByText("Sin configurar");
  fireEvent.change(screen.getByLabelText("Zona horaria"), {
    target: { value: "UTC" },
  });
  fireEvent.change(screen.getByLabelText("Lunes · minutos"), {
    target: { value: "35" },
  });
  fireEvent.click(
    screen.getByRole("button", { name: "Guardar disponibilidad" }),
  );
  fireEvent.click(
    screen.getByRole("link", { name: "Cancelar y volver a Proyectos" }),
  );
  expect(
    await screen.findByRole("heading", { name: "Proyectos" }),
  ).toBeVisible();
  expect(screen.queryByRole("spinbutton")).not.toBeInTheDocument();
  await act(async () =>
    finish(Response.json({ code: "UNAUTHENTICATED" }, { status: 401 })),
  );
  expect(screen.getByRole("heading", { name: "Proyectos" })).toBeVisible();
  expect(screen.getByRole("button", { name: "Cerrar sesión" })).toBeVisible();
  expect(
    fetcher.mock.calls.filter(([url]) => String(url) === "/api/session"),
  ).toHaveLength(1);
  const options = fetcher.mock.calls.find(
    ([, options]) => options?.method === "PUT",
  )![1];
  expect(options?.signal?.aborted).toBe(true);
});
it("@s47 navega a Disponibilidad con aviso y salida determinista", async () => {
  fixture(() => undefined);
  render(<App />);
  expect(screen.getByRole("link", { name: "Proyectos" })).toHaveAttribute(
    "aria-current",
    "page",
  );
  expect(
    within(screen.getByRole("banner")).getByText("Proyectos"),
  ).toBeVisible();
  fireEvent.click(screen.getByRole("link", { name: "Disponibilidad" }));
  expect(window.location.pathname).toBe("/disponibilidad");
  expect(
    within(screen.getByRole("banner")).getByText("Disponibilidad"),
  ).toBeVisible();
  expect(screen.getByRole("main")).toHaveAttribute("tabindex", "-1");
  expect(screen.getByRole("heading", { name: "Disponibilidad" })).toBeVisible();
  expect(
    screen.getByRole("heading", { name: "Disponibilidad" }),
  ).toHaveAttribute("tabindex", "-1");
  expect(screen.getByRole("heading", { name: "Disponibilidad" })).toHaveFocus();
  expect(
    screen.getByText("Los cambios sin guardar se pierden al salir"),
  ).toBeVisible();
  expect(screen.getByRole("link", { name: "Disponibilidad" })).toHaveAttribute(
    "aria-current",
    "page",
  );
  expect(screen.getByRole("link", { name: "Proyectos" })).not.toHaveAttribute(
    "aria-current",
  );
  expect(
    screen.getByRole("link", { name: "Cancelar y volver a Proyectos" }),
  ).toHaveAttribute("href", "/proyectos");
});
it("@s47 vuelve a la ruta exacta después de iniciar sesión", async () => {
  window.history.replaceState(null, "", "/disponibilidad");
  let signedIn = false;
  fixture((url, options) => {
    if (url === "/api/session") {
      if (options?.method === "POST") {
        signedIn = true;
        return new Response(null, { status: 204 });
      }
      return Response.json({
        authenticated: signedIn,
        username: signedIn ? "Pablo" : null,
        csrfToken: "token",
        csrfHeaderName: "X-CSRF-TOKEN",
      });
    }
  });
  render(<SessionGate />);
  fireEvent.change(await screen.findByLabelText("Usuario"), {
    target: { value: "Pablo" },
  });
  fireEvent.change(screen.getByLabelText("Contraseña"), {
    target: { value: "synthetic" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Iniciar sesión" }));
  expect(
    await screen.findByRole("heading", { name: "Disponibilidad" }),
  ).toBeVisible();
  expect(window.location.pathname).toBe("/disponibilidad");
});
function fixture(
  override: (
    url: string,
    options?: RequestInit,
  ) => Response | Promise<Response> | undefined,
) {
  return vi
    .spyOn(globalThis, "fetch")
    .mockImplementation(async (input, options) => {
      const url = String(input);
      const response = override(url, options);
      if (response) return response;
      if (url === api)
        return Response.json(absent, { headers: { ETag: absentTag } });
      if (url === `${api}/zones`)
        return Response.json({ items: ["CET", "Europe/Madrid", "UTC"] });
      if (url === "/api/session")
        return Response.json({
          authenticated: true,
          username: "Pablo",
          csrfToken: "token",
          csrfHeaderName: "X-CSRF-TOKEN",
        });
      if (url === "/api/v1/projects")
        return Response.json({ items: [], nextCursor: null });
      throw new Error(`Unexpected ${url}`);
    });
}

it("@s29 una ausencia confirmada presenta siete ceros editables sin guardarlos", async () => {
  window.history.replaceState(null, "", "/disponibilidad");
  const fetcher = fixture(() => undefined);
  render(<App />);
  expect(await screen.findByText("Sin configurar")).toBeVisible();
  for (const day of [
    "Lunes",
    "Martes",
    "Miércoles",
    "Jueves",
    "Viernes",
    "Sábado",
    "Domingo",
  ]) {
    expect(screen.getByLabelText(`${day} · minutos`)).toHaveValue(0);
    expect(screen.getByLabelText(`${day} · minutos`)).toBeEnabled();
  }
  fireEvent.change(screen.getByLabelText("Lunes · minutos"), {
    target: { value: "30" },
  });
  expect(screen.getByLabelText("Lunes · minutos")).toHaveValue(30);
  expect(screen.getByText("0 permite descansar")).toBeVisible();
  expect(
    fetcher.mock.calls.filter(([, options]) => options?.method === "PUT"),
  ).toHaveLength(0);
});

it("@s29 @s30 usa el catálogo para una sugerencia sin guardar y conserva elección", async () => {
  window.history.replaceState(null, "", "/disponibilidad");
  const original = new Intl.DateTimeFormat().resolvedOptions();
  vi.spyOn(Intl.DateTimeFormat.prototype, "resolvedOptions").mockReturnValue({
    ...original,
    timeZone: "Europe/Madrid",
  });
  fixture(() => undefined);
  render(<App />);
  const zone = await screen.findByLabelText("Zona horaria");
  await waitFor(() => expect(zone).toHaveValue("Europe/Madrid"));
  expect(zone.tagName).toBe("SELECT");
  expect(screen.getAllByRole("option")).toHaveLength(4);
  expect(screen.getByRole("option", { name: "Europe/Madrid" })).toBeEnabled();
  expect(
    screen.getByRole("option", { name: "Selecciona una zona" }),
  ).toBeEnabled();
  expect(screen.getByText("Sugerencia sin guardar")).toBeVisible();
  fireEvent.change(zone, { target: { value: "CET" } });
  expect(zone).toHaveValue("CET");
  expect(screen.queryByText("Sugerencia sin guardar")).not.toBeInTheDocument();
});
it("@s30 @s45 conserva zona histórica fuera del catálogo como no disponible", async () => {
  window.history.replaceState(null, "", "/disponibilidad");
  const dailyMinutes = {
    MONDAY: 60,
    TUESDAY: 60,
    WEDNESDAY: 60,
    THURSDAY: 60,
    FRIDAY: 60,
    SATURDAY: 0,
    SUNDAY: 0,
  };
  fixture((url) =>
    url === api
      ? Response.json(
          {
            configured: true,
            zoneId: "Historic/Removed",
            dailyMinutes,
            updatedAt: "2026-09-06T12:00:00Z",
          },
          {
            headers: {
              ETag: '"availability:6c5dbd10-9ad5-4000-8000-000000000001:0"',
            },
          },
        )
      : undefined,
  );
  render(<App />);
  expect(await screen.findByLabelText("Zona horaria")).toHaveValue(
    "Historic/Removed",
  );
  expect(
    screen.getByRole("option", { name: "Historic/Removed · No disponible" }),
  ).toBeInTheDocument();
  expect(screen.getByLabelText("Lunes · minutos")).toHaveValue(60);
  expect(screen.getByLabelText("Domingo · minutos")).toHaveValue(0);
});
it("@s30 un catálogo pendiente no declara inválida una zona guardada", async () => {
  window.history.replaceState(null, "", "/disponibilidad");
  let finish!: (response: Response) => void;
  fixture((url) =>
    url === `${api}/zones`
      ? new Promise((resolve) => {
          finish = resolve;
        })
      : url === api
        ? Response.json(
            {
              configured: true,
              zoneId: "Europe/Madrid",
              dailyMinutes: {
                MONDAY: 60,
                TUESDAY: 60,
                WEDNESDAY: 60,
                THURSDAY: 60,
                FRIDAY: 60,
                SATURDAY: 0,
                SUNDAY: 0,
              },
              updatedAt: "2026-09-06T12:00:00Z",
            },
            {
              headers: {
                ETag: '"availability:6c5dbd10-9ad5-4000-8000-000000000001:0"',
              },
            },
          )
        : undefined,
  );
  render(<App />);
  await screen.findByLabelText("Zona horaria");
  expect(
    screen.queryByRole("option", { name: /No disponible/ }),
  ).not.toBeInTheDocument();
  expect(screen.getByText("Consultando zonas")).toHaveAttribute(
    "role",
    "status",
  );
  expect(screen.getByLabelText("Zona horaria")).toHaveValue("Europe/Madrid");
  expect(
    screen.getByRole("option", { name: "Europe/Madrid · Zona guardada" }),
  ).toBeDisabled();
  await act(async () =>
    finish(Response.json({ items: ["Europe/Madrid", "UTC"] })),
  );
  expect(screen.getAllByRole("option")).toHaveLength(3);
  expect(screen.getByRole("option", { name: "Europe/Madrid" })).toBeEnabled();
});
it("@s29 @s31 guarda el borrador inicial sin exigir edición y confirma sólo respuesta válida", async () => {
  window.history.replaceState(null, "", "/disponibilidad");
  const options = new Intl.DateTimeFormat().resolvedOptions();
  vi.spyOn(Intl.DateTimeFormat.prototype, "resolvedOptions").mockReturnValue({
    ...options,
    timeZone: "UTC",
  });
  let finish!: (response: Response) => void;
  const fetcher = fixture((url, options) =>
    url === api && options?.method === "PUT"
      ? new Promise((resolve) => {
          finish = resolve;
        })
      : undefined,
  );
  render(<App />);
  await screen.findByText("Sin configurar");
  await waitFor(() =>
    expect(screen.getByLabelText("Zona horaria")).toHaveValue("UTC"),
  );
  const save = screen.getByRole("button", { name: "Guardar disponibilidad" });
  fireEvent.click(save);
  expect(save).toBeDisabled();
  expect(screen.getByText("Guardando disponibilidad")).toBeVisible();
  expect(screen.queryByText("Disponibilidad guardada")).not.toBeInTheDocument();
  for (const input of screen.getAllByRole("spinbutton"))
    expect(input).toBeDisabled();
  expect(screen.getByLabelText("Zona horaria")).toBeDisabled();
  fireEvent.click(save);
  const writes = fetcher.mock.calls.filter(
    ([, options]) => options?.method === "PUT",
  );
  expect(writes).toHaveLength(1);
  const payload = JSON.parse(String(writes[0][1]?.body));
  expect(payload).toEqual({
    zoneId: "UTC",
    dailyMinutes: {
      MONDAY: 0,
      TUESDAY: 0,
      WEDNESDAY: 0,
      THURSDAY: 0,
      FRIDAY: 0,
      SATURDAY: 0,
      SUNDAY: 0,
    },
  });
  expect(new Headers(writes[0][1]?.headers).get("If-Match")).toBe(absentTag);
  await act(async () =>
    finish(
      Response.json(
        { configured: true, ...payload, updatedAt: "2026-09-06T12:00:00Z" },
        {
          headers: {
            ETag: '"availability:6c5dbd10-9ad5-4000-8000-000000000001:0"',
          },
        },
      ),
    ),
  );
  expect(screen.getByText("Disponibilidad guardada")).toBeVisible();
  expect(
    screen.getByRole("button", { name: "Guardar disponibilidad" }),
  ).toBeEnabled();
});

it.each(["", "-1", "1441", "1.5"])(
  "@s38 @s42 rechaza presupuesto incompleto o fuera del rango: %s",
  async (value) => {
    window.history.replaceState(null, "", "/disponibilidad");
    const fetcher = fixture(() => undefined);
    render(<App />);
    await screen.findByText("Sin configurar");
    fireEvent.change(screen.getByLabelText("Zona horaria"), {
      target: { value: "UTC" },
    });
    const monday = screen.getByLabelText("Lunes · minutos");
    fireEvent.change(monday, { target: { value } });
    fireEvent.click(
      screen.getByRole("button", { name: "Guardar disponibilidad" }),
    );
    expect(monday).toHaveAttribute("aria-invalid", "true");
    expect(monday).toHaveFocus();
    expect(
      screen.getByText("Introduce minutos enteros entre 0 y 1440"),
    ).toBeVisible();
    expect(
      fetcher.mock.calls.filter(([, options]) => options?.method === "PUT"),
    ).toHaveLength(0);
  },
);

it("@s40 @s42 calcula sólo un total completo del borrador y no trabajo realizado", async () => {
  window.history.replaceState(null, "", "/disponibilidad");
  fixture(() => undefined);
  render(<App />);
  await screen.findByText("Sin configurar");
  expect(
    screen.getByText("Total semanal del borrador: 0 minutos"),
  ).toBeVisible();
  fireEvent.change(screen.getByLabelText("Lunes · minutos"), {
    target: { value: "1440" },
  });
  fireEvent.change(screen.getByLabelText("Martes · minutos"), {
    target: { value: "1" },
  });
  expect(
    screen.getByText("Total semanal del borrador: 1441 minutos"),
  ).toBeVisible();
  expect(
    screen.getByText("Es disponibilidad prevista, no trabajo realizado"),
  ).toBeVisible();
  for (const value of ["", "1e", "-1", "1441", "1.5"]) {
    fireEvent.change(screen.getByLabelText("Domingo · minutos"), {
      target: { value },
    });
    expect(
      screen.getByText(
        "Completa los siete presupuestos para calcular el total",
      ),
    ).toBeVisible();
    expect(
      screen.queryByText(/^Total semanal del borrador:/),
    ).not.toBeInTheDocument();
  }
  fireEvent.change(screen.getByLabelText("Domingo · minutos"), {
    target: { value: "0" },
  });
  expect(
    screen.getByText("Total semanal del borrador: 1441 minutos"),
  ).toBeVisible();
});

it.each([503, 200])(
  "@s30 recupera catálogo fallido sin alterar el borrador: %s",
  async (status) => {
    window.history.replaceState(null, "", "/disponibilidad");
    let reads = 0;
    const fetcher = fixture((url) =>
      url === `${api}/zones`
        ? ++reads === 1
          ? Response.json({ items: [] }, { status })
          : Response.json({ items: ["UTC"] })
        : undefined,
    );
    render(<App />);
    await screen.findByText("Sin configurar");
    fireEvent.change(screen.getByLabelText("Lunes · minutos"), {
      target: { value: "35" },
    });
    const retry = await screen.findByRole("button", {
      name: "Reintentar zonas",
    });
    expect(screen.getByLabelText("Zona horaria")).toHaveValue("");
    fireEvent.click(
      screen.getByRole("button", { name: "Guardar disponibilidad" }),
    );
    expect(
      fetcher.mock.calls.filter(([, options]) => options?.method === "PUT"),
    ).toHaveLength(0);
    fireEvent.click(retry);
    await screen.findByRole("option", { name: "UTC" });
    expect(screen.getByLabelText("Lunes · minutos")).toHaveValue(35);
    expect(
      screen.queryByRole("button", { name: "Reintentar zonas" }),
    ).not.toBeInTheDocument();
  },
);

it.each([503, 200])(
  "@s41 una lectura inicial fallida no inventa ausencia ni formulario: %s",
  async (status) => {
    window.history.replaceState(null, "", "/disponibilidad");
    let reads = 0;
    fixture((url) =>
      url === api
        ? ++reads === 1
          ? Response.json(absent, { status })
          : Response.json(absent, { headers: { ETag: absentTag } })
        : undefined,
    );
    render(<App />);
    const retry = await screen.findByRole("button", {
      name: "Reintentar disponibilidad",
    });
    expect(screen.queryByText("Sin configurar")).not.toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "Guardar disponibilidad" }),
    ).not.toBeInTheDocument();
    expect(screen.queryByRole("spinbutton")).not.toBeInTheDocument();
    fireEvent.click(retry);
    expect(await screen.findByText("Sin configurar")).toBeVisible();
    expect(
      screen.queryByRole("button", { name: "Reintentar disponibilidad" }),
    ).not.toBeInTheDocument();
  },
);

it.each([412, 503, 200, "network"])(
  "@s32 exige recarga tras guardado no confirmado sin perder borrador: %s",
  async (status) => {
    window.history.replaceState(null, "", "/disponibilidad");
    const fetcher = fixture((url, options) =>
      url === api && options?.method === "PUT"
        ? status === "network"
          ? Promise.reject(new TypeError("offline"))
          : Response.json(absent, { status: Number(status) })
        : undefined,
    );
    render(<App />);
    await screen.findByText("Sin configurar");
    fireEvent.change(screen.getByLabelText("Zona horaria"), {
      target: { value: "UTC" },
    });
    fireEvent.change(screen.getByLabelText("Lunes · minutos"), {
      target: { value: "35" },
    });
    fireEvent.click(
      screen.getByRole("button", { name: "Guardar disponibilidad" }),
    );
    expect(
      await screen.findByRole("button", { name: "Recargar versión guardada" }),
    ).toBeVisible();
    expect(screen.getByLabelText("Lunes · minutos")).toHaveValue(35);
    expect(screen.getByLabelText("Zona horaria")).toHaveValue("UTC");
    expect(
      screen.queryByText("Disponibilidad guardada"),
    ).not.toBeInTheDocument();
    const save = screen.getByRole("button", { name: "Guardar disponibilidad" });
    expect(save).toBeDisabled();
    fireEvent.click(save);
    expect(
      fetcher.mock.calls.filter(([, options]) => options?.method === "PUT"),
    ).toHaveLength(1);
    expect(
      screen.getByText(
        "La recarga descartará tu borrador sólo cuando recibamos una versión válida",
      ),
    ).toBeVisible();
  },
);

it("@s33 @s34 @s43 recarga deliberada conserva valores hasta GET válido y permite recuperarse", async () => {
  window.history.replaceState(null, "", "/disponibilidad");
  let reads = 0;
  let finish!: (value: Response) => void;
  const fetcher = fixture((url, options) => {
    if (url !== api) return;
    if (options?.method === "PUT") return Response.json({}, { status: 412 });
    if (++reads > 1)
      return new Promise<Response>((resolve) => {
        finish = resolve;
      });
  });
  render(<App />);
  await screen.findByText("Sin configurar");
  fireEvent.change(screen.getByLabelText("Zona horaria"), {
    target: { value: "UTC" },
  });
  fireEvent.change(screen.getByLabelText("Lunes · minutos"), {
    target: { value: "35" },
  });
  fireEvent.click(
    screen.getByRole("button", { name: "Guardar disponibilidad" }),
  );
  fireEvent.click(
    await screen.findByRole("button", { name: "Recargar versión guardada" }),
  );
  expect(screen.getByLabelText("Lunes · minutos")).toHaveValue(35);
  for (const input of screen.getAllByRole("spinbutton"))
    expect(input).toBeDisabled();
  expect(screen.getByLabelText("Zona horaria")).toBeDisabled();
  expect(
    screen.getByRole("button", { name: "Guardar disponibilidad" }),
  ).toBeDisabled();
  expect(screen.getByRole("status")).toHaveTextContent(
    "Consultando disponibilidad",
  );
  await act(async () => finish(Response.json({}, { status: 503 })));
  expect(screen.getByLabelText("Lunes · minutos")).toHaveValue(35);
  expect(
    screen.getByText(
      "No se pudo recargar la disponibilidad. Tu borrador se conserva.",
    ),
  ).toBeVisible();
  const reload = screen.getByRole("button", {
    name: "Recargar versión guardada",
  });
  reload.focus();
  fireEvent.click(reload);
  document.body.tabIndex = -1;
  document.body.focus();
  document.body.removeAttribute("tabindex");
  expect(
    screen.queryByText(
      "No se pudo recargar la disponibilidad. Tu borrador se conserva.",
    ),
  ).not.toBeInTheDocument();
  const dailyMinutes = {
    MONDAY: 90,
    TUESDAY: 0,
    WEDNESDAY: 0,
    THURSDAY: 0,
    FRIDAY: 0,
    SATURDAY: 0,
    SUNDAY: 0,
  };
  await act(async () =>
    finish(
      Response.json(
        {
          configured: true,
          zoneId: "CET",
          dailyMinutes,
          updatedAt: "2026-09-06T12:00:00Z",
        },
        {
          headers: {
            ETag: '"availability:6c5dbd10-9ad5-4000-8000-000000000001:5"',
          },
        },
      ),
    ),
  );
  expect(screen.getByLabelText("Lunes · minutos")).toHaveValue(90);
  expect(screen.getByLabelText("Zona horaria")).toHaveValue("CET");
  expect(screen.getByRole("heading", { name: "Disponibilidad" })).toHaveFocus();
  expect(
    screen.getByRole("button", { name: "Guardar disponibilidad" }),
  ).toBeEnabled();
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  expect(
    fetcher.mock.calls.filter(([, options]) => options?.method === "PUT"),
  ).toHaveLength(1);
});

it("@s32 el error de campo permite corregir y guardar sin una recarga que pierda el borrador", async () => {
  window.history.replaceState(null, "", "/disponibilidad");
  let writes = 0;
  fixture((url, options) => {
    if (url !== api || options?.method !== "PUT") return;
    if (++writes === 1)
      return Response.json(
        {
          errors: [
            {
              field: "dailyMinutes.MONDAY",
              code: "OUT_OF_RANGE",
              message: "Presupuesto no válido",
            },
          ],
        },
        { status: 400 },
      );
    return Response.json(
      {
        configured: true,
        ...JSON.parse(String(options.body)),
        updatedAt: "2026-09-06T12:00:00Z",
      },
      {
        headers: {
          ETag: '"availability:6c5dbd10-9ad5-4000-8000-000000000001:0"',
        },
      },
    );
  });
  render(<App />);
  await screen.findByText("Sin configurar");
  fireEvent.change(screen.getByLabelText("Zona horaria"), {
    target: { value: "UTC" },
  });
  fireEvent.change(screen.getByLabelText("Lunes · minutos"), {
    target: { value: "35" },
  });
  fireEvent.click(
    screen.getByRole("button", { name: "Guardar disponibilidad" }),
  );
  expect(await screen.findByText("Presupuesto no válido")).toBeVisible();
  const monday = screen.getByLabelText("Lunes · minutos");
  expect(monday).toHaveValue(35);
  expect(monday).toHaveAttribute("aria-invalid", "true");
  expect(monday).toHaveAccessibleDescription("Presupuesto no válido");
  expect(monday).toHaveFocus();
  expect(
    screen.getByRole("button", { name: "Guardar disponibilidad" }),
  ).toBeEnabled();
  expect(
    screen.queryByRole("button", { name: "Recargar versión guardada" }),
  ).not.toBeInTheDocument();
  fireEvent.change(monday, { target: { value: "30" } });
  fireEvent.click(
    screen.getByRole("button", { name: "Guardar disponibilidad" }),
  );
  expect(await screen.findByText("Disponibilidad guardada")).toBeVisible();
  expect(monday).not.toHaveAttribute("aria-invalid");
  expect(writes).toBe(2);
});

it("@s30 @s46 exige elegir una zona del catálogo y localiza el error del servidor", async () => {
  window.history.replaceState(null, "", "/disponibilidad");
  const original = new Intl.DateTimeFormat().resolvedOptions();
  vi.spyOn(Intl.DateTimeFormat.prototype, "resolvedOptions").mockReturnValue({
    ...original,
    timeZone: "Unknown/Zone",
  });
  let zoneWrites = 0;
  const fetcher = fixture((url, options) =>
    url === api && options?.method === "PUT"
      ? ++zoneWrites === 1
        ? Response.json(
            {
              errors: [
                {
                  field: "zoneId",
                  code: "INVALID_VALUE",
                  message: "Elige una zona disponible",
                },
              ],
            },
            { status: 400 },
          )
        : Response.json(
            {
              configured: true,
              ...JSON.parse(String(options.body)),
              updatedAt: "2026-09-06T12:00:00Z",
            },
            {
              headers: {
                ETag: '"availability:6c5dbd10-9ad5-4000-8000-000000000001:0"',
              },
            },
          )
      : undefined,
  );
  render(<App />);
  await screen.findByText("Sin configurar");
  const zone = screen.getByLabelText("Zona horaria");
  expect(zone).toHaveValue("");
  fireEvent.click(
    screen.getByRole("button", { name: "Guardar disponibilidad" }),
  );
  expect(zone).toHaveAttribute("aria-invalid", "true");
  expect(zone).toHaveFocus();
  expect(
    fetcher.mock.calls.filter(([, options]) => options?.method === "PUT"),
  ).toHaveLength(0);
  fireEvent.change(zone, { target: { value: "UTC" } });
  fireEvent.click(
    screen.getByRole("button", { name: "Guardar disponibilidad" }),
  );
  expect(await screen.findByText("Elige una zona disponible")).toBeVisible();
  expect(zone).toHaveAccessibleDescription("Elige una zona disponible");
  expect(zone).toHaveAttribute("aria-invalid", "true");
  expect(zone).toHaveFocus();
  expect(
    screen.getByRole("button", { name: "Guardar disponibilidad" }),
  ).toBeEnabled();
  fireEvent.change(zone, { target: { value: "CET" } });
  fireEvent.click(
    screen.getByRole("button", { name: "Guardar disponibilidad" }),
  );
  await screen.findByText("Disponibilidad guardada");
  expect(zone).not.toHaveAttribute("aria-invalid");
  expect(
    screen.queryByText("Elige una zona disponible"),
  ).not.toBeInTheDocument();
});
