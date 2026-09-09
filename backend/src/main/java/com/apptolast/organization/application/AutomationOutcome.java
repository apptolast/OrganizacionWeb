package com.apptolast.organization.application;

import com.apptolast.organization.domain.AutomationRun;

/**
 * One rule's verdict on one event. The run carries null createdTaskId and deliveryId: the adapter
 * fills them in with what it actually created inside the same confirmation.
 */
public record AutomationOutcome(AutomationRun run, AutomationEffect effect) {}
