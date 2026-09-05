package com.apptolast.organization.adapter.config;

import com.apptolast.organization.application.PublishOutboxUseCase;

public final class PublisherSchedule {
  private final PublishOutboxUseCase publisher;

  public PublisherSchedule(PublishOutboxUseCase publisher) {
    this.publisher = publisher;
  }

  @org.springframework.scheduling.annotation.Scheduled(fixedDelay = 1000, initialDelay = 1000)
  public void tick() {
    publisher.runCycle();
  }
}
