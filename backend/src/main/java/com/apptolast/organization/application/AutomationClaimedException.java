package com.apptolast.organization.application;

/**
 * Another worker already owns this (rule, event). Nothing of the confirmation lands, and nothing is
 * recorded either: the run that does exist is the other worker's, and it is the only one allowed.
 */
public final class AutomationClaimedException extends RuntimeException {
  public AutomationClaimedException() {
    super("Otro worker ya registró esta ejecución.");
  }
}
