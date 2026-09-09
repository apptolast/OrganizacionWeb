package com.apptolast.organization.application;

/** Nombre canónico del repositorio y login de la cuenta dueña del token. */
public record RepositoryIdentity(String fullName, String login) {}
