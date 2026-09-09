package com.apptolast.organization.application;

import com.apptolast.organization.domain.AutomationEvent;
import com.apptolast.organization.domain.AutomationRun;

/** A run left in retry, together with the event it has to be attempted on again. */
public record AutomationRetry(AutomationRun run, AutomationEvent event) {}
