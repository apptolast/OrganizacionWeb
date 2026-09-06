import { useEffect, useState, useRef, type FormEvent } from "react";
import { setCsrfToken, observeAccess, isCsrfFailure } from "./api-client";
import { readSession, type Session } from "./session-api";
export function useSession() {
  const channel = useRef<BroadcastChannel | null>(null);
  const operation = useRef<AbortController | null>(null);
  const request = useRef<AbortController | null>(null);
  const [closeError, setCloseError] = useState(false);
  const [closing, setClosing] = useState(false);
  const [failure, setFailure] = useState(false);
  const [session, setSession] = useState<Session>();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [saving, setSaving] = useState(false);
  const [loginError, setLoginError] = useState("");
  const [csrfExpired, setCsrfExpired] = useState(false);
  const [recoveryFailed, setRecoveryFailed] = useState(false);
  async function refresh(preservePrivate = false) {
    request.current?.abort();
    const controller = new AbortController();
    request.current = controller;
    setFailure(false);
    setRecoveryFailed(false);
    try {
      const next = await readSession(controller.signal);
      if (controller.signal.aborted) return;
      if (
        next.authenticated &&
        !isPrivateRoute(window.location.pathname, window.location.search)
      ) {
        window.history.replaceState(null, "", "/");
        window.dispatchEvent(new PopStateEvent("popstate"));
      }
      setCsrfToken(next.csrfToken);
      setSession(next);
      setCsrfExpired(false);
      return next;
    } catch {
      if (!controller.signal.aborted) {
        if (preservePrivate) setRecoveryFailed(true);
        else {
          setSession(undefined);
          setCsrfToken();
          setFailure(true);
        }
      }
    }
  }
  useEffect(() => {
    channel.current = new BroadcastChannel("organization-session");
    function revoke() {
      operation.current?.abort();
      setPassword("");
      setSaving(false);
      setClosing(false);
      setCloseError(false);
      setSession(undefined);
      setCsrfToken();
      void refresh();
    }
    channel.current.onmessage = (event) => {
      if (event.data === "logout") revoke();
    };
    function checkVisible() {
      if (document.visibilityState === "visible") void refresh();
    }
    document.addEventListener("visibilitychange", checkVisible);
    observeAccess((status) => {
      if (status === 403) {
        setCsrfExpired(true);
        return;
      }
      revoke();
    });
    void refresh();
    return () => {
      operation.current?.abort();
      request.current?.abort();
      setCsrfToken();
      observeAccess();
      channel.current?.close();
      document.removeEventListener("visibilitychange", checkVisible);
    };
  }, []);
  async function login(event: FormEvent) {
    event.preventDefault();
    if (saving) return;
    const controller = new AbortController();
    operation.current = controller;
    setSaving(true);
    setLoginError("");
    try {
      const response = await fetch("/api/session", {
        signal: controller.signal,
        method: "POST",
        credentials: "same-origin",
        headers: {
          "Content-Type": "application/x-www-form-urlencoded",
          "X-CSRF-TOKEN": session!.csrfToken,
        },
        body: new URLSearchParams({ username, password }).toString(),
      });
      if (controller.signal.aborted) return;
      setPassword("");
      if (response.status !== 204) throw response;
      await refresh();
    } catch (error) {
      if (controller.signal.aborted) return;
      const invalidCsrf = await isCsrfFailure(error);
      if (controller.signal.aborted) return;
      setCsrfExpired(invalidCsrf);
      setLoginError(
        error instanceof Response && error.status === 401
          ? "Usuario o contraseña incorrectos."
          : "No podemos confirmar el acceso. Inténtalo de nuevo cuando el servicio esté disponible.",
      );
    } finally {
      if (!controller.signal.aborted) {
        setPassword("");
        setSaving(false);
      }
    }
  }
  async function logout() {
    if (closing) return;
    const controller = new AbortController();
    operation.current = controller;
    setClosing(true);
    setCloseError(false);
    try {
      const response = await fetch("/api/session/logout", {
        signal: controller.signal,
        method: "POST",
        credentials: "same-origin",
        headers: { "X-CSRF-TOKEN": session!.csrfToken },
      });
      if (controller.signal.aborted) return;
      if (response.status === 401) {
        const current = await refresh(true);
        if (!controller.signal.aborted && (!current || current.authenticated))
          setCloseError(true);
        return;
      }
      if (response.status !== 204) throw response;
      setSession(undefined);
      channel.current?.postMessage("logout");
      await refresh();
    } catch (error) {
      if (controller.signal.aborted) return;
      const invalidCsrf = await isCsrfFailure(error);
      if (controller.signal.aborted) return;
      setCsrfExpired(invalidCsrf);
      setCloseError(true);
    } finally {
      if (!controller.signal.aborted) setClosing(false);
    }
  }
  return {
    closeError,
    closing,
    failure,
    session,
    username,
    setUsername,
    password,
    setPassword,
    saving,
    loginError,
    refresh,
    login,
    logout,
    csrfExpired,
    recoveryFailed,
  };
}
function isPrivateRoute(path: string, search: string) {
  if (path === "/proyectos") {
    const query = new URLSearchParams(search);
    return (
      !search ||
      (query.size === 1 && query.has("cursor") && Boolean(query.get("cursor")))
    );
  }
  return (
    !search &&
    (path === "/" ||
      /^\/proyectos\/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}(?:\/editar)?$/i.test(
        path,
      ))
  );
}
