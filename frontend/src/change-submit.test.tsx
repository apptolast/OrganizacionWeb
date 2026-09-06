import { act, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { ChangeSubmit } from "./change-submit";
import { agendaToday } from "./today-fixture";
import { observeAccess, setCsrfToken } from "./api-client";
import type { BlockState, RetainedChange } from "./reschedule-api";
import { useSession } from "./use-session";

const block = { ...agendaToday().items[0].block, zoneId: "UTC" };
const state: BlockState = {
  block,
  status: "planned",
  updatedAt: block.createdAt,
  revision: `"block:${block.id}:1"`,
};
const props = {
  state,
  onConfirmed: vi.fn(),
  onReload: vi.fn(),
  focusFallback: vi.fn(),
  onRejected: vi.fn(),
  onAccessFailure: vi.fn(),
};
it("@s33 @s38 announces work during sending and checking without removing focus or uncertainty", async () => {
  const sending = deferred<Response>();
  const checking = deferred<Response>();
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockReturnValueOnce(sending.promise)
      .mockReturnValueOnce(checking.promise),
  );
  render(<ChangeSubmit {...props} />);
  expect(screen.queryByRole("status")).not.toBeInTheDocument();
  const send = screen.getByRole("button", {
    name: "Confirmar cancelación del bloque",
  });
  send.focus();
  fireEvent.click(send);
  expect(screen.getByRole("status")).toHaveTextContent("Procesando cambio");
  expect(send).toHaveFocus();
  expect(screen.getByText(block.objective)).toBeVisible();
  await act(async () => sending.resolve(new Response(null, { status: 503 })));
  expect(screen.queryByRole("status")).not.toBeInTheDocument();
  const check = screen.getByRole("button", { name: "Comprobar cambio" });
  check.focus();
  fireEvent.click(check);
  expect(screen.getByRole("status")).toHaveTextContent("Procesando cambio");
  expect(screen.getByRole("alert")).toHaveTextContent(
    "No podemos confirmar el cambio",
  );
  expect(check).toHaveFocus();
  await act(async () =>
    checking.resolve(
      Response.json(problem("BLOCK_CHANGE_NOT_FOUND", 404), { status: 404 }),
    ),
  );
  expect(screen.queryByRole("status")).not.toBeInTheDocument();
  expect(check).toHaveFocus();
});
const movement: Pick<
  Extract<RetainedChange, { kind: "RESCHEDULED" }>,
  "input" | "preview"
