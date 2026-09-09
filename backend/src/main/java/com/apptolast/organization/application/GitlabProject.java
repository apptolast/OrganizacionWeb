package com.apptolast.organization.application;

/**
 * Proyecto tal y como GitLab lo identifica: el id numérico global de la instancia y la ruta
 * canónica. La ruta que guardamos es siempre la que responde GitLab, no la que escribió la persona.
 */
public record GitlabProject(long id, String pathWithNamespace) {}
