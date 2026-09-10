package com.apptolast.organization;

import com.apptolast.organization.adapter.net.AnchoredConnection;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OrganizationApplication {
  public static void main(String[] args) {
    // Antes de nada, y de verdad antes: el cliente HTTP del JDK lee la lista de cabeceras
    // restringidas una sola vez, al inicializar su clase de utilidades, y eso ocurre en cuanto
    // cualquier componente construye su primera petición. Autorizar «Host» desde el bloque
    // estático de los conectores llega tarde si otro fue antes, y entonces todo el anclaje se cae
    // en silencio (ver AnchoredConnectionTest.thisJvmAcceptsTheHostHeaderTheAnchoringDependsOn).
    // Aquí no hay nada del contexto de Spring todavía, así que este es el único sitio del que se
    // puede llegar el primero.
    AnchoredConnection.allow();
    SpringApplication.run(OrganizationApplication.class, args);
  }
}
