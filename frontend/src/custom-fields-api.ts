import {
  verifyPrivateRevision,
  throwCustomizationValidation,
} from "./customization-api";
import type { CustomFieldType } from "./customization-api";
import {
  isCustomFieldId,
  isCustomFieldLabel,
  isCustomFieldType,
} from "./customization-api";
import { exact, instant } from "./schedule-block-api";
import { apiRequest } from "./api-client";

export type CustomFieldValue = string | number | boolean | null;
export type CustomValueEntry = {
  fieldId: string;
  label: string;
  type: CustomFieldType;
  value: CustomFieldValue;
};
export type CustomValuesSnapshot = {
  configured: boolean;
  values: CustomValueEntry[];
  updatedAt: string | null;
  etag: string;
};

export async function readCustomFields(
  projectId: string,
  taskId?: string,
  signal?: AbortSignal,
): Promise<CustomValuesSnapshot> {
  const response = await apiRequest(
    `/api/v1/projects/${projectId}${taskId ? `/tasks/${taskId}` : ""}/custom-fields`,
    { credentials: "same-origin", cache: "no-store", signal },
  );
  return decodeCustomFields(response, projectId, taskId, signal);
}

async function decodeCustomFields(
  response: Response,
  projectId: string,
  taskId?: string,
  signal?: AbortSignal,
): Promise<CustomValuesSnapshot> {
  if (signal?.aborted)
    throw new DOMException("Consulta retirada", "AbortError");
  if (response.status !== 200) throw response;
  const body = await response.json();
  if (signal?.aborted)
    throw new DOMException("Consulta retirada", "AbortError");
  if (!exact(body, "configured values updatedAt"))
    throw new Error("Respuesta incompatible");
  if (typeof body.configured !== "boolean")
    throw new Error("Respuesta incompatible");
  if (!body.configured && body.updatedAt !== null)
    throw new Error("Respuesta incompatible");
  if (body.configured && !instant(body.updatedAt))
    throw new Error("Respuesta incompatible");
  const etag = response.headers.get("ETag");
  if (
    !etag?.startsWith(
      `"custom-values:${taskId ? "TASK" : "PROJECT"}:${taskId ?? projectId}:schema:`,
    )
  )
    throw new Error("Respuesta incompatible");
  const revision =
    "(?:unconfigured|[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}:(?:0|[1-9][0-9]*))";
  if (
    !new RegExp(
      `^"custom-values:(PROJECT|TASK):[0-9a-f-]+:schema:${revision}:values:${revision}"$`,
    ).test(etag)
  )
    throw new Error("Respuesta incompatible");
  if (body.configured === etag.endsWith(':values:unconfigured"'))
    throw new Error("Respuesta incompatible");
  for (const match of etag.matchAll(/:([0-9]+)(?=:values:|"$)/g)) {
    const version = match[1];
    if (
      version.length > 19 ||
      (version.length === 19 && version > "9223372036854775807")
    )
      throw new Error("Respuesta incompatible");
  }
  if (
    !Array.isArray(body.values) ||
    !body.values.every(
      (entry) =>
        exact(entry, "fieldId label type value") &&
        isCustomFieldType(entry.type),
    )
  )
    throw new Error("Respuesta incompatible");
  if (!body.configured && body.values.some((entry) => entry.value !== null))
    throw new Error("Respuesta incompatible");
  if (etag.includes(":schema:unconfigured:") && body.values.length !== 0)
    throw new Error("Respuesta incompatible");
  if (
    body.values.some(
      (entry) =>
        entry.value !== null &&
        entry.type === "NUMBER" &&
        (!Number.isInteger(entry.value) || Math.abs(entry.value) > 1000000000),
    )
  )
    throw new Error("Respuesta incompatible");
  if (!body.values.every((entry) => isCustomFieldLabel(entry.label)))
    throw new Error("Respuesta incompatible");
  if (!body.values.every((entry) => isCustomFieldId(entry.fieldId)))
    throw new Error("Respuesta incompatible");
  if (
    new Set(body.values.map((entry) => entry.fieldId)).size !==
    body.values.length
  )
    throw new Error("Respuesta incompatible");
  if (
    new Set(body.values.map((entry) => entry.label)).size !== body.values.length
  )
    throw new Error("Respuesta incompatible");
  if (
    body.values.some(
      (entry) =>
        entry.value !== null &&
        entry.type === "BOOLEAN" &&
        typeof entry.value !== "boolean",
    )
  )
    throw new Error("Respuesta incompatible");
  if (
    body.values.some(
      (entry) =>
        entry.value !== null &&
        entry.type === "DATE" &&
        (typeof entry.value !== "string" ||
          !/^[0-9]{4}-[0-9]{2}-[0-9]{2}$/.test(entry.value) ||
          !instant(`${entry.value}T00:00:00Z`)),
    )
  )
    throw new Error("Respuesta incompatible");
  if (
    body.values.some(
      (entry) =>
        entry.value !== null &&
        entry.type === "TEXT" &&
        (typeof entry.value !== "string" ||
          entry.value.length === 0 ||
          [...entry.value].length > 1000 ||
          /[\ud800-\udfff]/u.test(entry.value) ||
          entry.value.includes(String.fromCharCode(0))),
    )
  )
    throw new Error("Respuesta incompatible");
  if (body.values.length > 12) throw new Error("Respuesta incompatible");
  return {
    configured: body.configured,
    values: body.values as CustomValueEntry[],
    updatedAt: body.updatedAt as string | null,
    etag,
  };
}

export async function saveCustomFields(
  projectId: string,
  previous: CustomValuesSnapshot,
  input: { fieldId: string; value: CustomFieldValue }[],
  taskId?: string,
  signal?: AbortSignal,
): Promise<CustomValuesSnapshot> {
  const retained = previous.values.map((entry) => ({ ...entry }));
  const values = input.map((entry) => ({
    fieldId: entry.fieldId,
    value:
      entry.value === "" &&
      previous.values.find((field) => field.fieldId === entry.fieldId)?.type ===
        "TEXT"
        ? null
        : entry.value,
  }));
  const schema = previous.etag.split(":values:")[0];
  const transition = {
    configured: previous.configured,
    etag: previous.etag,
    updatedAt: previous.updatedAt,
    changed: retained.some(
      (entry) =>
        values.find((sent) => sent.fieldId === entry.fieldId)?.value !==
        entry.value,
    ),
  };
  const response = await apiRequest(
    `/api/v1/projects/${projectId}${taskId ? `/tasks/${taskId}` : ""}/custom-fields`,
    {
      method: "PUT",
      credentials: "same-origin",
      cache: "no-store",
      signal,
      headers: {
        "Content-Type": "application/json",
        "If-Match": previous.etag,
      },
      body: JSON.stringify({ values }),
    },
  );
  if (signal?.aborted)
    throw new DOMException("Consulta retirada", "AbortError");
  if (response.status === 400)
    await throwCustomizationValidation(
      response,
      (field) => {
        if (field === "values") return true;
        const match = /^values\[(0|[1-9][0-9]*)\]\.(fieldId|value)$/.exec(
          field,
        );
        return match !== null && Number(match[1]) < values.length;
      },
      signal,
    );
  const result = await decodeCustomFields(response, projectId, taskId, signal);
  if (result.etag.split(":values:")[0] !== schema)
    throw new Error("Respuesta incompatible");
  if (!result.configured) throw new Error("Respuesta incompatible");
  if (
    result.values.length !== values.length ||
    !result.values.every((entry) =>
      values.some(
        (sent) => sent.fieldId === entry.fieldId && sent.value === entry.value,
      ),
    )
  )
    throw new Error("Respuesta incompatible");
  if (
    result.values.length !== retained.length ||
    !retained.every((entry, index) => {
      const actual = result.values[index];
      return (
        actual.fieldId === entry.fieldId &&
        actual.label === entry.label &&
        actual.type === entry.type
      );
    })
  )
    throw new Error("Respuesta incompatible");
  verifyPrivateRevision(transition, result);
  return result;
}
