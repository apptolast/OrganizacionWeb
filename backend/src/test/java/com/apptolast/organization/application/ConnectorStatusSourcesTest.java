package com.apptolast.organization.application;

import static org.junit.jupiter.api.Assertions.*;

import com.apptolast.organization.domain.ApiCredential;
import com.apptolast.organization.domain.ApiCredentialPage;
import com.apptolast.organization.domain.ExternalCalendarSubscription;
import com.apptolast.organization.domain.FeedError;
import com.apptolast.organization.domain.IssueImportReceipt;
import com.apptolast.organization.domain.SyncStatus;
import com.apptolast.organization.domain.WebhookDelivery;
import com.apptolast.organization.domain.WebhookEndpoint;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * @s2 cada fila del catálogo deriva su estado de la fuente de su propia feature, sin inventar
 *     {@code connected}; @s3 sólo cuatro de las seis dependen de la clave de cifrado; @s5 el error
 *     publicado es un código estable y un instante, nunca el texto del proveedor.
 */
class ConnectorStatusSourcesTest {
  private static final String OWNER = "owner-1";
  private static final Instant NOW = Instant.parse("2026-09-10T09:00:00Z");
  private static final Instant EARLIER = Instant.parse("2026-09-09T08:00:00Z");
  private static final Instant LATER = Instant.parse("2026-09-10T08:30:00Z");
  private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

  // --------------------------------------------------------------- quién depende de la clave

  @ParameterizedTest
  @CsvSource({
    "api_credentials, false",
    "webhooks, true",
    "ics_calendar, false",
    "github, true",
    "external_calendar, true",
    "gitlab, true"
  })
  void s3_eachSourceDeclaresItsIdAndWhetherItDependsOnTheConnectorKey(String id, boolean encrypts) {
    var source = sourceOf(id);

    assertEquals(id, source.id());
    assertEquals(encrypts, source.encryptsSecrets());
  }

  private ConnectorStatusSource sourceOf(String id) {
    return switch (id) {
      case "api_credentials" -> new ApiCredentialStatusSource(new FakeCredentials(), CLOCK);
      case "webhooks" -> new WebhookStatusSource(new FakeEndpoints(), new FakeDeliveries());
      case "ics_calendar" -> new IcsCalendarStatusSource(new FakeFeedTokens());
      case "github" -> new GithubStatusSource(new FakeGithubConnections(), new FakeReceipts());
      case "external_calendar" -> new ExternalCalendarStatusSource(new FakeSubscriptions());
      default -> new GitlabStatusSource(owner -> GitlabConnectionView.notConnected());
    };
  }

  // ---------------------------------------------------------------------- 24 api_credentials

  @Test
  void s2_avigentCredentialMakesTheRowConnectedWithoutInventingActivity() {
    var credentials = new FakeCredentials();
    credentials.page(credential(NOW.plusSeconds(3600), null));

    var row = new ApiCredentialStatusSource(credentials, CLOCK).read(OWNER);

    assertEquals("connected", row.status());
    assertNull(row.lastActivityAt(), "24 no registra el uso: inventarlo sería mentir");
    assertNull(row.lastError());
  }

  @Test
  void s2_onlyRevokedCredentialsLeaveTheApiRowNotConnected() {
    var credentials = new FakeCredentials();
    credentials.page(credential(NOW.plusSeconds(3600), EARLIER));

    assertEquals(
        ConnectorRow.notConnected("api_credentials"),
        new ApiCredentialStatusSource(credentials, CLOCK).read(OWNER));
  }

  @Test
  void s2_anexpiredCredentialIsNotVigentEither() {
    var credentials = new FakeCredentials();
    credentials.page(credential(NOW.minusSeconds(1), null));

    assertEquals(
        "not_connected", new ApiCredentialStatusSource(credentials, CLOCK).read(OWNER).status());
  }

