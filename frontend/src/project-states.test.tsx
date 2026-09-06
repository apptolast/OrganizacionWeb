vi.mock("./project-tasks", () => ({ ProjectTasks: () => null }));
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
const project = {
  id: "6c5dbd10-9ad5-4000-8000-000000000001",
  ownerId: "owner",
  name: "Zenit Digital",
  description: "Una idea",
  status: "idea",
  createdAt: "2026-09-05T12:00:00Z",
  updatedAt: "2026-09-05T12:00:00Z",
};
const route = `/proyectos/${project.id}`;
const etag = `"${project.id}:0"`;
afterEach(() => window.history.replaceState(null, "", "/"));
it.each([
  ["idea", "Idea"],
  ["active", "Activo"],
  ["paused", "Pausado"],
  ["completed", "Terminado"],
])("@s14 lista muestra estado real %s", async (status, label) => {
  window.history.replaceState(null, "", "/proyectos");
  vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
    Response.json({ items: [{ ...project, status }], nextCursor: null }),
  );
  render(<App />);
  expect(await screen.findByText(label)).toBeVisible();
  expect(screen.getByRole("link", { name: project.name })).toBeVisible();
});

it.each([
  ["idea", "Idea"],
  ["active", "Activo"],
  ["paused", "Pausado"],
  ["completed", "Terminado"],
])("@s14 detalle y edición conservan estado %s", async (status, label) => {
  window.history.replaceState(null, "", route);
  const fetcher = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(
      Response.json({ ...project, status }, { headers: { ETag: etag } }),
    )
    .mockResolvedValueOnce(
      Response.json({ ...project, status }, { headers: { ETag: etag } }),
    )
    .mockResolvedValueOnce(
      Response.json(
        { ...project, status, name: "Editado" },
        { headers: { ETag: '"new"' } },
      ),
    );
  render(<App />);
  expect(await screen.findByText(label)).toBeVisible();
  fireEvent.click(screen.getByRole("link", { name: "Editar proyecto" }));
  const name = await screen.findByLabelText(/Nombre del proyecto/);
  fireEvent.change(name, { target: { value: "Editado" } });
  fireEvent.click(screen.getByRole("button", { name: "Guardar cambios" }));
  await waitFor(() =>
    expect(screen.getByRole("status")).toHaveTextContent(
      "Proyecto actualizado",
    ),
  );
  expect(name).toHaveValue("Editado");
  expect(JSON.parse(fetcher.mock.calls[2][1]?.body as string)).toEqual({
    name: "Editado",
    description: project.description,
  });
});

it.each([
  ["idea", ["Activar", "Marcar terminado"]],
  ["active", ["Pausar", "Marcar terminado"]],
  ["paused", ["Retomar", "Marcar terminado"]],
  ["completed", ["Reabrir en pausa"]],
] as const)(
  "@s15 ofrece únicamente acciones válidas desde %s",
  async (status, actions) => {
    window.history.replaceState(null, "", route);
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
      Response.json({ ...project, status }, { headers: { ETag: etag } }),
    );
    render(<App />);
    await screen.findByRole("heading", { name: project.name });
    const region = screen.getByRole("region", { name: "Estado del proyecto" });
    expect(
      within(region)
        .getAllByRole("button")
        .map((button) => button.textContent),
    ).toEqual(actions);
  },
);

it.each([
  ["idea", "Activar", "active"],
  ["idea", "Marcar terminado", "completed"],
  ["active", "Pausar", "paused"],
  ["active", "Marcar terminado", "completed"],
  ["paused", "Retomar", "active"],
  ["paused", "Marcar terminado", "completed"],
  ["completed", "Reabrir en pausa", "paused"],
])(
  "@s1 @s15 %s mediante %s confirma %s con ETag del único GET",
  async (from, label, to) => {
    window.history.replaceState(null, "", route);
    const fetcher = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(
        Response.json(
          { ...project, status: from },
          { headers: { ETag: etag } },
        ),
      )
      .mockResolvedValueOnce(
        Response.json(
          { ...project, status: to, updatedAt: "2026-09-06T01:00:00Z" },
          { headers: { ETag: '"next"' } },
        ),
      );
    render(<App />);
    fireEvent.click(await screen.findByRole("button", { name: label }));
    await waitFor(() =>
      expect(screen.getByRole("status")).toHaveTextContent(
        "Estado actualizado",
      ),
    );
    expect(fetcher).toHaveBeenCalledTimes(2);
    expect(fetcher).toHaveBeenLastCalledWith(
      `/api/v1/projects/${project.id}/status`,
      expect.objectContaining({
        method: "PUT",
        credentials: "same-origin",
        cache: "no-store",
        headers: { "Content-Type": "application/json", "If-Match": etag },
        body: JSON.stringify({ status: to }),
      }),
    );
    expect(
      screen.getByText(
        { active: "Activo", paused: "Pausado", completed: "Terminado" }[to]!,
      ),
    ).toBeVisible();
  },
);

