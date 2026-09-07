import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, expect, it, vi } from "vitest";
import { ProjectTasks } from "./project-tasks";
import { useCustomizationSession } from "./customization-state";
import { App } from "./App";

afterEach(() => {
  vi.unstubAllGlobals();
  window.history.replaceState({}, "", "/");
});
const projectId = "12345678-1234-1234-1234-123456789abc";
const date = "2026-09-07T10:00:00Z";
it("@s29 mounts project customization and hides only optional metadata without row queries", async () => {
  window.history.replaceState({}, "", "/proyectos");
  const unexpected: string[] = [];
  const fetcher = vi.fn().mockImplementation((url: string) => {
    if (url === "/api/v1/projects")
      return Promise.resolve(
        Response.json({
          items: [
            {
              id: projectId,
              name: "Mi proyecto",
              status: "idea",
              createdAt: date,
              updatedAt: date,
            },
          ],
          nextCursor: "older",
        }),
      );
    if (url === "/api/v1/me/customization/PROJECT")
      return Promise.resolve(
        Response.json(
          {
            configured: true,
            visibleFields: [],
            customFields: [],
            updatedAt: date,
          },
          {
            headers: {
              ETag: '"customization:PROJECT:22345678-1234-1234-1234-123456789abc:0"',
            },
          },
        ),
      );
    unexpected.push(url);
    return Promise.reject(new Error("Ruta no prevista"));
  });
  vi.stubGlobal("fetch", fetcher);
  render(<App />);
  expect(
    await screen.findByRole("button", { name: "Personalizar vista" }),
  ).toBeInTheDocument();
  const list = screen.getByRole("list", { name: "Proyectos guardados" });
  expect(
    within(list).getByRole("link", { name: /Mi proyecto/ }),
  ).toHaveAttribute("href", `/proyectos/${projectId}`);
  expect(within(list).getByText("Idea")).toBeInTheDocument();
  expect(within(list).queryByText(/Creado/)).not.toBeInTheDocument();
  expect(screen.getByRole("link", { name: /Más antiguos/ })).toHaveAttribute(
    "href",
    "/proyectos?cursor=older",
  );
  expect(fetcher).toHaveBeenCalledTimes(2);
  expect(unexpected).toEqual([]);
});

it("@s29 renders project timestamps in the configured DOM order with UTC labels", async () => {
  window.history.replaceState({}, "", "/proyectos");
  const updated = "2026-09-08T11:00:00Z";
  const fetcher = vi.fn().mockImplementation((url: string) => {
    if (url === "/api/v1/projects")
      return Promise.resolve(
        Response.json({
          items: [
            {
              id: projectId,
              name: "Mi proyecto",
              status: "idea",
              createdAt: date,
              updatedAt: updated,
            },
          ],
          nextCursor: null,
        }),
      );
    if (url === "/api/v1/me/customization/PROJECT")
      return Promise.resolve(
        Response.json(
          {
            configured: true,
            visibleFields: ["updatedAt", "createdAt"],
            customFields: [],
            updatedAt: date,
          },
          {
            headers: {
              ETag: '"customization:PROJECT:22345678-1234-1234-1234-123456789abc:0"',
            },
          },
        ),
      );
    return Promise.reject(new Error("Ruta no prevista"));
  });
  vi.stubGlobal("fetch", fetcher);
  render(<App />);
  await screen.findByRole("button", { name: "Personalizar vista" });
  const list = screen.getByRole("list", { name: "Proyectos guardados" });
  expect(
    [...list.querySelectorAll("time")].map((time) => time.dateTime),
  ).toEqual([updated, date]);
  expect(
    [...list.querySelectorAll("time")].every((time) =>
      time.textContent?.includes("UTC"),
    ),
  ).toBe(true);
});

