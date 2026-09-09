package com.apptolast.organization.application;

import com.apptolast.organization.domain.AutomationEvent;

/** One outbox row offered to the worker: the event as the rules see it, plus its blocked flag. */
public record AutomationCandidate(AutomationEvent event, boolean blocked) {}
