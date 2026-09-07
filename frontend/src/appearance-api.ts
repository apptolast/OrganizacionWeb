import { apiRequest } from "./api-client";
import { exact, instant } from "./schedule-block-api";

export type AppearanceInput = {
  theme: "LIGHT" | "DARK" | "SYSTEM";
  accentLight: string;
  accentDark: string;
};
export type AppearanceSnapshot = AppearanceInput & {
  configured: boolean;
  updatedAt: string | null;
  etag: string;
};
const lightSurfaces = [
  "#F8F9F5",
  "#FFFFFF",
  "#FDFEFB",
  "#EEF1E9",
  "#DFE8D9",
  "#D0DFC9",
];
const darkSurfaces = [
  "#111827",
  "#1F2937",
  "#182232",
  "#0B1220",
  "#28394A",
  "#33485C",
];
function luminance(color: string) {
  const channels = [1, 3, 5].map((start) => {
    const channel = parseInt(color.slice(start, start + 2), 16) / 255;
    return channel <= 0.04045
      ? channel / 12.92
      : ((channel + 0.055) / 1.055) ** 2.4;
  });
  return channels[0] * 0.2126 + channels[1] * 0.7152 + channels[2] * 0.0722;
}
function validAccent(value: unknown, surfaces: string[]): value is string {
  if (typeof value !== "string" || !/^#[0-9A-F]{6}$/.test(value)) return false;
  const foreground = luminance(value);
  return surfaces.every((surface) => {
    const background = luminance(surface);
    return (
      (Math.max(foreground, background) + 0.05) /
        (Math.min(foreground, background) + 0.05) >=
      4.5
    );
  });
}
export function isAppearanceAccent(value: string, theme: "LIGHT" | "DARK") {
  return validAccent(
    value.toUpperCase(),
    theme === "LIGHT" ? lightSurfaces : darkSurfaces,
  );
}
export class AppearanceValidationError extends Error {
  constructor(
    public readonly fields: Partial<Record<keyof AppearanceInput, string>>,
  ) {
    super("Revisa los campos de apariencia");
  }
}
export async function readAppearance(
  signal?: AbortSignal,
): Promise<AppearanceSnapshot> {
  const response = await apiRequest("/api/v1/me/appearance", {
    credentials: "same-origin",
    cache: "no-store",
    signal,
  });
  return snapshot(response, signal);
}
export async function saveAppearance(
  input: AppearanceInput,
  etag: string,
  signal?: AbortSignal,
): Promise<AppearanceSnapshot> {
  const sent = {
    theme: input.theme,
    accentLight: input.accentLight,
    accentDark: input.accentDark,
  };
  const response = await apiRequest("/api/v1/me/appearance", {
    method: "PUT",
    credentials: "same-origin",
    cache: "no-store",
    signal,
    headers: { "Content-Type": "application/json", "If-Match": etag },
    body: JSON.stringify(sent),
  });
  signal?.throwIfAborted();
  if (response.status === 400) {
    const problem: unknown = await response
      .clone()
      .json()
      .catch(() => null);
    signal?.throwIfAborted();
    if (
      exact(problem, "type title status code errors") &&
      problem.type === "urn:organization:problem:validation_error" &&
      problem.code === "VALIDATION_ERROR" &&
      problem.status === 400 &&
      typeof problem.title === "string" &&
      problem.title.trim() &&
      Array.isArray(problem.errors) &&
      problem.errors.length > 0 &&
      problem.errors.every(
        (entry) =>
          exact(entry, "field code message") &&
          ["theme", "accentLight", "accentDark"].includes(
            entry.field as string,
          ) &&
          [
            "REQUIRED",
            "INVALID_TYPE",
            "INVALID_VALUE",
            "INSUFFICIENT_CONTRAST",
          ].includes(entry.code as string) &&
          typeof entry.message === "string" &&
          entry.message.trim(),
      )
    ) {
      throw new AppearanceValidationError(
        Object.fromEntries(
          problem.errors.map((entry) => [entry.field, entry.message]),
        ),
      );
    }
  }
  const next = await snapshot(response, signal);
  signal?.throwIfAborted();
  if (
    !next.configured ||
    next.theme !== sent.theme ||
    next.accentLight !== sent.accentLight.toUpperCase() ||
    next.accentDark !== sent.accentDark.toUpperCase()
  )
    throw new Error("Confirmación de apariencia incompatible");
  return next;
}
async function snapshot(
  response: Response,
  signal?: AbortSignal,
): Promise<AppearanceSnapshot> {
  signal?.throwIfAborted();
  if (response.status !== 200) throw response;
  const data: unknown = await response.json();
  signal?.throwIfAborted();
  if (
    !exact(data, "configured theme accentLight accentDark updatedAt") ||
    typeof data.configured !== "boolean"
  )
    throw new Error("Respuesta de apariencia inválida");
  if (
    data.configured === false &&
    (data.theme !== "SYSTEM" ||
      data.accentLight !== "#244C3C" ||
      data.accentDark !== "#B7E4C7" ||
      data.updatedAt !== null ||
      response.headers.get("ETag") !== '"appearance:unconfigured"')
  )
    throw new Error("Respuesta de apariencia inválida");
  if (
    data.theme !== "LIGHT" &&
    data.theme !== "DARK" &&
    data.theme !== "SYSTEM"
  )
    throw new Error("Respuesta de apariencia inválida");
  if (data.configured && !instant(data.updatedAt))
    throw new Error("Respuesta de apariencia inválida");
  if (
    !validAccent(data.accentLight, lightSurfaces) ||
    !validAccent(data.accentDark, darkSurfaces)
  )
    throw new Error("Respuesta de apariencia inválida");
  const etag = response.headers.get("ETag");
  if (data.configured) {
    const match = etag?.match(
      /^"appearance:[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}:(0|[1-9][0-9]{0,18})"$/,
    );
    if (!match || BigInt(match[1]) > 9223372036854775807n)
      throw new Error("Respuesta de apariencia inválida");
  }
  return { ...data, etag } as AppearanceSnapshot;
}
