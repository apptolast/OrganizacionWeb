import { afterEach, expect, it, vi } from "vitest";
import { observeAccess, setCsrfToken } from "./api-client";
import {
  CustomizationValidationError,
  readCustomization,
  saveCustomizationView,
  createCustomField,
  updateCustomField,
} from "./customization-api";

afterEach(() => {
  vi.unstubAllGlobals();
  observeAccess();
  setCsrfToken();
});

it("@s1 reads absent PROJECT defaults through the authenticated endpoint", async () => {
  const body = {
    configured: false,
    visibleFields: ["createdAt"],
    customFields: [],
    updatedAt: null,
  };
  const etag = '"customization:PROJECT:unconfigured"';
  const fetcher = vi
    .fn()
    .mockResolvedValue(Response.json(body, { headers: { ETag: etag } }));
  vi.stubGlobal("fetch", fetcher);
  await expect(readCustomization("PROJECT")).resolves.toEqual({
    ...body,
    etag,
  });
  expect(fetcher).toHaveBeenCalledExactlyOnceWith(
    "/api/v1/me/customization/PROJECT",
    expect.objectContaining({ credentials: "same-origin", cache: "no-store" }),
  );
});

it("@s27 rejects a configuration response with an additional field", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        {
          configured: false,
          visibleFields: ["createdAt"],
          customFields: [],
          updatedAt: null,
          extra: true,
        },
        { headers: { ETag: '"customization:PROJECT:unconfigured"' } },
      ),
    ),
  );
  await expect(readCustomization("PROJECT")).rejects.toThrow();
});

it("@s20 preserves a failed read rather than publishing its problem as configuration", async () => {
  const response = Response.json(
    { code: "STORAGE_UNAVAILABLE" },
    { status: 503 },
  );
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(response));
  await expect(readCustomization("PROJECT")).rejects.toBe(response);
});

it("@s27 rejects PROJECT defaults carrying the TASK tag", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        {
          configured: false,
          visibleFields: ["createdAt"],
          customFields: [],
          updatedAt: null,
        },
        { headers: { ETag: '"customization:TASK:unconfigured"' } },
      ),
    ),
  );
  await expect(readCustomization("PROJECT")).rejects.toThrow();
});

it("@s27 rejects altered defaults for an absent PROJECT scope", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        {
          configured: false,
          visibleFields: [],
          customFields: [],
          updatedAt: null,
        },
        { headers: { ETag: '"customization:PROJECT:unconfigured"' } },
      ),
    ),
  );
  await expect(readCustomization("PROJECT")).rejects.toThrow();
});

it("@s27 rejects absence with a saved date", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        {
          configured: false,
          visibleFields: ["createdAt"],
          customFields: [],
          updatedAt: "2026-09-07T10:00:00Z",
        },
        { headers: { ETag: '"customization:PROJECT:unconfigured"' } },
      ),
    ),
  );
  await expect(readCustomization("PROJECT")).rejects.toThrow();
});

it("@s27 rejects a nonboolean configured flag", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        {
          configured: "false",
          visibleFields: ["createdAt"],
          customFields: [],
          updatedAt: null,
        },
        { headers: { ETag: '"customization:PROJECT:unconfigured"' } },
      ),
    ),
  );
  await expect(readCustomization("PROJECT")).rejects.toThrow();
});

it("@s27 rejects a configured snapshot with an invalid timestamp", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        {
          configured: true,
          visibleFields: [],
          customFields: [],
          updatedAt: "not-a-date",
        },
        {
          headers: {
            ETag: '"customization:PROJECT:12345678-1234-1234-1234-123456789abc:0"',
          },
        },
      ),
    ),
  );
  await expect(readCustomization("PROJECT")).rejects.toThrow();
});

