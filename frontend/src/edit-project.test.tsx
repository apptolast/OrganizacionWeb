vi.mock("./project-tasks", () => ({ ProjectTasks: () => null }));
import {
  render,
  screen,
  fireEvent,
  act,
  waitFor,
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
const route = `/proyectos/${project.id}/editar`;
const etag = `"${project.id}:0"`;
afterEach(() => window.history.replaceState(null, "", "/"));
it("@s17 precarga datos reales y permite cancelar al detalle", async () => {
  window.history.replaceState(null, "", route);
  const fetcher = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValue(Response.json(project, { headers: { ETag: etag } }));
  render(<App />);
  expect(await screen.findByLabelText(/Nombre del proyecto/)).toHaveValue(
    project.name,
  );
  expect(screen.getByLabelText(/Descripción/)).toHaveValue(project.description);
  expect(screen.getByRole("button", { name: "Guardar cambios" })).toBeEnabled();
  expect(screen.getByRole("link", { name: "Cancelar" })).toHaveAttribute(
    "href",
    `/proyectos/${project.id}`,
  );
  expect(fetcher).toHaveBeenCalledWith(
    `/api/v1/projects/${project.id}`,
    expect.objectContaining({
      credentials: "same-origin",
      cache: "no-store",
      signal: expect.any(AbortSignal),
    }),
  );
});

it("@s17 envía los campos con If-Match y usa la confirmación y el ETag nuevos", async () => {
  window.history.replaceState(null, "", route);
  const fetcher = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(Response.json(project, { headers: { ETag: etag } }))
    .mockResolvedValueOnce(
      Response.json(
        { ...project, name: "Confirmado" },
        { headers: { ETag: '"next"' } },
      ),
    );
  render(<App />);
  const name = await screen.findByLabelText(/Nombre del proyecto/);
  fireEvent.change(name, { target: { value: "  editado  " } });
  fireEvent.click(screen.getByRole("button", { name: "Guardar cambios" }));
  expect(await screen.findByRole("status")).toHaveTextContent(
    "Proyecto actualizado",
  );
  expect(name).toHaveValue("Confirmado");
  expect(fetcher).toHaveBeenLastCalledWith(
    `/api/v1/projects/${project.id}`,
    expect.objectContaining({
      method: "PUT",
      credentials: "same-origin",
      cache: "no-store",
      headers: { "Content-Type": "application/json", "If-Match": etag },
      body: JSON.stringify({
        name: "  editado  ",
        description: project.description,
      }),
    }),
  );
});

it("@s17 anuncia espera inmediata y bloquea doble envío hasta confirmar", async () => {
  window.history.replaceState(null, "", route);
  let finish!: (value: Response) => void;
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
  const name = await screen.findByLabelText(/Nombre del proyecto/);
  const button = screen.getByRole("button", { name: "Guardar cambios" });
  fireEvent.click(button);
  fireEvent.submit(button.closest("form")!);
  expect(screen.getByRole("status")).toHaveTextContent("Guardando cambios");
  expect(button).toBeDisabled();
  expect(name).toHaveAttribute("readonly");
  expect(fetcher).toHaveBeenCalledTimes(2);
  await act(async () =>
    finish(Response.json(project, { headers: { ETag: etag } })),
  );
  expect(button).toBeEnabled();
  expect(screen.getByRole("status")).toHaveTextContent("Proyecto actualizado");
});

it.each([400, 503, 500, "network"])(
  "@s18 conserva borrador y permite reintentar tras %s",
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
        Response.json({ title: "private SQL" }, { status: status as number }),
      );
    fetcher.mockResolvedValueOnce(
      Response.json(
        { ...project, name: "Confirmado" },
        { headers: { ETag: '"next"' } },
      ),
    );
    render(<App />);
    const name = await screen.findByLabelText(/Nombre del proyecto/);
    fireEvent.change(name, { target: { value: "  borrador exacto  " } });
    fireEvent.click(screen.getByRole("button", { name: "Guardar cambios" }));
    expect(await screen.findByRole("alert")).not.toHaveTextContent("private");
    expect(name).toHaveValue("  borrador exacto  ");
    expect(screen.queryByText("Proyecto actualizado")).not.toBeInTheDocument();
    expect(fetcher).toHaveBeenCalledTimes(2);
    fireEvent.click(screen.getByRole("button", { name: "Guardar cambios" }));
    expect(await screen.findByRole("status")).toHaveTextContent(
      "Proyecto actualizado",
    );
  },
);

