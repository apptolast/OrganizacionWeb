import { act, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { useWorkSessionDecision } from "./use-work-session-decision";
import { apiRequest, observeAccess } from "./api-client";

afterEach(() => {
  observeAccess(undefined);
  vi.unstubAllGlobals();
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