it("@s15 anuncia espera y bloquea acciones hasta confirmación sin anticipar estado", async () => {
  window.history.replaceState(null, "", route);
  let finish!: (response: Response) => void;
  const fetcher = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(Response.json(project, { headers: { ETag: etag } }))
    .mockImplementationOnce(
      () =>
        new Promise((resolve) => {
          finish = resolve;
        }),
    );
  render(<App />);
  const activate = await screen.findByRole("button", { name: "Activar" });
  fireEvent.click(activate);
  fireEvent.click(activate);
  expect(screen.getByRole("status")).toHaveTextContent("Cambiando estado");
  expect(screen.getByText("Idea", { selector: "span" })).toBeVisible();
  expect(activate).toBeDisabled();
  expect(
    screen.getByRole("button", { name: "Marcar terminado" }),
  ).toBeDisabled();
  expect(fetcher).toHaveBeenCalledTimes(2);
  await act(async () =>
    finish(
      Response.json(
        { ...project, status: "active" },
        { headers: { ETag: '"next"' } },
      ),
    ),
  );
  expect(screen.getByRole("status")).toHaveTextContent("Estado actualizado");
  expect(screen.getByRole("button", { name: "Pausar" })).toBeEnabled();
});

it.each([null, 'W/"weak"', "invalid", '"a", "b"'])(
  "@s15 impide cambiar estado sin precondición fuerte %s",
  async (tag) => {
    window.history.replaceState(null, "", route);
    const fetcher = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(
        Response.json(project, { headers: tag ? { ETag: tag } : {} }),
      );
    render(<App />);
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "No podemos cambiar el estado con esta versión",
    );
    expect(screen.getByRole("button", { name: "Activar" })).toBeDisabled();
    expect(fetcher).toHaveBeenCalledTimes(1);
    expect(
      screen.getByRole("button", { name: "Recargar versión guardada" }),
    ).toBeEnabled();
  },
);

it("@s9 @s15 conflicto conserva estado hasta recarga deliberada y usa su nuevo ETag", async () => {
  window.history.replaceState(null, "", route);
  const fetcher = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(Response.json(project, { headers: { ETag: etag } }))
    .mockResolvedValueOnce(
      Response.json({ code: "PROJECT_CONFLICT" }, { status: 412 }),
    )
    .mockResolvedValueOnce(
      Response.json(
        { ...project, status: "paused" },
        { headers: { ETag: '"new"' } },
      ),
    )
    .mockResolvedValueOnce(
      Response.json(
        { ...project, status: "active" },
        { headers: { ETag: '"next"' } },
      ),
    );
  render(<App />);
  fireEvent.click(await screen.findByRole("button", { name: "Activar" }));
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "versión más reciente",
  );
  expect(fetcher).toHaveBeenCalledTimes(2);
  expect(screen.getByText("Idea", { selector: "span" })).toBeVisible();
  fireEvent.click(
    screen.getByRole("button", { name: "Recargar versión guardada" }),
  );
  fireEvent.click(await screen.findByRole("button", { name: "Retomar" }));
  await waitFor(() =>
    expect(screen.getByRole("status")).toHaveTextContent("Estado actualizado"),
  );
  expect(fetcher.mock.calls[3][1]?.headers).toEqual({
    "Content-Type": "application/json",
    "If-Match": '"new"',
  });
});

