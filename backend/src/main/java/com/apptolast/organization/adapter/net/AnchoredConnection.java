package com.apptolast.organization.adapter.net;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLParameters;

/**
 * Las piezas comunes de anclar una petición saliente a la dirección ya validada sin perder el
 * nombre. Las comparten los dos conectores que salen a internet —los webhooks de la feature 25 y el
 * calendario externo de la 28— porque la enmienda B3 es una sola decisión y tenerla escrita dos
 * veces fue justamente lo que dejó que los dos carriles decidieran lo contrario sin verse.
 *
 * <p>Anclar consiste en tres cosas a la vez, y las tres hacen falta: conectar a la {@link
 * #literal(URI, InetAddress) dirección literal}, decirle al servidor el {@link #authority(URI,
 * String) nombre} en la cabecera {@code Host}, y ofrecerle ese mismo nombre en la {@link
 * #sniFor(String) indicación de servidor} de TLS, que es además contra lo que se verifica el
 * certificado. Anclar sin las dos últimas rompería la validación del nombre; con ellas, no.
 */
public final class AnchoredConnection {
  /** Un nombre escrito como dirección IPv4; la forma IPv6 se reconoce por los dos puntos. */
  private static final Pattern IPV4 = Pattern.compile("(\\d{1,3}\\.){3}\\d{1,3}");

  private static final String RESTRICTED_HEADERS = "jdk.httpclient.allowRestrictedHeaders";
  private static final String HOST = "host";

  private AnchoredConnection() {}

  /**
   * Enviar «Host» a mano es imprescindible para conectar por dirección literal sin perder el
   * nombre, y el cliente del JDK lo prohíbe salvo que se le autorice antes de cargar su clase de
   * utilidades. Se añade sin pisar lo que ya hubiera declarado el despliegue.
   */
  public static void allow() {
    var allowed = System.getProperty(RESTRICTED_HEADERS, "");
    if (!allowed.toLowerCase(Locale.ROOT).contains(HOST))
      System.setProperty(RESTRICTED_HEADERS, allowed.isEmpty() ? HOST : allowed + "," + HOST);
  }

  /** La misma URL, pero con la dirección ya validada en lugar del nombre. */
  public static URI literal(URI target, InetAddress pinned) {
    var address = pinned.getHostAddress();
    var written = pinned instanceof Inet6Address ? "[" + address + "]" : address;
    var port = target.getPort() < 0 ? "" : ":" + target.getPort();
    var path =
        target.getRawPath() == null || target.getRawPath().isEmpty() ? "/" : target.getRawPath();
    var query = target.getRawQuery() == null ? "" : "?" + target.getRawQuery();
    return URI.create(target.getScheme() + "://" + written + port + path + query);
  }

  /** El nombre original con su puerto: lo que el receptor debe ver en {@code Host}. */
  public static String authority(URI target, String host) {
    return target.getPort() < 0 ? host : host + ":" + target.getPort();
  }

  /**
   * Los parámetros de TLS del destino: el nombre como indicación de servidor y la verificación del
   * certificado por nombre de HTTPS. Sin la indicación, conectar por dirección literal ofrecería la
   * IP y el proveedor no podría ni elegir su certificado ni ser validado por su nombre.
   *
   * <p>Si el destino ya venía escrito como dirección no hay nombre que indicar: el RFC 6066 prohíbe
   * poner direcciones ahí, y el certificado se verifica entonces contra la dirección.
   */
  public static SSLParameters sniFor(String host) {
    var parameters = new SSLParameters();
    if (!isAddressLiteral(host)) parameters.setServerNames(List.of(new SNIHostName(host)));
    parameters.setEndpointIdentificationAlgorithm("HTTPS");
    return parameters;
  }

  private static boolean isAddressLiteral(String host) {
    return host.indexOf(':') >= 0 || host.startsWith("[") || IPV4.matcher(host).matches();
  }
}
