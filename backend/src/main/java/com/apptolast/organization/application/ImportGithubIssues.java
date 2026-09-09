package com.apptolast.organization.application;

import static java.time.temporal.ChronoUnit.MICROS;

import com.apptolast.organization.domain.ExternalIssue;
import com.apptolast.organization.domain.IssueImportReceipt;
import com.apptolast.organization.domain.Task;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Trae las issues abiertas del repositorio conectado y las convierte en tareas del proyecto
 * indicado. Importar es bajo demanda y de un solo sentido: nada vuelve a GitHub.
 *
 * <p>Las precondiciones se evalúan en un orden fijo —conexión, validez de la conexión, proyecto,
 * estado del proyecto y sólo entonces la exclusión mutua— para que la respuesta no dependa de en
 * qué orden fallan varias a la vez, ni delate proyectos ajenos a quien todavía no ha conectado.
 *
 * <p>Cada issue se confirma por separado con su tarea, su evento y su enlace. Una issue inservible
 * cuenta como fallida y no detiene a las demás; un fallo del almacén sí detiene la importación,
 * porque a partir de ahí ya no se puede garantizar que tarea, evento y enlace vayan juntos.
 */
public final class ImportGithubIssues implements ImportGithubIssuesUseCase {
  public static final String SOURCE = "github";
  static final Duration ABANDONED_AFTER = Duration.ofMinutes(15);
  private static final int MAX_PAGES = 2;
  private static final String COMPLETED_PROJECT = "completed";

  /** El fallo no vino del gestor externo, así que no hay código HTTP suyo que anotar. */
  private static final int NOT_GITHUB = 0;

  private final ConnectorConnectionStore connections;
  private final IssueImportReceiptStore receipts;
  private final ProjectQueries projects;
  private final IssueSource source;
  private final ImportedTaskCommit commit;
  private final SecretCipher cipher;
  private final ConnectorAudit audit;
  private final Clock clock;

  public ImportGithubIssues(
      ConnectorConnectionStore connections,
      IssueImportReceiptStore receipts,
      ProjectQueries projects,
      IssueSource source,
      ImportedTaskCommit commit,
      SecretCipher cipher,
      ConnectorAudit audit,
      Clock clock) {
    this.connections = connections;
    this.receipts = receipts;
    this.projects = projects;
    this.source = source;
    this.commit = commit;
    this.cipher = cipher;
    this.audit = audit;
    this.clock = clock;
  }

  @Override
  public IssueImportReceipt execute(String ownerId, UUID projectId) {
    if (!cipher.enabled()) throw new ConnectorsDisabledException();
    var connection = connections.find(ownerId).orElseThrow(ConnectionNotFoundException::new);
    if (!connection.isValid()) throw new ConnectionInvalidException();
    requireWritableProject(ownerId, projectId);
    var startedAt = now();
    var receipt =
        receipts.begin(
            ownerId,
            projectId,
            connection.repository(),
            startedAt,
            startedAt.minus(ABANDONED_AFTER));
    return run(ownerId, projectId, connection, receipt);
  }

  private void requireWritableProject(String ownerId, UUID projectId) {
    var project =
        projects.find(ownerId, projectId).orElseThrow(ResourceNotFoundException::new).project();
    if (COMPLETED_PROJECT.equals(project.status())) throw new ProjectCompletedException();
  }

  private IssueImportReceipt run(
      String ownerId, UUID projectId, StoredConnection connection, IssueImportReceipt receipt) {
    var tally = new Tally();
    try {
      boolean truncated = collect(ownerId, projectId, connection, receipt, tally);
      var closed =
          receipts.finish(
              ownerId, receipt.id(), IssueImportReceipt.COMPLETED, null, truncated, now());
      audit.importFinished(
          ownerId,
          connection.repository(),
          closed.id(),
          closed.created(),
          closed.skipped(),
          closed.failed(),
          closed.truncated());
      return closed;
    } catch (IssueSourceException error) {
      if (error.reason() == IssueSourceException.Reason.TOKEN_REJECTED)
        connections.invalidate(ownerId);
      throw failed(
          ownerId,
          connection,
          receipt,
          ConnectorFailures.importErrorCode(error),
          error.retryAfterSeconds(),
          error.githubStatus());
    } catch (ProjectCompletedException error) {
      throw failed(ownerId, connection, receipt, "PROJECT_COMPLETED", 0, NOT_GITHUB);
    } catch (StorageUnavailableException error) {
      throw failed(ownerId, connection, receipt, "STORAGE_UNAVAILABLE", 0, NOT_GITHUB);
    }
  }

  /** Lee como mucho dos páginas y devuelve si el gestor anuncia todavía más. */
  private boolean collect(
      String ownerId,
      UUID projectId,
      StoredConnection connection,
      IssueImportReceipt receipt,
      Tally tally) {
    // El puerto unificado obliga a decidir: la 27 conserva su comportamiento previo, así que un
    // secreto ilegible sigue siendo SecretUndecipherableException. Hoy nadie la captura y acaba en
    // 500; queda anotado como defecto latente de la 27, ajeno a este carril.
    var token =
        cipher
            .decrypt(ownerId, connection.tokenCiphertext())
            .orElseThrow(SecretUndecipherableException::new);
    boolean more = false;
    for (int page = 1; page <= MAX_PAGES; page++) {
      var listed = source.list(connection.repository(), token, page);
      more = listed.more();
      for (var issue : listed.issues()) {
        absorb(ownerId, projectId, issue, tally);
        receipts.progress(ownerId, receipt.id(), tally.created, tally.skipped, tally.failed);
      }
      if (!listed.full()) break;
    }
    return more;
  }

  private void absorb(String ownerId, UUID projectId, ExternalIssue issue, Tally tally) {
    var title = issue.taskTitle();
    if (title.isEmpty()) {
      tally.failed++;
      return;
    }
    boolean created =
        commit.save(
            ownerId,
            projectId,
            issue,
            projectStatus -> newTask(ownerId, projectId, issue, title, projectStatus));
    if (created) tally.created++;
    else tally.skipped++;
  }

  private TaskCreation newTask(
      String ownerId, UUID projectId, ExternalIssue issue, String title, String projectStatus) {
    if (COMPLETED_PROJECT.equals(projectStatus)) throw new ProjectCompletedException();
    var task =
        Task.create(
            UUID.randomUUID(), projectId, title, issue.taskCompletionCriterion(), null, now());
    return new TaskCreation(
        task,
        new TaskCreated(
            UUID.randomUUID(),
            projectId,
            ownerId,
            task.createdAt(),
            1,
            "TaskCreated.v1",
            task.id(),
            task.title()));
  }

  private IssueImportFailedException failed(
      String ownerId,
      StoredConnection connection,
      IssueImportReceipt receipt,
      String errorCode,
      int retryAfterSeconds,
      int githubStatus) {
    var closed =
        receipts.finish(ownerId, receipt.id(), IssueImportReceipt.FAILED, errorCode, false, now());
    audit.importFailed(
        ownerId, connection.repository(), closed.id(), errorCode, closed.created(), githubStatus);
    return new IssueImportFailedException(closed, retryAfterSeconds);
  }

  private Instant now() {
    return clock.instant().truncatedTo(MICROS);
  }

  /** Contadores del recibo mientras la importación avanza. */
  private static final class Tally {
    private int created;
    private int skipped;
    private int failed;
  }
}
