package com.apptolast.organization.application;

/** Decide si se puede abrir una conexión saliente hacia un host. */
@FunctionalInterface
public interface OutboundGuard {
  enum Verdict {
    ALLOWED,
    UNRESOLVABLE,
    BLOCKED
  }

  Verdict check(String host);
}
