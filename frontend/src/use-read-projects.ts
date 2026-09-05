import { useEffect, useState } from "react";
import type { Project } from "./projects-api";
import { readProjects, type ProjectPage } from "./read-projects-api";
export function useReadProjects(route: string) {
  const [data, setData] = useState<ProjectPage | Project>();
  const [failure, setFailure] = useState<number | null>(null);
  const [revision, setRevision] = useState(0);
  useEffect(() => {
    const controller = new AbortController();
    void readProjects(route, controller.signal)
      .then((result) => {
        if (!controller.signal.aborted) setData(result);
      })
      .catch((error: unknown) => {
        if (!controller.signal.aborted)
          setFailure(error instanceof Response ? error.status : 0);
      });
    return () => controller.abort();
  }, [revision, route]);
  return {
    isDetail: route.startsWith("/proyectos/"),
    isContinuation: route.includes("?"),
    page: data && "items" in data ? data : undefined,
    project: data && "id" in data ? data : undefined,
    failure,
    retry: () => {
      setData(undefined);
      setFailure(null);
      setRevision(revision + 1);
    },
  };
}
