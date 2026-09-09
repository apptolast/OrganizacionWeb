import { RouteLink } from "./navigation";

/**
 * Índice de integraciones. Existe para que cada conector tenga dónde vivir sin que el menú
 * principal crezca una entrada por cada uno.
 */
export function IntegrationsIndex() {
  return (
    <main id="proyectos" tabIndex={-1}>
      <h1>Integraciones</h1>
      <p>Conecta OrganizaciónWeb con las herramientas que ya usas.</p>
      <ul>
        <li>
          <RouteLink href="/integraciones/github">Conector de GitHub</RouteLink>
          <p>
            Trae las issues abiertas de un repositorio como tareas de un
            proyecto propio.
          </p>
        </li>
        <li>
          <RouteLink href="/integraciones/api">
            API para integraciones
          </RouteLink>
          <p>
            Credenciales personales para que otros programas lean tus datos.
          </p>
        </li>
      </ul>
    </main>
  );
}
