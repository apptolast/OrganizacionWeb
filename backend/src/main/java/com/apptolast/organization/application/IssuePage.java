package com.apptolast.organization.application;

import com.apptolast.organization.domain.ExternalIssue;
import java.util.List;

/**
 * Página de issues. {@code elements} cuenta lo que devolvió el gestor antes de descartar nada,
 * porque de ese número depende si se pide la página siguiente. {@code excluded} cuenta lo que el
 * adaptador descartó por no ser trabajo planificable, que el recibo declara como omitido. {@code
 * more} dice si el gestor anuncia todavía más páginas, y de ahí sale {@code truncated}.
 */
public record IssuePage(
    List<ExternalIssue> issues, int elements, int excluded, boolean more) {
  public static final int PAGE_SIZE = 100;

  public IssuePage {
    issues = List.copyOf(issues);
  }

  /** Página sin nada que descartar: la forma corriente para un gestor que no filtra. */
  public IssuePage(List<ExternalIssue> issues, int elements, boolean more) {
    this(issues, elements, 0, more);
  }

  public boolean full() {
    return elements == PAGE_SIZE;
  }
}
