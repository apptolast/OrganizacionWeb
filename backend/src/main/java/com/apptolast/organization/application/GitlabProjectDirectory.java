package com.apptolast.organization.application;

/**
 * Puerto de salida que comprueba credenciales y proyecto contra GitLab. Se separa de {@link
 * IssueSource} a propósito: importar sólo necesita listar issues, y el caso de uso compartido de
 * importación no debe poder verificar proyectos de nadie.
 */
public interface GitlabProjectDirectory {
  /** Devuelve el proyecto, o lanza {@link IssueSourceException} ya clasificada por el adaptador. */
  GitlabProject verify(String projectPath, String token);
}
