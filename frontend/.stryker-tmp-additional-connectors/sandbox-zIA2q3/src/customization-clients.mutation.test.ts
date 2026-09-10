// @ts-nocheck
import { afterEach, expect, it, vi } from "vitest";
import {
  createCustomField,
  updateCustomField,
  CustomizationValidationError,
  type CustomizationSnapshot,
} from "./customization-api";
import {
  readCustomFields,
  saveCustomFields,
  type CustomValuesSnapshot,
} from "./custom-fields-api";

const projectId = "12345678-1234-1234-1234-123456789abc";
const taskId = "22345678-1234-1234-1234-123456789abc";
const fieldId = "32345678-1234-1234-1234-123456789abc";
const otherId = "42345678-1234-1234-1234-123456789abc";
const schemaId = "52345678-1234-1234-1234-123456789abc";
const timestamp = "2026-09-07T10:00:00Z";
const later = "2026-09-07T10:00:01Z";
function configuration(): CustomizationSnapshot {
  return {
    configured: true,
    visibleFields: [],
    customFields: [
      { id: fieldId, label: "Nota", type: "TEXT", active: true },
      { id: otherId, label: "Archivado", type: "NUMBER", active: false },
    ],
    updatedAt: timestamp,
    etag: `"customization:PROJECT:${schemaId}:7"`,
  };
}
function response(body: object, etag: string) {
  return Response.json(body, { headers: { ETag: etag } });
}
afterEach(() => {
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});

it("@s28 accepts creation over existing definitions without losing inactive metadata", async () => {
  const previous = configuration();
  const body = {
    configured: true,
    visibleFields: [],
    customFields: [
      ...previous.customFields,
      { id: taskId, label: "Fecha", type: "DATE", active: true },
    ],
    updatedAt: later,
  };
  const etag = `"customization:PROJECT:${schemaId}:8"`;
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(response(body, etag)));
  await expect(
    createCustomField("PROJECT", previous, { label: "Fecha", type: "DATE" }),
  ).resolves.toEqual({ ...body, etag });
});
it("@s28 rejects an update receipt that changes an untouched definition identity", async () => {
  const previous = configuration();
  const body = {
    configured: true,
    visibleFields: [],
    customFields: [
      { ...previous.customFields[0], label: "Nueva" },
      { ...previous.customFields[1], id: taskId },
    ],
    updatedAt: later,
  };
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        response(body, `"customization:PROJECT:${schemaId}:8"`),
      ),
  );
  await expect(
    updateCustomField("PROJECT", previous, fieldId, {
      label: "Nueva",
      active: true,
    }),
  ).rejects.toThrow("Respuesta incompatible");
});

it("@s28 rejects an otherwise correct field update acknowledged at the old revision", async () => {
  const previous = configuration();
  const body = {
    configured: true,
    visibleFields: [],
    customFields: [
      { ...previous.customFields[0], label: "Nueva" },
      previous.customFields[1],
    ],
    updatedAt: later,
  };
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(response(body, previous.etag)),
  );
  await expect(
    updateCustomField("PROJECT", previous, fieldId, {
      label: "Nueva",
      active: true,
    }),
  ).rejects.toThrow("Respuesta incompatible");
});

function taskValues(): CustomValuesSnapshot {
  return {
    configured: true,
    values: [
      { fieldId, label: "Nota", type: "TEXT", value: "Antes" },
      { fieldId: otherId, label: "Hecho", type: "BOOLEAN", value: false },
    ],
    updatedAt: timestamp,
    etag: `"custom-values:TASK:${taskId}:schema:${schemaId}:7:values:${projectId}:3"`,
  };
}
it("@s11 saves both task values through the task URL with its compound precondition", async () => {
  const previous = taskValues();
  const input = [
    { fieldId, value: "Después" },
    { fieldId: otherId, value: true },
  ];
  const body = {
    configured: true,
    values: [
      { ...previous.values[0], value: "Después" },
      { ...previous.values[1], value: true },
    ],
    updatedAt: later,
  };
  const etag = previous.etag.replace(':3"', ':4"');
  const fetcher = vi.fn().mockResolvedValue(response(body, etag));
  vi.stubGlobal("fetch", fetcher);
  await expect(
    saveCustomFields(projectId, previous, input, taskId),
  ).resolves.toEqual({ ...body, etag });
  expect(fetcher).toHaveBeenCalledExactlyOnceWith(
    `/api/v1/projects/${projectId}/tasks/${taskId}/custom-fields`,
    expect.objectContaining({
      method: "PUT",
      credentials: "same-origin",
      cache: "no-store",
    }),
  );
  const options = fetcher.mock.calls[0][1];
  expect(new Headers(options.headers).get("If-Match")).toBe(previous.etag);
  expect(JSON.parse(options.body)).toEqual({ values: input });
});

it("@s28 rejects a two-value confirmation that only saved the first submitted value", async () => {
  const previous = taskValues();
  const body = {
    configured: true,
    values: [{ ...previous.values[0], value: "Después" }, previous.values[1]],
    updatedAt: later,
  };
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(response(body, previous.etag.replace(':3"', ':4"'))),
  );
  await expect(
    saveCustomFields(
      projectId,
      previous,
      [
        { fieldId, value: "Después" },
        { fieldId: otherId, value: true },
      ],
      taskId,
    ),
  ).rejects.toThrow("Respuesta incompatible");
});

