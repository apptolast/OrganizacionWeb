import { afterEach, expect, it, vi } from "vitest";
import {
  createWebhook,
  deleteWebhook,
  listWebhooks,
  listWebhookDeliveries,
  pingWebhook,
  redeliverWebhook,
  setWebhookStatus,
  webhookEventTypes,
} from "./webhooks-client";

const id = "12345678-1234-4234-8234-123456789abc";
const other = "22222222-2222-4222-8222-222222222222";
const secret = `whsec_${"A".repeat(43)}`;

afterEach(() => {
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});

function endpoint(overrides: Record<string, unknown> = {}) {
  return {
    id,
    url: "https://example.com/hooks",
    description: "Mi hook",
    eventTypes: ["TaskCreated.v1"],
    status: "active",
    disabledReason: null,
    disabledAt: null,
    createdAt: "2026-09-08T10:00:00.000000Z",
    updatedAt: "2026-09-08T10:00:00.000000Z",
    ...overrides,
  };
}

function delivery(overrides: Record<string, unknown> = {}) {
  return {
    id: other,
    eventId: other,
    eventType: "TaskCreated.v1",
    status: "succeeded",
    attempt: 1,
    httpStatus: 200,
    latencyMs: 12,
    errorClass: null,
    nextAttemptAt: null,
    createdAt: "2026-09-08T10:00:00.000000Z",
    updatedAt: "2026-09-08T10:00:00.000000Z",
    ...overrides,
  };
}

function stub(response: unknown, status = 200) {
  const fetcher = vi
    .fn()
    .mockResolvedValue(
      status === 204
        ? new Response(null, { status })
        : Response.json(response, { status }),
    );
  vi.stubGlobal("fetch", fetcher);
  return fetcher;
}

it("@s36 lists the owner's webhooks over the closed endpoint DTO", async () => {
  const fetcher = stub({ items: [endpoint()] });

  const items = await listWebhooks(new AbortController().signal);

  expect(fetcher).toHaveBeenCalledWith(
    "/api/v1/me/webhooks",
    expect.objectContaining({}),
  );
  expect(items).toEqual([endpoint()]);
});

it("@s36 accepts an empty list", async () => {
  stub({ items: [] });
  expect(await listWebhooks(new AbortController().signal)).toEqual([]);
});

it("@s36 rejects a list whose element carries an unexpected property", async () => {
  stub({ items: [{ ...endpoint(), secret }] });
  await expect(listWebhooks(new AbortController().signal)).rejects.toThrow(
    "Confirmación incompatible",
  );
});

it("@s36 rejects a body with anything beyond items", async () => {
  stub({ items: [], nextCursor: null });
  await expect(listWebhooks(new AbortController().signal)).rejects.toThrow(
    "Confirmación incompatible",
  );
});

it("@s36 rejects a status the contract does not define", async () => {
  stub({ items: [endpoint({ status: "paused" })] });
  await expect(listWebhooks(new AbortController().signal)).rejects.toThrow(
    "Confirmación incompatible",
  );
});

// `rejects.toThrow("…")` se cumple también cuando lo que se lanza es `undefined`
// en vez de un Error, así que por sí solo no acredita que el rechazo lleve un
// diagnóstico. Este oráculo fija el valor rechazado entero, una vez, para toda
// la familia de rechazos del cliente.
it("@s36 rejects with a real Error carrying the diagnosis, not a bare throw", async () => {
  stub({ items: [endpoint({ status: "paused" })] });

  const rejection: unknown = await listWebhooks(
    new AbortController().signal,
  ).then(
    () => null,
    (reason: unknown) => reason,
  );

  expect(rejection).toBeInstanceOf(Error);
  expect((rejection as Error).message).toBe("Confirmación incompatible");
});

// Cada campo del DTO cerrado tiene su propia guarda y ninguna se ejercía: todas
// las pruebas partían del mismo endpoint válido y sólo variaban `status`.
it("@s36 rejects an id of thirty-six characters that is not a uuid", async () => {
  const looksRight = "zzzzzzzz-zzzz-4zzz-8zzz-zzzzzzzzzzzz";
  expect(looksRight).toHaveLength(36);
  stub({ items: [endpoint({ id: looksRight })] });

  await expect(listWebhooks(new AbortController().signal)).rejects.toThrow(
    "Confirmación incompatible",
  );
});

