import { StrictMode } from "react";
import { SessionGate } from "./session-gate";
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

const project = {
  id: "6c5dbd10-9ad5-4000-8000-000000000001",
  ownerId: "Pablo",
  name: "Zenit Digital",
  description: "",
  status: "active",
  createdAt: "2026-09-05T12:00:00Z",
  updatedAt: "2026-09-05T12:00:00Z",
};
const task = {
  id: "7c5dbd10-9ad5-4000-8000-000000000002",
  projectId: project.id,
  title: "Preparar portada",
  completionCriterion: "La portada se puede revisar",
  estimatedMinutes: 30,
  status: "pending",
  createdAt: "2026-09-06T12:00:00Z",
  updatedAt: "2026-09-06T12:00:00Z",
};
const route = `/proyectos/${project.id}/tareas/${task.id}`;
const api = `/api/v1/projects/${project.id}/tasks/${task.id}`;
const pending = {
  status: "pending",
  completedAt: null,
  updatedAt: task.updatedAt,
};
const etag = `"task:${task.id}:0"`;
function contextFetch(
  override: (
    url: string,
    options?: RequestInit,
  ) => Response | Promise<Response> | undefined,
) {
  return vi
    .spyOn(globalThis, "fetch")
    .mockImplementation(async (input, options) => {
      const url = String(input);
      const result = override(url, options);
      if (result) return result;
      if (url === `/api/v1/projects/${project.id}`)
        return Response.json(project, { headers: { ETag: '"project"' } });
      if (url.endsWith("/parent")) return Response.json({ parent: null });
      if (url.endsWith("/subtasks") || url.endsWith("/history"))
        return Response.json({ items: [], nextCursor: null });
      if (url.endsWith("/status"))
        return Response.json(pending, { headers: { ETag: etag } });
      return Response.json(task);
    });
}
afterEach(() => window.history.replaceState(null, "", "/"));
it("@s1 @s34 consulta revisión antes de habilitar una transición", async () => {
  window.history.replaceState(null, "", route);
  let finish!: (response: Response) => void;
  const fetcher = contextFetch((url) =>
    url === `${api}/status`
      ? new Promise((resolve) => {
          finish = resolve;
        })
      : undefined,
  );
  render(<App />);
  expect(
    await screen.findByText("Consultando estado de la tarea"),
  ).toBeVisible();
  expect(
    screen.queryByRole("button", { name: "Completar tarea" }),
  ).not.toBeInTheDocument();
  await waitFor(() => expect(finish).toBeTypeOf("function"));
  await act(async () =>
    finish(Response.json(pending, { headers: { ETag: etag } })),
  );
  const control = screen.getByRole("region", { name: "Estado de la tarea" });
  expect(
    within(control).getByRole("button", { name: "Completar tarea" }),
  ).toBeEnabled();
  expect(within(control).getByText("Pendiente")).toBeVisible();
  expect(fetcher).toHaveBeenCalledWith(
    `${api}/status`,
    expect.objectContaining({ cache: "no-store", credentials: "same-origin" }),
  );
});
it("@s2 @s21 completa sólo tras confirmación y bloquea el doble envío", async () => {
  window.history.replaceState(null, "", route);
  let finish!: (response: Response) => void;
  const fetcher = contextFetch((url, options) =>
    url === `${api}/status` && options?.method === "PUT"
      ? new Promise((resolve) => {
          finish = resolve;
        })
      : undefined,
  );
  render(<App />);
  const button = await screen.findByRole("button", { name: "Completar tarea" });
  fireEvent.click(button);
  expect(button).toBeDisabled();
  expect(screen.getByText("Cambiando estado de la tarea")).toBeVisible();
  fireEvent.click(button);
  expect(
    fetcher.mock.calls.filter(([, options]) => options?.method === "PUT"),
  ).toHaveLength(1);
  expect(
    screen.queryByText("Estado de tarea actualizado"),
  ).not.toBeInTheDocument();
  await act(async () =>
    finish(
      Response.json(
        {
          status: "completed",
          completedAt: "2026-09-06T14:00:00Z",
          updatedAt: "2026-09-06T14:00:00Z",
        },
        { headers: { ETag: `"task:${task.id}:1"` } },
      ),
    ),
  );
  const control = screen.getByRole("region", { name: "Estado de la tarea" });
  expect(within(control).getByText("Completada")).toBeVisible();
  expect(
    within(control).getByRole("button", { name: "Reabrir tarea" }),
  ).toBeEnabled();
  expect(
    within(control).getByText("Estado de tarea actualizado"),
  ).toBeVisible();
  const put = fetcher.mock.calls.find(
    ([, options]) => options?.method === "PUT",
  )!;
  expect(new Headers(put[1]?.headers).get("If-Match")).toBe(etag);
  expect(JSON.parse(String(put[1]?.body))).toEqual({ status: "completed" });
});
it.each([503, 412])(
  "@s23 exige consultar estado tras PUT %s sin repetir escritura",
  async (status) => {
    window.history.replaceState(null, "", route);
    const fetcher = contextFetch((url, options) =>
      url === `${api}/status` && options?.method === "PUT"
        ? new Response(null, { status })
        : undefined,
    );
    render(<App />);
    fireEvent.click(
      await screen.findByRole("button", { name: "Completar tarea" }),
    );
    const retry = await screen.findByRole("button", {
      name: "Consultar estado vigente",
    });
    expect(
      screen.queryByText("Estado de tarea actualizado"),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "Completar tarea" }),
    ).not.toBeInTheDocument();
    fireEvent.click(retry);
    expect(
      await screen.findByRole("button", { name: "Completar tarea" }),
    ).toBeEnabled();
    expect(
      fetcher.mock.calls.filter(([, options]) => options?.method === "PUT"),
    ).toHaveLength(1);
  },
);
it("@s34 distingue fallo de lectura y permite recuperarlo", async () => {
  window.history.replaceState(null, "", route);
  let reads = 0;
  contextFetch((url) =>
    url === `${api}/status` && ++reads === 1
      ? new Response(null, { status: 503 })
      : undefined,
  );
  render(<App />);
  fireEvent.click(
    await screen.findByRole("button", { name: "Reintentar estado" }),
  );
  expect(
    await screen.findByRole("button", { name: "Completar tarea" }),
  ).toBeEnabled();
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
});
it.each([401, 404])(
  "@s36 retira todos los datos ante estado GET %s",
  async (status) => {
    window.history.replaceState(null, "", route);
    contextFetch((url) =>
      url === `${api}/status` ? new Response(null, { status }) : undefined,
    );
    render(<App />);
    expect(
      await screen.findByRole("button", { name: "Reintentar tarea" }),
    ).toBeVisible();
    expect(
      screen.queryByRole("heading", { name: task.title }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByLabelText("Título de la tarea"),
    ).not.toBeInTheDocument();
  },
);
it.each([401, 404])(
  "@s14 @s24 retira datos y borrador ante PUT %s",
  async (status) => {
    window.history.replaceState(null, "", route);
    contextFetch((url, options) =>
      url === `${api}/status` && options?.method === "PUT"
        ? new Response(null, { status })
        : undefined,
    );
    render(<App />);
    fireEvent.change(await screen.findByLabelText("Título de la tarea"), {
      target: { value: "Borrador privado" },
    });
    fireEvent.click(
      await screen.findByRole("button", { name: "Completar tarea" }),
    );
    expect(
      await screen.findByRole("button", { name: "Reintentar tarea" }),
    ).toBeVisible();
    expect(
      screen.queryByRole("heading", { name: task.title }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByDisplayValue("Borrador privado"),
    ).not.toBeInTheDocument();
  },
);
it("@s24 cancela PUT al navegar y una respuesta 401 tardía no cierra la sesión vigente", async () => {
  window.history.replaceState(null, "", route);
  let finish!: (response: Response) => void;
  let signal: AbortSignal | null | undefined;
  contextFetch((url, options) => {
    if (url === "/api/session")
      return Response.json({
        authenticated: true,
        username: "Pablo",
        csrfToken: "private",
        csrfHeaderName: "X-CSRF-TOKEN",
      });
    if (url === "/api/v1/projects")
      return Response.json({ items: [], nextCursor: null });
    if (url === `${api}/status` && options?.method === "PUT") {
      signal = options.signal;
      return new Promise((resolve) => {
        finish = resolve;
      });
    }
  });
  render(<SessionGate />);
  fireEvent.click(
    await screen.findByRole("button", { name: "Completar tarea" }),
  );
  fireEvent.click(screen.getByRole("link", { name: "Proyectos" }));
  expect(
    await screen.findByRole("heading", { name: "Proyectos" }),
  ).toBeVisible();
  expect(signal?.aborted).toBe(true);
  await act(async () => finish(new Response(null, { status: 401 })));
  expect(screen.getByRole("heading", { name: "Proyectos" })).toBeVisible();
  expect(screen.queryByLabelText("Contraseña")).not.toBeInTheDocument();
});

it("@s10 distingue historial vacío confirmado de la carga", async () => {
  window.history.replaceState(null, "", route);
  let finish!: (response: Response) => void;
  contextFetch((url) =>
    url === `${api}/history`
      ? new Promise((resolve) => {
          finish = resolve;
        })
      : undefined,
  );
  render(<App />);
  expect(
    await screen.findByText("Consultando historial de la tarea"),
  ).toBeVisible();
  expect(
    screen.queryByText("Todavía no hay cambios de estado."),
  ).not.toBeInTheDocument();
  await act(async () => finish(Response.json({ items: [], nextCursor: null })));
  expect(
    within(
      screen.getByRole("region", { name: "Historial de la tarea" }),
    ).getByText("Todavía no hay cambios de estado."),
  ).toBeVisible();
  expect(screen.getByRole("button", { name: "Completar tarea" })).toBeEnabled();
});
it("@s3 @s12 muestra transiciones y permite recorrer páginas sin cambiar el estado", async () => {
  window.history.replaceState(null, "", route);
  const newest = {
    id: "8c5dbd10-9ad5-4000-8000-000000000001",
    fromStatus: "completed",
    toStatus: "pending",
    occurredAt: "2026-09-06T14:00:00Z",
  };
  const older = {
    ...newest,
    id: "8c5dbd10-9ad5-4000-8000-000000000002",
    fromStatus: "pending",
    toStatus: "completed",
    occurredAt: "2026-09-06T13:00:00Z",
  };
  const fetcher = contextFetch((url) =>
    url === `${api}/history`
      ? Response.json({ items: [newest], nextCursor: "opaque+/=" })
      : url === `${api}/history?cursor=opaque%2B%2F%3D`
        ? Response.json({ items: [older], nextCursor: null })
        : undefined,
  );
  render(<App />);
  expect(await screen.findByText("Reabierta")).toBeVisible();
  const region = screen.getByRole("region", { name: "Historial de la tarea" });
  expect(
    within(region).getByRole("list", { name: "Transiciones de la tarea" }),
  ).toBeVisible();
  fireEvent.click(
    within(region).getByRole("button", { name: "Más transiciones antiguas" }),
  );
  expect(await within(region).findByText("Completada")).toBeVisible();
  expect(within(region).queryByText("Reabierta")).not.toBeInTheDocument();
  fireEvent.click(
    within(region).getByRole("button", {
      name: "Volver al historial reciente",
    }),
  );
  expect(await within(region).findByText("Reabierta")).toBeVisible();
  expect(
    fetcher.mock.calls.filter(([, options]) => options?.method === "PUT"),
  ).toHaveLength(0);
});
it("@s25 un PUT confirmado recarga historial y su fallo no revierte ni reenvía", async () => {
  window.history.replaceState(null, "", route);
  let reads = 0;
  const transition = {
    id: "8c5dbd10-9ad5-4000-8000-000000000001",
    fromStatus: "pending",
    toStatus: "completed",
    occurredAt: "2026-09-06T14:00:00Z",
  };
  const fetcher = contextFetch((url, options) => {
    if (url === `${api}/status` && options?.method === "PUT")
      return Response.json(
        {
          status: "completed",
          completedAt: transition.occurredAt,
          updatedAt: transition.occurredAt,
        },
        { headers: { ETag: `"task:${task.id}:1"` } },
      );
    if (url === `${api}/history`) {
      reads++;
      return reads === 1
        ? Response.json({ items: [], nextCursor: null })
        : reads === 2
          ? new Response(null, { status: 503 })
          : Response.json({ items: [transition], nextCursor: null });
    }
  });
  render(<App />);
  await screen.findByText("Todavía no hay cambios de estado.");
  fireEvent.click(screen.getByRole("button", { name: "Completar tarea" }));
  const retry = await screen.findByRole("button", {
    name: "Reintentar historial",
  });
  expect(screen.getByText("Estado de tarea actualizado")).toBeVisible();
  expect(screen.getByRole("button", { name: "Reabrir tarea" })).toBeEnabled();
  fireEvent.click(retry);
  const list = await screen.findByRole("list", {
    name: "Transiciones de la tarea",
  });
  expect(within(list).getByText("Completada")).toBeVisible();
  expect(
    fetcher.mock.calls.filter(([, options]) => options?.method === "PUT"),
  ).toHaveLength(1);
});
it.each([401, 404])(
  "@s14 @s24 historial %s retira datos y su reintento no muestra snapshot antiguo",
  async (status) => {
    window.history.replaceState(null, "", route);
    let reads = 0;
    let finish!: (response: Response) => void;
    contextFetch((url) =>
      url === `${api}/history` && ++reads === 1
        ? new Response(null, { status })
        : url === api && reads
          ? new Promise((resolve) => {
              finish = resolve;
            })
          : undefined,
    );
    render(<App />);
    fireEvent.click(
      await screen.findByRole("button", { name: "Reintentar tarea" }),
    );
    expect(screen.getByText("Cargando tarea")).toBeVisible();
    expect(
      screen.queryByRole("heading", { name: task.title }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole("region", { name: "Estado de la tarea" }),
    ).not.toBeInTheDocument();
    await waitFor(() => expect(finish).toBeTypeOf("function"));
    await act(async () => finish(Response.json(task)));
    expect(
      await screen.findByRole("button", { name: "Completar tarea" }),
    ).toBeEnabled();
  },
);
it("@s3 al reabrir usa la nueva revisión y retira la fecha actual de finalización", async () => {
  window.history.replaceState(null, "", route);
  const completedAt = "2026-09-06T14:00:00.123456Z";
  const currentTag = `"task:${task.id}:1"`;
  const fetcher = contextFetch((url, options) =>
    url === `${api}/status`
      ? options?.method === "PUT"
        ? Response.json(
            { ...pending, updatedAt: "2026-09-06T15:00:00Z" },
            { headers: { ETag: `"task:${task.id}:2"` } },
          )
        : Response.json(
            { status: "completed", completedAt, updatedAt: completedAt },
            { headers: { ETag: currentTag } },
          )
      : undefined,
  );
  render(<App />);
  const region = await screen.findByRole("region", {
    name: "Estado de la tarea",
  });
  await within(region).findByRole("button", { name: "Reabrir tarea" });
  expect(region.querySelector("time")).toHaveAttribute("datetime", completedAt);
  expect(region.querySelector("time")?.parentElement).toHaveTextContent(
    /^Finalizada el /,
  );
  fireEvent.click(
    within(region).getByRole("button", { name: "Reabrir tarea" }),
  );
  expect(
    await within(region).findByRole("button", { name: "Completar tarea" }),
  ).toBeEnabled();
  expect(region.querySelector("time")).not.toBeInTheDocument();
  const put = fetcher.mock.calls.find(
    ([, options]) => options?.method === "PUT",
  )!;
  expect(new Headers(put[1]?.headers).get("If-Match")).toBe(currentTag);
  expect(JSON.parse(String(put[1]?.body))).toEqual({ status: "pending" });
});
it("@s26 conserva un destino de foco local al paginar historial", async () => {
  window.history.replaceState(null, "", route);
  contextFetch((url) =>
    url === `${api}/history`
      ? Response.json({ items: [], nextCursor: "page" })
      : url.includes("/history?cursor=")
        ? Response.json({ items: [], nextCursor: null })
        : undefined,
  );
  render(<App />);
  const next = await screen.findByRole("button", {
    name: "Más transiciones antiguas",
  });
  next.focus();
  fireEvent.click(next);
  const heading = screen.getByRole("heading", {
    name: "Historial de la tarea",
  });
  await waitFor(() => expect(heading).toHaveFocus());
  expect(heading).toHaveAttribute("tabindex", "-1");
});
it("@s26 recuperar estado restaura foco local sin robar el control elegido", async () => {
  window.history.replaceState(null, "", route);
  let reads = 0;
  contextFetch((url) =>
    url === `${api}/status` && ++reads === 1
      ? new Response(null, { status: 503 })
      : undefined,
  );
  render(<App />);
  const retry = await screen.findByRole("button", {
    name: "Reintentar estado",
  });
  retry.focus();
  fireEvent.click(retry);
  const heading = screen.getByRole("heading", { name: "Estado de la tarea" });
  await waitFor(() => expect(heading).toHaveFocus());
  expect(heading).toHaveAttribute("tabindex", "-1");
});
it("@s9 la lista muestra el estado confirmado de cada tarea", async () => {
  window.history.replaceState(null, "", `/proyectos/${project.id}`);
  contextFetch((url) =>
    url === `/api/v1/projects/${project.id}/tasks`
      ? Response.json({
          items: [{ ...task, status: "completed" }],
          nextCursor: null,
        })
      : undefined,
  );
  render(<App />);
  const list = await screen.findByRole("list", { name: "Tareas guardadas" });
  expect(within(list).getByText("Completada")).toBeVisible();
  expect(within(list).queryByText("Pendiente")).not.toBeInTheDocument();
});
it("@s22 muestra fechas de estado e historial con zona UTC explícita", async () => {
  window.history.replaceState(null, "", route);
  const at = "2026-09-06T14:23:00Z";
  contextFetch((url) =>
    url === `${api}/status`
      ? Response.json(
          { status: "completed", completedAt: at, updatedAt: at },
          { headers: { ETag: etag } },
        )
      : url === `${api}/history`
        ? Response.json({
            items: [
              {
                id: "8c5dbd10-9ad5-4000-8000-000000000001",
                fromStatus: "pending",
                toStatus: "completed",
                occurredAt: at,
              },
            ],
            nextCursor: null,
          })
        : undefined,
  );
  render(<App />);
  await screen.findByRole("button", { name: "Reabrir tarea" });
  for (const name of ["Estado de la tarea", "Historial de la tarea"]) {
    const time = screen.getByRole("region", { name }).querySelector("time")!;
    expect(time).toHaveAttribute("datetime", at);
    expect(time).toHaveTextContent("14:23");
    expect(time).toHaveTextContent(" UTC");
  }
});
it("@s35 una lectura abortada del montaje StrictMode no sustituye el PUT confirmado", async () => {
  window.history.replaceState(null, "", route);
  let reads = 0;
  let finish!: (response: Response) => void;
  let oldSignal: AbortSignal | null | undefined;
  contextFetch((url, options) => {
    if (url !== `${api}/status`) return;
    if (options?.method === "PUT")
      return Response.json(
        {
          status: "completed",
          completedAt: "2026-09-06T14:00:00Z",
          updatedAt: "2026-09-06T14:00:00Z",
        },
        { headers: { ETag: `"task:${task.id}:1"` } },
      );
    if (reads++ === 0) {
      oldSignal = options?.signal;
      return new Promise((resolve) => {
        finish = resolve;
      });
    }
  });
  render(
    <StrictMode>
      <App />
    </StrictMode>,
  );
  fireEvent.click(
    await screen.findByRole("button", { name: "Completar tarea" }),
  );
  await screen.findByText("Estado de tarea actualizado");
  expect(oldSignal?.aborted).toBe(true);
  await act(async () =>
    finish(Response.json(pending, { headers: { ETag: etag } })),
  );
  expect(screen.getByRole("button", { name: "Reabrir tarea" })).toBeEnabled();
  expect(
    within(
      screen.getByRole("region", { name: "Estado de la tarea" }),
    ).queryByText("Pendiente"),
  ).not.toBeInTheDocument();
});

it.each(["status", "history"])(
  "@s24 cancela GET %s al navegar y descarta su 401 tardío",
  async (endpoint) => {
    window.history.replaceState(null, "", route);
    let finish!: (response: Response) => void;
    let signal: AbortSignal | null | undefined;
    contextFetch((url, options) => {
      if (url === "/api/session")
        return Response.json({
          authenticated: true,
          username: "Pablo",
          csrfToken: "private",
          csrfHeaderName: "X-CSRF-TOKEN",
        });
      if (url === "/api/v1/projects")
        return Response.json({ items: [], nextCursor: null });
      if (url === `${api}/${endpoint}`) {
        signal = options?.signal;
        return new Promise((resolve) => {
          finish = resolve;
        });
      }
    });
    render(<SessionGate />);
    await screen.findByRole("heading", { name: task.title });
    await waitFor(() => expect(finish).toBeTypeOf("function"));
    fireEvent.click(screen.getByRole("link", { name: "Proyectos" }));
    await screen.findByRole("heading", { name: "Proyectos" });
    expect(signal?.aborted).toBe(true);
    await act(async () => finish(new Response(null, { status: 401 })));
    expect(screen.getByRole("heading", { name: "Proyectos" })).toBeVisible();
    expect(screen.queryByLabelText("Contraseña")).not.toBeInTheDocument();
  },
);
it.each(["missing-etag", "wrong-etag", "bad-body", "network"])(
  "@s23 respuesta incierta %s conserva recuperación deliberada",
  async (mode) => {
    window.history.replaceState(null, "", route);
    const fetcher = contextFetch((url, options) => {
      if (url !== `${api}/status` || options?.method !== "PUT") return;
      if (mode === "network") return Promise.reject(new TypeError("Offline"));
      const body = mode === "bad-body" ? { status: "inventado" } : pending;
      return Response.json(body, {
        headers:
          mode === "missing-etag"
            ? {}
            : {
                ETag:
                  mode === "wrong-etag"
                    ? '"task:7c5dbd10-9ad5-4000-8000-000000000003:1"'
                    : etag,
              },
      });
    });
    render(<App />);
    fireEvent.click(
      await screen.findByRole("button", { name: "Completar tarea" }),
    );
    expect(
      await screen.findByRole("button", { name: "Consultar estado vigente" }),
    ).toBeEnabled();
    expect(
      screen.queryByText("Estado de tarea actualizado"),
    ).not.toBeInTheDocument();
    expect(
      fetcher.mock.calls.filter(([, options]) => options?.method === "PUT"),
    ).toHaveLength(1);
  },
);
it.each(
  ["status", "history", "write"].flatMap((request) =>
    ["navigate", "logout"].map((action) => ({ request, action })),
  ),
)(
  "@s24 $request pendiente durante $action no repuebla ni invalida la nueva vista",
  async ({ request, action }) => {
    window.history.replaceState(null, "", route);
    const next = {
      ...task,
      id: "7c5dbd10-9ad5-4000-8000-000000000003",
      title: "Segunda tarea",
    };
    let finish!: (response: Response) => void;
    let oldSignal: AbortSignal | null | undefined;
    let signedIn = true;
    contextFetch((url, options) => {
      if (url === "/api/session")
        return Response.json({
          authenticated: signedIn,
          username: signedIn ? "Pablo" : null,
          csrfToken: "token",
          csrfHeaderName: "X-CSRF-TOKEN",
        });
      if (url === "/api/session/logout") {
        signedIn = false;
        return new Response(null, { status: 204 });
      }
      if (
        url === `${api}/${request === "write" ? "status" : request}` &&
        (request !== "write" || options?.method === "PUT")
      ) {
        oldSignal = options?.signal;
        return new Promise((resolve) => {
          finish = resolve;
        });
      }
      if (url === `/api/v1/projects/${project.id}/tasks/${next.id}`)
        return Response.json(next);
      if (url === `/api/v1/projects/${project.id}/tasks/${next.id}/status`)
        return Response.json(pending, {
          headers: { ETag: `"task:${next.id}:0"` },
        });
    });
    render(<SessionGate />);
    await screen.findByRole("heading", { name: task.title });
    if (request === "write")
      fireEvent.click(
        await screen.findByRole("button", { name: "Completar tarea" }),
      );
    await waitFor(() => expect(finish).toBeTypeOf("function"));
    if (action === "logout") {
      fireEvent.click(screen.getByRole("button", { name: "Cerrar sesión" }));
      await screen.findByLabelText("Contraseña");
    } else {
      act(() => {
        window.history.pushState(
          null,
          "",
          `/proyectos/${project.id}/tareas/${next.id}`,
        );
        window.dispatchEvent(new PopStateEvent("popstate"));
      });
      await screen.findByRole("heading", { name: next.title });
    }
    expect(oldSignal?.aborted).toBe(true);
    await act(async () => finish(new Response(null, { status: 401 })));
    expect(
      screen.queryByRole("heading", { name: task.title }),
    ).not.toBeInTheDocument();
    if (action === "logout")
      expect(screen.getByLabelText("Contraseña")).toBeVisible();
    else
      expect(screen.getByRole("heading", { name: next.title })).toBeVisible();
  },
);

it.each(["invalid-body", "missing-etag", "wrong-etag"])(
  "@s34 GET %s no habilita escritura",
  async (mode) => {
    window.history.replaceState(null, "", route);
    contextFetch((url) =>
      url === `${api}/status`
        ? Response.json(
            mode === "invalid-body" ? { status: "pending" } : pending,
            {
              headers:
                mode === "missing-etag"
                  ? {}
                  : {
                      ETag:
                        mode === "wrong-etag"
                          ? '"task:7c5dbd10-9ad5-4000-8000-000000000003:0"'
                          : etag,
                    },
            },
          )
        : undefined,
    );
    render(<App />);
    expect(
      await screen.findByRole("button", { name: "Reintentar estado" }),
    ).toBeEnabled();
    expect(
      screen.queryByRole("button", { name: "Completar tarea" }),
    ).not.toBeInTheDocument();
  },
);
it("@s25 un historial antiguo rechazado no oculta el nuevo historial confirmado", async () => {
  window.history.replaceState(null, "", route);
  let reads = 0;
  let reject!: (reason: unknown) => void;
  contextFetch((url, options) => {
    if (url === `${api}/history` && reads++ === 0)
      return new Promise((_resolve, rejecter) => {
        reject = rejecter;
      });
    if (url === `${api}/status` && options?.method === "PUT")
      return Response.json(
        {
          status: "completed",
          completedAt: "2026-09-06T14:00:00Z",
          updatedAt: "2026-09-06T14:00:00Z",
        },
        { headers: { ETag: `"task:${task.id}:1"` } },
      );
  });
  render(<App />);
  fireEvent.click(
    await screen.findByRole("button", { name: "Completar tarea" }),
  );
  await screen.findByText("Estado de tarea actualizado");
  await screen.findByText("Todavía no hay cambios de estado.");
  await act(async () => reject(new TypeError("Old failure")));
  expect(
    screen.queryByRole("button", { name: "Reintentar historial" }),
  ).not.toBeInTheDocument();
  expect(screen.getByRole("button", { name: "Reabrir tarea" })).toBeEnabled();
});
it("@s8 completar sigue disponible aunque el proyecto esté terminado", async () => {
  window.history.replaceState(null, "", route);
  contextFetch((url, options) =>
    url === `/api/v1/projects/${project.id}`
      ? Response.json(
          { ...project, status: "completed" },
          { headers: { ETag: '"project"' } },
        )
      : url === `${api}/status` && options?.method === "PUT"
        ? Response.json(
            {
              status: "completed",
              completedAt: "2026-09-06T14:00:00Z",
              updatedAt: "2026-09-06T14:00:00Z",
            },
            { headers: { ETag: `"task:${task.id}:1"` } },
          )
        : undefined,
  );
  render(<App />);
  await screen.findByRole("button", { name: "Reabrir en pausa" });
  expect(screen.queryByLabelText("Título de la tarea")).not.toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", { name: "Completar tarea" }));
  expect(
    await screen.findByRole("button", { name: "Reabrir tarea" }),
  ).toBeEnabled();
  expect(
    screen.getByRole("button", { name: "Reabrir en pausa" }),
  ).toBeEnabled();
});
it("@s27 crear un hijo no reabre al padre completado", async () => {
  window.history.replaceState(null, "", route);
  const child = {
    ...task,
    id: "7c5dbd10-9ad5-4000-8000-000000000003",
    title: "Hijo pendiente",
  };
  const fetcher = contextFetch((url, options) => {
    if (url === `${api}/status`)
      return Response.json(
        {
          status: "completed",
          completedAt: task.updatedAt,
          updatedAt: task.updatedAt,
        },
        { headers: { ETag: etag } },
      );
    if (url === `${api}/subtasks` && options?.method === "POST")
      return Response.json(child, { status: 201 });
  });
  render(<App />);
  fireEvent.change(await screen.findByLabelText("Título de la tarea"), {
    target: { value: child.title },
  });
  fireEvent.change(screen.getByLabelText("Criterio de finalización"), {
    target: { value: child.completionCriterion },
  });
  fireEvent.change(screen.getByLabelText("Estimación en minutos"), {
    target: { value: "30" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Crear subtarea" }));
  await screen.findByText("Subtarea guardada");
  expect(screen.getByRole("button", { name: "Reabrir tarea" })).toBeEnabled();
  expect(
    within(
      screen.getByRole("region", { name: "Estado de la tarea" }),
    ).getByText("Completada"),
  ).toBeVisible();
  expect(
    fetcher.mock.calls.filter(([, options]) => options?.method === "PUT"),
  ).toHaveLength(0);
});
it("@s12 @s18 volver al historial reciente recupera un error de página antigua", async () => {
  window.history.replaceState(null, "", route);
  contextFetch((url) =>
    url === `${api}/history`
      ? Response.json({ items: [], nextCursor: "old" })
      : url.includes("/history?cursor=")
        ? new Response(null, { status: 400 })
        : undefined,
  );
  render(<App />);
  fireEvent.click(
    await screen.findByRole("button", { name: "Más transiciones antiguas" }),
  );
  await screen.findByRole("button", { name: "Reintentar historial" });
  fireEvent.click(
    screen.getByRole("button", { name: "Volver al historial reciente" }),
  );
  expect(
    await screen.findByRole("button", { name: "Más transiciones antiguas" }),
  ).toBeEnabled();
  expect(
    screen.queryByRole("button", { name: "Reintentar historial" }),
  ).not.toBeInTheDocument();
});
it("@s15 recuperar CSRF conserva borrador y exige una nueva decisión sin reenvío", async () => {
  window.history.replaceState(null, "", route);
  let sessions = 0;
  const fetcher = contextFetch((url, options) => {
    if (url === "/api/session")
      return Response.json({
        authenticated: true,
        username: "Pablo",
        csrfToken: sessions++ ? "fresh" : "initial",
        csrfHeaderName: "X-CSRF-TOKEN",
      });
    if (url === `${api}/status` && options?.method === "PUT")
      return Response.json({ code: "CSRF_INVALID" }, { status: 403 });
  });
  render(<SessionGate />);
  fireEvent.change(await screen.findByLabelText("Título de la tarea"), {
    target: { value: "Borrador conservado" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Completar tarea" }));
  fireEvent.click(
    await screen.findByRole("button", { name: "Recuperar acceso" }),
  );
  await waitFor(() =>
    expect(
      screen.queryByRole("button", { name: "Recuperar acceso" }),
    ).not.toBeInTheDocument(),
  );
  expect(screen.getByLabelText("Título de la tarea")).toHaveValue(
    "Borrador conservado",
  );
  fireEvent.click(
    screen.getByRole("button", { name: "Consultar estado vigente" }),
  );
  expect(
    await screen.findByRole("button", { name: "Completar tarea" }),
  ).toBeEnabled();
  const writes = fetcher.mock.calls.filter(
    ([, options]) => options?.method === "PUT",
  );
  expect(writes).toHaveLength(1);
  expect(new Headers(writes[0][1]?.headers).get("X-CSRF-TOKEN")).toBe(
    "initial",
  );
});
it.each(["status", "history"])(
  "@s26 la respuesta de %s no roba foco elegido durante la espera",
  async (endpoint) => {
    window.history.replaceState(null, "", route);
    let reads = 0;
    let finish!: (response: Response) => void;
    contextFetch((url) => {
      if (url !== `${api}/${endpoint}`) return;
      if (reads++ === 0) return new Response(null, { status: 503 });
      return new Promise((resolve) => {
        finish = resolve;
      });
    });
    render(<App />);
    const retry = await screen.findByRole("button", {
      name:
        endpoint === "status" ? "Reintentar estado" : "Reintentar historial",
    });
    retry.focus();
    fireEvent.click(retry);
    const chosen = screen.getByRole("link", { name: "Volver al proyecto" });
    chosen.focus();
    await waitFor(() => expect(finish).toBeTypeOf("function"));
    await act(async () =>
      finish(
        endpoint === "status"
          ? Response.json(pending, { headers: { ETag: etag } })
          : Response.json({ items: [], nextCursor: null }),
      ),
    );
    expect(chosen).toHaveFocus();
  },
);
it("@s23 consultar tras conflicto actualiza también el historial antes vacío", async () => {
  window.history.replaceState(null, "", route);
  let statusReads = 0;
  let historyReads = 0;
  const transition = {
    id: "8c5dbd10-9ad5-4000-8000-000000000001",
    fromStatus: "pending",
    toStatus: "completed",
    occurredAt: "2026-09-06T14:00:00Z",
  };
  const fetcher = contextFetch((url, options) => {
    if (url === `${api}/status`) {
      if (options?.method === "PUT") return new Response(null, { status: 412 });
      return statusReads++ === 0
        ? Response.json(pending, { headers: { ETag: etag } })
        : Response.json(
            {
              status: "completed",
              completedAt: transition.occurredAt,
              updatedAt: transition.occurredAt,
            },
            { headers: { ETag: `"task:${task.id}:1"` } },
          );
    }
    if (url === `${api}/history`)
      return Response.json({
        items: historyReads++ === 0 ? [] : [transition],
        nextCursor: null,
      });
  });
  render(<App />);
  await screen.findByText("Todavía no hay cambios de estado.");
  expect(historyReads).toBe(1);
  fireEvent.click(screen.getByRole("button", { name: "Completar tarea" }));
  fireEvent.click(
    await screen.findByRole("button", { name: "Consultar estado vigente" }),
  );
  expect(
    await screen.findByRole("button", { name: "Reabrir tarea" }),
  ).toBeEnabled();
  const list = await screen.findByRole("list", {
    name: "Transiciones de la tarea",
  });
  expect(within(list).getByText("Completada")).toBeVisible();
  expect(
    fetcher.mock.calls.filter(([, options]) => options?.method === "PUT"),
  ).toHaveLength(1);
});
it("no anuncia éxito antes de una escritura confirmada", async () => {
  window.history.replaceState(null, "", route);
  contextFetch(() => undefined);
  render(<App />);
  await screen.findByRole("button", { name: "Completar tarea" });
  expect(
    screen.queryByText("Estado de tarea actualizado"),
  ).not.toBeInTheDocument();
});
it("consultar estado incierto retira la revisión vieja durante el GET", async () => {
  window.history.replaceState(null, "", route);
  let reads = 0;
  let finish!: (response: Response) => void;
  contextFetch((url, options) => {
    if (url !== `${api}/status`) return;
    if (options?.method === "PUT") return new Response(null, { status: 412 });
    if (reads++ > 0)
      return new Promise((resolve) => {
        finish = resolve;
      });
  });
  render(<App />);
  fireEvent.click(
    await screen.findByRole("button", { name: "Completar tarea" }),
  );
  fireEvent.click(
    await screen.findByRole("button", { name: "Consultar estado vigente" }),
  );
  expect(screen.getByText("Consultando estado de la tarea")).toBeVisible();
  expect(
    screen.queryByRole("button", { name: "Completar tarea" }),
  ).not.toBeInTheDocument();
  await act(async () =>
    finish(Response.json(pending, { headers: { ETag: etag } })),
  );
});
it("historial permite un segundo reintento después de dos fallos", async () => {
  window.history.replaceState(null, "", route);
  let reads = 0;
  contextFetch((url) =>
    url === `${api}/history` && reads++ < 2
      ? new Response(null, { status: 503 })
      : undefined,
  );
  render(<App />);
  const retry = await screen.findByRole("button", {
    name: "Reintentar historial",
  });
  retry.focus();
  fireEvent.click(retry);
  await waitFor(() =>
    expect(
      screen.getByRole("heading", { name: "Historial de la tarea" }),
    ).toHaveFocus(),
  );
  fireEvent.click(
    await screen.findByRole("button", { name: "Reintentar historial" }),
  );
  expect(
    await screen.findByText("Todavía no hay cambios de estado."),
  ).toBeVisible();
  expect(reads).toBe(3);
});
it("cada página muestra carga sin conservar entradas ni botones antiguos", async () => {
  window.history.replaceState(null, "", route);
  let finish!: (response: Response) => void;
  let firstReads = 0;
  const row = {
    id: "8c5dbd10-9ad5-4000-8000-000000000001",
    fromStatus: "pending",
    toStatus: "completed",
    occurredAt: task.updatedAt,
  };
  contextFetch((url) => {
    if (url === `${api}/history` && firstReads++ === 0)
      return Response.json({ items: [row], nextCursor: "older" });
    if (url.startsWith(`${api}/history`))
      return new Promise((resolve) => {
        finish = resolve;
      });
  });
  render(<App />);
  const next = await screen.findByRole("button", {
    name: "Más transiciones antiguas",
  });
  expect(
    screen.queryByRole("button", { name: "Volver al historial reciente" }),
  ).not.toBeInTheDocument();
  fireEvent.click(next);
  expect(screen.getByText("Consultando historial de la tarea")).toBeVisible();
  expect(
    screen.queryByRole("list", { name: "Transiciones de la tarea" }),
  ).not.toBeInTheDocument();
  await act(async () =>
    finish(Response.json({ items: [row], nextCursor: null })),
  );
  fireEvent.click(
    screen.getByRole("button", { name: "Volver al historial reciente" }),
  );
  expect(screen.getByText("Consultando historial de la tarea")).toBeVisible();
  expect(
    screen.queryByRole("list", { name: "Transiciones de la tarea" }),
  ).not.toBeInTheDocument();
  await act(async () => finish(Response.json({ items: [], nextCursor: null })));
});
it.each(["success", "failure"])(
  "historial antiguo de StrictMode %s no sustituye página confirmada",
  async (outcome) => {
    window.history.replaceState(null, "", route);
    let reads = 0;
    let finish!: (response: Response) => void;
    contextFetch((url) =>
      url === `${api}/history` && reads++ === 0
        ? new Promise((resolve) => {
            finish = resolve;
          })
        : undefined,
    );
    render(
      <StrictMode>
        <App />
      </StrictMode>,
    );
    await screen.findByText("Todavía no hay cambios de estado.");
    await act(async () =>
      finish(
        outcome === "failure"
          ? new Response(null, { status: 404 })
          : Response.json({
              items: [
                {
                  id: "8c5dbd10-9ad5-4000-8000-000000000001",
                  fromStatus: "pending",
                  toStatus: "completed",
                  occurredAt: task.updatedAt,
                },
              ],
              nextCursor: null,
            }),
      ),
    );
    expect(screen.getByText("Todavía no hay cambios de estado.")).toBeVisible();
    expect(
      screen.queryByRole("button", { name: "Reintentar tarea" }),
    ).not.toBeInTheDocument();
  },
);
it.each(["PUT", "consulta"])(
  "dos confirmaciones por %s refrescan dos veces el historial",
  async (source) => {
    window.history.replaceState(null, "", route);
    let completed = false;
    let historyReads = 0;
    let version = 0;
    contextFetch((url, options) => {
      if (url === `${api}/status`) {
        if (options?.method === "PUT") {
          completed = !completed;
          version++;
          if (source === "consulta") return new Response(null, { status: 412 });
        }
        return Response.json(
          {
            status: completed ? "completed" : "pending",
            completedAt: completed ? task.updatedAt : null,
            updatedAt: task.updatedAt,
          },
          { headers: { ETag: `"task:${task.id}:${version}"` } },
        );
      }
      if (url === `${api}/history`) {
        historyReads++;
        return Response.json({ items: [], nextCursor: null });
      }
    });
    render(<App />);
    await screen.findByText("Todavía no hay cambios de estado.");
    for (let i = 0; i < 2; i++) {
      fireEvent.click(
        screen.getByRole("button", {
          name: i ? "Reabrir tarea" : "Completar tarea",
        }),
      );
      if (source === "consulta")
        fireEvent.click(
          await screen.findByRole("button", {
            name: "Consultar estado vigente",
          }),
        );
      await waitFor(() => expect(historyReads).toBe(i + 2));
      await screen.findByRole("button", {
        name: i ? "Completar tarea" : "Reabrir tarea",
      });
    }
  },
);
it("un 404 de status antiguo no deniega la confirmación StrictMode vigente", async () => {
  window.history.replaceState(null, "", route);
  let reads = 0;
  let finish!: (response: Response) => void;
  contextFetch((url) =>
    url === `${api}/status` && reads++ === 0
      ? new Promise((resolve) => {
          finish = resolve;
        })
      : undefined,
  );
  render(
    <StrictMode>
      <App />
    </StrictMode>,
  );
  await screen.findByRole("button", { name: "Completar tarea" });
  await act(async () => finish(new Response(null, { status: 404 })));
  expect(screen.getByRole("button", { name: "Completar tarea" })).toBeEnabled();
  expect(
    screen.queryByRole("button", { name: "Reintentar tarea" }),
  ).not.toBeInTheDocument();
});
it("un PUT abandonado por denegación del historial no deniega el detalle recuperado", async () => {
  window.history.replaceState(null, "", route);
  let histories = 0;
  let finishHistory!: (response: Response) => void;
  let finishWrite!: (response: Response) => void;
  contextFetch((url, options) => {
    if (url === `${api}/history` && histories++ === 0)
      return new Promise((resolve) => {
        finishHistory = resolve;
      });
    if (url === `${api}/status` && options?.method === "PUT")
      return new Promise((resolve) => {
        finishWrite = resolve;
      });
  });
  render(<App />);
  fireEvent.click(
    await screen.findByRole("button", { name: "Completar tarea" }),
  );
  await act(async () => finishHistory(new Response(null, { status: 404 })));
  fireEvent.click(screen.getByRole("button", { name: "Reintentar tarea" }));
  await screen.findByRole("button", { name: "Completar tarea" });
  await act(async () => finishWrite(new Response(null, { status: 404 })));
  expect(screen.getByRole("button", { name: "Completar tarea" })).toBeEnabled();
  expect(
    screen.queryByRole("button", { name: "Reintentar tarea" }),
  ).not.toBeInTheDocument();
});
it("restaura foco local tras el desenfoque nativo del botón deshabilitado", async () => {
  window.history.replaceState(null, "", route);
  let finish!: (response: Response) => void;
  contextFetch((url, options) =>
    url === `${api}/status` && options?.method === "PUT"
      ? new Promise((resolve) => {
          finish = resolve;
        })
      : undefined,
  );
  render(<App />);
  const button = await screen.findByRole("button", { name: "Completar tarea" });
  button.focus();
  fireEvent.click(button);
  // JSDOM conserva el foco de disabled; reproducimos el desenfoque nativo del navegador.
  document.body.tabIndex = -1;
  document.body.focus();
  document.body.removeAttribute("tabindex");
  await act(async () =>
    finish(
      Response.json(
        {
          status: "completed",
          completedAt: task.updatedAt,
          updatedAt: task.updatedAt,
        },
        { headers: { ETag: `"task:${task.id}:1"` } },
      ),
    ),
  );
  expect(
    screen.getByRole("heading", { name: "Estado de la tarea" }),
  ).toHaveFocus();
});