  @Test
  void s2_thecredentialsAreReadPageByPageUntilAvigentOneAppears() {
    var credentials = new FakeCredentials();
    credentials.page(credential(NOW.plusSeconds(3600), EARLIER));
    credentials.page(credential(NOW.plusSeconds(3600), null));

    assertEquals(
        "connected", new ApiCredentialStatusSource(credentials, CLOCK).read(OWNER).status());
    assertEquals(List.of(OWNER + ":null", OWNER + ":cursor-1"), credentials.asked);
  }

  @Test
  void s2_withoutAnyCredentialTheApiRowIsNotConnected() {
    assertEquals(
        "not_connected",
        new ApiCredentialStatusSource(new FakeCredentials(), CLOCK).read(OWNER).status());
  }

  // ------------------------------------------------------------------------------ 25 webhooks

  @Test
  void s2_anactiveEndpointWithAdeliveryIsConnectedAtTheLastDelivery() {
    var endpoints = new FakeEndpoints();
    var deliveries = new FakeDeliveries();
    var id = endpoints.active();
    deliveries.at(id, EARLIER);
    deliveries.at(id, LATER);

    var row = new WebhookStatusSource(endpoints, deliveries).read(OWNER);

    assertEquals("connected", row.status());
    assertEquals(LATER, row.lastActivityAt());
    assertNull(row.lastError());
  }

  @Test
  void s2_thelastDeliveryIsTheNewestAcrossEveryEndpoint() {
    var endpoints = new FakeEndpoints();
    var deliveries = new FakeDeliveries();
    var first = endpoints.active();
    var second = endpoints.active();
    deliveries.at(first, EARLIER);
    deliveries.at(second, LATER);

    assertEquals(
        LATER, new WebhookStatusSource(endpoints, deliveries).read(OWNER).lastActivityAt());
  }

  @Test
  void s2_everyEndpointDisabledByExhaustionIsAnErrorRowWithItsCodeAndInstant() {
    var endpoints = new FakeEndpoints();
    var deliveries = new FakeDeliveries();
    var id = endpoints.exhausted(LATER);
    deliveries.at(id, EARLIER);

    var row = new WebhookStatusSource(endpoints, deliveries).read(OWNER);

    assertEquals("error", row.status());
    assertEquals(EARLIER, row.lastActivityAt());
    assertEquals(new ConnectorError(WebhookEndpoint.DELIVERY_EXHAUSTED, LATER), row.lastError());
  }

  @Test
  void s2_oneSurvivingActiveEndpointStillCountsAsConnected() {
    var endpoints = new FakeEndpoints();
    endpoints.exhausted(LATER);
    endpoints.active();

    assertEquals(
        "connected", new WebhookStatusSource(endpoints, new FakeDeliveries()).read(OWNER).status());
  }

  @Test
  void s2_endpointsDisabledByHandAreNotAnError() {
    var endpoints = new FakeEndpoints();
    endpoints.disabledByHand();

    var row = new WebhookStatusSource(endpoints, new FakeDeliveries()).read(OWNER);

    assertEquals("not_connected", row.status());
    assertNull(row.lastError());
  }

  @Test
  void s2_withoutEndpointsTheWebhookRowIsNotConnectedAndNoDeliveryIsRead() {
    var deliveries = new FakeDeliveries();

    var row = new WebhookStatusSource(new FakeEndpoints(), deliveries).read(OWNER);

    assertEquals(ConnectorRow.notConnected("webhooks"), row);
    assertTrue(deliveries.asked.isEmpty());
  }

  // -------------------------------------------------------------------------- 26 ics_calendar

  @Test
  void s2_anactiveFeedTokenIsConnectedAndDeclaresNoActivity() {
    var tokens = new FakeFeedTokens();
    tokens.createdAt = EARLIER;

    var row = new IcsCalendarStatusSource(tokens).read(OWNER);

    assertEquals("connected", row.status());
    assertNull(row.lastActivityAt(), "un feed no registra cuándo se leyó");
    assertNull(row.lastError());
  }

