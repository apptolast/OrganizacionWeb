package com.apptolast.organization.domain;

import java.util.List;

public record BlockChangePage(List<BlockChangeReceipt> items, BlockChangePosition next) {}
