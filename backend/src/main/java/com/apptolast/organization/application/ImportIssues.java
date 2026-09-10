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
 * Trae las issues abiertas del proyecto conectado y las convierte en tareas del proyecto indicado.
 * Importar es bajo demanda y de un solo sentido: nada vuelve al gestor externo.
 *
 * <p>El caso de uso no sabe qué gestor hay al otro lado. Recibe una implementación de {@link
 * IssueConnections} y otra de {@link IssueSource} por gestor, así que hoy sirve a GitHub sin
 * nombrarlo y otro gestor no añadiría ninguna rama aquí.
 *
 * <p>Las precondiciones se evalúan en un orden fijo —conexión, validez de la conexión, proyecto,
 * estado del proyecto y sólo entonces la exclusión mutua— para que la respuesta no dependa de en
 * qué orden fallan varias a la vez, ni delate proyectos ajenos a quien todavía no ha conectado.
 *
 * <p>Cada issue se confirma por separado con su tarea, su evento y su enlace. Una issue inservible
 * cuenta como fallida y no detiene a las demás; un fallo del almacén sí detiene la importación,
 * porque a partir de ahí ya no se puede garantizar que tarea, evento y enlace vayan juntos.
 */
public final class ImportIssues implements ImportIssuesUseCase {
  static final Duration ABANDONED_AFTER = Duration.ofMinutes(15);
  private static final int MAX_PAGES = 2;
  private static final String COMPLETED_PROJECT = "completed";

  /** El fallo no vino del gestor externo, así que no hay código HTTP suyo que anotar. */
  private static final int NOT_THE_PROVIDER = 0;

  /**
   * Ninguna clave del llavero abre el texto cifrado guardado: la del servidor cambió. Es el mismo
   * código que el adaptador HTTP traduce a 503.
   */
  private static final String KEY_MISMATCH = "CONNECTOR_KEY_MISMATCH";

  private final IssueConnections connections;
  private final IssueImportReceiptStore receipts;
  private final ProjectQueries projects;
  private final IssueSource source;
  private final ImportedTaskCommit commit;
  private final SecretCipher cipher;
  private final ConnectorAudit audit;
  private final Clock clock;

  public ImportIssues(
      IssueConnections connections,
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
    if (!connection.valid()) throw new ConnectionInvalidException();
    requireWritableProject(ownerId, projectId);
    var startedAt = now();
    var receipt =
        receipts.begin(
            ownerId,
            projectId,
            connections.source(),
            connection.projectPath(),
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
      String ownerId, UUID projectId, IssueConnection connection, IssueImportReceipt receipt) {
    var tally = new Tally();
    try {
      boolean truncated = collect(ownerId, projectId, connection, receipt, tally);
      var closed =
          receipts.finish(
              ownerId, receipt.id(), IssueImportReceipt.COMPLETED, null, truncated, now());
      audit.importFinished(
          connections.source(),
          ownerId,
          connection.projectPath(),
          closed.id(),
          closed.created(),
          closed.skipped(),
          closed.failed(),
          closed.truncated());
      return closed;
    } catch (IssueSourceException error) {
      throw giveUp(ownerId, connection, receipt, error);
    } catch (ProjectCompletedException error) {
      throw failed(ownerId, connection, receipt, "PROJECT_COMPLETED", 0, NOT_THE_PROVIDER);
    } catch (StorageUnavailableException error) {
      throw failed(ownerId, connection, receipt, "STORAGE_UNAVAILABLE", 0, NOT_THE_PROVIDER);
    } catch (SecretUndecipherableException error) {
      // El recibo ya está insertado como en curso, así que hay que cerrarlo antes de propagar: un
      // fallo de la clave del servidor no puede dejar al propietario con una importación fantasma
      // que bloquee las siguientes quince minutos con un IMPORT_IN_PROGRESS —un 409 mentiroso—
      // que no corresponde a ninguna importación viva. La excepción sigue subiendo tal cual para
      // que el adaptador la traduzca a 503 CONNECTOR_KEY_MISMATCH.
      failed(ownerId, connection, receipt, KEY_MISMATCH, 0, NOT_THE_PROVIDER);
      throw error;
    }
  }

  /**
   * Un token rechazado deja la conexión inservible; cualquier otro fallo del gestor sólo se anota,
   * porque el token sigue valiendo y volver a intentarlo es lo razonable.
   */
  private IssueImportFailedException giveUp(
      String ownerId,
      IssueConnection connection,
      IssueImportReceipt receipt,
      IssueSourceException error) {
    var errorCode = ConnectorFailures.importErrorCode(error);
    var at = now();
    if (error.reason() == IssueSourceException.Reason.TOKEN_REJECTED)
      connections.invalidate(ownerId, errorCode, at);
    else connections.recordFailure(ownerId, errorCode, at);
    return failed(
        ownerId, connection, receipt, errorCode, error.retryAfterSeconds(), error.providerStatus());
  }

  /** Lee como mucho dos páginas y devuelve si el gestor anuncia todavía más. */
  private boolean collect(
      String ownerId,
      UUID projectId,
      IssueConnection connection,
      IssueImportReceipt receipt,
      Tally tally) {
    // Un secreto ilegible es SecretUndecipherableException. El adaptador HTTP la traduce a 503
    // CONNECTOR_KEY_MISMATCH, así que una clave rotada da un fallo honesto y no un 500.
    var token =
        cipher
            .decrypt(ownerId, connection.tokenCiphertext())
            .orElseThrow(SecretUndecipherableException::new);
    boolean more = false;
    for (int page = 1; page <= MAX_PAGES; page++) {
      var listed = source.list(connection.reference(), token, page);
      more = listed.more();
      // Lo que el adaptador descartó por no ser trabajo planificable ya está decidido: se cuenta
      // como omitido igual que una issue que ya tenía enlace.
      tally.skipped += listed.excluded();
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
            connections.source(),
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
      IssueConnection connection,
      IssueImportReceipt receipt,
      String errorCode,
      int retryAfterSeconds,
      int providerStatus) {
    var closed =
        receipts.finish(ownerId, receipt.id(), IssueImportReceipt.FAILED, errorCode, false, now());
    audit.importFailed(
        connections.source(),
        ownerId,
        connection.projectPath(),
        closed.id(),
        errorCode,
        closed.created(),
        providerStatus);
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
