import { setCsrfToken, observeAccess } from "./api-client";
import { App } from "./App";
import { afterEach, expect, it, vi } from "vitest";
import {
  cleanup,
  render,
  screen,
  fireEvent,
  act,
} from "@testing-library/react";
import { WorkSessionReader } from "./work-session-reader";
const session = {
  id: "12345678-1234-1234-1234-123456789abc",
  projectId: "22345678-1234-1234-1234-123456789abc",
  taskId: "32345678-1234-1234-1234-123456789abc",
  startedAt: "2026-09-07T10:00:00.123456Z",
  plannedMinutes: 25,
  plannedEndAt: "2026-09-07T10:25:00.123456Z",
  zoneId: "UTC",
};
const before = {
  session,
  status: "running",
  revision: "1",
  changedAt: session.startedAt,
  workedMicroseconds: "0",
  runningSince: session.startedAt,
};
const closed = {
  ...before,
  status: "closed",
  revision: "2",
  changedAt: "2026-09-07T10:00:01.123457Z",
  workedMicroseconds: "1000001",
  runningSince: null,
};
const closure = {
  progressNote: "Avance parcial",
  nextStep: "Continuar mañana",
  workDate: "2026-09-07",
  closeZoneId: "UTC",
};
const receipt = {
  id: "42345678-1234-1234-1234-123456789abc",
  sessionId: session.id,
  action: "CLOSE",
  occurredAt: closed.changedAt,
  before,
  after: closed,
  closure,
};
const props = {
  id: session.id,
  projectId: session.projectId,
  taskId: session.taskId,
};
function closedResponse() {
  return Response.json(
    {
      state: closed,
      serverNow: "2026-09-08T12:00:00Z",
      netMicroseconds: "1000001",
    },
    { headers: { "Work-Session-Revision": `work-session-${session.id}-2` } },
  );
}
afterEach(() => {
  cleanup();
  setCsrfToken(undefined);
  observeAccess(undefined);
  vi.unstubAllGlobals();
});
it("@s33 recovers the closure by known session URL without an active session or key", async () => {
  const fetcher = vi.fn().mockImplementation((url: string) => {
    if (url.endsWith("/state")) return Promise.resolve(closedResponse());
    if (url.endsWith("/closure"))
      return Promise.resolve(Response.json(receipt));
    throw new Error(`Unexpected request ${url}`);
  });
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSessionReader {...props} />);
  expect(await screen.findByText("Sesión cerrada")).toBeVisible();
  expect(screen.getByText(closure.progressNote)).toBeVisible();
  expect(screen.getByText(closure.nextStep)).toBeVisible();
  expect(screen.getByText("Tiempo de trabajo total: 1,000001 s")).toBeVisible();
  expect(screen.getByText("Día atribuido: 2026-09-07")).toBeVisible();
  expect(fetcher).toHaveBeenCalledTimes(2);
});
it("@s33 rejects a session snapshot belonging to another task route", async () => {
  const fetcher = vi.fn().mockResolvedValue(closedResponse());
  vi.stubGlobal("fetch", fetcher);
  render(
    <WorkSessionReader
      {...props}
      taskId="62345678-1234-1234-1234-123456789abc"
    />,
  );
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "Esta sesión no está disponible en esta tarea.",
  );
  expect(screen.queryByText(closure.progressNote)).not.toBeInTheDocument();
  expect(fetcher).toHaveBeenCalledTimes(1);
});
it("@s27 retries a failed closure lookup without claiming a missing closure", async () => {
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(closedResponse())
    .mockResolvedValueOnce(new Response(null, { status: 503 }))
    .mockResolvedValueOnce(closedResponse())
    .mockResolvedValueOnce(Response.json(receipt));
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSessionReader {...props} />);
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "No se ha podido consultar la sesión de trabajo.",
  );
  expect(screen.queryByText("Sesión cerrada")).not.toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", { name: "Reintentar lectura" }));
  expect(await screen.findByText(closure.progressNote)).toBeVisible();
  expect(fetcher).toHaveBeenCalledTimes(4);
});
it("@s33 opens the stable session URL through App after reload", async () => {
  const previous = window.location.pathname;
  window.history.replaceState(
    null,
    "",
    `/proyectos/${props.projectId}/tareas/${props.taskId}/sesiones/${props.id}`,
  );
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValueOnce(closedResponse())
      .mockResolvedValueOnce(Response.json(receipt)),
  );
  try {
    render(<App />);
    expect(await screen.findByText(closure.nextStep)).toBeVisible();
    expect(
      screen.getByRole("link", { name: "Volver a la tarea" }),
    ).toHaveAttribute(
      "href",
      `/proyectos/${props.projectId}/tareas/${props.taskId}`,
    );
  } finally {
    window.history.replaceState(null, "", previous);
  }
});
it("@s32 closes explicitly from the stable URL with both optional notes", async () => {
  const openResponse = Response.json(
    { state: before, serverNow: before.changedAt, netMicroseconds: "0" },
    { headers: { "Work-Session-Revision": `work-session-${session.id}-1` } },
  );
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(openResponse)
    .mockResolvedValueOnce(
      Response.json(receipt, {
        status: 201,
        headers: { Location: `/api/v1/work-session-changes/${receipt.id}` },
      }),
    );
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSessionReader {...props} />);
  fireEvent.change(await screen.findByLabelText("Avance anotado (opcional)"), {
    target: { value: closure.progressNote },
  });
  fireEvent.change(screen.getByLabelText("Siguiente paso (opcional)"), {
    target: { value: closure.nextStep },
  });
  expect(fetcher).toHaveBeenCalledTimes(1);
  fireEvent.click(screen.getByRole("button", { name: "Confirmar cierre" }));
  expect(await screen.findByText("Sesión cerrada")).toBeVisible();
  expect(fetcher.mock.calls[1][0]).toBe(
    `/api/v1/work-sessions/${session.id}/close`,
  );
  expect(JSON.parse(fetcher.mock.calls[1][1].body)).toEqual({
    progressNote: closure.progressNote,
    nextStep: closure.nextStep,
  });
});
it("@s40 announces a pending close and prevents duplicate submission while preserving focus", async () => {
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(
      Response.json(
        { state: before, serverNow: before.changedAt, netMicroseconds: "0" },
        {
          headers: { "Work-Session-Revision": `work-session-${session.id}-1` },
        },
      ),
    )
    .mockImplementation(() => new Promise(() => {}));
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSessionReader {...props} />);
  const input = await screen.findByLabelText("Avance anotado (opcional)");
  const submit = screen.getByRole("button", { name: "Confirmar cierre" });
  submit.focus();
  fireEvent.click(submit);
  fireEvent.submit(submit.closest("form")!);
  expect(screen.getByRole("status")).toHaveTextContent(
    "Cerrando sesión de trabajo",
  );
  expect(submit).toHaveFocus();
  expect(submit).toHaveAttribute("aria-disabled", "true");
  expect(input).toHaveAttribute("readonly");
  expect(fetcher).toHaveBeenCalledTimes(2);
});
it("@s5 preserves an overlong draft and rejects it before transmitting", async () => {
  const fetcher = vi.fn().mockResolvedValueOnce(
    Response.json(
      { state: before, serverNow: before.changedAt, netMicroseconds: "0" },
      {
        headers: { "Work-Session-Revision": `work-session-${session.id}-1` },
      },
    ),
  );
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSessionReader {...props} />);
  const input = await screen.findByLabelText("Siguiente paso (opcional)");
  const text = "😀".repeat(2001);
  fireEvent.change(input, { target: { value: text } });
  fireEvent.click(screen.getByRole("button", { name: "Confirmar cierre" }));
  expect(screen.getByRole("alert")).toHaveTextContent(
    "Cada nota admite hasta 2.000 caracteres válidos.",
  );
  expect(input).toHaveValue(text);
  expect(input).toHaveAttribute("aria-invalid", "true");
  expect(fetcher).toHaveBeenCalledTimes(1);
});
it("@s35 checks an uncertain close by its retained key without another POST", async () => {
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(
      Response.json(
        { state: before, serverNow: before.changedAt, netMicroseconds: "0" },
        {
          headers: { "Work-Session-Revision": `work-session-${session.id}-1` },
        },
      ),
    )
    .mockResolvedValueOnce(new Response(null, { status: 503 }))
    .mockResolvedValueOnce(Response.json(receipt));
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSessionReader {...props} />);
  fireEvent.change(await screen.findByLabelText("Avance anotado (opcional)"), {
    target: { value: closure.progressNote },
  });
  fireEvent.change(screen.getByLabelText("Siguiente paso (opcional)"), {
    target: { value: closure.nextStep },
  });
  fireEvent.click(screen.getByRole("button", { name: "Confirmar cierre" }));
  const check = await screen.findByRole("button", { name: "Comprobar cierre" });
  expect(screen.getByRole("alert")).toHaveTextContent(
    "No podemos confirmar el cierre.",
  );
  expect(
    screen.queryByRole("button", { name: "Confirmar cierre" }),
  ).not.toBeInTheDocument();
  const key = fetcher.mock.calls[1][1].headers["Idempotency-Key"];
  fireEvent.click(check);
  expect(await screen.findByText("Sesión cerrada")).toBeVisible();
  expect(fetcher.mock.calls[2][0]).toBe(
    `/api/v1/work-session-changes/by-request/${key}`,
  );
  expect(
    fetcher.mock.calls.filter(([, init]) => init.method === "POST"),
  ).toHaveLength(1);
});
it("@s35 permits only deliberate identical resend after a recognized missing change", async () => {
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(
      Response.json(
        { state: before, serverNow: before.changedAt, netMicroseconds: "0" },
        {
          headers: { "Work-Session-Revision": `work-session-${session.id}-1` },
        },
      ),
    )
    .mockResolvedValueOnce(new Response(null, { status: 503 }))
    .mockResolvedValueOnce(
      Response.json(
        {
          type: "urn:organization:problem:work_session_change_not_found",
          title: "No se ha encontrado el cambio de sesión.",
          status: 404,
          code: "WORK_SESSION_CHANGE_NOT_FOUND",
        },
        { status: 404 },
      ),
    )
    .mockResolvedValueOnce(
      Response.json(receipt, {
        status: 201,
        headers: { Location: `/api/v1/work-session-changes/${receipt.id}` },
      }),
    );
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSessionReader {...props} />);
  fireEvent.change(await screen.findByLabelText("Avance anotado (opcional)"), {
    target: { value: closure.progressNote },
  });
  fireEvent.change(screen.getByLabelText("Siguiente paso (opcional)"), {
    target: { value: closure.nextStep },
  });
  fireEvent.click(screen.getByRole("button", { name: "Confirmar cierre" }));
  fireEvent.click(
    await screen.findByRole("button", { name: "Comprobar cierre" }),
  );
  const resend = await screen.findByRole("button", {
    name: "Reenviar el mismo cierre",
  });
  expect(fetcher).toHaveBeenCalledTimes(3);
  fireEvent.click(resend);
  expect(await screen.findByText("Sesión cerrada")).toBeVisible();
  expect(fetcher.mock.calls[3][1].body).toBe(fetcher.mock.calls[1][1].body);
  expect(fetcher.mock.calls[3][1].headers).toEqual(
    fetcher.mock.calls[1][1].headers,
  );
});
it("@s36 retains the draft after 412 and uses a new revision and key only after a new decision", async () => {
  const paused = {
    ...before,
    status: "paused",
    revision: "2",
    runningSince: null,
  };
  const result = {
    ...receipt,
    before: paused,
    after: { ...closed, revision: "3", workedMicroseconds: "0" },
  };
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(
      Response.json(
        { state: before, serverNow: before.changedAt, netMicroseconds: "0" },
        {
          headers: { "Work-Session-Revision": `work-session-${session.id}-1` },
        },
      ),
    )
    .mockResolvedValueOnce(
      Response.json(
        {
          type: "urn:organization:problem:precondition_failed",
          title: "La revisión ha cambiado.",
          status: 412,
          code: "PRECONDITION_FAILED",
        },
        { status: 412 },
      ),
    )
    .mockResolvedValueOnce(
      Response.json(
        { state: paused, serverNow: before.changedAt, netMicroseconds: "0" },
        {
          headers: { "Work-Session-Revision": `work-session-${session.id}-2` },
        },
      ),
    )
    .mockResolvedValueOnce(
      Response.json(result, {
        status: 201,
        headers: { Location: `/api/v1/work-session-changes/${receipt.id}` },
      }),
    );
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSessionReader {...props} />);
  fireEvent.change(await screen.findByLabelText("Avance anotado (opcional)"), {
    target: { value: closure.progressNote },
  });
  fireEvent.change(screen.getByLabelText("Siguiente paso (opcional)"), {
    target: { value: closure.nextStep },
  });
  fireEvent.click(screen.getByRole("button", { name: "Confirmar cierre" }));
  const consult = await screen.findByRole("button", {
    name: "Consultar estado actual",
  });
  expect(fetcher).toHaveBeenCalledTimes(2);
  fireEvent.click(consult);
  expect(await screen.findByLabelText("Avance anotado (opcional)")).toHaveValue(
    closure.progressNote,
  );
  expect(screen.getByLabelText("Siguiente paso (opcional)")).toHaveValue(
    closure.nextStep,
  );
  expect(fetcher).toHaveBeenCalledTimes(3);
  fireEvent.click(screen.getByRole("button", { name: "Confirmar cierre" }));
  expect(await screen.findByText("Sesión cerrada")).toBeVisible();
  expect(fetcher.mock.calls[3][1].headers["Idempotency-Key"]).not.toBe(
    fetcher.mock.calls[1][1].headers["Idempotency-Key"],
  );
  expect(fetcher.mock.calls[3][1].headers["Work-Session-Revision"]).toBe(
    `work-session-${session.id}-2`,
  );
});
it("@s37 renews CSRF separately and resends only the same retained closure manually", async () => {
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(
      Response.json(
        { state: before, serverNow: before.changedAt, netMicroseconds: "0" },
        {
          headers: { "Work-Session-Revision": `work-session-${session.id}-1` },
        },
      ),
    )
    .mockResolvedValueOnce(
      Response.json(
        {
          type: "urn:organization:problem:csrf_invalid",
          title: "Recupera el acceso.",
          status: 403,
          code: "CSRF_INVALID",
        },
        { status: 403 },
      ),
    )
    .mockImplementation(() => new Promise(() => {}));
  vi.stubGlobal("fetch", fetcher);
  setCsrfToken("before");
  render(<WorkSessionReader {...props} />);
  fireEvent.change(await screen.findByLabelText("Avance anotado (opcional)"), {
    target: { value: " Mi avance " },
  });
  fireEvent.click(screen.getByRole("button", { name: "Confirmar cierre" }));
  const resend = await screen.findByRole("button", {
    name: "Reenviar el mismo cierre",
  });
  setCsrfToken("renewed");
  expect(fetcher).toHaveBeenCalledTimes(2);
  fireEvent.click(resend);
  expect(fetcher).toHaveBeenCalledTimes(3);
  expect(fetcher.mock.calls[2][1].body).toBe(fetcher.mock.calls[1][1].body);
  const previous = new Headers(fetcher.mock.calls[1][1].headers);
  const current = new Headers(fetcher.mock.calls[2][1].headers);
  expect(current.get("Idempotency-Key")).toBe(previous.get("Idempotency-Key"));
  expect(current.get("Work-Session-Revision")).toBe(
    previous.get("Work-Session-Revision"),
  );
  expect(current.get("X-CSRF-TOKEN")).toBe("renewed");
});
it("@s38 aborts a transmitted closure when its private reader unmounts", async () => {
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(
      Response.json(
        { state: before, serverNow: before.changedAt, netMicroseconds: "0" },
        {
          headers: { "Work-Session-Revision": `work-session-${session.id}-1` },
        },
      ),
    )
    .mockImplementation(() => new Promise(() => {}));
  vi.stubGlobal("fetch", fetcher);
  const view = render(<WorkSessionReader {...props} />);
  await screen.findByLabelText("Avance anotado (opcional)");
  fireEvent.click(screen.getByRole("button", { name: "Confirmar cierre" }));
  const signal = fetcher.mock.calls[1][1].signal;
  view.unmount();
  expect(signal.aborted).toBe(true);
});
it("@s38 replaces private drafts on route change and ignores a late command response", async () => {
  let resolve!: (value: Response) => void;
  const pending = new Promise<Response>((done) => {
    resolve = done;
  });
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(
      Response.json(
        { state: before, serverNow: before.changedAt, netMicroseconds: "0" },
        {
          headers: { "Work-Session-Revision": `work-session-${session.id}-1` },
        },
      ),
    )
    .mockReturnValueOnce(pending)
    .mockImplementation(() => new Promise(() => {}));
  vi.stubGlobal("fetch", fetcher);
  const view = render(<WorkSessionReader {...props} />);
  fireEvent.change(await screen.findByLabelText("Avance anotado (opcional)"), {
    target: { value: "Borrador privado" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Confirmar cierre" }));
  const signal = fetcher.mock.calls[1][1].signal;
  view.rerender(
    <WorkSessionReader {...props} id="62345678-1234-1234-1234-123456789abc" />,
  );
  expect(
    screen.queryByDisplayValue("Borrador privado"),
  ).not.toBeInTheDocument();
  expect(signal.aborted).toBe(true);
  await act(async () => {
    resolve(
      Response.json(receipt, {
        status: 201,
        headers: { Location: `/api/v1/work-session-changes/${receipt.id}` },
      }),
    );
    await pending;
  });
  expect(screen.queryByText("Sesión cerrada")).not.toBeInTheDocument();
  expect(fetcher).toHaveBeenCalledTimes(3);
});
it("@s40 retrying a failed read announces loading and gives disappearing initiator a focus destination", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValueOnce(new Response(null, { status: 503 }))
      .mockImplementation(() => new Promise(() => {})),
  );
  render(<WorkSessionReader {...props} />);
  const retry = await screen.findByRole("button", {
    name: "Reintentar lectura",
  });
  retry.focus();
  fireEvent.click(retry);
  expect(screen.getByRole("status")).toHaveTextContent(
    "Consultando sesión de trabajo",
  );
  expect(
    screen.getByRole("heading", { name: "Sesión de trabajo" }),
  ).toHaveFocus();
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
});

it("@s40 distinguishes a pending recovery read from transmitting a closure", async () => {
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(
      Response.json(
        { state: before, serverNow: before.changedAt, netMicroseconds: "0" },
        {
          headers: { "Work-Session-Revision": `work-session-${session.id}-1` },
        },
      ),
    )
    .mockResolvedValueOnce(new Response(null, { status: 503 }))
    .mockImplementation(() => new Promise(() => {}));
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSessionReader {...props} />);
  await screen.findByLabelText("Avance anotado (opcional)");
  fireEvent.click(screen.getByRole("button", { name: "Confirmar cierre" }));
  const check = await screen.findByRole("button", { name: "Comprobar cierre" });
  check.focus();
  fireEvent.click(check);
  expect(screen.getByRole("status")).toHaveTextContent("Comprobando cierre");
  expect(check).toHaveFocus();
  expect(fetcher).toHaveBeenCalledTimes(3);
});