it("@s27 rejects a noncanonical configured revision", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        {
          configured: true,
          visibleFields: [],
          customFields: [],
          updatedAt: "2026-09-07T10:00:00Z",
        },
        {
          headers: {
            ETag: '"customization:PROJECT:12345678-1234-1234-1234-123456789abc:01"',
          },
        },
      ),
    ),
  );
  await expect(readCustomization("PROJECT")).rejects.toThrow();
});

it("@s27 rejects a configured revision above signed BIGINT", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        {
          configured: true,
          visibleFields: [],
          customFields: [],
          updatedAt: "2026-09-07T10:00:00Z",
        },
        {
          headers: {
            ETag: '"customization:PROJECT:12345678-1234-1234-1234-123456789abc:9223372036854775808"',
          },
        },
      ),
    ),
  );
  await expect(readCustomization("PROJECT")).rejects.toThrow();
});

it("@s27 rejects metadata outside the requested scope", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        {
          configured: true,
          visibleFields: ["completionCriterion"],
          customFields: [],
          updatedAt: "2026-09-07T10:00:00Z",
        },
        {
          headers: {
            ETag: '"customization:PROJECT:12345678-1234-1234-1234-123456789abc:0"',
          },
        },
      ),
    ),
  );
  await expect(readCustomization("PROJECT")).rejects.toThrow();
});

it("@s27 rejects repeated metadata", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        {
          configured: true,
          visibleFields: ["createdAt", "createdAt"],
          customFields: [],
          updatedAt: "2026-09-07T10:00:00Z",
        },
        {
          headers: {
            ETag: '"customization:PROJECT:12345678-1234-1234-1234-123456789abc:0"',
          },
        },
      ),
    ),
  );
  await expect(readCustomization("PROJECT")).rejects.toThrow();
});

it("@s27 rejects absence containing a definition", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        {
          configured: false,
          visibleFields: ["createdAt"],
          customFields: [
            {
              id: "22345678-1234-1234-1234-123456789abc",
              label: "Dato",
              type: "TEXT",
              active: true,
            },
          ],
          updatedAt: null,
        },
        { headers: { ETag: '"customization:PROJECT:unconfigured"' } },
      ),
    ),
  );
  await expect(readCustomization("PROJECT")).rejects.toThrow();
});

it("@s27 rejects a definition with an unsupported type", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        {
          configured: true,
          visibleFields: [],
          customFields: [
            {
              id: "22345678-1234-1234-1234-123456789abc",
              label: "Dato",
              type: "FORMULA",
              active: true,
            },
          ],
          updatedAt: "2026-09-07T10:00:00Z",
        },
        {
          headers: {
            ETag: '"customization:PROJECT:12345678-1234-1234-1234-123456789abc:0"',
          },
        },
      ),
    ),
  );
  await expect(readCustomization("PROJECT")).rejects.toThrow();
});

it("@s27 rejects a definition whose label is empty", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        {
          configured: true,
          visibleFields: [],
          customFields: [
            {
              id: "22345678-1234-1234-1234-123456789abc",
              label: "",
              type: "TEXT",
              active: true,
            },
          ],
          updatedAt: "2026-09-07T10:00:00Z",
        },
        {
          headers: {
            ETag: '"customization:PROJECT:12345678-1234-1234-1234-123456789abc:0"',
          },
        },
      ),
    ),
  );
  await expect(readCustomization("PROJECT")).rejects.toThrow();
});

it("@s27 enforces the label limit in Unicode code points", async () => {
  const field = {
    id: "22345678-1234-1234-1234-123456789abc",
    label: "\u{1f331}".repeat(60),
    type: "TEXT",
    active: true,
  };
  const body = {
    configured: true,
    visibleFields: [],
    customFields: [field],
    updatedAt: "2026-09-07T10:00:00Z",
  };
  const etag = '"customization:PROJECT:12345678-1234-1234-1234-123456789abc:0"';
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValueOnce(Response.json(body, { headers: { ETag: etag } }))
      .mockResolvedValueOnce(
        Response.json(
          { ...body, customFields: [{ ...field, label: field.label + "x" }] },
          { headers: { ETag: etag } },
        ),
      ),
  );
  await expect(readCustomization("PROJECT")).resolves.toEqual({
    ...body,
    etag,
  });
  await expect(readCustomization("PROJECT")).rejects.toThrow();
});

