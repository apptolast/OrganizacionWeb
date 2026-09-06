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
import { SessionGate } from "./session-gate";
import { StrictMode } from "react";
import { ProjectTasks } from "./project-tasks";
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
const route = `/proyectos/${project.id}`;
const tasksUrl = `/api/v1/projects/${project.id}/tasks`;
afterEach(() => window.history.replaceState(null, "", "/"));
it("confirmación recibida y clic de reintento antes del commit React no cancelan GET", async () => {
  window.history.replaceState(null, "", route);
  let resolve!: (response: Response) => void;
  vi.spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(
      Response.json(project, { headers: { ETag: '"version"' } }),
    )
    .mockResolvedValueOnce(new Response(null, { status: 503 }))
    .mockImplementationOnce(
      () =>
        new Promise((done) => {
          resolve = done;
        }),
    )
    .mockImplementation(async () =>
      Response.json({ items: [task], nextCursor: null }),
    );
  render(<App />);
  fireEvent.change(await screen.findByLabelText("Título de la tarea"), {
    target: { value: task.title },
  });
  const retry = await screen.findByRole("button", {
    name: "Reintentar tareas",
  });
  fireEvent.click(screen.getByRole("button", { name: "Crear tarea" }));
  await act(async () => {
    resolve(Response.json(task, { status: 201 }));
    await new Promise((done) => setTimeout(done, 0));
    expect(retry.isConnected).toBe(true);
    fireEvent.click(retry);
  });
  await waitFor(() =>
    expect(
      screen.getByRole("list", { name: "Tareas guardadas" }),
    ).toHaveTextContent(task.title),
  );
});
it("al visitar tareas antiguas conserva la tarjeta de la creación confirmada", async () => {
  window.history.replaceState(null, "", route);
  const old = {
    ...task,
    id: "7c5dbd10-9ad5-4000-8000-000000000099",
    title: "Antigua",
  };
  vi.spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(
      Response.json(project, { headers: { ETag: '"version"' } }),
    )
    .mockResolvedValueOnce(Response.json({ items: [], nextCursor: null }))
    .mockResolvedValueOnce(Response.json(task, { status: 201 }))
    .mockResolvedValueOnce(Response.json({ items: [task], nextCursor: "next" }))
    .mockResolvedValueOnce(Response.json({ items: [old], nextCursor: null }));
  render(<App />);
  fireEvent.change(await screen.findByLabelText("Título de la tarea"), {
    target: { value: task.title },
  });
  fireEvent.click(screen.getByRole("button", { name: "Crear tarea" }));
  fireEvent.click(
    await screen.findByRole("button", { name: "Más tareas antiguas" }),
  );
  await screen.findByRole("heading", { name: old.title });
  expect(
    within(
      screen.getByRole("article", { name: "Última tarea guardada" }),
    ).getByRole("heading", { name: task.title }),
  ).toBeVisible();
});
it("reintento y confirmación en el mismo ciclo no cancelan la actualización de lista", async () => {
  window.history.replaceState(null, "", route);
  let resolve!: (response: Response) => void;
  vi.spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(
      Response.json(project, { headers: { ETag: '"version"' } }),
    )
    .mockResolvedValueOnce(new Response(null, { status: 503 }))
    .mockImplementationOnce(
      () =>
        new Promise((done) => {
          resolve = done;
        }),
    )
    .mockImplementation(async () =>
      Response.json({ items: [task], nextCursor: null }),
    );
  render(<App />);
  fireEvent.change(await screen.findByLabelText("Título de la tarea"), {
    target: { value: task.title },
  });
  await screen.findByRole("button", { name: "Reintentar tareas" });
  fireEvent.click(screen.getByRole("button", { name: "Crear tarea" }));
  await act(async () => {
    fireEvent.click(screen.getByRole("button", { name: "Reintentar tareas" }));
    resolve(Response.json(task, { status: 201 }));
  });
  await waitFor(() =>
    expect(
      screen.getByRole("list", { name: "Tareas guardadas" }),
    ).toHaveTextContent(task.title),
  );
});
it("un reintento de lista durante POST no impide refrescar al confirmar la creación", async () => {
  window.history.replaceState(null, "", route);
  let resolve!: (response: Response) => void;
  const fetcher = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(
      Response.json(project, { headers: { ETag: '"version"' } }),
    )
    .mockResolvedValueOnce(new Response(null, { status: 503 }))
    .mockImplementationOnce(
      () =>
        new Promise((done) => {
          resolve = done;
        }),
    )
    .mockResolvedValueOnce(Response.json({ items: [], nextCursor: null }))
    .mockResolvedValueOnce(Response.json({ items: [task], nextCursor: null }));
  render(<App />);
  fireEvent.change(await screen.findByLabelText("Título de la tarea"), {
    target: { value: task.title },
  });
  await screen.findByRole("button", { name: "Reintentar tareas" });
  fireEvent.click(screen.getByRole("button", { name: "Crear tarea" }));
  fireEvent.click(screen.getByRole("button", { name: "Reintentar tareas" }));
  await screen.findByText("Todavía no hay tareas en este proyecto.");
  await act(async () => resolve(Response.json(task, { status: 201 })));
  expect(
    await screen.findByRole("list", { name: "Tareas guardadas" }),
  ).toHaveTextContent(task.title);
  expect(fetcher).toHaveBeenCalledTimes(5);
});
it("una revisión tardía no restaura proyecto retirado por un 404 de sus acciones", async () => {
  window.history.replaceState(null, "", route);
  let resolve!: (response: Response) => void;
  let reviewSignal: AbortSignal | null | undefined;
  vi.spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(
      Response.json(project, { headers: { ETag: '"version"' } }),
    )
    .mockResolvedValueOnce(Response.json({ items: [], nextCursor: null }))
    .mockResolvedValueOnce(
      Response.json({ code: "PROJECT_COMPLETED" }, { status: 409 }),
    )
    .mockImplementationOnce((input, options) => {
      expect(String(input)).toBe(`/api/v1/projects/${project.id}`);
      expect(options?.method ?? "GET").toBe("GET");
      reviewSignal = options?.signal;
      return new Promise((done) => {
        resolve = done;
      });
    })
    .mockResolvedValueOnce(new Response(null, { status: 404 }));
  render(<App />);
  fireEvent.change(await screen.findByLabelText("Título de la tarea"), {
    target: { value: "Privado" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Crear tarea" }));
  fireEvent.click(
    await screen.findByRole("button", { name: "Revisar estado del proyecto" }),
  );
  fireEvent.click(screen.getByRole("button", { name: "Pausar" }));
  await screen.findByText("Este proyecto no está disponible para tu cuenta.");
  await waitFor(() => expect(reviewSignal?.aborted).toBe(true));
  await waitFor(() =>
    expect(
      screen.queryByRole("region", { name: "Tareas" }),
    ).not.toBeInTheDocument(),
  );
  await act(async () =>
    resolve(Response.json(project, { headers: { ETag: '"stale"' } })),
  );
  expect(
    screen.queryByRole("region", { name: "Tareas" }),
  ).not.toBeInTheDocument();
  expect(
    screen.queryByRole("heading", { name: project.name }),
  ).not.toBeInTheDocument();
});
it("una segunda creación pendiente no conserva confirmación antigua y refresca sin filas obsoletas", async () => {
  window.history.replaceState(null, "", route);
  let resolve!: (response: Response) => void;
  let resolveRead!: (response: Response) => void;
  const fetcher = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(
      Response.json(project, { headers: { ETag: '"version"' } }),
    )
    .mockResolvedValueOnce(Response.json({ items: [], nextCursor: null }))
    .mockResolvedValueOnce(Response.json(task, { status: 201 }))
    .mockResolvedValueOnce(Response.json({ items: [task], nextCursor: null }))
    .mockImplementationOnce(
      () =>
        new Promise((done) => {
          resolve = done;
        }),
    )
    .mockImplementationOnce(
      () =>
        new Promise((done) => {
          resolveRead = done;
        }),
    );
  render(<App />);
  fireEvent.change(await screen.findByLabelText("Título de la tarea"), {
    target: { value: task.title },
  });
  fireEvent.click(screen.getByRole("button", { name: "Crear tarea" }));
  await waitFor(() =>
    expect(
      screen.getByRole("list", { name: "Tareas guardadas" }),
    ).toHaveTextContent(task.title),
  );
  fireEvent.click(screen.getByRole("button", { name: "Crear tarea" }));
  expect(screen.queryByText("Tarea guardada")).not.toBeInTheDocument();
  const next = {
    ...task,
    id: "7c5dbd10-9ad5-4000-8000-000000000099",
    title: "Nueva",
  };
  await act(async () => resolve(Response.json(next, { status: 201 })));
  expect(
    screen.queryByRole("list", { name: "Tareas guardadas" }),
  ).not.toBeInTheDocument();
  expect(screen.getByText("Cargando tareas")).toBeVisible();
  await act(async () =>
    resolveRead(Response.json({ items: [next, task], nextCursor: null })),
  );
  expect(fetcher).toHaveBeenCalledTimes(6);
});
it("la revisión pendiente bloquea acciones repetidas y retira el aviso tras recuperarse", async () => {
  window.history.replaceState(null, "", route);
  let resolve!: (response: Response) => void;
  const fetcher = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(
      Response.json(project, { headers: { ETag: '"version"' } }),
    )
    .mockResolvedValueOnce(Response.json({ items: [], nextCursor: null }))
    .mockResolvedValueOnce(
      Response.json({ code: "PROJECT_COMPLETED" }, { status: 409 }),
    )
    .mockImplementationOnce(
      () =>
        new Promise((done) => {
          resolve = done;
        }),
    );
  render(<App />);
  fireEvent.change(await screen.findByLabelText("Título de la tarea"), {
    target: { value: "Borrador" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Crear tarea" }));
  const review = await screen.findByRole("button", {
    name: "Revisar estado del proyecto",
  });
  expect(
    screen.queryByText("Revisando estado del proyecto"),
  ).not.toBeInTheDocument();
  fireEvent.click(review);
  fireEvent.click(review);
  expect(review).toBeDisabled();
  expect(screen.getByText("Revisando estado del proyecto")).toHaveAttribute(
    "role",
    "status",
  );
  expect(screen.getByRole("button", { name: "Crear tarea" })).toBeDisabled();
  expect(fetcher).toHaveBeenCalledTimes(4);
  await act(async () =>
    resolve(Response.json(project, { headers: { ETag: '"next"' } })),
  );
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  expect(
    screen.queryByText("Revisando estado del proyecto"),
  ).not.toBeInTheDocument();
});
it.each([
  [409, { code: "OTHER" }],
  [409, null],
  [500, { code: "PROJECT_COMPLETED", errors: [{ field: "title" }] }],
  [400, null],
  [400, { errors: [null, 42, "text", {}, { field: "unknown" }] }],
])(
  "un problema no aplicable no inventa conflictos ni errores de campo %#",
  async (status, body) => {
    window.history.replaceState(null, "", route);
    vi.spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(
        Response.json(project, { headers: { ETag: '"version"' } }),
      )
      .mockResolvedValueOnce(Response.json({ items: [], nextCursor: null }))
      .mockResolvedValueOnce(Response.json(body, { status: status as number }));
    render(<App />);
    fireEvent.change(await screen.findByLabelText("Título de la tarea"), {
      target: { value: "Válido" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Crear tarea" }));
    await screen.findByText("No hemos podido guardar la tarea.");
    expect(
      screen.queryByRole("button", { name: "Revisar estado del proyecto" }),
    ).not.toBeInTheDocument();
    expect(screen.getByLabelText("Título de la tarea")).not.toHaveAttribute(
      "aria-invalid",
      "true",
    );
    expect(screen.getByRole("button", { name: "Crear tarea" })).toBeEnabled();
  },
);
it("varios errores del servidor se asocian en orden visual ignorando entradas ajenas", async () => {
  window.history.replaceState(null, "", route);
  vi.spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(
      Response.json(project, { headers: { ETag: '"version"' } }),
    )
    .mockResolvedValueOnce(Response.json({ items: [], nextCursor: null }))
    .mockResolvedValueOnce(
      Response.json(
        {
          errors: [
            null,
            42,
            {},
            { field: "completionCriterion" },
            { field: "title" },
          ],
        },
        { status: 400 },
      ),
    );
  render(<App />);
  fireEvent.change(await screen.findByLabelText("Título de la tarea"), {
    target: { value: "Válido" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Crear tarea" }));
  await screen.findByText("Escribe un título de entre 1 y 160 caracteres.");
  expect(screen.getByLabelText("Título de la tarea")).toHaveFocus();
  expect(screen.getByLabelText("Criterio de finalización")).toHaveAttribute(
    "aria-invalid",
    "true",
  );
});
it("permite dos reintentos de lectura fallidos antes de la recuperación", async () => {
  window.history.replaceState(null, "", route);
  const fetcher = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(
      Response.json(project, { headers: { ETag: '"version"' } }),
    )
    .mockResolvedValueOnce(new Response(null, { status: 503 }))
    .mockResolvedValueOnce(new Response(null, { status: 503 }))
    .mockResolvedValueOnce(Response.json({ items: [task], nextCursor: null }));
  render(<App />);
  fireEvent.click(
    await screen.findByRole("button", { name: "Reintentar tareas" }),
  );
  fireEvent.click(
    await screen.findByRole("button", { name: "Reintentar tareas" }),
  );
  await waitFor(() =>
    expect(screen.getByRole("heading", { name: task.title })).toBeVisible(),
  );
  expect(fetcher).toHaveBeenCalledTimes(4);
});
it("cancela el submit nativo y limpia el error mientras reintenta guardar", async () => {
  window.history.replaceState(null, "", route);
  let resolve!: (response: Response) => void;
  vi.spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(
      Response.json(project, { headers: { ETag: '"version"' } }),
    )
    .mockResolvedValueOnce(Response.json({ items: [], nextCursor: null }))
    .mockResolvedValueOnce(new Response(null, { status: 503 }))
    .mockImplementationOnce(
      () =>
        new Promise((done) => {
          resolve = done;
        }),
    )
    .mockResolvedValueOnce(Response.json({ items: [task], nextCursor: null }));
  render(<App />);
  const input = await screen.findByLabelText("Título de la tarea");
  fireEvent.change(input, { target: { value: task.title } });
  expect(fireEvent.submit(input.closest("form")!)).toBe(false);
  await screen.findByText("No hemos podido guardar la tarea.");
  fireEvent.click(screen.getByRole("button", { name: "Crear tarea" }));
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  await act(async () => resolve(Response.json(task, { status: 201 })));
});
it("al paginar retira la página anterior y conserva el contexto tras clic en texto", async () => {
  window.history.replaceState(null, "", route);
  let resolve!: (response: Response) => void;
  vi.spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(
      Response.json(project, { headers: { ETag: '"version"' } }),
    )
    .mockResolvedValueOnce(Response.json({ items: [task], nextCursor: "next" }))
    .mockImplementationOnce(
      () =>
        new Promise((done) => {
          resolve = done;
        }),
    );
  render(<App />);
  const more = await screen.findByRole("button", {
    name: "Más tareas antiguas",
  });
  more.focus();
  fireEvent.click(more);
  expect(
    screen.queryByRole("heading", { name: task.title }),
  ).not.toBeInTheDocument();
  expect(screen.getByText("Cargando tareas")).toBeVisible();
  fireEvent.click(screen.getByText("Pasos pequeños, con un resultado claro."));
  expect(screen.getByRole("heading", { name: "Tareas" })).not.toHaveFocus();
  await act(async () =>
    resolve(Response.json({ items: [], nextCursor: null })),
  );
  expect(screen.getByRole("heading", { name: "Tareas" })).toHaveFocus();
});
it("inicia campos vacíos y válidos con encabezado fuera del orden de tabulación", async () => {
  window.history.replaceState(null, "", route);
  vi.spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(
      Response.json(project, { headers: { ETag: '"version"' } }),
    )
    .mockResolvedValueOnce(
      Response.json({
        items: [{ ...task, estimatedMinutes: null }],
        nextCursor: null,
      }),
    );
  render(<App />);
  await screen.findByText("Sin estimación");
  for (const label of [
    "Título de la tarea",
    "Criterio de finalización",
    "Estimación en minutos",
  ]) {
    const input = screen.getByLabelText(label);
    expect(input).toHaveValue(label === "Estimación en minutos" ? null : "");
    expect(input).not.toHaveAttribute("aria-invalid", "true");
    expect(input).not.toHaveAttribute("aria-describedby");
  }
  expect(screen.getByRole("heading", { name: "Tareas" })).toHaveAttribute(
    "tabindex",
    "-1",
  );
});
it.each([
  ["Título de la tarea", "", "Escribe un título de entre 1 y 160 caracteres."],
  [
    "Criterio de finalización",
    "a".repeat(2001),
    "El criterio admite hasta 2000 caracteres.",
  ],
  [
    "Estimación en minutos",
    "1441",
    "La estimación debe ser un número entero entre 1 y 1440.",
  ],
])(
  "asocia la descripción accesible del error en %s",
  async (label, value, message) => {
    window.history.replaceState(null, "", route);
    vi.spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(
        Response.json(project, { headers: { ETag: '"version"' } }),
      )
      .mockResolvedValueOnce(Response.json({ items: [], nextCursor: null }));
    render(<App />);
    fireEvent.change(await screen.findByLabelText("Título de la tarea"), {
      target: { value: "Válido" },
    });
    const input = screen.getByLabelText(label);
    fireEvent.change(input, { target: { value } });
    fireEvent.click(screen.getByRole("button", { name: "Crear tarea" }));
    expect(input).toHaveAccessibleDescription(message);
  },
);
it.each([null, 30])(
  "la tarjeta confirmada conserva estimación %s cuando falla GET",
  async (estimatedMinutes) => {
    window.history.replaceState(null, "", route);
    vi.spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(
        Response.json(project, { headers: { ETag: '"version"' } }),
      )
      .mockResolvedValueOnce(Response.json({ items: [], nextCursor: null }))
      .mockResolvedValueOnce(
        Response.json({ ...task, estimatedMinutes }, { status: 201 }),
      )
      .mockResolvedValueOnce(new Response(null, { status: 503 }));
    render(<App />);
    fireEvent.change(await screen.findByLabelText("Título de la tarea"), {
      target: { value: task.title },
    });
    fireEvent.click(screen.getByRole("button", { name: "Crear tarea" }));
    const card = await screen.findByRole("article", {
      name: "Última tarea guardada",
    });
    expect(
      within(card).getByText(
        estimatedMinutes === null ? "Sin estimación" : "Estimación: 30 min",
      ),
    ).toBeVisible();
  },
);
it("una lista con varias tareas incluye la confirmada una sola vez", async () => {
  window.history.replaceState(null, "", route);
  const old = {
    ...task,
    id: "7c5dbd10-9ad5-4000-8000-000000000099",
    title: "Anterior",
  };
  vi.spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(
      Response.json(project, { headers: { ETag: '"version"' } }),
    )
    .mockResolvedValueOnce(Response.json({ items: [old], nextCursor: null }))
    .mockResolvedValueOnce(Response.json(task, { status: 201 }))
    .mockResolvedValueOnce(
      Response.json({ items: [task, old], nextCursor: null }),
    );
  render(<App />);
  fireEvent.change(await screen.findByLabelText("Título de la tarea"), {
    target: { value: task.title },
  });
  fireEvent.click(screen.getByRole("button", { name: "Crear tarea" }));
  await waitFor(() =>
    expect(
      within(
        screen.getByRole("list", { name: "Tareas guardadas" }),
      ).getAllByRole("listitem"),
    ).toHaveLength(2),
  );
  expect(
    screen.queryByRole("article", { name: "Última tarea guardada" }),
  ).not.toBeInTheDocument();
  expect(screen.getAllByRole("heading", { name: task.title })).toHaveLength(1);
});
it.each(["resolve", "reject"])(
  "StrictMode ignora una lectura antigua que termina con %s",
  async (outcome) => {
    let resolve!: (response: Response) => void;
    let reject!: (error: Error) => void;
    vi.spyOn(globalThis, "fetch")
      .mockImplementationOnce(
        () =>
          new Promise((done, fail) => {
            resolve = done;
            reject = fail;
          }),
      )
      .mockResolvedValueOnce(Response.json({ items: [], nextCursor: null }));
    render(
      <StrictMode>
        <ProjectTasks
          projectId={project.id}
          projectStatus="active"
          onProjectConfirmed={vi.fn()}
        />
      </StrictMode>,
    );
    await screen.findByText("Todavía no hay tareas en este proyecto.");
    await act(async () => {
      if (outcome === "resolve")
        resolve(Response.json({ items: [task], nextCursor: null }));
      else reject(new Error("old"));
    });
    expect(
      screen.getByText("Todavía no hay tareas en este proyecto."),
    ).toBeVisible();
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
    expect(
      screen.queryByRole("heading", { name: task.title }),
    ).not.toBeInTheDocument();
  },
);
it("@s10 un CSRF caducado conserva borrador y recupera token sin repetir POST", async () => {
  window.history.replaceState(null, "", route);
  const session = {
    authenticated: true,
    username: "usuario",
    csrfToken: "old",
    csrfHeaderName: "X-CSRF-TOKEN",
  };
  const fetcher = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(Response.json(session))
    .mockResolvedValueOnce(
      Response.json(project, { headers: { ETag: '"version"' } }),
    )
    .mockResolvedValueOnce(Response.json({ items: [task], nextCursor: null }))
    .mockResolvedValueOnce(
      Response.json({ code: "CSRF_INVALID" }, { status: 403 }),
    )
    .mockResolvedValueOnce(Response.json({ ...session, csrfToken: "new" }));
  render(<SessionGate />);
  fireEvent.change(await screen.findByLabelText("Título de la tarea"), {
    target: { value: "Borrador CSRF" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Crear tarea" }));
  fireEvent.click(
    await screen.findByRole("button", { name: "Recuperar acceso" }),
  );
  await waitFor(() =>
    expect(
      screen.queryByRole("button", { name: "Recuperar acceso" }),
    ).not.toBeInTheDocument(),
  );
  expect(screen.getByLabelText("Título de la tarea")).toHaveValue(
    "Borrador CSRF",
  );
  expect(
    fetcher.mock.calls.filter(([, options]) => options?.method === "POST"),
  ).toHaveLength(1);
});
it("@s14 revisar estado fallido conserva borrador y permite reintento GET", async () => {
  window.history.replaceState(null, "", route);
  const fetcher = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(
      Response.json(project, { headers: { ETag: '"version"' } }),
    )
    .mockResolvedValueOnce(Response.json({ items: [], nextCursor: null }))
    .mockResolvedValueOnce(
      Response.json({ code: "PROJECT_COMPLETED" }, { status: 409 }),
    )
    .mockResolvedValueOnce(new Response(null, { status: 503 }))
    .mockResolvedValueOnce(
      Response.json(project, { headers: { ETag: '"new"' } }),
    );
  render(<App />);
  fireEvent.change(await screen.findByLabelText("Título de la tarea"), {
    target: { value: "Conservar" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Crear tarea" }));
  fireEvent.click(
    await screen.findByRole("button", { name: "Revisar estado del proyecto" }),
  );
  await screen.findByText(
    "No hemos podido revisar el estado del proyecto. Tu borrador se conserva.",
  );
  expect(screen.getByLabelText("Título de la tarea")).toHaveValue("Conservar");
  fireEvent.click(
    screen.getByRole("button", { name: "Revisar estado del proyecto" }),
  );
  await waitFor(() =>
    expect(screen.getByRole("button", { name: "Crear tarea" })).toBeEnabled(),
  );
  expect(
    fetcher.mock.calls.filter(([, options]) => options?.method === "POST"),
  ).toHaveLength(1);
});
it("@s33 no desplaza el foco elegido mientras llega otra página", async () => {
  window.history.replaceState(null, "", route);
  let resolve!: (response: Response) => void;
  vi.spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(
      Response.json(project, { headers: { ETag: '"version"' } }),
    )
    .mockResolvedValueOnce(Response.json({ items: [task], nextCursor: "next" }))
    .mockImplementationOnce(
      () =>
        new Promise((done) => {
          resolve = done;
        }),
    );
  render(<App />);
  const more = await screen.findByRole("button", {
    name: "Más tareas antiguas",
  });
  more.focus();
  fireEvent.click(more);
  const input = screen.getByLabelText("Título de la tarea");
  input.focus();
  await act(async () =>
    resolve(Response.json({ items: [], nextCursor: null })),
  );
  expect(input).toHaveFocus();
});
it("@s2 @s3 @s5 admite límites Unicode suplementarios y texto literal sin truncarlo", async () => {
  window.history.replaceState(null, "", route);
  const title = "😀".repeat(160);
  const criterion = "😀".repeat(1990) + "\n<script>";
  const result = {
    ...task,
    title,
    completionCriterion: criterion,
    estimatedMinutes: null,
  };
  const fetcher = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(
      Response.json(project, { headers: { ETag: '"version"' } }),
    )
    .mockResolvedValueOnce(Response.json({ items: [], nextCursor: null }))
    .mockResolvedValueOnce(Response.json(result, { status: 201 }))
    .mockResolvedValueOnce(
      Response.json({ items: [result], nextCursor: null }),
    );
  render(<App />);
  const input = await screen.findByLabelText("Título de la tarea");
  fireEvent.change(input, { target: { value: "\u0085" + title + "\u2000" } });
  fireEvent.change(screen.getByLabelText("Criterio de finalización"), {
    target: { value: criterion },
  });
  expect(input).not.toHaveAttribute("maxlength");
  fireEvent.click(screen.getByRole("button", { name: "Crear tarea" }));
  await screen.findByText("Tarea guardada");
  await waitFor(() =>
    expect(
      screen.getByRole("list", { name: "Tareas guardadas" }),
    ).toHaveTextContent("<script>"),
  );
  expect(JSON.parse(fetcher.mock.calls[2][1]?.body as string)).toEqual({
    title: "\u0085" + title + "\u2000",
    completionCriterion: criterion,
    estimatedMinutes: null,
  });
  expect(document.querySelector("script")).toBeNull();
});
it("@s32 un 401 vigente retira lista y borrador mediante SessionGate real", async () => {
  window.history.replaceState(null, "", route);
  const session = {
    authenticated: true,
    username: "usuario",
    csrfToken: "token",
    csrfHeaderName: "X-CSRF-TOKEN",
  };
  vi.spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(Response.json(session))
    .mockResolvedValueOnce(
      Response.json(project, { headers: { ETag: '"version"' } }),
    )
    .mockResolvedValueOnce(Response.json({ items: [task], nextCursor: null }))
    .mockResolvedValueOnce(new Response(null, { status: 401 }))
    .mockResolvedValueOnce(
      Response.json({ ...session, authenticated: false, username: null }),
    );
  render(<SessionGate />);
  await screen.findByRole("heading", { name: task.title });
  fireEvent.change(screen.getByLabelText("Título de la tarea"), {
    target: { value: "Privado" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Crear tarea" }));
  expect(await screen.findByLabelText("Contraseña")).toBeVisible();
  expect(
    screen.queryByRole("heading", { name: task.title }),
  ).not.toBeInTheDocument();
  expect(screen.queryByDisplayValue("Privado")).not.toBeInTheDocument();
});
it("@s29 volver a recientes limpia el error de página sin quitar las acciones", async () => {
  window.history.replaceState(null, "", route);
  vi.spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(
      Response.json(project, { headers: { ETag: '"version"' } }),
    )
    .mockResolvedValueOnce(Response.json({ items: [task], nextCursor: "next" }))
    .mockResolvedValueOnce(new Response(null, { status: 503 }))
    .mockResolvedValueOnce(Response.json({ items: [task], nextCursor: null }));
  render(<App />);
  fireEvent.click(
    await screen.findByRole("button", { name: "Más tareas antiguas" }),
  );
  await screen.findByText("No hemos podido cargar las tareas.");
  expect(screen.getByRole("button", { name: "Pausar" })).toBeEnabled();
  fireEvent.click(
    screen.getByRole("button", { name: "Volver a tareas recientes" }),
  );
  await waitFor(() =>
    expect(screen.getByRole("heading", { name: task.title })).toBeVisible(),
  );
  expect(
    screen.queryByText("No hemos podido cargar las tareas."),
  ).not.toBeInTheDocument();
});
it("@s33 la paginación devuelve foco al encabezado cuando desaparece el botón", async () => {
  window.history.replaceState(null, "", route);
  vi.spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(
      Response.json(project, { headers: { ETag: '"version"' } }),
    )
    .mockResolvedValueOnce(Response.json({ items: [task], nextCursor: "next" }))
    .mockResolvedValueOnce(Response.json({ items: [], nextCursor: null }));
  render(<App />);
  const more = await screen.findByRole("button", {
    name: "Más tareas antiguas",
  });
  more.focus();
  fireEvent.click(more);
  await screen.findByText("Todavía no hay tareas en este proyecto.");
  expect(screen.getByRole("heading", { name: "Tareas" })).toHaveFocus();
});
it("@s27 @s33 los errores de campo del servidor mantienen valores y foco semántico", async () => {
  window.history.replaceState(null, "", route);
  vi.spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(
      Response.json(project, { headers: { ETag: '"version"' } }),
    )
    .mockResolvedValueOnce(Response.json({ items: [], nextCursor: null }))
    .mockResolvedValueOnce(
      Response.json(
        {
          errors: [
            {
              field: "completionCriterion",
              code: "TOO_LONG",
              message: "private",
            },
          ],
        },
        { status: 400 },
      ),
    );
  render(<App />);
  fireEvent.change(await screen.findByLabelText("Título de la tarea"), {
    target: { value: "Título" },
  });
  const input = screen.getByLabelText("Criterio de finalización");
  fireEvent.change(input, { target: { value: "Criterio exacto" } });
  fireEvent.click(screen.getByRole("button", { name: "Crear tarea" }));
  expect(
    await screen.findByText("El criterio admite hasta 2000 caracteres."),
  ).toBeVisible();
  expect(input).toHaveFocus();
  expect(input).toHaveValue("Criterio exacto");
  expect(input).toHaveAttribute("aria-invalid", "true");
  expect(screen.queryByText("private")).not.toBeInTheDocument();
});
it("@s7 distingue entrada numérica incompleta de estimación vacía voluntaria", async () => {
  window.history.replaceState(null, "", route);
  const fetcher = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(
      Response.json(project, { headers: { ETag: '"version"' } }),
    )
    .mockResolvedValueOnce(Response.json({ items: [], nextCursor: null }));
  render(<App />);
  fireEvent.change(await screen.findByLabelText("Título de la tarea"), {
    target: { value: "Válido" },
  });
  const input = screen.getByLabelText(
    "Estimación en minutos",
  ) as HTMLInputElement;
  vi.spyOn(input.validity, "badInput", "get").mockReturnValue(true);
  fireEvent.click(screen.getByRole("button", { name: "Crear tarea" }));
  expect(
    await screen.findByText(
      "La estimación debe ser un número entero entre 1 y 1440.",
    ),
  ).toBeVisible();
  expect(input).toHaveFocus();
  expect(fetcher).toHaveBeenCalledTimes(2);
});
it("@s14 conserva borrador en 409 y revisa estado sólo por decisión explícita", async () => {
  window.history.replaceState(null, "", route);
  const fetcher = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(
      Response.json(project, { headers: { ETag: '"version"' } }),
    )
    .mockResolvedValueOnce(Response.json({ items: [task], nextCursor: null }))
    .mockResolvedValueOnce(
      Response.json({ code: "PROJECT_COMPLETED" }, { status: 409 }),
    )
    .mockResolvedValueOnce(
      Response.json(
        { ...project, status: "completed" },
        { headers: { ETag: '"completed"' } },
      ),
    )
    .mockResolvedValueOnce(
      Response.json(
        { ...project, status: "paused" },
        { headers: { ETag: '"paused"' } },
      ),
    );
  render(<App />);
  fireEvent.change(await screen.findByLabelText("Título de la tarea"), {
    target: { value: "  Mi borrador  " },
  });
  fireEvent.click(screen.getByRole("button", { name: "Crear tarea" }));
  const review = await screen.findByRole("button", {
    name: "Revisar estado del proyecto",
  });
  expect(fetcher).toHaveBeenCalledTimes(3);
  expect(screen.getByLabelText("Título de la tarea")).toHaveValue(
    "  Mi borrador  ",
  );
  expect(screen.getByRole("button", { name: "Crear tarea" })).toBeDisabled();
  fireEvent.click(review);
  expect(await screen.findByText(/Reabre el proyecto en pausa/)).toBeVisible();
  expect(screen.queryByLabelText("Título de la tarea")).not.toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", { name: "Reabrir en pausa" }));
  expect(await screen.findByLabelText("Título de la tarea")).toHaveValue(
    "  Mi borrador  ",
  );
  expect(fetcher.mock.calls[3][0]).toBe(`/api/v1/projects/${project.id}`);
  expect(new Headers(fetcher.mock.calls[4][1]?.headers).get("If-Match")).toBe(
    '"completed"',
  );
});
it("@s26 mantiene tarea confirmada aunque refrescar desde página antigua falle", async () => {
  window.history.replaceState(null, "", route);
  const created = {
    ...task,
    id: "7c5dbd10-9ad5-4000-8000-000000000099",
    title: "Nueva confirmada",
  };
  const fetcher = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(
      Response.json(project, { headers: { ETag: '"version"' } }),
    )
    .mockResolvedValueOnce(Response.json({ items: [task], nextCursor: "next" }))
    .mockResolvedValueOnce(Response.json({ items: [], nextCursor: null }))
    .mockResolvedValueOnce(Response.json(created, { status: 201 }))
    .mockResolvedValueOnce(new Response(null, { status: 503 }));
  render(<App />);
  fireEvent.click(
    await screen.findByRole("button", { name: "Más tareas antiguas" }),
  );
  await screen.findByText("Todavía no hay tareas en este proyecto.");
  fireEvent.change(screen.getByLabelText("Título de la tarea"), {
    target: { value: created.title },
  });
  fireEvent.click(screen.getByRole("button", { name: "Crear tarea" }));
  expect(await screen.findByText("Tarea guardada")).toBeVisible();
  expect(
    await screen.findByRole("heading", { name: created.title }),
  ).toBeVisible();
  expect(
    await screen.findByText("No hemos podido cargar las tareas."),
  ).toBeVisible();
  expect(fetcher.mock.calls[4][0]).toBe(tasksUrl);
  expect(fetcher).toHaveBeenCalledTimes(5);
});
it("@s32 cancela POST al salir y un 401 tardío no invalida la sesión vigente", async () => {
  window.history.replaceState(null, "", route);
  let resolve!: (response: Response) => void;
  const session = {
    authenticated: true,
    username: "usuario",
    csrfToken: "token",
    csrfHeaderName: "X-CSRF-TOKEN",
  };
  const fetcher = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(Response.json(session))
    .mockResolvedValueOnce(
      Response.json(project, { headers: { ETag: '"version"' } }),
    )
    .mockResolvedValueOnce(Response.json({ items: [], nextCursor: null }))
    .mockImplementationOnce(
      () =>
        new Promise((done) => {
          resolve = done;
        }),
    )
    .mockResolvedValueOnce(Response.json({ items: [], nextCursor: null }));
  render(<SessionGate />);
  fireEvent.change(await screen.findByLabelText("Título de la tarea"), {
    target: { value: "Privado" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Crear tarea" }));
  fireEvent.click(screen.getByRole("link", { name: "Proyectos" }));
  expect((fetcher.mock.calls[3][1]?.signal as AbortSignal).aborted).toBe(true);
  await act(async () => resolve(new Response(null, { status: 401 })));
  expect(screen.getByRole("button", { name: "Cerrar sesión" })).toBeVisible();
  expect(screen.queryByLabelText("Contraseña")).not.toBeInTheDocument();
  expect(screen.queryByDisplayValue("Privado")).not.toBeInTheDocument();
});
it("@s31 aborta una lectura antigua al navegar y no acepta su respuesta tardía", async () => {
  window.history.replaceState(null, "", route);
  let resolve!: (response: Response) => void;
  const fetcher = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(
      Response.json(project, { headers: { ETag: '"version"' } }),
    )
    .mockImplementationOnce(
      () =>
        new Promise((done) => {
          resolve = done;
        }),
    )
    .mockResolvedValueOnce(Response.json({ items: [], nextCursor: null }));
  render(<App />);
  await screen.findByLabelText("Título de la tarea");
  fireEvent.change(screen.getByLabelText("Título de la tarea"), {
    target: { value: "Privado" },
  });
  fireEvent.click(screen.getByRole("link", { name: "Proyectos" }));
  expect((fetcher.mock.calls[1][1]?.signal as AbortSignal).aborted).toBe(true);
  await act(async () =>
    resolve(Response.json({ items: [task], nextCursor: null })),
  );
  expect(
    screen.queryByRole("region", { name: "Tareas" }),
  ).not.toBeInTheDocument();
  expect(screen.queryByDisplayValue("Privado")).not.toBeInTheDocument();
  expect(
    screen.queryByRole("heading", { name: task.title }),
  ).not.toBeInTheDocument();
});
it.each([
  [
    "Título de la tarea",
    "\u0085\u2000",
    "Escribe un título de entre 1 y 160 caracteres.",
  ],
  [
    "Título de la tarea",
    "😀".repeat(161),
    "Escribe un título de entre 1 y 160 caracteres.",
  ],
  [
    "Criterio de finalización",
    "😀".repeat(2001),
    "El criterio admite hasta 2000 caracteres.",
  ],
  [
    "Estimación en minutos",
    "0",
    "La estimación debe ser un número entero entre 1 y 1440.",
  ],
  [
    "Estimación en minutos",
    "1.5",
    "La estimación debe ser un número entero entre 1 y 1440.",
  ],
])(
  "@s33 valida %s y enfoca el primer campo sin enviar",
  async (label, value, message) => {
    window.history.replaceState(null, "", route);
    const fetcher = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(
        Response.json(project, { headers: { ETag: '"version"' } }),
      )
      .mockResolvedValueOnce(Response.json({ items: [], nextCursor: null }));
    render(<App />);
    fireEvent.change(await screen.findByLabelText("Título de la tarea"), {
      target: { value: "Válido" },
    });
    const input = screen.getByLabelText(label);
    fireEvent.change(input, { target: { value } });
    fireEvent.click(screen.getByRole("button", { name: "Crear tarea" }));
    expect(await screen.findByText(message)).toBeVisible();
    expect(input).toHaveAttribute("aria-invalid", "true");
    expect(input).toHaveFocus();
    expect(fetcher).toHaveBeenCalledTimes(2);
  },
);
it("@s21 @s35 navega con cursor opaco sin mezclar páginas ni perder borrador", async () => {
  window.history.replaceState(null, "", route);
  const oldTask = {
    ...task,
    id: "7c5dbd10-9ad5-4000-8000-000000000003",
    title: "Antigua",
  };
  const fetcher = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(
      Response.json(project, { headers: { ETag: '"version"' } }),
    )
    .mockResolvedValueOnce(
      Response.json({ items: [task], nextCursor: "opaque+/=token" }),
    )
    .mockResolvedValueOnce(
      Response.json({ items: [oldTask], nextCursor: null }),
    )
    .mockResolvedValueOnce(Response.json({ items: [task], nextCursor: null }));
  render(<App />);
  await screen.findByRole("heading", { name: task.title });
  fireEvent.change(screen.getByLabelText("Título de la tarea"), {
    target: { value: "Borrador" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Más tareas antiguas" }));
  expect(
    await screen.findByRole("heading", { name: oldTask.title }),
  ).toBeVisible();
  expect(
    screen.queryByRole("heading", { name: task.title }),
  ).not.toBeInTheDocument();
  expect(fetcher.mock.calls[2][0]).toBe(
    tasksUrl + "?cursor=opaque%2B%2F%3Dtoken",
  );
  expect(
    screen.queryByRole("button", { name: "Más tareas antiguas" }),
  ).not.toBeInTheDocument();
  fireEvent.click(
    screen.getByRole("button", { name: "Volver a tareas recientes" }),
  );
  await waitFor(() =>
    expect(screen.getByRole("heading", { name: task.title })).toBeVisible(),
  );
  expect(screen.getByLabelText("Título de la tarea")).toHaveValue("Borrador");
  expect(fetcher.mock.calls[3][0]).toBe(tasksUrl);
});
it("@s30 un proyecto terminado conserva tareas y sólo permite crear tras reabrir", async () => {
  window.history.replaceState(null, "", route);
  vi.spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(
      Response.json(
        { ...project, status: "completed" },
        { headers: { ETag: '"version"' } },
      ),
    )
    .mockResolvedValueOnce(Response.json({ items: [task], nextCursor: null }))
    .mockResolvedValueOnce(
      Response.json(
        { ...project, status: "paused" },
        { headers: { ETag: '"next"' } },
      ),
    );
  render(<App />);
  await waitFor(() =>
    expect(screen.getByRole("heading", { name: task.title })).toBeVisible(),
  );
  expect(screen.queryByLabelText("Título de la tarea")).not.toBeInTheDocument();
  expect(
    screen.getByText(/Reabre el proyecto en pausa para añadir tareas/),
  ).toBeVisible();
  expect(
    screen.getByText(/Terminar el proyecto no completa sus tareas pendientes/),
  ).toBeVisible();
  fireEvent.click(screen.getByRole("button", { name: "Reabrir en pausa" }));
  expect(await screen.findByLabelText("Título de la tarea")).toBeVisible();
  expect(screen.getByRole("heading", { name: task.title })).toBeVisible();
});
it("@s28 bloquea doble envío y anuncia espera sin confirmar antes del servidor", async () => {
  window.history.replaceState(null, "", route);
  let resolve!: (response: Response) => void;
  const fetcher = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(
      Response.json(project, { headers: { ETag: '"version"' } }),
    )
    .mockResolvedValueOnce(Response.json({ items: [], nextCursor: null }))
    .mockImplementationOnce(
      () =>
        new Promise((done) => {
          resolve = done;
        }),
    )
    .mockResolvedValueOnce(Response.json({ items: [task], nextCursor: null }));
  render(<App />);
  const title = await screen.findByLabelText("Título de la tarea");
  fireEvent.change(title, { target: { value: task.title } });
  const button = screen.getByRole("button", { name: "Crear tarea" });
  fireEvent.click(button);
  fireEvent.submit(button.closest("form")!);
  expect(screen.getByText("Guardando tarea")).toHaveAttribute("role", "status");
  expect(button).toBeDisabled();
  expect(screen.queryByText("Tarea guardada")).not.toBeInTheDocument();
  expect(fetcher).toHaveBeenCalledTimes(3);
  await act(async () => resolve(Response.json(task, { status: 201 })));
  expect(button).toBeEnabled();
  expect(screen.getByText("Tarea guardada")).toBeVisible();
});
it.each([400, 503, "network"])(
  "@s27 conserva el borrador exacto tras escritura fallida %s",
  async (status) => {
    window.history.replaceState(null, "", route);
    const fetcher = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(
        Response.json(project, { headers: { ETag: '"version"' } }),
      )
      .mockResolvedValueOnce(Response.json({ items: [], nextCursor: null }));
    if (typeof status === "number")
      fetcher.mockResolvedValueOnce(
        Response.json({ title: "private" }, { status }),
      );
    else fetcher.mockRejectedValueOnce(new Error("private"));
    render(<App />);
    const title = await screen.findByLabelText("Título de la tarea");
    fireEvent.change(title, { target: { value: "  portada  " } });
    fireEvent.change(screen.getByLabelText("Criterio de finalización"), {
      target: { value: "  línea\notra  " },
    });
    fireEvent.change(screen.getByLabelText("Estimación en minutos"), {
      target: { value: "31" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Crear tarea" }));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      typeof status === "number"
        ? "No hemos podido guardar la tarea"
        : "No podemos confirmar si la tarea se guardó",
    );
    expect(title).toHaveValue("  portada  ");
    expect(screen.getByLabelText("Criterio de finalización")).toHaveValue(
      "  línea\notra  ",
    );
    expect(screen.getByLabelText("Estimación en minutos")).toHaveValue(31);
    expect(screen.queryByText("Tarea guardada")).not.toBeInTheDocument();
    expect(fetcher).toHaveBeenCalledTimes(3);
    expect(screen.queryByText("private")).not.toBeInTheDocument();
  },
);
it("@s26 guarda una tarea y actualiza la colección tras confirmación real", async () => {
  window.history.replaceState(null, "", route);
  const fetcher = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(
      Response.json(project, { headers: { ETag: '"version"' } }),
    )
    .mockResolvedValueOnce(Response.json({ items: [], nextCursor: null }))
    .mockResolvedValueOnce(Response.json(task, { status: 201 }))
    .mockResolvedValueOnce(Response.json({ items: [task], nextCursor: null }));
  render(<App />);
  const title = await screen.findByLabelText("Título de la tarea");
  fireEvent.change(title, { target: { value: task.title } });
  fireEvent.change(screen.getByLabelText("Criterio de finalización"), {
    target: { value: task.completionCriterion },
  });
  fireEvent.change(screen.getByLabelText("Estimación en minutos"), {
    target: { value: "30" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Crear tarea" }));
  expect(await screen.findByText("Tarea guardada")).toHaveAttribute(
    "role",
    "status",
  );
  await waitFor(() =>
    expect(screen.getByRole("heading", { name: task.title })).toBeVisible(),
  );
  expect(fetcher.mock.calls[2]).toEqual([
    tasksUrl,
    expect.objectContaining({
      method: "POST",
      credentials: "same-origin",
      body: JSON.stringify({
        title: task.title,
        completionCriterion: task.completionCriterion,
        estimatedMinutes: 30,
      }),
    }),
  ]);
});
it("@s29 la carga de tareas no impide usar las acciones del proyecto", async () => {
  window.history.replaceState(null, "", route);
  const fetcher = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(
      Response.json(project, { headers: { ETag: '"version"' } }),
    )
    .mockImplementationOnce(() => new Promise(() => {}));
  render(<App />);
  const region = await screen.findByRole("region", { name: "Tareas" });
  expect(within(region).getByRole("status")).toHaveTextContent(
    "Cargando tareas",
  );
  expect(screen.getByRole("button", { name: "Pausar" })).toBeEnabled();
  expect(fetcher.mock.calls[1]).toEqual([
    tasksUrl,
    expect.objectContaining({ credentials: "same-origin", cache: "no-store" }),
  ]);
});
it("@s19 @s29 una colección vacía ofrece creación sin inventar tareas", async () => {
  window.history.replaceState(null, "", route);
  vi.spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(
      Response.json(project, { headers: { ETag: '"version"' } }),
    )
    .mockResolvedValueOnce(Response.json({ items: [], nextCursor: null }));
  render(<App />);
  const region = await screen.findByRole("region", { name: "Tareas" });
  expect(
    await within(region).findByText("Todavía no hay tareas en este proyecto."),
  ).toBeVisible();
  expect(within(region).getByLabelText("Título de la tarea")).toBeVisible();
  expect(
    within(region).getByLabelText("Criterio de finalización"),
  ).toBeVisible();
  expect(
    within(region).getByLabelText("Estimación en minutos"),
  ).toHaveAttribute("type", "number");
  expect(within(region).queryByRole("listitem")).not.toBeInTheDocument();
});
it("@s20 @s23 muestra los datos confirmados como tareas pendientes y estimación", async () => {
  window.history.replaceState(null, "", route);
  vi.spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(
      Response.json(project, { headers: { ETag: '"version"' } }),
    )
    .mockResolvedValueOnce(Response.json({ items: [task], nextCursor: null }));
  render(<App />);
  const list = await screen.findByRole("list", { name: "Tareas guardadas" });
  expect(within(list).getByRole("heading", { name: task.title })).toBeVisible();
  expect(within(list).getByText(task.completionCriterion)).toBeVisible();
  expect(within(list).getByText("Pendiente")).toBeVisible();
  expect(within(list).getByText("Estimación: 30 min")).toBeVisible();
  expect(
    screen.getByText(/La estimación no es tiempo trabajado/),
  ).toBeVisible();
});
it.each([503, "network"])(
  "@s25 @s29 lectura fallida %s conserva acciones y se recupera sin falso vacío",
  async (status) => {
    window.history.replaceState(null, "", route);
    const fetcher = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(
        Response.json(project, { headers: { ETag: '"version"' } }),
      );
    if (typeof status === "number")
      fetcher.mockResolvedValueOnce(
        Response.json({ title: "SQL private" }, { status }),
      );
    else fetcher.mockRejectedValueOnce(new Error("private"));
    fetcher.mockResolvedValueOnce(
      Response.json({ items: [task], nextCursor: null }),
    );
    render(<App />);
    const region = await screen.findByRole("region", { name: "Tareas" });
    expect(await within(region).findByRole("alert")).toHaveTextContent(
      "No hemos podido cargar las tareas",
    );
    expect(
      within(region).queryByText("Todavía no hay tareas en este proyecto."),
    ).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Pausar" })).toBeEnabled();
    fireEvent.click(
      within(region).getByRole("button", { name: "Reintentar tareas" }),
    );
    expect(
      await within(region).findByRole("heading", { name: task.title }),
    ).toBeVisible();
    expect(fetcher).toHaveBeenCalledTimes(3);
  },
);