  @Test
  void s2_arevokedFeedTokenLeavesTheIcsRowNotConnected() {
    assertEquals(
        ConnectorRow.notConnected("ics_calendar"),
        new IcsCalendarStatusSource(new FakeFeedTokens()).read(OWNER));
  }

  // -------------------------------------------------------------------------------- 27 github

  @Test
  void s2_avalidGithubConnectionWithAfinishedImportIsConnectedAtItsFinish() {
    var connections = new FakeGithubConnections();
    var receipts = new FakeReceipts();
    connections.stored =
        new StoredConnection("o/r", "octo", StoredConnection.VALID, new byte[1], EARLIER);
    receipts.put("github", receipt("completed", EARLIER, LATER));

    var row = new GithubStatusSource(connections, receipts).read(OWNER);

    assertEquals("connected", row.status());
    assertEquals(LATER, row.lastActivityAt());
    assertNull(row.lastError());
  }

  @Test
  void s2_avalidGithubConnectionWithoutImportsFallsBackToWhenItWasConnected() {
    var connections = new FakeGithubConnections();
    connections.stored =
        new StoredConnection("o/r", "octo", StoredConnection.VALID, new byte[1], EARLIER);

    assertEquals(
        EARLIER,
        new GithubStatusSource(connections, new FakeReceipts()).read(OWNER).lastActivityAt());
  }

  @Test
  void s2_aninvalidGithubConnectionIsAnErrorRowWithConnectionInvalid() {
    var connections = new FakeGithubConnections();
    var receipts = new FakeReceipts();
    connections.stored =
        new StoredConnection("o/r", "octo", StoredConnection.INVALID, new byte[1], EARLIER);
    receipts.put("github", receipt("failed", EARLIER, LATER));

    var row = new GithubStatusSource(connections, receipts).read(OWNER);

    assertEquals("error", row.status());
    assertEquals(LATER, row.lastActivityAt());
    assertEquals(new ConnectorError("CONNECTION_INVALID", LATER), row.lastError());
  }

  @Test
  void s2_arunningGithubImportCountsFromWhenItStarted() {
    var connections = new FakeGithubConnections();
    var receipts = new FakeReceipts();
    connections.stored =
        new StoredConnection("o/r", "octo", StoredConnection.VALID, new byte[1], EARLIER);
    receipts.put("github", receipt("running", LATER, null));

    assertEquals(LATER, new GithubStatusSource(connections, receipts).read(OWNER).lastActivityAt());
  }

  @Test
  void s2_animportOfTheOtherSourceIsNotGithubActivity() {
    var connections = new FakeGithubConnections();
    var receipts = new FakeReceipts();
    connections.stored =
        new StoredConnection("o/r", "octo", StoredConnection.VALID, new byte[1], EARLIER);
    receipts.put("gitlab", receipt("completed", EARLIER, LATER));

    assertEquals(
        EARLIER, new GithubStatusSource(connections, receipts).read(OWNER).lastActivityAt());
  }

  @Test
  void s2_withoutConnectionTheGithubRowIsNotConnected() {
    assertEquals(
        ConnectorRow.notConnected("github"),
        new GithubStatusSource(new FakeGithubConnections(), new FakeReceipts()).read(OWNER));
  }

  // ---------------------------------------------------------------------- 28 external_calendar

  @Test
  void s2_asubscriptionThatLastSyncedOkIsConnectedAtItsLastAttempt() {
    var store = new FakeSubscriptions();
    store.stored = subscription(SyncStatus.OK, null, LATER);

    var row = new ExternalCalendarStatusSource(store).read(OWNER);

    assertEquals("connected", row.status());
    assertEquals(LATER, row.lastActivityAt());
    assertNull(row.lastError());
  }