it("@s32 presents immutable closure date and explicit empty notes without implying task completion", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValueOnce(closedResponse())
      .mockResolvedValueOnce(
        Response.json({
          ...receipt,
          closure: { ...closure, progressNote: "", nextStep: "" },
        }),
      ),
  );
  render(<WorkSessionReader {...props} />);
  await screen.findByText("Sesión cerrada");
  expect(screen.getByText("Sin avance anotado")).toBeVisible();
  expect(screen.getByText("Sin siguiente paso anotado")).toBeVisible();
  expect(
    document.querySelector(`time[datetime="${receipt.occurredAt}"]`),
  ).toBeVisible();
  expect(screen.getByText("UTC")).toBeVisible();
  expect(
    screen.getByText("El cierre de la sesión no completa la tarea."),
  ).toBeVisible();
});

it("@s34 preserves confirmed notes when the separate active lookup fails and retries that lookup only", async () => {
  const fetcher = vi.fn().mockImplementation((url: string) => {
    if (url.endsWith("/state"))
      return Promise.resolve(
        Response.json(
          { state: before, serverNow: before.changedAt, netMicroseconds: "0" },
          {
            headers: {
              "Work-Session-Revision": `work-session-${session.id}-1`,
            },
          },
        ),
      );
    if (url.endsWith("/close"))
      return Promise.resolve(
        Response.json(receipt, {
          status: 201,
          headers: { Location: `/api/v1/work-session-changes/${receipt.id}` },
        }),
      );
    if (url.endsWith("/active"))
      return Promise.resolve(new Response(null, { status: 503 }));
    throw new Error(`Unexpected ${url}`);
  });
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSessionReader {...props} />);
  fireEvent.change(await screen.findByLabelText("Avance anotado (opcional)"), {
    target: { value: closure.progressNote },
  });
  fireEvent.change(screen.getByLabelText("Siguiente paso (opcional)"), {
    target: { value: closure.nextStep },
  });
  fireEvent.click(screen.getByRole("button", { name: "Confirmar cierre" }));
  expect(
    await screen.findByText(
      "No se ha podido consultar si hay otra sesión abierta.",
    ),
  ).toBeVisible();
  expect(screen.getByText(closure.progressNote)).toBeVisible();
  fetcher.mockImplementation((url: string) => {
    expect(url).toBe("/api/v1/work-sessions/active");
    return Promise.resolve(Response.json({ session: null }));
  });
  fireEvent.click(
    screen.getByRole("button", { name: "Consultar sesión abierta" }),
  );
  expect(
    await screen.findByText("No hay ninguna sesión abierta."),
  ).toBeVisible();
  expect(screen.getByText(closure.progressNote)).toBeVisible();
  expect(fetcher).toHaveBeenCalledTimes(4);
});

