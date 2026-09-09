package com.apptolast.organization.application;

/**
 * La fila del conector de GitLab (29). No vuelve a derivar nada: pregunta a la misma lectura que
 * alimenta {@code GET /api/v1/me/connectors/gitlab} y se queda con tres de sus ocho campos. Así el
 * catálogo y la pantalla de detalle no pueden discrepar, que es justo lo que exigen @s14 y @s23.
 *
 * <p>Los cinco campos que se descartan —base de API, ruta del proyecto, identificador, pista del
 * token y versión— son precisamente los que @s5 prohíbe en el catálogo.
 */
public final class GitlabStatusSource implements ConnectorStatusSource {
  public static final String ID = "gitlab";

  private final ReadGitlabConnectionUseCase connection;

  public GitlabStatusSource(ReadGitlabConnectionUseCase connection) {
    this.connection = connection;
  }

  @Override
  public String id() {
    return ID;
  }

  @Override
  public boolean encryptsSecrets() {
    return true;
  }

  @Override
  public ConnectorRow read(String ownerId) {
    var view = connection.execute(ownerId);
    return new ConnectorRow(ID, view.status(), view.lastActivityAt(), view.lastError());
  }
}