  @Test
  void s2_afailedSubscriptionPublishesItsFeedErrorCodeAtTheLastAttempt() {
    var store = new FakeSubscriptions();
    store.stored = subscription(SyncStatus.FAILED, FeedError.FEED_REJECTED, LATER);

    var row = new ExternalCalendarStatusSource(store).read(OWNER);

    assertEquals("error", row.status());
    assertEquals(LATER, row.lastActivityAt());
    assertEquals(new ConnectorError("FEED_REJECTED", LATER), row.lastError());
  }

  @Test
  void s2_asubscriptionNeverSyncedIsConnectedWithoutActivity() {
    var store = new FakeSubscriptions();
    store.stored = subscription(null, null, null);

    var row = new ExternalCalendarStatusSource(store).read(OWNER);

    assertEquals("connected", row.status());
    assertNull(row.lastActivityAt());
    assertNull(row.lastError());
  }

  @Test
  void s2_withoutSubscriptionTheExternalCalendarRowIsNotConnected() {
    assertEquals(
        ConnectorRow.notConnected("external_calendar"),
        new ExternalCalendarStatusSource(new FakeSubscriptions()).read(OWNER));
  }

  // -------------------------------------------------------------------------------- 29 gitlab

  @Test
  void s2_aconnectedGitlabWithoutImportsIsConnectedAtTheConnectionInstant() {
    var row = new GitlabStatusSource(owner -> view("connected", EARLIER, null)).read(OWNER);

    assertEquals("connected", row.status());
    assertEquals(EARLIER, row.lastActivityAt());
    assertNull(row.lastError());
  }

  @Test
  void s2_agitlabInErrorPublishesTheSameLastErrorAsItsOwnScreen() {
    var error = new ConnectorError("CONNECTION_INVALID", LATER);

    var row = new GitlabStatusSource(owner -> view("error", LATER, error)).read(OWNER);

    assertEquals("error", row.status());
    assertEquals(LATER, row.lastActivityAt());
    assertEquals(error, row.lastError());
  }

  @Test
  void s2_withoutConnectionTheGitlabRowIsNotConnected() {
    assertEquals(
        ConnectorRow.notConnected("gitlab"),
        new GitlabStatusSource(owner -> GitlabConnectionView.notConnected()).read(OWNER));
  }

  @Test
  void s4_thegitlabRowIsAskedForTheAuthenticatedOwnerAndForNobodyElse() {
    var asked = new ArrayList<String>();

    new GitlabStatusSource(
            owner -> {
              asked.add(owner);
              return GitlabConnectionView.notConnected();
            })
        .read(OWNER);

    assertEquals(List.of(OWNER), asked);
  }

  @Test
  void s5_thegitlabRowNeverCarriesTheApiBaseOrTheProjectPath() {
    var view =
        new GitlabConnectionView(
            "error",
            "https://gitlab.example.com/api/v4",
            "grupo/proyecto",
            4821L,
            "WXYZ",
            LATER,
            new ConnectorError("CONNECTION_INVALID", LATER),
            2L);

    var row = new GitlabStatusSource(owner -> view).read(OWNER);

    assertFalse(row.toString().contains("grupo/proyecto"));
    assertFalse(row.toString().contains("gitlab.example.com"));
    assertFalse(row.toString().contains("WXYZ"));
  }

  // ------------------------------------------------------------------------------- ayudantes

  private static GitlabConnectionView view(String status, Instant activity, ConnectorError error) {
    return new GitlabConnectionView(
        status, "base", "grupo/proyecto", 1L, "WXYZ", activity, error, 1L);
  }

  private static ApiCredential credential(Instant expiresAt, Instant revokedAt) {
    return new ApiCredential(
        UUID.randomUUID(), "clave", List.of("tasks:read"), EARLIER, expiresAt, revokedAt);
  }

  private static IssueImportReceipt receipt(String status, Instant startedAt, Instant finishedAt) {
    return new IssueImportReceipt(
        UUID.randomUUID(),
        "gitlab",
        UUID.randomUUID(),
        "grupo/proyecto",
        status,
        0,
        0,
        0,
        false,
        null,
        startedAt,
        finishedAt);
  }

