package com.apptolast.organization.application;

import com.apptolast.organization.domain.ProjectSnapshot;

public record ProjectChange(ProjectSnapshot snapshot, ProjectUpdated event) {}
