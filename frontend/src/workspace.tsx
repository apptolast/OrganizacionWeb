import type { ReactNode } from "react";
import { RouteLink } from "./navigation";
export function Workspace({
  children,
  sessionControls,
  section,
}: {
  children: ReactNode;
  sessionControls?: ReactNode;
  section:
    | "Hoy"
    | "Proyectos"
    | "Disponibilidad"
    | "Historial"
    | "Revisión semanal"
    | "Apariencia"
    | "Exportación"
    | "Calendario"
    | "Calendario externo"
    | "Importación"
    | "API para integraciones"
    | "Webhooks"
    | null;
}) {
  return (
    <div className="workspace">
      <a className="skip-link" href="#proyectos">
        Saltar al contenido
      </a>
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-mark" aria-hidden="true">
            o.
          </span>
          <span translate="no">
            Organization<span className="brand-light">Web</span>
          </span>
        </div>
        <p className="sidebar-label">TU ESPACIO</p>
        <nav aria-label="Principal">
          <RouteLink
            href="/"
            aria-current={section === "Hoy" ? "page" : undefined}
          >
            <span aria-hidden="true">◉</span> Hoy
            {section === "Hoy" && (
              <span className="nav-dot" aria-hidden="true" />
            )}
          </RouteLink>
          <RouteLink
            href="/proyectos"
            aria-current={section === "Proyectos" ? "page" : undefined}
          >
            <span aria-hidden="true">▦</span> Proyectos{" "}
            {section === "Proyectos" && (
              <span className="nav-dot" aria-hidden="true" />
            )}
          </RouteLink>
          <RouteLink
            href="/disponibilidad"
            aria-current={section === "Disponibilidad" ? "page" : undefined}
          >
            <span aria-hidden="true">◷</span> Disponibilidad
            {section === "Disponibilidad" && (
              <span className="nav-dot" aria-hidden="true" />
            )}
          </RouteLink>
          <RouteLink
            href="/historial"
            aria-current={section === "Historial" ? "page" : undefined}
          >
            Historial
          </RouteLink>
          <RouteLink
            href="/revision-semanal"
            aria-current={section === "Revisión semanal" ? "page" : undefined}
          >
            Revisión semanal
          </RouteLink>
          <RouteLink
            href="/calendario-externo"
            aria-current={section === "Calendario externo" ? "page" : undefined}
          >
            Calendario externo
          </RouteLink>
          <RouteLink
            href="/apariencia"
            aria-current={section === "Apariencia" ? "page" : undefined}
          >
            Apariencia
          </RouteLink>
          <RouteLink
            href="/exportacion"
            aria-current={section === "Exportación" ? "page" : undefined}
          >
            Exportación
          </RouteLink>
          <RouteLink
            href="/calendario"
            aria-current={section === "Calendario" ? "page" : undefined}
          >
            Calendario
          </RouteLink>
          <RouteLink
            href="/importacion"
            aria-current={section === "Importación" ? "page" : undefined}
          >
            <span aria-hidden="true">↑</span> Importación
            {section === "Importación" && (
              <span className="nav-dot" aria-hidden="true" />
            )}
          </RouteLink>
          <RouteLink
            href="/integraciones/api"
            aria-current={
              section === "API para integraciones" ? "page" : undefined
            }
          >
            API para integraciones
          </RouteLink>
          <RouteLink
            href="/webhooks"
            aria-current={section === "Webhooks" ? "page" : undefined}
          >
            Webhooks
          </RouteLink>
        </nav>
        <div className="sidebar-note">
          <span className="note-symbol" aria-hidden="true">
            ↗
          </span>
          <p>
            Las grandes cosas
            <br />
            empiezan con
            <br />
            <em>un pequeño paso.</em>
          </p>
          <span className="note-line" />
        </div>
        <div className="personal-space">
          <span className="personal-avatar" aria-hidden="true">
            P
          </span>
          <div>
            Espacio personal<small>A tu ritmo</small>
          </div>
        </div>
      </aside>
      <div className="page">
        <header className="topbar">
          <span>
            Mi espacio{" "}
            <span className="breadcrumb-separator" aria-hidden="true">
              /
            </span>{" "}
            <strong>{section ?? "Página no encontrada"}</strong>
          </span>
          <span className="space-badge">
            <span aria-hidden="true">●</span> Un paso cada día
          </span>
          {sessionControls}
        </header>
        {children}
      </div>
    </div>
  );
}
