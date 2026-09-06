vi.mock("./project-tasks", () => ({ ProjectTasks: () => null }));
import { StrictMode, Profiler } from "react";
import {
  render,
  screen,
  waitFor,
  fireEvent,
  act,
} from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { SessionGate } from "./session-gate";
const anonymous = {
  authenticated: false,
  username: null,
  csrfToken: "anonymous-token",
  csrfHeaderName: "X-CSRF-TOKEN",
};
const authenticated = {
  ...anonymous,
  authenticated: true,
  username: "Pablo",
  csrfToken: "private-token",
};
afterEach(() => {
  window.history.replaceState(null, "", "/");
  vi.unstubAllGlobals();
});
it("@s1 comprueba acceso antes de montar vistas privadas", async () => {
  let firstCommit: string | undefined;
  let finish!: (response: Response) => void;
  const fetcher = vi.spyOn(globalThis, "fetch").mockImplementationOnce(
    () =>
      new Promise((resolve) => {
        finish = resolve;
      }),
  );
  render(
    <Profiler
      id="initial-access"
      onRender={() => {
        firstCommit ??=
          document.querySelector('[role="status"]')?.textContent ?? "missing";
      }}
    >
      <SessionGate />
    </Profiler>,
  );
  expect(firstCommit).toBe("Comprobando acceso");
  expect(screen.getByRole("status")).toHaveTextContent("Comprobando acceso");
  expect(
    screen.queryByLabelText(/Nombre del proyecto/),
  ).not.toBeInTheDocument();
  await act(async () => finish(Response.json(anonymous)));
  expect(screen.getByRole("textbox", { name: "Usuario" })).toHaveAttribute(
    "autocomplete",
    "username",
  );
  expect(screen.getByLabelText("Contraseña")).toHaveAttribute(
    "autocomplete",
    "current-password",
  );
  expect(fetcher).toHaveBeenCalledWith(
    "/api/session",
    expect.objectContaining({ credentials: "same-origin", cache: "no-store" }),
  );
  expect(fetcher).toHaveBeenCalledTimes(1);
});
it("@s2 confirma la sesión después de enviar formulario con CSRF", async () => {
  const fetcher = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(Response.json(anonymous))
    .mockResolvedValueOnce(new Response(null, { status: 204 }))
    .mockResolvedValueOnce(Response.json(authenticated));
  render(<SessionGate />);
  fireEvent.change(await screen.findByLabelText("Usuario"), {
    target: { value: "Pablo" },
  });
  fireEvent.change(screen.getByLabelText("Contraseña"), {
    target: { value: "secret & +" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Iniciar sesión" }));
  expect(await screen.findByLabelText(/Nombre del proyecto/)).toBeVisible();
  expect(fetcher.mock.calls[1]).toEqual([
    "/api/session",
    expect.objectContaining({
      method: "POST",
      credentials: "same-origin",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded",
        "X-CSRF-TOKEN": "anonymous-token",
      },
      body: "username=Pablo&password=secret+%26+%2B",
    }),
  ]);
  expect(fetcher.mock.calls[2][0]).toBe("/api/session");
  expect(screen.queryByLabelText("Contraseña")).not.toBeInTheDocument();
});
it.each([
  null,
  { ...authenticated, csrfToken: "" },
  { ...authenticated, extra: "secret" },
  { ...authenticated, username: null },
  { ...anonymous, username: "unexpected" },
  { ...authenticated, csrfHeaderName: "Other" },
])("@s1 no abre datos ante sesión incompatible %j", async (body) => {
  vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(Response.json(body));
  render(<SessionGate />);
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "No hemos podido comprobar el acceso",
  );
  expect(
    screen.queryByLabelText(/Nombre del proyecto/),
  ).not.toBeInTheDocument();
  expect(screen.getByRole("button", { name: "Reintentar" })).toBeEnabled();
});
it.each([401, 503, "network"])(
  "@s3 @s12 @s14 login fallido %s bloquea doble envío y borra contraseña",
  async (status) => {
    let finish!: (response: Response) => void;
    let fail!: (error: Error) => void;
    const fetcher = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(Response.json(anonymous))
      .mockImplementationOnce(
        () =>
          new Promise((resolve, reject) => {
            finish = resolve;
            fail = reject;
          }),
      );
    render(<SessionGate />);
    fireEvent.change(await screen.findByLabelText("Usuario"), {
      target: { value: "Pablo" },
    });
    fireEvent.change(screen.getByLabelText("Contraseña"), {
      target: { value: "private" },
    });
    const button = screen.getByRole("button", { name: "Iniciar sesión" });
    fireEvent.click(button);
    expect(button).toBeDisabled();
    expect(screen.getByRole("status")).toHaveTextContent(
      "Comprobando credenciales",
    );
    fireEvent.submit(button.closest("form")!);
    expect(fetcher).toHaveBeenCalledTimes(2);
    await act(async () =>
      typeof status === "number"
        ? finish(Response.json({ title: "private SQL" }, { status }))
        : fail(new Error("private network")),
    );
    expect(screen.getByRole("alert")).toHaveTextContent(
      status === 401
        ? "Usuario o contraseña incorrectos"
        : "No podemos confirmar el acceso",
    );
    expect(screen.getByLabelText("Contraseña")).toHaveValue("");
    expect(screen.getByLabelText("Usuario")).toHaveValue("Pablo");
    expect(button).toBeEnabled();
    expect(fetcher).toHaveBeenCalledTimes(2);
  },
);
it("@s18 crear desde la sesión real envía el token renovado", async () => {
  const fetcher = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(Response.json(authenticated))
    .mockResolvedValueOnce(Response.json({}, { status: 503 }));
  render(<SessionGate />);
  fireEvent.change(await screen.findByLabelText(/Nombre del proyecto/), {
    target: { value: "Proyecto seguro" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Crear proyecto" }));
  await waitFor(() => expect(fetcher).toHaveBeenCalledTimes(2));
  expect(
    new Headers(fetcher.mock.calls[1][1]?.headers).get("X-CSRF-TOKEN"),
  ).toBe("private-token");
});
it("@s6 una escritura HTTP 401 retira también el borrador privado", async () => {
  vi.spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(Response.json(authenticated))
    .mockResolvedValueOnce(Response.json({}, { status: 401 }))
    .mockResolvedValueOnce(Response.json(anonymous));
  render(<SessionGate />);
  fireEvent.change(await screen.findByLabelText(/Nombre del proyecto/), {
    target: { value: "Borrador privado" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Crear proyecto" }));
  expect(await screen.findByLabelText("Usuario")).toBeVisible();
  expect(
    screen.queryByDisplayValue("Borrador privado"),
  ).not.toBeInTheDocument();
});
it("@s7 cerrar retira el borrador antes de la respuesta y confirma el cierre", async () => {
  let finish!: (response: Response) => void;
  const fetcher = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(Response.json(authenticated))
    .mockImplementationOnce(
      () =>
        new Promise((resolve) => {
          finish = resolve;
        }),
    )
    .mockResolvedValueOnce(Response.json(anonymous));
  render(<SessionGate />);
  fireEvent.change(await screen.findByLabelText(/Nombre del proyecto/), {
    target: { value: "Privado" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Cerrar sesión" }));
  expect(screen.queryByDisplayValue("Privado")).not.toBeInTheDocument();
  expect(screen.getByRole("status")).toHaveTextContent("Cerrando sesión");
  expect(fetcher.mock.calls[1]).toEqual([
    "/api/session/logout",
    expect.objectContaining({
      method: "POST",
      credentials: "same-origin",
      headers: { "X-CSRF-TOKEN": "private-token" },
    }),
  ]);
  await act(async () => finish(new Response(null, { status: 204 })));
  expect(await screen.findByLabelText("Usuario")).toBeVisible();
});
it.each([503, "network"])(
  "@s12 cierre no confirmado %s oculta datos y permite reintento deliberado",
  async (status) => {
    const fetcher = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(Response.json(authenticated));
    if (typeof status === "number")
      fetcher.mockResolvedValueOnce(
        Response.json({ title: "private" }, { status }),
      );
    else fetcher.mockRejectedValueOnce(new Error("private"));
    fetcher
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
      .mockResolvedValueOnce(Response.json(anonymous));
    render(<SessionGate />);
    fireEvent.click(
      await screen.findByRole("button", { name: "Cerrar sesión" }),
    );
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "No podemos confirmar el cierre",
    );
    expect(
      screen.queryByLabelText(/Nombre del proyecto/),
    ).not.toBeInTheDocument();
    expect(fetcher).toHaveBeenCalledTimes(2);
    fireEvent.click(screen.getByRole("button", { name: "Reintentar cierre" }));
    expect(await screen.findByLabelText("Usuario")).toBeVisible();
    expect(fetcher.mock.calls[2][1]?.headers).toEqual({
      "X-CSRF-TOKEN": "private-token",
    });
  },
);
it.each([
  "/proyectos/6c5dbd10-9ad5-4000-8000-000000000001/editar",
  "/proyectos",
])("@s6 lectura privada %s también invalida acceso", async (route) => {
  window.history.replaceState(null, "", route);
  vi.spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(Response.json(authenticated))
    .mockResolvedValueOnce(Response.json({}, { status: 401 }))
    .mockResolvedValueOnce(Response.json(anonymous));
  render(<SessionGate />);
  expect(await screen.findByLabelText("Usuario")).toBeVisible();
});
it("@s14 StrictMode ignora un GET de sesión antiguo después de su cleanup", async () => {
  let finish!: (response: Response) => void;
  const fetcher = vi
    .spyOn(globalThis, "fetch")
    .mockImplementationOnce(
      () =>
        new Promise((resolve) => {
          finish = resolve;
        }),
    )
    .mockResolvedValueOnce(Response.json(authenticated));
  render(
    <StrictMode>
      <SessionGate />
    </StrictMode>,
  );
  expect(await screen.findByLabelText(/Nombre del proyecto/)).toBeVisible();
  expect((fetcher.mock.calls[0][1]?.signal as AbortSignal).aborted).toBe(true);
  await act(async () => finish(Response.json(anonymous)));
  expect(screen.getByLabelText(/Nombre del proyecto/)).toBeVisible();
});

it("@s14 un POST de creación abandonado no retira el acceso de la vista nueva", async () => {
  let finish!: (response: Response) => void;
  const fetcher = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(Response.json(authenticated))
    .mockImplementationOnce(
      () =>
        new Promise((resolve) => {
          finish = resolve;
        }),
    )
    .mockResolvedValueOnce(Response.json({ items: [], nextCursor: null }));
  render(<SessionGate />);
  fireEvent.change(await screen.findByLabelText(/Nombre del proyecto/), {
    target: { value: "Privado" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Crear proyecto" }));
  fireEvent.click(screen.getByRole("link", { name: /Proyectos/ }));
  await screen.findByRole("heading", { name: "Proyectos", level: 1 });
  await act(async () => finish(Response.json({}, { status: 401 })));
  expect(screen.getByRole("button", { name: "Cerrar sesión" })).toBeVisible();
  expect(fetcher).toHaveBeenCalledTimes(3);
});

it("@s14 un login resuelto tras desmontar no consulta ni abre sesión", async () => {
  let finish!: (response: Response) => void;
  const fetcher = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(Response.json(anonymous))
    .mockImplementationOnce(
      () =>
        new Promise((resolve) => {
          finish = resolve;
        }),
    );
  const view = render(<SessionGate />);
  fireEvent.click(
    await screen.findByRole("button", { name: "Iniciar sesión" }),
  );
  view.unmount();
  await act(async () => finish(new Response(null, { status: 204 })));
  expect(fetcher).toHaveBeenCalledTimes(2);
});
it("@s17 CSRF inválido recupera token por decisión y conserva borrador sin repetir POST", async () => {
  const fetcher = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(Response.json(authenticated))
    .mockResolvedValueOnce(
      Response.json({ code: "CSRF_INVALID" }, { status: 403 }),
    )
    .mockResolvedValueOnce(
      Response.json({ ...authenticated, csrfToken: "renewed-token" }),
    )
    .mockResolvedValueOnce(Response.json({}, { status: 503 }));
  render(<SessionGate />);
  fireEvent.change(await screen.findByLabelText(/Nombre del proyecto/), {
    target: { value: "Borrador conservado" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Crear proyecto" }));
  const recover = await screen.findByRole("button", {
    name: "Recuperar acceso",
  });
  expect(fetcher).toHaveBeenCalledTimes(2);
  expect(screen.getByDisplayValue("Borrador conservado")).toBeVisible();
  fireEvent.click(recover);
  await waitFor(() =>
    expect(
      screen.queryByRole("button", { name: "Recuperar acceso" }),
    ).not.toBeInTheDocument(),
  );
  expect(fetcher).toHaveBeenCalledTimes(3);
  expect(screen.getByDisplayValue("Borrador conservado")).toBeVisible();
  fireEvent.click(screen.getByRole("button", { name: "Crear proyecto" }));
  await waitFor(() => expect(fetcher).toHaveBeenCalledTimes(4));
  expect(
    new Headers(fetcher.mock.calls[3][1]?.headers).get("X-CSRF-TOKEN"),
  ).toBe("renewed-token");
});
it.each(["login", "logout"])(
  "@s9 @s17 %s permite renovar CSRF sin repetir la operación",
  async (operation) => {
    const fetcher = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(
        Response.json(operation === "login" ? anonymous : authenticated),
      )
      .mockResolvedValueOnce(
        Response.json({ code: "CSRF_INVALID" }, { status: 403 }),
      )
      .mockResolvedValueOnce(
        Response.json({
          ...(operation === "login" ? anonymous : authenticated),
          csrfToken: "fresh",
        }),
      );
    render(<SessionGate />);
    fireEvent.click(
      await screen.findByRole("button", {
        name: operation === "login" ? "Iniciar sesión" : "Cerrar sesión",
      }),
    );
    fireEvent.click(
      await screen.findByRole("button", { name: "Recuperar acceso" }),
    );
    await waitFor(() =>
      expect(
        screen.queryByRole("button", { name: "Recuperar acceso" }),
      ).not.toBeInTheDocument(),
    );
    expect(fetcher).toHaveBeenCalledTimes(3);
    if (operation === "logout") {
      expect(
        screen.queryByLabelText(/Nombre del proyecto/),
      ).not.toBeInTheDocument();
      expect(
        screen.getByRole("button", { name: "Reintentar cierre" }),
      ).toBeEnabled();
    }
  },
);
it("@s15 el cierre confirmado publica sólo una señal sin secretos", async () => {
  const channel = { onmessage: null, postMessage: vi.fn(), close: vi.fn() };
  vi.stubGlobal(
    "BroadcastChannel",
    vi.fn(function () {
      return channel;
    }),
  );
  vi.spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(Response.json(authenticated))
    .mockResolvedValueOnce(new Response(null, { status: 204 }))
    .mockResolvedValueOnce(Response.json(anonymous));
  const view = render(<SessionGate />);
  fireEvent.click(await screen.findByRole("button", { name: "Cerrar sesión" }));
  await screen.findByLabelText("Usuario");
  expect(channel.postMessage).toHaveBeenCalledExactlyOnceWith("logout");
  view.unmount();
  expect(channel.close).toHaveBeenCalledTimes(1);
});

it("@s15 otra pestaña retira datos antes de comprobar sesión y no acepta señales ajenas", async () => {
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
  let finish!: (response: Response) => void;
  const fetcher = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(Response.json(authenticated))
    .mockImplementationOnce(
      () =>
        new Promise((resolve) => {
          finish = resolve;
        }),
    );
  render(<SessionGate />);
  fireEvent.change(await screen.findByLabelText(/Nombre del proyecto/), {
    target: { value: "Privado" },
  });
  act(() =>
    channel.onmessage?.(
      new MessageEvent("message", { data: { token: "untrusted" } }),
    ),
  );
  expect(fetcher).toHaveBeenCalledTimes(1);
  act(() =>
    channel.onmessage?.(new MessageEvent("message", { data: "logout" })),
  );
  expect(screen.queryByDisplayValue("Privado")).not.toBeInTheDocument();
  await act(async () => finish(Response.json(anonymous)));
  expect(screen.getByLabelText("Usuario")).toBeVisible();
  expect(channel.postMessage).not.toHaveBeenCalled();
});
it("@s15 volver a visible comprueba la sesión y retira la vista caducada", async () => {
  const fetcher = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(Response.json(authenticated))
    .mockResolvedValueOnce(Response.json(anonymous));
  const view = render(<SessionGate />);
  await screen.findByLabelText(/Nombre del proyecto/);
  vi.spyOn(document, "visibilityState", "get").mockReturnValue("hidden");
  fireEvent(document, new Event("visibilitychange"));
  expect(fetcher).toHaveBeenCalledTimes(1);
  vi.spyOn(document, "visibilityState", "get").mockReturnValue("visible");
  fireEvent(document, new Event("visibilitychange"));
  expect(await screen.findByLabelText("Usuario")).toBeVisible();
  view.unmount();
  fireEvent(document, new Event("visibilitychange"));
  expect(fetcher).toHaveBeenCalledTimes(2);
});
it("@s19 entrar y salir dejan el foco en el encabezado del contexto nuevo", async () => {
  vi.spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(Response.json(anonymous))
    .mockResolvedValueOnce(new Response(null, { status: 204 }))
    .mockResolvedValueOnce(Response.json(authenticated))
    .mockResolvedValueOnce(new Response(null, { status: 204 }))
    .mockResolvedValueOnce(Response.json(anonymous));
  render(<SessionGate />);
  const enter = await screen.findByRole("button", { name: "Iniciar sesión" });
  enter.focus();
  fireEvent.click(enter);
  const createHeading = await screen.findByRole("heading", {
    name: /Dale espacio/,
  });
  expect(createHeading).toHaveFocus();
  expect(createHeading).toHaveAttribute("tabindex", "-1");
  const leave = screen.getByRole("button", { name: "Cerrar sesión" });
  leave.focus();
  fireEvent.click(leave);
  await screen.findByLabelText("Usuario");
  expect(screen.getByRole("heading", { name: /Tu espacio/ })).toHaveFocus();
});

it("@s17 recuperar CSRF con red caída conserva el borrador y permite otra comprobación", async () => {
  const fetcher = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(Response.json(authenticated))
    .mockResolvedValueOnce(
      Response.json({ code: "CSRF_INVALID" }, { status: 403 }),
    )
    .mockRejectedValueOnce(new Error("offline"))
    .mockResolvedValueOnce(
      Response.json({ ...authenticated, csrfToken: "new" }),
    );
  render(<SessionGate />);
  fireEvent.change(await screen.findByLabelText(/Nombre del proyecto/), {
    target: { value: "Conservar" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Crear proyecto" }));
  fireEvent.click(
    await screen.findByRole("button", { name: "Recuperar acceso" }),
  );
  await waitFor(() => expect(fetcher).toHaveBeenCalledTimes(3));
  expect(screen.getByDisplayValue("Conservar")).toBeVisible();
  fireEvent.click(screen.getByRole("button", { name: "Recuperar acceso" }));
  await waitFor(() =>
    expect(
      screen.queryByRole("button", { name: "Recuperar acceso" }),
    ).not.toBeInTheDocument(),
  );
  expect(screen.getByDisplayValue("Conservar")).toBeVisible();
});
it("@s1 un reintento tras fallo de comprobación no vuelve a montar datos antiguos", async () => {
  let finish!: (response: Response) => void;
  vi.spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(Response.json(authenticated))
    .mockRejectedValueOnce(new Error("offline"))
    .mockImplementationOnce(
      () =>
        new Promise((resolve) => {
          finish = resolve;
        }),
    );
  render(<SessionGate />);
  await screen.findByLabelText(/Nombre del proyecto/);
  vi.spyOn(document, "visibilityState", "get").mockReturnValue("visible");
  fireEvent(document, new Event("visibilitychange"));
  fireEvent.click(await screen.findByRole("button", { name: "Reintentar" }));
  expect(
    screen.queryByLabelText(/Nombre del proyecto/),
  ).not.toBeInTheDocument();
  await act(async () => finish(Response.json(anonymous)));
  expect(screen.getByLabelText("Usuario")).toBeVisible();
});
it.each([
  "/login?returnTo=https://outside.invalid",
  "/proyectos-maliciosos",
  "/?csrfToken=untrusted",
])("@s16 descarta retorno ajeno al recorrido %s", async (route) => {
  window.history.replaceState(null, "", route);
  vi.spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(Response.json(anonymous))
    .mockResolvedValueOnce(new Response(null, { status: 204 }))
    .mockResolvedValueOnce(Response.json(authenticated));
  render(<SessionGate />);
  fireEvent.click(
    await screen.findByRole("button", { name: "Iniciar sesión" }),
  );
  await screen.findByLabelText(/Nombre del proyecto/);
  expect(window.location.pathname + window.location.search).toBe("/");
});
it("@s14 logout resuelto tras desmontar no publica ni consulta otra sesión", async () => {
  const channel = { onmessage: null, postMessage: vi.fn(), close: vi.fn() };
  vi.stubGlobal(
    "BroadcastChannel",
    vi.fn(function () {
      return channel;
    }),
  );
  let finish!: (response: Response) => void;
  const fetcher = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(Response.json(authenticated))
    .mockImplementationOnce(
      () =>
        new Promise((resolve) => {
          finish = resolve;
        }),
    );
  const view = render(<SessionGate />);
  fireEvent.click(await screen.findByRole("button", { name: "Cerrar sesión" }));
  view.unmount();
  await act(async () => finish(new Response(null, { status: 204 })));
  expect(fetcher).toHaveBeenCalledTimes(2);
  expect(channel.postMessage).not.toHaveBeenCalled();
});
it("@s14 login cancelado por otra pestaña no borra una contraseña escrita después", async () => {
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
  let finish!: (response: Response) => void;
  vi.spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(Response.json(anonymous))
    .mockImplementationOnce(
      () =>
        new Promise((resolve) => {
          finish = resolve;
        }),
    )
    .mockResolvedValueOnce(Response.json(anonymous));
  render(<SessionGate />);
  fireEvent.click(
    await screen.findByRole("button", { name: "Iniciar sesión" }),
  );
  act(() =>
    channel.onmessage?.(new MessageEvent("message", { data: "logout" })),
  );
  expect(await screen.findByLabelText("Contraseña")).toHaveValue("");
  expect(screen.getByRole("button", { name: "Iniciar sesión" })).toBeEnabled();
  fireEvent.change(screen.getByLabelText("Contraseña"), {
    target: { value: "new-draft" },
  });
  await act(async () => finish(new Response(null, { status: 204 })));
  expect(screen.getByLabelText("Contraseña")).toHaveValue("new-draft");
});
it.each(["anonymous", "offline"])(
  "@s6 logout HTTP 401 comprueba la sesión: %s",
  async (outcome) => {
    const fetcher = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(Response.json(authenticated))
      .mockResolvedValueOnce(Response.json({}, { status: 401 }));
    if (outcome === "anonymous")
      fetcher.mockResolvedValueOnce(Response.json(anonymous));
    else fetcher.mockRejectedValueOnce(new Error("offline"));
    render(<SessionGate />);
    fireEvent.click(
      await screen.findByRole("button", { name: "Cerrar sesión" }),
    );
    if (outcome === "anonymous")
      expect(await screen.findByLabelText("Usuario")).toBeVisible();
    else
      expect(
        await screen.findByRole("button", { name: "Reintentar cierre" }),
      ).toBeVisible();
    expect(fetcher).toHaveBeenCalledTimes(3);
    expect(
      screen.queryByLabelText(/Nombre del proyecto/),
    ).not.toBeInTheDocument();
  },
);
it.each(["login", "logout"])(
  "@s14 %s ignora el problema CSRF cuyo cuerpo llega después de revocar",
  async (operation) => {
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
    let finish!: (body: unknown) => void;
    const copy = new Response();
    const parse = vi.spyOn(copy, "json").mockImplementation(
      () =>
        new Promise((resolve) => {
          finish = resolve;
        }),
    );
    const rejected = new Response(null, { status: 403 });
    vi.spyOn(rejected, "clone").mockReturnValue(copy);
    vi.spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(
        Response.json(operation === "login" ? anonymous : authenticated),
      )
      .mockResolvedValueOnce(rejected)
      .mockResolvedValueOnce(Response.json(anonymous));
    render(<SessionGate />);
    fireEvent.click(
      await screen.findByRole("button", {
        name: operation === "login" ? "Iniciar sesión" : "Cerrar sesión",
      }),
    );
    await waitFor(() => expect(parse).toHaveBeenCalled());
    act(() =>
      channel.onmessage?.(new MessageEvent("message", { data: "logout" })),
    );
    await screen.findByLabelText("Usuario");
    await act(async () => finish({ code: "CSRF_INVALID" }));
    expect(
      screen.queryByRole("button", { name: "Recuperar acceso" }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "Reintentar cierre" }),
    ).not.toBeInTheDocument();
  },
);
it("@s14 borra contraseña al recibir respuesta aunque la comprobación posterior siga pendiente", async () => {
  let finish!: (response: Response) => void;
  const fetcher = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(Response.json(anonymous))
    .mockResolvedValueOnce(new Response(null, { status: 204 }))
    .mockImplementationOnce(
      () =>
        new Promise((resolve) => {
          finish = resolve;
        }),
    );
  render(<SessionGate />);
  fireEvent.change(await screen.findByLabelText("Contraseña"), {
    target: { value: "secret" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Iniciar sesión" }));
  await waitFor(() => expect(fetcher).toHaveBeenCalledTimes(3));
  expect(screen.getByLabelText("Contraseña")).toHaveValue("");
  expect(
    screen.queryByLabelText(/Nombre del proyecto/),
  ).not.toBeInTheDocument();
  await act(async () => finish(Response.json(authenticated)));
  expect(screen.getByLabelText(/Nombre del proyecto/)).toBeVisible();
});
it.each([false, true])(
  "@s19 login fallido conserva foco elegido: %s",
  async (moved) => {
    let finish!: (response: Response) => void;
    vi.spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(Response.json(anonymous))
      .mockImplementationOnce(
        () =>
          new Promise((resolve) => {
            finish = resolve;
          }),
      );
    render(<SessionGate />);
    const enter = await screen.findByRole("button", { name: "Iniciar sesión" });
    enter.focus();
    fireEvent.click(enter);
    const username = screen.getByLabelText("Usuario");
    if (moved) username.focus();
    else {
      document.body.tabIndex = -1;
      document.body.focus();
      document.body.removeAttribute("tabindex");
    }
    await act(async () => finish(Response.json({}, { status: 401 })));
    expect(
      moved ? username : screen.getByRole("heading", { name: /Tu espacio/ }),
    ).toHaveFocus();
  },
);
it.each([
  "/proyectos",
  "/proyectos?cursor=opaque%2Btoken",
  "/proyectos/6c5dbd10-9ad5-4000-8000-000000000001",
  "/proyectos/6c5dbd10-9ad5-4000-8000-000000000001/editar",
])("@s16 conserva la ruta propia tras login %s", async (route) => {
  window.history.replaceState(null, "", route);
  const fetcher = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(Response.json(anonymous))
    .mockResolvedValueOnce(new Response(null, { status: 204 }))
    .mockResolvedValueOnce(Response.json(authenticated))
    .mockResolvedValueOnce(Response.json({}, { status: 404 }));
  render(<SessionGate />);
  fireEvent.click(
    await screen.findByRole("button", { name: "Iniciar sesión" }),
  );
  await waitFor(() => expect(fetcher).toHaveBeenCalledTimes(4));
  expect(window.location.pathname + window.location.search).toBe(route);
  expect(screen.getByRole("button", { name: "Cerrar sesión" })).toBeVisible();
});
it.each([
  42,
  [],
  { ...anonymous, authenticated: "false" },
  { ...anonymous, csrfToken: 42 },
  { ...anonymous, csrfToken: null },
  { ...anonymous, csrfHeaderName: undefined },
  { ...authenticated, username: "" },
])("@s1 rechaza tipos de sesión incompatibles %j", async (body) => {
  vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(Response.json(body));
  render(<SessionGate />);
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "No hemos podido comprobar",
  );
  expect(
    screen.queryByLabelText(/Nombre del proyecto/),
  ).not.toBeInTheDocument();
});
it.each([401, 503, 500])(
  "@s1 GET HTTP %i no acepta un cuerpo de sesión autenticada",
  async (status) => {
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
      Response.json(authenticated, { status }),
    );
    render(<SessionGate />);
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "No hemos podido comprobar",
    );
  },
);
it("@s13 login y cierre no escriben secretos en almacenamiento web", async () => {
  const storage = vi.spyOn(Storage.prototype, "setItem");
  vi.spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(Response.json(anonymous))
    .mockResolvedValueOnce(new Response(null, { status: 204 }))
    .mockResolvedValueOnce(Response.json(authenticated))
    .mockResolvedValueOnce(new Response(null, { status: 204 }))
    .mockResolvedValueOnce(Response.json(anonymous));
  render(<SessionGate />);
  fireEvent.change(await screen.findByLabelText("Contraseña"), {
    target: { value: "secret" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Iniciar sesión" }));
  fireEvent.click(await screen.findByRole("button", { name: "Cerrar sesión" }));
  await screen.findByLabelText("Usuario");
  expect(storage).not.toHaveBeenCalled();
  expect(window.location.search).toBe("");
});
it.each([
  { ...authenticated, authenticated: "true" },
  { ...anonymous, authenticated: 0 },
  { ...authenticated, username: 42 },
])("@s1 no convierte tipos escalares de identidad %j", async (body) => {
  vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(Response.json(body));
  render(<SessionGate />);
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "No hemos podido comprobar",
  );
});
it("@s14 acceso inicial vacío y submit nativo cancelado sin errores antiguos", async () => {
  let finish!: (response: Response) => void;
  vi.spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(Response.json(anonymous))
    .mockImplementationOnce(
      () =>
        new Promise((resolve) => {
          finish = resolve;
        }),
    );
  render(<SessionGate />);
  const username = await screen.findByLabelText("Usuario");
  expect(username).toHaveValue("");
  expect(screen.getByLabelText("Contraseña")).toHaveValue("");
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  const event = new Event("submit", { bubbles: true, cancelable: true });
  act(() => username.closest("form")!.dispatchEvent(event));
  expect(event.defaultPrevented).toBe(true);
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  await act(async () => finish(Response.json({}, { status: 401 })));
});
it("@s17 distingue primer aviso CSRF del fallo de recuperación", async () => {
  vi.spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(Response.json(authenticated))
    .mockResolvedValueOnce(
      Response.json({ code: "CSRF_INVALID" }, { status: 403 }),
    )
    .mockRejectedValueOnce(new Error("offline"));
  render(<SessionGate />);
  fireEvent.change(await screen.findByLabelText(/Nombre del proyecto/), {
    target: { value: "Conservar" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Crear proyecto" }));
  const recover = await screen.findByRole("button", {
    name: "Recuperar acceso",
  });
  expect(
    screen.queryByText(/No hemos podido renovar el acceso/),
  ).not.toBeInTheDocument();
  fireEvent.click(recover);
  expect(
    await screen.findByText(/No hemos podido renovar el acceso/),
  ).toBeVisible();
  expect(screen.getByDisplayValue("Conservar")).toBeVisible();
});
it.each([
  "/proyectos?cursor=",
  "/proyectos?cursor=one&cursor=two",
  "/proyectos?cursor=one&extra=value",
  "/proyectos?extra=value",
  "/prefix/proyectos/6c5dbd10-9ad5-4000-8000-000000000001",
  "/proyectos/6c5dbd10-9ad5-4000-8000-000000000001/suffix",
])("@s16 descarta rutas parcialmente válidas %s", async (route) => {
  window.history.replaceState(null, "", route);
  vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
    Response.json(authenticated),
  );
  render(<SessionGate />);
  await screen.findByLabelText(/Nombre del proyecto/);
  expect(window.location.pathname + window.location.search).toBe("/");
});
it("@s14 el rechazo de GET anterior no oculta una sesión vigente", async () => {
  let fail!: (error: Error) => void;
  vi.spyOn(globalThis, "fetch")
    .mockImplementationOnce(
      () =>
        new Promise((_resolve, reject) => {
          fail = reject;
        }),
    )
    .mockResolvedValueOnce(Response.json(authenticated));
  render(
    <StrictMode>
      <SessionGate />
    </StrictMode>,
  );
  await screen.findByLabelText(/Nombre del proyecto/);
  await act(async () => fail(new Error("late")));
  expect(screen.getByLabelText(/Nombre del proyecto/)).toBeVisible();
});
it("@s12 logout401 con GET fallido conserva capacidad de reintentar cierre", async () => {
  const fetcher = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(Response.json(authenticated))
    .mockResolvedValueOnce(Response.json({}, { status: 401 }))
    .mockRejectedValueOnce(new Error("offline"))
    .mockResolvedValueOnce(new Response(null, { status: 204 }))
    .mockResolvedValueOnce(Response.json(anonymous));
  render(<SessionGate />);
  fireEvent.click(await screen.findByRole("button", { name: "Cerrar sesión" }));
  fireEvent.click(
    await screen.findByRole("button", { name: "Reintentar cierre" }),
  );
  expect(await screen.findByLabelText("Usuario")).toBeVisible();
  expect(
    new Headers(fetcher.mock.calls[3][1]?.headers).get("X-CSRF-TOKEN"),
  ).toBe("private-token");
});
it("@s12 logout401 cuya comprobación sigue autenticada no reabre los datos", async () => {
  vi.spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(Response.json(authenticated))
    .mockResolvedValueOnce(Response.json({}, { status: 401 }))
    .mockResolvedValueOnce(Response.json(authenticated));
  render(<SessionGate />);
  fireEvent.click(await screen.findByRole("button", { name: "Cerrar sesión" }));
  expect(
    await screen.findByRole("button", { name: "Reintentar cierre" }),
  ).toBeVisible();
  expect(
    screen.queryByLabelText(/Nombre del proyecto/),
  ).not.toBeInTheDocument();
});
it("@s14 logout antiguo no retira la espera de un segundo cierre", async () => {
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
  let first!: (response: Response) => void;
  let second!: (response: Response) => void;
  vi.spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(Response.json(authenticated))
    .mockImplementationOnce(
      () =>
        new Promise((resolve) => {
          first = resolve;
        }),
    )
    .mockResolvedValueOnce(Response.json(authenticated))
    .mockImplementationOnce(
      () =>
        new Promise((resolve) => {
          second = resolve;
        }),
    )
    .mockResolvedValueOnce(Response.json(anonymous));
  render(<SessionGate />);
  fireEvent.click(await screen.findByRole("button", { name: "Cerrar sesión" }));
  act(() =>
    channel.onmessage?.(new MessageEvent("message", { data: "logout" })),
  );
  fireEvent.click(await screen.findByRole("button", { name: "Cerrar sesión" }));
  await act(async () => first(new Response(null, { status: 204 })));
  expect(screen.getByRole("status")).toHaveTextContent("Cerrando sesión");
  expect(
    screen.queryByLabelText(/Nombre del proyecto/),
  ).not.toBeInTheDocument();
  await act(async () => second(new Response(null, { status: 204 })));
});
it("@s7 un GET posterior al cierre abortado no vuelve a abrir sesión antigua", async () => {
  let first!: (response: Response) => void;
  let second!: (response: Response) => void;
  const fetcher = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(Response.json(authenticated))
    .mockResolvedValueOnce(new Response(null, { status: 204 }))
    .mockImplementationOnce(
      () =>
        new Promise((resolve) => {
          first = resolve;
        }),
    )
    .mockImplementationOnce(
      () =>
        new Promise((resolve) => {
          second = resolve;
        }),
    );
  render(<SessionGate />);
  fireEvent.click(await screen.findByRole("button", { name: "Cerrar sesión" }));
  await waitFor(() => expect(fetcher).toHaveBeenCalledTimes(3));
  vi.spyOn(document, "visibilityState", "get").mockReturnValue("visible");
  fireEvent(document, new Event("visibilitychange"));
  await act(async () => first(Response.json(anonymous)));
  expect(
    screen.queryByLabelText(/Nombre del proyecto/),
  ).not.toBeInTheDocument();
  expect(screen.getByRole("status")).toHaveTextContent("Comprobando acceso");
  await act(async () => second(Response.json(anonymous)));
  expect(screen.getByLabelText("Usuario")).toBeVisible();
});