it("@s27 rejects a noncanonical label with exterior Unicode whitespace", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        {
          configured: true,
          visibleFields: [],
          customFields: [
            {
              id: "22345678-1234-1234-1234-123456789abc",
              label: "\u0085Dato",
              type: "TEXT",
              active: true,
            },
          ],
          updatedAt: "2026-09-07T10:00:00Z",
        },
        {
          headers: {
            ETag: '"customization:PROJECT:12345678-1234-1234-1234-123456789abc:0"',
          },
        },
      ),
    ),
  );
  await expect(readCustomization("PROJECT")).rejects.toThrow();
});

it("@s27 rejects an isolated UTF16 surrogate in a label", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        {
          configured: true,
          visibleFields: [],
          customFields: [
            {
              id: "22345678-1234-1234-1234-123456789abc",
              label: "Dato\ud800",
              type: "TEXT",
              active: true,
            },
          ],
          updatedAt: "2026-09-07T10:00:00Z",
        },
        {
          headers: {
            ETag: '"customization:PROJECT:12345678-1234-1234-1234-123456789abc:0"',
          },
        },
      ),
    ),
  );
  await expect(readCustomization("PROJECT")).rejects.toThrow();
});

it("@s27 rejects a NUL in a label", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        {
          configured: true,
          visibleFields: [],
          customFields: [
            {
              id: "22345678-1234-1234-1234-123456789abc",
              label: "Dato\u0000",
              type: "TEXT",
              active: true,
            },
          ],
          updatedAt: "2026-09-07T10:00:00Z",
        },
        {
          headers: {
            ETag: '"customization:PROJECT:12345678-1234-1234-1234-123456789abc:0"',
          },
        },
      ),
    ),
  );
  await expect(readCustomization("PROJECT")).rejects.toThrow();
});

it("@s27 rejects a definition with an invalid identity", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        {
          configured: true,
          visibleFields: [],
          customFields: [
            { id: "not-an-id", label: "Dato", type: "TEXT", active: true },
          ],
          updatedAt: "2026-09-07T10:00:00Z",
        },
        {
          headers: {
            ETag: '"customization:PROJECT:12345678-1234-1234-1234-123456789abc:0"',
          },
        },
      ),
    ),
  );
  await expect(readCustomization("PROJECT")).rejects.toThrow();
});

it("@s27 rejects a nonboolean definition activation flag", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        {
          configured: true,
          visibleFields: [],
          customFields: [
            {
              id: "22345678-1234-1234-1234-123456789abc",
              label: "Dato",
              type: "TEXT",
              active: "false",
            },
          ],
          updatedAt: "2026-09-07T10:00:00Z",
        },
        {
          headers: {
            ETag: '"customization:PROJECT:12345678-1234-1234-1234-123456789abc:0"',
          },
        },
      ),
    ),
  );
  await expect(readCustomization("PROJECT")).rejects.toThrow();
});

it("@s27 rejects repeated definition IDs even with different labels", async () => {
  const field = {
    id: "22345678-1234-1234-1234-123456789abc",
    label: "Dato",
    type: "TEXT",
    active: true,
  };
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        {
          configured: true,
          visibleFields: [],
          customFields: [field, { ...field, label: "Otro" }],
          updatedAt: "2026-09-07T10:00:00Z",
        },
        {
          headers: {
            ETag: '"customization:PROJECT:12345678-1234-1234-1234-123456789abc:0"',
          },
        },
      ),
    ),
  );
  await expect(readCustomization("PROJECT")).rejects.toThrow();
});

