import { exact, instant } from "./schedule-block-api";

import { apiRequest } from "./api-client";

export class CustomizationValidationError extends Error {
  constructor(public readonly errors: { field: string; message: string }[]) {
    super("Revisa los campos");
  }
}

export type CustomizationScope = "PROJECT" | "TASK";
export type CustomFieldType = "TEXT" | "NUMBER" | "DATE" | "BOOLEAN";
export type CustomField = {
  id: string;
  label: string;
  type: CustomFieldType;
  active: boolean;
};
export type CustomizationSnapshot = {
  configured: boolean;
  visibleFields: string[];
  customFields: CustomField[];
  updatedAt: string | null;
  etag: string;
};

export async function readCustomization(
  scope: CustomizationScope,
  signal?: AbortSignal,
): Promise<CustomizationSnapshot> {
  const response = await apiRequest(`/api/v1/me/customization/${scope}`, {
    credentials: "same-origin",
    cache: "no-store",
    signal,
  });

  return decodeCustomization(response, scope, signal);
}

async function decodeCustomization(
  response: Response,
  scope: CustomizationScope,
  signal?: AbortSignal,
): Promise<CustomizationSnapshot> {
  if (signal?.aborted)
    throw new DOMException("Consulta retirada", "AbortError");
  if (response.status === 400) {
    const problem: unknown = await response
      .clone()
      .json()
      .catch(() => null);
    if (signal?.aborted)
      throw new DOMException("Consulta retirada", "AbortError");
    if (
      exact(problem, "type title status code errors") &&
      problem.type === "urn:organization:problem:validation_error" &&
      problem.status === 400 &&
      problem.code === "VALIDATION_ERROR" &&
      typeof problem.title === "string" &&
      problem.title.trim() &&
      Array.isArray(problem.errors) &&
      problem.errors.length > 0 &&
      problem.errors.every(
        (error) =>
          exact(error, "field code message") &&
          typeof error.field === "string" &&
          /^(label|type|active|customFields|visibleFields(?:\[[0-9]+\])?)$/.test(
            error.field,
          ) &&
          [
            "REQUIRED",
            "UNKNOWN_FIELD",
            "INVALID_TYPE",
            "INVALID_VALUE",
            "INVALID_FORMAT",
          ].includes(error.code as string) &&
          typeof error.message === "string" &&
          error.message.trim(),
      )
    ) {
      throw new CustomizationValidationError(
        problem.errors.map((error) => ({
          field: error.field,
          message: error.message,
        })),
      );
    }
  }
  if (response.status !== 200) throw response;

  const body = await response.json();
  if (signal?.aborted)
    throw new DOMException("Consulta retirada", "AbortError");

  if (!exact(body, "configured visibleFields customFields updatedAt"))
    throw new Error("Respuesta incompatible");

  if (typeof body.configured !== "boolean")
    throw new Error("Respuesta incompatible");
  if (body.configured && !instant(body.updatedAt))
    throw new Error("Respuesta incompatible");
  const etag = response.headers.get("ETag");

  if (
    body.configured === false &&
    etag !== `"customization:${scope}:unconfigured"`
  )
    throw new Error("Respuesta incompatible");

  if (
    body.configured &&
    (etag === null ||
      !new RegExp(
        `^"customization:${scope}:[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}:(0|[1-9][0-9]*)"$`,
      ).test(etag))
  )
    throw new Error("Respuesta incompatible");
  if (
    body.configured &&
    BigInt(etag!.slice(etag!.lastIndexOf(":") + 1, -1)) > 9223372036854775807n
  )
    throw new Error("Respuesta incompatible");
  const defaults =
    scope === "PROJECT"
      ? ["createdAt"]
      : ["completionCriterion", "estimatedMinutes"];

  if (
    body.configured === false &&
    JSON.stringify(body.visibleFields) !== JSON.stringify(defaults)
  )
    throw new Error("Respuesta incompatible");

  if (body.configured === false && body.updatedAt !== null)
    throw new Error("Respuesta incompatible");
  const allowed =
    scope === "PROJECT"
      ? ["createdAt", "updatedAt"]
      : ["completionCriterion", "estimatedMinutes", "createdAt", "updatedAt"];
  if (
    !Array.isArray(body.visibleFields) ||
    !body.visibleFields.every((field) => allowed.includes(field))
  )
    throw new Error("Respuesta incompatible");
  if (new Set(body.visibleFields).size !== body.visibleFields.length)
    throw new Error("Respuesta incompatible");
  if (
    !Array.isArray(body.customFields) ||
    (!body.configured && body.customFields.length !== 0)
  )
    throw new Error("Respuesta incompatible");
  if (
    !body.customFields.every(
      (field) =>
        exact(field, "id label type active") &&
        ["TEXT", "NUMBER", "DATE", "BOOLEAN"].includes(field.type as string),
    )
  )
    throw new Error("Respuesta incompatible");
  if (
    !body.customFields.every(
      (field) =>
        typeof field.label === "string" &&
        field.label.length > 0 &&
        [...field.label].length <= 60 &&
        !/^\p{White_Space}|\p{White_Space}$/u.test(field.label) &&
        !field.label.includes(String.fromCharCode(0)) &&
        !/[\ud800-\udfff]/u.test(field.label),
    )
  )
    throw new Error("Respuesta incompatible");
  if (
    !body.customFields.every(
      (field) =>
        typeof field.id === "string" &&
        /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/.test(
          field.id,
        ),
    )
  )
    throw new Error("Respuesta incompatible");
  if (!body.customFields.every((field) => typeof field.active === "boolean"))
    throw new Error("Respuesta incompatible");
  if (
    new Set(body.customFields.map((field) => field.id)).size !==
    body.customFields.length
  )
    throw new Error("Respuesta incompatible");
  if (
    new Set(body.customFields.map((field) => field.label)).size !==
    body.customFields.length
  )
    throw new Error("Respuesta incompatible");
  if (body.customFields.length > 12) throw new Error("Respuesta incompatible");
  return {
    configured: body.configured,
    visibleFields: body.visibleFields,
    customFields: body.customFields as CustomField[],
    updatedAt: body.updatedAt as string | null,
    etag: etag!,
  };
}