it("@s36 rejects a url that is not even a string", async () => {
  stub({ items: [endpoint({ url: 42 })] });

  await expect(listWebhooks(new AbortController().signal)).rejects.toThrow(
    "Confirmación incompatible",
  );
});

it("@s2 rejects a destination that is not https", async () => {
  stub({ items: [endpoint({ url: "http://example.com/hooks" })] });

  await expect(listWebhooks(new AbortController().signal)).rejects.toThrow(
    "Confirmación incompatible",
  );
});

it("@s36 rejects a description that is not a string", async () => {
  stub({ items: [endpoint({ description: 42 })] });

  await expect(listWebhooks(new AbortController().signal)).rejects.toThrow(
    "Confirmación incompatible",
  );
});

// @s3 mide la descripción en puntos de código, no en unidades UTF-16: ochenta
// emojis son ochenta puntos de código y ciento sesenta unidades.
it("@s3 accepts a description of exactly eighty code points", async () => {
  const eighty = "😀".repeat(80);
  expect([...eighty]).toHaveLength(80);
  expect(eighty.length).toBe(160);
  stub({ items: [endpoint({ description: eighty })] });

  const items = await listWebhooks(new AbortController().signal);

  expect(items[0].description).toBe(eighty);
});

it("@s3 rejects a description of eighty-one code points", async () => {
  stub({ items: [endpoint({ description: "😀".repeat(81) })] });

  await expect(listWebhooks(new AbortController().signal)).rejects.toThrow(
    "Confirmación incompatible",
  );
});

it("@s39 rejects a disabled endpoint that carries no reason", async () => {
  stub({ items: [endpoint({ status: "disabled" })] });
  await expect(listWebhooks(new AbortController().signal)).rejects.toThrow(
    "Confirmación incompatible",
  );
});

it("@s39 accepts a disabled endpoint with its reason and instant", async () => {
  stub({
    items: [
      endpoint({
        status: "disabled",
        disabledReason: "DELIVERY_EXHAUSTED",
        disabledAt: "2026-09-08T11:00:00.000000Z",
      }),
    ],
  });
  const items = await listWebhooks(new AbortController().signal);
  expect(items[0].disabledReason).toBe("DELIVERY_EXHAUSTED");
});

// El orden de catálogo, la ausencia de repetidos y el catálogo cerrado son tres
// reglas distintas de la misma expresión (@s3). Con un solo tipo por lista son
// indistinguibles: hacen falta listas de dos o más para separarlas.
it("@s36 accepts the twelve types when they arrive in catalogue order", async () => {
  stub({ items: [endpoint({ eventTypes: [...webhookEventTypes] })] });

  const items = await listWebhooks(new AbortController().signal);

  expect(items[0].eventTypes).toEqual([...webhookEventTypes]);
});

it("@s36 accepts two types that respect catalogue order", async () => {
  stub({
    items: [endpoint({ eventTypes: ["TaskCreated.v1", "BlockPlanned.v1"] })],
  });

  const items = await listWebhooks(new AbortController().signal);

  expect(items[0].eventTypes).toEqual(["TaskCreated.v1", "BlockPlanned.v1"]);
});

it("@s36 rejects two known types delivered out of catalogue order", async () => {
  stub({
    items: [endpoint({ eventTypes: ["SubtaskCreated.v1", "TaskCreated.v1"] })],
  });

  await expect(listWebhooks(new AbortController().signal)).rejects.toThrow(
    "Confirmación incompatible",
  );
});

it("@s36 rejects the same type repeated", async () => {
  stub({
    items: [endpoint({ eventTypes: ["TaskCreated.v1", "TaskCreated.v1"] })],
  });

  await expect(listWebhooks(new AbortController().signal)).rejects.toThrow(
    "Confirmación incompatible",
  );
});

it("@s36 rejects a type outside the catalogue when it is the only one", async () => {
  stub({ items: [endpoint({ eventTypes: ["taskcreated.v1"] })] });

  await expect(listWebhooks(new AbortController().signal)).rejects.toThrow(
    "Confirmación incompatible",
  );
});