it("@s27 rejects repeated labels across active and inactive definitions", async () => {
  const field = {
    id: "22345678-1234-1234-1234-123456789abc",
    label: "Dato",
    type: "TEXT",
    active: true,
  };
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        {
          configured: true,
          visibleFields: [],
          customFields: [
            field,
            {
              ...field,
              id: "32345678-1234-1234-1234-123456789abc",
              active: false,
            },
          ],
          updatedAt: "2026-09-07T10:00:00Z",
        },
        {
          headers: {
            ETag: '"customization:PROJECT:12345678-1234-1234-1234-123456789abc:0"',
          },
        },
      ),
    ),
  );
  await expect(readCustomization("PROJECT")).rejects.toThrow();
});

it("@s27 accepts twelve definitions but rejects thirteen including inactive ones", async () => {
  const fields = Array.from({ length: 13 }, (_, index) => ({
    id: `22345678-1234-1234-1234-${String(index).padStart(12, "0")}`,
    label: `Dato ${index}`,
    type: "TEXT",
    active: false,
  }));
  const body = {
    configured: true,
    visibleFields: [],
    customFields: fields.slice(0, 12),
    updatedAt: "2026-09-07T10:00:00Z",
  };
  const etag = '"customization:PROJECT:12345678-1234-1234-1234-123456789abc:0"';
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValueOnce(Response.json(body, { headers: { ETag: etag } }))
      .mockResolvedValueOnce(
        Response.json(
          { ...body, customFields: fields },
          { headers: { ETag: etag } },
        ),
      ),
  );
  await expect(readCustomization("PROJECT")).resolves.toEqual({
    ...body,
    etag,
  });
  await expect(readCustomization("PROJECT")).rejects.toThrow();
});

it("@s37 discards JSON that finishes after the read is aborted", async () => {
  let finish!: (body: unknown) => void;
  const response = Response.json(null, {
    headers: { ETag: '"customization:PROJECT:unconfigured"' },
  });
  vi.spyOn(response, "json").mockImplementation(
    () =>
      new Promise((resolve) => {
        finish = resolve;
      }),
  );
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(response));
  const controller = new AbortController();
  const pending = readCustomization("PROJECT", controller.signal);
  await vi.waitFor(() => expect(finish).toBeTypeOf("function"));
  controller.abort();
  finish({
    configured: false,
    visibleFields: ["createdAt"],
    customFields: [],
    updatedAt: null,
  });
  await expect(pending).rejects.toMatchObject({ name: "AbortError" });
});

it("@s37 discards a late HTTP401 before consuming it as a current failure", async () => {
  let finish!: (response: Response) => void;
  const observer = vi.fn();
  observeAccess(observer);
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation(
      () =>
        new Promise((resolve) => {
          finish = resolve;
        }),
    ),
  );
  const controller = new AbortController();
  const pending = readCustomization("PROJECT", controller.signal);
  controller.abort();
  finish(new Response(null, { status: 401 }));
  await expect(pending).rejects.toMatchObject({ name: "AbortError" });
  expect(observer).not.toHaveBeenCalled();
});

it("@s2 saves the ordered visible fields with the current tag and CSRF", async () => {
  const before = {
    configured: true,
    visibleFields: ["createdAt"],
    customFields: [],
    updatedAt: "2026-09-07T10:00:00Z",
    etag: '"customization:PROJECT:12345678-1234-1234-1234-123456789abc:0"',
  };
  const body = {
    configured: true,
    visibleFields: ["updatedAt", "createdAt"],
    customFields: [],
    updatedAt: "2026-09-07T10:01:00Z",
  };
  const etag = '"customization:PROJECT:12345678-1234-1234-1234-123456789abc:1"';
  const fetcher = vi
    .fn()
    .mockResolvedValue(Response.json(body, { headers: { ETag: etag } }));
  vi.stubGlobal("fetch", fetcher);
  setCsrfToken("csrf21");
  await expect(
    saveCustomizationView("PROJECT", before, ["updatedAt", "createdAt"]),
  ).resolves.toEqual({ ...body, etag });
  expect(fetcher).toHaveBeenCalledTimes(1);
  const [url, options] = fetcher.mock.calls[0];
  expect(url).toBe("/api/v1/me/customization/PROJECT");
  expect(options).toMatchObject({
    method: "PUT",
    credentials: "same-origin",
    cache: "no-store",
    body: JSON.stringify({ visibleFields: body.visibleFields }),
  });
  expect(new Headers(options.headers).get("If-Match")).toBe(before.etag);
  expect(new Headers(options.headers).get("X-CSRF-TOKEN")).toBe("csrf21");
});

