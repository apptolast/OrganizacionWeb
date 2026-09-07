import { useRef, useState, useCallback } from "react";
import type { WorkSessionIntent } from "./work-session-state-api";

export function useWorkSessionDecision() {
  const owner = useRef<WorkSessionIntent["action"] | undefined>(undefined);
  const [current, setCurrent] = useState<WorkSessionIntent["action"]>();
  const [generation, setGeneration] = useState(0);
  const reads = useRef(new Set<AbortController>());
  const track = useCallback((controller: AbortController) => {
    reads.current.add(controller);
    return () => {
      reads.current.delete(controller);
    };
  }, []);
  const release = useCallback(() => {
    owner.current = undefined;
    setCurrent(undefined);
  }, []);
  const settle = useCallback(() => {
    for (const controller of reads.current) controller.abort();
    reads.current.clear();
    release();
    setGeneration((value) => value + 1);
  }, [release]);
  const acquire = useCallback((action: WorkSessionIntent["action"]) => {
    if (owner.current && owner.current !== action) return false;
    for (const controller of reads.current) controller.abort();
    reads.current.clear();
    owner.current = action;
    setCurrent(action);
    return true;
  }, []);
  return {
    owner: current,
    acquire,
    generation,
    settle,
    release,
    track,
  };
}
export type SessionDecision = ReturnType<typeof useWorkSessionDecision>;