it("@s28 rejects a coherent values receipt that silently changes a field type", async () => {
  const previous = taskValues();
  const body = {
    configured: true,
    values: [
      { ...previous.values[0], type: "DATE", value: "2026-09-07" },
      previous.values[1],
    ],
    updatedAt: later,
  };
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(response(body, previous.etag.replace(':3"', ':4"'))),
  );
  await expect(
    saveCustomFields(
      projectId,
      previous,
      [
        { fieldId, value: "2026-09-07" },
        { fieldId: otherId, value: false },
      ],
      taskId,
    ),
  ).rejects.toThrow("Respuesta incompatible");
});

it("@s27 preserves nullable BOOLEAN and DATE values at the maximum compound revision", async () => {
  const values = [
    { fieldId, label: "Hecho", type: "BOOLEAN", value: null },
    { fieldId: otherId, label: "Fecha", type: "DATE", value: null },
  ];
  const body = { configured: true, values, updatedAt: timestamp };
  const etag = `"custom-values:TASK:${taskId}:schema:${schemaId}:9223372036854775807:values:${projectId}:9223372036854775807"`;
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(response(body, etag)));
  await expect(readCustomFields(projectId, taskId)).resolves.toEqual({
    ...body,
    etag,
  });
});

it("@s27 accepts exactly twelve distinct active values without truncation", async () => {
  const values = Array.from({ length: 12 }, (_, index) => ({
    fieldId: `32345678-1234-1234-1234-${String(index).padStart(12, "0")}`,
    label: `Dato ${index}`,
    type: "NUMBER",
    value: index,
  }));
  const body = { configured: true, values, updatedAt: timestamp };
  const etag = taskValues().etag;
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(response(body, etag)));
  await expect(readCustomFields(projectId, taskId)).resolves.toEqual({
    ...body,
    etag,
  });
});

function validationProblem() {
  return {
    type: "urn:organization:problem:validation_error",
    title: "Revisa los campos",
    status: 400,
    code: "VALIDATION_ERROR",
    errors: [
      {
        field: "values[0].value",
        code: "INVALID_VALUE",
        message: "Revisa la nota",
      },
      {
        field: "values[1].value",
        code: "INVALID_TYPE",
        message: "Revisa el booleano",
      },
    ],
  };
}
it("@s34 preserves both indexed corrections from one coherent validation problem", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(Response.json(validationProblem(), { status: 400 })),
  );
  const pending = saveCustomFields(
    projectId,
    taskValues(),
    [
      { fieldId, value: "Después" },
      { fieldId: otherId, value: true },
    ],
    taskId,
  );
  await expect(pending).rejects.toBeInstanceOf(CustomizationValidationError);
  await expect(pending).rejects.toMatchObject({
    errors: [
      { field: "values[0].value", message: "Revisa la nota" },
      { field: "values[1].value", message: "Revisa el booleano" },
    ],
  });
});

it("@s34 preserves the entire failed response when one correction has an extra member", async () => {
  const problem = validationProblem();
  const failure = Response.json(
    {
      ...problem,
      errors: [problem.errors[0], { ...problem.errors[1], extra: true }],
    },
    { status: 400 },
  );
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(failure));
  await expect(
    saveCustomFields(
      projectId,
      taskValues(),
      [
        { fieldId, value: "Después" },
        { fieldId: otherId, value: true },
      ],
      taskId,
    ),
  ).rejects.toBe(failure);
});

it("@s34 rejects a correction index at the submitted array's exclusive upper bound", async () => {
  const problem = validationProblem();
  const failure = Response.json(
    {
      ...problem,
      errors: [
        problem.errors[0],
        { ...problem.errors[1], field: "values[2].value" },
      ],
    },
    { status: 400 },
  );
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(failure));
  await expect(
    saveCustomFields(
      projectId,
      taskValues(),
      [
        { fieldId, value: "Después" },
        { fieldId: otherId, value: true },
      ],
      taskId,
    ),
  ).rejects.toBe(failure);
});

it("@s37 discards validation JSON completed after its task write is aborted", async () => {
  let finish!: (value: unknown) => void;
  const failure = Response.json(validationProblem(), { status: 400 });
  const clone = failure.clone();
  vi.spyOn(failure, "clone").mockReturnValue(clone);
  vi.spyOn(clone, "json").mockImplementation(
    () =>
      new Promise((resolve) => {
        finish = resolve;
      }),
  );
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(failure));
  const controller = new AbortController();
  const pending = saveCustomFields(
    projectId,
    taskValues(),
    [
      { fieldId, value: "Después" },
      { fieldId: otherId, value: true },
    ],
    taskId,
    controller.signal,
  );
  await vi.waitFor(() => expect(finish).toBeTypeOf("function"));
  controller.abort();
  finish(validationProblem());
  await expect(pending).rejects.toMatchObject({ name: "AbortError" });
});

it("@s27 rejects a twenty-digit values revision before treating it as a valid snapshot", async () => {
  const previous = taskValues();
  const { etag, ...body } = previous;
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        response(body, etag.replace(':3"', ':10000000000000000000"')),
      ),
  );
  await expect(readCustomFields(projectId, taskId)).rejects.toThrow(
    "Respuesta incompatible",
  );
});
