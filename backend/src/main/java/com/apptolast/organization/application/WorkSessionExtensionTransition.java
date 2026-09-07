package com.apptolast.organization.application;

public record WorkSessionExtensionTransition(
    WorkSessionTransitionReceipt receipt, WorkSessionExtended event) {}
