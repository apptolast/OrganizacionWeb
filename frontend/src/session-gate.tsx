import { App } from "./App";
import { useSession } from "./use-session";
import { useLayoutEffect } from "react";
export function SessionGate() {
  const {
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
  } = useSession();
  useLayoutEffect(() => {
    if (document.activeElement === document.body) {
      const heading = document.querySelector<HTMLElement>("main h1");
      if (heading) {
        heading.tabIndex = -1;
        heading.focus();
      }
    }
  }, [session?.authenticated, closing, closeError, failure, saving]);
  if (session?.authenticated && !closing && !closeError && !failure)
    return (
      <>
        <div className="access-recovery" hidden={!csrfExpired}>
          <p role={csrfExpired ? "alert" : undefined}>
            {recoveryFailed ? "No hemos podido renovar el acceso. " : ""}La
            protección de tu sesión necesita renovarse. Recupera el acceso y
            decide si vuelves a enviar los cambios.
          </p>
          <button onClick={() => refresh(true)}>Recuperar acceso</button>
        </div>
        <App
          sessionControls={
            <button className="session-logout" onClick={logout}>
              Cerrar sesión
            </button>
          }
        />
      </>
    );
  return (
    <main className="session-screen">
      <div className="brand">
        <span className="brand-mark" aria-hidden="true">
          o.
        </span>
        <span translate="no">
          Organization<span className="brand-light">Web</span>
        </span>
      </div>
      <section className="session-card" aria-labelledby="session-heading">
        <p className="eyebrow">TU ESPACIO PERSONAL</p>
        <h1 id="session-heading">
          Tu espacio,
          <br />a tu ritmo.
        </h1>
        <p className="session-intro">
          Un lugar para ordenar tus ideas y avanzar un pequeño paso cada día.
        </p>
        {closing ? (
          <p role="status">Cerrando sesión</p>
        ) : closeError ? (
          <div>
            <p role="alert">
              No podemos confirmar el cierre. Tus datos están ocultos; vuelve a
              intentarlo cuando el servicio esté disponible.
            </p>
            <button onClick={logout}>Reintentar cierre</button>
          </div>
        ) : failure ? (
          <div>
            <p role="alert">No hemos podido comprobar el acceso.</p>
            <button onClick={() => refresh()}>Reintentar</button>
          </div>
        ) : !session ? (
          <p role="status">Comprobando acceso</p>
        ) : (
          <>
            <form onSubmit={login} aria-busy={saving}>
              <div className="field">
                <label htmlFor="username">Usuario</label>
                <input
                  id="username"
                  name="username"
                  autoComplete="username"
                  value={username}
                  readOnly={saving}
                  onChange={(e) => setUsername(e.target.value)}
                />
              </div>
              <div className="field">
                <label htmlFor="password">Contraseña</label>
                <input
                  id="password"
                  name="password"
                  type="password"
                  autoComplete="current-password"
                  value={password}
                  readOnly={saving}
                  onChange={(e) => setPassword(e.target.value)}
                />
              </div>
              <button disabled={saving}>Iniciar sesión</button>
            </form>
            {saving && <p role="status">Comprobando credenciales</p>}
            {loginError && <p role="alert">{loginError}</p>}
          </>
        )}
      </section>
      <p className="session-footer">
        Organizarte también es dejar espacio para descansar.
      </p>
      {csrfExpired && (
        <div className="session-recovery">
          <p role="alert">
            Recupera la protección de tu sesión antes de volver a intentarlo.
          </p>
          <button onClick={() => refresh(true)}>Recuperar acceso</button>
        </div>
      )}
    </main>
  );
}