it("@s28 rejects an incompatible view confirmation using the captured intent", async () => {
  const previous = {
    configured: false,
    visibleFields: ["createdAt"],
    customFields: [],
    updatedAt: null,
    etag: '"customization:PROJECT:unconfigured"',
  };
  const visibleFields = ["updatedAt"];
  let finish!: (response: Response) => void;
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation(
      () =>
        new Promise((resolve) => {
          finish = resolve;
        }),
    ),
  );
  const pending = saveCustomizationView("PROJECT", previous, visibleFields);
  visibleFields[0] = "createdAt";
  finish(
    Response.json(
      {
        configured: true,
        visibleFields: ["createdAt"],
        customFields: [],
        updatedAt: "2026-09-07T10:01:00Z",
      },
      {
        headers: {
          ETag: '"customization:PROJECT:12345678-1234-1234-1234-123456789abc:0"',
        },
      },
    ),
  );
  await expect(pending).rejects.toThrow();
});

it("@s28 rejects a view confirmation that removes a previous definition", async () => {
  const previous = {
    configured: true,
    visibleFields: ["createdAt"],
    customFields: [
      {
        id: "22345678-1234-1234-1234-123456789abc",
        label: "Dato",
        type: "TEXT" as const,
        active: false,
      },
    ],
    updatedAt: "2026-09-07T10:00:00Z",
    etag: '"customization:PROJECT:12345678-1234-1234-1234-123456789abc:0"',
  };
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        {
          configured: true,
          visibleFields: [],
          customFields: [],
          updatedAt: "2026-09-07T10:01:00Z",
        },
        {
          headers: {
            ETag: '"customization:PROJECT:12345678-1234-1234-1234-123456789abc:1"',
          },
        },
      ),
    ),
  );
  await expect(
    saveCustomizationView("PROJECT", previous, []),
  ).rejects.toThrow();
});

it("@s28 rejects absence as confirmation of saving the default view", async () => {
  const body = {
    configured: false,
    visibleFields: ["createdAt"],
    customFields: [],
    updatedAt: null,
  };
  const etag = '"customization:PROJECT:unconfigured"';
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(Response.json(body, { headers: { ETag: etag } })),
  );
  await expect(
    saveCustomizationView("PROJECT", { ...body, etag }, ["createdAt"]),
  ).rejects.toThrow();
});

it("@s3 creates a normalized definition while preserving the default view", async () => {
  const previous = {
    configured: false,
    visibleFields: ["createdAt"],
    customFields: [],
    updatedAt: null,
    etag: '"customization:PROJECT:unconfigured"',
  };
  const body = {
    configured: true,
    visibleFields: ["createdAt"],
    customFields: [
      {
        id: "22345678-1234-1234-1234-123456789abc",
        label: "Dato",
        type: "TEXT",
        active: true,
      },
    ],
    updatedAt: "2026-09-07T10:00:00Z",
  };
  const etag = '"customization:PROJECT:12345678-1234-1234-1234-123456789abc:0"';
  const fetcher = vi
    .fn()
    .mockResolvedValue(Response.json(body, { headers: { ETag: etag } }));
  vi.stubGlobal("fetch", fetcher);
  await expect(
    createCustomField("PROJECT", previous, {
      label: "\u0085 Dato  ",
      type: "TEXT",
    }),
  ).resolves.toEqual({ ...body, etag });
  const [url, options] = fetcher.mock.calls[0];
  expect(url).toBe("/api/v1/me/customization/PROJECT/fields");
  expect(options.method).toBe("POST");
  expect(new Headers(options.headers).get("If-Match")).toBe(previous.etag);
  expect(JSON.parse(options.body)).toEqual({ label: "Dato", type: "TEXT" });
});

