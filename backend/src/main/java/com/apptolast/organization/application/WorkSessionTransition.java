package com.apptolast.organization.application;

public record WorkSessionTransition(WorkSessionTransitionReceipt receipt, WorkSessionStateChanged event) {}
