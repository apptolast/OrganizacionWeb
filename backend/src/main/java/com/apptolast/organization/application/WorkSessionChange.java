package com.apptolast.organization.application;

import com.apptolast.organization.domain.SessionStart;

public record WorkSessionChange(SessionStart session, WorkSessionStarted event) {}
