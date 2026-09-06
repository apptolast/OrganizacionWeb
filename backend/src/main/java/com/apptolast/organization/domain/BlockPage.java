package com.apptolast.organization.domain;

import java.util.List;

public record BlockPage(List<PlannedBlock> items, BlockPosition next) {}
