package com.apptolast.organization.adapter.net;

import com.apptolast.organization.application.HostResolver;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

/** Resolución DNS de la plataforma. Un host que no resuelve devuelve la lista vacía. */
public final class SystemHostResolver implements HostResolver {
  @Override
  public List<InetAddress> resolve(String host) {
    try {
      return List.of(InetAddress.getAllByName(host));
    } catch (UnknownHostException | SecurityException unresolved) {
      return List.of();
    }
  }
}