it("@s19 solo recarga versión guardada mediante decisión explícita tras conflicto", async () => {
  window.history.replaceState(null, "", route);
  const fetcher = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(Response.json(project, { headers: { ETag: etag } }))
    .mockResolvedValueOnce(
      Response.json({ code: "PROJECT_CONFLICT" }, { status: 412 }),
    )
    .mockResolvedValueOnce(
      Response.json(
        { ...project, name: "Otra pestaña" },
        { headers: { ETag: '"next"' } },
      ),
    )
    .mockResolvedValueOnce(
      Response.json(
        { ...project, name: "Final" },
        { headers: { ETag: '"last"' } },
      ),
    );
  render(<App />);
  const name = await screen.findByLabelText(/Nombre del proyecto/);
  fireEvent.change(name, { target: { value: "Mi borrador" } });
  fireEvent.click(screen.getByRole("button", { name: "Guardar cambios" }));
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "versión más reciente",
  );
  expect(name).toHaveValue("Mi borrador");
  expect(fetcher).toHaveBeenCalledTimes(2);
  fireEvent.click(
    screen.getByRole("button", { name: "Recargar versión guardada" }),
  );
  await waitFor(() =>
    expect(screen.getByLabelText(/Nombre del proyecto/)).toHaveValue(
      "Otra pestaña",
    ),
  );
  fireEvent.click(screen.getByRole("button", { name: "Guardar cambios" }));
  await waitFor(() =>
    expect(screen.getByRole("status")).toHaveTextContent(
      "Proyecto actualizado",
    ),
  );
  expect(fetcher.mock.calls[3][1]).toEqual(
    expect.objectContaining({
      headers: { "Content-Type": "application/json", "If-Match": '"next"' },
    }),
  );
});

it.each([401, 404])(
  "@s20 retira datos privados si guardar responde %s",
  async (status) => {
    window.history.replaceState(null, "", route);
    vi.spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(
        Response.json(project, { headers: { ETag: etag } }),
      )
      .mockResolvedValueOnce(Response.json({}, { status }));
    render(<App />);
    await screen.findByLabelText(/Nombre del proyecto/);
    fireEvent.click(screen.getByRole("button", { name: "Guardar cambios" }));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      status === 401 ? "Autenticación requerida" : "Proyecto no encontrado",
    );
    expect(screen.queryByRole("textbox")).not.toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "Guardar cambios" }),
    ).not.toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: "Volver a proyectos" }),
    ).toHaveAttribute("href", "/proyectos");
  },
);

it.each([401, 404, 503, 500, "network"])(
  "@s20 @s23 carga fallida %s no permite guardar y ofrece recuperación",
  async (status) => {
    window.history.replaceState(null, "", route);
    const fetcher = vi.spyOn(globalThis, "fetch");
    if (status === "network")
      fetcher.mockRejectedValueOnce(new TypeError("private"));
    else
      fetcher.mockResolvedValueOnce(
        Response.json({}, { status: status as number }),
      );
    fetcher.mockResolvedValueOnce(
      Response.json(project, { headers: { ETag: etag } }),
    );
    render(<App />);
    expect(screen.getByRole("status")).toHaveTextContent("Cargando proyecto");
    const alert = await screen.findByRole("alert");
    expect(alert).not.toHaveTextContent("private");
    expect(
      screen.queryByRole("button", { name: "Guardar cambios" }),
    ).not.toBeInTheDocument();
    if (status === 401 || status === 404)
      expect(alert).toHaveTextContent(
        status === 401 ? "Autenticación requerida" : "Proyecto no encontrado",
      );
    else {
      fireEvent.click(screen.getByRole("button", { name: "Reintentar" }));
      expect(await screen.findByLabelText(/Nombre del proyecto/)).toHaveValue(
        project.name,
      );
    }
  },
);

it.each([null, 'W/"weak"', "*", "unquoted", '"one", "two"'])(
  "@s23 no permite editar sin ETag fuerte válido: %s",
  async (tag) => {
    window.history.replaceState(null, "", route);
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
      Response.json(project, { headers: tag ? { ETag: tag } : {} }),
    );
    render(<App />);
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "No hemos podido cargar",
    );
    expect(
      screen.queryByRole("button", { name: "Guardar cambios" }),
    ).not.toBeInTheDocument();
  },
);

it.each([
  { ...project, id: "otro" },
  { ...project, updatedAt: "invalid" },
  { ...project, ownerId: 42 },
  null,
])("@s23 rechaza detalle incompatible %j", async (body) => {
  window.history.replaceState(null, "", route);
  vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
    Response.json(body, { headers: { ETag: etag } }),
  );
  render(<App />);
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "No hemos podido cargar",
  );
  expect(screen.queryByRole("textbox")).not.toBeInTheDocument();
});

