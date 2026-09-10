// @ts-nocheck
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