it("@s29 @s31 mounts the shared task view without resetting the task creation draft", async () => {
  window.history.replaceState({}, "", `/proyectos/${projectId}`);
  const taskId = "32345678-1234-1234-1234-123456789abc";
  const unexpected: string[] = [];
  const fetcher = vi.fn().mockImplementation((url: string) => {
    if (url === `/api/v1/projects/${projectId}`)
      return Promise.resolve(
        Response.json(
          {
            id: projectId,
            ownerId: "owner",
            name: "Mi proyecto",
            description: "Contexto",
            status: "idea",
            createdAt: date,
            updatedAt: date,
          },
          { headers: { ETag: '"project:0"' } },
        ),
      );
    if (url === `/api/v1/projects/${projectId}/tasks`)
      return Promise.resolve(
        Response.json({
          items: [
            {
              id: taskId,
              projectId,
              title: "Paso uno",
              completionCriterion: "Resultado visible",
              estimatedMinutes: null,
              status: "pending",
              createdAt: date,
              updatedAt: date,
            },
          ],
          nextCursor: null,
        }),
      );
    if (url === `/api/v1/projects/${projectId}/custom-fields`)
      return Promise.resolve(
        Response.json(
          { configured: false, values: [], updatedAt: null },
          {
            headers: {
              ETag: `"custom-values:PROJECT:${projectId}:schema:unconfigured:values:unconfigured"`,
            },
          },
        ),
      );
    if (url === "/api/v1/me/customization/TASK")
      return Promise.resolve(
        Response.json(
          {
            configured: true,
            visibleFields: ["estimatedMinutes", "completionCriterion"],
            customFields: [],
            updatedAt: date,
          },
          {
            headers: {
              ETag: '"customization:TASK:22345678-1234-1234-1234-123456789abc:0"',
            },
          },
        ),
      );
    unexpected.push(url);
    return Promise.reject(new Error("Ruta no prevista"));
  });
  vi.stubGlobal("fetch", fetcher);
  render(<App />);
  const user = userEvent.setup();
  const title = await screen.findByRole("textbox", {
    name: "Título de la tarea",
  });
  await user.type(title, "Mi borrador de negocio");
  await user.click(
    await screen.findByRole("button", { name: "Personalizar vista" }),
  );
  const list = screen.getByRole("list", { name: "Tareas guardadas" });
  expect(
    [...list.querySelectorAll("li p")].map((item) => item.textContent),
  ).toEqual(["Sin estimación", "Resultado visible"]);
  expect(within(list).getByRole("link", { name: "Paso uno" })).toHaveAttribute(
    "href",
    `/proyectos/${projectId}/tareas/${taskId}`,
  );
  expect(within(list).getByText("Pendiente")).toBeInTheDocument();
  expect(title).toHaveValue("Mi borrador de negocio");
  expect(unexpected).toEqual([]);
});

it("@s29 renders task dates in configured order using UTC", async () => {
  const taskId = "32345678-1234-1234-1234-123456789abc";
  const updated = "2026-09-08T12:00:00Z";
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation((url: string) => {
      if (url === `/api/v1/projects/${projectId}/tasks`)
        return Promise.resolve(
          Response.json({
            items: [
              {
                id: taskId,
                projectId,
                title: "Paso uno",
                completionCriterion: "Resultado visible",
                estimatedMinutes: 30,
                status: "pending",
                createdAt: date,
                updatedAt: updated,
              },
            ],
            nextCursor: null,
          }),
        );
      if (url === "/api/v1/me/customization/TASK")
        return Promise.resolve(
          Response.json(
            {
              configured: true,
              visibleFields: ["updatedAt", "createdAt"],
              customFields: [],
              updatedAt: date,
            },
            {
              headers: {
                ETag: '"customization:TASK:22345678-1234-1234-1234-123456789abc:0"',
              },
            },
          ),
        );
      return Promise.reject(new Error("Ruta no prevista"));
    }),
  );
  function Tasks() {
    const session = useCustomizationSession();
    return (
      <ProjectTasks
        projectId={projectId}
        projectStatus="idea"
        onProjectConfirmed={() => {}}
        customization={session}
      />
    );
  }
  render(<Tasks />);
  await screen.findByRole("button", { name: "Personalizar vista" });
  const list = screen.getByRole("list", { name: "Tareas guardadas" });
  expect(
    [...list.querySelectorAll("time")].map((time) => time.dateTime),
  ).toEqual([updated, date]);
  expect(
    [...list.querySelectorAll("time")].every((time) =>
      time.textContent?.includes("UTC"),
    ),
  ).toBe(true);
  expect(within(list).queryByText("Resultado visible")).not.toBeInTheDocument();
});

