package com.apptolast.organization.adapter.net;

import static org.junit.jupiter.api.Assertions.*;

import java.net.InetAddress;
import java.net.URI;
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