it("@s36 rejects a single unknown type hidden after valid ones", async () => {
  stub({
    items: [
      endpoint({ eventTypes: ["TaskCreated.v1", "ProjectCreated.v2"] }),
    ],
  });

  await expect(listWebhooks(new AbortController().signal)).rejects.toThrow(
    "Confirmación incompatible",
  );
});

it("@s36 rejects an endpoint subscribed to no type at all", async () => {
  stub({ items: [endpoint({ eventTypes: [] })] });

  await expect(listWebhooks(new AbortController().signal)).rejects.toThrow(
    "Confirmación incompatible",
  );
});

it("@s36 rejects eventTypes that is not an array", async () => {
  stub({ items: [endpoint({ eventTypes: "TaskCreated.v1" })] });

  await expect(listWebhooks(new AbortController().signal)).rejects.toThrow(
    "Confirmación incompatible",
  );
});

// La invariante de @s1 es doble: un endpoint desactivado lleva SIEMPRE razón e
// instante, y uno activo NUNCA. Comprobar sólo el caso en que fallan las dos
// mitades a la vez no separa el `||` del `&&`: hacen falta los asimétricos.
it("@s39 rejects a disabled endpoint with a reason but no instant", async () => {
  stub({
    items: [
      endpoint({
        status: "disabled",
        disabledReason: "MANUAL",
        disabledAt: null,
      }),
    ],
  });

  await expect(listWebhooks(new AbortController().signal)).rejects.toThrow(
    "Confirmación incompatible",
  );
});

it("@s39 rejects a disabled endpoint with an instant but no reason", async () => {
  stub({
    items: [
      endpoint({
        status: "disabled",
        disabledReason: null,
        disabledAt: "2026-09-08T11:00:00.000000Z",
      }),
    ],
  });

  await expect(listWebhooks(new AbortController().signal)).rejects.toThrow(
    "Confirmación incompatible",
  );
});

it("@s39 rejects a disabled reason outside the two the contract defines", async () => {
  stub({
    items: [
      endpoint({
        status: "disabled",
        disabledReason: "PAUSED",
        disabledAt: "2026-09-08T11:00:00.000000Z",
      }),
    ],
  });

  await expect(listWebhooks(new AbortController().signal)).rejects.toThrow(
    "Confirmación incompatible",
  );
});

it("@s39 rejects a disabledAt that is not an instant", async () => {
  stub({
    items: [
      endpoint({
        status: "disabled",
        disabledReason: "MANUAL",
        disabledAt: "2026-09-08",
      }),
    ],
  });

  await expect(listWebhooks(new AbortController().signal)).rejects.toThrow(
    "Confirmación incompatible",
  );
});

it("@s39 rejects an active endpoint that still carries a disabledAt", async () => {
  stub({
    items: [endpoint({ disabledAt: "2026-09-08T11:00:00.000000Z" })],
  });

  await expect(listWebhooks(new AbortController().signal)).rejects.toThrow(
    "Confirmación incompatible",
  );
});

it("@s37 creates a webhook and returns the one-time secret", async () => {
  const fetcher = stub({ endpoint: endpoint(), secret }, 201);

  const created = await createWebhook(
    {
      url: "https://example.com/hooks",
      description: "Mi hook",
      eventTypes: ["TaskCreated.v1"],
    },
    new AbortController().signal,
  );

  expect(created.secret).toBe(secret);
  expect(created.endpoint.id).toBe(id);
  const [url, options] = fetcher.mock.calls[0];
  expect(url).toBe("/api/v1/me/webhooks");
  expect(options.method).toBe("POST");
  expect(JSON.parse(options.body)).toEqual({
    url: "https://example.com/hooks",
    description: "Mi hook",
    eventTypes: ["TaskCreated.v1"],
  });
});

it("@s37 rejects a creation whose secret is not a whsec_ of 43 base64url characters", async () => {
  for (const bad of [
    "whsec_short",
    `owp_${"A".repeat(43)}`,
    `whsec_${"A".repeat(44)}`,
  ]) {
    stub({ endpoint: endpoint(), secret: bad }, 201);
    await expect(
      createWebhook(
        {
          url: "https://example.com/hooks",
          description: "",
          eventTypes: ["TaskCreated.v1"],
        },
        new AbortController().signal,
      ),
    ).rejects.toThrow("Confirmación incompatible");
  }
});