it("@s34 displays a different legitimate active session separately from the closed receipt", async () => {
  const other = {
    ...session,
    id: "62345678-1234-1234-1234-123456789abc",
    taskId: "72345678-1234-1234-1234-123456789abc",
  };
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation((url: string) => {
      if (url.endsWith("/state"))
        return Promise.resolve(
          Response.json(
            {
              state: before,
              serverNow: before.changedAt,
              netMicroseconds: "0",
            },
            {
              headers: {
                "Work-Session-Revision": `work-session-${session.id}-1`,
              },
            },
          ),
        );
      if (url.endsWith("/close"))
        return Promise.resolve(
          Response.json(receipt, {
            status: 201,
            headers: { Location: `/api/v1/work-session-changes/${receipt.id}` },
          }),
        );
      if (url.endsWith("/active"))
        return Promise.resolve(Response.json({ session: other }));
      throw new Error(`Unexpected ${url}`);
    }),
  );
  render(<WorkSessionReader {...props} />);
  fireEvent.change(await screen.findByLabelText("Avance anotado (opcional)"), {
    target: { value: closure.progressNote },
  });
  fireEvent.change(screen.getByLabelText("Siguiente paso (opcional)"), {
    target: { value: closure.nextStep },
  });
  fireEvent.click(screen.getByRole("button", { name: "Confirmar cierre" }));
  expect(
    await screen.findByRole("link", { name: "Ir a la sesión abierta" }),
  ).toHaveAttribute(
    "href",
    `/proyectos/${other.projectId}/tareas/${other.taskId}/sesiones/${other.id}`,
  );
  expect(screen.getByText(closure.progressNote)).toBeVisible();
  expect(
    screen.queryByText("No hay ninguna sesión abierta."),
  ).not.toBeInTheDocument();
});