it("@s32 mounts personal project fields separately from project facts and tasks", async () => {
  window.history.replaceState({}, "", `/proyectos/${projectId}`);
  const requests: string[] = [];
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation((url: string) => {
      requests.push(url);
      if (url === `/api/v1/projects/${projectId}`)
        return Promise.resolve(
          Response.json(
            {
              id: projectId,
              ownerId: "owner",
              name: "Mi proyecto",
              description: "Contexto de negocio",
              status: "idea",
              createdAt: date,
              updatedAt: date,
            },
            { headers: { ETag: '"project:0"' } },
          ),
        );
      if (url === `/api/v1/projects/${projectId}/tasks`)
        return Promise.resolve(Response.json({ items: [], nextCursor: null }));
      if (url === "/api/v1/me/customization/TASK")
        return Promise.resolve(
          Response.json(
            {
              configured: false,
              visibleFields: ["completionCriterion", "estimatedMinutes"],
              customFields: [],
              updatedAt: null,
            },
            { headers: { ETag: '"customization:TASK:unconfigured"' } },
          ),
        );
      if (url === `/api/v1/projects/${projectId}/custom-fields`)
        return Promise.resolve(
          Response.json(
            {
              configured: true,
              values: [
                {
                  fieldId: "52345678-1234-1234-1234-123456789abc",
                  label: "Revisado",
                  type: "BOOLEAN",
                  value: false,
                },
              ],
              updatedAt: date,
            },
            {
              headers: {
                ETag: `"custom-values:PROJECT:${projectId}:schema:22345678-1234-1234-1234-123456789abc:0:values:42345678-1234-1234-1234-123456789abc:0"`,
              },
            },
          ),
        );
      return Promise.reject(new Error("Ruta no prevista"));
    }),
  );
  render(<App />);
  expect(await screen.findByRole("combobox", { name: "Revisado" })).toHaveValue(
    "false",
  );
  expect(
    screen.getByRole("heading", { name: "Campos personales", level: 2 }),
  ).toBeInTheDocument();
  expect(screen.getByText("Contexto de negocio")).toBeInTheDocument();
  expect(
    screen.getByRole("textbox", { name: "Título de la tarea" }),
  ).toBeInTheDocument();
  await screen.findByRole("button", { name: "Personalizar vista" });
  expect(requests.sort()).toEqual(
    [
      `/api/v1/projects/${projectId}`,
      `/api/v1/projects/${projectId}/tasks`,
      `/api/v1/projects/${projectId}/custom-fields`,
      "/api/v1/me/customization/TASK",
    ].sort(),
  );
});

function taskContextFixture(fieldsResponse?: Response) {
  const taskId = "32345678-1234-1234-1234-123456789abc";
  const childId = "62345678-1234-1234-1234-123456789abc";
  const path = `/api/v1/projects/${projectId}/tasks/${taskId}`;
  window.history.replaceState(
    {},
    "",
    `/proyectos/${projectId}/tareas/${taskId}`,
  );
  const unexpected: string[] = [];
  const fetcher = vi.fn().mockImplementation((url: string) => {
    const task = {
      id: taskId,
      projectId,
      title: "Paso uno",
      completionCriterion: "Criterio esencial del detalle",
      estimatedMinutes: null,
      status: "pending",
      createdAt: date,
      updatedAt: date,
    };
    if (url === path) return Promise.resolve(Response.json(task));
    if (url === `/api/v1/projects/${projectId}`)
      return Promise.resolve(
        Response.json(
          {
            id: projectId,
            ownerId: "owner",
            name: "Mi proyecto",
            description: "",
            status: "active",
            createdAt: date,
            updatedAt: date,
          },
          { headers: { ETag: '"project:0"' } },
        ),
      );
    if (url === `${path}/status`)
      return Promise.resolve(
        Response.json(
          { status: "pending", completedAt: null, updatedAt: date },
          { headers: { ETag: `"task:${taskId}:0"` } },
        ),
      );
    if (url === `${path}/parent`)
      return Promise.resolve(Response.json({ parent: null }));
    if ([`${path}/blocks`, `${path}/history`].includes(url))
      return Promise.resolve(Response.json({ items: [], nextCursor: null }));
    if (url === "/api/v1/work-sessions/active")
      return Promise.resolve(Response.json({ session: null }));
    if (url === `${path}/subtasks`)
      return Promise.resolve(
        Response.json({
          items: [
            {
              ...task,
              id: childId,
              title: "Paso pequeño",
              completionCriterion: "Criterio opcional de lista",
            },
          ],
          nextCursor: null,
        }),
      );
    if (url === "/api/v1/me/customization/TASK")
      return Promise.resolve(
        Response.json(
          {
            configured: true,
            visibleFields: [],
            customFields: [],
            updatedAt: date,
          },
          {
            headers: {
              ETag: '"customization:TASK:22345678-1234-1234-1234-123456789abc:0"',
            },
          },
        ),
      );
    if (url === `${path}/custom-fields`)
      return Promise.resolve(
        fieldsResponse ??
          Response.json(
            {
              configured: true,
              values: [
                {
                  fieldId: "52345678-1234-1234-1234-123456789abc",
                  label: "Nota personal",
                  type: "TEXT",
                  value: "Sólo de esta tarea",
                },
              ],
              updatedAt: date,
            },
            {
              headers: {
                ETag: `"custom-values:TASK:${taskId}:schema:22345678-1234-1234-1234-123456789abc:0:values:42345678-1234-1234-1234-123456789abc:0"`,
              },
            },
          ),
      );
    unexpected.push(url);
    return Promise.reject(new Error("Ruta no prevista"));
  });
  vi.stubGlobal("fetch", fetcher);
  return { fetcher, unexpected, taskId, childId };
}

