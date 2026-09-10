import { RouteLink } from "./navigation";

/**
 * Índice de integraciones. Existe para que cada integración tenga dónde vivir sin que el menú
 * principal crezca una entrada por cada una.
 */
export function IntegrationsIndex() {
  return (
    <main id="proyectos" tabIndex={-1}>
      <h1>Integraciones</h1>
      <p>Conecta OrganizaciónWeb con las herramientas que ya usas.</p>
      <ul>
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
