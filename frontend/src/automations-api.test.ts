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

  const CONDITION_PROJECT = "33333333-3333-4333-8333-333333333333";
  const ENDPOINT = "88888888-8888-4888-8888-888888888888";
  const EVENT = "55555555-5555-4555-8555-555555555555";
  const TASK = "99999999-9999-4999-8999-999999999999";
  const RUN = "44444444-4444-4444-8444-444444444444";
  const INSTANT = "2026-09-08T10:15:30.123456Z";
  const INCOMPATIBLE = "Respuesta de automatizaciones incompatible.";

  const webhookAction = { type: "NOTIFY_WEBHOOK", endpointId: ENDPOINT };
  const taskAction = rule.action as Record<string, unknown>;

  const read = async (body: unknown) => {
    stub(answer(200, body));
    return readAutomations(new AbortController().signal);
  };
  const listing = async (item: unknown) => read({ items: [item] });

  it("@s37 refuses a rule that breaks any single part of the closed shape", async () => {
    const broken: [string, unknown][] = [
      ["clave de más", { ...rule, extra: 1 }],
      ["clave de menos", { ...rule, updatedAt: undefined }],
      ["id que no es un UUID", { ...rule, id: "22222222-2222-4222-8222" }],
      ["id con basura por delante", { ...rule, id: `zz${RULE}` }],
      ["id con basura por detrás", { ...rule, id: `${RULE}zz` }],
      ["id que sólo parece un UUID al convertirlo", { ...rule, id: [RULE] }],
      ["nombre que no es texto", { ...rule, name: 5 }],
      ["activación que no es booleana", { ...rule, enabled: "true" }],
      [
        "disparador con clave de más",
        { ...rule, trigger: { eventType: "TaskCreated.v1", extra: 1 } },
      ],
      [
        "disparador fuera de los doce publicados",
        { ...rule, trigger: { eventType: "TaskArchived.v1" } },
      ],
      ["condición que no es ni null ni objeto", { ...rule, condition: 5 }],
      [
        "condición con clave de más",
        { ...rule, condition: { projectId: PROJECT, extra: 1 } },
      ],
      [
        "condición cuyo proyecto no es un UUID",
        { ...rule, condition: { projectId: "cualquiera" } },
      ],
      ["versión no entera", { ...rule, version: 1.5 }],
      ["createdAt que no es un instante", { ...rule, createdAt: "2026-09-08" }],
      ["updatedAt que no es un instante", { ...rule, updatedAt: "ayer" }],
      ["acción con clave de más", { ...rule, action: { ...taskAction, x: 1 } }],
      [
        "acción con forma de CREATE_TASK y otro tipo",
        { ...rule, action: { ...taskAction, type: "NOTIFY_WEBHOOK" } },
      ],
      [
        "destino que no es un UUID",
        { ...rule, action: { ...taskAction, projectId: "ninguno" } },
      ],
      [
        "plantilla de título que no es texto",
        { ...rule, action: { ...taskAction, titleTemplate: 5 } },
      ],
      [
        "criterio que no es ni null ni texto",
        { ...rule, action: { ...taskAction, criterionTemplate: 5 } },
      ],
      [
        "minutos estimados no enteros",
        { ...rule, action: { ...taskAction, estimatedMinutes: 1.5 } },
      ],
      [
        "webhook con clave de más",
        { ...rule, action: { ...webhookAction, extra: 1 } },
      ],
      [
        "acción de un tipo que no existe",
        { ...rule, action: { type: "SEND_EMAIL", endpointId: ENDPOINT } },
      ],
      [
        "webhook cuyo endpoint no es un UUID",
        { ...rule, action: { type: "NOTIFY_WEBHOOK", endpointId: "ninguno" } },
      ],
    ];
    for (const [what, item] of broken)
      await expect(listing(item), what).rejects.toThrow(INCOMPATIBLE);
  });

  it("@s37 accepts every shape that the contract does publish", async () => {
    const valid: [string, unknown][] = [
      ["sin condición y con minutos", rule],
      [
        "condición sobre un proyecto propio",
        { ...rule, condition: { projectId: CONDITION_PROJECT } },
      ],
      [
        "criterio con texto",
        { ...rule, action: { ...taskAction, criterionTemplate: "Enviado" } },
      ],
      [
        "sin minutos estimados",
        { ...rule, action: { ...taskAction, estimatedMinutes: null } },
      ],
      ["acción de aviso al webhook", { ...rule, action: webhookAction }],
      ["regla desactivada", { ...rule, enabled: false }],
    ];
    for (const [what, item] of valid)
      await expect(listing(item), what).resolves.toEqual([item]);
  });

  it("@s37 refuses an envelope that is not exactly { items }", async () => {
    const broken: [string, unknown][] = [
      ["clave de más junto a items", { items: [rule], nextCursor: null }],
      ["sin la clave items", { rules: [rule] }],
      ["items que no es una lista", { items: { 0: rule } }],
      ["una regla válida y otra rota", { items: [rule, { ...rule, name: 5 }] }],
    ];
    for (const [what, body] of broken)
      await expect(read(body), what).rejects.toThrow(INCOMPATIBLE);
  });

  const validPreview = {
    type: "CREATE_TASK",
    projectId: PROJECT,
    title: "Revisar Redactar informe",
    completionCriterion: "",
    estimatedMinutes: 30,
    wouldFail: null,
  };
  const webhookPreview = {
    type: "NOTIFY_WEBHOOK",
    endpointId: ENDPOINT,
    eventId: EVENT,
  };
  const validMatch = {
    eventId: EVENT,
    eventType: "TaskCreated.v1",
    occurredAt: INSTANT,
    preview: validPreview,
    loopGuarded: false,
  };
  const simulated = async (body: unknown) => {
    stub(answer(200, body));
    return simulateAutomation(draft, new AbortController().signal);
  };
  const coincidence = async (item: unknown) =>
    simulated({ evaluatedEvents: 5, matches: [item] });

  it("@s39 refuses a coincidence that breaks any single part of its shape", async () => {
    const broken: [string, unknown][] = [
      ["clave de más", { ...validMatch, extra: 1 }],
      ["eventId que no es un UUID", { ...validMatch, eventId: "1" }],
      ["tipo de evento no publicado", { ...validMatch, eventType: "Otro.v1" }],
      ["occurredAt que no es un instante", { ...validMatch, occurredAt: "ya" }],
      ["loopGuarded que no es booleano", { ...validMatch, loopGuarded: "no" }],
      ["previsualización con clave de más", m({ ...validPreview, extra: 1 })],
      [
        "previsualización con forma de tarea y otro tipo",
        m({ ...validPreview, type: "NOTIFY_WEBHOOK" }),
      ],
      ["proyecto que no es un UUID", m({ ...validPreview, projectId: "x" })],
      ["título que no es texto", m({ ...validPreview, title: 5 })],
      [
        "criterio resuelto que no es texto",
        m({ ...validPreview, completionCriterion: null }),
      ],
      [
        "minutos estimados no enteros",
        m({ ...validPreview, estimatedMinutes: 1.5 }),
      ],
      ["motivo de fallo que no es texto", m({ ...validPreview, wouldFail: 7 })],
      ["aviso con clave de más", m({ ...webhookPreview, extra: 1 })],
      [
        "previsualización de un tipo que no existe",
        m({ ...webhookPreview, type: "SEND_EMAIL" }),
      ],
      ["aviso sin endpoint válido", m({ ...webhookPreview, endpointId: "x" })],
      ["aviso sin evento válido", m({ ...webhookPreview, eventId: "x" })],
    ];
    for (const [what, item] of broken)
      await expect(coincidence(item), what).rejects.toThrow(INCOMPATIBLE);
  });

  it("@s39 accepts the coincidences the contract publishes", async () => {
    const valid: [string, unknown][] = [
      ["tarea que fallaría", m({ ...validPreview, wouldFail: "TITLE_TOO_LONG" })],
      ["tarea sin minutos", m({ ...validPreview, estimatedMinutes: null })],
      ["aviso al webhook", m(webhookPreview)],
      ["evento frenado por el guardián de bucles", { ...validMatch, loopGuarded: true }],
    ];
    for (const [what, item] of valid)
      await expect(coincidence(item), what).resolves.toEqual({
        evaluatedEvents: 5,
        matches: [item],
      });
  });

  it("@s39 refuses a simulation whose envelope or list is not the published one", async () => {
    const broken: [string, unknown][] = [
      ["clave de más", { evaluatedEvents: 5, matches: [], extra: 1 }],
      ["sin la clave matches", { evaluatedEvents: 5 }],
      ["eventos evaluados no enteros", { evaluatedEvents: 1.5, matches: [] }],
      ["matches que no es una lista", { evaluatedEvents: 5, matches: {} }],
      [
        "una coincidencia válida y otra rota",
        { evaluatedEvents: 5, matches: [validMatch, { ...validMatch, x: 1 }] },
      ],
    ];
    for (const [what, body] of broken)
      await expect(simulated(body), what).rejects.toThrow(INCOMPATIBLE);
  });

  const validRun = {
    id: RUN,
    eventId: EVENT,
    eventType: "TaskCreated.v1",
    occurredAt: INSTANT,
    attempt: 1,
    status: "succeeded",
    createdTaskId: TASK,
    deliveryId: null,
    errorCode: null,
    executedAt: INSTANT,
  };
  const paged = async (body: unknown) => {
    stub(answer(200, body));
    return readAutomationRuns(RULE, null, new AbortController().signal);
  };
  const execution = async (item: unknown) =>
    paged({ items: [item], nextCursor: null });

  it("@s41 refuses an execution that breaks any single part of its shape", async () => {
    const broken: [string, unknown][] = [
      ["clave de más", { ...validRun, extra: 1 }],
      ["id que no es un UUID", { ...validRun, id: "1" }],
      ["eventId que no es un UUID", { ...validRun, eventId: "1" }],
      ["tipo de evento no publicado", { ...validRun, eventType: "Otro.v1" }],
      ["occurredAt que no es un instante", { ...validRun, occurredAt: "ya" }],
      ["intento no entero", { ...validRun, attempt: 1.5 }],
      ["tarea creada que no es un UUID", { ...validRun, createdTaskId: "1" }],
      ["entrega que no es un UUID", { ...validRun, deliveryId: "1" }],
      ["código de error que no es texto", { ...validRun, errorCode: 7 }],
      ["executedAt que no es un instante", { ...validRun, executedAt: "ya" }],
    ];
    for (const [what, item] of broken)
      await expect(execution(item), what).rejects.toThrow(INCOMPATIBLE);
  });

  it("@s41 accepts the three states and the nulls where they apply", async () => {
    const valid: [string, unknown][] = [
      ["reintento con entrega y sin tarea",
        { ...validRun, status: "retry", createdTaskId: null, deliveryId: ENDPOINT }],
      ["fallo con código de error",
        { ...validRun, status: "failed", createdTaskId: null, errorCode: "PROJECT_COMPLETED" }],
    ];
    for (const [what, item] of valid)
      await expect(execution(item), what).resolves.toEqual({
        items: [item],
        nextCursor: null,
      });
  });

  it("@s41 refuses a page whose envelope is not exactly { items, nextCursor }", async () => {
    const broken: [string, unknown][] = [
      ["clave de más", { items: [], nextCursor: null, extra: 1 }],
      ["sin nextCursor", { items: [] }],
      ["items que no es una lista", { items: {}, nextCursor: null }],
      ["cursor que no es ni null ni texto", { items: [], nextCursor: 7 }],
      [
        "una ejecución válida y otra rota",
        { items: [validRun, { ...validRun, attempt: "1" }], nextCursor: null },
      ],
    ];
    for (const [what, body] of broken)
      await expect(paged(body), what).rejects.toThrow(INCOMPATIBLE);
  });
});

function m(preview: unknown) {
  return {
    eventId: "55555555-5555-4555-8555-555555555555",
    eventType: "TaskCreated.v1",
    occurredAt: "2026-09-08T10:15:30.123456Z",
    preview,
    loopGuarded: false,
  };
}
