// @ts-nocheck
import { CustomizationValidationError } from "./customization-api";
import { afterEach, expect, it, vi } from "vitest";
import { observeAccess } from "./api-client";
import { readCustomFields, saveCustomFields } from "./custom-fields-api";

afterEach(() => {
  vi.unstubAllGlobals();
  observeAccess();
});

const projectId = "12345678-1234-1234-1234-123456789abc";

it("@s10 partial reads absent project values and their compound revision", async () => {
  const body = { configured: false, values: [], updatedAt: null };
  const etag = `"custom-values:PROJECT:${projectId}:schema:unconfigured:values:unconfigured"`;
  const fetcher = vi
    .fn()
    .mockResolvedValue(Response.json(body, { headers: { ETag: etag } }));
  vi.stubGlobal("fetch", fetcher);
  await expect(readCustomFields(projectId)).resolves.toEqual({ ...body, etag });
  expect(fetcher).toHaveBeenCalledExactlyOnceWith(
    `/api/v1/projects/${projectId}/custom-fields`,
    expect.objectContaining({ credentials: "same-origin", cache: "no-store" }),
  );
});

it("@s27 rejects a compound tag for another entity", async () => {
  const etag =
    '"custom-values:PROJECT:22345678-1234-1234-1234-123456789abc:schema:unconfigured:values:unconfigured"';
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        Response.json(
          { configured: false, values: [], updatedAt: null },
          { headers: { ETag: etag } },
        ),
      ),
  );
  await expect(readCustomFields(projectId)).rejects.toThrow();
});

it("@s27 rejects additional fields in the values envelope", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        { configured: false, values: [], updatedAt: null, extra: true },
        {
          headers: {
            ETag: `"custom-values:PROJECT:${projectId}:schema:unconfigured:values:unconfigured"`,
          },
        },
      ),
    ),
  );
  await expect(readCustomFields(projectId)).rejects.toThrow();
});

it("@s27 rejects a malformed compound values revision", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        { configured: false, values: [], updatedAt: null },
        {
          headers: {
            ETag: `"custom-values:PROJECT:${projectId}:schema:unconfigured:values:broken"`,
          },
        },
      ),
    ),
  );
  await expect(readCustomFields(projectId)).rejects.toThrow();
});

it("@s10 reads a task's active null field with configured schema and absent values", async () => {
  const taskId = "22345678-1234-1234-1234-123456789abc";
  const body = {
    configured: false,
    values: [
      {
        fieldId: "32345678-1234-1234-1234-123456789abc",
        label: "Dato",
        type: "TEXT",
        value: null,
      },
    ],
    updatedAt: null,
  };
  const etag = `"custom-values:TASK:${taskId}:schema:42345678-1234-1234-1234-123456789abc:0:values:unconfigured"`;
  const fetcher = vi
    .fn()
    .mockResolvedValue(Response.json(body, { headers: { ETag: etag } }));
  vi.stubGlobal("fetch", fetcher);
  await expect(readCustomFields(projectId, taskId)).resolves.toEqual({
    ...body,
    etag,
  });
  expect(fetcher.mock.calls[0][0]).toBe(
    `/api/v1/projects/${projectId}/tasks/${taskId}/custom-fields`,
  );
});

it("@s27 rejects values whose configured flag is not boolean", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        { configured: "false", values: [], updatedAt: null },
        {
          headers: {
            ETag: `"custom-values:PROJECT:${projectId}:schema:unconfigured:values:unconfigured"`,
          },
        },
      ),
    ),
  );
  await expect(readCustomFields(projectId)).rejects.toThrow();
});

it("@s27 rejects absent values with a saved timestamp", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        { configured: false, values: [], updatedAt: "2026-09-07T10:00:00Z" },
        {
          headers: {
            ETag: `"custom-values:PROJECT:${projectId}:schema:unconfigured:values:unconfigured"`,
          },
        },
      ),
    ),
  );
  await expect(readCustomFields(projectId)).rejects.toThrow();
});

it("@s27 rejects configured values with an invalid timestamp", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        { configured: true, values: [], updatedAt: "invalid" },
        {
          headers: {
            ETag: `"custom-values:PROJECT:${projectId}:schema:unconfigured:values:22345678-1234-1234-1234-123456789abc:0"`,
          },
        },
      ),
    ),
  );
  await expect(readCustomFields(projectId)).rejects.toThrow();
});

