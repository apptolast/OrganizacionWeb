// @ts-nocheck
import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import { Today } from "./today";
import { agendaToday as todaySnapshot } from "./today-fixture";

let calls: string[];
let holdExternal: Promise<void>;
let release: (() => void) | undefined;

beforeEach(() => {
  calls = [];
  holdExternal = new Promise<void>((resolve) => (release = resolve));
  vi.stubGlobal(
    "fetch",
    vi.fn(async (url: string, options: RequestInit = {}) => {
      const target = String(url);
      calls.push(`${options.method ?? "GET"} ${target.split("?")[0]}`);
      if (target.startsWith("/api/v1/me/external-calendar")) {
        await holdExternal;
        return target.includes("/sync")
          ? Response.json({
              performed: false,
              subscription: {
                id: "11111111-2222-3333-4444-555555555555",
                label: "Trabajo",
                urlHost: "calendar.google.com",
                urlTail: ".ics",
                lastAttemptAt: "2030-01-07T11:00:00Z",
                lastSyncAt: "2030-01-07T11:00:00Z",
                lastStatus: "OK",
                lastError: null,
                snapshotZoneId: "Europe/Madrid",
                imported: 1,
                skippedRecurring: 0,
                skippedCancelled: 0,
                skippedInvalid: 0,
                truncated: false,
                updatedAt: "2030-01-07T11:00:00Z",
              },
            })
          : Response.json({
              configured: true,
              lastSyncAt: "2030-01-07T11:00:00Z",
              lastStatus: "OK",
              items: [
                {
                  uid: "u1",
                  summary: "Reunión externa",
                  startAt: "2030-01-07T08:00:00Z",
                  endAt: "2030-01-07T09:00:00Z",
                  allDay: false,
                },
              ],
            });
      }
      return Response.json(todaySnapshot());
    }),
  );
});
afterEach(() => {
  release?.();
  vi.unstubAllGlobals();
});

it("@s35 la agenda de bloques está completa antes de que respondan las dos llamadas externas", async () => {
  render(<Today />);
  const snapshot = todaySnapshot();
  await screen.findByText(snapshot.items[0].taskTitle);
  expect(screen.getByText(snapshot.items[0].block.objective)).toBeVisible();
  expect(
    screen.queryByRole("region", { name: "Calendario externo" }),
  ).not.toBeInTheDocument();
  release?.();
  expect(
    await screen.findByRole("region", { name: "Calendario externo" }),
  ).toBeInTheDocument();
  expect(screen.getByText("Reunión externa")).toBeInTheDocument();
  expect(screen.getByText(snapshot.items[0].taskTitle)).toBeVisible();
});

it("@s35 Hoy pide primero su propia instantánea y solo después el calendario externo", async () => {
  render(<Today />);
  await screen.findByText(todaySnapshot().items[0].taskTitle);
  release?.();
  await waitFor(() => expect(calls).toHaveLength(3));
  expect(calls[0]).toBe("GET /api/v1/today");
  expect(calls[1]).toBe("POST /api/v1/me/external-calendar/sync");
  expect(calls[2]).toBe("GET /api/v1/me/external-calendar/events");
});
