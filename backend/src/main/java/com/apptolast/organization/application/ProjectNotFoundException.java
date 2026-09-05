package com.apptolast.organization.application;

public final class ProjectNotFoundException extends RuntimeException {
  public ProjectNotFoundException() {
    super("Proyecto no encontrado");
  }
}
