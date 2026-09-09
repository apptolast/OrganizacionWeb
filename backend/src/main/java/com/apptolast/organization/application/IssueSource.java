package com.apptolast.organization.application;

/**
 * Puerto de salida hacia el gestor de issues. El caso de uso no conoce GitHub: sólo este puerto,
 * {@link com.apptolast.organization.domain.ExternalIssue} y los códigos de
 * {@link IssueSourceException}. Un segundo conector implementa el mismo puerto sin tocar nada más.
 */
public interface IssueSource {
  /** Comprueba credenciales y repositorio, y devuelve el nombre canónico y el login. */
  RepositoryIdentity verify(String repository, String token);

  /** Página de issues abiertas, 1-indexada, cien por página. */
  IssuePage list(String repository, String token, int page);
}