it("@s29 @s32 mounts task values and uses the same task scope for subtasks", async () => {
  const { fetcher, unexpected, childId } = taskContextFixture();
  render(<App />);
  expect(
    await screen.findByRole("textbox", { name: "Nota personal" }),
  ).toHaveValue("Sólo de esta tarea");
  await screen.findByRole("button", { name: "Personalizar vista" });
  const list = screen.getByRole("list", { name: "Subtareas guardadas" });
  expect(
    within(list).getByRole("link", { name: "Paso pequeño" }),
  ).toHaveAttribute("href", `/proyectos/${projectId}/tareas/${childId}`);
  expect(
    within(list).queryByText("Criterio opcional de lista"),
  ).not.toBeInTheDocument();
  expect(screen.getByText("Criterio esencial del detalle")).toBeInTheDocument();
  expect(
    fetcher.mock.calls.filter(([url]) => url.endsWith("/custom-fields")),
  ).toHaveLength(1);
  expect(unexpected).toEqual([]);
});

it("@s37 withdraws the project context when its personal fields return a current 404", async () => {
  window.history.replaceState({}, "", `/proyectos/${projectId}`);
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation((url: string) => {
      if (url === `/api/v1/projects/${projectId}`)
        return Promise.resolve(
          Response.json(
            {
              id: projectId,
              ownerId: "owner",
              name: "Nombre privado",
              description: "Descripción privada",
              status: "idea",
              createdAt: date,
              updatedAt: date,
            },
            { headers: { ETag: '"project:0"' } },
          ),
        );
      if (url === `/api/v1/projects/${projectId}/tasks`)
        return Promise.resolve(Response.json({ items: [], nextCursor: null }));
      if (url === "/api/v1/me/customization/TASK")
        return Promise.resolve(
          Response.json(
            {
              configured: false,
              visibleFields: ["completionCriterion", "estimatedMinutes"],
              customFields: [],
              updatedAt: null,
            },
            { headers: { ETag: '"customization:TASK:unconfigured"' } },
          ),
        );
      if (url === `/api/v1/projects/${projectId}/custom-fields`)
        return Promise.resolve(new Response(null, { status: 404 }));
      return Promise.reject(new Error("Ruta no prevista"));
    }),
  );
  render(<App />);
  expect(
    await screen.findByRole("heading", { name: "Proyecto no encontrado" }),
  ).toBeInTheDocument();
  expect(screen.queryByText("Nombre privado")).not.toBeInTheDocument();
  expect(screen.queryByText("Descripción privada")).not.toBeInTheDocument();
  expect(
    screen.queryByRole("textbox", { name: "Título de la tarea" }),
  ).not.toBeInTheDocument();
});

it("@s37 withdraws the task context on a current personal fields 404", async () => {
  const { unexpected } = taskContextFixture(
    new Response(null, { status: 404 }),
  );
  render(<App />);
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "Esta tarea no está disponible para tu cuenta",
  );
  expect(
    screen.queryByText("Criterio esencial del detalle"),
  ).not.toBeInTheDocument();
  expect(
    screen.queryByRole("heading", { name: "Paso uno" }),
  ).not.toBeInTheDocument();
  expect(unexpected).toEqual([]);
});
