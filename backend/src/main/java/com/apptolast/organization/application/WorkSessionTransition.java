package com.apptolast.organization.application;

public record WorkSessionTransition(
    WorkSessionTransitionReceipt receipt,
    WorkSessionStateChanged event,
    WorkSessionClosed closedEvent) {
  public WorkSessionTransition(
      WorkSessionTransitionReceipt receipt, WorkSessionStateChanged event) {
    this(receipt, event, null);
  }
}
