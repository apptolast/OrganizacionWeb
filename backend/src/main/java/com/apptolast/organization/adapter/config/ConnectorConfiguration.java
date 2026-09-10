package com.apptolast.organization.adapter.config;

import com.apptolast.organization.adapter.connectors.AesGcmSecretCipher;
import com.apptolast.organization.adapter.connectors.ConnectorKeyRing;
import com.apptolast.organization.application.SecretCipher;
import java.security.SecureRandom;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cableado del cifrado de secretos en reposo. Nacio con los conectores de issues, retirados, y se
 * queda porque quien cifra hoy es el calendario externo: la URL del feed lleva el token del
 * proveedor y no puede estar en claro en la base.
 *
 * <p>Dos politicas deliberadamente distintas para la configuracion:
 *
 * <ul>
 *   <li><b>Clave ausente</b>: el cifrado se deshabilita y las rutas que lo necesitan responden 503.
 *       Un despliegue que todavia no cifra nada no tiene por que configurar una clave para
 *       arrancar.
 *   <li><b>Clave presente y mal formada</b>: la aplicacion no arranca. Aqui si hay intencion de
 *       cifrar, y arrancar con una clave rota significaria escribir secretos que despues no se
 *       pueden leer.
 * </ul>
 */
@Configuration
public class ConnectorConfiguration {
  @Bean
  public SecretCipher secretCipher(
      @Value("${app.connectors.key:}") String key,
      @Value("${app.connectors.key-previous:}") String previous) {
    return new AesGcmSecretCipher(
        ConnectorKeyRing.of(configured(key), configured(previous)), new SecureRandom());
  }

  /** Una variable de entorno sin definir llega como cadena vacia: eso es ausencia, no error. */
  private static String configured(String value) {
    return value == null || value.isBlank() ? null : value;
  }
}