it("@s28 rejects a creation receipt with another label", async () => {
  const previous = {
    configured: false,
    visibleFields: ["createdAt"],
    customFields: [],
    updatedAt: null,
    etag: '"customization:PROJECT:unconfigured"',
  };
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        {
          configured: true,
          visibleFields: ["createdAt"],
          customFields: [
            {
              id: "22345678-1234-1234-1234-123456789abc",
              label: "Otro",
              type: "TEXT",
              active: true,
            },
          ],
          updatedAt: "2026-09-07T10:00:00Z",
        },
        {
          headers: {
            ETag: '"customization:PROJECT:12345678-1234-1234-1234-123456789abc:0"',
          },
        },
      ),
    ),
  );
  await expect(
    createCustomField("PROJECT", previous, { label: "Dato", type: "TEXT" }),
  ).rejects.toThrow();
});

it("@s28 rejects creation that returns an existing identity", async () => {
  const field = {
    id: "22345678-1234-1234-1234-123456789abc",
    label: "Antes",
    type: "TEXT" as const,
    active: true,
  };
  const previous = {
    configured: true,
    visibleFields: [],
    customFields: [field],
    updatedAt: "2026-09-07T10:00:00Z",
    etag: '"customization:PROJECT:12345678-1234-1234-1234-123456789abc:0"',
  };
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        {
          configured: true,
          visibleFields: [],
          customFields: [{ ...field, label: "Dato" }],
          updatedAt: "2026-09-07T10:01:00Z",
        },
        {
          headers: {
            ETag: '"customization:PROJECT:12345678-1234-1234-1234-123456789abc:1"',
          },
        },
      ),
    ),
  );
  await expect(
    createCustomField("PROJECT", previous, { label: "Dato", type: "TEXT" }),
  ).rejects.toThrow();
});

it("@s28 rejects creation with the wrong declared type", async () => {
  const previous = {
    configured: false,
    visibleFields: ["createdAt"],
    customFields: [],
    updatedAt: null,
    etag: '"customization:PROJECT:unconfigured"',
  };
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        {
          configured: true,
          visibleFields: ["createdAt"],
          customFields: [
            {
              id: "22345678-1234-1234-1234-123456789abc",
              label: "Dato",
              type: "BOOLEAN",
              active: true,
            },
          ],
          updatedAt: "2026-09-07T10:00:00Z",
        },
        {
          headers: {
            ETag: '"customization:PROJECT:12345678-1234-1234-1234-123456789abc:0"',
          },
        },
      ),
    ),
  );
  await expect(
    createCustomField("PROJECT", previous, { label: "Dato", type: "TEXT" }),
  ).rejects.toThrow();
});

it("@s28 rejects creation that loses an inactive definition", async () => {
  const previous = {
    configured: true,
    visibleFields: [],
    customFields: [
      {
        id: "22345678-1234-1234-1234-123456789abc",
        label: "Antes",
        type: "TEXT" as const,
        active: false,
      },
    ],
    updatedAt: "2026-09-07T10:00:00Z",
    etag: '"customization:PROJECT:12345678-1234-1234-1234-123456789abc:0"',
  };
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        {
          configured: true,
          visibleFields: [],
          customFields: [
            {
              id: "32345678-1234-1234-1234-123456789abc",
              label: "Dato",
              type: "TEXT",
              active: true,
            },
          ],
          updatedAt: "2026-09-07T10:01:00Z",
        },
        {
          headers: {
            ETag: '"customization:PROJECT:12345678-1234-1234-1234-123456789abc:1"',
          },
        },
      ),
    ),
  );
  await expect(
    createCustomField("PROJECT", previous, { label: "Dato", type: "TEXT" }),
  ).rejects.toThrow();
});

