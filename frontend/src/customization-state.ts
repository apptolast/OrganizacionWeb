import {
  useCallback,
  useEffect,
  useRef,
  useState,
  type SetStateAction,
} from "react";
import {
  CustomizationValidationError,
  createCustomField,
  updateCustomField,
  type CustomFieldType,
  readCustomization,
  saveCustomizationView,
  type CustomizationScope,
  type CustomizationSnapshot,
} from "./customization-api";

import {
  readCustomFields,
  saveCustomFields,
  type CustomFieldValue,
  type CustomValuesSnapshot,
} from "./custom-fields-api";

export function useCustomizationSession() {
  const [valueDrafts, setValueDrafts] = useState<
    Record<string, Record<string, string>>
  >({});
  const currentValueDrafts = useRef(valueDrafts);
  currentValueDrafts.current = valueDrafts;
  const setValueDraft = (
    key: string,
    change: SetStateAction<Record<string, string>>,
  ) =>
    setValueDrafts((previous) => ({
      ...previous,
      [key]:
        typeof change === "function" ? change(previous[key] ?? {}) : change,
    }));
  const [viewDrafts, setViewDrafts] = useState<
    Partial<Record<CustomizationScope, string[]>>
  >({});
  const setViewDraft = (
    scope: CustomizationScope,
    draft: string[] | undefined,
  ) => setViewDrafts((previous) => ({ ...previous, [scope]: draft }));
  const [configs, setConfigs] = useState<
    Partial<Record<CustomizationScope, CustomizationSnapshot>>
  >({});
  const [failures, setFailures] = useState<
    Partial<Record<CustomizationScope, string>>
  >({});
  const [saving, setSaving] = useState<
    Partial<Record<CustomizationScope, boolean>>
  >({});
  const writing = useRef(new Map<CustomizationScope, AbortController>());
  const uncertain = useRef(new Set<CustomizationScope>());
  const [loading, setLoading] = useState<
    Partial<Record<CustomizationScope, boolean>>
  >({});
  const readControllers = useRef(
    new Map<CustomizationScope, AbortController>(),
  );
  useEffect(
    () => () => {
      readControllers.current.forEach((controller) => controller.abort());
      readControllers.current.clear();
      writing.current.forEach((controller) => controller.abort());
      writing.current.clear();
      reads.current.clear();
    },
    [],
  );
  const reads = useRef(new Map<CustomizationScope, Promise<void>>());
  const acceptedConfigs = useRef<
    Partial<Record<CustomizationScope, CustomizationSnapshot>>
  >({});
  const acceptConfig = useCallback(
    (scope: CustomizationScope, snapshot: CustomizationSnapshot) => {
      const previous = acceptedConfigs.current[scope];
      acceptedConfigs.current[scope] = snapshot;
      if (!previous || snapshot.etag !== previous.etag) {
        const affected = [...valueReads.current.keys()].filter(
          (key) =>
            (key.endsWith("/") ? "PROJECT" : "TASK") === scope &&
            (Boolean(previous) ||
              (valueSchemas.current.has(key) &&
                valueSchemas.current.get(key) !==
                  snapshot.etag.slice(`"customization:${scope}:`.length, -1))),
        );
        for (const key of affected) {
          if (valueWriting.current.has(key)) {
            valueWriting.current.get(key)?.abort();
            valueWriting.current.delete(key);
            setValueSaving((current) => ({ ...current, [key]: false }));
            uncertainValues.current.add(key);
          }
          valueControllers.current.get(key)?.abort();
          valueControllers.current.delete(key);
          setValueReading((current) => ({ ...current, [key]: false }));
          valueReads.current.delete(key);
          if (
            Object.keys(currentValueDrafts.current[key] ?? {}).length ||
            uncertainValues.current.has(key)
          )
            staleValues.current.add(key);
        }
        setValues((current) => {
          const next = { ...current };
          for (const key of affected)
            if (!staleValues.current.has(key)) delete next[key];
          return next;
        });
        setValueGenerations((current) => {
          const next = { ...current };
          for (const key of affected) next[key] = (next[key] ?? 0) + 1;
          return next;
        });
      }
      setConfigs((current) => ({ ...current, [scope]: snapshot }));
    },
    [],
  );
  const rejectOldValues = useCallback(
    (scope: CustomizationScope, snapshot: CustomValuesSnapshot) => {
      const config = acceptedConfigs.current[scope];
      if (!config || !config.configured) return;
      const expected = config.etag.slice(`"customization:${scope}:`.length, -1);
      const actual = snapshot.etag.split(":schema:")[1].split(":values:")[0];
      const [expectedId, expectedVersion] = expected.split(":");
      const [actualId, actualVersion] = actual.split(":");
      if (
        actualId !== expectedId ||
        BigInt(actualVersion) < BigInt(expectedVersion)
      )
        throw new Error("La respuesta pertenece a un esquema anterior");
    },
    [],
  );
  const loadConfig = useCallback(
    (scope: CustomizationScope) => {
      const current = reads.current.get(scope);
      if (current) return current;
      setLoading((previous) => ({ ...previous, [scope]: true }));
      setFailures((previous) => ({ ...previous, [scope]: undefined }));
      const controller = new AbortController();
      readControllers.current.set(scope, controller);
      const pending = readCustomization(scope, controller.signal)
        .then((snapshot) => {
          controller.signal.throwIfAborted();
          uncertain.current.delete(scope);
          acceptConfig(scope, snapshot);
        })
        .catch(() => {
          if (controller.signal.aborted) return;
          setFailures((previous) => ({
            ...previous,
            [scope]: "Vista base provisional: no se pudo consultar lo guardado",
          }));
        })
        .finally(() => {
          if (readControllers.current.get(scope) === controller) {
            readControllers.current.delete(scope);
            setLoading((previous) => ({ ...previous, [scope]: false }));
          }
        });
      reads.current.set(scope, pending);
      return pending;
    },
    [acceptConfig],
  );
  const retireConfigRead = (scope: CustomizationScope) => {
    readControllers.current.get(scope)?.abort();
    readControllers.current.delete(scope);
    setLoading((previous) => ({ ...previous, [scope]: false }));
  };
  const changeConfig = async (
    scope: CustomizationScope,
    change: (
      previous: CustomizationSnapshot,
      signal: AbortSignal,
    ) => Promise<CustomizationSnapshot>,
  ) => {
    const previous = configs[scope];
    if (!previous || uncertain.current.has(scope) || writing.current.has(scope))
      return false;
    const controller = new AbortController();
    retireConfigRead(scope);
    writing.current.set(scope, controller);
    setSaving((current) => ({ ...current, [scope]: true }));
    try {
      const snapshot = await change(previous, controller.signal);
      controller.signal.throwIfAborted();
      retireConfigRead(scope);
      acceptConfig(scope, snapshot);
      return true;
    } catch (error) {
      if (controller.signal.aborted) return false;
      if (error instanceof CustomizationValidationError) throw error;
      uncertain.current.add(scope);
      setFailures((current) => ({
        ...current,
        [scope]:
          "No se pudo confirmar el guardado. Recarga lo guardado antes de continuar.",
      }));
      return false;
    } finally {
      writing.current.delete(scope);
      setSaving((current) => ({ ...current, [scope]: false }));
    }
  };
  const saveView = (scope: CustomizationScope, visibleFields: string[]) =>
    changeConfig(scope, (previous, signal) =>
      saveCustomizationView(scope, previous, visibleFields, signal),
    );
  const createField = (
    scope: CustomizationScope,
    input: { label: string; type: CustomFieldType },
  ) =>
    changeConfig(scope, (previous, signal) =>
      createCustomField(scope, previous, input, signal),
    );
  const updateField = (
    scope: CustomizationScope,
    fieldId: string,
    input: { label: string; active: boolean },
  ) =>
    changeConfig(scope, (previous, signal) =>
      updateCustomField(scope, previous, fieldId, input, signal),
    );
  const [values, setValues] = useState<Record<string, CustomValuesSnapshot>>(
    {},
  );
  const [valueFailures, setValueFailures] = useState<
    Record<string, string | undefined>
  >({});
  const [valueGenerations, setValueGenerations] = useState<
    Record<string, number>
  >({});
  const staleValues = useRef(new Set<string>());
  const uncertainValues = useRef(new Set<string>());
  const [valueSaving, setValueSaving] = useState<Record<string, boolean>>({});
  const valueWriting = useRef(new Map<string, AbortController>());
  useEffect(
    () => () => {
      valueWriting.current.forEach((controller) => controller.abort());
      valueWriting.current.clear();
    },
    [],
  );
  const [valueReading, setValueReading] = useState<Record<string, boolean>>({});
  const valueControllers = useRef(new Map<string, AbortController>());
  const valueReads = useRef(new Map<string, Promise<void>>());
  const valueSchemas = useRef(new Map<string, string>());
  const withdrawValues = useCallback((key: string) => {
    valueControllers.current.get(key)?.abort();
    valueControllers.current.delete(key);
    valueWriting.current.get(key)?.abort();
    valueWriting.current.delete(key);
    valueReads.current.delete(key);
    setValues((previous) => {
      const next = { ...previous };
      delete next[key];
      return next;
    });
    setValueDrafts((previous) => {
      const next = { ...previous };
      delete next[key];
      return next;
    });
    setValueFailures((previous) => ({
      ...previous,
      [key]: "Los campos personales ya no están disponibles.",
    }));
    setValueReading((previous) => ({ ...previous, [key]: false }));
    setValueSaving((previous) => ({ ...previous, [key]: false }));
  }, []);
  const loadValues = useCallback(
    (projectId: string, taskId?: string, recover = false) => {
      const key = `${projectId}/${taskId ?? ""}`;
      if (
        (uncertainValues.current.has(key) || staleValues.current.has(key)) &&
        !recover
      )
        return Promise.resolve();
      const current = valueReads.current.get(key);
      if (current) return current;
      const controller = new AbortController();
      valueControllers.current.set(key, controller);
      setValueReading((previous) => ({ ...previous, [key]: true }));
      const pending = readCustomFields(projectId, taskId, controller.signal)
        .then((snapshot) => {
          controller.signal.throwIfAborted();
          rejectOldValues(taskId ? "TASK" : "PROJECT", snapshot);
          uncertainValues.current.delete(key);
          staleValues.current.delete(key);
          if (recover)
            setValueDrafts((previous) => ({ ...previous, [key]: {} }));
          setValueFailures((previous) => ({ ...previous, [key]: undefined }));
          valueSchemas.current.set(
            key,
            snapshot.etag.split(":schema:")[1].split(":values:")[0],
          );
          setValues((previous) => ({ ...previous, [key]: snapshot }));
        })
        .catch((error: unknown) => {
          if (controller.signal.aborted) return;
          if (
            error instanceof Response &&
            (error.status === 401 || error.status === 404)
          ) {
            withdrawValues(key);
            throw error;
          }
          setValueFailures((previous) => ({
            ...previous,
            [key]: "No se pudieron consultar los campos personales.",
          }));
        })
        .finally(() => {
          if (valueControllers.current.get(key) === controller) {
            valueControllers.current.delete(key);
            setValueReading((previous) => ({ ...previous, [key]: false }));
          }
        });
      valueReads.current.set(key, pending);
      return pending;
    },
    [withdrawValues, rejectOldValues],
  );
  const saveValues = async (
    projectId: string,
    input: { fieldId: string; value: CustomFieldValue }[],
    taskId?: string,
  ) => {
    const key = `${projectId}/${taskId ?? ""}`;
    if (
      uncertainValues.current.has(key) ||
      staleValues.current.has(key) ||
      valueWriting.current.has(key)
    )
      return false;
    const controller = new AbortController();
    retireValues(projectId, taskId);
    valueWriting.current.set(key, controller);
    setValueSaving((previous) => ({ ...previous, [key]: true }));
    try {
      const snapshot = await saveCustomFields(
        projectId,
        values[key],
        input,
        taskId,
        controller.signal,
      );
      controller.signal.throwIfAborted();
      rejectOldValues(taskId ? "TASK" : "PROJECT", snapshot);
      retireValues(projectId, taskId);
      valueReads.current.set(key, Promise.resolve());
      valueSchemas.current.set(
        key,
        snapshot.etag.split(":schema:")[1].split(":values:")[0],
      );
      setValues((previous) => ({ ...previous, [key]: snapshot }));
      return true;
    } catch (error) {
      if (controller.signal.aborted) return false;
      if (
        error instanceof Response &&
        (error.status === 401 || error.status === 404)
      ) {
        withdrawValues(key);
        throw error;
      }
      if (error instanceof CustomizationValidationError) throw error;
      uncertainValues.current.add(key);
      setValueFailures((previous) => ({
        ...previous,
        [key]:
          "No se pudo confirmar el guardado. Recarga lo guardado antes de continuar.",
      }));
      return false;
    } finally {
      if (valueWriting.current.get(key) === controller) {
        valueWriting.current.delete(key);
        setValueSaving((previous) => ({ ...previous, [key]: false }));
      }
    }
  };
  const retireValues = useCallback((projectId: string, taskId?: string) => {
    const key = `${projectId}/${taskId ?? ""}`;
    const controller = valueControllers.current.get(key);
    if (controller) {
      controller.abort();
      valueControllers.current.delete(key);
      valueReads.current.delete(key);
      setValueReading((previous) => ({ ...previous, [key]: false }));
    }
  }, []);
  const reloadValues = (projectId: string, taskId?: string) => {
    const key = `${projectId}/${taskId ?? ""}`;
    if (valueControllers.current.has(key)) return valueReads.current.get(key);
    valueReads.current.delete(key);
    return loadValues(projectId, taskId, true);
  };
  const reloadConfig = (scope: CustomizationScope) => {
    reads.current.delete(scope);
    return loadConfig(scope);
  };
  return {
    isValuesStale: (key: string) => staleValues.current.has(key),
    isValuesUncertain: (key: string) => uncertainValues.current.has(key),
    isConfigUncertain: (scope: CustomizationScope) =>
      uncertain.current.has(scope),
    valueDrafts,
    setValueDraft,
    viewDrafts,
    setViewDraft,
    values,
    valueGenerations,
    valueReading,
    valueSaving,
    valueFailures,
    reloadValues,
    retireValues,
    loadValues,
    saveValues,
    configs,
    loading,
    saving,
    failures,
    loadConfig,
    reloadConfig,
    saveView,
    createField,
    updateField,
  };
}
export type CustomizationSession = ReturnType<typeof useCustomizationSession>;
