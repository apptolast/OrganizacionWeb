import { act, render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, expect, it, vi } from "vitest";
import { CustomizationControls } from "./customization";
import { CustomFieldsPanel } from "./custom-fields";
import { useCustomizationSession } from "./customization-state";

afterEach(() => vi.unstubAllGlobals());

it("@s35 recovers PROJECT and allows a fresh save while TASK remains uncertain", async () => {
  const writes = { PROJECT: 0, TASK: 0 };
  const requests: { url: string; method: string; body?: string }[] = [];
  const fetcher = vi
    .fn()
    .mockImplementation((url: string, init?: RequestInit) => {
      const scope = url.includes("/PROJECT") ? "PROJECT" : "TASK";
      const method = init?.method ?? "GET";
      requests.push({ url, method, body: init?.body as string | undefined });
      if (method === "PUT" && ++writes[scope] === 1)
        return Promise.resolve(new Response(null, { status: 503 }));
      const saved = method === "PUT";
      return Promise.resolve(
        Response.json(
          {
            configured: true,
            visibleFields: saved ? ["createdAt", "updatedAt"] : ["createdAt"],
            customFields: [],
            updatedAt: "2026-09-07T10:00:00Z",
          },
          {
            headers: {
              ETag: `"customization:${scope}:12345678-1234-1234-1234-123456789abc:${saved ? 1 : 0}"`,
            },
          },
        ),
      );
    });
  vi.stubGlobal("fetch", fetcher);
  function Surface() {
    const session = useCustomizationSession();
    return (
      <>
        <section aria-label="Proyecto">
          <CustomizationControls scope="PROJECT" session={session} />
        </section>
        <section aria-label="Tarea">
          <CustomizationControls scope="TASK" session={session} />
        </section>
      </>
    );
  }
  render(<Surface />);
  const user = userEvent.setup();
  const project = within(screen.getByRole("region", { name: "Proyecto" }));
  const task = within(screen.getByRole("region", { name: "Tarea" }));
  for (const surface of [project, task]) {
    await user.click(
      await surface.findByRole("button", { name: "Personalizar vista" }),
    );
    await user.click(surface.getByRole("checkbox", { name: "Actualizado" }));
    await user.click(surface.getByRole("button", { name: "Guardar vista" }));
    expect(await surface.findByRole("alert")).toHaveTextContent(
      "No se pudo confirmar el guardado",
    );
  }
  await user.click(project.getByRole("button", { name: "Recargar guardado" }));
  await user.click(
    await project.findByRole("button", { name: "Personalizar vista" }),
  );
  expect(
    project.getByRole("checkbox", { name: "Actualizado" }),
  ).not.toBeChecked();
  await user.click(project.getByRole("checkbox", { name: "Actualizado" }));
  await user.click(project.getByRole("button", { name: "Guardar vista" }));
  expect(await project.findByText("Vista guardada")).toBeInTheDocument();
  expect(task.getByRole("alert")).toHaveTextContent(
    "No se pudo confirmar el guardado",
  );
  expect(
    task.queryByRole("button", { name: "Guardar vista" }),
  ).not.toBeInTheDocument();
  expect(writes).toEqual({ PROJECT: 2, TASK: 1 });
  expect(
    requests.filter(
      (request) => request.method === "GET" && request.url.endsWith("TASK"),
    ),
  ).toHaveLength(1);
  expect(
    requests
      .filter((request) => request.method === "PUT")
      .map((request) => JSON.parse(request.body!)),
  ).toEqual([
    { visibleFields: ["createdAt", "updatedAt"] },
    { visibleFields: ["createdAt", "updatedAt"] },
    { visibleFields: ["createdAt", "updatedAt"] },
  ]);
});

it("@s28 rejects a late values schema from another aggregate even with a higher version", async () => {
  const projectId = "12345678-1234-1234-1234-123456789abc";
  const fieldId = "22345678-1234-1234-1234-123456789abc";
  const schemaId = "32345678-1234-1234-1234-123456789abc";
  let finish!: (response: Response) => void;
  const fetcher = vi.fn().mockImplementation((url: string) => {
    if (url === "/api/v1/me/customization/PROJECT")
      return Promise.resolve(
        Response.json(
          {
            configured: true,
            visibleFields: ["createdAt"],
            customFields: [
              { id: fieldId, label: "Nota", type: "TEXT", active: true },
            ],
            updatedAt: "2026-09-07T10:00:00Z",
          },
          { headers: { ETag: `"customization:PROJECT:${schemaId}:0"` } },
        ),
      );
    if (url === `/api/v1/projects/${projectId}/custom-fields`)
      return new Promise<Response>((resolve) => {
        finish = resolve;
      });
    throw new Error(`Unexpected request: ${url}`);
  });
  vi.stubGlobal("fetch", fetcher);
  function Surface() {
    const session = useCustomizationSession();
    return (
      <>
        <CustomizationControls scope="PROJECT" session={session} />
        <CustomFieldsPanel projectId={projectId} session={session} />
      </>
    );
  }
  render(<Surface />);
  expect(
    await screen.findByRole("button", { name: "Personalizar vista" }),
  ).toBeInTheDocument();
  await waitFor(() => expect(finish).toBeTypeOf("function"));
  await act(async () =>
    finish(
      Response.json(
        {
          configured: true,
          values: [
            { fieldId, label: "Nota", type: "TEXT", value: "Esquema ajeno" },
          ],
          updatedAt: "2026-09-07T10:00:00Z",
        },
        {
          headers: {
            ETag: `"custom-values:PROJECT:${projectId}:schema:42345678-1234-1234-1234-123456789abc:9:values:52345678-1234-1234-1234-123456789abc:0"`,
          },
        },
      ),
    ),
  );
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "No se pudieron consultar los campos personales.",
  );
  expect(
    screen.queryByRole("textbox", { name: "Nota" }),
  ).not.toBeInTheDocument();
  expect(screen.queryByDisplayValue("Esquema ajeno")).not.toBeInTheDocument();
  expect(
    screen.queryByRole("button", { name: "Guardar campos" }),
  ).not.toBeInTheDocument();
  expect(screen.getByRole("button", { name: "Reintentar" })).toBeEnabled();
  expect(fetcher).toHaveBeenCalledTimes(2);
});