  private static ExternalCalendarSubscription rawSubscription(
      SyncStatus status, FeedError error, Instant attemptAt) {
    return new ExternalCalendarSubscription(
        UUID.randomUUID(),
        "Equipo",
        "calendario.example",
        "abcd",
        attemptAt,
        status == SyncStatus.OK ? attemptAt : null,
        status,
        error,
        "Europe/Madrid",
        0,
        0,
        0,
        0,
        false,
        NOW);
  }

  private static StoredSubscription subscription(
      SyncStatus status, FeedError error, Instant attemptAt) {
    return new StoredSubscription(rawSubscription(status, error, attemptAt), new byte[1], 1L);
  }

  private static final class FakeCredentials implements ApiCredentialQueries {
    private final Deque<List<ApiCredential>> pages = new ArrayDeque<>();
    final List<String> asked = new ArrayList<>();
    private int served;

    void page(ApiCredential... items) {
      pages.add(List.of(items));
    }

    @Override
    public Optional<ApiCredential> find(String owner, UUID id) {
      return Optional.empty();
    }

    @Override
    public ApiCredentialPage list(String owner, String cursor) {
      asked.add(owner + ":" + cursor);
      if (pages.isEmpty()) return new ApiCredentialPage(List.of(), null);
      var items = pages.poll();
      served++;
      return new ApiCredentialPage(items, pages.isEmpty() ? null : "cursor-" + served);
    }
  }

  private static final class FakeEndpoints implements WebhookEndpoints {
    private final List<WebhookEndpoint> stored = new ArrayList<>();

    UUID active() {
      return add(WebhookEndpoint.ACTIVE, null, null);
    }

    UUID exhausted(Instant at) {
      return add(WebhookEndpoint.DISABLED, WebhookEndpoint.DELIVERY_EXHAUSTED, at);
    }

    UUID disabledByHand() {
      return add(WebhookEndpoint.DISABLED, WebhookEndpoint.MANUAL, EARLIER);
    }

    private UUID add(String status, String reason, Instant disabledAt) {
      var id = UUID.randomUUID();
      stored.add(
          new WebhookEndpoint(
              id,
              "https://destino.example/hook",
              "destino",
              List.of("task.created.v1"),
              status,
              reason,
              disabledAt,
              EARLIER,
              EARLIER));
      return id;
    }

    @Override
    public void insert(String owner, WebhookEndpoint endpoint, byte[] secretCiphertext) {
      throw new UnsupportedOperationException();
    }

    @Override
    public List<WebhookEndpoint> list(String owner) {
      return List.copyOf(stored);
    }

    @Override
    public Optional<WebhookEndpoint> find(String owner, UUID id) {
      return stored.stream().filter(endpoint -> endpoint.id().equals(id)).findFirst();
    }

    @Override
    public Optional<WebhookEndpoint> changeStatus(String owner, UUID id, String status, Instant n) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean delete(String owner, UUID id) {
      throw new UnsupportedOperationException();
    }
  }

  private static final class FakeDeliveries implements WebhookDeliveries {
    private final Map<UUID, List<WebhookDelivery>> byEndpoint = new HashMap<>();
    final List<UUID> asked = new ArrayList<>();

    void at(UUID endpointId, Instant updatedAt) {
      var id = UUID.randomUUID();
      byEndpoint
          .computeIfAbsent(endpointId, key -> new ArrayList<>())
          .add(
              new WebhookDelivery(
                  id,
                  id,
                  "task.created.v1",
                  WebhookDelivery.SUCCEEDED,
                  1,
                  200,
                  12,
                  null,
                  null,
                  updatedAt,
                  updatedAt));
    }

    @Override
    public List<WebhookDelivery> list(String owner, UUID endpointId) {
      asked.add(endpointId);
      return List.copyOf(byEndpoint.getOrDefault(endpointId, List.of()));
    }

