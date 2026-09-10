// @ts-nocheck
import {
  act,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { useWorkSessionDecision } from "./use-work-session-decision";
import { apiRequest, observeAccess } from "./api-client";
import { WorkSessionStatePanel } from "./work-session-state";

afterEach(() => {
  observeAccess(undefined);
  vi.unstubAllGlobals();
});

it("@s40 two successive confirmations each require a new state before another decision", async () => {
  const session = {
    id: "12345678-1234-1234-1234-123456789abc",
    projectId: "22345678-1234-1234-1234-123456789abc",
    taskId: "32345678-1234-1234-1234-123456789abc",
    startedAt: "2026-09-07T10:00:00Z",
    plannedMinutes: 25,
    plannedEndAt: "2026-09-07T10:25:00Z",
    zoneId: "UTC",
  };
  const state = {
    session,
    status: "running",
    revision: "1",
    changedAt: session.startedAt,
    runningSince: session.startedAt,
    workedMicroseconds: "0",
  };
  const snapshot = (revision: string) =>
    Response.json(
      {
        state: { ...state, revision },
        serverNow: session.startedAt,
        netMicroseconds: "0",
      },
      {
        headers: {
          "Work-Session-Revision": `work-session-${session.id}-${revision}`,
        },
      },
    );
  let reads = 0;
  let finish!: (value: Response) => void;
  const fetcher = vi.fn<
    (url: string, options?: RequestInit) => Promise<Response>
  >((url) => {
    if (url.endsWith("/state")) {
      if (++reads === 3)
        return new Promise<Response>((resolve) => {
          finish = resolve;
        });
      return Promise.resolve(snapshot(String(reads)));
    }
    return new Promise<Response>(() => {});
  });
  vi.stubGlobal("fetch", fetcher);
  function Surface() {
    const decision = useWorkSessionDecision();
    return (
      <>
        <WorkSessionStatePanel
          session={session}
          onAccessFailure={vi.fn()}
          decision={decision}
        />
        <button onClick={decision.settle}>Confirmación hermana recibida</button>
      </>
    );
  }
  render(<Surface />);
  await screen.findByRole("button", { name: "Pausar" });
  fireEvent.click(
    screen.getByRole("button", { name: "Confirmación hermana recibida" }),
  );
  await waitFor(() => expect(reads).toBe(2));
  await waitFor(() =>
    expect(screen.getByRole("button", { name: "Pausar" })).toHaveAttribute(
      "aria-disabled",
      "false",
    ),
  );
  fireEvent.click(
    screen.getByRole("button", { name: "Confirmación hermana recibida" }),
  );
  await waitFor(() => expect(reads).toBe(3));
  fireEvent.click(screen.getByRole("button", { name: "Pausar" }));
  expect(
    fetcher.mock.calls.filter(([, options]) => options?.method === "POST"),
  ).toHaveLength(0);
  await act(async () => {
    finish(snapshot("3"));
  });
  fireEvent.click(screen.getByRole("button", { name: "Pausar" }));
  const post = fetcher.mock.calls.find(
    ([, options]) => options?.method === "POST",
  );
  expect(post?.[1]?.headers).toEqual(
    expect.objectContaining({
      "Work-Session-Revision": `work-session-${session.id}-3`,
    }),
  );
});

it("@s41 confirmation discards a lookup started during the pending decision before a late unauthorized response", async () => {
  let finish!: (value: Response) => void;
  vi.stubGlobal(
    "fetch",
    vi.fn(
      () =>
        new Promise<Response>((resolve) => {
          finish = resolve;
        }),
    ),
  );
  const observer = vi.fn();
  const display = vi.fn();
  observeAccess(observer);
  function Surface() {
    const decision = useWorkSessionDecision();
    return (
      <>
        <button
          onClick={() => {
            if (!decision.acquire("PAUSE")) return;
            const controller = new AbortController();
            const untrack = decision.track(controller);
            void apiRequest("/api/v1/work-sessions/example/end-time", {
              signal: controller.signal,
            })
              .then((response) => {
                if (!controller.signal.aborted) display(response.status);
              })
              .finally(untrack);
          }}
        >
          Pausar y consultar
        </button>
        <button onClick={decision.settle}>Confirmación recibida</button>
      </>
    );
  }
  render(<Surface />);
  fireEvent.click(screen.getByRole("button", { name: "Pausar y consultar" }));
  fireEvent.click(
    screen.getByRole("button", { name: "Confirmación recibida" }),
  );
  await act(async () => {
    finish(new Response(null, { status: 401 }));
  });
  expect(observer).not.toHaveBeenCalled();
  expect(display).not.toHaveBeenCalled();
});

it("@s36 two sibling decisions in one event transmit only the first intention", () => {
  const transmit = vi.fn();
  function Surface() {
    const decision = useWorkSessionDecision();
    return (
      <button
        onClick={() => {
          if (decision.acquire("PAUSE")) transmit("PAUSE");
          if (decision.acquire("EXTEND")) transmit("EXTEND");
        }}
      >
        Pausar y después ampliar
      </button>
    );
  }
  render(<Surface />);
  fireEvent.click(screen.getByRole("button"));
  expect(transmit.mock.calls).toEqual([["PAUSE"]]);
});