it("@s33 rejects a closure whose immutable task context differs from the validated state", async () => {
  const otherSession = {
    ...session,
    taskId: "72345678-1234-1234-1234-123456789abc",
  };
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValueOnce(closedResponse())
      .mockResolvedValueOnce(
        Response.json({
          ...receipt,
          before: { ...before, session: otherSession },
          after: { ...closed, session: otherSession },
        }),
      ),
  );
  render(<WorkSessionReader {...props} />);
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "Esta sesión no está disponible en esta tarea.",
  );
  expect(screen.queryByText(closure.progressNote)).not.toBeInTheDocument();
  expect(screen.queryByText("Sesión cerrada")).not.toBeInTheDocument();
});

it("@s32 the close form identifies its session times and explains that leaving does not revoke a sent close", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValueOnce(
        Response.json(
          { state: before, serverNow: before.changedAt, netMicroseconds: "0" },
          {
            headers: {
              "Work-Session-Revision": `work-session-${session.id}-1`,
            },
          },
        ),
      )
      .mockImplementation(() => new Promise(() => {})),
  );
  render(<WorkSessionReader {...props} />);
  await screen.findByLabelText("Avance anotado (opcional)");
  expect(
    document.querySelector(`time[datetime="${session.startedAt}"]`),
  ).toBeVisible();
  expect(
    document.querySelector(`time[datetime="${session.plannedEndAt}"]`),
  ).toBeVisible();
  fireEvent.click(screen.getByRole("button", { name: "Confirmar cierre" }));
  expect(
    screen.getByText(
      "Salir no revoca el cierre transmitido. Al volver puedes consultar esta sesión.",
    ),
  ).toBeVisible();
});