it("@s28 rejects creation that changes the saved view", async () => {
  const previous = {
    configured: false,
    visibleFields: ["createdAt"],
    customFields: [],
    updatedAt: null,
    etag: '"customization:PROJECT:unconfigured"',
  };
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        {
          configured: true,
          visibleFields: [],
          customFields: [
            {
              id: "22345678-1234-1234-1234-123456789abc",
              label: "Dato",
              type: "TEXT",
              active: true,
            },
          ],
          updatedAt: "2026-09-07T10:00:00Z",
        },
        {
          headers: {
            ETag: '"customization:PROJECT:12345678-1234-1234-1234-123456789abc:0"',
          },
        },
      ),
    ),
  );
  await expect(
    createCustomField("PROJECT", previous, { label: "Dato", type: "TEXT" }),
  ).rejects.toThrow();
});

it("@s28 rejects an inactive newly created field", async () => {
  const previous = {
    configured: false,
    visibleFields: ["createdAt"],
    customFields: [],
    updatedAt: null,
    etag: '"customization:PROJECT:unconfigured"',
  };
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        {
          configured: true,
          visibleFields: ["createdAt"],
          customFields: [
            {
              id: "22345678-1234-1234-1234-123456789abc",
              label: "Dato",
              type: "TEXT",
              active: false,
            },
          ],
          updatedAt: "2026-09-07T10:00:00Z",
        },
        {
          headers: {
            ETag: '"customization:PROJECT:12345678-1234-1234-1234-123456789abc:0"',
          },
        },
      ),
    ),
  );
  await expect(
    createCustomField("PROJECT", previous, { label: "Dato", type: "TEXT" }),
  ).rejects.toThrow();
});

it("@s4 renames and deactivates a field without sending its immutable type", async () => {
  const field = {
    id: "22345678-1234-1234-1234-123456789abc",
    label: "Antes",
    type: "TEXT" as const,
    active: true,
  };
  const previous = {
    configured: true,
    visibleFields: [],
    customFields: [field],
    updatedAt: "2026-09-07T10:00:00Z",
    etag: '"customization:PROJECT:12345678-1234-1234-1234-123456789abc:0"',
  };
  const body = {
    configured: true,
    visibleFields: [],
    customFields: [{ ...field, label: "Dato", active: false }],
    updatedAt: "2026-09-07T10:01:00Z",
  };
  const etag = '"customization:PROJECT:12345678-1234-1234-1234-123456789abc:1"';
  const fetcher = vi
    .fn()
    .mockResolvedValue(Response.json(body, { headers: { ETag: etag } }));
  vi.stubGlobal("fetch", fetcher);
  await expect(
    updateCustomField("PROJECT", previous, field.id, {
      label: " Dato ",
      active: false,
    }),
  ).resolves.toEqual({ ...body, etag });
  const [url, options] = fetcher.mock.calls[0];
  expect(url).toBe(`/api/v1/me/customization/PROJECT/fields/${field.id}`);
  expect(options.method).toBe("PUT");
  expect(new Headers(options.headers).get("If-Match")).toBe(previous.etag);
  expect(JSON.parse(options.body)).toEqual({ label: "Dato", active: false });
});

it("@s28 rejects an update that changes another definition", async () => {
  const field = {
    id: "22345678-1234-1234-1234-123456789abc",
    label: "Antes",
    type: "TEXT" as const,
    active: true,
  };
  const other = {
    ...field,
    id: "32345678-1234-1234-1234-123456789abc",
    label: "Otro",
  };
  const previous = {
    configured: true,
    visibleFields: [],
    customFields: [field, other],
    updatedAt: "2026-09-07T10:00:00Z",
    etag: '"customization:PROJECT:12345678-1234-1234-1234-123456789abc:0"',
  };
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        {
          configured: true,
          visibleFields: [],
          customFields: [
            { ...field, active: false },
            { ...other, label: "Cambiado" },
          ],
          updatedAt: "2026-09-07T10:01:00Z",
        },
        {
          headers: {
            ETag: '"customization:PROJECT:12345678-1234-1234-1234-123456789abc:1"',
          },
        },
      ),
    ),
  );
  await expect(
    updateCustomField("PROJECT", previous, field.id, {
      label: field.label,
      active: false,
    }),
  ).rejects.toThrow();
});

