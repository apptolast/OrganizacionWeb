package com.apptolast.organization.application;

import com.apptolast.organization.domain.ExternalIssue;
import java.util.List;

/**
 * Página de issues. {@code elements} cuenta lo que devolvió el gestor antes de descartar lo que no
 * es una issue, porque de ese número depende si se pide la página siguiente. {@code more} dice si
 * el gestor anuncia todavía más páginas, y de ahí sale {@code truncated} del recibo.
 */
public record IssuePage(List<ExternalIssue> issues, int elements, boolean more) {
  public static final int PAGE_SIZE = 100;

  public IssuePage {
    issues = List.copyOf(issues);
  }

  public boolean full() {
    return elements == PAGE_SIZE;
  }
}