it("@s36 a definitive state conflict consults the closure without resubmitting the retained draft", async () => {
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(
      Response.json(
        { state: before, serverNow: before.changedAt, netMicroseconds: "0" },
        {
          headers: { "Work-Session-Revision": `work-session-${session.id}-1` },
        },
      ),
    )
    .mockResolvedValueOnce(
      Response.json(
        {
          type: "urn:organization:problem:work_session_state_conflict",
          title: "Ya cerrada",
          status: 409,
          code: "WORK_SESSION_STATE_CONFLICT",
        },
        { status: 409 },
      ),
    )
    .mockResolvedValueOnce(closedResponse())
    .mockResolvedValueOnce(Response.json(receipt));
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSessionReader {...props} />);
  fireEvent.change(await screen.findByLabelText("Avance anotado (opcional)"), {
    target: { value: "Mi intención sin confirmar" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Confirmar cierre" }));
  fireEvent.click(
    await screen.findByRole("button", { name: "Consultar estado actual" }),
  );
  expect(await screen.findByText(closure.progressNote)).toBeVisible();
  expect(
    screen.queryByText("Mi intención sin confirmar"),
  ).not.toBeInTheDocument();
  expect(
    screen.queryByRole("button", { name: "Reenviar el mismo cierre" }),
  ).not.toBeInTheDocument();
  expect(
    fetcher.mock.calls.filter(([, options]) => options.method === "POST"),
  ).toHaveLength(1);
});

it("@s14 a definitive revision limit reports the rejection without treating the close as uncertain", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValueOnce(
        Response.json(
          { state: before, serverNow: before.changedAt, netMicroseconds: "0" },
          {
            headers: {
              "Work-Session-Revision": `work-session-${session.id}-1`,
            },
          },
        ),
      )
      .mockResolvedValueOnce(
        Response.json(
          {
            type: "urn:organization:problem:work_session_revision_exhausted",
            title: "Límite",
            status: 409,
            code: "WORK_SESSION_REVISION_EXHAUSTED",
          },
          { status: 409 },
        ),
      ),
  );
  render(<WorkSessionReader {...props} />);
  await screen.findByLabelText("Avance anotado (opcional)");
  fireEvent.click(screen.getByRole("button", { name: "Confirmar cierre" }));
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "No se pueden registrar más cambios en esta sesión.",
  );
  expect(
    screen.queryByRole("button", { name: "Comprobar cierre" }),
  ).not.toBeInTheDocument();
});

