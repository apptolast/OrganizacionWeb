// @ts-nocheck
import { microseconds } from "./work-session-api";
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
    await throwCustomizationValidation(
      response,
      (field) =>
        /^(label|type|active|customFields|visibleFields(?:\[[0-9]+\])?)$/.test(
          field,
        ),
      signal,
    );
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
        exact(field, "id label type active") && isCustomFieldType(field.type),
    )
  )
    throw new Error("Respuesta incompatible");
  if (
    !body.customFields.every(
      (field) => isCustomFieldLabel(field.label) && isCustomFieldId(field.id),
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
  const transition = {
    ...previous,
    changed: JSON.stringify(sent) !== JSON.stringify(previous.visibleFields),
  };
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
  verifyPrivateRevision(transition, result);
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
  const transition = { ...previous, changed: true };
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
  verifyPrivateRevision(transition, result);
  return result;
}

export async function updateCustomField(
  scope: CustomizationScope,
  previous: CustomizationSnapshot,
  fieldId: string,
  input: { label: string; active: boolean },
  signal?: AbortSignal,
): Promise<CustomizationSnapshot> {
  if (!previous.customFields.some((field) => field.id === fieldId))
    throw new Error("Campo no disponible");
  const sent = {
    label: input.label.replace(/^\p{White_Space}+|\p{White_Space}+$/gu, ""),
    active: input.active,
  };
  const retainedView = [...previous.visibleFields];
  const expected = previous.customFields.map((field) =>
    field.id === fieldId ? { ...field, ...sent } : { ...field },
  );
  const transition = {
    ...previous,
    changed: !sameFields(expected, previous.customFields),
  };
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
  verifyPrivateRevision(transition, result);
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

export function verifyPrivateRevision(
  previous: Pick<CustomizationSnapshot, "configured" | "etag" | "updatedAt"> & {
    changed: boolean;
  },
  result: Pick<CustomizationSnapshot, "configured" | "etag" | "updatedAt">,
) {
  if (!previous.configured && !result.etag.endsWith(':0"'))
    throw new Error("Respuesta incompatible");
  if (
    previous.configured &&
    !previous.changed &&
    (result.etag !== previous.etag || result.updatedAt !== previous.updatedAt)
  )
    throw new Error("Respuesta incompatible");
  if (previous.configured && previous.changed) {
    if (microseconds(result.updatedAt)! < microseconds(previous.updatedAt)!)
      throw new Error("Respuesta incompatible");
    const separator = previous.etag.lastIndexOf(":");
    const next = BigInt(previous.etag.slice(separator + 1, -1)) + 1n;
    if (result.etag !== `${previous.etag.slice(0, separator + 1)}${next}"`)
      throw new Error("Respuesta incompatible");
  }
}

export function isCustomFieldLabel(value: unknown): value is string {
  return (
    typeof value === "string" &&
    value.length > 0 &&
    [...value].length <= 60 &&
    !/^\p{White_Space}|\p{White_Space}$/u.test(value) &&
    !value.includes(String.fromCharCode(0)) &&
    !/[\ud800-\udfff]/u.test(value)
  );
}
export function isCustomFieldId(value: unknown): value is string {
  return (
    typeof value === "string" &&
    /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/.test(value)
  );
}
export function isCustomFieldType(value: unknown): value is CustomFieldType {
  return ["TEXT", "NUMBER", "DATE", "BOOLEAN"].includes(value as string);
}

export async function throwCustomizationValidation(
  response: Response,
  acceptField: (field: string) => boolean,
  signal?: AbortSignal,
) {
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
          acceptField(error.field) &&
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
}