it.each([
  [3, 3],
  [4, 2],
])(
  "@s4 @s8 @s15 límite muestra %i activos y límite %i sin pausar otro proyecto",
  async (activeCount, limit) => {
    window.history.replaceState(null, "", route);
    const fetcher = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(
        Response.json(project, { headers: { ETag: etag } }),
      )
      .mockResolvedValueOnce(
        Response.json(
          { code: "ACTIVE_PROJECT_LIMIT", activeCount, limit },
          { status: 409 },
        ),
      );
    render(<App />);
    fireEvent.click(await screen.findByRole("button", { name: "Activar" }));
    const alert = await screen.findByRole("alert");
    expect(alert).toHaveTextContent(`${activeCount} proyectos activos`);
    expect(alert).toHaveTextContent(`límite de ${limit}`);
    expect(
      screen.getByRole("link", { name: "Elegir qué pausar" }),
    ).toHaveAttribute("href", "/proyectos");
    expect(fetcher).toHaveBeenCalledTimes(2);
    expect(screen.getByText("Idea", { selector: "span" })).toBeVisible();
  },
);

it.each([400, 409, 503, 500, "network"])(
  "@s15 recupera fallo %s sin inventar éxito ni revelar detalles",
  async (status) => {
    window.history.replaceState(null, "", route);
    const fetcher = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(
        Response.json(project, { headers: { ETag: etag } }),
      );
    if (status === "network")
      fetcher.mockRejectedValueOnce(new TypeError("private"));
    else
      fetcher.mockResolvedValueOnce(
        Response.json(
          { title: "private SQL", code: "INVALID_PROJECT_TRANSITION" },
          { status: status as number },
        ),
      );
    fetcher.mockResolvedValueOnce(
      Response.json(
        { ...project, status: "active" },
        { headers: { ETag: '"next"' } },
      ),
    );
    render(<App />);
    fireEvent.click(await screen.findByRole("button", { name: "Activar" }));
    expect(await screen.findByRole("alert")).not.toHaveTextContent("private");
    expect(screen.getByText("Idea", { selector: "span" })).toBeVisible();
    expect(screen.queryByText("Estado actualizado")).not.toBeInTheDocument();
    expect(fetcher).toHaveBeenCalledTimes(2);
    fireEvent.click(screen.getByRole("button", { name: "Activar" }));
    await waitFor(() =>
      expect(screen.getByRole("status")).toHaveTextContent(
        "Estado actualizado",
      ),
    );
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  },
);

it.each([401, 404])(
  "@s15 retira datos si cambio pierde acceso %s",
  async (status) => {
    window.history.replaceState(null, "", route);
    vi.spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(
        Response.json(project, { headers: { ETag: etag } }),
      )
      .mockResolvedValueOnce(Response.json({}, { status }));
    render(<App />);
    fireEvent.click(await screen.findByRole("button", { name: "Activar" }));
    expect(await screen.findByRole("alert")).toBeVisible();
    expect(screen.getByRole("heading", { level: 1 })).toHaveTextContent(
      status === 401 ? "Autenticación requerida" : "Proyecto no encontrado",
    );
    expect(screen.queryByText(project.description)).not.toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "Activar" }),
    ).not.toBeInTheDocument();
  },
);

it.each(["success", "failure"])(
  "@s15 cancela cambio al salir e ignora respuesta antigua %s",
  async (outcome) => {
    window.history.replaceState(null, "", route);
    let finish!: (response: Response) => void;
    const other = {
      ...project,
      id: "6c5dbd10-9ad5-4000-8000-000000000002",
      name: "Otro proyecto",
    };
    const fetcher = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(
        Response.json(project, { headers: { ETag: etag } }),
      )
      .mockImplementationOnce(
        () =>
          new Promise((resolve) => {
            finish = resolve;
          }),
      )
      .mockResolvedValueOnce(
        Response.json(other, { headers: { ETag: '"other"' } }),
      );
    render(<App />);
    fireEvent.click(await screen.findByRole("button", { name: "Activar" }));
    act(() => {
      window.history.pushState(null, "", `/proyectos/${other.id}`);
      window.dispatchEvent(new PopStateEvent("popstate"));
    });
    expect((fetcher.mock.calls[1][1]?.signal as AbortSignal).aborted).toBe(
      true,
    );
    await screen.findByRole("heading", { name: other.name });
    await act(async () =>
      finish(
        outcome === "success"
          ? Response.json(
              { ...project, status: "active" },
              { headers: { ETag: '"late"' } },
            )
          : Response.json({}, { status: 401 }),
      ),
    );
    expect(screen.getByRole("heading", { name: other.name })).toBeVisible();
    expect(screen.queryByText("Estado actualizado")).not.toBeInTheDocument();
  },
);