> = {
  input: {
    startLocal: "2030-01-07T14:00",
    endLocal: "2030-01-07T15:00",
    zoneId: "UTC",
    startOffset: "Z",
    endOffset: "Z",
    allowOverBudget: false,
  },
  preview: {
    objective: block.objective,
    zoneId: "UTC",
    startAt: "2030-01-07T14:00:00Z",
    endAt: "2030-01-07T15:00:00Z",
    startOffset: "Z",
    endOffset: "Z",
    durationMinutes: 60,
    availabilityEtag: '"availability:00000000-0000-0000-0000-000000000004:1"',
    budgetZoneId: "UTC",
    days: [
      {
        date: "2030-01-07",
        budgetMinutes: 120,
        plannedSeconds: 0,
        requestedSeconds: 3600,
        excessSeconds: 0,
      },
    ],
  },
};
function problem(code: string, status = 409, extra = {}) {
  return {
    type: "urn:organization:problem:" + code.toLowerCase(),
    title: "No disponible",
    code,
    status,
    ...extra,
  };
}
function deferred<T>() {
  let resolve!: (value: T) => void;
  const promise = new Promise<T>((done) => {
    resolve = done;
  });
  return { promise, resolve };
}
afterEach(() => {
  vi.unstubAllGlobals();
  vi.clearAllMocks();
  observeAccess();
  setCsrfToken();
});
it("@s37 ignores valid receipt JSON completed after the editor closes", async () => {
  const receipt = {
    id: "00000000-0000-0000-0000-000000000009",
    blockId: block.id,
    kind: "CANCELLED",
    revision: `"block:${block.id}:2"`,
    occurredAt: "2030-01-07T13:00:00Z",
    before: block,
    after: null,
  };
  let body!: ReadableStreamDefaultController<Uint8Array>;
  const response = new Response(
    new ReadableStream({
      start(value) {
        body = value;
      },
    }),
    {
      status: 201,
      headers: {
        Location: `/api/v1/projects/${block.projectId}/tasks/${block.taskId}/blocks/changes/${receipt.id}`,
      },
    },
  );
  const finished = response.clone();
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(response));
  const view = render(<ChangeSubmit {...props} />);
  fireEvent.click(
    screen.getByRole("button", { name: "Confirmar cancelación del bloque" }),
  );
  await act(async () => {});
  view.unmount();
  await act(async () => {
    body.enqueue(new TextEncoder().encode(JSON.stringify(receipt)));
    body.close();
    await finished.json();
  });
  expect(props.onConfirmed).not.toHaveBeenCalled();
  expect(props.focusFallback).not.toHaveBeenCalled();
});
it("@s35 treats a recognized missing block as definitive without reporting parent access loss", async () => {
  const issue = problem("BLOCK_NOT_FOUND", 404);
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(Response.json(issue, { status: 404 })),
  );
  render(<ChangeSubmit {...props} />);
  fireEvent.click(
    screen.getByRole("button", { name: "Confirmar cancelación del bloque" }),
  );
  await screen.findByRole("alert");
  expect(props.onRejected).toHaveBeenCalledExactlyOnceWith(issue);
  expect(props.onAccessFailure).not.toHaveBeenCalled();
});
it("@s36 @s38 confirms a valid receipt once without stealing moved focus", async () => {
  const receipt = {
    id: "00000000-0000-0000-0000-000000000009",
    blockId: block.id,
    kind: "CANCELLED",
    revision: `"block:${block.id}:2"`,
    occurredAt: "2030-01-07T13:00:00Z",
    before: block,
    after: null,
  };
  const pending = deferred<Response>();
  const fetch = vi.fn().mockReturnValue(pending.promise);
  vi.stubGlobal("fetch", fetch);
  render(
    <>
      <button>Exterior</button>
      <ChangeSubmit {...props} />
    </>,
  );
  const send = screen.getByRole("button", {
    name: "Confirmar cancelación del bloque",
  });
  send.focus();
  fireEvent.click(send);
  fireEvent.click(send);
  expect(fetch).toHaveBeenCalledTimes(1);
  screen.getByRole("button", { name: "Exterior" }).focus();
  await act(async () =>
    pending.resolve(
      Response.json(receipt, {
        status: 201,
        headers: {
          Location: `/api/v1/projects/${block.projectId}/tasks/${block.taskId}/blocks/changes/${receipt.id}`,
        },
      }),
    ),
  );
  expect(props.onConfirmed).toHaveBeenCalledExactlyOnceWith(receipt);
  expect(props.focusFallback).not.toHaveBeenCalled();
  expect(screen.getByRole("button", { name: "Exterior" })).toHaveFocus();
});
it("@s37 ignores delayed CSRF classification after a session removes the editor", async () => {
  let body!: ReadableStreamDefaultController<Uint8Array>;
  const response = new Response(
    new ReadableStream({
      start(value) {
        body = value;
      },
    }),
    { status: 403 },
  );
  const finished = response.clone();
  const observer = vi.fn();
  observeAccess(observer);
  const fetch = vi.fn().mockResolvedValue(response);
  vi.stubGlobal("fetch", fetch);
  const view = render(<ChangeSubmit {...props} />);
  fireEvent.click(
    screen.getByRole("button", { name: "Confirmar cancelación del bloque" }),
  );
  await act(async () => {});
  view.unmount();
  await act(async () => {
    body.enqueue(
      new TextEncoder().encode(JSON.stringify(problem("CSRF_INVALID", 403))),
    );
    body.close();
    await finished.json();
  });
  expect(observer).not.toHaveBeenCalled();
  expect(props.onRejected).not.toHaveBeenCalled();
  expect(fetch).toHaveBeenCalledTimes(1);
});
it("@s37 ignores definitive classification whose JSON finishes after editor removal", async () => {
  let body!: ReadableStreamDefaultController<Uint8Array>;
  const response = new Response(
    new ReadableStream({
      start(value) {
        body = value;
      },
    }),
    { status: 409 },
  );
  const finished = response.clone();
  const fetch = vi.fn().mockResolvedValue(response);
  vi.stubGlobal("fetch", fetch);
  const view = render(<ChangeSubmit {...props} movement={movement} />);
  fireEvent.click(screen.getByRole("button", { name: "Confirmar movimiento" }));
  await act(async () => {});
  view.unmount();
  expect(fetch.mock.calls[0][1].signal.aborted).toBe(true);
  await act(async () => {
    body.enqueue(
      new TextEncoder().encode(JSON.stringify(problem("BLOCK_UNCHANGED"))),
    );
    body.close();
    await finished.json();
  });
  expect(props.onRejected).not.toHaveBeenCalled();
  expect(props.onAccessFailure).not.toHaveBeenCalled();
  expect(props.onConfirmed).not.toHaveBeenCalled();
  expect(fetch).toHaveBeenCalledTimes(1);
});
it.each(["projectId", "taskId", "id"] as const)(
  "@s37 abandons the previous %s request before stale401 reaches session",
  async (field) => {
    const old = deferred<Response>();
    const fetch = vi.fn().mockReturnValue(old.promise);
    const observer = vi.fn();
    observeAccess(observer);
    vi.stubGlobal("fetch", fetch);
    const view = render(<ChangeSubmit {...props} />);
    fireEvent.click(
      screen.getByRole("button", { name: "Confirmar cancelación del bloque" }),
    );
    const nextBlock = {
      ...block,
      [field]: "00000000-0000-0000-0000-000000000099",
    };
    view.rerender(
      <ChangeSubmit
        {...props}
        state={{
          ...state,
          block: nextBlock,
          revision: `"block:${nextBlock.id}:1"`,
        }}
      />,
    );
    expect(fetch.mock.calls[0][1].signal.aborted).toBe(true);
    await act(async () => old.resolve(new Response(null, { status: 401 })));
    expect(observer).not.toHaveBeenCalled();
    expect(props.onAccessFailure).not.toHaveBeenCalled();
    expect(props.onConfirmed).not.toHaveBeenCalled();
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "Confirmar cancelación del bloque" }),
    ).toHaveAttribute("aria-disabled", "false");
  },
);
it("@s35 uses manual session recovery and a separate resend preserving intention", async () => {
  function SessionFlow() {
    const session = useSession();
    return session.session?.authenticated ? (
      <>
        <button
          hidden={!session.csrfExpired}
          onClick={() => void session.refresh(true)}
        >
          Recuperar acceso
        </button>
        <ChangeSubmit {...props} movement={movement} />
      </>
    ) : (
      <p>Cargando sesión</p>
    );
  }
  const session = (token: string) =>
    Response.json({
      authenticated: true,
      username: "persona-a",
      csrfToken: token,
      csrfHeaderName: "X-CSRF-TOKEN",
    });
  const renew = deferred<Response>();
  const fetch = vi
    .fn()
    .mockResolvedValueOnce(session("old-token"))
    .mockResolvedValueOnce(
      Response.json(problem("CSRF_INVALID", 403), { status: 403 }),
    )
    .mockReturnValueOnce(renew.promise)
    .mockResolvedValueOnce(new Response(null, { status: 503 }));
  vi.stubGlobal("fetch", fetch);
  render(<SessionFlow />);
  fireEvent.click(
    await screen.findByRole("button", { name: "Confirmar movimiento" }),
  );
  const recover = await screen.findByRole("button", {
    name: "Recuperar acceso",
  });
  const resend = await screen.findByRole("button", {
    name: "Reenviar el mismo cambio",
  });
  expect(fetch).toHaveBeenCalledTimes(2);
  fireEvent.click(recover);
  expect(fetch).toHaveBeenCalledTimes(3);
  await act(async () => renew.resolve(session("new-token")));
  expect(fetch).toHaveBeenCalledTimes(3);
  expect(resend).toBeVisible();
  fireEvent.click(resend);
  await screen.findByRole("button", { name: "Comprobar cambio" });
  expect(fetch).toHaveBeenCalledTimes(4);
  const first = fetch.mock.calls[1][1] as RequestInit;
  const last = fetch.mock.calls[3][1] as RequestInit;
  expect(last.body).toBe(first.body);
  for (const name of ["Idempotency-Key", "If-Match", "Availability-Revision"])
    expect(new Headers(last.headers).get(name)).toBe(
      new Headers(first.headers).get(name),
    );
  expect(new Headers(first.headers).get("X-CSRF-TOKEN")).toBe("old-token");
  expect(new Headers(last.headers).get("X-CSRF-TOKEN")).toBe("new-token");
  expect(props.onRejected).not.toHaveBeenCalled();
});
it.each(["BLOCK_CONFLICT", "BLOCK_CANCELLED"])(
  "@s35 @s38 focuses deliberate state recovery after %s",
  async (code) => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        Response.json(problem(code, code === "BLOCK_CONFLICT" ? 412 : 409), {
          status: code === "BLOCK_CONFLICT" ? 412 : 409,
        }),
      ),
    );
    render(<ChangeSubmit {...props} onRejected={undefined} />);
    const send = screen.getByRole("button", {
      name: "Confirmar cancelación del bloque",
    });
    send.focus();
    fireEvent.click(send);
    const reload = await screen.findByRole("button", {
      name: "Consultar estado actual",
    });
    expect(reload).toHaveFocus();
    expect(props.onReload).not.toHaveBeenCalled();
    fireEvent.click(reload);
    expect(props.onReload).toHaveBeenCalledOnce();
  },
);
it.each([false, true])(
  "@s38 preserves sending/checking focus without stealing an external control (outside=%s)",
  async (outside) => {
    const pending = deferred<Response>();
    const checking = deferred<Response>();
    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockReturnValueOnce(pending.promise)
        .mockReturnValueOnce(checking.promise),
    );
    render(
      <>
        <button>Exterior</button>
        <ChangeSubmit {...props} />
      </>,
    );
    const send = screen.getByRole("button", {
      name: "Confirmar cancelación del bloque",
    });
    send.focus();
    fireEvent.click(send);
    expect(send).toHaveFocus();
    expect(send).toHaveAttribute("aria-disabled", "true");
    if (outside) screen.getByRole("button", { name: "Exterior" }).focus();
    await act(async () => pending.resolve(new Response(null, { status: 503 })));
    const check = screen.getByRole("button", { name: "Comprobar cambio" });
    expect(
      outside ? screen.getByRole("button", { name: "Exterior" }) : check,
    ).toHaveFocus();
    check.focus();
    fireEvent.click(check);
    expect(check).toHaveFocus();
    if (outside) screen.getByRole("button", { name: "Exterior" }).focus();
    await act(async () =>
      checking.resolve(
        Response.json(problem("BLOCK_CHANGE_NOT_FOUND", 404), { status: 404 }),
      ),
    );
    expect(
      outside ? screen.getByRole("button", { name: "Exterior" }) : check,
    ).toHaveFocus();
  },
);
it.each([false, true])(
  "@s34 checks absence then manually resends the retained request (move=%s)",
  async (move) => {
    const fetch = vi
      .fn()
      .mockResolvedValueOnce(new Response(null, { status: 503 }))
      .mockResolvedValueOnce(
        Response.json(problem("BLOCK_CHANGE_NOT_FOUND", 404), { status: 404 }),
      )
      .mockResolvedValueOnce(new Response(null, { status: 503 }));
    vi.stubGlobal("fetch", fetch);
    render(<ChangeSubmit {...props} movement={move ? movement : undefined} />);
    fireEvent.click(
      screen.getByRole("button", {
        name: move
          ? "Confirmar movimiento"
          : "Confirmar cancelación del bloque",
      }),
    );
    fireEvent.click(
      await screen.findByRole("button", { name: "Comprobar cambio" }),
    );
    const resend = await screen.findByRole("button", {
      name: "Reenviar el mismo cambio",
    });
    expect(fetch).toHaveBeenCalledTimes(2);
    const first = fetch.mock.calls[0][1] as RequestInit;
    expect(fetch.mock.calls[1][0]).toContain(
      "/changes/by-request/" +
        new Headers(first.headers).get("Idempotency-Key"),
    );
    fireEvent.click(resend);
    await screen.findByRole("button", { name: "Comprobar cambio" });
    expect(fetch).toHaveBeenCalledTimes(3);
    const last = fetch.mock.calls[2][1] as RequestInit;
    expect(last.body).toBe(first.body);
    expect(new Headers(last.headers)).toEqual(new Headers(first.headers));
    expect(fetch.mock.calls[2][0]).toBe(fetch.mock.calls[0][0]);
    expect(props.onRejected).not.toHaveBeenCalled();
  },
);
it.each(["network", "storage", "unknown", "invalid", "idempotency"])(
  "@s33 keeps %s uncertain without another intention",
  async (outcome) => {
    const fetch = vi.fn();
    if (outcome === "network")
      fetch.mockRejectedValue(new TypeError("Offline"));
    else
      fetch.mockResolvedValue(
        outcome === "invalid"
          ? Response.json({}, { status: 201 })
          : Response.json(
              problem(
                outcome === "storage"
                  ? "STORAGE_UNAVAILABLE"
                  : outcome === "idempotency"
                    ? "IDEMPOTENCY_CONFLICT"
                    : "UNKNOWN",
                outcome === "storage" ? 503 : 409,
              ),
              { status: outcome === "storage" ? 503 : 409 },
            ),
      );
    vi.stubGlobal("fetch", fetch);
    render(<ChangeSubmit {...props} />);
    fireEvent.click(
      screen.getByRole("button", { name: "Confirmar cancelación del bloque" }),
    );
    expect(
      await screen.findByRole("button", { name: "Comprobar cambio" }),
    ).toBeVisible();
    expect(
      screen.queryByRole("button", { name: "Reenviar el mismo cambio" }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole("button", {
        name: "Confirmar cancelación del bloque",
      }),
    ).not.toBeInTheDocument();
    expect(fetch).toHaveBeenCalledTimes(1);
    expect(props.onRejected).not.toHaveBeenCalled();
    expect(props.onConfirmed).not.toHaveBeenCalled();
  },
);
it.each([401, 404])("@s37 reports current access loss %s", async (status) => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        Response.json(
          problem(
            status === 401 ? "AUTHENTICATION_REQUIRED" : "RESOURCE_NOT_FOUND",
            status,
          ),
          { status },
        ),
      ),
  );
  render(<ChangeSubmit {...props} />);
  fireEvent.click(
    screen.getByRole("button", { name: "Confirmar cancelación del bloque" }),
  );
  await screen.findByRole("alert");
  expect(props.onAccessFailure).toHaveBeenCalledExactlyOnceWith(status);
  expect(props.onRejected).not.toHaveBeenCalled();
});
it.each([
  problem("BLOCK_UNCHANGED"),
  problem("BLOCK_VERSION_EXHAUSTED"),
  problem("PROJECT_COMPLETED"),
  problem("TASK_COMPLETED"),
  problem("AVAILABILITY_REQUIRED"),
  problem("AVAILABILITY_ZONE_UNAVAILABLE"),
  problem("AVAILABILITY_CONFLICT", 412),
  problem("PRECONDITION_REQUIRED", 428),
  problem("MALFORMED_JSON", 400),
  problem("VALIDATION_ERROR", 400, {
    errors: [{ field: "startTime", code: "IN_PAST", message: "En el pasado" }],
  }),
  problem("BLOCK_OVERLAP", 409, {
    conflict: {
      id: block.id,
      projectId: block.projectId,
      taskId: block.taskId,
    },
  }),
  problem("BUDGET_EXCEEDED", 409, {
    budgetZoneId: "UTC",
    days: [
      {
        date: "2030-01-07",
        budgetMinutes: 0,
        plannedSeconds: 0,
        requestedSeconds: 3600,
        excessSeconds: 3600,
      },
    ],
  }),
])(
  "@s35 returns recognized movement rejection $code without retaining recovery",
  async (issue) => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(Response.json(issue, { status: issue.status })),
    );
    render(<ChangeSubmit {...props} movement={movement} />);
    fireEvent.click(
      screen.getByRole("button", { name: "Confirmar movimiento" }),
    );
    await screen.findByRole("alert");
    expect(props.onRejected).toHaveBeenCalledExactlyOnceWith(issue);
    expect(
      screen.queryByRole("button", { name: "Comprobar cambio" }),
    ).not.toBeInTheDocument();
  },
);

it("@s35 returns a definitive cancellation rejection for deliberate correction", async () => {
  const issue = problem("BLOCK_CANCELLED");
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(Response.json(issue, { status: 409 })),
  );
  render(<ChangeSubmit {...props} />);
  fireEvent.click(
    screen.getByRole("button", { name: "Confirmar cancelación del bloque" }),
  );
  await screen.findByRole("alert");
  expect(props.onRejected).toHaveBeenCalledExactlyOnceWith(issue);
  expect(
    screen.queryByRole("button", { name: "Comprobar cambio" }),
  ).not.toBeInTheDocument();
  expect(props.onConfirmed).not.toHaveBeenCalled();
});
