package com.apptolast.organization.application;

import com.apptolast.organization.domain.*;

public record BlockMutation(BlockState state, BlockChangeReceipt receipt, BlockChanged event) {}
