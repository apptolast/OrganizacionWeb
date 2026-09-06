package com.apptolast.organization.application;

import com.apptolast.organization.domain.PlannedBlock;

public record BlockChange(PlannedBlock block, BlockPlanned event) {}
