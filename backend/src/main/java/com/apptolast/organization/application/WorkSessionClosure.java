package com.apptolast.organization.application;

import java.time.LocalDate;

public record WorkSessionClosure(
    String progressNote, String nextStep, LocalDate workDate, String closeZoneId) {}