it("@s35 a failed key lookup preserves uncertainty and cannot enable resend", async () => {
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(
      Response.json(
        { state: before, serverNow: before.changedAt, netMicroseconds: "0" },
        {
          headers: { "Work-Session-Revision": `work-session-${session.id}-1` },
        },
      ),
    )
    .mockResolvedValueOnce(new Response(null, { status: 503 }))
    .mockResolvedValueOnce(
      Response.json({ code: "UNKNOWN", status: 503 }, { status: 503 }),
    );
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSessionReader {...props} />);
  await screen.findByLabelText("Avance anotado (opcional)");
  fireEvent.click(screen.getByRole("button", { name: "Confirmar cierre" }));
  fireEvent.click(
    await screen.findByRole("button", { name: "Comprobar cierre" }),
  );
  await act(async () => {});
  expect(screen.getByRole("alert")).toHaveTextContent(
    "No podemos confirmar el cierre.",
  );
  expect(
    screen.getByRole("button", { name: "Comprobar cierre" }),
  ).toHaveAttribute("aria-disabled", "false");
  expect(
    screen.queryByRole("button", { name: "Reenviar el mismo cierre" }),
  ).not.toBeInTheDocument();
  expect(
    fetcher.mock.calls.filter(([, options]) => options.method === "POST"),
  ).toHaveLength(1);
});

