import { expect, it } from "vitest";
import { validateTask } from "./task-validation";

it.each([
  ["a".repeat(80) + " " + "b".repeat(80), "", "", ["title"]],
  ["  " + "😀".repeat(160) + "  ", "", "", []],
  ["A", "😀".repeat(2000), "1", []],
  ["A", "", "1440", []],
  ["A", "", "1441", ["estimatedMinutes"]],
] as const)(
  "valida límites sin recortar espacios interiores %#",
  (title, criterion, estimate, expected) => {
    expect(validateTask(title, criterion, estimate)).toEqual(expected);
  },
);
