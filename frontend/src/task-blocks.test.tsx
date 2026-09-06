import { StrictMode } from "react";
import {
  act,
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { App } from "./App";
import { SessionGate } from "./session-gate";

const project = {
  id: "6c5dbd10-9ad5-4000-8000-000000000001",
  ownerId: "Pablo",
  name: "Zenit",
  description: "",
  status: "active",
  createdAt: "2026-09-06T12:00:00Z",
  updatedAt: "2026-09-06T12:00:00Z",
};
const task = {
  id: "7c5dbd10-9ad5-4000-8000-000000000002",
  projectId: project.id,
  title: "Preparar portada",
  completionCriterion: "Revisable",
  estimatedMinutes: 30,
  status: "pending",
  createdAt: project.createdAt,
  updatedAt: project.updatedAt,
};
const api = `/api/v1/projects/${project.id}/tasks/${task.id}`;
const route = `/proyectos/${project.id}/tareas/${task.id}`;
const pending = {
  status: "pending",
  completedAt: null,
  updatedAt: task.updatedAt,
};
const preview = {
  objective: "Preparar borrador",
  zoneId: "UTC",
  startAt: "2030-01-07T10:00:00Z",
  endAt: "2030-01-07T11:00:00Z",
  startOffset: "Z",
  endOffset: "Z",
  durationMinutes: 60,
  availabilityEtag: '"availability:6c5dbd10-9ad5-4000-8000-000000000001:0"',
  budgetZoneId: "Europe/Madrid",
  days: [
    {
      date: "2030-01-07",
      budgetMinutes: 120,
      plannedSeconds: 0,
      requestedSeconds: 3600,
      excessSeconds: 0,
    },
  ],
};
const block = {
  id: "8c5dbd10-9ad5-4000-8000-000000000003",
  projectId: project.id,
  taskId: task.id,
  objective: preview.objective,
  startAt: preview.startAt,
  endAt: preview.endAt,
  zoneId: preview.zoneId,
  durationMinutes: preview.durationMinutes,
  createdAt: "2026-09-06T12:00:00.000001Z",
};
function problem(code: string, status: number, extra = {}) {
  return Response.json(
    {
      type: `urn:organization:problem:${code.toLowerCase()}`,
      title: "No se pudo realizar la operación",
      status,
      code,
      ...extra,
    },
    { status, headers: { "Content-Type": "application/problem+json" } },
  );
}
async function openEditor() {
  fireEvent.click(
    await screen.findByRole("button", { name: "Planificar bloque" }),
  );
  await waitFor(() =>
    expect(screen.getByLabelText("Zona del bloque")).toHaveValue(
      "Europe/Madrid",
    ),
  );
  fireEvent.change(screen.getByLabelText("Objetivo del bloque"), {
    target: { value: " Preparar borrador " },
  });
  fireEvent.change(screen.getByLabelText("Inicio del bloque"), {
    target: { value: "2030-01-07T10:00" },
  });
  fireEvent.change(screen.getByLabelText("Fin del bloque"), {
    target: { value: "2030-01-07T11:00" },
  });
  fireEvent.change(screen.getByLabelText("Zona del bloque"), {
    target: { value: "UTC" },
  });
}
function fixture(
  override: (
    url: string,
    options?: RequestInit,
  ) => Response | Promise<Response> | undefined = () => undefined,
) {
  return vi
    .spyOn(globalThis, "fetch")
    .mockImplementation(async (input, options) => {
      const url = String(input);
      const response = override(url, options);
      if (response) return response;
      if (url === "/api/v1/me/availability/zones")
        return Response.json({ items: ["Europe/Madrid", "UTC"] });
      if (url === "/api/v1/me/availability")
        return Response.json(
          {
            configured: true,
            zoneId: "Europe/Madrid",
            dailyMinutes: {
              MONDAY: 120,
              TUESDAY: 120,
              WEDNESDAY: 120,
              THURSDAY: 120,
              FRIDAY: 120,
              SATURDAY: 0,
              SUNDAY: 0,
            },
            updatedAt: task.updatedAt,
          },
          {
            headers: {
              ETag: '"availability:6c5dbd10-9ad5-4000-8000-000000000001:0"',
            },
          },
        );
      if (url === `/api/v1/projects/${project.id}`)
        return Response.json(project, { headers: { ETag: '"project"' } });
      if (url === `${api}/parent`) return Response.json({ parent: null });
      if (url === `${api}/status`)
        return Response.json(pending, {
          headers: { ETag: `"task:${task.id}:0"` },
        });
      if (
        url.endsWith("/subtasks") ||
        url.endsWith("/history") ||
        url.endsWith("/blocks")
      )
        return Response.json({ items: [], nextCursor: null });
      return Response.json(task);
    });
}
afterEach(() => window.history.replaceState(null, "", "/"));

it("@s50 IDEMPOTENCY_CONFLICT conserva key e intención bloqueadas y sólo consulta su resultado", async () => {
  window.history.replaceState(null, "", route);
  const fetcher = fixture((url, options) =>
    url.endsWith("/preview")
      ? Response.json(preview)
      : url.includes("/by-request/")
        ? Response.json(block)
        : url === `${api}/blocks` && options?.method === "POST"
          ? problem("IDEMPOTENCY_CONFLICT", 409)
          : undefined,
  );
  render(<App />);
  await openEditor();
  fireEvent.click(screen.getByRole("button", { name: "Revisar bloque" }));
  await screen.findByRole("region", { name: "Revisión del bloque" });
  fireEvent.click(screen.getByRole("button", { name: "Guardar bloque" }));
  const check = await screen.findByRole("button", {
    name: "Comprobar guardado",
  });
  expect(screen.getByLabelText("Objetivo del bloque")).toBeDisabled();
  expect(
    screen.queryByRole("button", { name: "Reenviar el mismo bloque" }),
  ).not.toBeInTheDocument();
  fireEvent.click(check);
  await screen.findByText("Bloque guardado");
  const writes = fetcher.mock.calls.filter(
    ([url, options]) => url === `${api}/blocks` && options?.method === "POST",
  );
  expect(writes).toHaveLength(1);
  const key = new Headers(writes[0][1]?.headers).get("Idempotency-Key");
  expect(
    fetcher.mock.calls.filter(
      ([url]) => url === `${api}/blocks/by-request/${key}`,
    ),
  ).toHaveLength(1);
});

it("@s52 una reconsulta fallida del conflicto retira detalles anteriores y permite reintentar", async () => {
  window.history.replaceState(null, "", route);
  let reads = 0;
  fixture((url) =>
    url.endsWith("/preview")
      ? problem("BLOCK_OVERLAP", 409, {
          conflict: {
            id: block.id,
            projectId: block.projectId,
            taskId: block.taskId,
          },
        })
      : url.endsWith(`/blocks/${block.id}`)
        ? ++reads === 1
          ? Response.json({ ...block, objective: "Reserva privada anterior" })
          : problem("RESOURCE_NOT_FOUND", 404)
        : undefined,
  );
  render(<App />);
  await openEditor();
  fireEvent.click(screen.getByRole("button", { name: "Revisar bloque" }));
  const consult = await screen.findByRole("button", {
    name: "Consultar bloque en conflicto",
  });
  fireEvent.click(consult);
  await screen.findByRole("heading", { name: "Reserva privada anterior" });
  fireEvent.click(consult);
  await screen.findByText(
    "No se pudo consultar el bloque en conflicto. Puedes volver a intentarlo.",
  );
  expect(
    screen.queryByText("Reserva privada anterior"),
  ).not.toBeInTheDocument();
  expect(consult).toBeEnabled();
});

it("@s56 completar proyecto mantiene intención incierta y permite confirmarla sin nueva creación", async () => {
  window.history.replaceState(null, "", route);
  const fetcher = fixture((url, options) => {
    if (
      url === `/api/v1/projects/${project.id}/status` &&
      options?.method === "PUT"
    )
      return Response.json(
        { ...project, status: "completed" },
        { headers: { ETag: '"project:1"' } },
      );
    if (url.endsWith("/preview")) return Response.json(preview);
    if (url.includes("/by-request/")) return Response.json(block);
    if (url === `${api}/blocks` && options?.method === "POST")
      return problem("STORAGE_UNAVAILABLE", 503);
  });
  render(<App />);
  await openEditor();
  fireEvent.click(screen.getByRole("button", { name: "Revisar bloque" }));
  await screen.findByRole("region", { name: "Revisión del bloque" });
  fireEvent.click(screen.getByRole("button", { name: "Guardar bloque" }));
  const check = await screen.findByRole("button", {
    name: "Comprobar guardado",
  });
  fireEvent.click(screen.getByRole("button", { name: "Marcar terminado" }));
  await screen.findByRole("button", { name: "Reabrir en pausa" });
  expect(check).toBeEnabled();
  expect(screen.getByLabelText("Objetivo del bloque")).toHaveValue(
    " Preparar borrador ",
  );
  fireEvent.click(check);
  await screen.findByText("Bloque guardado");
  expect(
    screen.queryByRole("button", { name: "Planificar bloque" }),
  ).not.toBeInTheDocument();
  expect(
    fetcher.mock.calls.filter(
      ([url, options]) => url === `${api}/blocks` && options?.method === "POST",
    ),
  ).toHaveLength(1);
  expect(
    fetcher.mock.calls.filter(
      ([url]) => url === `/api/v1/projects/${project.id}`,
    ),
  ).toHaveLength(1);
});

it("@s52 SessionGate retira lista e intención incierta al perder sesión durante recuperación", async () => {
  window.history.replaceState(null, "", route);
  let sessions = 0;
  const fetcher = fixture((url, options) => {
    if (url === "/api/session")
      return Response.json({
        authenticated: ++sessions === 1,
        username: sessions === 1 ? "Pablo" : null,
        csrfToken: "token",
        csrfHeaderName: "X-CSRF-TOKEN",
      });
    if (url.endsWith("/preview")) return Response.json(preview);
    if (url.includes("/by-request/")) return problem("UNAUTHORIZED", 401);
    if (url === `${api}/blocks`)
      return options?.method === "POST"
        ? problem("STORAGE_UNAVAILABLE", 503)
        : Response.json({
            items: [{ ...block, objective: "Reserva privada" }],
            nextCursor: null,
          });
  });
  render(<SessionGate />);
  await openEditor();
  await screen.findByRole("heading", { name: "Reserva privada" });
  fireEvent.click(screen.getByRole("button", { name: "Revisar bloque" }));
  await screen.findByRole("region", { name: "Revisión del bloque" });
  fireEvent.click(screen.getByRole("button", { name: "Guardar bloque" }));
  fireEvent.click(
    await screen.findByRole("button", { name: "Comprobar guardado" }),
  );
  await screen.findByLabelText("Contraseña");
  expect(
    screen.queryByRole("region", { name: "Bloques planificados" }),
  ).not.toBeInTheDocument();
  expect(screen.queryByText("Reserva privada")).not.toBeInTheDocument();
  expect(
    screen.queryByDisplayValue(" Preparar borrador "),
  ).not.toBeInTheDocument();
  expect(
    fetcher.mock.calls.filter(
      ([url, options]) => url === `${api}/blocks` && options?.method === "POST",
    ),
  ).toHaveLength(1);
});

it("@s58 comprobar creación confirmada restaura foco al desaparecer la recuperación", async () => {
  window.history.replaceState(null, "", route);
  fixture((url, options) =>
    url.endsWith("/preview")
      ? Response.json(preview)
      : url.includes("/by-request/")
        ? Response.json(block)
        : url === `${api}/blocks` && options?.method === "POST"
          ? problem("STORAGE_UNAVAILABLE", 503)
          : undefined,
  );
  render(<App />);
  await openEditor();
  fireEvent.click(screen.getByRole("button", { name: "Revisar bloque" }));
  await screen.findByRole("region", { name: "Revisión del bloque" });
  fireEvent.click(screen.getByRole("button", { name: "Guardar bloque" }));
  const check = await screen.findByRole("button", {
    name: "Comprobar guardado",
  });
  check.focus();
  fireEvent.click(check);
  await screen.findByText("Bloque guardado");
  expect(
    screen.getByRole("heading", { name: "Bloques planificados" }),
  ).toHaveFocus();
});

it("@s58 confirmar creación lleva foco al encabezado al desaparecer Guardar", async () => {
  window.history.replaceState(null, "", route);
  fixture((url, options) =>
    url.endsWith("/preview")
      ? Response.json(preview)
      : url === `${api}/blocks` && options?.method === "POST"
        ? Response.json(block, {
            status: 201,
            headers: { Location: `${api}/blocks/${block.id}` },
          })
        : undefined,
  );
  render(<App />);
  await openEditor();
  fireEvent.click(screen.getByRole("button", { name: "Revisar bloque" }));
  await screen.findByRole("region", { name: "Revisión del bloque" });
  const save = screen.getByRole("button", { name: "Guardar bloque" });
  save.focus();
  fireEvent.click(save);
  await screen.findByText("Bloque guardado");
  expect(
    screen.getByRole("heading", { name: "Bloques planificados" }),
  ).toHaveFocus();
});

it("@s49 consulta el bloque propio que causa solape sin perder el borrador", async () => {
  window.history.replaceState(null, "", route);
  const conflict = {
    ...block,
    objective: "Reserva previa",
    projectId: "6c5dbd10-9ad5-4000-8000-000000000009",
    taskId: "7c5dbd10-9ad5-4000-8000-000000000009",
  };
  const fetcher = fixture((url) =>
    url.endsWith("/preview")
      ? problem("BLOCK_OVERLAP", 409, {
          conflict: {
            id: conflict.id,
            projectId: conflict.projectId,
            taskId: conflict.taskId,
          },
        })
      : url.endsWith(`/blocks/${conflict.id}`)
        ? Response.json(conflict)
        : undefined,
  );
  render(<App />);
  await openEditor();
  fireEvent.click(screen.getByRole("button", { name: "Revisar bloque" }));
  fireEvent.click(
    await screen.findByRole("button", {
      name: "Consultar bloque en conflicto",
    }),
  );
  expect(
    await screen.findByRole("heading", { name: "Reserva previa" }),
  ).toBeVisible();
  expect(
    fetcher.mock.calls.some(
      ([url]) =>
        url ===
        `/api/v1/projects/${conflict.projectId}/tasks/${conflict.taskId}/blocks/${conflict.id}`,
    ),
  ).toBe(true);
  expect(screen.getByLabelText("Objetivo del bloque")).toHaveValue(
    " Preparar borrador ",
  );
});

it("@s52 RESOURCE_NOT_FOUND del listado retira el contexto y borrador privado", async () => {
  window.history.replaceState(null, "", route);
  let finish!: (response: Response) => void;
  fixture((url) =>
    url === `${api}/blocks`
      ? new Promise((resolve) => {
          finish = resolve;
        })
      : undefined,
  );
  render(<App />);
  await openEditor();
  await act(async () => finish(problem("RESOURCE_NOT_FOUND", 404)));
  expect(
    await screen.findByText("Esta tarea no está disponible para tu cuenta."),
  ).toBeVisible();
  expect(
    screen.queryByRole("region", { name: "Bloques planificados" }),
  ).not.toBeInTheDocument();
  expect(screen.queryByText(task.title)).not.toBeInTheDocument();
  expect(
    screen.queryByDisplayValue(" Preparar borrador "),
  ).not.toBeInTheDocument();
});

it("@s49 un solape definitivo retira revisión y permite corregir sin incertidumbre", async () => {
  window.history.replaceState(null, "", route);
  fixture((url, options) =>
    url.endsWith("/preview")
      ? Response.json(preview)
      : url === `${api}/blocks` && options?.method === "POST"
        ? problem("BLOCK_OVERLAP", 409, {
            conflict: { id: block.id, projectId: project.id, taskId: task.id },
          })
        : undefined,
  );
  render(<App />);
  await openEditor();
  fireEvent.click(screen.getByRole("button", { name: "Revisar bloque" }));
  await screen.findByRole("region", { name: "Revisión del bloque" });
  fireEvent.click(screen.getByRole("button", { name: "Guardar bloque" }));
  await screen.findByText(
    "No se guardó el bloque. Revisa los datos antes de volver a guardar.",
  );
  expect(screen.getByLabelText("Objetivo del bloque")).toBeEnabled();
  expect(screen.getByLabelText("Objetivo del bloque")).toHaveValue(
    " Preparar borrador ",
  );
  expect(
    screen.queryByRole("region", { name: "Revisión del bloque" }),
  ).not.toBeInTheDocument();
  expect(
    screen.queryByRole("button", { name: "Comprobar guardado" }),
  ).not.toBeInTheDocument();
  expect(screen.getByRole("button", { name: "Guardar bloque" })).toBeDisabled();
});

it.each([
  ["preview", false],
  ["preview", true],
  ["create", false],
  ["create", true],
])(
  "@s58 restaura destino útil después de %s sin robar foco externo=%s",
  async (operation, moved) => {
    window.history.replaceState(null, "", route);
    let finish!: (response: Response) => void;
    fixture((url, options) => {
      if (url.endsWith("/preview"))
        return operation === "preview"
          ? new Promise((resolve) => {
              finish = resolve;
            })
          : Response.json(preview);
      if (url === `${api}/blocks` && options?.method === "POST")
        return new Promise((resolve) => {
          finish = resolve;
        });
    });
    render(<App />);
    await openEditor();
    const inspect = screen.getByRole("button", { name: "Revisar bloque" });
    inspect.focus();
    fireEvent.click(inspect);
    if (operation === "create") {
      await screen.findByRole("region", { name: "Revisión del bloque" });
      const save = screen.getByRole("button", { name: "Guardar bloque" });
      save.focus();
      fireEvent.click(save);
    }
    document.body.tabIndex = -1;
    document.body.focus();
    document.body.removeAttribute("tabindex");
    const outside = screen.getByRole("link", { name: "Volver al proyecto" });
    if (moved) outside.focus();
    await act(async () =>
      finish(
        operation === "preview"
          ? Response.json(preview)
          : problem("STORAGE_UNAVAILABLE", 503),
      ),
    );
    expect(
      moved
        ? outside
        : operation === "preview"
          ? inspect
          : screen.getByRole("heading", { name: "Bloques planificados" }),
    ).toHaveFocus();
  },
);

it.each([
  ["startLocal", "Inicio del bloque", "NONEXISTENT_LOCAL_TIME"],
  ["endLocal", "Fin del bloque", "OUT_OF_RANGE"],
  ["zoneId", "Zona del bloque", "INVALID_VALUE"],
])(
  "@s59 error de %s queda asociado y enfoca el control afectado",
  async (field, label, code) => {
    window.history.replaceState(null, "", route);
    fixture((url) =>
      url.endsWith("/preview")
        ? problem("VALIDATION_ERROR", 400, {
            errors: [{ field, code, message: "Corrige este dato" }],
          })
        : undefined,
    );
    render(<App />);
    await openEditor();
    const inspect = screen.getByRole("button", { name: "Revisar bloque" });
    inspect.focus();
    fireEvent.click(inspect);
    const input = screen.getByLabelText(label);
    await waitFor(() =>
      expect(input).toHaveAccessibleDescription("Corrige este dato"),
    );
    expect(input).toHaveAttribute("aria-invalid", "true");
    expect(input).toHaveFocus();
  },
);

it("@s49 exceso concurrente exige revisar de nuevo antes de aceptar", async () => {
  window.history.replaceState(null, "", route);
  let previews = 0;
  const days = [
    { ...preview.days[0], plannedSeconds: 6600, excessSeconds: 3000 },
  ];
  fixture((url, options) =>
    url.endsWith("/preview")
      ? Response.json(++previews === 1 ? preview : { ...preview, days })
      : url === `${api}/blocks` && options?.method === "POST"
        ? problem("BUDGET_EXCEEDED", 409, {
            budgetZoneId: preview.budgetZoneId,
            days,
          })
        : undefined,
  );
  render(<App />);
  await openEditor();
  fireEvent.click(screen.getByRole("button", { name: "Revisar bloque" }));
  await screen.findByRole("region", { name: "Revisión del bloque" });
  fireEvent.click(screen.getByRole("button", { name: "Guardar bloque" }));
  await screen.findByText(
    "El presupuesto cambió. Revisa este bloque de nuevo antes de decidir.",
  );
  expect(screen.queryByRole("checkbox")).not.toBeInTheDocument();
  expect(screen.getByLabelText("Objetivo del bloque")).toBeEnabled();
  fireEvent.click(screen.getByRole("button", { name: "Revisar bloque" }));
  expect(await screen.findByRole("checkbox")).not.toBeChecked();
});

it.each(["AVAILABILITY_REQUIRED", "AVAILABILITY_ZONE_UNAVAILABLE"])(
  "@s55 %s ofrece configuración con descarte explicado",
  async (code) => {
    window.history.replaceState(null, "", route);
    fixture((url) =>
      url.endsWith("/preview") ? problem(code, 409) : undefined,
    );
    render(<App />);
    await openEditor();
    fireEvent.click(screen.getByRole("button", { name: "Revisar bloque" }));
    expect(
      await screen.findByRole("link", { name: "Configurar disponibilidad" }),
    ).toHaveAttribute("href", "/disponibilidad");
    expect(
      screen.getByText(
        "Salir para configurar disponibilidad descarta este borrador.",
      ),
    ).toBeVisible();
    expect(screen.getByLabelText("Objetivo del bloque")).toHaveValue(
      " Preparar borrador ",
    );
  },
);

it.each([false, true])(
  "@s56 completar tarea con editor abierto preserva borrador y recuperación incierta=%s",
  async (sent) => {
    window.history.replaceState(null, "", route);
    fixture((url, options) => {
      if (url.endsWith("/preview")) return Response.json(preview);
      if (url === `${api}/blocks` && options?.method === "POST")
        return problem("STORAGE_UNAVAILABLE", 503);
      if (url === `${api}/status` && options?.method === "PUT")
        return Response.json(
          {
            status: "completed",
            completedAt: task.updatedAt,
            updatedAt: task.updatedAt,
          },
          { headers: { ETag: `"task:${task.id}:1"` } },
        );
    });
    render(<App />);
    await openEditor();
    fireEvent.click(screen.getByRole("button", { name: "Revisar bloque" }));
    await screen.findByRole("region", { name: "Revisión del bloque" });
    if (sent) {
      fireEvent.click(screen.getByRole("button", { name: "Guardar bloque" }));
      await screen.findByRole("button", { name: "Comprobar guardado" });
    }
    fireEvent.click(screen.getByRole("button", { name: "Completar tarea" }));
    await screen.findByRole("button", { name: "Reabrir tarea" });
    expect(screen.getByLabelText("Objetivo del bloque")).toHaveValue(
      " Preparar borrador ",
    );
    expect(
      screen.getByRole("button", { name: "Guardar bloque" }),
    ).toBeDisabled();
    if (sent)
      expect(
        screen.getByRole("button", { name: "Comprobar guardado" }),
      ).toBeEnabled();
    else
      expect(
        screen.getByRole("button", { name: "Revisar bloque" }),
      ).toBeDisabled();
  },
);

it("@s39 @s57 conserva datetime y muestra UTC explícito si Intl no reconoce la zona guardada", async () => {
  window.history.replaceState(null, "", route);
  fixture((url) =>
    url === `${api}/blocks`
      ? Response.json({
          items: [{ ...block, zoneId: "Legacy/Retired" }],
          nextCursor: null,
        })
      : url.endsWith("/preview")
        ? Response.json(preview)
        : undefined,
  );
  render(<App />);
  const list = await screen.findByRole("list", {
    name: "Bloques planificados",
  });
  expect(
    within(list).getByText(`${block.startAt} UTC · Legacy/Retired`),
  ).toHaveAttribute("datetime", block.startAt);
  expect(
    within(list).getByText(`${block.endAt} UTC · Legacy/Retired`),
  ).toHaveAttribute("datetime", block.endAt);
  await openEditor();
  fireEvent.click(screen.getByRole("button", { name: "Revisar bloque" }));
  const review = await screen.findByRole("region", {
    name: "Revisión del bloque",
  });
  expect(
    review.querySelector(`time[datetime="${preview.startAt}"]`),
  ).toBeVisible();
  expect(review).toHaveTextContent("Desfase de inicio: Z");
  expect(review).toHaveTextContent("Desfase de fin: Z");
});

it("@s54 error de lista conserva editor y creación confirmada aunque falle refrescar", async () => {
  window.history.replaceState(null, "", route);
  let reads = 0;
  fixture((url, options) => {
    if (url.endsWith("/preview")) return Response.json(preview);
    if (url === `${api}/blocks` && options?.method === "POST")
      return Response.json(block, {
        status: 201,
        headers: { Location: `${api}/blocks/${block.id}` },
      });
    if (url === `${api}/blocks`)
      return ++reads === 2
        ? Response.json({ items: [], nextCursor: null })
        : problem("STORAGE_UNAVAILABLE", 503);
  });
  render(<App />);
  await openEditor();
  expect(
    await screen.findByText("No se pudieron consultar los bloques."),
  ).toHaveAttribute("role", "alert");
  fireEvent.click(screen.getByRole("button", { name: "Reintentar bloques" }));
  await screen.findByText(
    "Todavía no hay bloques planificados para esta tarea.",
  );
  expect(screen.getByLabelText("Objetivo del bloque")).toHaveValue(
    " Preparar borrador ",
  );
  fireEvent.click(screen.getByRole("button", { name: "Revisar bloque" }));
  await screen.findByRole("region", { name: "Revisión del bloque" });
  fireEvent.click(screen.getByRole("button", { name: "Guardar bloque" }));
  await screen.findByText("Bloque guardado");
  await screen.findByText("No se pudieron consultar los bloques.");
  expect(screen.getByText(block.id)).toBeVisible();
  expect(reads).toBe(3);
});

it("@s25 @s26 consulta bloques persistidos y pagina sin tocar el borrador", async () => {
  window.history.replaceState(null, "", route);
  const next = {
    ...block,
    id: "8c5dbd10-9ad5-4000-8000-000000000004",
    objective: "Segundo bloque",
    startAt: "2030-01-07T11:00:00Z",
    endAt: "2030-01-07T12:00:00Z",
  };
  const fetcher = fixture((url) =>
    url === `${api}/blocks`
      ? Response.json({ items: [block], nextCursor: "opaque+cursor=" })
      : url.startsWith(`${api}/blocks?`)
        ? Response.json({ items: [next], nextCursor: null })
        : undefined,
  );
  render(<App />);
  const list = await screen.findByRole("list", {
    name: "Bloques planificados",
  });
  expect(within(list).getByText(block.objective)).toBeVisible();
  await openEditor();
  fireEvent.click(
    screen.getByRole("button", { name: "Ver bloques anteriores" }),
  );
  expect(await screen.findByText("Segundo bloque")).toBeVisible();
  expect(screen.getByLabelText("Objetivo del bloque")).toHaveValue(
    " Preparar borrador ",
  );
  expect(
    screen.queryByRole("button", { name: "Ver bloques anteriores" }),
  ).not.toBeInTheDocument();
  expect(
    fetcher.mock.calls.some(
      ([url]) => url === `${api}/blocks?cursor=opaque%2Bcursor%3D`,
    ),
  ).toBe(true);
  fireEvent.click(
    screen.getByRole("button", { name: "Volver a bloques recientes" }),
  );
  await waitFor(() =>
    expect(
      within(
        screen.getByRole("list", { name: "Bloques planificados" }),
      ).getByText(block.objective),
    ).toBeVisible(),
  );
});

it("@s41 @s42 las ocurrencias DST se eligen por extremo y se retiran al cambiar su fecha", async () => {
  window.history.replaceState(null, "", route);
  let previews = 0;
  const fetcher = fixture((url) => {
    if (!url.endsWith("/preview")) return;
    if (++previews < 3) {
      const field = previews === 1 ? "startOffset" : "endOffset";
      return problem("VALIDATION_ERROR", 400, {
        errors: [
          { field, code: "AMBIGUOUS_OFFSET", message: "Elige una ocurrencia" },
        ],
        validOffsets: { [field]: ["+02:00", "+01:00"] },
      });
    }
    return Response.json({
      ...preview,
      zoneId: "Europe/Madrid",
      startAt: "2026-10-25T00:15:00Z",
      endAt: "2026-10-25T01:45:00Z",
      startOffset: "+02:00",
      endOffset: "+01:00",
      durationMinutes: 90,
      days: [
        { ...preview.days[0], date: "2026-10-25", requestedSeconds: 5400 },
      ],
    });
  });
  render(<App />);
  await openEditor();
  fireEvent.change(screen.getByLabelText("Zona del bloque"), {
    target: { value: "Europe/Madrid" },
  });
  fireEvent.change(screen.getByLabelText("Inicio del bloque"), {
    target: { value: "2026-10-25T02:15" },
  });
  fireEvent.change(screen.getByLabelText("Fin del bloque"), {
    target: { value: "2026-10-25T02:45" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Revisar bloque" }));
  const start = await screen.findByLabelText("Ocurrencia de inicio");
  expect(start).toHaveAccessibleDescription("Elige una ocurrencia");
  expect(start).toHaveValue("");
  fireEvent.change(start, { target: { value: "+02:00" } });
  fireEvent.click(screen.getByRole("button", { name: "Revisar bloque" }));
  const end = await screen.findByLabelText("Ocurrencia de fin");
  expect(end).toHaveAccessibleDescription("Elige una ocurrencia");
  expect(end).toHaveValue("");
  fireEvent.change(end, { target: { value: "+01:00" } });
  fireEvent.click(screen.getByRole("button", { name: "Revisar bloque" }));
  await screen.findByRole("region", { name: "Revisión del bloque" });
  const calls = fetcher.mock.calls.filter(([url]) =>
    String(url).endsWith("/preview"),
  );
  expect(JSON.parse(String(calls[1][1]?.body))).toMatchObject({
    startOffset: "+02:00",
    endOffset: null,
  });
  expect(JSON.parse(String(calls[2][1]?.body))).toMatchObject({
    startOffset: "+02:00",
    endOffset: "+01:00",
  });
  fireEvent.change(screen.getByLabelText("Inicio del bloque"), {
    target: { value: "2026-10-25T03:15" },
  });
  expect(
    screen.queryByLabelText("Ocurrencia de inicio"),
  ).not.toBeInTheDocument();
  expect(screen.getByLabelText("Ocurrencia de fin")).toHaveValue("+01:00");
  expect(screen.getByRole("button", { name: "Guardar bloque" })).toBeDisabled();
});

it("@s49 @s59 errores de campo reconocidos conservan texto seguro y permiten corregir", async () => {
  window.history.replaceState(null, "", route);
  let posts = 0;
  fixture((url, options) =>
    url.endsWith("/preview")
      ? Response.json(preview)
      : url === `${api}/blocks` && options?.method === "POST"
        ? ++posts === 1
          ? problem("VALIDATION_ERROR", 400, {
              errors: [
                {
                  field: "objective",
                  code: "INVALID_VALUE",
                  message: "Corrige <b>el objetivo</b>",
                },
              ],
            })
          : Response.json(block, {
              status: 201,
              headers: { Location: `${api}/blocks/${block.id}` },
            })
        : undefined,
  );
  render(<App />);
  await openEditor();
  fireEvent.click(screen.getByRole("button", { name: "Revisar bloque" }));
  await screen.findByRole("region", { name: "Revisión del bloque" });
  fireEvent.click(screen.getByRole("button", { name: "Guardar bloque" }));
  await screen.findByText("Corrige <b>el objetivo</b>");
  const objective = screen.getByLabelText("Objetivo del bloque");
  expect(objective).toBeEnabled();
  expect(objective).toHaveAttribute("aria-invalid", "true");
  expect(objective).toHaveAccessibleDescription("Corrige <b>el objetivo</b>");
  expect(document.querySelector("#block-objective-error b")).toBeNull();
  fireEvent.change(objective, { target: { value: "Preparar borrador" } });
  fireEvent.click(screen.getByRole("button", { name: "Revisar bloque" }));
  await screen.findByRole("region", { name: "Revisión del bloque" });
  expect(objective).not.toHaveAttribute("aria-invalid", "true");
  fireEvent.click(screen.getByRole("button", { name: "Guardar bloque" }));
  await screen.findByText("Bloque guardado");
});

it("@s51 recuperar CSRF conserva petición y exige reenvío manual con token renovado", async () => {
  window.history.replaceState(null, "", route);
  let sessions = 0;
  let posts = 0;
  const fetcher = fixture((url, options) => {
    if (url === "/api/session")
      return Response.json({
        authenticated: true,
        username: "Pablo",
        csrfToken: ++sessions === 1 ? "old" : "new",
        csrfHeaderName: "X-CSRF-TOKEN",
      });
    if (url.endsWith("/preview")) return Response.json(preview);
    if (url === `${api}/blocks` && options?.method === "POST")
      return ++posts === 1
        ? problem("CSRF_INVALID", 403)
        : Response.json(block, {
            headers: { Location: `${api}/blocks/${block.id}` },
          });
  });
  render(<SessionGate />);
  await openEditor();
  fireEvent.click(screen.getByRole("button", { name: "Revisar bloque" }));
  await screen.findByRole("region", { name: "Revisión del bloque" });
  fireEvent.click(screen.getByRole("button", { name: "Guardar bloque" }));
  const resend = await screen.findByRole("button", {
    name: "Reenviar el mismo bloque",
  });
  expect(
    screen.getByText(/La protección de sesión rechazó este envío/),
  ).toBeVisible();
  fireEvent.click(screen.getByRole("button", { name: "Recuperar acceso" }));
  await waitFor(() => expect(sessions).toBe(2));
  expect(posts).toBe(1);
  fireEvent.click(resend);
  await screen.findByText("Bloque guardado");
  const calls = fetcher.mock.calls.filter(
    ([url, options]) => url === `${api}/blocks` && options?.method === "POST",
  );
  expect(calls).toHaveLength(2);
  expect(calls[1][1]?.body).toBe(calls[0][1]?.body);
  expect(new Headers(calls[1][1]?.headers).get("X-CSRF-TOKEN")).toBe("new");
  expect(new Headers(calls[1][1]?.headers).get("Idempotency-Key")).toBe(
    new Headers(calls[0][1]?.headers).get("Idempotency-Key"),
  );
});

it.each(["preview", "create", "check"])(
  "@s52 pérdida de contexto en %s retira lista y borrador",
  async (operation) => {
    window.history.replaceState(null, "", route);
    fixture((url, options) => {
      if (url.endsWith("/preview"))
        return operation === "preview"
          ? problem("RESOURCE_NOT_FOUND", 404)
          : Response.json(preview);
      if (url.includes("/by-request/"))
        return problem("RESOURCE_NOT_FOUND", 404);
      if (url === `${api}/blocks` && options?.method === "POST")
        return operation === "create"
          ? problem("RESOURCE_NOT_FOUND", 404)
          : problem("STORAGE_UNAVAILABLE", 503);
    });
    render(<App />);
    await openEditor();
    fireEvent.click(screen.getByRole("button", { name: "Revisar bloque" }));
    if (operation !== "preview") {
      await screen.findByRole("region", { name: "Revisión del bloque" });
      fireEvent.click(screen.getByRole("button", { name: "Guardar bloque" }));
    }
    if (operation === "check")
      fireEvent.click(
        await screen.findByRole("button", { name: "Comprobar guardado" }),
      );
    await screen.findByText("Esta tarea no está disponible para tu cuenta.");
    expect(
      screen.queryByLabelText("Objetivo del bloque"),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole("region", { name: "Bloques planificados" }),
    ).not.toBeInTheDocument();
  },
);

it.each([
  ["AVAILABILITY_CONFLICT", 412],
  ["PRECONDITION_REQUIRED", 428],
  ["PROJECT_COMPLETED", 409],
  ["TASK_COMPLETED", 409],
  ["AVAILABILITY_REQUIRED", 409],
  ["AVAILABILITY_ZONE_UNAVAILABLE", 409],
])(
  "@s49 rechazo definitivo %s conserva borrador pero exige nueva revisión",
  async (code, status) => {
    window.history.replaceState(null, "", route);
    let posts = 0;
    const fetcher = fixture((url, options) =>
      url.endsWith("/preview")
        ? Response.json(preview)
        : url === `${api}/blocks` && options?.method === "POST"
          ? ++posts === 1
            ? problem(String(code), Number(status))
            : Response.json(block, {
                status: 201,
                headers: { Location: `${api}/blocks/${block.id}` },
              })
          : undefined,
    );
    render(<App />);
    await openEditor();
    fireEvent.click(screen.getByRole("button", { name: "Revisar bloque" }));
    await screen.findByRole("region", { name: "Revisión del bloque" });
    fireEvent.click(screen.getByRole("button", { name: "Guardar bloque" }));
    await screen.findByText(
      "No se guardó el bloque. Revisa los datos antes de volver a guardar.",
    );
    expect(screen.getByLabelText("Objetivo del bloque")).toBeEnabled();
    expect(screen.getByLabelText("Objetivo del bloque")).toHaveValue(
      " Preparar borrador ",
    );
    expect(
      screen.getByRole("button", { name: "Guardar bloque" }),
    ).toBeDisabled();
    expect(
      screen.queryByRole("region", { name: "Revisión del bloque" }),
    ).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Revisar bloque" }));
    await screen.findByRole("region", { name: "Revisión del bloque" });
    fireEvent.click(screen.getByRole("button", { name: "Guardar bloque" }));
    await screen.findByText("Bloque guardado");
    const calls = fetcher.mock.calls.filter(
      ([url, options]) => url === `${api}/blocks` && options?.method === "POST",
    );
    expect(new Headers(calls[1][1]?.headers).get("Idempotency-Key")).not.toBe(
      new Headers(calls[0][1]?.headers).get("Idempotency-Key"),
    );
  },
);

it("@s47 @s48 404 por key conserva bloqueo y permite reenviar exactamente el mismo bloque", async () => {
  window.history.replaceState(null, "", route);
  let posts = 0;
  const fetcher = fixture((url, options) =>
    url.endsWith("/preview")
      ? Response.json(preview)
      : url.includes("/by-request/")
        ? problem("BLOCK_NOT_FOUND", 404)
        : url === `${api}/blocks` && options?.method === "POST"
          ? ++posts === 1
            ? problem("STORAGE_UNAVAILABLE", 503)
            : Response.json(block, {
                headers: { Location: `${api}/blocks/${block.id}` },
              })
          : undefined,
  );
  render(<App />);
  await openEditor();
  fireEvent.click(screen.getByRole("button", { name: "Revisar bloque" }));
  await screen.findByRole("region", { name: "Revisión del bloque" });
  fireEvent.click(screen.getByRole("button", { name: "Guardar bloque" }));
  fireEvent.click(
    await screen.findByRole("button", { name: "Comprobar guardado" }),
  );
  const resend = await screen.findByRole("button", {
    name: "Reenviar el mismo bloque",
  });
  expect(screen.getByLabelText("Inicio del bloque")).toBeDisabled();
  expect(posts).toBe(1);
  fireEvent.click(resend);
  await screen.findByText("Bloque guardado");
  const calls = fetcher.mock.calls.filter(
    ([url, options]) => url === `${api}/blocks` && options?.method === "POST",
  );
  expect(calls).toHaveLength(2);
  expect(calls[1][1]?.body).toBe(calls[0][1]?.body);
  for (const name of ["Idempotency-Key", "Availability-Revision"])
    expect(new Headers(calls[1][1]?.headers).get(name)).toBe(
      new Headers(calls[0][1]?.headers).get(name),
    );
});

it("@s44 @s53 cerrar no anula envío ni deja que su respuesta cierre otro editor", async () => {
  window.history.replaceState(null, "", route);
  let finish!: (response: Response) => void;
  fixture((url, options) =>
    url.endsWith("/preview")
      ? Response.json(preview)
      : url === `${api}/blocks` && options?.method === "POST"
        ? new Promise((resolve) => {
            finish = resolve;
          })
        : undefined,
  );
  render(<App />);
  await openEditor();
  fireEvent.click(screen.getByRole("button", { name: "Revisar bloque" }));
  await screen.findByRole("region", { name: "Revisión del bloque" });
  fireEvent.click(screen.getByRole("button", { name: "Guardar bloque" }));
  expect(
    screen.getByText(/Cerrar no anula una petición enviada/),
  ).toBeVisible();
  fireEvent.click(screen.getByRole("button", { name: "Cancelar bloque" }));
  fireEvent.click(screen.getByRole("button", { name: "Planificar bloque" }));
  fireEvent.change(screen.getByLabelText("Objetivo del bloque"), {
    target: { value: "Nueva intención" },
  });
  await act(async () =>
    finish(
      Response.json(block, {
        status: 201,
        headers: { Location: `${api}/blocks/${block.id}` },
      }),
    ),
  );
  expect(screen.getByLabelText("Objetivo del bloque")).toHaveValue(
    "Nueva intención",
  );
  expect(screen.queryByText("Bloque guardado")).not.toBeInTheDocument();
});

it.each(["503", "network", "invalid"])(
  "@s45 @s46 conserva intención incierta %s y confirma sólo consulta válida",
  async (kind) => {
    window.history.replaceState(null, "", route);
    const fetcher = fixture((url, options) => {
      if (url.endsWith("/preview")) return Response.json(preview);
      if (url.includes("/by-request/")) return Response.json(block);
      if (url === `${api}/blocks` && options?.method === "POST")
        return kind === "network"
          ? Promise.reject(new Error("offline"))
          : kind === "503"
            ? Response.json({}, { status: 503 })
            : Response.json(
                { ...block, objective: "Distinto" },
                {
                  status: 201,
                  headers: { Location: `${api}/blocks/${block.id}` },
                },
              );
    });
    render(<App />);
    await openEditor();
    fireEvent.click(screen.getByRole("button", { name: "Revisar bloque" }));
    await screen.findByRole("region", { name: "Revisión del bloque" });
    fireEvent.click(screen.getByRole("button", { name: "Guardar bloque" }));
    const check = await screen.findByRole("button", {
      name: "Comprobar guardado",
    });
    expect(screen.getByLabelText("Objetivo del bloque")).toBeDisabled();
    expect(screen.getByLabelText("Objetivo del bloque")).toHaveValue(
      " Preparar borrador ",
    );
    expect(screen.queryByText("Bloque guardado")).not.toBeInTheDocument();
    const sent = fetcher.mock.calls.find(
      ([url, options]) => url === `${api}/blocks` && options?.method === "POST",
    )!;
    const key = new Headers(sent[1]?.headers).get("Idempotency-Key");
    fireEvent.click(check);
    expect(await screen.findByText("Bloque guardado")).toBeVisible();
    expect(
      fetcher.mock.calls.some(
        ([url]) => url === `${api}/blocks/by-request/${key}`,
      ),
    ).toBe(true);
    expect(
      fetcher.mock.calls.filter(
        ([url, options]) =>
          url === `${api}/blocks` && options?.method === "POST",
      ),
    ).toHaveLength(1);
  },
);

it("@s44 @s46 confirma sólo creación persistida con key y revisión, bloqueando doble envío", async () => {
  window.history.replaceState(null, "", route);
  let finish!: (response: Response) => void;
  const fetcher = fixture((url, options) =>
    url.endsWith("/preview")
      ? Response.json(preview)
      : url === `${api}/blocks` && options?.method === "POST"
        ? new Promise((resolve) => {
            finish = resolve;
          })
        : undefined,
  );
  render(<App />);
  await openEditor();
  fireEvent.click(screen.getByRole("button", { name: "Revisar bloque" }));
  await screen.findByRole("region", { name: "Revisión del bloque" });
  fireEvent.click(screen.getByRole("button", { name: "Guardar bloque" }));
  expect(screen.getByText("Guardando bloque")).toHaveAttribute(
    "role",
    "status",
  );
  expect(screen.getByLabelText("Objetivo del bloque")).toBeDisabled();
  expect(screen.getByLabelText("Zona del bloque")).toBeDisabled();
  fireEvent.click(screen.getByRole("button", { name: "Guardar bloque" }));
  const calls = fetcher.mock.calls.filter(
    ([url, options]) => url === `${api}/blocks` && options?.method === "POST",
  );
  expect(calls).toHaveLength(1);
  const headers = new Headers(calls[0][1]?.headers);
  expect(headers.get("Idempotency-Key")).toMatch(
    /^[\da-f]{8}(-[\da-f]{4}){3}-[\da-f]{12}$/,
  );
  expect(headers.get("Availability-Revision")).toBe(preview.availabilityEtag);
  expect(JSON.parse(String(calls[0][1]?.body))).toEqual({
    objective: preview.objective,
    startLocal: "2030-01-07T10:00",
    endLocal: "2030-01-07T11:00",
    zoneId: "UTC",
    startOffset: "Z",
    endOffset: "Z",
    allowOverBudget: false,
  });
  await act(async () =>
    finish(
      Response.json(block, {
        status: 201,
        headers: { Location: `${api}/blocks/${block.id}` },
      }),
    ),
  );
  expect(screen.getByText("Bloque guardado")).toHaveAttribute("role", "status");
  expect(screen.getByText(block.id)).toBeVisible();
});

it("@s39 @s43 exceso muestra presupuesto y exige aceptación específica sin preselección", async () => {
  window.history.replaceState(null, "", route);
  fixture((url) =>
    url === `${api}/blocks/preview`
      ? Response.json({
          ...preview,
          days: [
            { ...preview.days[0], plannedSeconds: 5400, excessSeconds: 1800 },
          ],
        })
      : undefined,
  );
  render(<App />);
  await openEditor();
  fireEvent.click(screen.getByRole("button", { name: "Revisar bloque" }));
  const consent = await screen.findByRole("checkbox", {
    name: /Acepto superar/,
  });
  expect(consent).not.toBeChecked();
  expect(screen.getByRole("button", { name: "Guardar bloque" })).toBeDisabled();
  expect(
    screen.getByRole("region", { name: "Revisión del bloque" }),
  ).toHaveTextContent("2030-01-07");
  expect(
    screen.getByRole("region", { name: "Revisión del bloque" }),
  ).toHaveTextContent("Presupuesto: 120 minutos");
  expect(
    screen.getByRole("region", { name: "Revisión del bloque" }),
  ).toHaveTextContent("Exceso: 1800 segundos");
  expect(
    screen.getByText(/aunque otras reservas aumenten el exceso/),
  ).toBeVisible();
  fireEvent.click(consent);
  expect(screen.getByRole("button", { name: "Guardar bloque" })).toBeEnabled();
  fireEvent.change(screen.getByLabelText("Objetivo del bloque"), {
    target: { value: "Cambio" },
  });
  expect(screen.queryByRole("checkbox")).not.toBeInTheDocument();
  expect(screen.getByRole("button", { name: "Guardar bloque" })).toBeDisabled();
});

it.each(["network", "503", "invalid"])(
  "@s40 @s54 preview fallido %s conserva borrador y retira revisión anterior",
  async (kind) => {
    window.history.replaceState(null, "", route);
    let previews = 0;
    fixture((url) => {
      if (url !== `${api}/blocks/preview`) return;
      if (++previews === 1) return Response.json(preview);
      if (kind === "network") return Promise.reject(new Error("offline"));
      return kind === "503"
        ? Response.json({}, { status: 503 })
        : Response.json({ ...preview, durationMinutes: 61 });
    });
    render(<App />);
    await openEditor();
    fireEvent.click(screen.getByRole("button", { name: "Revisar bloque" }));
    await screen.findByRole("region", { name: "Revisión del bloque" });
    fireEvent.click(screen.getByRole("button", { name: "Revisar bloque" }));
    expect(
      await screen.findByText(
        "No se pudo revisar el bloque. Conservamos tus datos.",
      ),
    ).toHaveAttribute("role", "alert");
    expect(
      screen.queryByRole("region", { name: "Revisión del bloque" }),
    ).not.toBeInTheDocument();
    expect(screen.getByLabelText("Objetivo del bloque")).toHaveValue(
      " Preparar borrador ",
    );
    expect(
      screen.getByRole("button", { name: "Guardar bloque" }),
    ).toBeDisabled();
  },
);

it("@s42 @s53 editar invalida revisión vigente y respuesta pendiente", async () => {
  window.history.replaceState(null, "", route);
  let finish!: (response: Response) => void;
  let previews = 0;
  fixture((url) =>
    url === `${api}/blocks/preview`
      ? ++previews === 1
        ? Response.json(preview)
        : new Promise((resolve) => {
            finish = resolve;
          })
      : undefined,
  );
  render(<App />);
  await openEditor();
  fireEvent.click(screen.getByRole("button", { name: "Revisar bloque" }));
  await screen.findByRole("region", { name: "Revisión del bloque" });
  fireEvent.change(screen.getByLabelText("Objetivo del bloque"), {
    target: { value: "Otra intención" },
  });
  expect(screen.getByRole("button", { name: "Guardar bloque" })).toBeDisabled();
  expect(
    screen.queryByRole("region", { name: "Revisión del bloque" }),
  ).not.toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", { name: "Revisar bloque" }));
  fireEvent.change(screen.getByLabelText("Fin del bloque"), {
    target: { value: "2030-01-07T12:00" },
  });
  await act(async () =>
    finish(Response.json({ ...preview, objective: "Otra intención" })),
  );
  expect(
    screen.queryByRole("region", { name: "Revisión del bloque" }),
  ).not.toBeInTheDocument();
  expect(screen.getByRole("button", { name: "Guardar bloque" })).toBeDisabled();
});

it("@s39 revisa intención antes de habilitar creación y distingue ambas zonas", async () => {
  window.history.replaceState(null, "", route);
  let finish!: (response: Response) => void;
  const fetcher = fixture((url) =>
    url === `${api}/blocks/preview`
      ? new Promise((resolve) => {
          finish = resolve;
        })
      : undefined,
  );
  render(<App />);
  await openEditor();
  fireEvent.click(screen.getByRole("button", { name: "Revisar bloque" }));
  expect(screen.getByText("Revisando bloque")).toHaveAttribute(
    "role",
    "status",
  );
  expect(screen.getByRole("button", { name: "Guardar bloque" })).toBeDisabled();
  await act(async () => finish(Response.json(preview)));
  expect(screen.getByRole("button", { name: "Guardar bloque" })).toBeEnabled();
  const review = screen.getByRole("region", { name: "Revisión del bloque" });
  expect(review).toHaveTextContent("60 minutos");
  expect(review).toHaveTextContent("Zona del bloque: UTC");
  expect(review).toHaveTextContent("Zona del presupuesto: Europe/Madrid");
  expect(review).toHaveTextContent("Preparar borrador");
  const request = fetcher.mock.calls.find(
    ([url]) => url === `${api}/blocks/preview`,
  )!;
  expect(JSON.parse(String(request[1]?.body))).toEqual({
    objective: " Preparar borrador ",
    startLocal: "2030-01-07T10:00",
    endLocal: "2030-01-07T11:00",
    zoneId: "UTC",
    startOffset: null,
    endOffset: null,
  });
  expect(
    fetcher.mock.calls.filter(
      ([url, options]) => url === `${api}/blocks` && options?.method === "POST",
    ),
  ).toHaveLength(0);
});

it("@s38 @s54 recupera configuración fallida sin perder objetivo", async () => {
  window.history.replaceState(null, "", route);
  let reads = 0;
  fixture((url) =>
    url === "/api/v1/me/availability" && ++reads === 1
      ? Response.json({}, { status: 503 })
      : undefined,
  );
  render(<App />);
  fireEvent.click(
    await screen.findByRole("button", { name: "Planificar bloque" }),
  );
  fireEvent.change(await screen.findByLabelText("Objetivo del bloque"), {
    target: { value: "Conservar esto" },
  });
  expect(
    await screen.findByText(
      "No se pudo consultar la configuración del bloque.",
    ),
  ).toHaveAttribute("role", "alert");
  fireEvent.click(
    screen.getByRole("button", { name: "Reintentar configuración" }),
  );
  await waitFor(() =>
    expect(screen.getByLabelText("Zona del bloque")).toHaveValue(
      "Europe/Madrid",
    ),
  );
  expect(screen.getByLabelText("Objetivo del bloque")).toHaveValue(
    "Conservar esto",
  );
});

it("@s38 abre editor nativo con zona guardada sin enviar ni guardar automáticamente", async () => {
  window.history.replaceState(null, "", route);
  const fetcher = fixture();
  render(<App />);
  fireEvent.click(
    await screen.findByRole("button", { name: "Planificar bloque" }),
  );
  const objective = await screen.findByLabelText("Objetivo del bloque");
  expect(objective.tagName).toBe("TEXTAREA");
  expect(screen.getByLabelText("Inicio del bloque")).toHaveAttribute(
    "type",
    "datetime-local",
  );
  expect(screen.getByLabelText("Fin del bloque")).toHaveAttribute("step", "60");
  await waitFor(() =>
    expect(screen.getByLabelText("Zona del bloque")).toHaveValue(
      "Europe/Madrid",
    ),
  );
  expect(screen.getByRole("button", { name: "Guardar bloque" })).toBeDisabled();
  expect(screen.getByRole("button", { name: "Revisar bloque" })).toBeEnabled();
  fireEvent.change(objective, { target: { value: "Primera sección" } });
  expect(
    fetcher.mock.calls.filter(([, options]) => options?.method === "POST"),
  ).toHaveLength(0);
  expect(
    screen.getByText(
      "Los bloques son tiempo planificado, no trabajo realizado.",
    ),
  ).toBeVisible();
});

it("@s56 un proyecto terminado conserva la sección pero impide planificación nueva", async () => {
  window.history.replaceState(null, "", route);
  fixture((url) =>
    url === `/api/v1/projects/${project.id}`
      ? Response.json(
          { ...project, status: "completed" },
          { headers: { ETag: '"project"' } },
        )
      : undefined,
  );
  render(<App />);
  await screen.findByRole("button", { name: "Reabrir en pausa" });
  expect(
    screen.getByRole("region", { name: "Bloques planificados" }),
  ).toBeVisible();
  expect(
    screen.queryByRole("button", { name: "Planificar bloque" }),
  ).not.toBeInTheDocument();
});

it("@s56 retira elegibilidad incierta y la recupera con consulta deliberada", async () => {
  window.history.replaceState(null, "", route);
  let statusReads = 0;
  let finish!: (response: Response) => void;
  fixture((url, options) => {
    if (url !== `${api}/status`) return;
    if (options?.method === "PUT") return Response.json({}, { status: 412 });
    if (++statusReads > 1)
      return new Promise((resolve) => {
        finish = resolve;
      });
  });
  render(<App />);
  await screen.findByRole("button", { name: "Planificar bloque" });
  fireEvent.click(screen.getByRole("button", { name: "Completar tarea" }));
  fireEvent.click(
    await screen.findByRole("button", { name: "Consultar estado vigente" }),
  );
  expect(
    screen.queryByRole("button", { name: "Planificar bloque" }),
  ).not.toBeInTheDocument();
  await act(async () =>
    finish(
      Response.json(pending, { headers: { ETag: `"task:${task.id}:1"` } }),
    ),
  );
  expect(
    screen.getByRole("button", { name: "Planificar bloque" }),
  ).toBeEnabled();
});

it("@s56 comparte estado confirmado sin leer de nuevo el proyecto", async () => {
  window.history.replaceState(null, "", route);
  let finish!: (response: Response) => void;
  const fetcher = fixture((url, options) =>
    url === `${api}/status` && options?.method === "PUT"
      ? new Promise((resolve) => {
          finish = resolve;
        })
      : undefined,
  );
  render(<App />);
  expect(
    await screen.findByRole("button", { name: "Planificar bloque" }),
  ).toBeEnabled();
  fireEvent.click(screen.getByRole("button", { name: "Completar tarea" }));
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
    within(
      screen.getByRole("region", { name: "Bloques planificados" }),
    ).queryByRole("button", { name: "Planificar bloque" }),
  ).not.toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", { name: "Reabrir tarea" }));
  await act(async () =>
    finish(
      Response.json(pending, { headers: { ETag: `"task:${task.id}:2"` } }),
    ),
  );
  await waitFor(() =>
    expect(
      screen.getByRole("button", { name: "Planificar bloque" }),
    ).toBeEnabled(),
  );
  expect(
    fetcher.mock.calls.filter(
      ([url]) => url === `/api/v1/projects/${project.id}`,
    ),
  ).toHaveLength(1);
});

it("@s53 listado tardío de otra tarea no reemplaza el contexto navegado", async () => {
  window.history.replaceState(null, "", route);
  const nextTask = {
    ...task,
    id: "7c5dbd10-9ad5-4000-8000-000000000099",
    title: "Segunda tarea",
  };
  const nextApi = `/api/v1/projects/${project.id}/tasks/${nextTask.id}`;
  let finish!: (value: unknown) => void;
  let signal: AbortSignal | null | undefined;
  fixture((url, options) => {
    if (url === `${api}/blocks`) {
      signal = options?.signal;
      const response = Response.json({});
      vi.spyOn(response, "json").mockImplementation(
        () =>
          new Promise((resolve) => {
            finish = resolve;
          }),
      );
      return response;
    }
    if (url === nextApi) return Response.json(nextTask);
    if (url === `${nextApi}/status`)
      return Response.json(pending, {
        headers: { ETag: `"task:${nextTask.id}:0"` },
      });
    if (url === `${nextApi}/parent`) return Response.json({ parent: null });
    if (url === `${nextApi}/blocks`)
      return Response.json({
        items: [
          {
            ...block,
            taskId: nextTask.id,
            objective: "Bloque del contexto nuevo",
          },
        ],
        nextCursor: null,
      });
  });
  render(<App />);
  await waitFor(() => expect(finish).toBeTypeOf("function"));
  act(() => {
    window.history.pushState(
      null,
      "",
      `/proyectos/${project.id}/tareas/${nextTask.id}`,
    );
    window.dispatchEvent(new PopStateEvent("popstate"));
  });
  await screen.findByRole("heading", { name: "Bloque del contexto nuevo" });
  expect(signal?.aborted).toBe(true);
  await act(async () =>
    finish({
      items: [{ ...block, objective: "Bloque privado antiguo" }],
      nextCursor: null,
    }),
  );
  expect(
    screen.getByRole("heading", { name: "Bloque del contexto nuevo" }),
  ).toBeVisible();
  expect(screen.queryByText("Bloque privado antiguo")).not.toBeInTheDocument();
});

it("@s53 recuperación tardía no confirma ni roba foco tras navegar a otra tarea", async () => {
  window.history.replaceState(null, "", route);
  const nextTask = {
    ...task,
    id: "7c5dbd10-9ad5-4000-8000-000000000099",
    title: "Segunda tarea",
  };
  const nextApi = `/api/v1/projects/${project.id}/tasks/${nextTask.id}`;
  let finish!: (value: unknown) => void;
  let signal: AbortSignal | null | undefined;
  const fetcher = fixture((url, options) => {
    if (url.endsWith("/preview")) return Response.json(preview);
    if (url === `${api}/blocks` && options?.method === "POST")
      return problem("STORAGE_UNAVAILABLE", 503);
    if (url.includes("/by-request/")) {
      signal = options?.signal;
      const response = Response.json({});
      vi.spyOn(response, "json").mockImplementation(
        () =>
          new Promise((resolve) => {
            finish = resolve;
          }),
      );
      return response;
    }
    if (url === nextApi) return Response.json(nextTask);
    if (url === `${nextApi}/status`)
      return Response.json(pending, {
        headers: { ETag: `"task:${nextTask.id}:0"` },
      });
    if (url === `${nextApi}/parent`) return Response.json({ parent: null });
  });
  render(<App />);
  await openEditor();
  fireEvent.click(screen.getByRole("button", { name: "Revisar bloque" }));
  await screen.findByRole("region", { name: "Revisión del bloque" });
  fireEvent.click(screen.getByRole("button", { name: "Guardar bloque" }));
  fireEvent.click(
    await screen.findByRole("button", { name: "Comprobar guardado" }),
  );
  await waitFor(() => expect(finish).toBeTypeOf("function"));
  act(() => {
    window.history.pushState(
      null,
      "",
      `/proyectos/${project.id}/tareas/${nextTask.id}`,
    );
    window.dispatchEvent(new PopStateEvent("popstate"));
  });
  const plan = await screen.findByRole("button", { name: "Planificar bloque" });
  plan.focus();
  expect(signal?.aborted).toBe(true);
  await act(async () => finish(block));
  expect(screen.queryByText("Bloque guardado")).not.toBeInTheDocument();
  expect(plan).toHaveFocus();
  expect(screen.getByRole("heading", { name: "Segunda tarea" })).toBeVisible();
  expect(
    fetcher.mock.calls.filter(
      ([url, options]) => url === `${api}/blocks` && options?.method === "POST",
    ),
  ).toHaveLength(1);
});

it("@s53 renovación CSRF tardía no restaura contexto revocado durante recuperación", async () => {
  window.history.replaceState(null, "", route);
  let sessions = 0;
  let finish!: (value: unknown) => void;
  let signal: AbortSignal | null | undefined;
  const authenticated = {
    authenticated: true,
    username: "Pablo",
    csrfToken: "renewed",
    csrfHeaderName: "X-CSRF-TOKEN",
  };
  const fetcher = fixture((url, options) => {
    if (url === "/api/session") {
      sessions++;
      if (sessions === 2) {
        signal = options?.signal;
        const response = Response.json({});
        vi.spyOn(response, "json").mockImplementation(
          () =>
            new Promise((resolve) => {
              finish = resolve;
            }),
        );
        return response;
      }
      return Response.json(
        sessions === 1
          ? authenticated
          : { ...authenticated, authenticated: false, username: null },
      );
    }
    if (url.endsWith("/preview")) return Response.json(preview);
    if (url === `${api}/blocks` && options?.method === "POST")
      return problem("CSRF_INVALID", 403);
    if (url === `${api}/status` && options?.method === "PUT")
      return problem("UNAUTHORIZED", 401);
  });
  render(<SessionGate />);
  await openEditor();
  fireEvent.click(screen.getByRole("button", { name: "Revisar bloque" }));
  await screen.findByRole("region", { name: "Revisión del bloque" });
  fireEvent.click(screen.getByRole("button", { name: "Guardar bloque" }));
  await screen.findByRole("button", { name: "Reenviar el mismo bloque" });
  fireEvent.click(screen.getByRole("button", { name: "Recuperar acceso" }));
  await waitFor(() => expect(finish).toBeTypeOf("function"));
  fireEvent.click(screen.getByRole("button", { name: "Completar tarea" }));
  await screen.findByLabelText("Contraseña");
  expect(signal?.aborted).toBe(true);
  await act(async () => finish(authenticated));
  expect(screen.getByLabelText("Contraseña")).toBeVisible();
  expect(
    screen.queryByRole("region", { name: "Bloques planificados" }),
  ).not.toBeInTheDocument();
  expect(
    screen.queryByDisplayValue(" Preparar borrador "),
  ).not.toBeInTheDocument();
  expect(
    fetcher.mock.calls.filter(
      ([url, options]) => url === `${api}/blocks` && options?.method === "POST",
    ),
  ).toHaveLength(1);
});
it("@s56 reintentar tarea espera su estado nuevo antes de permitir planificar", async () => {
  window.history.replaceState(null, "", route);
  let finishList!: (response: Response) => void;
  let finishStatus!: (response: Response) => void;
  let listReads = 0;
  let statusReads = 0;
  fixture((url) => {
    if (url === `${api}/blocks` && ++listReads === 1)
      return new Promise((resolve) => {
        finishList = resolve;
      });
    if (url === `${api}/status` && ++statusReads === 2)
      return new Promise((resolve) => {
        finishStatus = resolve;
      });
  });
  render(<App />);
  await screen.findByRole("button", { name: "Planificar bloque" });
  await act(async () => finishList(problem("RESOURCE_NOT_FOUND", 404)));
  fireEvent.click(
    await screen.findByRole("button", { name: "Reintentar tarea" }),
  );
  await screen.findByRole("heading", { name: "Estado del proyecto" });
  expect(screen.getByText("Consultando estado de la tarea")).toBeVisible();
  expect(
    screen.queryByRole("button", { name: "Planificar bloque" }),
  ).not.toBeInTheDocument();
  await act(async () =>
    finishStatus(
      Response.json(pending, {
        headers: { ETag: `"task:${task.id}:1"` },
      }),
    ),
  );
  expect(
    screen.getByRole("button", { name: "Planificar bloque" }),
  ).toBeEnabled();
});
it("@s53 una transición vieja no cambia la elegibilidad tras reintentar tarea", async () => {
  window.history.replaceState(null, "", route);
  let finishList!: (response: Response) => void;
  let finishWrite!: (response: Response) => void;
  let listReads = 0;
  fixture((url, options) => {
    if (url === `${api}/blocks` && ++listReads === 1)
      return new Promise((resolve) => {
        finishList = resolve;
      });
    if (url === `${api}/status` && options?.method === "PUT")
      return new Promise((resolve) => {
        finishWrite = resolve;
      });
  });
  render(<App />);
  await screen.findByRole("button", { name: "Planificar bloque" });
  fireEvent.click(screen.getByRole("button", { name: "Completar tarea" }));
  await waitFor(() => expect(finishWrite).toBeDefined());
  await act(async () => finishList(problem("RESOURCE_NOT_FOUND", 404)));
  fireEvent.click(
    await screen.findByRole("button", { name: "Reintentar tarea" }),
  );
  await screen.findByRole("button", { name: "Planificar bloque" });
  await act(async () =>
    finishWrite(
      Response.json(
        {
          ...pending,
          status: "completed",
          completedAt: task.updatedAt,
        },
        { headers: { ETag: `"task:${task.id}:1"` } },
      ),
    ),
  );
  expect(
    screen.getByRole("button", { name: "Planificar bloque" }),
  ).toBeEnabled();
  expect(screen.getByRole("button", { name: "Completar tarea" })).toBeEnabled();
});
it("@s53 proyecto anterior no termina la carga ni reemplaza contexto tras retry de tarea", async () => {
  window.history.replaceState(null, "", route);
  let firstProject!: (response: Response) => void;
  let currentProject!: (response: Response) => void;
  let finishList!: (response: Response) => void;
  let projectReads = 0;
  let listReads = 0;
  fixture((url) => {
    if (url === `/api/v1/projects/${project.id}`)
      return new Promise((resolve) => {
        if (++projectReads === 1) firstProject = resolve;
        else currentProject = resolve;
      });
    if (url === `${api}/blocks` && ++listReads === 1)
      return new Promise((resolve) => {
        finishList = resolve;
      });
  });
  render(<App />);
  await screen.findByRole("button", { name: "Completar tarea" });
  await waitFor(() => expect(firstProject).toBeDefined());
  await act(async () => finishList(problem("RESOURCE_NOT_FOUND", 404)));
  fireEvent.click(
    await screen.findByRole("button", { name: "Reintentar tarea" }),
  );
  await waitFor(() => expect(currentProject).toBeDefined());
  await act(async () =>
    firstProject(
      Response.json(
        { ...project, status: "completed" },
        { headers: { ETag: '"old"' } },
      ),
    ),
  );
  expect(screen.getByText("Consultando proyecto")).toBeVisible();
  expect(
    screen.queryByRole("button", { name: "Planificar bloque" }),
  ).not.toBeInTheDocument();
  expect(
    screen.queryByRole("button", { name: "Reabrir en pausa" }),
  ).not.toBeInTheDocument();
  await act(async () =>
    currentProject(Response.json(project, { headers: { ETag: '"current"' } })),
  );
  expect(
    screen.getByRole("button", { name: "Planificar bloque" }),
  ).toBeEnabled();
});
it("@s53 rechazo del proyecto antiguo no retira tarea recuperada", async () => {
  window.history.replaceState(null, "", route);
  let firstProject!: (response: Response) => void;
  let finishList!: (response: Response) => void;
  let projectReads = 0;
  let listReads = 0;
  fixture((url) => {
    if (url === `/api/v1/projects/${project.id}` && ++projectReads === 1)
      return new Promise((resolve) => {
        firstProject = resolve;
      });
    if (url === `${api}/blocks` && ++listReads === 1)
      return new Promise((resolve) => {
        finishList = resolve;
      });
  });
  render(<App />);
  await screen.findByRole("button", { name: "Completar tarea" });
  await waitFor(() => expect(firstProject).toBeDefined());
  await act(async () => finishList(problem("RESOURCE_NOT_FOUND", 404)));
  fireEvent.click(
    await screen.findByRole("button", { name: "Reintentar tarea" }),
  );
  await screen.findByRole("button", { name: "Planificar bloque" });
  await act(async () => firstProject(new Response(null, { status: 404 })));
  expect(
    screen.getByRole("button", { name: "Planificar bloque" }),
  ).toBeEnabled();
  expect(
    screen.queryByRole("button", { name: "Reintentar tarea" }),
  ).not.toBeInTheDocument();
});
it("@s49 @s58 rechazo del reenvío restaura foco y una intención nueva exige comprobar antes de reenviar", async () => {
  window.history.replaceState(null, "", route);
  let writes = 0;
  fixture((url, options) => {
    if (url.endsWith("/preview")) return Response.json(preview);
    if (url.includes("/by-request/")) return problem("BLOCK_NOT_FOUND", 404);
    if (url === `${api}/blocks` && options?.method === "POST") {
      if (++writes === 2)
        return problem("BUDGET_EXCEEDED", 409, {
          budgetZoneId: preview.budgetZoneId,
          days: [
            { ...preview.days[0], plannedSeconds: 6600, excessSeconds: 3000 },
          ],
        });
      return problem("STORAGE_UNAVAILABLE", 503);
    }
  });
  render(<App />);
  await openEditor();
  fireEvent.click(screen.getByRole("button", { name: "Revisar bloque" }));
  await screen.findByRole("region", { name: "Revisión del bloque" });
  fireEvent.click(screen.getByRole("button", { name: "Guardar bloque" }));
  fireEvent.click(
    await screen.findByRole("button", { name: "Comprobar guardado" }),
  );
  const resend = await screen.findByRole("button", {
    name: "Reenviar el mismo bloque",
  });
  resend.focus();
  fireEvent.click(resend);
  await screen.findByText(
    "El presupuesto cambió. Revisa este bloque de nuevo antes de decidir.",
  );
  expect(
    screen.getByRole("heading", { name: "Bloques planificados" }),
  ).toHaveFocus();
  expect(
    screen.queryByRole("button", { name: "Reenviar el mismo bloque" }),
  ).not.toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", { name: "Revisar bloque" }));
  await screen.findByRole("region", { name: "Revisión del bloque" });
  fireEvent.click(screen.getByRole("button", { name: "Guardar bloque" }));
  await screen.findByRole("button", { name: "Comprobar guardado" });
  expect(
    screen.queryByRole("button", { name: "Reenviar el mismo bloque" }),
  ).not.toBeInTheDocument();
  expect(writes).toBe(3);
});
it("@s41 varios días con exceso sólo en uno exigen consentimiento y revisión sin exceso lo retira", async () => {
  window.history.replaceState(null, "", route);
  let reviews = 0;
  fixture((url) => {
    if (!url.endsWith("/preview")) return;
    const excess = ++reviews === 1;
    return Response.json({
      ...preview,
      startAt: "2030-01-07T22:30:00Z",
      endAt: "2030-01-07T23:30:00Z",
      days: [
        {
          ...preview.days[0],
          requestedSeconds: 1800,
          plannedSeconds: excess ? 7200 : 0,
          excessSeconds: excess ? 1800 : 0,
        },
        { ...preview.days[0], date: "2030-01-08", requestedSeconds: 1800 },
      ],
    });
  });
  render(<App />);
  await openEditor();
  fireEvent.change(screen.getByLabelText("Inicio del bloque"), {
    target: { value: "2030-01-07T22:30" },
  });
  fireEvent.change(screen.getByLabelText("Fin del bloque"), {
    target: { value: "2030-01-07T23:30" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Revisar bloque" }));
  const acceptance = await screen.findByRole("checkbox");
  expect(acceptance).not.toBeChecked();
  expect(screen.getByRole("button", { name: "Guardar bloque" })).toBeDisabled();
  fireEvent.click(acceptance);
  expect(screen.getByRole("button", { name: "Guardar bloque" })).toBeEnabled();
  fireEvent.click(screen.getByRole("button", { name: "Revisar bloque" }));
  await waitFor(() =>
    expect(screen.queryByRole("checkbox")).not.toBeInTheDocument(),
  );
  expect(screen.getByRole("button", { name: "Guardar bloque" })).toBeEnabled();
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
});
it("@s56 el envío del formulario se cancela y no duplica preview pendiente ni revisa tarea completada", async () => {
  window.history.replaceState(null, "", route);
  let finishPreview!: (response: Response) => void;
  let reviews = 0;
  fixture((url, options) => {
    if (url.endsWith("/preview")) {
      reviews++;
      return new Promise((resolve) => {
        finishPreview = resolve;
      });
    }
    if (url === `${api}/status` && options?.method === "PUT")
      return Response.json(
        { ...pending, status: "completed", completedAt: task.updatedAt },
        { headers: { ETag: `"task:${task.id}:1"` } },
      );
  });
  render(<App />);
  await openEditor();
  const form = screen.getByLabelText("Objetivo del bloque").closest("form")!;
  expect(fireEvent.submit(form)).toBe(false);
  await waitFor(() => expect(reviews).toBe(1));
  expect(screen.getByText("Revisando bloque")).toBeVisible();
  expect(fireEvent.submit(form)).toBe(false);
  expect(reviews).toBe(1);
  await act(async () => finishPreview(Response.json(preview)));
  fireEvent.click(screen.getByRole("button", { name: "Completar tarea" }));
  await screen.findByRole("button", { name: "Reabrir tarea" });
  expect(fireEvent.submit(form)).toBe(false);
  expect(reviews).toBe(1);
  expect(screen.getByRole("button", { name: "Guardar bloque" })).toBeDisabled();
});
it("@s56 identidad retenida puede reenviarse tras ausencia confirmada aunque tarea complete", async () => {
  window.history.replaceState(null, "", route);
  let writes = 0;
  const fetcher = fixture((url, options) => {
    if (url.endsWith("/preview")) return Response.json(preview);
    if (url.includes("/by-request/")) return problem("BLOCK_NOT_FOUND", 404);
    if (url === `${api}/blocks` && options?.method === "POST")
      return ++writes === 1
        ? problem("STORAGE_UNAVAILABLE", 503)
        : Response.json(block, {
            status: 201,
            headers: { Location: `${api}/blocks/${block.id}` },
          });
    if (url === `${api}/status` && options?.method === "PUT")
      return Response.json(
        { ...pending, status: "completed", completedAt: task.updatedAt },
        { headers: { ETag: `"task:${task.id}:1"` } },
      );
  });
  render(<App />);
  await openEditor();
  fireEvent.click(screen.getByRole("button", { name: "Revisar bloque" }));
  await screen.findByRole("region", { name: "Revisión del bloque" });
  fireEvent.click(screen.getByRole("button", { name: "Guardar bloque" }));
  await screen.findByRole("button", { name: "Comprobar guardado" });
  fireEvent.click(screen.getByRole("button", { name: "Completar tarea" }));
  await screen.findByRole("button", { name: "Reabrir tarea" });
  fireEvent.click(screen.getByRole("button", { name: "Comprobar guardado" }));
  fireEvent.click(
    await screen.findByRole("button", { name: "Reenviar el mismo bloque" }),
  );
  await screen.findByText("Bloque guardado");
  const requests = fetcher.mock.calls.filter(
    ([url, options]) => url === `${api}/blocks` && options?.method === "POST",
  );
  expect(requests).toHaveLength(2);
  expect(requests[1][1]?.body).toBe(requests[0][1]?.body);
  expect(new Headers(requests[1][1]?.headers).get("Idempotency-Key")).toBe(
    new Headers(requests[0][1]?.headers).get("Idempotency-Key"),
  );
  expect(
    screen.queryByRole("button", { name: "Planificar bloque" }),
  ).not.toBeInTheDocument();
});
it("@s40 disponibilidad sin configurar conserva campos vacíos y selector sin zona inventada", async () => {
  window.history.replaceState(null, "", route);
  let finishConfiguration!: (response: Response) => void;
  const fetcher = fixture((url) => {
    if (url === "/api/v1/me/availability")
      return new Promise((resolve) => {
        finishConfiguration = resolve;
      });
    if (url.endsWith("/preview")) return problem("AVAILABILITY_REQUIRED", 409);
  });
  render(<App />);
  fireEvent.click(
    await screen.findByRole("button", { name: "Planificar bloque" }),
  );
  expect(screen.getByLabelText("Objetivo del bloque")).toHaveValue("");
  expect(screen.getByLabelText("Inicio del bloque")).toHaveValue("");
  expect(screen.getByLabelText("Fin del bloque")).toHaveValue("");
  expect(screen.getByLabelText("Zona del bloque")).toHaveValue("");
  expect(
    within(screen.getByLabelText("Zona del bloque")).getAllByRole("option"),
  ).toHaveLength(1);
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  await act(async () =>
    finishConfiguration(
      Response.json(
        {
          configured: false,
          zoneId: null,
          dailyMinutes: null,
          updatedAt: null,
        },
        { headers: { ETag: '"availability:unconfigured"' } },
      ),
    ),
  );
  expect(screen.getByLabelText("Zona del bloque")).toHaveValue("");
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", { name: "Revisar bloque" }));
  await screen.findByRole("link", { name: "Configurar disponibilidad" });
  const sent = fetcher.mock.calls.find(([url]) =>
    String(url).endsWith("/preview"),
  );
  expect(JSON.parse(String(sent?.[1]?.body))).toMatchObject({
    objective: "",
    startLocal: "",
    endLocal: "",
    zoneId: "",
  });
});
it("@s40 configuración permite dos reintentos y retira errores durante espera y éxito", async () => {
  window.history.replaceState(null, "", route);
  let requests = 0;
  let finishRetry!: (response: Response) => void;
  fixture((url) => {
    if (url !== "/api/v1/me/availability") return;
    if (++requests === 1) return new Response(null, { status: 503 });
    if (requests === 2)
      return new Promise((resolve) => {
        finishRetry = resolve;
      });
  });
  render(<App />);
  fireEvent.click(
    await screen.findByRole("button", { name: "Planificar bloque" }),
  );
  fireEvent.click(
    await screen.findByRole("button", { name: "Reintentar configuración" }),
  );
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  await act(async () => finishRetry(new Response(null, { status: 503 })));
  fireEvent.click(
    await screen.findByRole("button", { name: "Reintentar configuración" }),
  );
  await waitFor(() =>
    expect(screen.getByLabelText("Zona del bloque")).toHaveValue(
      "Europe/Madrid",
    ),
  );
  expect(requests).toBe(3);
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  expect(
    screen.queryByRole("button", { name: "Reintentar configuración" }),
  ).not.toBeInTheDocument();
});
it("@s42 ocurrencias muestran UTC cero y cambiar fin o zona retira sólo offsets obsoletos", async () => {
  window.history.replaceState(null, "", route);
  let reviews = 0;
  const fetcher = fixture((url) => {
    if (!url.endsWith("/preview")) return;
    const field = ++reviews === 1 ? "startOffset" : "endOffset";
    return problem("VALIDATION_ERROR", 400, {
      errors: [
        { field, code: "AMBIGUOUS_OFFSET", message: "Elige una ocurrencia" },
      ],
      validOffsets: { [field]: ["+01:00", "Z"] },
    });
  });
  render(<App />);
  await openEditor();
  fireEvent.click(screen.getByRole("button", { name: "Revisar bloque" }));
  const start = await screen.findByLabelText("Ocurrencia de inicio");
  expect(start).toHaveAttribute("aria-invalid", "true");
  expect(within(start).getByRole("option", { name: "UTC+00:00" })).toHaveValue(
    "Z",
  );
  expect(within(start).getByRole("option", { name: "UTC+01:00" })).toHaveValue(
    "+01:00",
  );
  fireEvent.change(start, { target: { value: "+01:00" } });
  fireEvent.click(screen.getByRole("button", { name: "Revisar bloque" }));
  const end = await screen.findByLabelText("Ocurrencia de fin");
  expect(end).toHaveAttribute("aria-invalid", "true");
  expect(start).toHaveValue("+01:00");
  fireEvent.change(end, { target: { value: "Z" } });
  expect(end).toHaveValue("Z");
  fireEvent.change(screen.getByLabelText("Fin del bloque"), {
    target: { value: "2030-01-07T11:30" },
  });
  expect(screen.queryByLabelText("Ocurrencia de fin")).not.toBeInTheDocument();
  expect(start).toHaveValue("+01:00");
  fireEvent.click(screen.getByRole("button", { name: "Revisar bloque" }));
  await screen.findByLabelText("Ocurrencia de fin");
  let sent = fetcher.mock.calls.filter(([url]) =>
    String(url).endsWith("/preview"),
  );
  expect(JSON.parse(String(sent[2][1]?.body))).toMatchObject({
    startOffset: "+01:00",
    endOffset: null,
  });
  fireEvent.change(screen.getByLabelText("Zona del bloque"), {
    target: { value: "Europe/Madrid" },
  });
  expect(
    screen.queryByLabelText("Ocurrencia de inicio"),
  ).not.toBeInTheDocument();
  expect(screen.queryByLabelText("Ocurrencia de fin")).not.toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", { name: "Revisar bloque" }));
  await screen.findByLabelText("Ocurrencia de fin");
  sent = fetcher.mock.calls.filter(([url]) => String(url).endsWith("/preview"));
  expect(JSON.parse(String(sent[3][1]?.body))).toMatchObject({
    startOffset: null,
    endOffset: null,
    zoneId: "Europe/Madrid",
  });
});
it("@s39 bloque guardado muestra fecha local española con segundos y desfase de su zona", async () => {
  window.history.replaceState(null, "", route);
  fixture((url) =>
    url === `${api}/blocks`
      ? Response.json({
          items: [{ ...block, zoneId: "Europe/Madrid" }],
          nextCursor: null,
        })
      : undefined,
  );
  render(<App />);
  const list = await screen.findByRole("list", {
    name: "Bloques planificados",
  });
  const start = within(list).getByText(
    "7 ene 2030, 11:00:00 (GMT+01:00) · Europe/Madrid",
  );
  expect(start).toHaveAttribute("datetime", block.startAt);
  expect(
    within(list).getByText("7 ene 2030, 12:00:00 (GMT+01:00) · Europe/Madrid"),
  ).toHaveAttribute("datetime", block.endAt);
  expect(start.parentElement).toHaveTextContent("Inicio: 7 ene 2030");
  expect(
    screen.queryByText("Todavía no hay bloques planificados para esta tarea."),
  ).not.toBeInTheDocument();
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
});
it("@s52 consulta del conflicto distingue espera error y reintento sin alertas falsas", async () => {
  window.history.replaceState(null, "", route);
  let finish!: (response: Response) => void;
  fixture((url) => {
    if (url.endsWith("/preview"))
      return problem("BLOCK_OVERLAP", 409, {
        conflict: { id: block.id, projectId: project.id, taskId: task.id },
      });
    if (url === `${api}/blocks/${block.id}`)
      return new Promise((resolve) => {
        finish = resolve;
      });
  });
  render(<App />);
  await openEditor();
  fireEvent.click(screen.getByRole("button", { name: "Revisar bloque" }));
  const conflict = await screen.findByRole("region", {
    name: "Bloque en conflicto",
  });
  const consult = within(conflict).getByRole("button", {
    name: "Consultar bloque en conflicto",
  });
  expect(within(conflict).queryByRole("alert")).not.toBeInTheDocument();
  expect(within(conflict).queryByRole("status")).not.toBeInTheDocument();
  fireEvent.click(consult);
  expect(consult).toBeDisabled();
  expect(within(conflict).getByRole("status")).toHaveTextContent(
    "Consultando bloque en conflicto",
  );
  await act(async () => finish(new Response(null, { status: 503 })));
  expect(within(conflict).getByRole("alert")).toBeVisible();
  expect(within(conflict).queryByRole("status")).not.toBeInTheDocument();
  expect(consult).toBeEnabled();
  fireEvent.click(consult);
  expect(within(conflict).queryByRole("alert")).not.toBeInTheDocument();
  expect(consult).toBeDisabled();
  await act(async () => finish(Response.json(block)));
  expect(
    within(conflict).getByRole("heading", { name: block.objective }),
  ).toBeVisible();
  expect(within(conflict).queryByRole("alert")).not.toBeInTheDocument();
  expect(within(conflict).queryByRole("status")).not.toBeInTheDocument();
});
it("@s53 StrictMode descarta listado y configuración anteriores al montaje vigente", async () => {
  window.history.replaceState(null, "", route);
  let oldList!: (response: Response) => void;
  let oldConfiguration!: (response: Response) => void;
  let lists = 0;
  let configurations = 0;
  fixture((url) => {
    if (url === `${api}/blocks`) {
      if (++lists === 1)
        return new Promise((resolve) => {
          oldList = resolve;
        });
      return Response.json({ items: [block], nextCursor: null });
    }
    if (url === "/api/v1/me/availability" && ++configurations === 1)
      return new Promise((resolve) => {
        oldConfiguration = resolve;
      });
  });
  render(
    <StrictMode>
      <App />
    </StrictMode>,
  );
  const list = await screen.findByRole("list", {
    name: "Bloques planificados",
  });
  expect(
    within(list).getByRole("heading", { name: block.objective }),
  ).toBeVisible();
  await act(async () =>
    oldList(Response.json({ items: [], nextCursor: null })),
  );
  expect(
    within(list).getByRole("heading", { name: block.objective }),
  ).toBeVisible();
  await openEditor();
  await act(async () =>
    oldConfiguration(
      Response.json(
        {
          configured: false,
          zoneId: null,
          dailyMinutes: null,
          updatedAt: null,
        },
        { headers: { ETag: '"availability:unconfigured"' } },
      ),
    ),
  );
  expect(screen.getByLabelText("Zona del bloque")).toHaveValue("UTC");
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
});
it("@s48 comprobar guardado bloquea acciones durante espera y fallo desconocido no autoriza reenvío", async () => {
  window.history.replaceState(null, "", route);
  let rejectCheck!: (error: unknown) => void;
  fixture((url, options) => {
    if (url.endsWith("/preview")) return Response.json(preview);
    if (url === `${api}/blocks` && options?.method === "POST")
      return problem("STORAGE_UNAVAILABLE", 503);
    if (url.includes("/by-request/"))
      return new Promise((_resolve, reject) => {
        rejectCheck = reject;
      });
  });
  render(<App />);
  await openEditor();
  fireEvent.click(screen.getByRole("button", { name: "Revisar bloque" }));
  await screen.findByRole("region", { name: "Revisión del bloque" });
  fireEvent.click(screen.getByRole("button", { name: "Guardar bloque" }));
  const check = await screen.findByRole("button", {
    name: "Comprobar guardado",
  });
  fireEvent.click(check);
  expect(check).toBeDisabled();
  expect(
    screen.queryByRole("button", { name: "Reenviar el mismo bloque" }),
  ).not.toBeInTheDocument();
  await act(async () => rejectCheck(new TypeError("Sin conexión")));
  expect(check).toBeEnabled();
  expect(
    screen.queryByRole("button", { name: "Reenviar el mismo bloque" }),
  ).not.toBeInTheDocument();
  expect(screen.getByRole("alert")).toHaveTextContent(
    "No podemos confirmar el guardado. Conservamos este bloque y su identificación para comprobarlo sin duplicarlo.",
  );
  expect(screen.getByLabelText("Objetivo del bloque")).toBeDisabled();
});
it("@s56 reconsulta de proyecto pendiente o fallida no reutiliza su snapshot elegible", async () => {
  window.history.replaceState(null, "", route);
  let projectReads = 0;
  let finishProject!: (response: Response) => void;
  fixture((url, options) => {
    if (url === `/api/v1/projects/${project.id}/status`)
      return new Response(null, { status: 412 });
    if (
      url === `/api/v1/projects/${project.id}` &&
      options?.method !== "PUT" &&
      ++projectReads === 2
    )
      return new Promise((resolve) => {
        finishProject = resolve;
      });
  });
  render(<App />);
  await screen.findByRole("button", { name: "Planificar bloque" });
  fireEvent.click(screen.getByRole("button", { name: "Pausar" }));
  fireEvent.click(
    await screen.findByRole("button", { name: "Recargar versión guardada" }),
  );
  expect(screen.getByText("Consultando proyecto")).toBeVisible();
  expect(
    screen.queryByRole("button", { name: "Planificar bloque" }),
  ).not.toBeInTheDocument();
  await act(async () => finishProject(new Response(null, { status: 503 })));
  await screen.findByRole("button", { name: "Reintentar proyecto" });
  expect(
    screen.queryByRole("button", { name: "Planificar bloque" }),
  ).not.toBeInTheDocument();
});
it("@s54 paginar y confirmar desde página antigua retira filas durante cada nueva consulta", async () => {
  window.history.replaceState(null, "", route);
  let finishPage!: (response: Response) => void;
  let recentReads = 0;
  const old = {
    ...block,
    id: "8c5dbd10-9ad5-4000-8000-000000000004",
    objective: "Bloque antiguo",
  };
  const fetcher = fixture((url, options) => {
    if (url.endsWith("/preview")) return Response.json(preview);
    if (url === `${api}/blocks` && options?.method === "POST")
      return Response.json(block, {
        status: 201,
        headers: { Location: `${api}/blocks/${block.id}` },
      });
    if (url === `${api}/blocks` && ++recentReads === 1)
      return Response.json({ items: [block], nextCursor: "older" });
    if (url.startsWith(`${api}/blocks`))
      return new Promise((resolve) => {
        finishPage = resolve;
      });
  });
  render(<App />);
  fireEvent.click(
    await screen.findByRole("button", { name: "Ver bloques anteriores" }),
  );
  expect(
    screen.queryByRole("list", { name: "Bloques planificados" }),
  ).not.toBeInTheDocument();
  expect(screen.getByText("Consultando bloques")).toBeVisible();
  await act(async () =>
    finishPage(Response.json({ items: [old], nextCursor: null })),
  );
  fireEvent.click(
    screen.getByRole("button", { name: "Volver a bloques recientes" }),
  );
  expect(screen.queryByText("Bloque antiguo")).not.toBeInTheDocument();
  expect(screen.getByText("Consultando bloques")).toBeVisible();
  await act(async () =>
    finishPage(Response.json({ items: [block], nextCursor: "older" })),
  );
  fireEvent.click(
    screen.getByRole("button", { name: "Ver bloques anteriores" }),
  );
  await act(async () =>
    finishPage(Response.json({ items: [old], nextCursor: null })),
  );
  await openEditor();
  fireEvent.click(screen.getByRole("button", { name: "Revisar bloque" }));
  await screen.findByRole("region", { name: "Revisión del bloque" });
  fireEvent.click(screen.getByRole("button", { name: "Guardar bloque" }));
  await screen.findByText("Bloque guardado");
  expect(screen.queryByText("Bloque antiguo")).not.toBeInTheDocument();
  expect(screen.getByText("Consultando bloques")).toBeVisible();
  expect(
    screen.queryByLabelText("Objetivo del bloque"),
  ).not.toBeInTheDocument();
  expect(String(fetcher.mock.calls.at(-1)?.[0])).toBe(`${api}/blocks`);
  await act(async () =>
    finishPage(Response.json({ items: [block], nextCursor: null })),
  );
  expect(
    screen.queryByRole("button", { name: "Volver a bloques recientes" }),
  ).not.toBeInTheDocument();
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
});
