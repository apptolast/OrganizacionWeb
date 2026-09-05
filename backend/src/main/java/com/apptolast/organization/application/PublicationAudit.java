package com.apptolast.organization.application;

import com.apptolast.organization.domain.PublicationAttempt;

public interface PublicationAudit {
  void event(PublicationAttempt attempt);

  void workerError(String code);
}
