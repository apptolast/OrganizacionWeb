import { apiRequest } from "./api-client";
export const DAY_KEYS = [
  "MONDAY",
  "TUESDAY",
  "WEDNESDAY",
  "THURSDAY",
  "FRIDAY",
  "SATURDAY",
  "SUNDAY",
] as const;
export type DayKey = (typeof DAY_KEYS)[number];
export type DailyMinutes = Record<DayKey, number>;
export type AvailabilityInput = { zoneId: string; dailyMinutes: DailyMinutes };
export type AvailabilitySnapshot = (
  | { configured: false; zoneId: null; dailyMinutes: null; updatedAt: null }
  | {
      configured: true;
      zoneId: string;
      dailyMinutes: DailyMinutes;
      updatedAt: string;
    }
) & { etag: string };
export async function readAvailability(
  signal?: AbortSignal,
): Promise<AvailabilitySnapshot> {
  const response = await apiRequest("/api/v1/me/availability", {
    credentials: "same-origin",
    cache: "no-store",
    signal,
  });
  return snapshot(response);
}
async function snapshot(response: Response): Promise<AvailabilitySnapshot> {
  if (response.status !== 200) throw response;
  const data: unknown = await response.json(),
    etag = response.headers.get("ETag");
  if (
    isRecord(data) &&
    Object.keys(data).length === 4 &&
    data.configured === true &&
    typeof data.zoneId === "string" &&
    data.zoneId.length > 0 &&
    isDailyMinutes(data.dailyMinutes) &&
    isInstant(data.updatedAt) &&
    isConfiguredTag(etag)
  )
    return {
      configured: true,
      zoneId: data.zoneId,
      dailyMinutes: data.dailyMinutes,
      updatedAt: data.updatedAt,
      etag,
    };
  if (
    !isRecord(data) ||
    Object.keys(data).length !== 4 ||
    data.configured !== false ||
    data.zoneId !== null ||
    data.dailyMinutes !== null ||
    data.updatedAt !== null ||
    etag !== '"availability:unconfigured"'
  )
    throw new Error("Respuesta de disponibilidad inválida");
  return {
    configured: false,
    zoneId: null,
    dailyMinutes: null,
    updatedAt: null,
    etag,
  };
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}
function isDailyMinutes(value: unknown): value is DailyMinutes {
  return (
    isRecord(value) &&
    Object.keys(value).length === 7 &&
    DAY_KEYS.every(
      (key) =>
        typeof value[key] === "number" &&
        Number.isInteger(value[key]) &&
        value[key] >= 0 &&
        value[key] <= 1440,
    )
  );
}
function isConfiguredTag(value: string | null): value is string {
  const match = value?.match(
    /^"availability:[\da-f]{8}-[\da-f]{4}-[\da-f]{4}-[\da-f]{4}-[\da-f]{12}:(0|[1-9]\d*)"$/,
  );
  return Boolean(match && BigInt(match[1]) <= 9223372036854775807n);
}
function isInstant(value: unknown): value is string {
  return (
    typeof value === "string" &&
    /^(?:\d{4}|[+-]\d{6})-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d{1,6})?Z$/.test(
      value,
    ) &&
    Number.isFinite(Date.parse(value)) &&
    new Date(value).toISOString().split(".")[0] ===
      value.replace(/(?:\.\d+)?Z$/, "")
  );
}
export async function readAvailabilityZones(
  signal?: AbortSignal,
): Promise<string[]> {
  const response = await apiRequest("/api/v1/me/availability/zones", {
    credentials: "same-origin",
    cache: "no-store",
    signal,
  });
  if (response.status !== 200) throw response;
  const data: unknown = await response.json();
  if (
    !isRecord(data) ||
    Object.keys(data).length !== 1 ||
    !Array.isArray(data.items) ||
    !data.items.includes("UTC") ||
    !data.items.every(
      (item, index, items) =>
        typeof item === "string" &&
        item.trim().length > 0 &&
        (index === 0 || items[index - 1] < item),
    )
  )
    throw new Error("Catálogo de zonas inválido");
  return data.items;
}
export async function saveAvailability(
  input: AvailabilityInput,
  etag: string,
  signal?: AbortSignal,
): Promise<Extract<AvailabilitySnapshot, { configured: true }>> {
  const sent = {
    zoneId: input.zoneId,
    dailyMinutes: { ...input.dailyMinutes },
  };
  const response = await apiRequest("/api/v1/me/availability", {
    method: "PUT",
    credentials: "same-origin",
    signal,
    headers: { "Content-Type": "application/json", "If-Match": etag },
    body: JSON.stringify(sent),
  });
  const result = await snapshot(response);
  if (
    !result.configured ||
    result.zoneId !== sent.zoneId ||
    DAY_KEYS.some((key) => result.dailyMinutes[key] !== sent.dailyMinutes[key])
  )
    throw new Error("Confirmación de disponibilidad inválida");
  return result;
}
