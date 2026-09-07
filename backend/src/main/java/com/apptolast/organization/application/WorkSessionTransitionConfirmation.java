package com.apptolast.organization.application;

public record WorkSessionTransitionConfirmation(
    WorkSessionTransitionReceipt receipt, boolean replayed) {}
