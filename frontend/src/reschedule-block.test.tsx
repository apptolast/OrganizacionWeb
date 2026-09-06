import {
  act,
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { TaskBlocks } from "./task-blocks";
import { agendaToday } from "./today-fixture";
import { observeAccess, setCsrfToken } from "./api-client";
import userEvent from "@testing-library/user-event";
const block = { ...agendaToday().items[0].block, zoneId: "Europe/Madrid" };
const base = `/api/v1/projects/${block.projectId}/tasks/${block.taskId}/blocks`;
const tag = `"block:${block.id}:1"`;
const movePreview = {
  objective: block.objective,
  zoneId: block.zoneId,
  startAt: "2030-01-07T13:00:00Z",
  endAt: "2030-01-07T14:00:00Z",
  startOffset: "+01:00",
  endOffset: "+01:00",
  durationMinutes: 60,
  availabilityEtag: '"availability:00000000-0000-0000-0000-000000000004:1"',
  budgetZoneId: "UTC",
  days: [
    {
      date: "2030-01-07",
      budgetMinutes: 120,
      plannedSeconds: 1800,
      requestedSeconds: 3600,
      excessSeconds: 0,
    },
  ],
};
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
  observeAccess();
  setCsrfToken();
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
  expect(
    await within(
      await screen.findByRole("region", { name: "Estado actual del bloque" }),
    ).findByText("Cancelado"),
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
it("@s7 @s29 reviews original and proposed time before allowing a move", async () => {
  const fetch = vi.fn((url: string) =>
    Promise.resolve(
      Response.json(
        url.endsWith("/preview")
          ? movePreview
          : url.endsWith("/state")
            ? { block, status: "planned", updatedAt: block.createdAt }
            : { items: [block], nextCursor: null },
        { headers: { ETag: tag } },
      ),
    ),
  );
  vi.stubGlobal("fetch", fetch);
  mount();
  fireEvent.click(
    await screen.findByRole("button", {
      name: "Mover bloque: " + block.objective,
    }),
  );
  fireEvent.change(await screen.findByLabelText("Inicio local"), {
    target: { value: "2030-01-07T14:00" },
  });
  fireEvent.change(screen.getByLabelText("Fin local"), {
    target: { value: "2030-01-07T15:00" },
  });
  expect(
    screen.queryByRole("button", { name: "Confirmar movimiento" }),
  ).not.toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", { name: "Revisar movimiento" }));
  const review = await screen.findByRole("region", {
    name: "Revisión del movimiento",
  });
  expect(within(review).getByText("Antes")).toBeInTheDocument();
  expect(within(review).getByText("Después")).toBeInTheDocument();
  expect(
    review.querySelector('time[datetime="2030-01-07T12:00:00Z"]'),
  ).toBeInTheDocument();
  expect(
    review.querySelector('time[datetime="2030-01-07T13:00:00Z"]'),
  ).toBeInTheDocument();
  expect(
    screen.getByRole("button", { name: "Confirmar movimiento" }),
  ).toBeInTheDocument();
  expect(
    fetch.mock.calls.filter(([url]) => url.endsWith("/reschedule")),
  ).toHaveLength(0);
});

it("@s8 confirms the reviewed move with both revisions and explicit non-excess consent", async () => {
  const after = {
    ...block,
    startAt: movePreview.startAt,
    endAt: movePreview.endAt,
  };
  const receipt = {
    id: "00000000-0000-0000-0000-000000000007",
    blockId: block.id,
    kind: "RESCHEDULED",
    revision: `"block:${block.id}:2"`,
    occurredAt: "2030-01-07T11:00:00Z",
    before: block,
    after,
  };
  let moved = false;
  const fetch = vi.fn((url: string, options?: RequestInit) => {
    if (url.endsWith("/reschedule") && options?.method === "POST") {
      moved = true;
      return Promise.resolve(
        Response.json(receipt, {
          status: 201,
          headers: { Location: base + "/changes/" + receipt.id },
        }),
      );
    }
    return Promise.resolve(
      Response.json(
        url.endsWith("/preview")
          ? movePreview
          : url.endsWith("/state")
            ? { block, status: "planned", updatedAt: block.createdAt }
            : { items: [moved ? after : block], nextCursor: null },
        { headers: { ETag: tag } },
      ),
    );
  });
  vi.stubGlobal("fetch", fetch);
  mount();
  fireEvent.click(
    await screen.findByRole("button", {
      name: "Mover bloque: " + block.objective,
    }),
  );
  fireEvent.change(await screen.findByLabelText("Inicio local"), {
    target: { value: "2030-01-07T14:00" },
  });
  fireEvent.change(screen.getByLabelText("Fin local"), {
    target: { value: "2030-01-07T15:00" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Revisar movimiento" }));
  const confirm = await screen.findByRole("button", {
    name: "Confirmar movimiento",
  });
  confirm.focus();
  fireEvent.click(confirm);
  expect(
    await screen.findByText("Cambio confirmado (hecho histórico)"),
  ).toBeInTheDocument();
  expect(
    screen.getByRole("heading", { name: "Bloques planificados" }),
  ).toHaveFocus();
  const request = fetch.mock.calls.find(([url]) =>
    url.endsWith("/reschedule"),
  )![1]!;
  expect(new Headers(request.headers).get("If-Match")).toBe(tag);
  expect(new Headers(request.headers).get("Availability-Revision")).toBe(
    movePreview.availabilityEtag,
  );
  expect(JSON.parse(request.body as string)).toEqual({
    startLocal: "2030-01-07T14:00",
    endLocal: "2030-01-07T15:00",
    zoneId: block.zoneId,
    startOffset: "+01:00",
    endOffset: "+01:00",
    allowOverBudget: false,
  });
});

it("@s29 withdraws the movement review when its input changes", async () => {
  const fetch = vi.fn((url: string) =>
    Promise.resolve(
      Response.json(
        url.endsWith("/preview")
          ? movePreview
          : url.endsWith("/state")
            ? { block, status: "planned", updatedAt: block.createdAt }
            : { items: [block], nextCursor: null },
        { headers: { ETag: tag } },
      ),
    ),
  );
  vi.stubGlobal("fetch", fetch);
  mount();
  fireEvent.click(
    await screen.findByRole("button", {
      name: "Mover bloque: " + block.objective,
    }),
  );
  fireEvent.change(await screen.findByLabelText("Inicio local"), {
    target: { value: "2030-01-07T14:00" },
  });
  fireEvent.change(screen.getByLabelText("Fin local"), {
    target: { value: "2030-01-07T15:00" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Revisar movimiento" }));
  await screen.findByRole("button", { name: "Confirmar movimiento" });
  fireEvent.change(screen.getByLabelText("Fin local"), {
    target: { value: "2030-01-07T16:00" },
  });
  expect(
    screen.queryByRole("region", { name: "Revisión del movimiento" }),
  ).not.toBeInTheDocument();
  expect(
    screen.queryByRole("button", { name: "Confirmar movimiento" }),
  ).not.toBeInTheDocument();
});

it("@s37 aborts an obsolete preview and ignores its late success after editing", async () => {
  let resolve!: (response: Response) => void;
  const pending = new Promise<Response>((done) => {
    resolve = done;
  });
  const fetch = vi.fn<
    (url: string, options?: RequestInit) => Promise<Response>
  >((url) =>
    url.endsWith("/preview")
      ? pending
      : Promise.resolve(
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
  fireEvent.click(
    await screen.findByRole("button", {
      name: "Mover bloque: " + block.objective,
    }),
  );
  fireEvent.change(await screen.findByLabelText("Inicio local"), {
    target: { value: "2030-01-07T14:00" },
  });
  fireEvent.change(screen.getByLabelText("Fin local"), {
    target: { value: "2030-01-07T15:00" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Revisar movimiento" }));
  fireEvent.change(screen.getByLabelText("Fin local"), {
    target: { value: "2030-01-07T16:00" },
  });
  expect(
    fetch.mock.calls.find(([url]) => url.endsWith("/preview"))![1]?.signal
      ?.aborted,
  ).toBe(true);
  await act(async () =>
    resolve(Response.json(movePreview, { headers: { ETag: tag } })),
  );
  expect(
    screen.queryByRole("region", { name: "Revisión del movimiento" }),
  ).not.toBeInTheDocument();
  expect(screen.getByLabelText("Fin local")).toHaveValue("2030-01-07T16:00");
});

it("@s37 closing the editor aborts a pending preview before an old 401 reaches the session observer", async () => {
  let resolve!: (response: Response) => void;
  const pending = new Promise<Response>((done) => {
    resolve = done;
  });
  const observer = vi.fn();
  observeAccess(observer);
  const fetch = vi.fn<
    (url: string, options?: RequestInit) => Promise<Response>
  >((url) =>
    url.endsWith("/preview")
      ? pending
      : Promise.resolve(
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
  fireEvent.click(
    await screen.findByRole("button", {
      name: "Mover bloque: " + block.objective,
    }),
  );
  await screen.findByLabelText("Inicio local");
  fireEvent.click(screen.getByRole("button", { name: "Revisar movimiento" }));
  fireEvent.click(screen.getByRole("button", { name: "Cancelar edición" }));
  expect(
    fetch.mock.calls.find(([url]) => url.endsWith("/preview"))![1]?.signal
      ?.aborted,
  ).toBe(true);
  await act(async () => resolve(new Response(null, { status: 401 })));
  expect(observer).not.toHaveBeenCalled();
  expect(
    screen.queryByRole("region", { name: "Mover bloque" }),
  ).not.toBeInTheDocument();
  observeAccess();
});

it("@s35 a failed preview preserves the draft and allows deliberate review again", async () => {
  let previews = 0;
  const fetch = vi.fn((url: string) => {
    if (url.endsWith("/preview"))
      return ++previews === 1
        ? Promise.reject(new TypeError("offline"))
        : Promise.resolve(
            Response.json(movePreview, { headers: { ETag: tag } }),
          );
    return Promise.resolve(
      Response.json(
        url.endsWith("/state")
          ? { block, status: "planned", updatedAt: block.createdAt }
          : { items: [block], nextCursor: null },
        { headers: { ETag: tag } },
      ),
    );
  });
  vi.stubGlobal("fetch", fetch);
  mount();
  fireEvent.click(
    await screen.findByRole("button", {
      name: "Mover bloque: " + block.objective,
    }),
  );
  fireEvent.change(await screen.findByLabelText("Inicio local"), {
    target: { value: "2030-01-07T14:00" },
  });
  fireEvent.change(screen.getByLabelText("Fin local"), {
    target: { value: "2030-01-07T15:00" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Revisar movimiento" }));
  expect(
    await screen.findByText("No se pudo revisar el movimiento."),
  ).toBeInTheDocument();
  expect(screen.getByLabelText("Inicio local")).toHaveValue("2030-01-07T14:00");
  expect(
    screen.queryByRole("button", { name: "Confirmar movimiento" }),
  ).not.toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", { name: "Revisar movimiento" }));
  await screen.findByRole("button", { name: "Confirmar movimiento" });
  expect(
    screen.queryByText("No se pudo revisar el movimiento."),
  ).not.toBeInTheDocument();
});

it("@s34 recovers a movement with lost ACK by its retained request key without another POST", async () => {
  const after = {
    ...block,
    startAt: movePreview.startAt,
    endAt: movePreview.endAt,
  };
  const receipt = {
    id: "00000000-0000-0000-0000-000000000007",
    blockId: block.id,
    kind: "RESCHEDULED",
    revision: `"block:${block.id}:2"`,
    occurredAt: "2030-01-07T11:00:00Z",
    before: block,
    after,
  };
  const fetch = vi.fn<
    (url: string, options?: RequestInit) => Promise<Response>
  >((url) => {
    if (url.endsWith("/reschedule"))
      return Promise.reject(new TypeError("lost ACK"));
    if (url.includes("/by-request/"))
      return Promise.resolve(Response.json(receipt));
    return Promise.resolve(
      Response.json(
        url.endsWith("/preview")
          ? movePreview
          : url.endsWith("/state")
            ? { block, status: "planned", updatedAt: block.createdAt }
            : { items: [block], nextCursor: null },
        { headers: { ETag: tag } },
      ),
    );
  });
  vi.stubGlobal("fetch", fetch);
  const view = mount();
  fireEvent.click(
    await screen.findByRole("button", {
      name: "Mover bloque: " + block.objective,
    }),
  );
  fireEvent.change(await screen.findByLabelText("Inicio local"), {
    target: { value: "2030-01-07T14:00" },
  });
  fireEvent.change(screen.getByLabelText("Fin local"), {
    target: { value: "2030-01-07T15:00" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Revisar movimiento" }));
  fireEvent.click(
    await screen.findByRole("button", { name: "Confirmar movimiento" }),
  );
  await screen.findByText("No podemos confirmar el cambio.");
  view.rerender(
    <TaskBlocks
      projectId={block.projectId}
      taskId={block.taskId}
      taskStatus="completed"
      projectStatus="completed"
      onAccessFailure={vi.fn()}
    />,
  );
  expect(screen.getByLabelText("Inicio local")).toHaveAttribute("readonly");
  expect(screen.getByLabelText("Zona del movimiento")).toHaveAttribute(
    "readonly",
  );
  fireEvent.change(screen.getByLabelText("Inicio local"), {
    target: { value: "2030-01-07T17:00" },
  });
  expect(screen.getByLabelText("Inicio local")).toHaveValue("2030-01-07T14:00");
  fireEvent.click(
    await screen.findByRole("button", { name: "Comprobar cambio" }),
  );
  await screen.findByText("Cambio confirmado (hecho histórico)");
  const posts = fetch.mock.calls.filter(([url]) => url.endsWith("/reschedule"));
  expect(posts).toHaveLength(1);
  const key = new Headers(posts[0][1]?.headers).get("Idempotency-Key");
  expect(
    fetch.mock.calls.find(([url]) => url.includes("/by-request/"))![0],
  ).toBe(base + "/changes/by-request/" + key);
});

it("@s9 requires explicit consent for an excess shown in the movement review", async () => {
  const preview = {
    ...movePreview,
    days: [
      {
        date: "2030-01-07",
        budgetMinutes: 60,
        plannedSeconds: 1800,
        requestedSeconds: 3600,
        excessSeconds: 1800,
      },
    ],
  };
  const after = { ...block, startAt: preview.startAt, endAt: preview.endAt };
  const receipt = {
    id: "00000000-0000-0000-0000-000000000007",
    blockId: block.id,
    kind: "RESCHEDULED",
    revision: `"block:${block.id}:2"`,
    occurredAt: "2030-01-07T11:00:00Z",
    before: block,
    after,
  };
  const fetch = vi.fn<
    (url: string, options?: RequestInit) => Promise<Response>
  >((url) =>
    Promise.resolve(
      Response.json(
        url.endsWith("/reschedule")
          ? receipt
          : url.endsWith("/preview")
            ? preview
            : url.endsWith("/state")
              ? { block, status: "planned", updatedAt: block.createdAt }
              : { items: [block], nextCursor: null },
        url.endsWith("/reschedule")
          ? {
              status: 201,
              headers: { Location: base + "/changes/" + receipt.id },
            }
          : { headers: { ETag: tag } },
      ),
    ),
  );
  vi.stubGlobal("fetch", fetch);
  mount();
  fireEvent.click(
    await screen.findByRole("button", {
      name: "Mover bloque: " + block.objective,
    }),
  );
  fireEvent.change(await screen.findByLabelText("Inicio local"), {
    target: { value: "2030-01-07T14:00" },
  });
  fireEvent.change(screen.getByLabelText("Fin local"), {
    target: { value: "2030-01-07T15:00" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Revisar movimiento" }));
  const confirm = await screen.findByRole("button", {
    name: "Confirmar movimiento",
  });
  expect(confirm).toHaveAttribute("aria-disabled", "true");
  fireEvent.click(confirm);
  expect(fetch.mock.calls.some(([url]) => url.endsWith("/reschedule"))).toBe(
    false,
  );
  expect(
    screen.getByText(
      /presupuesto 60 minutos, reservado 1800 segundos, solicitado 3600 segundos, exceso 1800 segundos/,
    ),
  ).toBeInTheDocument();
  fireEvent.click(
    screen.getByRole("checkbox", { name: /Acepto superar el presupuesto/ }),
  );
  fireEvent.change(screen.getByLabelText("Zona del movimiento"), {
    target: { value: "UTC" },
  });
  expect(
    screen.queryByRole("region", { name: "Revisión del movimiento" }),
  ).not.toBeInTheDocument();
  fireEvent.change(screen.getByLabelText("Zona del movimiento"), {
    target: { value: block.zoneId },
  });
  fireEvent.click(screen.getByRole("button", { name: "Revisar movimiento" }));
  const consent = await screen.findByRole("checkbox", {
    name: /Acepto superar el presupuesto/,
  });
  expect(consent).not.toBeChecked();
  fireEvent.click(consent);
  fireEvent.click(screen.getByRole("button", { name: "Confirmar movimiento" }));
  expect(consent).toBeDisabled();
  fireEvent.click(consent);
  expect(consent).toBeChecked();
  await screen.findByText("Cambio confirmado (hecho histórico)");
  expect(
    JSON.parse(
      fetch.mock.calls.find(([url]) => url.endsWith("/reschedule"))![1]!
        .body as string,
    ).allowOverBudget,
  ).toBe(true);
});

it("@s39 opens history deliberately and refreshes it after a confirmed change", async () => {
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
        url.endsWith("/changes")
          ? { items: cancelled ? [receipt] : [], nextCursor: null }
          : url.endsWith("/state")
            ? { block, status: "planned", updatedAt: block.createdAt }
            : { items: cancelled ? [] : [block], nextCursor: null },
        { headers: { ETag: tag } },
      ),
    );
  });
  vi.stubGlobal("fetch", fetch);
  mount();
  await screen.findByText(block.objective);
  expect(fetch.mock.calls.some(([url]) => url.endsWith("/changes"))).toBe(
    false,
  );
  fireEvent.click(
    screen.getByRole("button", { name: "Ver cambios de bloques" }),
  );
  await waitFor(() =>
    expect(
      fetch.mock.calls.filter(([url]) => url.endsWith("/changes")),
    ).toHaveLength(1),
  );
  fireEvent.click(
    screen.getByRole("button", { name: "Cancelar bloque: " + block.objective }),
  );
  fireEvent.click(
    await screen.findByRole("button", {
      name: "Confirmar cancelación del bloque",
    }),
  );
  await screen.findByText("Cambio confirmado (hecho histórico)");
  await waitFor(() =>
    expect(
      fetch.mock.calls.filter(([url]) => url.endsWith("/changes")),
    ).toHaveLength(2),
  );
  expect(
    await within(
      screen.getByRole("region", { name: "Cambios de bloques" }),
    ).findByText(block.objective),
  ).toBeInTheDocument();
});

it("@s30 shows an unknown historical zone without inventing editable local times", async () => {
  const historical = { ...block, zoneId: "Unknown/Historical" };
  const fetch = vi.fn((url: string, options?: RequestInit) =>
    Promise.resolve(
      Response.json(
        url.endsWith("/state")
          ? { block: historical, status: "planned", updatedAt: block.createdAt }
          : { items: [historical], nextCursor: null },
        { headers: { ETag: tag, ...(options?.method ? {} : {}) } },
      ),
    ),
  );
  vi.stubGlobal("fetch", fetch);
  mount();
  fireEvent.click(
    await screen.findByRole("button", {
      name: "Mover bloque: " + block.objective,
    }),
  );
  expect(
    await screen.findByText(
      "Elige una zona resoluble e introduce las horas del nuevo destino.",
    ),
  ).toBeInTheDocument();
  expect(screen.getByLabelText("Inicio local")).toHaveValue("");
  expect(screen.getByLabelText("Fin local")).toHaveValue("");
  expect(screen.getByLabelText("Zona del movimiento")).toHaveValue("");
  const review = screen.getByRole("button", { name: "Revisar movimiento" });
  expect(review).toHaveAttribute("aria-disabled", "true");
  fireEvent.click(review);
  expect(
    fetch.mock.calls.some(([, options]) => options?.method === "POST"),
  ).toBe(false);
  expect(screen.getByText(/2030-01-07T12:00:00Z UTC/)).toHaveTextContent(
    "Unknown/Historical",
  );
});

it("@s28 @s40 offers server zone suggestions only when the zone field is used", async () => {
  const fetch = vi.fn((url: string) =>
    Promise.resolve(
      Response.json(
        url.endsWith("/zones")
          ? { items: ["Europe/Madrid", "UTC"] }
          : url.endsWith("/state")
            ? { block, status: "planned", updatedAt: block.createdAt }
            : { items: [block], nextCursor: null },
        { headers: { ETag: tag } },
      ),
    ),
  );
  vi.stubGlobal("fetch", fetch);
  mount();
  fireEvent.click(
    await screen.findByRole("button", {
      name: "Mover bloque: " + block.objective,
    }),
  );
  const zone = await screen.findByLabelText("Zona del movimiento");
  expect(fetch.mock.calls.some(([url]) => url.endsWith("/zones"))).toBe(false);
  fireEvent.focus(zone);
  await waitFor(() =>
    expect(
      document.querySelector('datalist option[value="UTC"]'),
    ).toBeInTheDocument(),
  );
  expect(zone).toHaveAttribute("list", document.querySelector("datalist")!.id);
  fireEvent.focus(zone);
  expect(
    fetch.mock.calls.filter(([url]) => url.endsWith("/zones")),
  ).toHaveLength(1);
  expect(fetch.mock.calls.some(([url]) => url.endsWith("/availability"))).toBe(
    false,
  );
});

it("@s40 keeps the zone draft and offers an explicit retry after catalog failure", async () => {
  let zones = 0;
  const fetch = vi.fn((url: string) => {
    if (url.endsWith("/zones"))
      return ++zones === 1
        ? Promise.reject(new TypeError("offline"))
        : Promise.resolve(Response.json({ items: ["Europe/Madrid", "UTC"] }));
    return Promise.resolve(
      Response.json(
        url.endsWith("/state")
          ? { block, status: "planned", updatedAt: block.createdAt }
          : { items: [block], nextCursor: null },
        { headers: { ETag: tag } },
      ),
    );
  });
  vi.stubGlobal("fetch", fetch);
  mount();
  fireEvent.click(
    await screen.findByRole("button", {
      name: "Mover bloque: " + block.objective,
    }),
  );
  const zone = await screen.findByLabelText("Zona del movimiento");
  fireEvent.focus(zone);
  expect(
    await screen.findByText(
      "No se pudieron consultar las sugerencias de zonas.",
    ),
  ).toBeInTheDocument();
  expect(zone).toHaveValue(block.zoneId);
  fireEvent.click(
    screen.getByRole("button", { name: "Reintentar sugerencias de zonas" }),
  );
  await waitFor(() =>
    expect(
      document.querySelector('datalist option[value="UTC"]'),
    ).toBeInTheDocument(),
  );
  expect(
    screen.queryByText("No se pudieron consultar las sugerencias de zonas."),
  ).not.toBeInTheDocument();
});

it("@s37 closing the editor aborts zone suggestions before a late unauthorized response", async () => {
  let resolve!: (response: Response) => void;
  const pending = new Promise<Response>((done) => {
    resolve = done;
  });
  const observer = vi.fn();
  observeAccess(observer);
  const fetch = vi.fn<
    (url: string, options?: RequestInit) => Promise<Response>
  >((url) =>
    url.endsWith("/zones")
      ? pending
      : Promise.resolve(
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
  fireEvent.click(
    await screen.findByRole("button", {
      name: "Mover bloque: " + block.objective,
    }),
  );
  fireEvent.focus(await screen.findByLabelText("Zona del movimiento"));
  fireEvent.click(screen.getByRole("button", { name: "Cancelar edición" }));
  expect(
    fetch.mock.calls.find(([url]) => url.endsWith("/zones"))![1]?.signal
      ?.aborted,
  ).toBe(true);
  await act(async () => resolve(new Response(null, { status: 401 })));
  expect(observer).not.toHaveBeenCalled();
  observeAccess();
});

it("@s36 only offers movement with confirmed eligible task and project while keeping cancellation available", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockImplementation(() =>
        Promise.resolve(Response.json({ items: [block], nextCursor: null })),
      ),
  );
  const view = render(
    <TaskBlocks
      projectId={block.projectId}
      taskId={block.taskId}
      taskStatus="completed"
      projectStatus="active"
      onAccessFailure={vi.fn()}
    />,
  );
  await screen.findByText(block.objective);
  expect(
    screen.queryByRole("button", { name: "Mover bloque: " + block.objective }),
  ).not.toBeInTheDocument();
  expect(
    screen.getByRole("button", { name: "Cancelar bloque: " + block.objective }),
  ).toBeInTheDocument();
  view.rerender(
    <TaskBlocks
      projectId={block.projectId}
      taskId={block.taskId}
      taskStatus="pending"
      onAccessFailure={vi.fn()}
    />,
  );
  expect(
    screen.queryByRole("button", { name: "Mover bloque: " + block.objective }),
  ).not.toBeInTheDocument();
  view.rerender(
    <TaskBlocks
      projectId={block.projectId}
      taskId={block.taskId}
      taskStatus="pending"
      projectStatus="active"
      onAccessFailure={vi.fn()}
    />,
  );
  expect(
    await screen.findByRole("button", {
      name: "Mover bloque: " + block.objective,
    }),
  ).toBeInTheDocument();
});

it("@s36 withdraws a ready movement review when task eligibility becomes unknown", async () => {
  const access = vi.fn();
  vi.stubGlobal(
    "fetch",
    vi.fn((url: string) =>
      Promise.resolve(
        Response.json(
          url.endsWith("/preview")
            ? movePreview
            : url.endsWith("/state")
              ? { block, status: "planned", updatedAt: block.createdAt }
              : { items: [block], nextCursor: null },
          { headers: { ETag: tag } },
        ),
      ),
    ),
  );
  const view = render(
    <TaskBlocks
      projectId={block.projectId}
      taskId={block.taskId}
      taskStatus="pending"
      projectStatus="active"
      onAccessFailure={access}
    />,
  );
  fireEvent.click(
    await screen.findByRole("button", {
      name: "Mover bloque: " + block.objective,
    }),
  );
  fireEvent.change(await screen.findByLabelText("Inicio local"), {
    target: { value: "2030-01-07T14:00" },
  });
  fireEvent.change(screen.getByLabelText("Fin local"), {
    target: { value: "2030-01-07T15:00" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Revisar movimiento" }));
  await screen.findByRole("button", { name: "Confirmar movimiento" });
  view.rerender(
    <TaskBlocks
      projectId={block.projectId}
      taskId={block.taskId}
      projectStatus="active"
      onAccessFailure={access}
    />,
  );
  expect(
    screen.queryByRole("button", { name: "Confirmar movimiento" }),
  ).not.toBeInTheDocument();
  expect(
    screen.getByRole("button", { name: "Revisar movimiento" }),
  ).toHaveAttribute("aria-disabled", "true");
  view.rerender(
    <TaskBlocks
      projectId={block.projectId}
      taskId={block.taskId}
      taskStatus="pending"
      projectStatus="active"
      onAccessFailure={access}
    />,
  );
  expect(
    screen.queryByRole("button", { name: "Confirmar movimiento" }),
  ).not.toBeInTheDocument();
});

it("@s7 @s29 chooses each ambiguous occurrence from server offsets without an implicit default", async () => {
  let previews = 0;
  const preview = {
    ...movePreview,
    startAt: "2030-10-27T00:15:00Z",
    endAt: "2030-10-27T01:45:00Z",
    startOffset: "+02:00",
    endOffset: "+01:00",
    durationMinutes: 90,
    days: [
      {
        date: "2030-10-27",
        budgetMinutes: 120,
        plannedSeconds: 0,
        requestedSeconds: 5400,
        excessSeconds: 0,
      },
    ],
  };
  const fetch = vi.fn((url: string, options?: RequestInit) => {
    if (url.endsWith("/preview")) {
      if (++previews < 3) {
        const field = previews === 1 ? "startOffset" : "endOffset";
        return Promise.resolve(
          Response.json(
            {
              type: "urn:organization:problem:validation_error",
              title: "Validación",
              status: 400,
              code: "VALIDATION_ERROR",
              errors: [
                {
                  field,
                  code: "AMBIGUOUS_OFFSET",
                  message: "Elige una ocurrencia",
                },
              ],
              validOffsets: { [field]: ["+02:00", "+01:00"] },
            },
            { status: 400 },
          ),
        );
      }
      expect(JSON.parse(options!.body as string)).toMatchObject({
        startOffset: "+02:00",
        endOffset: "+01:00",
      });
      return Promise.resolve(
        Response.json(preview, { headers: { ETag: tag } }),
      );
    }
    return Promise.resolve(
      Response.json(
        url.endsWith("/state")
          ? { block, status: "planned", updatedAt: block.createdAt }
          : { items: [block], nextCursor: null },
        { headers: { ETag: tag } },
      ),
    );
  });
  vi.stubGlobal("fetch", fetch);
  mount();
  fireEvent.click(
    await screen.findByRole("button", {
      name: "Mover bloque: " + block.objective,
    }),
  );
  fireEvent.change(await screen.findByLabelText("Inicio local"), {
    target: { value: "2030-10-27T02:15" },
  });
  fireEvent.change(screen.getByLabelText("Fin local"), {
    target: { value: "2030-10-27T02:45" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Revisar movimiento" }));
  const start = await screen.findByLabelText("Ocurrencia de inicio");
  expect(start).toHaveValue("");
  expect(start).toHaveAttribute("aria-invalid", "true");
  expect(start).toHaveAccessibleDescription("Elige una ocurrencia");
  fireEvent.change(start, { target: { value: "+02:00" } });
  expect(start).not.toHaveAttribute("aria-invalid", "true");
  fireEvent.click(screen.getByRole("button", { name: "Revisar movimiento" }));
  const end = await screen.findByLabelText("Ocurrencia de fin");
  expect(end).toHaveValue("");
  expect(end).toHaveAttribute("aria-invalid", "true");
  expect(end).toHaveAccessibleDescription("Elige una ocurrencia");
  fireEvent.change(end, { target: { value: "+01:00" } });
  fireEvent.click(screen.getByRole("button", { name: "Revisar movimiento" }));
  await screen.findByRole("button", { name: "Confirmar movimiento" });
  fireEvent.change(screen.getByLabelText("Ocurrencia de inicio"), {
    target: { value: "+01:00" },
  });
  expect(
    screen.queryByRole("button", { name: "Confirmar movimiento" }),
  ).not.toBeInTheDocument();
  fireEvent.change(screen.getByLabelText("Ocurrencia de inicio"), {
    target: { value: "+02:00" },
  });
  fireEvent.change(screen.getByLabelText("Fin local"), {
    target: { value: "2030-10-27T03:45" },
  });
  expect(screen.queryByLabelText("Ocurrencia de fin")).not.toBeInTheDocument();
  expect(screen.getByLabelText("Ocurrencia de inicio")).toHaveValue("+02:00");
  expect(
    screen.queryByRole("button", { name: "Confirmar movimiento" }),
  ).not.toBeInTheDocument();
  fireEvent.change(screen.getByLabelText("Zona del movimiento"), {
    target: { value: "UTC" },
  });
  expect(
    screen.queryByLabelText("Ocurrencia de inicio"),
  ).not.toBeInTheDocument();
});

it("@s40 Enter in the movement destination reviews it without committing", async () => {
  const fetch = vi.fn((url: string) =>
    Promise.resolve(
      Response.json(
        url.endsWith("/preview")
          ? movePreview
          : url.endsWith("/state")
            ? { block, status: "planned", updatedAt: block.createdAt }
            : { items: [block], nextCursor: null },
        { headers: { ETag: tag } },
      ),
    ),
  );
  vi.stubGlobal("fetch", fetch);
  mount();
  fireEvent.click(
    await screen.findByRole("button", {
      name: "Mover bloque: " + block.objective,
    }),
  );
  fireEvent.change(await screen.findByLabelText("Inicio local"), {
    target: { value: "2030-01-07T14:00" },
  });
  fireEvent.change(screen.getByLabelText("Fin local"), {
    target: { value: "2030-01-07T15:00" },
  });
  screen.getByLabelText("Fin local").focus();
  await userEvent.setup().keyboard("{Enter}");
  await screen.findByRole("button", { name: "Confirmar movimiento" });
  expect(
    fetch.mock.calls.filter(([url]) => url.endsWith("/preview")),
  ).toHaveLength(1);
  expect(fetch.mock.calls.some(([url]) => url.endsWith("/reschedule"))).toBe(
    false,
  );
});

it("@s35 associates a nonexistent local time error with its editable field", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn((url: string) =>
      Promise.resolve(
        Response.json(
          url.endsWith("/preview")
            ? {
                type: "urn:organization:problem:validation_error",
                title: "Validación",
                status: 400,
                code: "VALIDATION_ERROR",
                errors: [
                  {
                    field: "startLocal",
                    code: "NONEXISTENT_LOCAL_TIME",
                    message: "Esta hora local no existe",
                  },
                ],
              }
            : url.endsWith("/state")
              ? { block, status: "planned", updatedAt: block.createdAt }
              : { items: [block], nextCursor: null },
          url.endsWith("/preview")
            ? { status: 400 }
            : { headers: { ETag: tag } },
        ),
      ),
    ),
  );
  mount();
  fireEvent.click(
    await screen.findByRole("button", {
      name: "Mover bloque: " + block.objective,
    }),
  );
  const start = await screen.findByLabelText("Inicio local");
  fireEvent.change(start, { target: { value: "2030-03-31T02:30" } });
  fireEvent.click(screen.getByRole("button", { name: "Revisar movimiento" }));
  expect(
    await screen.findByText("Esta hora local no existe"),
  ).toBeInTheDocument();
  expect(start).toHaveAttribute("aria-invalid", "true");
  expect(
    document.getElementById(start.getAttribute("aria-describedby")!),
  ).toHaveTextContent("Esta hora local no existe");
  expect(start).toHaveValue("2030-03-31T02:30");
});

it("@s35 a movement revision conflict consults actual state while preserving its draft for a new preview", async () => {
  let reads = 0;
  const nextTag = `"block:${block.id}:2"`;
  const fetch = vi.fn<
    (url: string, options?: RequestInit) => Promise<Response>
  >((url) => {
    if (url.endsWith("/reschedule"))
      return Promise.resolve(
        Response.json(
          {
            type: "urn:organization:problem:block_conflict",
            title: "Conflicto",
            status: 412,
            code: "BLOCK_CONFLICT",
          },
          { status: 412 },
        ),
      );
    if (url.endsWith("/state")) {
      reads++;
      if (reads === 2)
        return Promise.resolve(new Response(null, { status: 503 }));
      return Promise.resolve(
        Response.json(
          { block, status: "planned", updatedAt: block.createdAt },
          { headers: { ETag: reads === 1 ? tag : nextTag } },
        ),
      );
    }
    return Promise.resolve(
      Response.json(
        url.endsWith("/preview")
          ? movePreview
          : { items: [block], nextCursor: null },
        { headers: { ETag: reads === 1 ? tag : nextTag } },
      ),
    );
  });
  vi.stubGlobal("fetch", fetch);
  mount();
  fireEvent.click(
    await screen.findByRole("button", {
      name: "Mover bloque: " + block.objective,
    }),
  );
  fireEvent.change(await screen.findByLabelText("Inicio local"), {
    target: { value: "2030-01-07T14:00" },
  });
  fireEvent.change(screen.getByLabelText("Fin local"), {
    target: { value: "2030-01-07T15:00" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Revisar movimiento" }));
  fireEvent.click(
    await screen.findByRole("button", { name: "Confirmar movimiento" }),
  );
  fireEvent.click(
    await screen.findByRole("button", { name: "Consultar estado actual" }),
  );
  await waitFor(() => expect(reads).toBe(2));
  fireEvent.click(
    await screen.findByRole("button", { name: "Consultar estado actual" }),
  );
  await waitFor(() => expect(reads).toBe(3));
  await waitFor(() =>
    expect(
      screen.getByRole("button", { name: "Revisar movimiento" }),
    ).toHaveAttribute("aria-disabled", "false"),
  );
  expect(screen.getByLabelText("Inicio local")).toHaveValue("2030-01-07T14:00");
  expect(screen.getByLabelText("Fin local")).toHaveValue("2030-01-07T15:00");
  fireEvent.click(screen.getByRole("button", { name: "Revisar movimiento" }));
  await screen.findByRole("button", { name: "Confirmar movimiento" });
  const requests = fetch.mock.calls.filter(([url]) => url.endsWith("/preview"));
  expect(new Headers(requests.at(-1)![1]?.headers).get("If-Match")).toBe(
    nextTag,
  );
  expect(
    fetch.mock.calls.filter(([url]) => url.endsWith("/reschedule")),
  ).toHaveLength(1);
});

it("@s37 an absent task context discovered by state lookup is propagated to its owner", async () => {
  const access = vi.fn();
  vi.stubGlobal(
    "fetch",
    vi.fn((url: string) =>
      Promise.resolve(
        Response.json(
          url.endsWith("/state")
            ? {
                type: "urn:organization:problem:resource_not_found",
                title: "No encontrado",
                status: 404,
                code: "RESOURCE_NOT_FOUND",
              }
            : { items: [block], nextCursor: null },
          { status: url.endsWith("/state") ? 404 : 200 },
        ),
      ),
    ),
  );
  render(
    <TaskBlocks
      projectId={block.projectId}
      taskId={block.taskId}
      taskStatus="pending"
      projectStatus="active"
      onAccessFailure={access}
    />,
  );
  fireEvent.click(
    await screen.findByRole("button", {
      name: "Mover bloque: " + block.objective,
    }),
  );
  await waitFor(() => expect(access).toHaveBeenCalledExactlyOnceWith(404));
  expect(screen.queryByLabelText("Inicio local")).not.toBeInTheDocument();
});

it("@s35 a known CSRF rejection retains the cancellation and allows deliberate resend with renewed access", async () => {
  const receipt = {
    id: "00000000-0000-0000-0000-000000000007",
    blockId: block.id,
    kind: "CANCELLED",
    revision: `"block:${block.id}:2"`,
    occurredAt: "2030-01-07T11:00:00Z",
    before: block,
    after: null,
  };
  let posts = 0;
  const fetch = vi.fn((url: string, options?: RequestInit) => {
    if (options?.method === "POST")
      return Promise.resolve(
        ++posts === 1
          ? Response.json(
              {
                type: "urn:organization:problem:csrf_invalid",
                title: "Acceso",
                status: 403,
                code: "CSRF_INVALID",
              },
              { status: 403 },
            )
          : Response.json(receipt, {
              status: 201,
              headers: { Location: base + "/changes/" + receipt.id },
            }),
      );
    return Promise.resolve(
      Response.json(
        url.endsWith("/state")
          ? { block, status: "planned", updatedAt: block.createdAt }
          : { items: [block], nextCursor: null },
        { headers: { ETag: tag } },
      ),
    );
  });
  setCsrfToken("old");
  vi.stubGlobal("fetch", fetch);
  mount();
  fireEvent.click(
    await screen.findByRole("button", {
      name: "Cancelar bloque: " + block.objective,
    }),
  );
  fireEvent.click(
    await screen.findByRole("button", {
      name: "Confirmar cancelación del bloque",
    }),
  );
  const resend = await screen.findByRole("button", {
    name: "Reenviar el mismo cambio",
  });
  expect(posts).toBe(1);
  setCsrfToken("renewed");
  fireEvent.click(resend);
  await screen.findByText("Cambio confirmado (hecho histórico)");
  const sent = fetch.mock.calls.filter(
    ([, options]) => options?.method === "POST",
  );
  expect(sent).toHaveLength(2);
  expect(new Headers(sent[1][1]?.headers).get("Idempotency-Key")).toBe(
    new Headers(sent[0][1]?.headers).get("Idempotency-Key"),
  );
  expect(sent[1][1]?.body).toBe(sent[0][1]?.body);
  expect(new Headers(sent[1][1]?.headers).get("X-CSRF-TOKEN")).toBe("renewed");
  expect(fetch.mock.calls.some(([url]) => url.includes("/by-request/"))).toBe(
    false,
  );
  setCsrfToken();
});

it("@s38 keeps preview focus and coalesces repeated review while awaiting its response", async () => {
  let resolve!: (response: Response) => void;
  const pending = new Promise<Response>((done) => {
    resolve = done;
  });
  const fetch = vi.fn((url: string) =>
    url.endsWith("/preview")
      ? pending
      : Promise.resolve(
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
  fireEvent.click(
    await screen.findByRole("button", {
      name: "Mover bloque: " + block.objective,
    }),
  );
  fireEvent.change(await screen.findByLabelText("Inicio local"), {
    target: { value: "2030-01-07T14:00" },
  });
  fireEvent.change(screen.getByLabelText("Fin local"), {
    target: { value: "2030-01-07T15:00" },
  });
  const review = screen.getByRole("button", { name: "Revisar movimiento" });
  review.focus();
  fireEvent.click(review);
  fireEvent.click(review);
  expect(
    fetch.mock.calls.filter(([url]) => url.endsWith("/preview")),
  ).toHaveLength(1);
  expect(review).toHaveAttribute("aria-disabled", "true");
  expect(review).toHaveFocus();
  expect(screen.getByText("Revisando movimiento")).toHaveAttribute(
    "role",
    "status",
  );
  await act(async () =>
    resolve(Response.json(movePreview, { headers: { ETag: tag } })),
  );
  expect(review).toHaveAttribute("aria-disabled", "false");
  expect(review).toHaveFocus();
  expect(screen.queryByText("Revisando movimiento")).not.toBeInTheDocument();
});

it("@s28 cancellation identifies the interval from current state rather than an older list row", async () => {
  const current = {
    ...block,
    startAt: "2030-01-07T14:00:00Z",
    endAt: "2030-01-07T15:00:00Z",
  };
  vi.stubGlobal(
    "fetch",
    vi.fn((url: string) =>
      Promise.resolve(
        Response.json(
          url.endsWith("/state")
            ? { block: current, status: "planned", updatedAt: block.createdAt }
            : { items: [block], nextCursor: null },
          { headers: { ETag: tag } },
        ),
      ),
    ),
  );
  mount();
  fireEvent.click(
    await screen.findByRole("button", {
      name: "Cancelar bloque: " + block.objective,
    }),
  );
  await screen.findByRole("button", {
    name: "Confirmar cancelación del bloque",
  });
  const panel = screen.getByRole("region", { name: "Cancelar bloque" });
  expect(
    panel.querySelector('time[datetime="2030-01-07T14:00:00Z"]'),
  ).toBeInTheDocument();
  expect(
    panel.querySelector('time[datetime="2030-01-07T15:00:00Z"]'),
  ).toBeInTheDocument();
  expect(
    panel.querySelector('time[datetime="2030-01-07T12:00:00Z"]'),
  ).not.toBeInTheDocument();
});

it("@s35 associates end and zone validation errors with the corresponding field", async () => {
  for (const [field, label] of [
    ["endLocal", "Fin local"],
    ["zoneId", "Zona del movimiento"],
  ]) {
    vi.stubGlobal(
      "fetch",
      vi.fn((url: string) =>
        Promise.resolve(
          Response.json(
            url.endsWith("/preview")
              ? {
                  type: "urn:organization:problem:validation_error",
                  title: "Validación",
                  status: 400,
                  code: "VALIDATION_ERROR",
                  errors: [
                    {
                      field,
                      code: "INVALID_VALUE",
                      message: "Corrige este destino",
                    },
                  ],
                }
              : url.endsWith("/state")
                ? { block, status: "planned", updatedAt: block.createdAt }
                : { items: [block], nextCursor: null },
            url.endsWith("/preview")
              ? { status: 400 }
              : { headers: { ETag: tag } },
          ),
        ),
      ),
    );
    mount();
    fireEvent.click(
      await screen.findByRole("button", {
        name: "Mover bloque: " + block.objective,
      }),
    );
    await screen.findByLabelText("Inicio local");
    fireEvent.click(screen.getByRole("button", { name: "Revisar movimiento" }));
    expect(await screen.findByText("Corrige este destino")).toBeInTheDocument();
    const input = screen.getByLabelText(label);
    expect(input).toHaveAttribute("aria-invalid", "true");
    expect(
      document.getElementById(input.getAttribute("aria-describedby")!),
    ).toHaveTextContent("Corrige este destino");
    cleanup();
  }
});

it("@s35 a definitive budget rejection unlocks the draft and requires a fresh review and consent", async () => {
  let writes = 0;
  const days = [
    {
      date: "2030-01-07",
      budgetMinutes: 0,
      plannedSeconds: 0,
      requestedSeconds: 3600,
      excessSeconds: 3600,
    },
  ];
  const fetch = vi.fn((url: string) => {
    if (url.endsWith("/reschedule")) {
      writes++;
      return Promise.resolve(
        Response.json(
          {
            type: "urn:organization:problem:budget_exceeded",
            title: "Presupuesto",
            status: 409,
            code: "BUDGET_EXCEEDED",
            budgetZoneId: "UTC",
            days,
          },
          { status: 409 },
        ),
      );
    }
    return Promise.resolve(
      Response.json(
        url.endsWith("/preview")
          ? writes
            ? { ...movePreview, days }
            : movePreview
          : url.endsWith("/state")
            ? { block, status: "planned", updatedAt: block.createdAt }
            : { items: [block], nextCursor: null },
        { headers: { ETag: tag } },
      ),
    );
  });
  vi.stubGlobal("fetch", fetch);
  mount();
  fireEvent.click(
    await screen.findByRole("button", {
      name: "Mover bloque: " + block.objective,
    }),
  );
  fireEvent.change(await screen.findByLabelText("Inicio local"), {
    target: { value: "2030-01-07T14:00" },
  });
  fireEvent.change(screen.getByLabelText("Fin local"), {
    target: { value: "2030-01-07T15:00" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Revisar movimiento" }));
  fireEvent.click(
    await screen.findByRole("button", { name: "Confirmar movimiento" }),
  );
  expect(
    await screen.findByText(
      "El presupuesto cambió. Revisa el movimiento antes de decidir.",
    ),
  ).toBeInTheDocument();
  expect(
    screen.queryByRole("region", { name: "Revisión del movimiento" }),
  ).not.toBeInTheDocument();
  expect(screen.getByLabelText("Inicio local")).not.toHaveAttribute("readonly");
  expect(screen.getByLabelText("Inicio local")).toHaveValue("2030-01-07T14:00");
  fireEvent.click(screen.getByRole("button", { name: "Revisar movimiento" }));
  expect(
    await screen.findByRole("checkbox", {
      name: /Acepto superar el presupuesto/,
    }),
  ).not.toBeChecked();
  expect(writes).toBe(1);
});

it("@s38 keyboard entry and closing the inline panel preserve a logical focus destination", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn((url: string) =>
      Promise.resolve(
        Response.json(
          url.endsWith("/state")
            ? { block, status: "planned", updatedAt: block.createdAt }
            : { items: [block], nextCursor: null },
          { headers: { ETag: tag } },
        ),
      ),
    ),
  );
  mount();
  const open = await screen.findByRole("button", {
    name: "Mover bloque: " + block.objective,
  });
  open.focus();
  fireEvent.click(open);
  expect(screen.getByRole("heading", { name: "Mover bloque" })).toHaveFocus();
  const close = screen.getByRole("button", { name: "Cancelar edición" });
  close.focus();
  fireEvent.click(close);
  expect(
    screen.getByRole("heading", { name: "Bloques planificados" }),
  ).toHaveFocus();
});

it("@s37 propagates absent task context from movement preview or sending", async () => {
  for (const phase of ["preview", "reschedule"]) {
    const access = vi.fn();
    vi.stubGlobal(
      "fetch",
      vi.fn((url: string) =>
        Promise.resolve(
          Response.json(
            url.endsWith("/" + phase)
              ? {
                  type: "urn:organization:problem:resource_not_found",
                  title: "No encontrado",
                  status: 404,
                  code: "RESOURCE_NOT_FOUND",
                }
              : url.endsWith("/preview")
                ? movePreview
                : url.endsWith("/state")
                  ? { block, status: "planned", updatedAt: block.createdAt }
                  : { items: [block], nextCursor: null },
            url.endsWith("/" + phase)
              ? { status: 404 }
              : { headers: { ETag: tag } },
          ),
        ),
      ),
    );
    render(
      <TaskBlocks
        projectId={block.projectId}
        taskId={block.taskId}
        taskStatus="pending"
        projectStatus="active"
        onAccessFailure={access}
      />,
    );
    fireEvent.click(
      await screen.findByRole("button", {
        name: "Mover bloque: " + block.objective,
      }),
    );
    fireEvent.change(await screen.findByLabelText("Inicio local"), {
      target: { value: "2030-01-07T14:00" },
    });
    fireEvent.change(screen.getByLabelText("Fin local"), {
      target: { value: "2030-01-07T15:00" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Revisar movimiento" }));
    if (phase === "reschedule")
      fireEvent.click(
        await screen.findByRole("button", { name: "Confirmar movimiento" }),
      );
    await waitFor(() => expect(access).toHaveBeenCalledExactlyOnceWith(404));
    cleanup();
  }
});

it("@s31 does not offer an action when current state is already cancelled", async () => {
  const fetch = vi.fn((url: string) =>
    Promise.resolve(
      Response.json(
        url.endsWith("/state")
          ? { block, status: "cancelled", updatedAt: block.createdAt }
          : { items: [block], nextCursor: null },
        { headers: { ETag: tag } },
      ),
    ),
  );
  vi.stubGlobal("fetch", fetch);
  mount();
  fireEvent.click(
    await screen.findByRole("button", {
      name: "Cancelar bloque: " + block.objective,
    }),
  );
  expect(
    await screen.findByText("Este bloque está cancelado."),
  ).toBeInTheDocument();
  expect(
    screen.queryByRole("button", { name: "Confirmar cancelación del bloque" }),
  ).not.toBeInTheDocument();
  expect(fetch).toHaveBeenCalledTimes(2);
});

it("@s35 consults current state after a revision conflict without resending", async () => {
  let writes = 0;
  const fetch = vi.fn((url: string, options?: RequestInit) => {
    if (options?.method === "POST") {
      writes++;
      return Promise.resolve(
        Response.json(
          {
            type: "urn:organization:problem:block_conflict",
            title: "Conflicto",
            status: 412,
            code: "BLOCK_CONFLICT",
          },
          { status: 412 },
        ),
      );
    }
    return Promise.resolve(
      Response.json(
        url.endsWith("/state")
          ? {
              block,
              status: writes ? "cancelled" : "planned",
              updatedAt: block.createdAt,
            }
          : { items: [block], nextCursor: null },
        { headers: { ETag: tag } },
      ),
    );
  });
  vi.stubGlobal("fetch", fetch);
  mount();
  fireEvent.click(
    await screen.findByRole("button", {
      name: "Cancelar bloque: " + block.objective,
    }),
  );
  fireEvent.click(
    await screen.findByRole("button", {
      name: "Confirmar cancelación del bloque",
    }),
  );
  fireEvent.click(
    await screen.findByRole("button", { name: "Consultar estado actual" }),
  );
  expect(
    await screen.findByText("Este bloque está cancelado."),
  ).toBeInTheDocument();
  expect(
    screen.queryByRole("button", { name: "Reenviar el mismo cambio" }),
  ).not.toBeInTheDocument();
  expect(writes).toBe(1);
});

it("@s33 @s34 keeps an uncertain cancellation and resends only the same intention after receipt absence", async () => {
  let writes = 0;
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
    if (options?.method === "POST")
      return ++writes === 1
        ? Promise.reject(new TypeError("lost ACK"))
        : Promise.resolve(
            Response.json(receipt, {
              status: 200,
              headers: { Location: base + "/changes/" + receipt.id },
            }),
          );
    if (url.includes("/by-request/"))
      return Promise.resolve(
        Response.json(
          {
            type: "urn:organization:problem:block_change_not_found",
            title: "No encontrado",
            status: 404,
            code: "BLOCK_CHANGE_NOT_FOUND",
          },
          { status: 404 },
        ),
      );
    return Promise.resolve(
      Response.json(
        url.endsWith("/state")
          ? { block, status: "planned", updatedAt: block.createdAt }
          : { items: [block], nextCursor: null },
        { headers: { ETag: tag } },
      ),
    );
  });
  vi.stubGlobal("fetch", fetch);
  mount();
  fireEvent.click(
    await screen.findByRole("button", {
      name: "Cancelar bloque: " + block.objective,
    }),
  );
  fireEvent.click(
    await screen.findByRole("button", {
      name: "Confirmar cancelación del bloque",
    }),
  );
  expect(
    await screen.findByText("No podemos confirmar el cambio."),
  ).toBeInTheDocument();
  expect(
    screen.queryByRole("button", { name: "Confirmar cancelación del bloque" }),
  ).not.toBeInTheDocument();
  expect(writes).toBe(1);
  fireEvent.click(screen.getByRole("button", { name: "Comprobar cambio" }));
  fireEvent.click(
    await screen.findByRole("button", { name: "Reenviar el mismo cambio" }),
  );
  await screen.findByText("Cambio confirmado (hecho histórico)");
  const requests = fetch.mock.calls.filter(([, o]) => o?.method === "POST");
  expect(requests).toHaveLength(2);
  expect(requests[1][0]).toBe(requests[0][0]);
  expect(requests[1][1]?.headers).toEqual(requests[0][1]?.headers);
  expect(requests[1][1]?.body).toBe(requests[0][1]?.body);
});

it("@s36 aborts a pending preview when eligibility is withdrawn and ignores its late result", async () => {
  let resolve!: (response: Response) => void;
  let signal: AbortSignal | undefined | null;
  const access = vi.fn();
  vi.stubGlobal(
    "fetch",
    vi.fn((url: string, options?: RequestInit) => {
      if (url.endsWith("/preview")) {
        signal = options?.signal;
        return new Promise<Response>((done) => {
          resolve = done;
        });
      }
      return Promise.resolve(
        Response.json(
          url.endsWith("/state")
            ? { block, status: "planned", updatedAt: block.createdAt }
            : { items: [block], nextCursor: null },
          { headers: { ETag: tag } },
        ),
      );
    }),
  );
  const props = {
    projectId: block.projectId,
    taskId: block.taskId,
    projectStatus: "active" as const,
    onAccessFailure: access,
  };
  const view = render(<TaskBlocks {...props} taskStatus="pending" />);
  fireEvent.click(
    await screen.findByRole("button", {
      name: "Mover bloque: " + block.objective,
    }),
  );
  fireEvent.change(await screen.findByLabelText("Inicio local"), {
    target: { value: "2030-01-07T14:00" },
  });
  fireEvent.change(screen.getByLabelText("Fin local"), {
    target: { value: "2030-01-07T15:00" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Revisar movimiento" }));
  await waitFor(() => expect(signal).toBeDefined());
  view.rerender(<TaskBlocks {...props} />);
  expect(signal?.aborted).toBe(true);
  view.rerender(<TaskBlocks {...props} taskStatus="pending" />);
  await act(async () => {
    resolve(Response.json(movePreview, { headers: { ETag: tag } }));
  });
  expect(
    screen.queryByRole("button", { name: "Confirmar movimiento" }),
  ).not.toBeInTheDocument();
  expect(
    screen.getByRole("button", { name: "Revisar movimiento" }),
  ).toHaveAttribute("aria-disabled", "false");
});

it("@s29 withdraws the previous review while a fresh review is pending", async () => {
  let resolve!: (response: Response) => void;
  let previews = 0;
  vi.stubGlobal(
    "fetch",
    vi.fn((url: string) => {
      if (url.endsWith("/preview")) {
        if (++previews === 2)
          return new Promise<Response>((done) => {
            resolve = done;
          });
        return Promise.resolve(
          Response.json(movePreview, { headers: { ETag: tag } }),
        );
      }
      return Promise.resolve(
        Response.json(
          url.endsWith("/state")
            ? { block, status: "planned", updatedAt: block.createdAt }
            : { items: [block], nextCursor: null },
          { headers: { ETag: tag } },
        ),
      );
    }),
  );
  mount();
  fireEvent.click(
    await screen.findByRole("button", {
      name: "Mover bloque: " + block.objective,
    }),
  );
  fireEvent.change(await screen.findByLabelText("Inicio local"), {
    target: { value: "2030-01-07T14:00" },
  });
  fireEvent.change(screen.getByLabelText("Fin local"), {
    target: { value: "2030-01-07T15:00" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Revisar movimiento" }));
  await screen.findByRole("button", { name: "Confirmar movimiento" });
  fireEvent.click(screen.getByRole("button", { name: "Revisar movimiento" }));
  expect(
    screen.queryByRole("button", { name: "Confirmar movimiento" }),
  ).not.toBeInTheDocument();
  await act(async () => {
    resolve(Response.json(movePreview, { headers: { ETag: tag } }));
  });
  expect(
    screen.getByRole("button", { name: "Confirmar movimiento" }),
  ).toBeInTheDocument();
});

it("@s35 a conflict lookup that finds cancellation prevents any new movement", async () => {
  let reads = 0;
  const fetcher = vi.fn((url: string) => {
    if (url.endsWith("/reschedule"))
      return Promise.resolve(
        Response.json(
          {
            type: "urn:organization:problem:block_conflict",
            title: "Conflict",
            status: 412,
            code: "BLOCK_CONFLICT",
          },
          { status: 412 },
        ),
      );
    if (url.endsWith("/state"))
      return Promise.resolve(
        Response.json(
          {
            block,
            status: ++reads === 1 ? "planned" : "cancelled",
            updatedAt: block.createdAt,
          },
          { headers: { ETag: reads === 1 ? tag : `"block:${block.id}:2"` } },
        ),
      );
    return Promise.resolve(
      Response.json(
        url.endsWith("/preview")
          ? movePreview
          : { items: [block], nextCursor: null },
        { headers: { ETag: tag } },
      ),
    );
  });
  vi.stubGlobal("fetch", fetcher);
  mount();
  fireEvent.click(
    await screen.findByRole("button", {
      name: "Mover bloque: " + block.objective,
    }),
  );
  fireEvent.change(await screen.findByLabelText("Inicio local"), {
    target: { value: "2030-01-07T14:00" },
  });
  fireEvent.change(screen.getByLabelText("Fin local"), {
    target: { value: "2030-01-07T15:00" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Revisar movimiento" }));
  fireEvent.click(
    await screen.findByRole("button", { name: "Confirmar movimiento" }),
  );
  fireEvent.click(
    await screen.findByRole("button", { name: "Consultar estado actual" }),
  );
  await screen.findByText("Este bloque está cancelado.");
  const review = screen.getByRole("button", { name: "Revisar movimiento" });
  expect(review).toHaveAttribute("aria-disabled", "true");
  fireEvent.click(review);
  expect(
    fetcher.mock.calls.filter(([url]) => url.endsWith("/preview")),
  ).toHaveLength(1);
  expect(screen.getByLabelText("Inicio local")).toHaveValue("2030-01-07T14:00");
});

it("@s33 locks occurrence selectors and guards edits while the movement is uncertain", async () => {
  let previews = 0;
  vi.stubGlobal(
    "fetch",
    vi.fn((url: string) => {
      if (url.endsWith("/reschedule"))
        return Promise.reject(new TypeError("lost acknowledgement"));
      if (url.endsWith("/preview") && ++previews <= 2)
        return Promise.resolve(
          Response.json(
            {
              type: "urn:organization:problem:validation_error",
              title: "Validation",
              status: 400,
              code: "VALIDATION_ERROR",
              errors: [
                {
                  field: previews === 1 ? "startOffset" : "endOffset",
                  code: "INVALID_OFFSET",
                  message: "Elige una ocurrencia",
                },
              ],
              validOffsets: {
                [previews === 1 ? "startOffset" : "endOffset"]: ["+01:00"],
              },
            },
            { status: 400 },
          ),
        );
      return Promise.resolve(
        Response.json(
          url.endsWith("/preview")
            ? movePreview
            : url.endsWith("/state")
              ? { block, status: "planned", updatedAt: block.createdAt }
              : { items: [block], nextCursor: null },
          { headers: { ETag: tag } },
        ),
      );
    }),
  );
  mount();
  fireEvent.click(
    await screen.findByRole("button", {
      name: "Mover bloque: " + block.objective,
    }),
  );
  fireEvent.change(await screen.findByLabelText("Inicio local"), {
    target: { value: "2030-01-07T14:00" },
  });
  fireEvent.change(screen.getByLabelText("Fin local"), {
    target: { value: "2030-01-07T15:00" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Revisar movimiento" }));
  fireEvent.change(await screen.findByLabelText("Ocurrencia de inicio"), {
    target: { value: "+01:00" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Revisar movimiento" }));
  fireEvent.change(await screen.findByLabelText("Ocurrencia de fin"), {
    target: { value: "+01:00" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Revisar movimiento" }));
  fireEvent.click(
    await screen.findByRole("button", { name: "Confirmar movimiento" }),
  );
  await screen.findByRole("button", { name: "Comprobar cambio" });
  for (const label of ["Ocurrencia de inicio", "Ocurrencia de fin"]) {
    const select = screen.getByLabelText(label);
    expect(select).toBeDisabled();
    fireEvent.change(select, { target: { value: "" } });
    expect(select).toHaveValue("+01:00");
  }
  expect(
    screen.getByRole("button", { name: "Comprobar cambio" }),
  ).toBeInTheDocument();
});

it("@s35 preserves a definitive unchanged rejection beside the editable draft", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn((url: string) => {
      if (url.endsWith("/reschedule"))
        return Promise.resolve(
          Response.json(
            {
              type: "urn:organization:problem:block_unchanged",
              title: "Unchanged",
              status: 409,
              code: "BLOCK_UNCHANGED",
            },
            { status: 409 },
          ),
        );
      return Promise.resolve(
        Response.json(
          url.endsWith("/preview")
            ? movePreview
            : url.endsWith("/state")
              ? { block, status: "planned", updatedAt: block.createdAt }
              : { items: [block], nextCursor: null },
          { headers: { ETag: tag } },
        ),
      );
    }),
  );
  mount();
  fireEvent.click(
    await screen.findByRole("button", {
      name: "Mover bloque: " + block.objective,
    }),
  );
  fireEvent.change(await screen.findByLabelText("Inicio local"), {
    target: { value: "2030-01-07T14:00" },
  });
  fireEvent.change(screen.getByLabelText("Fin local"), {
    target: { value: "2030-01-07T15:00" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Revisar movimiento" }));
  fireEvent.click(
    await screen.findByRole("button", { name: "Confirmar movimiento" }),
  );
  await screen.findByText(
    "El destino no cambia el bloque. Corrige el borrador y vuelve a revisar.",
  );
  expect(
    screen.queryByRole("button", { name: "Confirmar movimiento" }),
  ).not.toBeInTheDocument();
  expect(screen.getByLabelText("Inicio local")).not.toHaveAttribute("readonly");
});

it("@s35 associates definitive send validation with the draft and permits correction", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn((url: string) => {
      if (url.endsWith("/reschedule"))
        return Promise.resolve(
          Response.json(
            {
              type: "urn:organization:problem:validation_error",
              title: "Validation",
              status: 400,
              code: "VALIDATION_ERROR",
              errors: [
                {
                  field: "endLocal",
                  code: "INVALID_VALUE",
                  message: "Corrige la hora final",
                },
              ],
            },
            { status: 400 },
          ),
        );
      return Promise.resolve(
        Response.json(
          url.endsWith("/preview")
            ? movePreview
            : url.endsWith("/state")
              ? { block, status: "planned", updatedAt: block.createdAt }
              : { items: [block], nextCursor: null },
          { headers: { ETag: tag } },
        ),
      );
    }),
  );
  mount();
  fireEvent.click(
    await screen.findByRole("button", {
      name: "Mover bloque: " + block.objective,
    }),
  );
  fireEvent.change(await screen.findByLabelText("Inicio local"), {
    target: { value: "2030-01-07T14:00" },
  });
  fireEvent.change(screen.getByLabelText("Fin local"), {
    target: { value: "2030-01-07T15:00" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Revisar movimiento" }));
  fireEvent.click(
    await screen.findByRole("button", { name: "Confirmar movimiento" }),
  );
  await waitFor(() =>
    expect(screen.getByLabelText("Fin local")).toHaveAccessibleDescription(
      "Corrige la hora final",
    ),
  );
  expect(screen.getByRole("alert")).toHaveTextContent(
    "El cambio fue rechazado. Corrige el borrador y vuelve a revisar.",
  );
  fireEvent.change(screen.getByLabelText("Fin local"), {
    target: { value: "2030-01-07T16:00" },
  });
  expect(screen.getByLabelText("Fin local")).not.toHaveAttribute(
    "aria-invalid",
    "true",
  );
  expect(
    screen.queryByRole("button", { name: "Confirmar movimiento" }),
  ).not.toBeInTheDocument();
});

it("@s35 preview conflict offers a deliberate state refresh without losing the destination", async () => {
  let reads = 0;
  const nextTag = `"block:${block.id}:2"`;
  const fetcher = vi.fn((url: string, options?: RequestInit) => {
    if (url.endsWith("/state"))
      return Promise.resolve(
        Response.json(
          { block, status: "planned", updatedAt: block.createdAt },
          { headers: { ETag: ++reads === 1 ? tag : nextTag } },
        ),
      );
    if (url.endsWith("/preview"))
      return Promise.resolve(
        reads === 1
          ? Response.json(
              {
                type: "urn:organization:problem:block_conflict",
                title: "Conflict",
                status: 412,
                code: "BLOCK_CONFLICT",
              },
              { status: 412 },
            )
          : Response.json(movePreview, { headers: { ETag: nextTag } }),
      );
    expect(options?.method).not.toBe("POST");
    return Promise.resolve(Response.json({ items: [block], nextCursor: null }));
  });
  vi.stubGlobal("fetch", fetcher);
  mount();
  fireEvent.click(
    await screen.findByRole("button", {
      name: "Mover bloque: " + block.objective,
    }),
  );
  fireEvent.change(await screen.findByLabelText("Inicio local"), {
    target: { value: "2030-01-07T14:00" },
  });
  fireEvent.change(screen.getByLabelText("Fin local"), {
    target: { value: "2030-01-07T15:00" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Revisar movimiento" }));
  fireEvent.click(
    await screen.findByRole("button", { name: "Consultar estado actual" }),
  );
  await waitFor(() => expect(reads).toBe(2));
  expect(screen.getByLabelText("Inicio local")).toHaveValue("2030-01-07T14:00");
  expect(screen.getByLabelText("Fin local")).toHaveValue("2030-01-07T15:00");
  fireEvent.click(screen.getByRole("button", { name: "Revisar movimiento" }));
  await screen.findByRole("button", { name: "Confirmar movimiento" });
  const last = fetcher.mock.calls
    .filter(([url]) => url.endsWith("/preview"))
    .at(-1)!;
  expect(new Headers(last[1]?.headers).get("If-Match")).toBe(nextTag);
  expect(fetcher.mock.calls.some(([url]) => url.endsWith("/reschedule"))).toBe(
    false,
  );
});

it("@s38 restores logical focus during initial and conflict recovery without stealing another control", async () => {
  for (const phase of ["initial", "conflict"] as const) {
    let reads = 0;
    let resolve!: (response: Response) => void;
    vi.stubGlobal(
      "fetch",
      vi.fn((url: string) => {
        if (url.endsWith("/state")) {
          if (++reads === 2)
            return new Promise<Response>((done) => {
              resolve = done;
            });
          return Promise.resolve(
            phase === "initial"
              ? new Response(null, { status: 503 })
              : Response.json(
                  { block, status: "planned", updatedAt: block.createdAt },
                  { headers: { ETag: tag } },
                ),
          );
        }
        if (url.endsWith("/reschedule"))
          return Promise.resolve(
            Response.json(
              {
                type: "urn:organization:problem:block_conflict",
                title: "Conflict",
                status: 412,
                code: "BLOCK_CONFLICT",
              },
              { status: 412 },
            ),
          );
        return Promise.resolve(
          Response.json(
            url.endsWith("/preview")
              ? movePreview
              : { items: [block], nextCursor: null },
            { headers: { ETag: tag } },
          ),
        );
      }),
    );
    mount();
    fireEvent.click(
      await screen.findByRole("button", {
        name: "Mover bloque: " + block.objective,
      }),
    );
    if (phase === "conflict") {
      fireEvent.change(await screen.findByLabelText("Inicio local"), {
        target: { value: "2030-01-07T14:00" },
      });
      fireEvent.change(screen.getByLabelText("Fin local"), {
        target: { value: "2030-01-07T15:00" },
      });
      fireEvent.click(
        screen.getByRole("button", { name: "Revisar movimiento" }),
      );
      fireEvent.click(
        await screen.findByRole("button", { name: "Confirmar movimiento" }),
      );
    }
    const consult = await screen.findByRole("button", {
      name: "Consultar estado actual",
    });
    consult.focus();
    fireEvent.click(consult);
    if (phase === "conflict")
      expect(screen.getByText("Consultando estado actual")).toHaveAttribute(
        "role",
        "status",
      );
    expect(
      screen.getByRole("heading", { name: /^Mover bloque$/ }),
    ).toHaveFocus();
    const close = screen.getByRole("button", { name: "Cancelar edición" });
    close.focus();
    await act(async () => {
      resolve(
        Response.json(
          { block, status: "planned", updatedAt: block.createdAt },
          { headers: { ETag: tag } },
        ),
      );
    });
    expect(close).toHaveFocus();
    expect(
      screen.queryByText("Consultando estado actual"),
    ).not.toBeInTheDocument();
    cleanup();
  }
});

it("@s38 explains that closing does not revoke a transmitted request and ignores its late result", async () => {
  let resolve!: (response: Response) => void;
  const fetcher = vi.fn((url: string, options?: RequestInit) => {
    if (options?.method === "POST")
      return new Promise<Response>((done) => {
        resolve = done;
      });
    return Promise.resolve(
      Response.json(
        url.endsWith("/state")
          ? { block, status: "planned", updatedAt: block.createdAt }
          : { items: [block], nextCursor: null },
        { headers: { ETag: tag } },
      ),
    );
  });
  vi.stubGlobal("fetch", fetcher);
  mount();
  fireEvent.click(
    await screen.findByRole("button", {
      name: "Cancelar bloque: " + block.objective,
    }),
  );
  fireEvent.click(
    await screen.findByRole("button", {
      name: "Confirmar cancelación del bloque",
    }),
  );
  expect(
    screen.getByText(
      "Cerrar la edición no revoca una operación ya enviada. Consulta el historial para comprobar su resultado.",
    ),
  ).toBeVisible();
  fireEvent.click(screen.getByRole("button", { name: "Cancelar edición" }));
  await act(async () => {
    resolve(
      Response.json(
        {
          id: "00000000-0000-0000-0000-000000000007",
          blockId: block.id,
          kind: "CANCELLED",
          revision: `"block:${block.id}:2"`,
          occurredAt: block.createdAt,
          before: block,
          after: null,
        },
        {
          status: 201,
          headers: {
            Location: `${base}/changes/00000000-0000-0000-0000-000000000007`,
          },
        },
      ),
    );
  });
  expect(
    screen.queryByText("Cambio confirmado (hecho histórico)"),
  ).not.toBeInTheDocument();
  expect(
    fetcher.mock.calls.filter(([, options]) => options?.method === "POST"),
  ).toHaveLength(1);
});

it("@s35 removes corrected start and zone errors before displaying a valid review", async () => {
  for (const [field, label, invalid, corrected] of [
    ["startLocal", "Inicio local", "2030-01-07T13:00", "2030-01-07T14:00"],
    ["zoneId", "Zona del movimiento", "Missing/Zone", "Europe/Madrid"],
  ]) {
    let previews = 0;
    vi.stubGlobal(
      "fetch",
      vi.fn((url: string) => {
        if (url.endsWith("/preview") && ++previews === 1)
          return Promise.resolve(
            Response.json(
              {
                type: "urn:organization:problem:validation_error",
                title: "Validation",
                status: 400,
                code: "VALIDATION_ERROR",
                errors: [
                  {
                    field,
                    code: "INVALID_VALUE",
                    message: "Corrige este destino",
                  },
                ],
              },
              { status: 400 },
            ),
          );
        return Promise.resolve(
          Response.json(
            url.endsWith("/preview")
              ? movePreview
              : url.endsWith("/state")
                ? { block, status: "planned", updatedAt: block.createdAt }
                : { items: [block], nextCursor: null },
            { headers: { ETag: tag } },
          ),
        );
      }),
    );
    mount();
    fireEvent.click(
      await screen.findByRole("button", {
        name: "Mover bloque: " + block.objective,
      }),
    );
    fireEvent.change(await screen.findByLabelText("Inicio local"), {
      target: { value: "2030-01-07T14:00" },
    });
    fireEvent.change(screen.getByLabelText("Fin local"), {
      target: { value: "2030-01-07T15:00" },
    });
    fireEvent.change(screen.getByLabelText(label), {
      target: { value: invalid },
    });
    fireEvent.click(screen.getByRole("button", { name: "Revisar movimiento" }));
    await waitFor(() =>
      expect(screen.getByLabelText(label)).toHaveAccessibleDescription(
        "Corrige este destino",
      ),
    );
    fireEvent.change(screen.getByLabelText(label), {
      target: { value: corrected },
    });
    expect(screen.getByLabelText(label)).not.toHaveAttribute(
      "aria-invalid",
      "true",
    );
    fireEvent.click(screen.getByRole("button", { name: "Revisar movimiento" }));
    await screen.findByRole("button", { name: "Confirmar movimiento" });
    expect(screen.queryByText("Corrige este destino")).not.toBeInTheDocument();
    cleanup();
  }
});