it("@s38 an old HTTP401 from a retired reader cannot revoke current access", async () => {
  let finish!: (value: Response) => void;
  const pending = new Promise<Response>((resolve) => {
    finish = resolve;
  });
  const fetcher = vi
    .fn()
    .mockReturnValueOnce(pending)
    .mockImplementation(() => new Promise(() => {}));
  vi.stubGlobal("fetch", fetcher);
  const access = vi.fn();
  observeAccess(access);
  const view = render(<WorkSessionReader {...props} />);
  view.rerender(
    <WorkSessionReader {...props} id="62345678-1234-1234-1234-123456789abc" />,
  );
  await act(async () => {
    finish(new Response(null, { status: 401 }));
    await pending;
  });
  expect(access).not.toHaveBeenCalled();
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
});

it("@s40 a deliberate blur before the close response does not move focus back to the reader", async () => {
  let finish!: (value: Response) => void;
  const pending = new Promise<Response>((resolve) => {
    finish = resolve;
  });
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValueOnce(
        Response.json(
          { state: before, serverNow: before.changedAt, netMicroseconds: "0" },
          {
            headers: {
              "Work-Session-Revision": `work-session-${session.id}-1`,
            },
          },
        ),
      )
      .mockReturnValueOnce(pending)
      .mockImplementation(() => new Promise(() => {})),
  );
  render(<WorkSessionReader {...props} />);
  await screen.findByLabelText("Avance anotado (opcional)");
  const button = screen.getByRole("button", { name: "Confirmar cierre" });
  button.focus();
  fireEvent.click(button);
  button.blur();
  await act(async () => {
    finish(
      Response.json(
        { ...receipt, closure: { ...closure, progressNote: "", nextStep: "" } },
        {
          status: 201,
          headers: { Location: `/api/v1/work-session-changes/${receipt.id}` },
        },
      ),
    );
    await pending;
  });
  expect(screen.getByText("Sesión cerrada")).toBeVisible();
  expect(document.body).toHaveFocus();
});