it.each(["resolve", "reject"])(
  "@s23 ignora respuesta antigua %s durante StrictMode",
  async (outcome) => {
    window.history.replaceState(null, "", route);
    let resolve!: (value: Response) => void;
    let reject!: (error: Error) => void;
    vi.spyOn(globalThis, "fetch")
      .mockImplementationOnce(
        () =>
          new Promise((a, b) => {
            resolve = a;
            reject = b;
          }),
      )
      .mockResolvedValueOnce(
        Response.json(project, { headers: { ETag: etag } }),
      );
    render(
      <StrictMode>
        <App />
      </StrictMode>,
    );
    const name = await screen.findByLabelText(/Nombre del proyecto/);
    await act(async () => {
      if (outcome === "resolve")
        resolve(
          Response.json(
            { ...project, name: "obsoleto" },
            { headers: { ETag: '"old"' } },
          ),
        );
      else reject(new Error("old"));
    });
    expect(name).toHaveValue(project.name);
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  },
);
import { StrictMode } from "react";

it("@s19 bloquea guardar durante recarga deliberada y conserva borrador si esta falla", async () => {
  window.history.replaceState(null, "", route);
  let finish!: (value: Response) => void;
  const fetcher = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(Response.json(project, { headers: { ETag: etag } }))
    .mockResolvedValueOnce(Response.json({}, { status: 412 }))
    .mockImplementationOnce(
      () =>
        new Promise((resolve) => {
          finish = resolve;
        }),
    );
  render(<App />);
  const name = await screen.findByLabelText(/Nombre del proyecto/);
  fireEvent.change(name, { target: { value: "borrador" } });
  fireEvent.click(screen.getByRole("button", { name: "Guardar cambios" }));
  fireEvent.click(
    await screen.findByRole("button", { name: "Recargar versión guardada" }),
  );
  const save = screen.getByRole("button", { name: "Guardar cambios" });
  expect(save).toBeDisabled();
  fireEvent.submit(save.closest("form")!);
  expect(fetcher).toHaveBeenCalledTimes(3);
  expect(screen.getByRole("status")).toHaveTextContent("Cargando proyecto");
  await act(async () => finish(Response.json({}, { status: 503 })));
  expect(name).toHaveValue("borrador");
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "Conservamos tu borrador",
  );
});

it("@s18 @s22 asocia validación con campos y enfoca el primero sin revelar mensajes internos", async () => {
  window.history.replaceState(null, "", route);
  vi.spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(Response.json(project, { headers: { ETag: etag } }))
    .mockResolvedValueOnce(
      Response.json(
        {
          code: "VALIDATION_ERROR",
          errors: [
            { field: "name", message: "private SQL" },
            { field: "description", message: "private" },
          ],
        },
        { status: 400 },
      ),
    );
  render(<App />);
  const name = await screen.findByLabelText(/Nombre del proyecto/);
  fireEvent.change(name, { target: { value: " " } });
  fireEvent.click(screen.getByRole("button", { name: "Guardar cambios" }));
  await screen.findByRole("alert");
  expect(name).toHaveAttribute("aria-invalid", "true");
  expect(name).toHaveFocus();
  expect(name).toHaveAccessibleDescription(/nombre/i);
  expect(screen.getByLabelText(/Descripción/)).toHaveAttribute(
    "aria-invalid",
    "true",
  );
  expect(document.body).not.toHaveTextContent("private");
});

it("@s23 cancela PUT al salir y su respuesta no reemplaza otro proyecto", async () => {
  window.history.replaceState(null, "", route);
  let finish!: (value: Response) => void;
  const other = {
    ...project,
    id: "6c5dbd10-9ad5-4000-8000-000000000002",
    name: "Otro",
  };
  const fetcher = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(Response.json(project, { headers: { ETag: etag } }))
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
  await screen.findByLabelText(/Nombre del proyecto/);
  fireEvent.click(screen.getByRole("button", { name: "Guardar cambios" }));
  act(() => {
    window.history.pushState(null, "", `/proyectos/${other.id}/editar`);
    window.dispatchEvent(new PopStateEvent("popstate"));
  });
  expect((fetcher.mock.calls[1][1]?.signal as AbortSignal).aborted).toBe(true);
  expect(await screen.findByLabelText(/Nombre del proyecto/)).toHaveValue(
    "Otro",
  );
  await act(async () =>
    finish(Response.json(project, { headers: { ETag: etag } })),
  );
  expect(screen.getByLabelText(/Nombre del proyecto/)).toHaveValue("Otro");
  expect(screen.queryByText("Proyecto actualizado")).not.toBeInTheDocument();
});

