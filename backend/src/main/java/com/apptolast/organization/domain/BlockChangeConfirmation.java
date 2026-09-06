package com.apptolast.organization.domain;

public record BlockChangeConfirmation(BlockChangeReceipt receipt, boolean replayed) {}