export async function saveCustomizationView(
  scope: CustomizationScope,
  previous: CustomizationSnapshot,
  visibleFields: string[],
  signal?: AbortSignal,
): Promise<CustomizationSnapshot> {
  const sent = [...visibleFields];
  const retained = previous.customFields.map((field) => ({ ...field }));
  const response = await apiRequest(`/api/v1/me/customization/${scope}`, {
    method: "PUT",
    credentials: "same-origin",
    cache: "no-store",
    signal,
    headers: { "Content-Type": "application/json", "If-Match": previous.etag },
    body: JSON.stringify({ visibleFields: sent }),
  });
  const result = await decodeCustomization(response, scope, signal);
  if (!result.configured) throw new Error("Respuesta incompatible");
  if (JSON.stringify(result.visibleFields) !== JSON.stringify(sent))
    throw new Error("Respuesta incompatible");
  if (!sameFields(result.customFields, retained))
    throw new Error("Respuesta incompatible");
  return result;
}

export async function createCustomField(
  scope: CustomizationScope,
  previous: CustomizationSnapshot,
  input: { label: string; type: CustomFieldType },
  signal?: AbortSignal,
): Promise<CustomizationSnapshot> {
  const sent = {
    label: input.label.replace(/^\p{White_Space}+|\p{White_Space}+$/gu, ""),
    type: input.type,
  };
  const retained = previous.customFields.map((field) => ({ ...field }));
  const retainedView = [...previous.visibleFields];
  const existing = retained.map((field) => field.id);
  const response = await apiRequest(
    `/api/v1/me/customization/${scope}/fields`,
    {
      method: "POST",
      credentials: "same-origin",
      cache: "no-store",
      signal,
      headers: {
        "Content-Type": "application/json",
        "If-Match": previous.etag,
      },
      body: JSON.stringify(sent),
    },
  );
  const result = await decodeCustomization(response, scope, signal);
  const added = result.customFields.find(
    (field) => !existing.includes(field.id),
  );
  if (
    !added ||
    added.label !== sent.label ||
    added.type !== sent.type ||
    !added.active
  )
    throw new Error("Respuesta incompatible");
  const remaining = result.customFields.filter(
    (field) => field.id !== added.id,
  );
  if (!sameFields(remaining, retained))
    throw new Error("Respuesta incompatible");
  if (JSON.stringify(result.visibleFields) !== JSON.stringify(retainedView))
    throw new Error("Respuesta incompatible");
  return result;
}

export async function updateCustomField(
  scope: CustomizationScope,
  previous: CustomizationSnapshot,
  fieldId: string,
  input: { label: string; active: boolean },
  signal?: AbortSignal,
): Promise<CustomizationSnapshot> {
  const sent = {
    label: input.label.replace(/^\p{White_Space}+|\p{White_Space}+$/gu, ""),
    active: input.active,
  };
  const retainedView = [...previous.visibleFields];
  const expected = previous.customFields.map((field) =>
    field.id === fieldId ? { ...field, ...sent } : { ...field },
  );
  const response = await apiRequest(
    `/api/v1/me/customization/${scope}/fields/${fieldId}`,
    {
      method: "PUT",
      credentials: "same-origin",
      cache: "no-store",
      signal,
      headers: {
        "Content-Type": "application/json",
        "If-Match": previous.etag,
      },
      body: JSON.stringify(sent),
    },
  );
  const result = await decodeCustomization(response, scope, signal);
  if (!sameFields(result.customFields, expected))
    throw new Error("Respuesta incompatible");
  if (JSON.stringify(result.visibleFields) !== JSON.stringify(retainedView))
    throw new Error("Respuesta incompatible");
  return result;
}

function sameFields(actual: CustomField[], expected: CustomField[]) {
  return (
    actual.length === expected.length &&
    expected.every((field, index) => {
      const current = actual[index];
      return (
        current.id === field.id &&
        current.label === field.label &&
        current.type === field.type &&
        current.active === field.active
      );
    })
  );
}
