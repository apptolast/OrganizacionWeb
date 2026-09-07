import {
  useEffect,
  useLayoutEffect,
  useRef,
  useState,
  type CSSProperties,
  type RefObject,
} from "react";
import { useAppearance } from "./appearance-state";
import {
  isAppearanceAccent,
  AppearanceValidationError,
  type AppearanceInput,
  type AppearanceSnapshot,
} from "./appearance-api";

export function Appearance() {
  const { snapshot, failed } = useAppearance();
  const heading = useRef<HTMLHeadingElement>(null);
  useLayoutEffect(() => {
    heading.current?.focus();
  }, []);
  return (
    <main className="appearance" id="proyectos">
      <h1 ref={heading} tabIndex={-1}>
        Apariencia
      </h1>
      {!snapshot && !failed && <p role="status">Consultando apariencia…</p>}
      {failed && snapshot && (
        <>
          <p role="alert">
            No se pudo consultar la apariencia. Se conserva la última versión
            confirmada.
          </p>
        </>
      )}
      {snapshot && <AppearanceForm snapshot={snapshot} heading={heading} />}
    </main>
  );
}
function AppearanceForm({
  snapshot,
  heading,
}: {
  snapshot: AppearanceSnapshot;
  heading: RefObject<HTMLHeadingElement | null>;
}) {
  const { save, reload, reading, uncertain } = useAppearance();
  const sending = useRef(false);
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);
  const recoveryInitiator = useRef<HTMLElement | null>(null);
  useEffect(() => {
    const moved = (event: FocusEvent) => {
      if (event.target !== recoveryInitiator.current)
        recoveryInitiator.current = null;
    };
    document.addEventListener("focusin", moved);
    return () => document.removeEventListener("focusin", moved);
  }, []);
  useLayoutEffect(() => {
    const control = recoveryInitiator.current;
    if (!uncertain && control) {
      recoveryInitiator.current = null;
      if (!control.isConnected && document.activeElement === document.body)
        heading.current?.focus();
    }
  }, [uncertain, heading]);
  const [fieldErrors, setFieldErrors] = useState<
    Partial<Record<keyof AppearanceInput, string>>
  >({});
  const [draft, setDraft] = useState<AppearanceInput>(snapshot);
  const [preview, setPreview] = useState({
    accentLight: snapshot.accentLight,
    accentDark: snapshot.accentDark,
  });
  const editDraft = (next: AppearanceInput) => {
    setDraft(next);
    setSaved(false);
  };
  const lightValid = isAppearanceAccent(draft.accentLight, "LIGHT");
  const darkValid = isAppearanceAccent(draft.accentDark, "DARK");
  const editColor = (field: "accentLight" | "accentDark", value: string) => {
    editDraft({ ...draft, [field]: value });
    setFieldErrors({ ...fieldErrors, [field]: undefined });
    if (isAppearanceAccent(value, field === "accentLight" ? "LIGHT" : "DARK"))
      setPreview({ ...preview, [field]: value.toUpperCase() });
  };
  const changed =
    draft.theme !== snapshot.theme ||
    draft.accentLight !== snapshot.accentLight ||
    draft.accentDark !== snapshot.accentDark;
  return (
    <form
      aria-busy={saving}
      onSubmit={async (event) => {
        event.preventDefault();
        if (sending.current || uncertain || !lightValid || !darkValid) return;
        sending.current = true;
        setSaving(true);
        setSaved(false);
        try {
          const next = await save(draft);
          editDraft(next);
          setFieldErrors({});
          setPreview({
            accentLight: next.accentLight,
            accentDark: next.accentDark,
          });
          setSaved(true);
        } catch (error) {
          if (error instanceof AppearanceValidationError)
            setFieldErrors(error.fields);
        } finally {
          sending.current = false;
          setSaving(false);
        }
      }}
    >
      <fieldset
        disabled={saving}
        aria-invalid={Boolean(fieldErrors.theme)}
        aria-describedby={
          fieldErrors.theme ? "appearance-theme-error" : undefined
        }
      >
        <legend>Tema</legend>
        {(
          [
            ["LIGHT", "Claro"],
            ["DARK", "Oscuro"],
            ["SYSTEM", "Sistema"],
          ] as const
        ).map(([value, label]) => (
          <label key={value}>
            <input
              type="radio"
              name="theme"
              value={value}
              checked={draft.theme === value}
              onChange={() => {
                editDraft({ ...draft, theme: value });
                setFieldErrors({ ...fieldErrors, theme: undefined });
              }}
            />
            {label}
          </label>
        ))}
      </fieldset>
      {fieldErrors.theme && (
        <p id="appearance-theme-error" role="alert">
          {fieldErrors.theme}
        </p>
      )}
      <label htmlFor="appearance-light">Color de acento claro</label>
      <label>
        Seleccionar acento claro
        <input
          type="color"
          disabled={saving}
          value={(/^#[0-9a-f]{6}$/i.test(draft.accentLight)
            ? draft.accentLight
            : preview.accentLight
          ).toLowerCase()}
          onChange={(event) => editColor("accentLight", event.target.value)}
        />
      </label>
      <input
        id="appearance-light"
        name="accentLight"
        readOnly={saving}
        value={draft.accentLight}
        onChange={(event) => editColor("accentLight", event.target.value)}
        aria-invalid={!lightValid || Boolean(fieldErrors.accentLight)}
        aria-describedby={
          !lightValid || fieldErrors.accentLight
            ? "appearance-light-error"
            : undefined
        }
      />
      {(!lightValid || fieldErrors.accentLight) && (
        <p id="appearance-light-error" role="alert">
          {fieldErrors.accentLight ??
            "Elige un color con más contraste. La muestra conserva el último color válido."}
        </p>
      )}
      <label htmlFor="appearance-dark">Color de acento oscuro</label>
      <label>
        Seleccionar acento oscuro
        <input
          type="color"
          disabled={saving}
          value={(/^#[0-9a-f]{6}$/i.test(draft.accentDark)
            ? draft.accentDark
            : preview.accentDark
          ).toLowerCase()}
          onChange={(event) => editColor("accentDark", event.target.value)}
        />
      </label>
      <input
        id="appearance-dark"
        name="accentDark"
        readOnly={saving}
        value={draft.accentDark}
        onChange={(event) => editColor("accentDark", event.target.value)}
        aria-invalid={!darkValid || Boolean(fieldErrors.accentDark)}
        aria-describedby={
          !darkValid || fieldErrors.accentDark
            ? "appearance-dark-error"
            : undefined
        }
      />
      {(!darkValid || fieldErrors.accentDark) && (
        <p id="appearance-dark-error" role="alert">
          {fieldErrors.accentDark ??
            "Elige un color con más contraste. La muestra conserva el último color válido."}
        </p>
      )}
      {changed && <p>Tienes cambios sin guardar.</p>}
      {changed && (
        <p>Al salir de esta pantalla se descartan los cambios sin guardar.</p>
      )}
      <button
        type="submit"
        aria-disabled={saving || uncertain || !lightValid || !darkValid}
      >
        Guardar apariencia
      </button>
      <button
        type="button"
        disabled={saving}
        onClick={() => {
          editDraft({
            theme: "SYSTEM",
            accentLight: "#244C3C",
            accentDark: "#B7E4C7",
          });
          setPreview({ accentLight: "#244C3C", accentDark: "#B7E4C7" });
          setFieldErrors({});
        }}
      >
        Restaurar valores predeterminados
      </button>
      <button
        type="button"
        disabled={saving}
        onClick={() => {
          editDraft(snapshot);
          setFieldErrors({});
          setPreview({
            accentLight: snapshot.accentLight,
            accentDark: snapshot.accentDark,
          });
        }}
      >
        Cancelar cambios
      </button>
      {saving ? (
        <p role="status">Guardando apariencia…</p>
      ) : (
        saved && <p role="status">Apariencia guardada.</p>
      )}
      {uncertain && (
        <>
          <p role="alert">
            No podemos confirmar el guardado. Recargar la versión guardada
            reemplazará este borrador.
          </p>
          {reading && <p role="status">Consultando versión guardada…</p>}
          <button
            type="button"
            aria-disabled={reading}
            onClick={async (event) => {
              if (reading) return;
              recoveryInitiator.current = event.currentTarget;
              try {
                const next = await reload();
                editDraft(next);
                setFieldErrors({});
                setPreview({
                  accentLight: next.accentLight,
                  accentDark: next.accentDark,
                });
              } catch {
                /* La consulta conserva el borrador y la incertidumbre. */
              }
            }}
          >
            Recargar versión guardada
          </button>
        </>
      )}
      <section
        aria-label="Vista previa clara"
        data-theme="light"
        style={{ "--accent": preview.accentLight } as CSSProperties}
      >
        <h2>Vista previa clara</h2>
        <a
          href="#appearance-preview-light"
          onClick={(event) => event.preventDefault()}
        >
          Enlace de ejemplo
        </a>
        <button type="button">Botón de ejemplo</button>
        <p>Los cambios de esta muestra aún no están guardados.</p>
      </section>
      <section
        aria-label="Vista previa oscura"
        data-theme="dark"
        style={{ "--accent": preview.accentDark } as CSSProperties}
      >
        <h2>Vista previa oscura</h2>
        <a
          href="#appearance-preview-dark"
          onClick={(event) => event.preventDefault()}
        >
          Enlace de ejemplo
        </a>
        <button type="button">Botón de ejemplo</button>
      </section>
    </form>
  );
}
