import { act, renderHook } from "@testing-library/react";
import type { FormEvent } from "react";
import { expect, it, vi } from "vitest";
import { useCreateProject } from "./use-create-project";
it("recibir validación después de desmontar el formulario no intenta enfocar DOM inexistente", async () => {
  let resolve!: (response: Response) => void;
  vi.spyOn(globalThis, "fetch").mockImplementation(
    () =>
      new Promise((r) => {
        resolve = r;
      }),
  );
  const { result, unmount } = renderHook(() => useCreateProject());
  let pending!: Promise<void>;
  act(() => {
    pending = result.current.submit({
      preventDefault: vi.fn(),
    } as unknown as FormEvent<HTMLFormElement>);
  });
  unmount();
  resolve(
    Response.json(
      {
        title: "Revisa el nombre.",
        errors: [{ field: "name", message: "Escribe un nombre." }],
      },
      { status: 400 },
    ),
  );
  await expect(pending).resolves.toBeUndefined();
});
