import {
  useEffect,
  useState,
  useRef,
  useLayoutEffect,
  type FormEvent,
} from "react";
import { editRequest } from "./edit-project-api";
export function useEditProject(route: string) {
  const detailRoute = route.slice(0, -"/editar".length);
  const [draft, setDraft] = useState<{ name: string; description: string }>();
  const writeRequest = useRef<AbortController | null>(null);
  const formRef = useRef<HTMLFormElement>(null);
  const [fields, setFields] = useState<string[]>([]);
  useLayoutEffect(() => {
    const field = formRef.current?.elements.namedItem(fields[0]);
    if (field instanceof HTMLElement) field.focus();
  }, [fields]);
  const [etag, setEtag] = useState("");
  const [success, setSuccess] = useState(false);
  const [saving, setSaving] = useState(false);
  const returnFocus = useRef<HTMLElement | null>(null);
  useLayoutEffect(() => {
    if (!saving && document.activeElement === document.body)
      returnFocus.current?.focus();
  }, [saving]);
  const [loadFailure, setLoadFailure] = useState(false);
  const [failure, setFailure] = useState<number | null>(null);
  const [revision, setRevision] = useState(0);
  const [loading, setLoading] = useState(true);
  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setLoadFailure(false);
    void editRequest(detailRoute, controller.signal)
      .then((data) => {
        if (controller.signal.aborted) return;
        setDraft(data.project);
        setEtag(data.etag);
      })
      .catch(
        (error) =>
          !controller.signal.aborted &&
          (setLoadFailure(true),
          setFailure(error instanceof Response ? error.status : 0)),
      )
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => {
      controller.abort();
      writeRequest.current?.abort();
    };
  }, [detailRoute, revision]);
  async function submit(event: FormEvent) {
    event.preventDefault();
    if (saving || loading) return;
    returnFocus.current =
      document.activeElement instanceof HTMLButtonElement
        ? document.activeElement
        : null;
    setLoadFailure(false);
    setSaving(true);
    setFields([]);
    setSuccess(false);
    setFailure(null);
    const controller = new AbortController();
    writeRequest.current = controller;
    try {
      const data = await editRequest(
        detailRoute,
        controller.signal,
        { name: draft!.name, description: draft!.description },
        etag,
      );
      if (controller.signal.aborted) return;
      setDraft(data.project);
      setEtag(data.etag);
      setSuccess(true);
    } catch (error) {
      if (controller.signal.aborted) return;
      if (error instanceof Response && error.status === 400) {
        const body: unknown = await error.json().catch(() => null);
        if (
          body &&
          typeof body === "object" &&
          "errors" in body &&
          Array.isArray(body.errors)
        )
          setFields(
            body.errors.flatMap((entry: unknown) =>
              entry &&
              typeof entry === "object" &&
              "field" in entry &&
              (entry.field === "name" || entry.field === "description")
                ? [entry.field]
                : [],
            ),
          );
      }
      setFailure(error instanceof Response ? error.status : 0);
      if (
        error instanceof Response &&
        (error.status === 401 || error.status === 404)
      ) {
        setDraft(undefined);
        setEtag("");
      }
    }
    setSaving(false);
  }
  return {
    loadFailure,
    formRef,
    fields,
    loading,
    detailRoute,
    draft,
    setDraft: (value: { name: string; description: string }) => {
      setDraft(value);
      setSuccess(false);
    },
    success,
    saving,
    failure,
    setFailure,
    revision,
    setRevision,
    submit,
  };
}