it("@s27 rejects absent values carrying a configured values revision", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        { configured: false, values: [], updatedAt: null },
        {
          headers: {
            ETag: `"custom-values:PROJECT:${projectId}:schema:unconfigured:values:22345678-1234-1234-1234-123456789abc:0"`,
          },
        },
      ),
    ),
  );
  await expect(readCustomFields(projectId)).rejects.toThrow();
});

it("@s27 rejects a schema revision above BIGINT in a values snapshot", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        { configured: false, values: [], updatedAt: null },
        {
          headers: {
            ETag: `"custom-values:PROJECT:${projectId}:schema:22345678-1234-1234-1234-123456789abc:9223372036854775808:values:unconfigured"`,
          },
        },
      ),
    ),
  );
  await expect(readCustomFields(projectId)).rejects.toThrow();
});

it("@s27 rejects an unsupported type in a personal value", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        {
          configured: false,
          values: [
            {
              fieldId: "32345678-1234-1234-1234-123456789abc",
              label: "Dato",
              type: "FORMULA",
              value: null,
            },
          ],
          updatedAt: null,
        },
        {
          headers: {
            ETag: `"custom-values:PROJECT:${projectId}:schema:22345678-1234-1234-1234-123456789abc:0:values:unconfigured"`,
          },
        },
      ),
    ),
  );
  await expect(readCustomFields(projectId)).rejects.toThrow();
});

it("@s27 rejects a non-null value when no values row exists", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        {
          configured: false,
          values: [
            {
              fieldId: "32345678-1234-1234-1234-123456789abc",
              label: "Dato",
              type: "NUMBER",
              value: 0,
            },
          ],
          updatedAt: null,
        },
        {
          headers: {
            ETag: `"custom-values:PROJECT:${projectId}:schema:22345678-1234-1234-1234-123456789abc:0:values:unconfigured"`,
          },
        },
      ),
    ),
  );
  await expect(readCustomFields(projectId)).rejects.toThrow();
});

it("@s27 rejects active values when the schema is unconfigured", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        {
          configured: false,
          values: [
            {
              fieldId: "32345678-1234-1234-1234-123456789abc",
              label: "Dato",
              type: "TEXT",
              value: null,
            },
          ],
          updatedAt: null,
        },
        {
          headers: {
            ETag: `"custom-values:PROJECT:${projectId}:schema:unconfigured:values:unconfigured"`,
          },
        },
      ),
    ),
  );
  await expect(readCustomFields(projectId)).rejects.toThrow();
});

function savedValues(values: unknown[]) {
  return Response.json(
    { configured: true, values, updatedAt: "2026-09-07T10:00:00Z" },
    {
      headers: {
        ETag: `"custom-values:PROJECT:${projectId}:schema:22345678-1234-1234-1234-123456789abc:0:values:42345678-1234-1234-1234-123456789abc:0"`,
      },
    },
  );
}
const valueField = {
  fieldId: "32345678-1234-1234-1234-123456789abc",
  label: "Dato",
};

it("@s27 rejects NUMBER strings instead of coercing them", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        savedValues([{ ...valueField, type: "NUMBER", value: "0" }]),
      ),
  );
  await expect(readCustomFields(projectId)).rejects.toThrow();
});

it("@s27 rejects fractional NUMBER values", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        savedValues([{ ...valueField, type: "NUMBER", value: 1.25 }]),
      ),
  );
  await expect(readCustomFields(projectId)).rejects.toThrow();
});

it("@s27 preserves zero and the INTEGER boundary but rejects values above it", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValueOnce(
        savedValues([{ ...valueField, type: "NUMBER", value: 0 }]),
      )
      .mockResolvedValueOnce(
        savedValues([{ ...valueField, type: "NUMBER", value: 1000000000 }]),
      )
      .mockResolvedValueOnce(
        savedValues([{ ...valueField, type: "NUMBER", value: 1000000001 }]),
      ),
  );
  expect((await readCustomFields(projectId)).values[0].value).toBe(0);
  expect((await readCustomFields(projectId)).values[0].value).toBe(1000000000);
  await expect(readCustomFields(projectId)).rejects.toThrow();
});