    @Override
    public boolean hasPendingPing(String owner, UUID endpointId) {
      throw new UnsupportedOperationException();
    }

    @Override
    public WebhookDelivery enqueuePing(
        String owner, UUID endpointId, WebhookDelivery delivery, String body) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<WebhookDelivery> find(String owner, UUID endpointId, UUID deliveryId) {
      throw new UnsupportedOperationException();
    }

    @Override
    public WebhookDelivery requeue(String owner, UUID endpointId, WebhookDelivery delivery) {
      throw new UnsupportedOperationException();
    }
  }

  private static final class FakeFeedTokens implements CalendarFeedTokens {
    Instant createdAt;

    @Override
    public Optional<Instant> createdAt(String owner) {
      return Optional.ofNullable(createdAt);
    }

    @Override
    public void replace(String owner, byte[] fingerprint, Instant createdAt) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void revoke(String owner) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<String> ownerOf(byte[] fingerprint) {
      throw new UnsupportedOperationException();
    }
  }

  private static final class FakeGithubConnections implements ConnectorConnectionStore {
    StoredConnection stored;

    @Override
    public Optional<StoredConnection> find(String ownerId) {
      return Optional.ofNullable(stored);
    }

    @Override
    public void save(String ownerId, StoredConnection connection) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean delete(String ownerId) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void invalidate(String ownerId) {
      throw new UnsupportedOperationException();
    }
  }

  private static final class FakeSubscriptions implements ExternalCalendarStore {
    StoredSubscription stored;

    @Override
    public Optional<StoredSubscription> find(String ownerId) {
      return Optional.ofNullable(stored);
    }

    @Override
    public StoredSubscription create(
        String ownerId,
        UUID id,
        com.apptolast.organization.domain.ExternalCalendarInput input,
        byte[] urlCiphertext,
        Instant now) {
      throw new UnsupportedOperationException();
    }

    @Override
    public StoredSubscription relabel(
        String ownerId, String label, byte[] urlCiphertext, Instant now) {
      throw new UnsupportedOperationException();
    }

    @Override
    public StoredSubscription rebind(
        String ownerId,
        com.apptolast.organization.domain.ExternalCalendarInput input,
        byte[] urlCiphertext,
        Instant now) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean delete(String ownerId) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<StoredSubscription> commitSuccess(
        String ownerId,
        long expectedVersion,
        com.apptolast.organization.domain.SyncSummary summary,
        List<com.apptolast.organization.domain.ExternalEvent> events,
        Instant syncAt) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<StoredSubscription> commitFailure(
        String ownerId, long expectedVersion, FeedError error, Instant attemptAt) {
      throw new UnsupportedOperationException();
    }

    @Override
    public List<com.apptolast.organization.domain.ExternalEvent> events(
        String ownerId, Instant from, Instant to) {
      throw new UnsupportedOperationException();
    }
  }

  private static final class FakeReceipts implements IssueImportReceiptStore {
    private final Map<String, IssueImportReceipt> latest = new HashMap<>();

    void put(String source, IssueImportReceipt receipt) {
      latest.put(source, receipt);
    }

    @Override
    public IssueImportReceipt begin(
        String ownerId,
        UUID projectId,
        String source,
        String projectPath,
        Instant startedAt,
        Instant staleBefore) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void progress(String ownerId, UUID importId, int created, int skipped, int failed) {
      throw new UnsupportedOperationException();
    }

    @Override
    public IssueImportReceipt finish(
        String ownerId,
        UUID importId,
        String status,
        String errorCode,
        boolean truncated,
        Instant finishedAt) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<IssueImportReceipt> find(String ownerId, UUID importId) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<IssueImportReceipt> latest(String ownerId, String source) {
      return Optional.ofNullable(latest.get(source));
    }

    @Override
    public boolean importing(String ownerId, Instant staleBefore) {
      throw new UnsupportedOperationException();
    }
  }
}