it.each([
  [200, false],
  [200, true],
  [503, false],
])("@s16 foco tras respuesta %i con foco movido %s", async (status, moved) => {
  window.history.replaceState(null, "", route);
  let finish!: (response: Response) => void;
  vi.spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(Response.json(project, { headers: { ETag: etag } }))
    .mockImplementationOnce(
      () =>
        new Promise((resolve) => {
          finish = resolve;
        }),
    );
  render(<App />);
  const activate = await screen.findByRole("button", { name: "Activar" });
  activate.focus();
  fireEvent.click(activate);
  const edit = screen.getByRole("link", { name: "Editar proyecto" });
  if (moved) edit.focus();
  else {
    document.body.tabIndex = -1;
    document.body.focus();
    document.body.removeAttribute("tabindex");
  }
  await act(async () =>
    finish(
      status === 200
        ? Response.json(
            { ...project, status: "active" },
            { headers: { ETag: '"next"' } },
          )
        : Response.json({}, { status }),
    ),
  );
  expect(
    moved
      ? edit
      : status === 200
        ? screen.getByRole("heading", { name: "Estado del proyecto" })
        : activate,
  ).toHaveFocus();
});

it("@s15 no anuncia cambio si HTTP 200 confirma un destino diferente", async () => {
  window.history.replaceState(null, "", route);
  vi.spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(Response.json(project, { headers: { ETag: etag } }))
    .mockResolvedValueOnce(
      Response.json(
        { ...project, status: "paused" },
        { headers: { ETag: '"wrong"' } },
      ),
    );
  render(<App />);
  fireEvent.click(await screen.findByRole("button", { name: "Activar" }));
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "No podemos confirmar",
  );
  expect(screen.getByText("Idea", { selector: "span" })).toBeVisible();
  expect(screen.queryByText("Estado actualizado")).not.toBeInTheDocument();
});

it.each([
  null,
  42,
  { code: "ACTIVE_PROJECT_LIMIT", activeCount: "3", limit: 3 },
  { code: "ACTIVE_PROJECT_LIMIT", activeCount: -1, limit: 3 },
  { code: "ACTIVE_PROJECT_LIMIT", activeCount: 1.5, limit: 3 },
  { code: "ACTIVE_PROJECT_LIMIT", activeCount: 3, limit: 0 },
  { code: "ACTIVE_PROJECT_LIMIT", activeCount: 3, limit: 2.5 },
  { code: "OTHER", activeCount: 3, limit: 3 },
])("@s15 no inventa capacidad ante problema incompatible %j", async (body) => {
  window.history.replaceState(null, "", route);
  vi.spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(Response.json(project, { headers: { ETag: etag } }))
    .mockResolvedValueOnce(Response.json(body, { status: 409 }));
  render(<App />);
  fireEvent.click(await screen.findByRole("button", { name: "Activar" }));
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "El servicio no ha confirmado",
  );
  expect(
    screen.queryByRole("link", { name: "Elegir qué pausar" }),
  ).not.toBeInTheDocument();
  expect(screen.getByRole("button", { name: "Activar" })).toBeEnabled();
});
it("@s15 el segundo cambio usa ETag confirmado y no persiste proyecto ni credenciales", async () => {
  window.history.replaceState(null, "", route);
  const storage = vi.spyOn(Storage.prototype, "setItem");
  const fetcher = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(Response.json(project, { headers: { ETag: etag } }))
    .mockResolvedValueOnce(
      Response.json(
        { ...project, status: "active" },
        { headers: { ETag: '"next"' } },
      ),
    )
    .mockResolvedValueOnce(
      Response.json(
        { ...project, status: "paused" },
        { headers: { ETag: '"last"' } },
      ),
    );
  render(<App />);
  fireEvent.click(await screen.findByRole("button", { name: "Activar" }));
  fireEvent.click(await screen.findByRole("button", { name: "Pausar" }));
  await waitFor(() =>
    expect(screen.getByText("Pausado", { selector: "span" })).toBeVisible(),
  );
  expect(fetcher.mock.calls[2][1]?.headers).toEqual({
    "Content-Type": "application/json",
    "If-Match": '"next"',
  });
  expect(storage).not.toHaveBeenCalled();
});

