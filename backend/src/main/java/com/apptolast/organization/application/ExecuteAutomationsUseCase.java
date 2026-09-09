package com.apptolast.organization.application;

/** One pass of the worker over every owner that has rules. */
@FunctionalInterface
public interface ExecuteAutomationsUseCase {
  void runCycle();
}