it("@s17 enlaza editar desde detalle y deja de anunciar confirmación al escribir", async () => {
  window.history.replaceState(null, "", `/proyectos/${project.id}`);
  vi.spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(Response.json(project))
    .mockResolvedValueOnce(Response.json(project, { headers: { ETag: etag } }))
    .mockResolvedValueOnce(Response.json(project, { headers: { ETag: etag } }));
  render(<App />);
  fireEvent.click(await screen.findByRole("link", { name: "Editar proyecto" }));
  const name = await screen.findByLabelText(/Nombre del proyecto/);
  fireEvent.click(screen.getByRole("button", { name: "Guardar cambios" }));
  await waitFor(() =>
    expect(screen.getByRole("status")).toHaveTextContent(
      "Proyecto actualizado",
    ),
  );
  fireEvent.change(name, { target: { value: "Otra edición" } });
  expect(screen.queryByText("Proyecto actualizado")).not.toBeInTheDocument();
});

it("@s22 devuelve foco al botón tras guardar sin robarlo si se cambió de control", async () => {
  window.history.replaceState(null, "", route);
  let finish!: (value: Response) => void;
  vi.spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(Response.json(project, { headers: { ETag: etag } }))
    .mockImplementationOnce(
      () =>
        new Promise((resolve) => {
          finish = resolve;
        }),
    );
  render(<App />);
  await screen.findByLabelText(/Nombre del proyecto/);
  const save = screen.getByRole("button", { name: "Guardar cambios" });
  save.focus();
  fireEvent.click(save);
  document.body.tabIndex = -1;
  document.body.focus();
  document.body.removeAttribute("tabindex");
  expect(document.body).toHaveFocus();
  await act(async () =>
    finish(Response.json(project, { headers: { ETag: etag } })),
  );
  expect(save).toHaveFocus();
});

it("@s19 identifica fallo de lectura al recargar y conserva el borrador", async () => {
  window.history.replaceState(null, "", route);
  vi.spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(Response.json(project, { headers: { ETag: etag } }))
    .mockResolvedValueOnce(Response.json({}, { status: 412 }))
    .mockRejectedValueOnce(new TypeError("red"));
  render(<App />);
  const name = await screen.findByLabelText(/Nombre del proyecto/);
  fireEvent.change(name, { target: { value: "borrador" } });
  fireEvent.click(screen.getByRole("button", { name: "Guardar cambios" }));
  fireEvent.click(
    await screen.findByRole("button", { name: "Recargar versión guardada" }),
  );
  await waitFor(() =>
    expect(screen.getByRole("alert")).toHaveTextContent(
      "No hemos podido cargar la versión guardada",
    ),
  );
  expect(name).toHaveValue("borrador");
  expect(
    screen.getByRole("button", { name: "Recargar versión guardada" }),
  ).toBeEnabled();
});

it.each(["missing-etag", "wrong-id", "invalid-json", "status201"])(
  "@s18 no confirma PUT incompatible %s y conserva borrador",
  async (kind) => {
    window.history.replaceState(null, "", route);
    const invalid =
      kind === "invalid-json"
        ? new Response("proxy HTML", { headers: { ETag: etag } })
        : Response.json(
            { ...project, id: kind === "wrong-id" ? "other" : project.id },
            {
              status: kind === "status201" ? 201 : 200,
              headers: kind === "missing-etag" ? {} : { ETag: etag },
            },
          );
    vi.spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(
        Response.json(project, { headers: { ETag: etag } }),
      )
      .mockResolvedValueOnce(invalid);
    render(<App />);
    const name = await screen.findByLabelText(/Nombre del proyecto/);
    fireEvent.change(name, { target: { value: "borrador exacto" } });
    fireEvent.click(screen.getByRole("button", { name: "Guardar cambios" }));
    await screen.findByRole("alert");
    expect(name).toHaveValue("borrador exacto");
    expect(screen.queryByText("Proyecto actualizado")).not.toBeInTheDocument();
  },
);
it("@s7 @s21 guarda Unicode literal sin almacenamiento persistente y respeta foco movido", async () => {
  window.history.replaceState(null, "", route);
  let finish!: (value: Response) => void;
  const storage = vi.spyOn(Storage.prototype, "setItem");
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
  const name = await screen.findByLabelText(/Nombre del proyecto/);
  const description = screen.getByLabelText(/Descripción/);
  fireEvent.change(name, { target: { value: "🧠".repeat(120) } });
  fireEvent.change(description, {
    target: { value: "<script>literal</script>\n  " },
  });
  const save = screen.getByRole("button", { name: "Guardar cambios" });
  save.focus();
  fireEvent.click(save);
  description.focus();
  await act(async () =>
    finish(
      Response.json(
        {
          ...project,
          name: "🧠".repeat(120),
          description: "<script>literal</script>\n  ",
        },
        { headers: { ETag: etag } },
      ),
    ),
  );
  expect(description).toHaveFocus();
  expect(description).toHaveValue("<script>literal</script>\n  ");
  expect(document.querySelector("script")).toBeNull();
  expect(storage).not.toHaveBeenCalled();
  expect(JSON.parse(fetcher.mock.calls[1][1]?.body as string)).toEqual({
    name: "🧠".repeat(120),
    description: "<script>literal</script>\n  ",
  });
});

