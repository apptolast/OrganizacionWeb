// @ts-nocheck
function stryNS_9fa48() {
  var g = typeof globalThis === 'object' && globalThis && globalThis.Math === Math && globalThis || new Function("return this")();
  var ns = g.__stryker__ || (g.__stryker__ = {});
  if (ns.activeMutant === undefined && g.process && g.process.env && g.process.env.__STRYKER_ACTIVE_MUTANT__) {
    ns.activeMutant = g.process.env.__STRYKER_ACTIVE_MUTANT__;
  }
  function retrieveNS() {
    return ns;
  }
  stryNS_9fa48 = retrieveNS;
  return retrieveNS();
}
stryNS_9fa48();
function stryCov_9fa48() {
  var ns = stryNS_9fa48();
  var cov = ns.mutantCoverage || (ns.mutantCoverage = {
    static: {},
    perTest: {}
  });
  function cover() {
    var c = cov.static;
    if (ns.currentTestId) {
      c = cov.perTest[ns.currentTestId] = cov.perTest[ns.currentTestId] || {};
    }
    var a = arguments;
    for (var i = 0; i < a.length; i++) {
      c[a[i]] = (c[a[i]] || 0) + 1;
    }
  }
  stryCov_9fa48 = cover;
  cover.apply(null, arguments);
}
function stryMutAct_9fa48(id) {
  var ns = stryNS_9fa48();
  function isActive(id) {
    if (ns.activeMutant === id) {
      if (ns.hitCount !== void 0 && ++ns.hitCount > ns.hitLimit) {
        throw new Error('Stryker: Hit count limit reached (' + ns.hitCount + ')');
      }
      return true;
    }
    return false;
  }
  stryMutAct_9fa48 = isActive;
  return isActive(id);
}
import type { ReactNode } from "react";
import { RouteLink } from "./navigation";
export function Workspace({
  children,
  sessionControls,
  section
}: {
  children: ReactNode;
  sessionControls?: ReactNode;
  section: "Hoy" | "Proyectos" | "Disponibilidad" | "Historial" | "Revisión semanal" | "Apariencia" | "Exportación" | "Calendario" | "Calendario externo" | "Importación" | "Conectores" | "API para integraciones" | "Webhooks" | "Automatizaciones" | null;
}) {
  return <div className="workspace">
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
          <RouteLink href="/" aria-current={section === "Hoy" ? "page" : undefined}>
            <span aria-hidden="true">◉</span> Hoy
            {section === "Hoy" && <span className="nav-dot" aria-hidden="true" />}
          </RouteLink>
          <RouteLink href="/proyectos" aria-current={section === "Proyectos" ? "page" : undefined}>
            <span aria-hidden="true">▦</span> Proyectos{" "}
            {section === "Proyectos" && <span className="nav-dot" aria-hidden="true" />}
          </RouteLink>
          <RouteLink href="/disponibilidad" aria-current={section === "Disponibilidad" ? "page" : undefined}>
            <span aria-hidden="true">◷</span> Disponibilidad
            {section === "Disponibilidad" && <span className="nav-dot" aria-hidden="true" />}
          </RouteLink>
          <RouteLink href="/historial" aria-current={section === "Historial" ? "page" : undefined}>
            Historial
          </RouteLink>
          <RouteLink href="/revision-semanal" aria-current={section === "Revisión semanal" ? "page" : undefined}>
            Revisión semanal
          </RouteLink>
          <RouteLink href="/calendario-externo" aria-current={section === "Calendario externo" ? "page" : undefined}>
            Calendario externo
          </RouteLink>
          <RouteLink href="/apariencia" aria-current={section === "Apariencia" ? "page" : undefined}>
            Apariencia
          </RouteLink>
          <RouteLink href="/exportacion" aria-current={section === "Exportación" ? "page" : undefined}>
            Exportación
          </RouteLink>
          <RouteLink href="/calendario" aria-current={section === "Calendario" ? "page" : undefined}>
            Calendario
          </RouteLink>
          <RouteLink href="/importacion" aria-current={section === "Importación" ? "page" : undefined}>
            <span aria-hidden="true">↑</span> Importación
            {section === "Importación" && <span className="nav-dot" aria-hidden="true" />}
          </RouteLink>
          <RouteLink href="/conectores" aria-current={section === "Conectores" ? "page" : undefined}>
            Conectores
            {section === "Conectores" && <span className="nav-dot" aria-hidden="true" />}
          </RouteLink>
          <RouteLink href="/integraciones/api" aria-current={section === "API para integraciones" ? "page" : undefined}>
            API para integraciones
          </RouteLink>
          <RouteLink href="/webhooks" aria-current={section === "Webhooks" ? "page" : undefined}>
            Webhooks
          </RouteLink>
          <RouteLink href="/automatizaciones" aria-current={(stryMutAct_9fa48("898") ? section !== "Automatizaciones" : stryMutAct_9fa48("897") ? false : stryMutAct_9fa48("896") ? true : (stryCov_9fa48("896", "897", "898"), section === (stryMutAct_9fa48("899") ? "" : (stryCov_9fa48("899"), "Automatizaciones")))) ? stryMutAct_9fa48("900") ? "" : (stryCov_9fa48("900"), "page") : undefined}>
            Automatizaciones
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
    </div>;
}