package com.apptolast.organization.application;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @s1 el catálogo son seis filas en un orden fijo, pase lo que pase; @s3 sin clave, las que cifran
 *     salen deshabilitadas sin que nadie intente descifrarlas; @s5 el error es un código y un
 *     instante; @s6 leerlo no llama a nadie ni escribe; @s7 el almacenamiento caído no produce un
 *     catálogo optimista.
 */
class ReadConnectorCatalogTest {
  private static final String OWNER = "owner-1";
  private static final Instant AT = Instant.parse("2026-09-09T11:00:00Z");
  private static final List<String> ORDER =
      List.of(
          "api_credentials", "webhooks", "ics_calendar", "github", "external_calendar", "gitlab");

  private FakeSources sources;
  private ConnectorFakes.FakeCipher cipher;

  @BeforeEach
  void setUp() {
    sources = new FakeSources();
    cipher = new ConnectorFakes.FakeCipher();
  }

  private static ConnectorFakes.FakeCipher disabledCipher() {
    var off = new ConnectorFakes.FakeCipher();
    off.disable();
    return off;
  }

  private ReadConnectorCatalogUseCase catalog() {
    return new ReadConnectorCatalog(sources.all(), cipher);
  }

  /** Un origen de estado por conector, que anota a quién se le preguntó. */
  private static final class FakeSources {
    private final Map<String, ConnectorRow> rows = new java.util.HashMap<>();
    private final List<String> asked = new ArrayList<>();
    private final List<String> encrypting =
        List.of("webhooks", "github", "external_calendar", "gitlab");
    private RuntimeException failure;

    void answer(String id, ConnectorRow row) {
      rows.put(id, row);
    }

    void fail(RuntimeException error) {
      failure = error;
    }

    List<ConnectorStatusSource> all() {
      var built = new ArrayList<ConnectorStatusSource>();
      for (var id : ORDER) built.add(source(id));
      return built;
    }

    private ConnectorStatusSource source(String id) {
      return new ConnectorStatusSource() {
        @Override
        public String id() {
          return id;
        }

        @Override
        public boolean encryptsSecrets() {
          return encrypting.contains(id);
        }

        @Override
        public ConnectorRow read(String ownerId) {
          asked.add(id + ":" + ownerId);
          if (failure != null) throw failure;
          return rows.getOrDefault(id, ConnectorRow.notConnected(id));
        }
      };
    }
  }

  @Test
  void s1_thecatalogIsAlwaysTheSameSixRowsInTheSameOrder() {
    var catalog = catalog().execute(OWNER);

    assertEquals(ORDER, catalog.connectors().stream().map(ConnectorRow::id).toList());
  }

  @Test
  void s1_withoutAnyIntegrationTheSixRowsAreNotConnectedAndCarryNothingElse() {
    for (var row : catalog().execute(OWNER).connectors()) {
      assertEquals("not_connected", row.status());
      assertNull(row.lastActivityAt());
      assertNull(row.lastError());
    }
  }

  @Test
  void s2_eachRowShowsWhatItsOwnSourceAnswered() {
    sources.answer("webhooks", new ConnectorRow("webhooks", "connected", AT, null));
    sources.answer(
        "github",
        new ConnectorRow("github", "error", AT, new ConnectorError("CONNECTION_INVALID", AT)));

    var catalog = catalog().execute(OWNER);

    assertEquals("connected", catalog.row("webhooks").status());
    assertEquals(AT, catalog.row("webhooks").lastActivityAt());
    assertEquals("error", catalog.row("github").status());
    assertEquals("CONNECTION_INVALID", catalog.row("github").lastError().code());
    assertEquals(AT, catalog.row("github").lastError().at());
    assertEquals("not_connected", catalog.row("gitlab").status());
  }

  @Test
  void s4_everySourceIsAskedForTheAuthenticatedOwnerAndForNobodyElse() {
    catalog().execute(OWNER);

    assertEquals(ORDER.stream().map(id -> id + ":" + OWNER).toList(), sources.asked);
  }

  @Test
  void s3_withoutTheConnectorKeyTheOnesThatEncryptAreDisabledAndAreNotEvenAsked() {
    var disabled = new ReadConnectorCatalog(sources.all(), disabledCipher());

    var catalog = disabled.execute(OWNER);

    for (var id : List.of("webhooks", "github", "external_calendar", "gitlab")) {
      assertEquals("disabled", catalog.row(id).status(), id);
      assertNull(catalog.row(id).lastActivityAt(), id);
      assertNull(catalog.row(id).lastError(), id);
    }
    assertEquals(List.of("api_credentials:" + OWNER, "ics_calendar:" + OWNER), sources.asked);
  }

  @Test
  void s3_theOnesThatDoNotEncryptStillAnswerWithoutTheKey() {
    sources.answer("ics_calendar", new ConnectorRow("ics_calendar", "connected", null, null));
    var disabled = new ReadConnectorCatalog(sources.all(), disabledCipher());

    assertEquals("connected", disabled.execute(OWNER).row("ics_calendar").status());
  }

  @Test
  void s7_astorageFailureIsNotSwallowedIntoAnOptimisticCatalog() {
    sources.fail(new StorageUnavailableException(new RuntimeException("postgres")));

    assertThrows(StorageUnavailableException.class, () -> catalog().execute(OWNER));
  }

  @Test
  void s6_readingTheCatalogTwiceAsksTheSameAndAnswersTheSame() {
    sources.answer("gitlab", new ConnectorRow("gitlab", "connected", AT, null));

    var first = catalog().execute(OWNER);
    var second = catalog().execute(OWNER);

    assertEquals(first, second);
  }
}
