package com.apptolast.organization.application;

import com.apptolast.organization.domain.SessionStart;

public record WorkSessionConfirmation(SessionStart session, boolean replayed) {}
