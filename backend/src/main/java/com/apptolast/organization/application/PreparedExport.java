package com.apptolast.organization.application;

import java.io.IOException;
import java.io.OutputStream;

public interface PreparedExport {
  String filename();
  long contentLength();
  void writeTo(OutputStream output) throws IOException;
}