it("@s27 rejects a values entry with an invalid label", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        savedValues([{ ...valueField, label: "", type: "TEXT", value: null }]),
      ),
  );
  await expect(readCustomFields(projectId)).rejects.toThrow();
});

it("@s27 rejects a values entry with an invalid field identity", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        savedValues([
          { ...valueField, fieldId: "invalid", type: "TEXT", value: null },
        ]),
      ),
  );
  await expect(readCustomFields(projectId)).rejects.toThrow();
});

it("@s27 rejects duplicated field IDs in values", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      savedValues([
        { ...valueField, type: "TEXT", value: null },
        { ...valueField, label: "Otro", type: "TEXT", value: null },
      ]),
    ),
  );
  await expect(readCustomFields(projectId)).rejects.toThrow();
});

it("@s27 rejects duplicated labels on distinct value identities", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      savedValues([
        { ...valueField, type: "TEXT", value: null },
        {
          ...valueField,
          fieldId: "52345678-1234-1234-1234-123456789abc",
          type: "TEXT",
          value: null,
        },
      ]),
    ),
  );
  await expect(readCustomFields(projectId)).rejects.toThrow();
});

it("@s27 preserves BOOLEAN false but rejects its string spelling", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValueOnce(
        savedValues([{ ...valueField, type: "BOOLEAN", value: false }]),
      )
      .mockResolvedValueOnce(
        savedValues([{ ...valueField, type: "BOOLEAN", value: "false" }]),
      ),
  );
  expect((await readCustomFields(projectId)).values[0].value).toBe(false);
  await expect(readCustomFields(projectId)).rejects.toThrow();
});

it("@s27 preserves a leap-day DATE but rejects an impossible civil date", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValueOnce(
        savedValues([{ ...valueField, type: "DATE", value: "2024-02-29" }]),
      )
      .mockResolvedValueOnce(
        savedValues([{ ...valueField, type: "DATE", value: "2025-02-29" }]),
      ),
  );
  expect((await readCustomFields(projectId)).values[0].value).toBe(
    "2024-02-29",
  );
  await expect(readCustomFields(projectId)).rejects.toThrow();
});

it("@s27 preserves TEXT whitespace but rejects an unnormalized empty string", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValueOnce(
        savedValues([{ ...valueField, type: "TEXT", value: "  \n " }]),
      )
      .mockResolvedValueOnce(
        savedValues([{ ...valueField, type: "TEXT", value: "" }]),
      ),
  );
  expect((await readCustomFields(projectId)).values[0].value).toBe("  \n ");
  await expect(readCustomFields(projectId)).rejects.toThrow();
});

it("@s27 applies the TEXT limit in code points without truncating emoji", async () => {
  const text = "\u{1f331}".repeat(1000);
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValueOnce(
        savedValues([{ ...valueField, type: "TEXT", value: text }]),
      )
      .mockResolvedValueOnce(
        savedValues([{ ...valueField, type: "TEXT", value: text + "x" }]),
      ),
  );
  expect((await readCustomFields(projectId)).values[0].value).toBe(text);
  await expect(readCustomFields(projectId)).rejects.toThrow();
});

it("@s27 rejects an invalid Unicode scalar in TEXT", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        savedValues([{ ...valueField, type: "TEXT", value: "Dato\ud800" }]),
      ),
  );
  await expect(readCustomFields(projectId)).rejects.toThrow();
});

it("@s27 rejects NUL inside TEXT", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        savedValues([{ ...valueField, type: "TEXT", value: "Dato\u0000" }]),
      ),
  );
  await expect(readCustomFields(projectId)).rejects.toThrow();
});

it("@s27 rejects more values than the twelve-definition limit", async () => {
  const values = Array.from({ length: 13 }, (_, index) => ({
    fieldId: `32345678-1234-1234-1234-${String(index).padStart(12, "0")}`,
    label: `Dato ${index}`,
    type: "TEXT",
    value: null,
  }));
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(savedValues(values)));
  await expect(readCustomFields(projectId)).rejects.toThrow();
});

it("@s20 preserves a failed values read as its original response", async () => {
  const response = Response.json(
    { code: "STORAGE_UNAVAILABLE" },
    { status: 503 },
  );
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(response));
  await expect(readCustomFields(projectId)).rejects.toBe(response);
});

