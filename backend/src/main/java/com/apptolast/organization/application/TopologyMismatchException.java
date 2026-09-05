package com.apptolast.organization.application;

public final class TopologyMismatchException extends RuntimeException {
  public TopologyMismatchException() {
    super("topology_mismatch");
  }
}
