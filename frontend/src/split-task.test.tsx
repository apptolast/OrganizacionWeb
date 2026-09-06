import {
  render,
  screen,
  fireEvent,
  waitFor,
  within,
  act,
} from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { App } from "./App";
import { StrictMode } from "react";
import { SessionGate } from "./session-gate";
const anonymous = {
  authenticated: false,
  username: null,
  csrfToken: "anonymous",
  csrfHeaderName: "X-CSRF-TOKEN",
};
const authenticated = {
  ...anonymous,
  authenticated: true,
  username: "Pablo",
  csrfToken: "private",
};
const project = {
  id: "6c5dbd10-9ad5-4000-8000-000000000001",
  ownerId: "owner",
  name: "Zenit Digital",
  description: "Una idea",
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
function detailFetch(...responses: Array<Response | Promise<Response>>) {
  return vi.spyOn(globalThis, "fetch").mockImplementation(async (url) => {
    const stateResponse = taskStateRead(String(url));
    if (stateResponse) return stateResponse;
    if (url === `/api/v1/projects/${project.id}`)
      return Response.json(project, { headers: { ETag: '"version"' } });
    if (String(url).endsWith("/subtasks"))
      return Response.json({ items: [], nextCursor: null });
    const response = responses.shift();
    if (!response) throw new Error(`Petición inesperada: ${url}`);
    return response;
  });
}
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
      const stateResponse = taskStateRead(url);
      if (stateResponse) return stateResponse;
      const custom = override(url, options);
      if (custom) return custom;
      if (url === "/api/session") return Response.json(authenticated);
      if (url === `/api/v1/projects/${project.id}`)
        return Response.json(project, { headers: { ETag: '"version"' } });
      if (url.endsWith("/parent")) return Response.json({ parent: null });
      if (url.endsWith("/subtasks"))
        return Response.json({ items: [], nextCursor: null });
      return Response.json(task);
    });
}
afterEach(() => window.history.replaceState(null, "", "/"));
it.each([`/archivo${route}`, `${route}/extra`])(
  "no interpreta una ruta parcial %s como detalle de tarea",
  async (path) => {
    window.history.replaceState(null, "", path);
    const fetcher = contextFetch(() => undefined);
    render(<App />);
    await act(async () => {});
    expect(
      fetcher.mock.calls.some(
        ([url]) => url === `/api/v1/projects/${project.id}/tasks/${task.id}`,
      ),
    ).toBe(false);
    expect(
      screen.queryByRole("heading", { name: task.title }),
    ).not.toBeInTheDocument();
  },
);
it("un contexto de proyecto antiguo no invalida sesión y borrador de otra tarea", async () => {
  window.history.replaceState(null, "", route);
  const child = {
    ...task,
    id: "7c5dbd10-9ad5-4000-8000-000000000004",
    title: "Nueva tarea",
  };
  let reads = 0;
  let finish!: (response: Response) => void;
  contextFetch((url) => {
    if (url === `/api/v1/projects/${project.id}` && reads++ === 0)
      return new Promise((resolve) => {
        finish = resolve;
      });
    if (url === `/api/v1/projects/${project.id}/tasks/${child.id}`)
      return Response.json(child);
  });
  render(<SessionGate />);
  expect(await screen.findByText("Consultando proyecto")).toBeVisible();
  act(() => {
    window.history.pushState(
      null,
      "",
      `/proyectos/${project.id}/tareas/${child.id}`,
    );
    window.dispatchEvent(new PopStateEvent("popstate"));
  });
  fireEvent.change(await screen.findByLabelText("Título de la tarea"), {
    target: { value: "Borrador vigente" },
  });
  await act(async () => finish(new Response(null, { status: 401 })));
  expect(screen.getByLabelText("Título de la tarea")).toHaveValue(
    "Borrador vigente",
  );
});
it("retira snapshot anterior hasta confirmar proyecto después de un reintento de acceso", async () => {
  window.history.replaceState(null, "", route);
  let reads = 0;
  let finish!: (response: Response) => void;
  contextFetch((url, options) => {
    if (options?.method === "PUT") return new Response(null, { status: 404 });
    if (url === `/api/v1/projects/${project.id}` && reads++ === 1)
      return new Promise((resolve) => {
        finish = resolve;
      });
  });
  render(<App />);
  fireEvent.click(await screen.findByRole("button", { name: "Pausar" }));
  fireEvent.click(
    await screen.findByRole("button", { name: "Reintentar tarea" }),
  );
  expect(
    await screen.findByRole("heading", { name: task.title }),
  ).toBeVisible();
  expect(screen.getByText("Consultando proyecto")).toBeVisible();
  expect(screen.queryByLabelText("Título de la tarea")).not.toBeInTheDocument();
  expect(
    screen.queryByRole("button", { name: "Pausar" }),
  ).not.toBeInTheDocument();
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  await act(async () =>
    finish(Response.json(project, { headers: { ETag: '"version"' } })),
  );
  expect(await screen.findByLabelText("Título de la tarea")).toHaveValue("");
});
it.each([401, 404])(
  "retira el detalle cuando GET del proyecto responde %s",
  async (status) => {
    window.history.replaceState(null, "", route);
    let finish!: (response: Response) => void;
    contextFetch((url) =>
      url === `/api/v1/projects/${project.id}`
        ? new Promise((resolve) => {
            finish = resolve;
          })
        : undefined,
    );
    render(<App />);
    expect(
      await screen.findByRole("heading", { name: task.title }),
    ).toBeVisible();
    await waitFor(() => expect(finish).toBeTypeOf("function"));
    await act(async () => finish(new Response(null, { status })));
    expect(screen.getByRole("alert")).toHaveTextContent(
      status === 404
        ? "Esta tarea no está disponible"
        : "No se ha podido cargar la tarea",
    );
    expect(screen.queryByText(task.title)).not.toBeInTheDocument();
    expect(
      screen.queryByRole("region", { name: "Padre directo" }),
    ).not.toBeInTheDocument();
  },
);
it("muestra ausencia confirmada de estimación en el detalle", async () => {
  window.history.replaceState(null, "", route);
  contextFetch((url) =>
    url === `/api/v1/projects/${project.id}/tasks/${task.id}`
      ? Response.json({ ...task, estimatedMinutes: null })
      : undefined,
  );
  render(<App />);
  expect(await screen.findByText("Sin estimación")).toBeVisible();
  expect(screen.queryByText("Estimación: null min")).not.toBeInTheDocument();
});
it("enfoca tarea antes de que termine el contexto del proyecto", async () => {
  window.history.replaceState(null, "", route);
  contextFetch((url) =>
    url === `/api/v1/projects/${project.id}`
      ? new Promise(() => {})
      : undefined,
  );
  render(<App />);
  const heading = await screen.findByRole("heading", { name: task.title });
  expect(heading).toHaveFocus();
  expect(heading).toHaveAttribute("tabindex", "-1");
  expect(screen.getByText("Consultando proyecto")).toBeVisible();
});
it("no roba foco elegido mientras se carga el detalle y conserva enlace de salida", async () => {
  window.history.replaceState(null, "", route);
  let finish!: (response: Response) => void;
  contextFetch((url) =>
    url === `/api/v1/projects/${project.id}/tasks/${task.id}`
      ? new Promise((resolve) => {
          finish = resolve;
        })
      : undefined,
  );
  render(<App />);
  const back = screen.getByRole("link", { name: "Volver al proyecto" });
  expect(back).toHaveAttribute("href", `/proyectos/${project.id}`);
  back.focus();
  await act(async () => finish(Response.json(task)));
  expect(back).toHaveFocus();
});
it("permite reintentar la relación después de dos fallos consecutivos", async () => {
  window.history.replaceState(null, "", route);
  let reads = 0;
  contextFetch((url) =>
    url.endsWith("/parent")
      ? reads++ < 2
        ? new Response(null, { status: 503 })
        : Response.json({ parent: null })
      : undefined,
  );
  render(<App />);
  fireEvent.click(
    await screen.findByRole("button", { name: "Reintentar relación" }),
  );
  fireEvent.click(
    await screen.findByRole("button", { name: "Reintentar relación" }),
  );
  expect(await screen.findByText("Tarea principal confirmada")).toBeVisible();
  expect(screen.getByRole("region", { name: "Padre directo" })).toHaveAttribute(
    "tabindex",
    "-1",
  );
});
it.each(["success", "failure"])(
  "@s30 ignora contexto de proyecto %s de una tarea abandonada",
  async (outcome) => {
    window.history.replaceState(null, "", route);
    const child = {
      ...task,
      id: "7c5dbd10-9ad5-4000-8000-000000000004",
      title: "Tarea nueva",
    };
    let reads = 0;
    let finish!: (response: Response) => void;
    let reject!: (error: Error) => void;
    contextFetch((url) => {
      if (url === `/api/v1/projects/${project.id}` && reads++ === 0)
        return new Promise((resolve, no) => {
          finish = resolve;
          reject = no;
        });
      if (url === `/api/v1/projects/${project.id}/tasks/${child.id}`)
        return Response.json(child);
    });
    render(<App />);
    expect(await screen.findByText("Consultando proyecto")).toBeVisible();
    act(() => {
      window.history.pushState(
        null,
        "",
        `/proyectos/${project.id}/tareas/${child.id}`,
      );
      window.dispatchEvent(new PopStateEvent("popstate"));
    });
    fireEvent.change(await screen.findByLabelText("Título de la tarea"), {
      target: { value: "Borrador nuevo" },
    });
    await act(async () => {
      if (outcome === "success")
        finish(
          Response.json(
            { ...project, status: "completed" },
            { headers: { ETag: '"old"' } },
          ),
        );
      else reject(new Error("old"));
    });
    expect(screen.getByLabelText("Título de la tarea")).toHaveValue(
      "Borrador nuevo",
    );
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
    expect(screen.queryByText("Consultando proyecto")).not.toBeInTheDocument();
  },
);
it.each([false, true])(
  "@s32 recupera foco tras recargar proyecto sin robar foco elegido=%s",
  async (moved) => {
    window.history.replaceState(null, "", route);
    let reads = 0;
    let finish!: (response: Response) => void;
    contextFetch((url, options) => {
      if (options?.method === "PUT")
        return Response.json({ code: "PROJECT_CONFLICT" }, { status: 412 });
      if (url === `/api/v1/projects/${project.id}` && reads++ === 1)
        return new Promise((resolve) => {
          finish = resolve;
        });
    });
    render(<App />);
    fireEvent.click(await screen.findByRole("button", { name: "Pausar" }));
    const retry = await screen.findByRole("button", {
      name: "Recargar versión guardada",
    });
    retry.focus();
    fireEvent.click(retry);
    const field = screen.getByLabelText("Título de la tarea");
    if (moved) field.focus();
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
    expect(screen.getByText("Consultando proyecto")).toBeVisible();
    await act(async () =>
      finish(Response.json(project, { headers: { ETag: '"version"' } })),
    );
    expect(
      moved ? field : screen.getByRole("heading", { name: task.title }),
    ).toHaveFocus();
  },
);
it.each([false, true])(
  "@s32 restaura foco local de relación respetando movimiento elegido=%s",
  async (moved) => {
    window.history.replaceState(null, "", route);
    let reads = 0;
    let finish!: (response: Response) => void;
    contextFetch((url) =>
      url.endsWith("/parent")
        ? reads++ === 0
          ? new Response(null, { status: 503 })
          : new Promise((resolve) => {
              finish = resolve;
            })
        : undefined,
    );
    render(<App />);
    const retry = await screen.findByRole("button", {
      name: "Reintentar relación",
    });
    retry.focus();
    fireEvent.click(retry);
    const back = screen.getByRole("link", { name: "Volver al proyecto" });
    if (moved) back.focus();
    await act(async () => finish(Response.json({ parent: null })));
    expect(
      moved ? back : screen.getByRole("region", { name: "Padre directo" }),
    ).toHaveFocus();
    expect(screen.getByText("Tarea principal confirmada")).toHaveAttribute(
      "role",
      "status",
    );
  },
);
it("@s27 un reintento tras 404 no vuelve a mostrar el contenido retirado durante la espera", async () => {
  window.history.replaceState(null, "", route);
  let reads = 0;
  let finish!: (response: Response) => void;
  contextFetch((url, options) => {
    if (options?.method === "PUT") return new Response(null, { status: 404 });
    if (
      url === `/api/v1/projects/${project.id}/tasks/${task.id}` &&
      reads++ === 1
    )
      return new Promise((resolve) => {
        finish = resolve;
      });
  });
  render(<App />);
  fireEvent.change(await screen.findByLabelText("Título de la tarea"), {
    target: { value: "Dato retirado" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Pausar" }));
  fireEvent.click(
    await screen.findByRole("button", { name: "Reintentar tarea" }),
  );
  expect(screen.getByText("Cargando tarea")).toBeVisible();
  expect(screen.queryByText(task.title)).not.toBeInTheDocument();
  expect(screen.queryByLabelText("Título de la tarea")).not.toBeInTheDocument();
  await act(async () => finish(new Response(null, { status: 503 })));
  expect(screen.getByRole("alert")).toHaveTextContent(
    "No se ha podido cargar la tarea",
  );
  fireEvent.click(screen.getByRole("button", { name: "Reintentar tarea" }));
  expect(
    await screen.findByRole("heading", { name: task.title }),
  ).toHaveFocus();
  expect(await screen.findByLabelText("Título de la tarea")).toHaveValue("");
});
it("@s3 @s23 navega varios niveles y vuelve al padre directo", async () => {
  window.history.replaceState(null, "", route);
  const child = {
    ...task,
    id: "7c5dbd10-9ad5-4000-8000-000000000004",
    title: "Paso pequeño",
  };
  const grandchild = {
    ...task,
    id: "7c5dbd10-9ad5-4000-8000-000000000005",
    title: "Paso mínimo",
  };
  const base = `/api/v1/projects/${project.id}/tasks`;
  contextFetch((url) => {
    if (url === `${base}/${task.id}/subtasks`)
      return Response.json({ items: [child], nextCursor: null });
    if (url === `${base}/${child.id}/subtasks`)
      return Response.json({ items: [grandchild], nextCursor: null });
    if (url === `${base}/${child.id}/parent`)
      return Response.json({ parent: task });
    if (url === `${base}/${grandchild.id}/parent`)
      return Response.json({ parent: child });
    if (url === `${base}/${child.id}`) return Response.json(child);
    if (url === `${base}/${grandchild.id}`) return Response.json(grandchild);
  });
  render(<App />);
  fireEvent.click(await screen.findByRole("link", { name: child.title }));
  fireEvent.click(await screen.findByRole("link", { name: grandchild.title }));
  expect(
    await screen.findByRole("heading", { name: grandchild.title, level: 1 }),
  ).toBeVisible();
  fireEvent.click(
    await within(
      screen.getByRole("region", { name: "Padre directo" }),
    ).findByRole("link", { name: child.title }),
  );
  expect(
    await screen.findByRole("heading", { name: child.title, level: 1 }),
  ).toBeVisible();
  expect(window.location.pathname).toBe(
    `/proyectos/${project.id}/tareas/${child.id}`,
  );
});
it("@s32 mantiene validación y foco del formulario reutilizado", async () => {
  window.history.replaceState(null, "", route);
  const fetcher = contextFetch(() => undefined);
  render(<App />);
  fireEvent.click(
    await screen.findByRole("button", { name: "Crear subtarea" }),
  );
  expect(screen.getByLabelText("Título de la tarea")).toHaveFocus();
  expect(screen.getByLabelText("Título de la tarea")).toHaveAttribute(
    "aria-invalid",
    "true",
  );
  fireEvent.change(screen.getByLabelText("Título de la tarea"), {
    target: { value: "😀".repeat(160) },
  });
  const estimate = screen.getByLabelText("Estimación en minutos");
  Object.defineProperty(estimate, "validity", {
    configurable: true,
    get: () => ({ badInput: true }),
  });
  fireEvent.click(screen.getByRole("button", { name: "Crear subtarea" }));
  expect(estimate).toHaveFocus();
  expect(estimate).toHaveAttribute("aria-invalid", "true");
  expect(
    fetcher.mock.calls.some(([, options]) => options?.method === "POST"),
  ).toBe(false);
});
it("@s23 anuncia la carga independiente de subtareas sin bloquear acciones", async () => {
  window.history.replaceState(null, "", route);
  contextFetch((url) =>
    url.endsWith("/subtasks") ? new Promise(() => {}) : undefined,
  );
  render(<App />);
  expect(await screen.findByText("Cargando subtareas")).toBeVisible();
  expect(screen.getByRole("button", { name: "Pausar" })).toBeEnabled();
  expect(screen.getByRole("button", { name: "Crear subtarea" })).toBeEnabled();
});
it.each(["GET", "POST"])(
  "@s31 un %s antiguo no restaura datos ni revoca la sesión posterior",
  async (method) => {
    window.history.replaceState(null, "", route);
    let logged = true;
    let first = true;
    let finish!: (response: Response) => void;
    contextFetch((url, options) => {
      if (url === "/api/session/logout") {
        logged = false;
        return new Response(null, { status: 204 });
      }
      if (url === "/api/session" && options?.method === "POST") {
        logged = true;
        return new Response(null, { status: 204 });
      }
      if (url === "/api/session")
        return Response.json(logged ? authenticated : anonymous);
      if (
        first &&
        ((method === "POST" && options?.method === "POST") ||
          (method === "GET" && url.endsWith("/subtasks")))
      ) {
        first = false;
        return new Promise((resolve) => {
          finish = resolve;
        });
      }
    });
    render(<SessionGate />);
    fireEvent.change(await screen.findByLabelText("Título de la tarea"), {
      target: { value: "Secreto anterior" },
    });
    if (method === "POST")
      fireEvent.click(screen.getByRole("button", { name: "Crear subtarea" }));
    fireEvent.click(screen.getByRole("button", { name: "Cerrar sesión" }));
    fireEvent.change(await screen.findByLabelText("Usuario"), {
      target: { value: "Pablo" },
    });
    expect(
      screen.queryByLabelText("Título de la tarea"),
    ).not.toBeInTheDocument();
    fireEvent.change(screen.getByLabelText("Contraseña"), {
      target: { value: "secret" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Iniciar sesión" }));
    expect(await screen.findByLabelText("Título de la tarea")).toHaveValue("");
    await act(async () =>
      finish(Response.json({ code: "UNAUTHENTICATED" }, { status: 401 })),
    );
    expect(screen.getByLabelText("Título de la tarea")).toHaveValue("");
    expect(screen.queryByLabelText("Usuario")).not.toBeInTheDocument();
    expect(screen.queryByText("Subtarea guardada")).not.toBeInTheDocument();
  },
);
it.each(["GET", "POST"])(
  "@s30 cambia de tarea sin aceptar el %s anterior ni su borrador",
  async (method) => {
    window.history.replaceState(null, "", route);
    const child = {
      ...task,
      id: "7c5dbd10-9ad5-4000-8000-000000000004",
      title: "Otro paso",
    };
    let finish!: (response: Response) => void;
    let oldSignal: AbortSignal | null | undefined;
    contextFetch((url, options) => {
      if (
        (method === "POST" && options?.method === "POST") ||
        (method === "GET" &&
          url === `/api/v1/projects/${project.id}/tasks/${task.id}/subtasks`)
      ) {
        oldSignal = options?.signal;
        return new Promise((resolve) => {
          finish = resolve;
        });
      }
      if (url === `/api/v1/projects/${project.id}/tasks/${child.id}`)
        return Response.json(child);
    });
    render(<App />);
    fireEvent.change(await screen.findByLabelText("Título de la tarea"), {
      target: { value: "Borrador anterior" },
    });
    if (method === "POST")
      fireEvent.click(screen.getByRole("button", { name: "Crear subtarea" }));
    act(() => {
      window.history.pushState(
        null,
        "",
        `/proyectos/${project.id}/tareas/${child.id}`,
      );
      window.dispatchEvent(new PopStateEvent("popstate"));
    });
    expect(
      await screen.findByRole("heading", { name: child.title }),
    ).toBeVisible();
    fireEvent.change(await screen.findByLabelText("Título de la tarea"), {
      target: { value: "Borrador actual" },
    });
    expect(oldSignal?.aborted).toBe(true);
    await act(async () =>
      finish(
        method === "POST"
          ? Response.json(
              { ...task, title: "Anterior guardada" },
              { status: 201 },
            )
          : Response.json({ items: [task], nextCursor: null }),
      ),
    );
    expect(screen.getByLabelText("Título de la tarea")).toHaveValue(
      "Borrador actual",
    );
    expect(screen.queryByText("Anterior guardada")).not.toBeInTheDocument();
    expect(screen.queryByText("Subtarea guardada")).not.toBeInTheDocument();
  },
);
it("@s23 @s25 describe hijos vacíos y estimaciones independientes", async () => {
  window.history.replaceState(null, "", route);
  contextFetch(() => undefined);
  render(<App />);
  expect(
    await screen.findByText("Esta tarea todavía no tiene subtareas."),
  ).toBeVisible();
  expect(
    screen.getByText(
      "Cada estimación es independiente; las subtareas no se suman automáticamente a la tarea principal.",
    ),
  ).toBeVisible();
});
it("@s32 enfoca el título al entrar y permite saltar al contenido", async () => {
  window.history.replaceState(null, "", route);
  detailFetch(Response.json(task), Response.json({ parent: null }));
  render(<App />);
  expect(
    await screen.findByRole("heading", { name: task.title }),
  ).toHaveFocus();
  expect(screen.getByRole("main")).toHaveAttribute("tabindex", "-1");
});
it("@s27 retira detalle, lista y borrador cuando el POST pierde sesión", async () => {
  window.history.replaceState(null, "", route);
  let expired = false;
  contextFetch((url, options) => {
    if (url === "/api/session")
      return Response.json(expired ? anonymous : authenticated);
    if (options?.method === "POST") {
      expired = true;
      return Response.json({ code: "UNAUTHENTICATED" }, { status: 401 });
    }
  });
  render(<SessionGate />);
  fireEvent.change(await screen.findByLabelText("Título de la tarea"), {
    target: { value: "Privado" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Crear subtarea" }));
  expect(await screen.findByLabelText("Usuario")).toBeVisible();
  expect(screen.queryByText(task.title)).not.toBeInTheDocument();
  expect(screen.queryByLabelText("Título de la tarea")).not.toBeInTheDocument();
  expect(
    screen.queryByRole("region", { name: "Subtareas" }),
  ).not.toBeInTheDocument();
});
it("@s26 renueva CSRF deliberadamente conservando borrador sin repetir POST", async () => {
  window.history.replaceState(null, "", route);
  let sessions = 0;
  const fetcher = contextFetch((url, options) => {
    if (url === "/api/session")
      return Response.json({
        ...authenticated,
        csrfToken: sessions++ ? "renewed" : "private",
      });
    if (options?.method === "POST")
      return Response.json({ code: "CSRF_INVALID" }, { status: 403 });
  });
  render(<SessionGate />);
  fireEvent.change(await screen.findByLabelText("Título de la tarea"), {
    target: { value: "Privado" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Crear subtarea" }));
  fireEvent.click(
    await screen.findByRole("button", { name: "Recuperar acceso" }),
  );
  await waitFor(() =>
    expect(
      screen.queryByRole("button", { name: "Recuperar acceso" }),
    ).not.toBeInTheDocument(),
  );
  expect(screen.getByLabelText("Título de la tarea")).toHaveValue("Privado");
  expect(
    fetcher.mock.calls.filter(([, options]) => options?.method === "POST"),
  ).toHaveLength(1);
  fireEvent.click(screen.getByRole("button", { name: "Crear subtarea" }));
  await waitFor(() =>
    expect(
      fetcher.mock.calls.filter(([, options]) => options?.method === "POST"),
    ).toHaveLength(2),
  );
  expect(
    new Headers(
      fetcher.mock.calls.filter(
        ([, options]) => options?.method === "POST",
      )[1][1]?.headers,
    ).get("X-CSRF-TOKEN"),
  ).toBe("renewed");
});
it("@s26 @s36 revisa proyecto terminado con UUID en mayúsculas y reabre sin perder borrador", async () => {
  window.history.replaceState(
    null,
    "",
    `/proyectos/${project.id.toUpperCase()}/tareas/${task.id.toUpperCase()}`,
  );
  let reads = 0;
  const fetcher = contextFetch((url, options) => {
    if (options?.method === "POST")
      return Response.json({ code: "PROJECT_COMPLETED" }, { status: 409 });
    if (options?.method === "PUT")
      return Response.json(
        { ...project, status: "paused" },
        { headers: { ETag: '"reopened"' } },
      );
    if (url === `/api/v1/projects/${project.id}`)
      return Response.json(
        { ...project, status: reads++ ? "completed" : "active" },
        { headers: { ETag: reads === 1 ? '"version"' : '"completed"' } },
      );
  });
  render(<App />);
  fireEvent.change(await screen.findByLabelText("Título de la tarea"), {
    target: { value: "Borrador guardado" },
  });
  expect(
    screen.getByRole("link", { name: "Volver al proyecto" }),
  ).toHaveAttribute("href", `/proyectos/${project.id}`);
  fireEvent.change(screen.getByLabelText("Criterio de finalización"), {
    target: { value: "Resultado" },
  });
  fireEvent.change(screen.getByLabelText("Estimación en minutos"), {
    target: { value: "15" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Crear subtarea" }));
  fireEvent.click(
    await screen.findByRole("button", { name: "Revisar estado del proyecto" }),
  );
  fireEvent.click(
    await screen.findByRole("button", { name: "Reabrir en pausa" }),
  );
  expect(await screen.findByLabelText("Título de la tarea")).toHaveValue(
    "Borrador guardado",
  );
  expect(screen.getByLabelText("Criterio de finalización")).toHaveValue(
    "Resultado",
  );
  expect(screen.getByLabelText("Estimación en minutos")).toHaveValue(15);
  expect(
    fetcher.mock.calls.filter(([, options]) => options?.method === "POST"),
  ).toHaveLength(1);
  expect(
    new Headers(
      fetcher.mock.calls.find(([, options]) => options?.method === "PUT")?.[1]
        ?.headers,
    ).get("If-Match"),
  ).toBe('"completed"');
});
it.each([400, 409, 503, "network"])(
  "@s26 conserva los tres campos ante %s sin reenvío automático",
  async (status) => {
    window.history.replaceState(null, "", route);
    const fetcher = contextFetch((_, options) =>
      options?.method === "POST"
        ? status === "network"
          ? Promise.reject(new Error("offline"))
          : Response.json(
              {
                code: status === 409 ? "PROJECT_COMPLETED" : "INVALID_REQUEST",
                errors: status === 400 ? [{ field: "title" }] : [],
              },
              { status: status as number },
            )
        : undefined,
    );
    render(<App />);
    fireEvent.change(await screen.findByLabelText("Título de la tarea"), {
      target: { value: "  Borrador  " },
    });
    fireEvent.change(screen.getByLabelText("Criterio de finalización"), {
      target: { value: "Primera línea\nOtra línea" },
    });
    fireEvent.change(screen.getByLabelText("Estimación en minutos"), {
      target: { value: "12" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Crear subtarea" }));
    await waitFor(() =>
      expect(screen.getAllByRole("alert").length).toBeGreaterThan(0),
    );
    expect(screen.getByLabelText("Título de la tarea")).toHaveValue(
      "  Borrador  ",
    );
    expect(screen.getByLabelText("Criterio de finalización")).toHaveValue(
      "Primera línea\nOtra línea",
    );
    expect(screen.getByLabelText("Estimación en minutos")).toHaveValue(12);
    expect(
      fetcher.mock.calls.filter(([, options]) => options?.method === "POST"),
    ).toHaveLength(1);
  },
);
it("@s26 conserva el borrador cuando falla la recarga del proyecto y permite recuperarlo", async () => {
  window.history.replaceState(null, "", route);
  let reads = 0;
  vi.spyOn(globalThis, "fetch").mockImplementation(async (url, options) => {
    const stateResponse = taskStateRead(String(url));
    if (stateResponse) return stateResponse;
    if (options?.method === "PUT")
      return Response.json({ code: "PROJECT_CONFLICT" }, { status: 412 });
    if (url === `/api/v1/projects/${project.id}`)
      return reads++ === 1
        ? new Response(null, { status: 503 })
        : Response.json(project, { headers: { ETag: '"version"' } });
    if (String(url).endsWith("/parent")) return Response.json({ parent: null });
    if (String(url).endsWith("/subtasks"))
      return Response.json({ items: [], nextCursor: null });
    return Response.json(task);
  });
  render(<App />);
  fireEvent.change(await screen.findByLabelText("Título de la tarea"), {
    target: { value: "Mi borrador" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Pausar" }));
  fireEvent.click(
    await screen.findByRole("button", { name: "Recargar versión guardada" }),
  );
  fireEvent.click(
    await screen.findByRole("button", { name: "Reintentar proyecto" }),
  );
  expect(await screen.findByRole("button", { name: "Pausar" })).toBeEnabled();
  expect(screen.getByLabelText("Título de la tarea")).toHaveValue(
    "Mi borrador",
  );
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
});
it("@s26 conserva el borrador al recargar un conflicto del estado del proyecto", async () => {
  window.history.replaceState(null, "", route);
  let reads = 0;
  vi.spyOn(globalThis, "fetch").mockImplementation(async (url, options) => {
    const stateResponse = taskStateRead(String(url));
    if (stateResponse) return stateResponse;
    if (options?.method === "PUT")
      return Response.json({ code: "PROJECT_CONFLICT" }, { status: 412 });
    if (url === `/api/v1/projects/${project.id}`)
      return Response.json(
        { ...project, status: reads++ ? "paused" : "active" },
        { headers: { ETag: reads === 1 ? '"version"' : '"next"' } },
      );
    if (String(url).endsWith("/parent")) return Response.json({ parent: null });
    if (String(url).endsWith("/subtasks"))
      return Response.json({ items: [], nextCursor: null });
    return Response.json(task);
  });
  render(<App />);
  fireEvent.change(await screen.findByLabelText("Título de la tarea"), {
    target: { value: "Mi borrador" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Pausar" }));
  fireEvent.click(
    await screen.findByRole("button", { name: "Recargar versión guardada" }),
  );
  expect(await screen.findByRole("button", { name: "Retomar" })).toBeEnabled();
  expect(screen.getByLabelText("Título de la tarea")).toHaveValue(
    "Mi borrador",
  );
});
it("@s23 conserva la ruta de tarea tras iniciar sesión", async () => {
  window.history.replaceState(null, "", route);
  let logged = false;
  vi.spyOn(globalThis, "fetch").mockImplementation(async (url, options) => {
    const stateResponse = taskStateRead(String(url));
    if (stateResponse) return stateResponse;
    if (url === "/api/session" && options?.method === "POST") {
      logged = true;
      return new Response(null, { status: 204 });
    }
    if (url === "/api/session")
      return Response.json(logged ? authenticated : anonymous);
    if (url === `/api/v1/projects/${project.id}`)
      return Response.json(project, { headers: { ETag: '"version"' } });
    if (String(url).endsWith("/parent")) return Response.json({ parent: null });
    if (String(url).endsWith("/subtasks"))
      return Response.json({ items: [], nextCursor: null });
    return Response.json(task);
  });
  render(<SessionGate />);
  fireEvent.change(await screen.findByLabelText("Usuario"), {
    target: { value: "Pablo" },
  });
  fireEvent.change(screen.getByLabelText("Contraseña"), {
    target: { value: "secret" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Iniciar sesión" }));
  expect(
    await screen.findByRole("heading", { name: task.title }),
  ).toBeVisible();
  expect(window.location.pathname).toBe(route);
});
it("@s23 distingue una tarea no disponible sin afirmar una relación raíz", async () => {
  window.history.replaceState(null, "", route);
  vi.spyOn(globalThis, "fetch").mockResolvedValue(
    new Response(null, { status: 404 }),
  );
  render(<App />);
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "Esta tarea no está disponible para tu cuenta",
  );
  expect(
    screen.queryByRole("region", { name: "Padre directo" }),
  ).not.toBeInTheDocument();
  expect(
    screen.queryByRole("region", { name: "Subtareas" }),
  ).not.toBeInTheDocument();
});
it("@s28 @s29 confirma una sola creación aunque falle la recarga de hijos", async () => {
  window.history.replaceState(null, "", route);
  const child = {
    ...task,
    id: "7c5dbd10-9ad5-4000-8000-000000000004",
    title: "Nuevo paso",
    completionCriterion: "",
    estimatedMinutes: null,
  };
  let finish!: (response: Response) => void;
  let posted = false;
  const fetcher = vi
    .spyOn(globalThis, "fetch")
    .mockImplementation(async (url, options) => {
      const stateResponse = taskStateRead(String(url));
      if (stateResponse) return stateResponse;
      if (options?.method === "POST") {
        posted = true;
        return new Promise((resolve) => {
          finish = resolve;
        });
      }
      if (url === `/api/v1/projects/${project.id}`)
        return Response.json(project, { headers: { ETag: '"version"' } });
      if (String(url).endsWith("/parent"))
        return Response.json({ parent: null });
      if (String(url).endsWith("/subtasks"))
        return posted
          ? new Response(null, { status: 503 })
          : Response.json({ items: [], nextCursor: null });
      return Response.json(task);
    });
  render(<App />);
  fireEvent.change(await screen.findByLabelText("Título de la tarea"), {
    target: { value: child.title },
  });
  const button = screen.getByRole("button", { name: "Crear subtarea" });
  fireEvent.click(button);
  fireEvent.submit(button.closest("form")!);
  expect(button).toBeDisabled();
  expect(screen.getByText("Guardando subtarea")).toBeVisible();
  await act(async () => finish(Response.json(child, { status: 201 })));
  expect(
    await screen.findByRole("button", { name: "Reintentar subtareas" }),
  ).toBeEnabled();
  expect(screen.getByText("Subtarea guardada")).toBeVisible();
  expect(screen.getByRole("link", { name: child.title })).toHaveAttribute(
    "href",
    `/proyectos/${project.id}/tareas/${child.id}`,
  );
  expect(
    fetcher.mock.calls.filter(([, options]) => options?.method === "POST"),
  ).toHaveLength(1);
});
it.each(["success", "failure"])(
  "@s30 ignora la relación %s abortada sin cambiar la raíz confirmada",
  async (outcome) => {
    window.history.replaceState(null, "", route);
    let finish!: (response: Response) => void;
    let reject!: (error: Error) => void;
    let reads = 0;
    vi.spyOn(globalThis, "fetch").mockImplementation(async (url) => {
      const stateResponse = taskStateRead(String(url));
      if (stateResponse) return stateResponse;
      if (url === `/api/v1/projects/${project.id}`)
        return Response.json(project, { headers: { ETag: '"version"' } });
      if (String(url).endsWith("/parent")) {
        if (reads++ === 0)
          return new Promise((resolve, no) => {
            finish = resolve;
            reject = no;
          });
        return Response.json({ parent: null });
      }
      if (String(url).endsWith("/subtasks"))
        return Response.json({ items: [], nextCursor: null });
      return Response.json(task);
    });
    render(
      <StrictMode>
        <App />
      </StrictMode>,
    );
    expect(await screen.findByText("Tarea principal confirmada")).toBeVisible();
    await act(async () => {
      if (outcome === "success")
        finish(
          Response.json({
            parent: {
              ...task,
              id: "7c5dbd10-9ad5-4000-8000-000000000004",
              title: "Padre antiguo",
            },
          }),
        );
      else reject(new Error("old"));
    });
    expect(screen.getByText("Tarea principal confirmada")).toBeVisible();
    expect(screen.queryByText("Padre antiguo")).not.toBeInTheDocument();
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  },
);
it.each(["success", "failure"])(
  "@s30 ignora el GET %s antiguo bajo StrictMode",
  async (outcome) => {
    window.history.replaceState(null, "", route);
    let finish!: (response: Response) => void;
    let reject!: (error: Error) => void;
    let reads = 0;
    vi.spyOn(globalThis, "fetch").mockImplementation(async (url) => {
      const stateResponse = taskStateRead(String(url));
      if (stateResponse) return stateResponse;
      if (url === `/api/v1/projects/${project.id}`)
        return Response.json(project, { headers: { ETag: '"version"' } });
      if (String(url).endsWith("/parent"))
        return Response.json({ parent: null });
      if (String(url).endsWith("/subtasks"))
        return Response.json({ items: [], nextCursor: null });
      if (reads++ === 0)
        return new Promise((resolve, no) => {
          finish = resolve;
          reject = no;
        });
      return Response.json(task);
    });
    render(
      <StrictMode>
        <App />
      </StrictMode>,
    );
    expect(
      await screen.findByRole("button", { name: "Crear subtarea" }),
    ).toBeVisible();
    await act(async () => {
      if (outcome === "success")
        finish(Response.json({ ...task, title: "Respuesta antigua" }));
      else reject(new Error("old"));
    });
    expect(screen.getByRole("heading", { name: task.title })).toBeVisible();
    expect(screen.queryByText("Respuesta antigua")).not.toBeInTheDocument();
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  },
);
it("@s23 pagina hijos y recupera recientes después de un error independiente", async () => {
  window.history.replaceState(null, "", route);
  const child = {
    ...task,
    id: "7c5dbd10-9ad5-4000-8000-000000000004",
    title: "Paso",
  };
  const fetcher = vi
    .spyOn(globalThis, "fetch")
    .mockImplementation(async (url) => {
      const stateResponse = taskStateRead(String(url));
      if (stateResponse) return stateResponse;
      if (url === `/api/v1/projects/${project.id}`)
        return Response.json(project, { headers: { ETag: '"version"' } });
      if (String(url).endsWith("/parent"))
        return Response.json({ parent: null });
      if (String(url).includes("?cursor="))
        return new Response(null, { status: 503 });
      if (String(url).endsWith("/subtasks"))
        return Response.json({ items: [child], nextCursor: "next/+=" });
      return Response.json(task);
    });
  render(<App />);
  expect(
    await screen.findByRole("list", { name: "Subtareas guardadas" }),
  ).toBeVisible();
  fireEvent.click(
    screen.getByRole("button", { name: "Más subtareas antiguas" }),
  );
  expect(
    await screen.findByRole("button", { name: "Reintentar subtareas" }),
  ).toBeEnabled();
  expect(screen.getByRole("button", { name: "Pausar" })).toBeEnabled();
  fireEvent.click(
    screen.getByRole("button", { name: "Volver a subtareas recientes" }),
  );
  await waitFor(() =>
    expect(screen.getByRole("link", { name: child.title })).toBeVisible(),
  );
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  expect(fetcher).toHaveBeenCalledWith(
    `/api/v1/projects/${project.id}/tasks/${task.id}/subtasks?cursor=next%2F%2B%3D`,
    expect.anything(),
  );
});
it("@s36 permite reabrir el proyecto desde el contexto de la tarea", async () => {
  window.history.replaceState(null, "", route);
  const fetcher = vi
    .spyOn(globalThis, "fetch")
    .mockImplementation(async (url, options) => {
      const stateResponse = taskStateRead(String(url));
      if (stateResponse) return stateResponse;
      if (options?.method === "PUT")
        return Response.json(
          { ...project, status: "paused" },
          { headers: { ETag: '"next-version"' } },
        );
      if (url === `/api/v1/projects/${project.id}`)
        return Response.json(
          { ...project, status: "completed" },
          { headers: { ETag: '"version"' } },
        );
      if (String(url).endsWith("/parent"))
        return Response.json({ parent: null });
      if (String(url).endsWith("/subtasks"))
        return Response.json({ items: [], nextCursor: null });
      return Response.json(task);
    });
  render(<App />);
  fireEvent.click(
    await screen.findByRole("button", { name: "Reabrir en pausa" }),
  );
  expect(
    await screen.findByRole("button", { name: "Crear subtarea" }),
  ).toBeEnabled();
  expect(screen.getByRole("heading", { name: task.title })).toBeVisible();
  const put = fetcher.mock.calls.find(
    ([, options]) => options?.method === "PUT",
  );
  expect(put?.[0]).toBe(`/api/v1/projects/${project.id}/status`);
  expect(new Headers(put?.[1]?.headers).get("If-Match")).toBe('"version"');
});
it("@s25 crea una subtarea sin modificar la estimación del padre", async () => {
  window.history.replaceState(null, "", route);
  const child = {
    ...task,
    id: "7c5dbd10-9ad5-4000-8000-000000000004",
    title: "Redactar titular",
    completionCriterion: "Texto listo",
    estimatedMinutes: 10,
  };
  let saved = false;
  const fetcher = vi
    .spyOn(globalThis, "fetch")
    .mockImplementation(async (url, options) => {
      const stateResponse = taskStateRead(String(url));
      if (stateResponse) return stateResponse;
      if (options?.method === "POST") {
        saved = true;
        return Response.json(child, { status: 201 });
      }
      if (url === `/api/v1/projects/${project.id}`)
        return Response.json(project, { headers: { ETag: '"version"' } });
      if (String(url).endsWith("/parent"))
        return Response.json({ parent: null });
      if (String(url).endsWith("/subtasks"))
        return Response.json({ items: saved ? [child] : [], nextCursor: null });
      return Response.json(task);
    });
  render(<App />);
  fireEvent.change(await screen.findByLabelText("Título de la tarea"), {
    target: { value: child.title },
  });
  fireEvent.change(screen.getByLabelText("Criterio de finalización"), {
    target: { value: child.completionCriterion },
  });
  fireEvent.change(screen.getByLabelText("Estimación en minutos"), {
    target: { value: "10" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Crear subtarea" }));
  expect(await screen.findByText("Subtarea guardada")).toBeVisible();
  await waitFor(() =>
    expect(screen.getByRole("link", { name: child.title })).toBeVisible(),
  );
  expect(screen.getByText("Estimación: 30 min")).toBeVisible();
  expect(
    fetcher.mock.calls.filter(([, options]) => options?.method === "POST"),
  ).toEqual([
    [
      `/api/v1/projects/${project.id}/tasks/${task.id}/subtasks`,
      expect.objectContaining({
        body: JSON.stringify({
          title: child.title,
          completionCriterion: child.completionCriterion,
          estimatedMinutes: 10,
        }),
      }),
    ],
  ]);
});
it("@s23 carga sólo los hijos directos junto al proyecto confirmado", async () => {
  window.history.replaceState(null, "", route);
  const child = {
    ...task,
    id: "7c5dbd10-9ad5-4000-8000-000000000004",
    title: "Redactar titular",
  };
  const fetcher = vi
    .spyOn(globalThis, "fetch")
    .mockImplementation(async (url) => {
      const stateResponse = taskStateRead(String(url));
      if (stateResponse) return stateResponse;
      if (url === `/api/v1/projects/${project.id}`)
        return Response.json(project, { headers: { ETag: '"version"' } });
      if (String(url).endsWith("/parent"))
        return Response.json({ parent: null });
      if (String(url).endsWith("/subtasks"))
        return Response.json({ items: [child], nextCursor: null });
      return Response.json(task);
    });
  render(<App />);
  const children = await screen.findByRole("region", { name: "Subtareas" });
  expect(
    await within(children).findByRole("link", { name: child.title }),
  ).toHaveAttribute("href", `/proyectos/${project.id}/tareas/${child.id}`);
  expect(
    within(children).getByRole("button", { name: "Crear subtarea" }),
  ).toBeEnabled();
  expect(fetcher).toHaveBeenCalledWith(
    `/api/v1/projects/${project.id}/tasks/${task.id}/subtasks`,
    expect.objectContaining({ cache: "no-store" }),
  );
  expect(
    fetcher.mock.calls.some(
      ([url]) => String(url) === `/api/v1/projects/${project.id}/tasks`,
    ),
  ).toBe(false);
});
it("@s24 @s38 conserva el detalle cuando falla la relación y permite confirmar raíz después", async () => {
  window.history.replaceState(null, "", route);
  detailFetch(
    Response.json(task),
    new Response(null, { status: 503 }),
    Response.json({ parent: null }),
  );
  render(<App />);
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "No se ha podido consultar la relación",
  );
  expect(screen.getByRole("heading", { name: task.title })).toBeVisible();
  expect(
    screen.queryByText("Tarea principal confirmada"),
  ).not.toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", { name: "Reintentar relación" }));
  expect(await screen.findByText("Tarea principal confirmada")).toBeVisible();
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
});
it("@s23 muestra el enlace al padre directo confirmado", async () => {
  window.history.replaceState(null, "", route);
  const parent = {
    ...task,
    id: "7c5dbd10-9ad5-4000-8000-000000000003",
    title: "Diseñar la web",
  };
  detailFetch(Response.json(task), Response.json({ parent }));
  render(<App />);
  const relation = await screen.findByRole("region", { name: "Padre directo" });
  expect(
    await within(relation).findByRole("link", { name: parent.title }),
  ).toHaveAttribute("href", `/proyectos/${project.id}/tareas/${parent.id}`);
  expect(
    screen.queryByText("Tarea principal confirmada"),
  ).not.toBeInTheDocument();
});
it("@s23 identifica una tarea raíz sólo tras confirmar la relación", async () => {
  window.history.replaceState(null, "", route);
  let resolveParent!: (response: Response) => void;
  detailFetch(
    Response.json(task),
    new Promise((resolve) => {
      resolveParent = resolve;
    }),
  );
  render(<App />);
  expect(
    await screen.findByRole("heading", { name: task.title }),
  ).toBeVisible();
  expect(
    screen.queryByText("Tarea principal confirmada"),
  ).not.toBeInTheDocument();
  await waitFor(() => expect(resolveParent).toBeDefined());
  await act(async () => resolveParent(Response.json({ parent: null })));
  expect(await screen.findByText("Tarea principal confirmada")).toBeVisible();
});
it("@s23 permite reintentar un detalle no confirmado sin mostrar contenido", async () => {
  window.history.replaceState(null, "", route);
  detailFetch(
    new Response(null, { status: 503 }),
    Response.json(task),
    Response.json({ parent: null }),
  );
  render(<App />);
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "No se ha podido cargar la tarea",
  );
  expect(screen.queryByText(task.title)).not.toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", { name: "Reintentar tarea" }));
  expect(
    await screen.findByRole("heading", { name: task.title }),
  ).toBeVisible();
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
});
it("@s23 carga directamente el detalle confirmado y conserva enlace al proyecto", async () => {
  window.history.replaceState(null, "", route);
  const fetcher = detailFetch(
    Response.json(task),
    Response.json({ parent: null }),
  );
  render(<App />);
  expect(
    await screen.findByRole("heading", { name: task.title, level: 1 }),
  ).toBeVisible();
  expect(screen.getByText(task.completionCriterion)).toBeVisible();
  expect(screen.getByText("Estimación: 30 min")).toBeVisible();
  expect(await screen.findByText("Pendiente")).toBeVisible();
  expect(
    screen.getByRole("link", { name: "Volver al proyecto" }),
  ).toHaveAttribute("href", `/proyectos/${project.id}`);
  expect(fetcher.mock.calls[0]).toEqual([
    `/api/v1/projects/${project.id}/tasks/${task.id}`,
    expect.objectContaining({ credentials: "same-origin", cache: "no-store" }),
  ]);
});
it("@s23 permite abrir el detalle desde la lista plana del proyecto", async () => {
  window.history.replaceState(null, "", `/proyectos/${project.id}`);
  vi.spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(
      Response.json(project, { headers: { ETag: '"version"' } }),
    )
    .mockResolvedValueOnce(Response.json({ items: [task], nextCursor: null }));
  render(<App />);
  expect(await screen.findByRole("link", { name: task.title })).toHaveAttribute(
    "href",
    route,
  );
});

function taskStateRead(url: string) {
  const match = /\/tasks\/([^/]+)\/status$/.exec(url);
  if (match)
    return Response.json(
      { status: "pending", completedAt: null, updatedAt: task.updatedAt },
      { headers: { ETag: `"task:${match[1].toLowerCase()}:0"` } },
    );
  if (/\/tasks\/[^/]+\/history(?:\?|$)/.test(url))
    return Response.json({ items: [], nextCursor: null });
}