it("@s22 al entrar desde enlace enfoca encabezado sin robar foco durante guardado", async () => {
  window.history.replaceState(null, "", `/proyectos/${project.id}`);
  vi.spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(Response.json(project))
    .mockResolvedValueOnce(Response.json(project, { headers: { ETag: etag } }));
  render(<App />);
  const edit = await screen.findByRole("link", { name: "Editar proyecto" });
  edit.focus();
  fireEvent.click(edit);
  expect(
    screen.getByRole("heading", { name: "Editar proyecto" }),
  ).toHaveFocus();
  const name = await screen.findByLabelText(/Nombre del proyecto/);
  name.focus();
  fireEvent.change(name, { target: { value: "nuevo" } });
  expect(name).toHaveFocus();
});

it("reutiliza ETag confirmado en un segundo guardado y cancela submit nativo", async () => {
  window.history.replaceState(null, "", route);
  const fetcher = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(Response.json(project, { headers: { ETag: etag } }))
    .mockResolvedValueOnce(
      Response.json(project, { headers: { ETag: '"second"' } }),
    )
    .mockResolvedValueOnce(
      Response.json(project, { headers: { ETag: '"third"' } }),
    );
  render(<App />);
  await screen.findByLabelText(/Nombre del proyecto/);
  const form = screen
    .getByRole("button", { name: "Guardar cambios" })
    .closest("form")!;
  expect(fireEvent.submit(form)).toBe(false);
  await waitFor(() =>
    expect(screen.getByRole("status")).toHaveTextContent(
      "Proyecto actualizado",
    ),
  );
  fireEvent.submit(form);
  await waitFor(() => expect(fetcher).toHaveBeenCalledTimes(3));
  expect(fetcher.mock.calls[2][1]?.headers).toEqual({
    "Content-Type": "application/json",
    "If-Match": '"second"',
  });
  await waitFor(() =>
    expect(screen.getByRole("status")).toHaveTextContent(
      "Proyecto actualizado",
    ),
  );
});
it("retira error anterior durante reintento y después de confirmación", async () => {
  window.history.replaceState(null, "", route);
  let finish!: (value: Response) => void;
  vi.spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(Response.json(project, { headers: { ETag: etag } }))
    .mockResolvedValueOnce(Response.json({}, { status: 503 }))
    .mockImplementationOnce(
      () =>
        new Promise((resolve) => {
          finish = resolve;
        }),
    );
  render(<App />);
  await screen.findByLabelText(/Nombre del proyecto/);
  const save = screen.getByRole("button", { name: "Guardar cambios" });
  fireEvent.click(save);
  await screen.findByRole("alert");
  fireEvent.click(save);
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  await act(async () =>
    finish(Response.json(project, { headers: { ETag: etag } })),
  );
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
});
it.each([
  null,
  42,
  { errors: "invalid" },
  { errors: [null, 42, {}, { field: "unknown" }] },
])(
  "problema400 incompatible %j conserva borrador sin fallos de render",
  async (body) => {
    window.history.replaceState(null, "", route);
    vi.spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(
        Response.json(project, { headers: { ETag: etag } }),
      )
      .mockResolvedValueOnce(Response.json(body, { status: 400 }));
    render(<App />);
    const name = await screen.findByLabelText(/Nombre del proyecto/);
    fireEvent.click(screen.getByRole("button", { name: "Guardar cambios" }));
    await screen.findByRole("alert");
    expect(name).toHaveValue(project.name);
    expect(name).toHaveAttribute("aria-invalid", "false");
    expect(screen.getByLabelText(/Descripción/)).toHaveAttribute(
      "aria-invalid",
      "false",
    );
    expect(
      screen.getByRole("button", { name: "Guardar cambios" }),
    ).toBeEnabled();
  },
);
