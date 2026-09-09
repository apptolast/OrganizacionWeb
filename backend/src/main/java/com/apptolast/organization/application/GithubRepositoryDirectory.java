package com.apptolast.organization.application;

/**
 * Puerto de salida que comprueba credenciales y repositorio contra GitHub, hermano de {@link
 * GitlabProjectDirectory}. Devuelve el nombre canónico del repositorio y el login de la cuenta
 * dueña del token, o lanza {@link IssueSourceException} ya clasificada por el adaptador.
 */
public interface GithubRepositoryDirectory {
  RepositoryIdentity verify(String repository, String token);
}
