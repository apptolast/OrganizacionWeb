import {
  useState,
  useRef,
  useEffect,
  useLayoutEffect,
  type FormEvent,
} from "react";
import { createProject, type Project, type FieldError } from "./projects-api";
export function useCreateProject() {
  const request = useRef<AbortController | null>(null);
  useEffect(() => () => request.current?.abort(), []);
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [saving, setSaving] = useState(false);
  const [project, setProject] = useState<Project>();
  const [errors, setErrors] = useState<FieldError[]>([]);
  const [failure, setFailure] = useState("");
  const formRef = useRef<HTMLFormElement>(null);
  const returnFocus = useRef<HTMLElement | null>(null);
  useLayoutEffect(() => {
    if (!saving && document.activeElement === document.body) {
      returnFocus.current?.focus();
    }
  }, [saving]);
  const nameError = errors.find((error) => error.field === "name")?.message;
  const descriptionError = errors.find(
    (error) => error.field === "description",
  )?.message;
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (saving) return;
    returnFocus.current =
      document.activeElement instanceof HTMLButtonElement
        ? document.activeElement
        : null;
    setSaving(true);
    setErrors([]);
    setFailure("");
    setProject(undefined);
    const controller = new AbortController();
    request.current = controller;
    const result = await createProject(
      { name, description },
      controller.signal,
    );
    if (controller.signal.aborted) return;
    if (result.kind === "created") setProject(result.project);
    else {
      setFailure(result.message);
      setErrors(result.errors);
      const field = formRef.current?.elements.namedItem(
        result.errors[0]?.field ?? "",
      );
      if (field instanceof HTMLElement) field.focus();
    }
    setSaving(false);
  }
  return {
    name,
    setName,
    description,
    setDescription,
    saving,
    project,
    failure,
    formRef,
    nameError,
    descriptionError,
    submit,
  };
}
