package com.apptolast.organization.application;

@FunctionalInterface
public interface PublishOutboxUseCase {
  void runCycle();
}
