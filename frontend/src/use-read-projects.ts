import { useEffect, useState } from "react";
import {
  readProjects,
  type ProjectPage,
  type ProjectSnapshot,
} from "./read-projects-api";
export function useReadProjects(route: string) {
  const [data, setData] = useState<ProjectPage | ProjectSnapshot>();
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
    project: data && "project" in data ? data.project : undefined,
    snapshot: data && "project" in data ? data : undefined,
    confirmProject: (snapshot: ProjectSnapshot) => setData(snapshot),
    revokeProject: (status: number) => {
      setData(undefined);
      setFailure(status);
    },
    failure,
    retry: () => {
      setData(undefined);
      setFailure(null);
      setRevision(revision + 1);
    },
  };
}