it("@s32 @s35 recovers one TASK resource without dropping another resource's visible draft", async () => {
  const projectId = "12345678-1234-1234-1234-123456789abc";
  const taskIds = [
    "22345678-1234-1234-1234-123456789abc",
    "32345678-1234-1234-1234-123456789abc",
  ];
  const fieldId = "42345678-1234-1234-1234-123456789abc";
  const writes = [0, 0];
  const reads = [0, 0];
  const sent: unknown[] = [];
  const fetcher = vi
    .fn()
    .mockImplementation((url: string, init?: RequestInit) => {
      const index = taskIds.findIndex(
        (id) =>
          url === `/api/v1/projects/${projectId}/tasks/${id}/custom-fields`,
      );
      if (index < 0) throw new Error(`Unexpected request: ${url}`);
      const saving = init?.method === "PUT";
      if (saving) {
        sent.push(JSON.parse(init.body as string));
        if (++writes[index] === 1)
          return Promise.resolve(new Response(null, { status: 503 }));
      } else reads[index]++;
      return Promise.resolve(
        Response.json(
          {
            configured: true,
            values: [
              {
                fieldId,
                label: "Nota",
                type: "TEXT",
                value: saving ? "Nueva intención" : `Guardada ${index}`,
              },
            ],
            updatedAt: "2026-09-07T10:00:00Z",
          },
          {
            headers: {
              ETag: `"custom-values:TASK:${taskIds[index]}:schema:52345678-1234-1234-1234-123456789abc:0:values:62345678-1234-1234-1234-123456789abc:${saving ? 1 : 0}"`,
            },
          },
        ),
      );
    });
  vi.stubGlobal("fetch", fetcher);
  function Surface() {
    const session = useCustomizationSession();
    return (
      <>
        {taskIds.map((taskId, index) => (
          <section key={taskId} aria-label={`Tarea ${index}`}>
            <CustomFieldsPanel
              projectId={projectId}
              taskId={taskId}
              session={session}
            />
          </section>
        ))}
      </>
    );
  }
  render(<Surface />);
  const user = userEvent.setup();
  const surfaces = [0, 1].map((index) =>
    within(screen.getByRole("region", { name: `Tarea ${index}` })),
  );
  for (const [index, surface] of surfaces.entries()) {
    const input = await surface.findByRole("textbox", { name: "Nota" });
    await user.clear(input);
    await user.type(input, `Borrador ${index}`);
    if (index === 0) {
      await user.click(surface.getByRole("button", { name: "Guardar campos" }));
      expect(await surface.findByRole("alert")).toHaveTextContent(
        "No se pudo confirmar",
      );
    }
  }
  await user.click(
    surfaces[0].getByRole("button", { name: "Recargar guardado" }),
  );
  const recovered = await surfaces[0].findByRole("textbox", { name: "Nota" });
  expect(recovered).toHaveValue("Guardada 0");
  await user.clear(recovered);
  await user.type(recovered, "Nueva intención");
  await user.click(surfaces[0].getByRole("button", { name: "Guardar campos" }));
  expect(await surfaces[0].findByText("Campos guardados")).toBeInTheDocument();
  expect(surfaces[1].getByRole("textbox", { name: "Nota" })).toHaveValue(
    "Borrador 1",
  );
  expect(
    surfaces[1].getByRole("button", { name: "Guardar campos" }),
  ).toBeEnabled();
  expect(surfaces[1].queryByRole("alert")).not.toBeInTheDocument();
  expect(writes).toEqual([2, 0]);
  expect(reads).toEqual([2, 1]);
  expect(sent).toEqual(
    ["Borrador 0", "Nueva intención"].map((value) => ({
      values: [{ fieldId, value }],
    })),
  );
});
