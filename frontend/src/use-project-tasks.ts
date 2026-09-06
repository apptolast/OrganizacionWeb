import { useEffect, useRef, useState, type FormEvent } from "react";
import { createTask, readTasks, type TaskPage, type Task } from "./tasks-api";
import { validateTask, taskMessages, type TaskField } from "./task-validation";
import { readProjects, type ProjectSnapshot } from "./read-projects-api";
export function useProjectTasks(
  projectId: string,
  onProjectConfirmed: (snapshot: ProjectSnapshot) => void,
) {
  const [title, setTitle] = useState("");
  const [errors, setErrors] = useState<TaskField[]>([]);
  const [criterion, setCriterion] = useState("");
  const [estimate, setEstimate] = useState("");
  const [saveFailure, setSaveFailure] = useState<string>();
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState<Task>();
  const [page, setPage] = useState<TaskPage>();
  const [failure, setFailure] = useState(false);
  const [revision, setRevision] = useState(0);
  const [cursor, setCursor] = useState<string>();
  const [completedConflict, setCompletedConflict] = useState(false);
  const [reviewing, setReviewing] = useState(false);
  const review = useRef<AbortController | undefined>(undefined);
  const write = useRef<AbortController | undefined>(undefined);
  useEffect(
    () => () => {
      write.current?.abort();
      review.current?.abort();
    },
    [],
  );
  useEffect(() => {
    const controller = new AbortController();
    void readTasks(projectId, cursor, controller.signal)
      .then((data) => {
        if (!controller.signal.aborted) setPage(data);
      })
      .catch(() => {
        if (!controller.signal.aborted) setFailure(true);
      });
    return () => controller.abort();
  }, [projectId, revision, cursor]);
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (saving || completedConflict || reviewing) return;
    const form = event.currentTarget;
    const invalid = validateTask(title, criterion, estimate);
    if (
      event.currentTarget.querySelector<HTMLInputElement>(
        '[name="estimatedMinutes"]',
      )?.validity.badInput &&
      !invalid.includes("estimatedMinutes")
    )
      invalid.push("estimatedMinutes");
    setErrors(invalid);
    if (invalid.length) {
      event.currentTarget
        .querySelector<HTMLElement>(`[name="${invalid[0]}"]`)
        ?.focus();
      return;
    }
    setSaving(true);
    setSaveFailure(undefined);
    setSaved(undefined);
    const controller = new AbortController();
    write.current = controller;
    try {
      const created = await createTask(
        projectId,
        {
          title,
          completionCriterion: criterion,
          estimatedMinutes: estimate === "" ? null : Number(estimate),
        },
        controller.signal,
      );
      if (controller.signal.aborted) return;
      setSaved(created);
      setCursor(undefined);
      setPage(undefined);
      setFailure(false);
      setRevision((value) => value + 1);
    } catch (error) {
      if (controller.signal.aborted) return;
      const problem =
        error instanceof Response
          ? await error
              .clone()
              .json()
              .catch(() => null)
          : null;
      if (controller.signal.aborted) return;
      if (
        error instanceof Response &&
        error.status === 409 &&
        problem?.code === "PROJECT_COMPLETED"
      )
        setCompletedConflict(true);
      if (
        error instanceof Response &&
        error.status === 400 &&
        Array.isArray(problem?.errors)
      ) {
        const invalid = (Object.keys(taskMessages) as TaskField[]).filter(
          (field) =>
            problem.errors.some(
              (entry: unknown) =>
                typeof entry === "object" &&
                entry !== null &&
                "field" in entry &&
                entry.field === field,
            ),
        );
        setErrors(invalid);
        if (invalid.length)
          form.querySelector<HTMLElement>(`[name="${invalid[0]}"]`)?.focus();
      }
      setSaveFailure(
        error instanceof Response
          ? "No hemos podido guardar la tarea."
          : "No podemos confirmar si la tarea se guardó. Comprueba la lista antes de volver a intentarlo.",
      );
    } finally {
      if (!controller.signal.aborted) setSaving(false);
    }
  }
  async function reviewProject() {
    if (reviewing) return;
    const controller = new AbortController();
    review.current = controller;
    setReviewing(true);
    try {
      const result = await readProjects(
        `/proyectos/${projectId}`,
        controller.signal,
      );
      if (controller.signal.aborted) return;
      onProjectConfirmed(result as ProjectSnapshot);
      setCompletedConflict(false);
      setSaveFailure(undefined);
    } catch {
      if (!controller.signal.aborted)
        setSaveFailure(
          "No hemos podido revisar el estado del proyecto. Tu borrador se conserva.",
        );
    } finally {
      if (!controller.signal.aborted) setReviewing(false);
    }
  }
  function reload() {
    setPage(undefined);
    setFailure(false);
    setRevision((value) => value + 1);
  }
  function changePage(next?: string) {
    setPage(undefined);
    setFailure(false);
    setCursor(next);
  }
  return {
    title,
    setTitle,
    criterion,
    setCriterion,
    estimate,
    setEstimate,
    errors,
    saving,
    saved,
    saveFailure,
    page,
    failure,
    cursor,
    completedConflict,
    reviewing,
    submit,
    reviewProject,
    reload,
    changePage,
  };
}