it("@s37 discards an aborted values HTTP401 before handling access", async () => {
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
  const pending = readCustomFields(projectId, undefined, controller.signal);
  controller.abort();
  finish(new Response(null, { status: 401 }));
  await expect(pending).rejects.toMatchObject({ name: "AbortError" });
  expect(observer).not.toHaveBeenCalled();
});

it("@s37 discards values JSON completed after navigation aborts its read", async () => {
  let finish!: (body: unknown) => void;
  const response = savedValues([]);
  vi.spyOn(response, "json").mockImplementation(
    () =>
      new Promise((resolve) => {
        finish = resolve;
      }),
  );
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(response));
  const controller = new AbortController();
  const pending = readCustomFields(projectId, undefined, controller.signal);
  await vi.waitFor(() => expect(finish).toBeTypeOf("function"));
  controller.abort();
  finish({ configured: true, values: [], updatedAt: "2026-09-07T10:00:00Z" });
  await expect(pending).rejects.toMatchObject({ name: "AbortError" });
});

it("@s11 saves the complete values set and normalizes empty TEXT without losing zero", async () => {
  const entries = [
    { ...valueField, type: "NUMBER" as const, value: null },
    {
      fieldId: "52345678-1234-1234-1234-123456789abc",
      label: "Nota",
      type: "TEXT" as const,
      value: null,
    },
  ];
  const previous = {
    configured: false,
    values: entries,
    updatedAt: null,
    etag: `"custom-values:PROJECT:${projectId}:schema:22345678-1234-1234-1234-123456789abc:0:values:unconfigured"`,
  };
  const response = savedValues([{ ...entries[0], value: 0 }, entries[1]]);
  const fetcher = vi.fn().mockResolvedValue(response);
  vi.stubGlobal("fetch", fetcher);
  const result = await saveCustomFields(projectId, previous, [
    { fieldId: entries[0].fieldId, value: 0 },
    { fieldId: entries[1].fieldId, value: "" },
  ]);
  expect(result.values).toEqual([{ ...entries[0], value: 0 }, entries[1]]);
  expect(fetcher).toHaveBeenCalledTimes(1);
  const [url, options] = fetcher.mock.calls[0];
  expect(url).toBe(`/api/v1/projects/${projectId}/custom-fields`);
  expect(options).toMatchObject({
    method: "PUT",
    credentials: "same-origin",
    cache: "no-store",
  });
  expect(new Headers(options.headers).get("If-Match")).toBe(previous.etag);
  expect(JSON.parse(options.body)).toEqual({
    values: [
      { fieldId: entries[0].fieldId, value: 0 },
      { fieldId: entries[1].fieldId, value: null },
    ],
  });
});

it("@s28 rejects a values confirmation different from the captured input", async () => {
  const entry = { ...valueField, type: "NUMBER" as const, value: null };
  const previous = {
    configured: false,
    values: [entry],
    updatedAt: null,
    etag: `"custom-values:PROJECT:${projectId}:schema:22345678-1234-1234-1234-123456789abc:0:values:unconfigured"`,
  };
  const input = [{ fieldId: entry.fieldId, value: 1 }];
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
  const pending = saveCustomFields(projectId, previous, input);
  input[0].value = 2;
  finish(savedValues([{ ...entry, value: 2 }]));
  await expect(pending).rejects.toThrow();
});

it("@s28 rejects absence as confirmation of storing null values", async () => {
  const entry = { ...valueField, type: "TEXT" as const, value: null };
  const body = { configured: false, values: [entry], updatedAt: null };
  const etag = `"custom-values:PROJECT:${projectId}:schema:22345678-1234-1234-1234-123456789abc:0:values:unconfigured"`;
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(Response.json(body, { headers: { ETag: etag } })),
  );
  await expect(
    saveCustomFields(projectId, { ...body, etag }, [
      { fieldId: entry.fieldId, value: null },
    ]),
  ).rejects.toThrow();
});

