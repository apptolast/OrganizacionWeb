package com.apptolast.organization.adapter.net;

import static org.junit.jupiter.api.Assertions.*;

import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpRequest;
import java.util.Locale;
import javax.net.ssl.SNIHostName;
import org.junit.jupiter.api.Test;

/**
 * Las pruebas de la pieza compartida del anclaje. Existen aparte de las de los dos conectores
 * porque hay una parte del contrato que ninguna prueba de extremo a extremo puede matar: el cliente
 * HTTP del JDK impone por su cuenta la verificación del nombre en https, así que quitarla de aquí
 * no rompe ninguna descarga. Medido, no supuesto: con la línea comentada, `JdkWebhookSenderTest`
 * seguía verde entera. Se prueba aquí, al nivel al que sí se puede afirmar.
 */
class AnchoredConnectionTest {
  private static final String NAME = "destino.anclado.invalid";

  @Test
  void theNameOfTheDestinationTravelsAsServerNameIndication() {
    var parameters = AnchoredConnection.sniFor(NAME);

    assertEquals(1, parameters.getServerNames().size());
    assertEquals(NAME, ((SNIHostName) parameters.getServerNames().getFirst()).getAsciiName());
  }

  /**
   * Los parámetros llevan la verificación del nombre puesta. El cliente del JDK la impondría de
   * todos modos en https; que esté aquí es lo que hace que la pieza sea correcta por sí sola, para
   * quien la use con un socket o un motor de TLS, donde por omisión NO se verifica nada.
   */
  @Test
  void theParametersAskForCertificateVerificationByName() {
    assertEquals("HTTPS", AnchoredConnection.sniFor(NAME).getEndpointIdentificationAlgorithm());
    assertEquals(
        "HTTPS", AnchoredConnection.sniFor("127.0.0.1").getEndpointIdentificationAlgorithm());
  }

  /**
   * El RFC 6066 prohíbe poner direcciones en la indicación de servidor. Y además el JDK acepta la
   * forma IPv4 y estalla con la IPv6, así que dejarlo pasar sería una grieta con dos caras.
   */
  @Test
  void anAddressIsNeverOfferedAsAServerName() {
    assertNull(AnchoredConnection.sniFor("127.0.0.1").getServerNames());
    assertNull(AnchoredConnection.sniFor("203.0.113.7").getServerNames());
    assertNull(AnchoredConnection.sniFor("[::1]").getServerNames());
    assertNull(AnchoredConnection.sniFor("[2001:db8::1]").getServerNames());
  }

  @Test
  void theAddressReplacesTheNameAndTheRestOfTheUrlSurvives() throws Exception {
    var target = URI.create("https://" + NAME + ":8443/feeds/a.ics?token=abc");

    var literal = AnchoredConnection.literal(target, InetAddress.getByName("203.0.113.7"));

    assertEquals("https://203.0.113.7:8443/feeds/a.ics?token=abc", literal.toString());
  }

  @Test
  void anIpv6AddressIsWrittenBetweenBrackets() throws Exception {
    var target = URI.create("https://" + NAME + "/f.ics");

    var literal = AnchoredConnection.literal(target, InetAddress.getByName("2001:db8::1"));

    assertEquals("https://[2001:db8:0:0:0:0:0:1]/f.ics", literal.toString());
  }

  /** Sin puerto y sin ruta la URL anclada sigue siendo una URL: la raíz, no el vacío. */
  @Test
  void anEmptyPathBecomesTheRootAndTheDefaultPortStaysImplicit() throws Exception {
    var literal =
        AnchoredConnection.literal(
            URI.create("https://" + NAME), InetAddress.getByName("203.0.113.7"));

    assertEquals("https://203.0.113.7/", literal.toString());
  }

  @Test
  void theAuthorityKeepsTheNameAndOnlyTheExplicitPort() {
    assertEquals(NAME, AnchoredConnection.authority(URI.create("https://" + NAME + "/x"), NAME));
    assertEquals(
        NAME + ":8443",
        AnchoredConnection.authority(URI.create("https://" + NAME + ":8443/x"), NAME));
  }

  /**
   * La capacidad de la que depende TODO el anclaje, afirmada al nivel en el que de verdad importa:
   * no «la propiedad está puesta», sino «este JVM deja poner la cabecera».
   *
   * <p>No es lo mismo, y la diferencia costó un defecto real. El cliente del JDK lee la propiedad
   * <b>una sola vez</b>, al inicializar su clase de utilidades, que ocurre en cuanto alguien
   * construye la primera petición. Si otro componente lo hace antes de que se cargue esta clase, el
   * bloque estático llega tarde: la propiedad queda puesta y sin efecto. Entonces cada petición
   * anclada muere con {@code IllegalArgumentException}, y los adaptadores la convierten en «no
   * alcanzable» sin decir por qué.
   *
   * <p>Medido en `main` (6997f5d), antes de este encargo: `HttpCalendarFeedTest` sola daba verde, y
   * junto a `HttpGithubIssueSourceTest` —que construye un `HttpClient` en su constructor— caían 20
   * pruebas con FEED_UNREACHABLE en vez de lo esperado. El arreglo es declarar la propiedad al
   * arrancar el JVM; el bloque estático se queda como red, no como garantía.
   */
  @Test
  void thisJvmAcceptsTheHostHeaderTheAnchoringDependsOn() {
    assertDoesNotThrow(
        () ->
            HttpRequest.newBuilder(URI.create("https://203.0.113.7/x"))
                .header("Host", NAME)
                .GET()
                .build(),
        "hace falta -Djdk.httpclient.allowRestrictedHeaders=host al arrancar el JVM");
  }

  /**
   * Sin esta autorización el cliente del JDK descarta la cabecera Host y el anclaje pierde el
   * nombre.
   */
  @Test
  void theRestrictedHostHeaderIsEnabled() {
    AnchoredConnection.allow();

    assertTrue(
        System.getProperty("jdk.httpclient.allowRestrictedHeaders", "")
            .toLowerCase(Locale.ROOT)
            .contains("host"));
  }

  /** Y no pisa lo que hubiera declarado el despliegue: se añade a la lista, no la sustituye. */
  @Test
  void enablingItDoesNotDropWhatTheDeploymentAlreadyDeclared() {
    var property = "jdk.httpclient.allowRestrictedHeaders";
    var previous = System.getProperty(property);
    try {
      System.setProperty(property, "connection");

      AnchoredConnection.allow();

      assertEquals("connection,host", System.getProperty(property));
    } finally {
      if (previous == null) System.clearProperty(property);
      else System.setProperty(property, previous);
    }
  }
}
