package com.apptolast.organization.application;

public final class ProjectConflictException extends RuntimeException {
  public ProjectConflictException() {
    super("El proyecto tiene una versión más reciente.");
  }
}