it("@s14 rechaza un estado que imita una cadena mediante un array", async () => {
  window.history.replaceState(null, "", route);
  vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
    Response.json(
      { ...project, status: ["idea"] },
      { headers: { ETag: etag } },
    ),
  );
  render(<App />);
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "No hemos podido cargar el proyecto",
  );
  expect(
    screen.queryByRole("button", { name: "Activar" }),
  ).not.toBeInTheDocument();
});

it("@s15 un nuevo error sustituye la capacidad de un intento anterior", async () => {
  window.history.replaceState(null, "", route);
  vi.spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(Response.json(project, { headers: { ETag: etag } }))
    .mockResolvedValueOnce(
      Response.json(
        { code: "ACTIVE_PROJECT_LIMIT", activeCount: 3, limit: 3 },
        { status: 409 },
      ),
    )
    .mockResolvedValueOnce(Response.json({}, { status: 503 }));
  render(<App />);
  fireEvent.click(await screen.findByRole("button", { name: "Activar" }));
  expect(
    await screen.findByRole("link", { name: "Elegir qué pausar" }),
  ).toBeVisible();
  fireEvent.click(screen.getByRole("button", { name: "Activar" }));
  await waitFor(() =>
    expect(screen.getByRole("alert")).toHaveTextContent(
      "El servicio no ha confirmado",
    ),
  );
  expect(
    screen.queryByRole("link", { name: "Elegir qué pausar" }),
  ).not.toBeInTheDocument();
});

it("@s15 la recarga deliberada retira las acciones de la versión antigua mientras espera", async () => {
  window.history.replaceState(null, "", route);
  let finish!: (response: Response) => void;
  vi.spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(Response.json(project, { headers: { ETag: etag } }))
    .mockResolvedValueOnce(Response.json({}, { status: 412 }))
    .mockImplementationOnce(
      () =>
        new Promise((resolve) => {
          finish = resolve;
        }),
    );
  render(<App />);
  fireEvent.click(await screen.findByRole("button", { name: "Activar" }));
  fireEvent.click(
    await screen.findByRole("button", { name: "Recargar versión guardada" }),
  );
  expect(
    screen.queryByRole("button", { name: "Activar" }),
  ).not.toBeInTheDocument();
  expect(screen.getByRole("status")).toHaveTextContent("Cargando");
  await act(async () =>
    finish(
      Response.json(
        { ...project, status: "paused" },
        { headers: { ETag: '"fresh"' } },
      ),
    ),
  );
  expect(screen.getByRole("button", { name: "Retomar" })).toBeEnabled();
});

it("@s15 un cuerpo de capacidad no convierte HTTP 500 en un rechazo de límite", async () => {
  window.history.replaceState(null, "", route);
  vi.spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(Response.json(project, { headers: { ETag: etag } }))
    .mockResolvedValueOnce(
      Response.json(
        { code: "ACTIVE_PROJECT_LIMIT", activeCount: 3, limit: 3 },
        { status: 500 },
      ),
    );
  render(<App />);
  fireEvent.click(await screen.findByRole("button", { name: "Activar" }));
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "El servicio no ha confirmado",
  );
  await waitFor(() =>
    expect(screen.getByRole("button", { name: "Activar" })).toBeEnabled(),
  );
  expect(
    screen.queryByRole("link", { name: "Elegir qué pausar" }),
  ).not.toBeInTheDocument();
});

it("@s15 al perder acceso también retira el enlace de edición privado", async () => {
  window.history.replaceState(null, "", route);
  vi.spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(Response.json(project, { headers: { ETag: etag } }))
    .mockResolvedValueOnce(Response.json({}, { status: 401 }));
  render(<App />);
  fireEvent.click(await screen.findByRole("button", { name: "Activar" }));
  await screen.findByRole("alert");
  expect(
    screen.queryByRole("link", { name: "Editar proyecto" }),
  ).not.toBeInTheDocument();
});