it("@s39 changes the status and returns the endpoint", async () => {
  const fetcher = stub(
    endpoint({
      status: "disabled",
      disabledReason: "MANUAL",
      disabledAt: "2026-09-08T11:00:00.000000Z",
    }),
  );

  const changed = await setWebhookStatus(
    id,
    "disabled",
    new AbortController().signal,
  );

  expect(changed.status).toBe("disabled");
  const [url, options] = fetcher.mock.calls[0];
  expect(url).toBe(`/api/v1/me/webhooks/${id}/status`);
  expect(options.method).toBe("PUT");
  expect(JSON.parse(options.body)).toEqual({ status: "disabled" });
});

it("@s39 deletes a webhook", async () => {
  const fetcher = stub(null, 204);

  await deleteWebhook(id, new AbortController().signal);

  const [url, options] = fetcher.mock.calls[0];
  expect(url).toBe(`/api/v1/me/webhooks/${id}`);
  expect(options.method).toBe("DELETE");
});

it("@s39 sends a ping and returns the pending delivery", async () => {
  const fetcher = stub(
    {
      delivery: delivery({
        status: "pending",
        attempt: 0,
        httpStatus: null,
        latencyMs: null,
        eventType: "webhook.ping.v1",
        nextAttemptAt: "2026-09-08T10:00:00.000000Z",
      }),
    },
    202,
  );

  const sent = await pingWebhook(id, new AbortController().signal);

  expect(sent.status).toBe("pending");
  expect(sent.eventType).toBe("webhook.ping.v1");
  expect(fetcher.mock.calls[0][0]).toBe(`/api/v1/me/webhooks/${id}/ping`);
});

it("@s40 lists deliveries over the closed delivery DTO", async () => {
  const fetcher = stub({ items: [delivery()] });

  const items = await listWebhookDeliveries(id, new AbortController().signal);

  expect(items).toEqual([delivery()]);
  expect(fetcher.mock.calls[0][0]).toBe(`/api/v1/me/webhooks/${id}/deliveries`);
});

it("@s40 rejects a delivery that leaks a body or a url", async () => {
  stub({ items: [{ ...delivery(), body: "{}" }] });
  await expect(
    listWebhookDeliveries(id, new AbortController().signal),
  ).rejects.toThrow("Confirmación incompatible");
});

it("@s40 redelivers a terminal delivery", async () => {
  const fetcher = stub(
    {
      delivery: delivery({
        status: "pending",
        attempt: 0,
        httpStatus: null,
        latencyMs: null,
        nextAttemptAt: "2026-09-08T13:00:00.000000Z",
      }),
    },
    202,
  );

  const reopened = await redeliverWebhook(
    id,
    other,
    new AbortController().signal,
  );

  expect(reopened.attempt).toBe(0);
  expect(fetcher.mock.calls[0][0]).toBe(
    `/api/v1/me/webhooks/${id}/deliveries/${other}/redeliver`,
  );
});

it("@s41 throws the response itself so the view can read its problem code", async () => {
  stub({ code: "WEBHOOK_LIMIT" }, 409);
  await expect(
    listWebhooks(new AbortController().signal),
  ).rejects.toBeInstanceOf(Response);
});

it("@s38 refuses to start once the signal is already aborted", async () => {
  const fetcher = stub({ items: [] });
  const controller = new AbortController();
  controller.abort();

  await expect(listWebhooks(controller.signal)).rejects.toThrow();
  expect(fetcher).not.toHaveBeenCalled();
});

it("@s37 exposes the twelve subscribable types in catalogue order", () => {
  expect(webhookEventTypes).toEqual([
    "ProjectCreated.v1",
    "ProjectUpdated.v1",
    "ProjectStatusChanged.v1",
    "TaskCreated.v1",
    "SubtaskCreated.v1",
    "TaskStatusChanged.v1",
    "BlockPlanned.v1",
    "BlockChanged.v1",
    "WorkSessionStarted.v1",
    "WorkSessionStateChanged.v1",
    "WorkSessionExtended.v1",
    "WorkSessionClosed.v1",
  ]);
});
