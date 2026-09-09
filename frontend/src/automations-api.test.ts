import { afterEach, describe, expect, it, vi } from "vitest";
import {
  AutomationConflict,
  AutomationFieldErrors,
  createAutomation,
  deleteAutomation,
  readAutomationRuns,
  readAutomations,
  replaceAutomation,
  simulateAutomation,
} from "./automations-api";
import type { Automation, AutomationDraft } from "./automations-api";

const RULE = "22222222-2222-4222-8222-222222222222";
const PROJECT = "11111111-1111-4111-8111-111111111111";

const rule: Automation = {
  id: RULE,
  name: "Seguimiento",
  enabled: true,
  trigger: { eventType: "TaskCreated.v1" },
  condition: null,
  action: {
    type: "CREATE_TASK",
    projectId: PROJECT,
    titleTemplate: "Revisar {{task.title}}",
    criterionTemplate: null,
    estimatedMinutes: 30,
  },
  version: 1,
  createdAt: "2026-09-08T10:15:30.123456Z",
  updatedAt: "2026-09-08T10:15:30.123456Z",
};

const draft: AutomationDraft = {
  name: "Seguimiento",
  enabled: true,
  trigger: { eventType: "TaskCreated.v1" },
  condition: null,
  action: rule.action,
};

function answer(status: number, body: unknown, headers: HeadersInit = {}) {
  return new Response(body === undefined ? null : JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json", ...headers },
  });
}

function stub(...responses: Response[]) {
  const fetcher = vi.fn();
  for (const response of responses) fetcher.mockResolvedValueOnce(response);
  vi.stubGlobal("fetch", fetcher);
  return fetcher;
}

afterEach(() => vi.unstubAllGlobals());

