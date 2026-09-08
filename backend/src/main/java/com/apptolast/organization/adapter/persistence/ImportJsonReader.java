package com.apptolast.organization.adapter.persistence;

import com.apptolast.organization.application.ImportCounts;
import com.apptolast.organization.application.ImportInvalidFileException;
import com.apptolast.organization.application.ImportTooLargeException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

public final class ImportJsonReader {
  public record Header(
      String owner, Instant exportedAt, String fileSha256, long byteLength, ImportCounts counts) {}

  @FunctionalInterface
  public interface RecordConsumer {
    void accept(String collection, JsonNode row) throws IOException;
  }

  public Header read(InputStream input, RecordConsumer consumer) throws IOException {
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException unavailable) {
      throw new IllegalStateException(unavailable);
    }
    var counted = new CountedInput(new DigestInputStream(input, digest));
    var mapper = new ObjectMapper();
    String owner = null;
    Instant exportedAt = null;
    ImportCounts counts = null;
    try (var parser = mapper.getFactory().createParser(counted)) {
      parser.disable(JsonParser.Feature.AUTO_CLOSE_SOURCE);
      parser.nextToken();
      while (parser.nextToken() != JsonToken.END_OBJECT) {
        String field = parser.currentName();
        parser.nextToken();
        switch (field) {
          case "owner" -> owner = parser.getText();
          case "exportedAt" -> exportedAt = Instant.parse(parser.getText());
          case "counts" -> counts = mapper.readValue(parser, ImportCounts.class);
          case "data" -> parser.skipChildren();
          case "format", "schemaVersion" -> parser.skipChildren();
          default -> throw new ImportInvalidFileException();
        }
      }
      parser.nextToken();
    }
    return new Header(
        owner, exportedAt, HexFormat.of().formatHex(digest.digest()), counted.length, counts);
  }

  private static final class CountedInput extends FilterInputStream {
    private long length;

    private CountedInput(InputStream input) {
      super(input);
    }

    @Override
    public int read() throws IOException {
      int value = in.read();
      if (value != -1) length++;
      if (length > 33_554_432L) throw new ImportTooLargeException();
      return value;
    }

    @Override
    public int read(byte[] bytes, int offset, int size) throws IOException {
      int count = in.read(bytes, offset, (int) Math.min(size, 33_554_433L - length));
      if (count > 0) length += count;
      if (length > 33_554_432L) throw new ImportTooLargeException();
      return count;
    }
  }
}