it("@s32 explains the irreversible notes and unchanged task before confirmation", async () => {
  const fetcher = vi.fn().mockResolvedValue(
    Response.json(
      { state: before, serverNow: before.changedAt, netMicroseconds: "0" },
      {
        headers: { "Work-Session-Revision": `work-session-${session.id}-1` },
      },
    ),
  );
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSessionReader {...props} />);
  await screen.findByLabelText("Avance anotado (opcional)");
  expect(
    screen.getByRole("heading", { name: "Cerrar sesión de trabajo" }),
  ).toBeVisible();
  expect(
    screen.getByText(
      "Las notas quedarán guardadas y no se podrán editar después del cierre. La tarea seguirá en su estado actual.",
    ),
  ).toBeVisible();
  expect(fetcher).toHaveBeenCalledTimes(1);
});

it("@s38 discards closure JSON decoded after navigating to another session", async () => {
  let finish!: (value: unknown) => void;
  const pending = new Promise((resolve) => {
    finish = resolve;
  });
  const delayed = Response.json(receipt);
  vi.spyOn(delayed, "json").mockReturnValue(pending);
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValueOnce(closedResponse())
      .mockResolvedValueOnce(delayed)
      .mockImplementation(() => new Promise(() => {})),
  );
  const view = render(<WorkSessionReader {...props} />);
  await act(async () => {});
  expect(delayed.json).toHaveBeenCalledOnce();
  view.rerender(
    <WorkSessionReader {...props} id="62345678-1234-1234-1234-123456789abc" />,
  );
  await act(async () => {
    finish(receipt);
    await pending;
  });
  expect(screen.queryByText(closure.progressNote)).not.toBeInTheDocument();
  expect(screen.queryByText("Sesión cerrada")).not.toBeInTheDocument();
});

it("@s38 a late problem classification cannot restore the retired close draft", async () => {
  let finish!: (value: unknown) => void;
  const pending = new Promise((resolve) => {
    finish = resolve;
  });
  const failure = new Response(null, { status: 503 });
  const copy = new Response(null, { status: 503 });
  vi.spyOn(copy, "json").mockReturnValue(pending);
  vi.spyOn(failure, "clone").mockReturnValue(copy);
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValueOnce(
        Response.json(
          { state: before, serverNow: before.changedAt, netMicroseconds: "0" },
          {
            headers: {
              "Work-Session-Revision": `work-session-${session.id}-1`,
            },
          },
        ),
      )
      .mockResolvedValueOnce(failure)
      .mockImplementation(() => new Promise(() => {})),
  );
  const view = render(<WorkSessionReader {...props} />);
  await screen.findByLabelText("Avance anotado (opcional)");
  fireEvent.click(screen.getByRole("button", { name: "Confirmar cierre" }));
  await act(async () => {});
  expect(copy.json).toHaveBeenCalled();
  view.rerender(
    <WorkSessionReader {...props} id="62345678-1234-1234-1234-123456789abc" />,
  );
  await act(async () => {
    finish({ code: "UNKNOWN", status: 503 });
    await pending;
  });
  expect(
    screen.queryByRole("button", { name: "Comprobar cierre" }),
  ).not.toBeInTheDocument();
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
});

it("@s40 a disappearing focused confirm button transfers focus to the session heading", async () => {
  let finish!: (value: Response) => void;
  const pending = new Promise<Response>((resolve) => {
    finish = resolve;
  });
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValueOnce(
        Response.json(
          { state: before, serverNow: before.changedAt, netMicroseconds: "0" },
          {
            headers: {
              "Work-Session-Revision": `work-session-${session.id}-1`,
            },
          },
        ),
      )
      .mockReturnValueOnce(pending)
      .mockImplementation(() => new Promise(() => {})),
  );
  render(<WorkSessionReader {...props} />);
  await screen.findByLabelText("Avance anotado (opcional)");
  const button = screen.getByRole("button", { name: "Confirmar cierre" });
  button.focus();
  fireEvent.click(button);
  await act(async () => {
    finish(
      Response.json(
        { ...receipt, closure: { ...closure, progressNote: "", nextStep: "" } },
        {
          status: 201,
          headers: { Location: `/api/v1/work-session-changes/${receipt.id}` },
        },
      ),
    );
    await pending;
  });
  expect(
    screen.getByRole("heading", { name: "Sesión de trabajo" }),
  ).toHaveFocus();
});
