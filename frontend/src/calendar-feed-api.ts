import { apiRequest } from "./api-client";
import { exact, instant } from "./schedule-block-api";

const FEED = "/api/v1/me/calendar-feed";
const DOWNLOAD = "/api/v1/me/calendar.ics";
export const CALENDAR_FILE_NAME = "organizationweb-bloques.ics";
const FEED_ADDRESS = /^https:\/\/[^/]+\/calendar\/[A-Za-z0-9_-]{43}\.ics$/;
const OPENING = "BEGIN:VCALENDAR\r\n";
const CLOSING = "END:VCALENDAR\r\n";

export type CalendarFeedStatus = {
  active: boolean;
  createdAt: string | null;
};

export type CalendarFeedLink = {
  url: string;
  createdAt: string;
};

function incompatible(): never {
  throw new Error("Respuesta del calendario incompatible.");
}

export async function readCalendarFeed(
  signal: AbortSignal,
): Promise<CalendarFeedStatus> {
  signal.throwIfAborted();
  const response = await apiRequest(FEED, { signal });
  signal.throwIfAborted();
  if (response.status !== 200) throw response;
  const status: unknown = await response.json().catch(() => incompatible());
  if (!exact(status, "active createdAt") || typeof status.active !== "boolean")
    incompatible();
  if (status.active ? !instant(status.createdAt) : status.createdAt !== null)
    incompatible();
  return {
    active: status.active,
    createdAt: status.createdAt as string | null,
  };
}

export async function createCalendarFeed(
  signal: AbortSignal,
): Promise<CalendarFeedLink> {
  signal.throwIfAborted();
  const response = await apiRequest(FEED, { method: "POST", signal });
  signal.throwIfAborted();
  if (response.status !== 201) throw response;
  const link: unknown = await response.json().catch(() => incompatible());
  if (
    !exact(link, "url createdAt") ||
    typeof link.url !== "string" ||
    !FEED_ADDRESS.test(link.url) ||
    !instant(link.createdAt)
  )
    incompatible();
  return { url: link.url, createdAt: link.createdAt as string };
}

export async function revokeCalendarFeed(signal: AbortSignal): Promise<void> {
  signal.throwIfAborted();
  const response = await apiRequest(FEED, { method: "DELETE", signal });
  signal.throwIfAborted();
  if (response.status !== 204) throw response;
}

/** Nothing is offered for download until the whole document has been checked. */
export async function readCalendarFile(signal: AbortSignal) {
  signal.throwIfAborted();
  const response = await apiRequest(DOWNLOAD, {
    signal,
    headers: { Accept: "text/calendar" },
  });
  signal.throwIfAborted();
  if (response.status !== 200) throw response;
  if (
    !/^text\/calendar\s*;\s*charset=utf-8$/i.test(
      response.headers.get("Content-Type") ?? "",
    )
  )
    incompatible();
  const declared = response.headers.get("Content-Length");
  if (!declared || !/^[1-9][0-9]*$/.test(declared)) incompatible();
  const bytes = new Uint8Array(await response.arrayBuffer());
  signal.throwIfAborted();
  if (bytes.byteLength !== Number(declared)) incompatible();
  const document = new TextDecoder("utf-8", {
    fatal: true,
    ignoreBOM: true,
  }).decode(bytes);
  if (!document.startsWith(OPENING) || !document.endsWith(CLOSING))
    incompatible();
  return { bytes, fileName: CALENDAR_FILE_NAME };
}
