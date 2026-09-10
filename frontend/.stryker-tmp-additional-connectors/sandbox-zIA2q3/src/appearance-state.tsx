// @ts-nocheck
import {
  createContext,
  useContext,
  useEffect,
  useLayoutEffect,
  useState,
  useRef,
  useCallback,
  type ReactNode,
} from "react";
import {
  readAppearance,
  saveAppearance,
  AppearanceValidationError,
  type AppearanceInput,
  type AppearanceSnapshot,
} from "./appearance-api";

type AppearanceState = {
  snapshot?: AppearanceSnapshot;
  failed: boolean;
  reading: boolean;
  uncertain: boolean;
  reload: () => Promise<AppearanceSnapshot>;
  refreshAfterImport: () => Promise<AppearanceSnapshot>;
  save: (input: AppearanceInput) => Promise<AppearanceSnapshot>;
};
const AppearanceContext = createContext<AppearanceState>({
  failed: false,
  reading: true,
  uncertain: false,
  reload: async () => {
    throw new Error("Apariencia no disponible");
  },
  refreshAfterImport: async () => {
    throw new Error("Apariencia no disponible");
  },
  save: async () => {
    throw new Error("Consulta la apariencia antes de guardar");
  },
});
export const useAppearance = () => useContext(AppearanceContext);
// La barra del navegador sigue al lienzo real, aunque la preferencia contradiga al OS.
function paintThemeColor() {
  const canvas = getComputedStyle(document.documentElement)
    .getPropertyValue("--canvas")
    .trim();
  if (!canvas) return;
  for (const meta of document.querySelectorAll<HTMLMetaElement>(
    'meta[name="theme-color"]',
  ))
    meta.content = canvas;
}
export function AppearanceProvider({ children }: { children: ReactNode }) {
  const [snapshot, setSnapshot] = useState<AppearanceSnapshot>();
  const [failed, setFailed] = useState(false);
  const [reading, setReading] = useState(true);
  const [uncertain, setUncertain] = useState(false);
  const readRequest = useRef<AbortController | null>(null);
  const writeRequest = useRef<AbortController | null>(null);
  const retireRead = () => {
    readRequest.current?.abort();
    readRequest.current = null;
    setReading(false);
  };
  const retryInitiator = useRef<HTMLElement | null>(null);
  useLayoutEffect(() => {
    const control = retryInitiator.current;
    if (control && !control.isConnected) {
      retryInitiator.current = null;
      if (document.activeElement === document.body) {
        const heading = document.querySelector<HTMLElement>("main h1");
        if (heading) {
          heading.tabIndex = -1;
          heading.focus();
        }
      }
    }
  });
  const save = async (input: AppearanceInput) => {
    if (!snapshot || writeRequest.current || uncertain)
      throw new Error("Consulta la apariencia antes de guardar");
    retireRead();
    const controller = new AbortController();
    writeRequest.current = controller;
    try {
      const next = await saveAppearance(
        input,
        snapshot.etag,
        controller.signal,
      );
      controller.signal.throwIfAborted();
      retireRead();
      setSnapshot(next);
      return next;
    } catch (error) {
      if (
        !controller.signal.aborted &&
        !(error instanceof AppearanceValidationError)
      )
        setUncertain(true);
      throw error;
    } finally {
      if (writeRequest.current === controller) writeRequest.current = null;
    }
  };
  useEffect(() => () => writeRequest.current?.abort(), []);
  const load = useCallback(() => {
    if (readRequest.current)
      return Promise.reject(new Error("Consulta de apariencia en curso"));
    const controller = new AbortController();
    readRequest.current = controller;
    return readAppearance(controller.signal)
      .then((next) => {
        controller.signal.throwIfAborted();
        setSnapshot(next);
        setUncertain(false);
        return next;
      })
      .catch((error) => {
        if (!controller.signal.aborted) setFailed(true);
        throw error;
      })
      .finally(() => {
        if (readRequest.current === controller) {
          readRequest.current = null;
          setReading(false);
        }
      });
  }, []);
  const reload = () => {
    setFailed(false);
    setReading(true);
    return load();
  };
  useEffect(() => {
    if (!snapshot) return;
    const media =
      snapshot.theme === "SYSTEM"
        ? window.matchMedia?.("(prefers-color-scheme: dark)")
        : undefined;
    const apply = () => {
      // media solo existe cuando la preferencia es SYSTEM, así que consultarlo
      // ya implica esa rama: repetir la comprobación aquí sería redundante.
      const theme =
        snapshot.theme === "DARK" || media?.matches ? "dark" : "light";
      document.documentElement.dataset.theme = theme;
      document.documentElement.style.colorScheme = theme;
      paintThemeColor();
      document.documentElement.style.setProperty(
        "--accent",
        theme === "dark" ? snapshot.accentDark : snapshot.accentLight,
      );
    };
    apply();
    media?.addEventListener("change", apply);
    document.addEventListener("visibilitychange", apply);
    return () => {
      media?.removeEventListener("change", apply);
      document.removeEventListener("visibilitychange", apply);
      delete document.documentElement.dataset.theme;
      document.documentElement.style.removeProperty("color-scheme");
      document.documentElement.style.removeProperty("--accent");
      paintThemeColor();
    };
  }, [snapshot]);
  useEffect(() => {
    void load().catch(() => {});
    return () => {
      readRequest.current?.abort();
      readRequest.current = null;
    };
  }, [load]);
  return (
    <AppearanceContext
      value={{
        snapshot,
        failed,
        reading,
        uncertain,
        reload,
        save,
        refreshAfterImport: async () => {
          if (uncertain || writeRequest.current)
            throw new Error(
              "Recupera primero el guardado de apariencia pendiente",
            );
          return reload();
        },
      }}
    >
      {failed && !snapshot && (
        <div className="appearance-load-error">
          <p role="alert">
            No se pudo cargar la apariencia. Usamos temporalmente valores
            seguros.
          </p>
          <button
            onClick={(event) => {
              retryInitiator.current = event.currentTarget;
              void reload().catch(() => {});
            }}
          >
            Reintentar
          </button>
        </div>
      )}
      {children}
    </AppearanceContext>
  );
}
