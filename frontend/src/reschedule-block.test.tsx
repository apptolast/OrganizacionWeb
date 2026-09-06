import {
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { TaskBlocks } from "./task-blocks";
import { agendaToday } from "./today-fixture";
const block = { ...agendaToday().items[0].block, zoneId: "Europe/Madrid" };
const base = `/api/v1/projects/${block.projectId}/tasks/${block.taskId}/blocks`;
const tag = `"block:${block.id}:1"`;
function mount() {
  return render(
    <TaskBlocks
      projectId={block.projectId}
      taskId={block.taskId}
      taskStatus="pending"
      projectStatus="active"
      onAccessFailure={vi.fn()}
    />,
  );
}
afterEach(() => {
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});
it("@s28 opens one inline movement editor from a deliberate current-state read", async () => {
  const fetch = vi.fn((url: string) =>
    Promise.resolve(
      Response.json(
        url.endsWith("/state")
          ? { block, status: "planned", updatedAt: block.createdAt }
          : { items: [block], nextCursor: null },
        { headers: { ETag: tag } },
      ),
    ),
  );
  vi.stubGlobal("fetch", fetch);
  mount();
  await screen.findByText(block.objective);
  expect(fetch).toHaveBeenCalledTimes(1);
  fireEvent.click(
    screen.getByRole("button", { name: "Mover bloque: " + block.objective }),
  );
  const editor = await screen.findByRole("region", { name: "Mover bloque" });
  expect(await within(editor).findByLabelText("Inicio local")).toHaveValue(
    "2030-01-07T13:00",
  );
  expect(within(editor).getByLabelText("Fin local")).toHaveValue(
    "2030-01-07T14:00",
  );
  expect(within(editor).getByText(block.objective)).toBeInTheDocument();
  expect(
    fetch.mock.calls.filter(([url]) => url.endsWith("/state")),
  ).toHaveLength(1);
  fireEvent.click(
    within(editor).getByRole("button", { name: "Cancelar edición" }),
  );
  expect(
    screen.queryByRole("region", { name: "Mover bloque" }),
  ).not.toBeInTheDocument();
  expect(fetch.mock.calls).toHaveLength(2);
});
it("@s31 state failure is recoverable and cannot enable editing", async () => {
  let reads = 0;
  vi.stubGlobal(
    "fetch",
    vi.fn((url: string) =>
      Promise.resolve(
        url.endsWith("/state")
          ? Response.json(
              { block, status: "planned", updatedAt: block.createdAt },
              { status: ++reads === 1 ? 503 : 200, headers: { ETag: tag } },
            )
          : Response.json({ items: [block], nextCursor: null }),
      ),
    ),
  );
  mount();
  fireEvent.click(
    await screen.findByRole("button", {
      name: "Mover bloque: " + block.objective,
    }),
  );
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "No se pudo consultar el estado del bloque",
  );
  expect(screen.queryByLabelText("Inicio local")).not.toBeInTheDocument();
  fireEvent.click(
    screen.getByRole("button", { name: "Consultar estado actual" }),
  );
  expect(await screen.findByLabelText("Inicio local")).toHaveValue(
    "2030-01-07T13:00",
  );
});
it("@s12 @s36 @s38 cancels a completed task reservation, retires its row and preserves the historical receipt", async () => {
  let cancelled = false;
  const receipt = {
    id: "00000000-0000-0000-0000-000000000007",
    blockId: block.id,
    kind: "CANCELLED",
    revision: `"block:${block.id}:2"`,
    occurredAt: "2030-01-07T11:00:00Z",
    before: block,
    after: null,
  };
  const fetch = vi.fn((url: string, options?: RequestInit) => {
    if (options?.method === "POST") {
      cancelled = true;
      return Promise.resolve(
        Response.json(receipt, {
          status: 201,
          headers: { Location: base + "/changes/" + receipt.id },
        }),
      );
    }
    return Promise.resolve(
      Response.json(
        url.endsWith("/state")
          ? {
              block,
              status: cancelled ? "cancelled" : "planned",
              updatedAt: cancelled ? receipt.occurredAt : block.createdAt,
            }
          : { items: cancelled ? [] : [block], nextCursor: null },
        { headers: { ETag: cancelled ? receipt.revision : tag } },
      ),
    );
  });
  vi.stubGlobal("fetch", fetch);
  render(
    <TaskBlocks
      projectId={block.projectId}
      taskId={block.taskId}
      taskStatus="completed"
      projectStatus="completed"
      onAccessFailure={vi.fn()}
    />,
  );
  fireEvent.click(
    await screen.findByRole("button", {
      name: "Cancelar bloque: " + block.objective,
    }),
  );
  const confirm = await screen.findByRole("button", {
    name: "Confirmar cancelación del bloque",
  });
  confirm.focus();
  fireEvent.click(confirm);
  expect(
    await screen.findByText("Cambio confirmado (hecho histórico)"),
  ).toBeInTheDocument();
  await waitFor(() =>
    expect(
      within(
        screen.getByRole("list", { name: "Bloques planificados" }),
      ).queryByText(block.objective),
    ).not.toBeInTheDocument(),
  );
  expect(
    screen.getByRole("heading", { name: "Bloques planificados" }),
  ).toHaveFocus();
  expect(
    fetch.mock.calls.filter(([, options]) => options?.method === "POST"),
  ).toHaveLength(1);
  expect(fetch.mock.calls.some(([url]) => url.includes("availability"))).toBe(
    false,
  );
});
