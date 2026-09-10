package com.apptolast.organization.application;

/**
 * Puerto de salida hacia el gestor de issues. El caso de uso no conoce a GitHub: sólo este puerto,
 * {@link com.apptolast.organization.domain.ExternalIssue} y los códigos de {@link
 * IssueSourceException}. Otro gestor implementaría este mismo puerto sin tocar nada más.
 *
 * <p>Verificar credenciales no vive aquí: cada gestor identifica su proyecto a su manera y con su
 * propio puerto, y quien importa no tiene por qué poder verificar proyectos de nadie.
 */
public interface IssueSource {
  /** Página de issues abiertas, 1-indexada, cien por página. */
  IssuePage list(String projectReference, String token, int page);
}