describe("automations api", () => {
  it("@s37 reads the owner's rules and rejects a body that is not the closed envelope", async () => {
    stub(answer(200, { items: [rule] }));
    const signal = new AbortController().signal;
    await expect(readAutomations(signal)).resolves.toEqual([rule]);

    stub(answer(200, { items: [{ ...rule, extra: 1 }] }));
    await expect(readAutomations(signal)).rejects.toThrow();

    stub(answer(200, { rules: [] }));
    await expect(readAutomations(signal)).rejects.toThrow();
  });

  it("@s37 surfaces a failing read as the response itself", async () => {
    stub(answer(503, { code: "STORAGE_UNAVAILABLE" }));
    await expect(
      readAutomations(new AbortController().signal),
    ).rejects.toBeInstanceOf(Response);
  });

  it("@s38 maps a field error of the server onto its field", async () => {
    stub(
      answer(400, {
        code: "INVALID_TEMPLATE",
        errors: [
          {
            field: "action.criterionTemplate",
            code: "UNKNOWN_PLACEHOLDER",
            message: "Revisa",
          },
        ],
      }),
    );
    await expect(
      createAutomation(draft, new AbortController().signal),
    ).rejects.toBeInstanceOf(AutomationFieldErrors);
    stub(
      answer(400, {
        code: "INVALID_TEMPLATE",
        errors: [
          {
            field: "action.criterionTemplate",
            code: "UNKNOWN_PLACEHOLDER",
            message: "Revisa",
          },
        ],
      }),
    );
    await createAutomation(draft, new AbortController().signal).catch(
      (error: AutomationFieldErrors) => {
        expect(error.fields).toEqual({
          "action.criterionTemplate":
            "Revisa los marcadores de esta plantilla.",
        });
      },
    );
  });

  it("@s38 names the field for every code the contract publishes", async () => {
    const codes = [
      ["UNCLOSED_PLACEHOLDER", "Falta cerrar un marcador con dos llaves."],
      [
        "PLACEHOLDER_NOT_AVAILABLE",
        "Ese marcador no existe para este disparador.",
      ],
      ["TARGET_NOT_FOUND", "Elige un proyecto propio existente."],
      ["ENDPOINT_NOT_FOUND", "Elige un endpoint propio y activo."],
      ["INVALID_VALUE", "Revisa el valor de este campo."],
    ] as const;
    for (const [code, message] of codes) {
      stub(
        answer(422, {
          code: "TARGET_NOT_FOUND",
          errors: [{ field: "name", code, message: "x" }],
        }),
      );
      await createAutomation(draft, new AbortController().signal).catch(
        (error: AutomationFieldErrors) =>
          expect(error.fields).toEqual({ name: message }),
      );
    }
  });

  it("@s40 reports a 412 as a conflict of its own so the draft can be kept", async () => {
    stub(answer(412, { code: "AUTOMATION_CONFLICT" }));
    await expect(
      replaceAutomation(RULE, 2, draft, new AbortController().signal),
    ).rejects.toBeInstanceOf(AutomationConflict);
  });

  it("@s40 sends the version as If-Match and returns the confirmed rule", async () => {
    const fetcher = stub(answer(200, { ...rule, version: 3, enabled: false }));
    const saved = await replaceAutomation(
      RULE,
      2,
      draft,
      new AbortController().signal,
    );
    expect(saved.enabled).toBe(false);
    expect(saved.version).toBe(3);
    const [url, options] = fetcher.mock.calls[0];
    expect(url).toBe(`/api/v1/me/automations/${RULE}`);
    expect(options.method).toBe("PUT");
    expect(new Headers(options.headers).get("If-Match")).toBe('"2"');
  });

  it("@s14 deletes with the version and expects no body", async () => {
    const fetcher = stub(new Response(null, { status: 204 }));
    await expect(
      deleteAutomation(RULE, 2, new AbortController().signal),
    ).resolves.toBeUndefined();
    const [, options] = fetcher.mock.calls[0];
    expect(options.method).toBe("DELETE");
    expect(new Headers(options.headers).get("If-Match")).toBe('"2"');
  });

  it("@s39 reads a simulation with its matches and previews", async () => {
    const match = {
      eventId: "55555555-5555-4555-8555-555555555555",
      eventType: "TaskCreated.v1",
      occurredAt: "2026-09-08T10:15:30.123456Z",
      preview: {
        type: "CREATE_TASK",
        projectId: PROJECT,
        title: "Revisar Redactar informe",
        completionCriterion: "",
        estimatedMinutes: 30,
        wouldFail: "PROJECT_COMPLETED",
      },
      loopGuarded: false,
    };
    const fetcher = stub(answer(200, { evaluatedEvents: 5, matches: [match] }));
    const result = await simulateAutomation(
      draft,
      new AbortController().signal,
    );
    expect(result.evaluatedEvents).toBe(5);
    expect(result.matches).toEqual([match]);
    expect(fetcher.mock.calls[0][0]).toBe("/api/v1/me/automations/simulate");
  });

  it("@s39 refuses a simulation whose envelope is not exactly the two keys", async () => {
    stub(answer(200, { evaluatedEvents: 5 }));
    await expect(
      simulateAutomation(draft, new AbortController().signal),
    ).rejects.toThrow();
  });

  it("@s41 reads a page of runs and its cursor, and asks for the next one", async () => {
    const run = {
      id: "44444444-4444-4444-8444-444444444444",
      eventId: "55555555-5555-4555-8555-555555555555",
      eventType: "TaskCreated.v1",
      occurredAt: "2026-09-08T10:15:30.123456Z",
      attempt: 1,
      status: "succeeded",
      createdTaskId: PROJECT,
      deliveryId: null,
      errorCode: null,
      executedAt: "2026-09-08T10:15:30.123456Z",
    };
    const fetcher = stub(answer(200, { items: [run], nextCursor: "abc" }));
    const page = await readAutomationRuns(
      RULE,
      null,
      new AbortController().signal,
    );
    expect(page).toEqual({ items: [run], nextCursor: "abc" });
    expect(fetcher.mock.calls[0][0]).toBe(
      `/api/v1/me/automations/${RULE}/runs`,
    );

    const next = stub(answer(200, { items: [], nextCursor: null }));
    await readAutomationRuns(RULE, "abc", new AbortController().signal);
    expect(next.mock.calls[0][0]).toBe(
      `/api/v1/me/automations/${RULE}/runs?cursor=abc`,
    );
  });

  it("@s41 rejects a run whose status is not one of the three published", async () => {
    stub(
      answer(200, {
        items: [
          {
            id: RULE,
            eventId: RULE,
            eventType: "TaskCreated.v1",
            occurredAt: "2026-09-08T10:15:30.123456Z",
            attempt: 1,
            status: "unknown",
            createdTaskId: null,
            deliveryId: null,
            errorCode: null,
            executedAt: "2026-09-08T10:15:30.123456Z",
          },
        ],
        nextCursor: null,
      }),
    );
    await expect(
      readAutomationRuns(RULE, null, new AbortController().signal),
    ).rejects.toThrow();
  });

  it("@s43 stops before touching the network when the signal is already aborted", async () => {
    const controller = new AbortController();
    controller.abort();
    const fetcher = stub(answer(200, { items: [] }));
    await expect(readAutomations(controller.signal)).rejects.toThrow();
    expect(fetcher).not.toHaveBeenCalled();
  });
});