it("@s28 rejects a field update that changes the saved view", async () => {
  const field = {
    id: "22345678-1234-1234-1234-123456789abc",
    label: "Antes",
    type: "TEXT" as const,
    active: true,
  };
  const previous = {
    configured: true,
    visibleFields: ["createdAt"],
    customFields: [field],
    updatedAt: "2026-09-07T10:00:00Z",
    etag: '"customization:PROJECT:12345678-1234-1234-1234-123456789abc:0"',
  };
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        {
          configured: true,
          visibleFields: [],
          customFields: [{ ...field, active: false }],
          updatedAt: "2026-09-07T10:01:00Z",
        },
        {
          headers: {
            ETag: '"customization:PROJECT:12345678-1234-1234-1234-123456789abc:1"',
          },
        },
      ),
    ),
  );
  await expect(
    updateCustomField("PROJECT", previous, field.id, {
      label: field.label,
      active: false,
    }),
  ).rejects.toThrow();
});

it("@s34 exposes a coherent label validation problem for correction", async () => {
  const previous = {
    configured: false,
    visibleFields: ["createdAt"],
    customFields: [],
    updatedAt: null,
    etag: '"customization:PROJECT:unconfigured"',
  };
  const errors = [
    { field: "label", code: "INVALID_VALUE", message: "Elige otra etiqueta." },
  ];
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        {
          type: "urn:organization:problem:validation_error",
          title: "Campos invalidos",
          status: 400,
          code: "VALIDATION_ERROR",
          errors,
        },
        { status: 400 },
      ),
    ),
  );
  await expect(
    createCustomField("PROJECT", previous, { label: "Dato", type: "TEXT" }),
  ).rejects.toMatchObject({
    constructor: CustomizationValidationError,
    errors: [{ field: "label", message: "Elige otra etiqueta." }],
  });
});

it("@s34 keeps a contradictory validation problem uncertain", async () => {
  const previous = {
    configured: false,
    visibleFields: ["createdAt"],
    customFields: [],
    updatedAt: null,
    etag: '"customization:PROJECT:unconfigured"',
  };
  const response = Response.json(
    {
      type: "urn:organization:problem:validation_error",
      title: "Campos invalidos",
      status: 412,
      code: "VALIDATION_ERROR",
      errors: [
        {
          field: "label",
          code: "INVALID_VALUE",
          message: "Elige otra etiqueta.",
        },
      ],
    },
    { status: 400 },
  );
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(response));
  await expect(
    createCustomField("PROJECT", previous, { label: "Dato", type: "TEXT" }),
  ).rejects.toBe(response);
});

it("@s34 rejects the complete mixed problem when one field is unknown", async () => {
  const previous = {
    configured: false,
    visibleFields: ["createdAt"],
    customFields: [],
    updatedAt: null,
    etag: '"customization:PROJECT:unconfigured"',
  };
  const response = Response.json(
    {
      type: "urn:organization:problem:validation_error",
      title: "Campos invalidos",
      status: 400,
      code: "VALIDATION_ERROR",
      errors: [
        {
          field: "label",
          code: "INVALID_VALUE",
          message: "Elige otra etiqueta.",
        },
        { field: "alien", code: "INVALID_VALUE", message: "Desconocido" },
      ],
    },
    { status: 400 },
  );
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(response));
  await expect(
    createCustomField("PROJECT", previous, { label: "Dato", type: "TEXT" }),
  ).rejects.toBe(response);
});