it("@s28 rejects a values confirmation with a different schema revision", async () => {
  const entry = { ...valueField, type: "NUMBER" as const, value: null };
  const previous = {
    configured: false,
    values: [entry],
    updatedAt: null,
    etag: `"custom-values:PROJECT:${projectId}:schema:22345678-1234-1234-1234-123456789abc:0:values:unconfigured"`,
  };
  const response = savedValues([{ ...entry, value: 1 }]);
  response.headers.set(
    "ETag",
    response.headers.get("ETag")!.replace(":0:values:", ":1:values:"),
  );
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(response));
  await expect(
    saveCustomFields(projectId, previous, [
      { fieldId: entry.fieldId, value: 1 },
    ]),
  ).rejects.toThrow();
});

it("@s28 rejects a values confirmation that renames a field without a schema change", async () => {
  const entry = { ...valueField, type: "NUMBER" as const, value: null };
  const previous = {
    configured: false,
    values: [entry],
    updatedAt: null,
    etag: `"custom-values:PROJECT:${projectId}:schema:22345678-1234-1234-1234-123456789abc:0:values:unconfigured"`,
  };
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(savedValues([{ ...entry, label: "Otro", value: 1 }])),
  );
  await expect(
    saveCustomFields(projectId, previous, [
      { fieldId: entry.fieldId, value: 1 },
    ]),
  ).rejects.toThrow();
});

it("@s28 rejects changed values confirmed with the old values revision", async () => {
  const entry = { ...valueField, type: "NUMBER" as const, value: 0 };
  const etag = `"custom-values:PROJECT:${projectId}:schema:22345678-1234-1234-1234-123456789abc:0:values:42345678-1234-1234-1234-123456789abc:0"`;
  const previous = {
    configured: true,
    values: [entry],
    updatedAt: "2026-09-07T09:00:00Z",
    etag,
  };
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(savedValues([{ ...entry, value: 1 }])),
  );
  await expect(
    saveCustomFields(projectId, previous, [
      { fieldId: entry.fieldId, value: 1 },
    ]),
  ).rejects.toThrow();
});

it("@s14 accepts an unchanged values set sent in another order", async () => {
  const entries = [
    { ...valueField, type: "NUMBER" as const, value: 0 },
    {
      fieldId: "52345678-1234-1234-1234-123456789abc",
      label: "Bandera",
      type: "BOOLEAN" as const,
      value: false,
    },
  ];
  const response = savedValues(entries);
  const previous = {
    configured: true,
    values: entries,
    updatedAt: "2026-09-07T10:00:00Z",
    etag: response.headers.get("ETag")!,
  };
  const fetcher = vi.fn().mockResolvedValue(response);
  vi.stubGlobal("fetch", fetcher);
  const result = await saveCustomFields(projectId, previous, [
    { fieldId: entries[1].fieldId, value: false },
    { fieldId: entries[0].fieldId, value: 0 },
  ]);
  expect(result).toEqual(previous);
  expect(
    JSON.parse(fetcher.mock.calls[0][1].body).values.map(
      (entry: { fieldId: string }) => entry.fieldId,
    ),
  ).toEqual([entries[1].fieldId, entries[0].fieldId]);
});

it("@s34 preserves a values error index in the sent order", async () => {
  const entries = [
    { ...valueField, type: "NUMBER" as const, value: null },
    {
      fieldId: "52345678-1234-1234-1234-123456789abc",
      label: "Nota",
      type: "TEXT" as const,
      value: null,
    },
  ];
  const previous = {
    configured: false,
    values: entries,
    updatedAt: null,
    etag: `"custom-values:PROJECT:${projectId}:schema:22345678-1234-1234-1234-123456789abc:0:values:unconfigured"`,
  };
  const response = Response.json(
    {
      type: "urn:organization:problem:validation_error",
      title: "Campos invalidos",
      status: 400,
      code: "VALIDATION_ERROR",
      errors: [
        {
          field: "values[0].value",
          code: "INVALID_VALUE",
          message: "Revisa este valor.",
        },
      ],
    },
    { status: 400 },
  );
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(response));
  await expect(
    saveCustomFields(projectId, previous, [
      { fieldId: entries[1].fieldId, value: "Nota" },
      { fieldId: entries[0].fieldId, value: 1 },
    ]),
  ).rejects.toMatchObject({
    constructor: CustomizationValidationError,
    errors: [{ field: "values[0].value", message: "Revisa este valor." }],
  });
});
