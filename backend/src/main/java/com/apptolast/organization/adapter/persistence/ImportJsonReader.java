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
  private static final java.util.Set<String> COLLECTIONS =
      java.util.Set.of(
          "projects",
          "tasks",
          "taskStatusHistory",
          "availability",
          "plannedBlocks",
          "blockProjections",
          "blockChanges",
          "workSessions",
          "workSessionIntervals",
          "workSessionChanges",
          "appearance",
          "customization",
          "projectCustomFieldValues",
          "taskCustomFieldValues");

  public record Header(
      String owner, Instant exportedAt, String fileSha256, long byteLength, ImportCounts counts) {}

  @FunctionalInterface
  public interface RecordConsumer {
    void accept(String collection, JsonNode row) throws IOException;
  }

  public Header read(InputStream input, RecordConsumer consumer) throws IOException {
    return read(input, consumer, null);
  }

  public Header read(InputStream input, RecordConsumer consumer, String expectedSha256)
      throws IOException {
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException unavailable) {
      throw new IllegalStateException(unavailable);
    }
    var counted = new CountedInput(new DigestInputStream(input, digest));
    var mapper =
        new ObjectMapper()
            .enable(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_INTEGER_FOR_INTS)
            .enable(
                com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    mapper
        .getFactory()
        .setStreamReadConstraints(
            com.fasterxml.jackson.core.StreamReadConstraints.builder()
                .maxNestingDepth(16)
                .maxNumberLength(33_554_432)
                .maxStringLength(33_554_432)
                .maxNameLength(33_554_432)
                .build());
    String owner = null;
    String exportedAt = null;
    java.util.Map<String, Long> counts = null;
    boolean invalidEnvelope = false;
    boolean validFormat = false;
    boolean validVersion = false;
    long totalRecords = 0;
    var actualCounts = new java.util.LinkedHashMap<String, Long>();
    var utf8 =
        java.nio.charset.StandardCharsets.UTF_8
            .newDecoder()
            .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
            .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT);
    try (var parser =
        new com.fasterxml.jackson.core.util.JsonParserDelegate(
            mapper.getFactory().createParser(new java.io.InputStreamReader(counted, utf8))) {
          @Override
          public java.math.BigInteger getBigIntegerValue() throws IOException {
            if (getTextLength() > 1000)
              throw new NumberFormatException("Unbounded integer representation");
            return super.getBigIntegerValue();
          }

          @Override
          public java.math.BigDecimal getDecimalValue() throws IOException {
            if (getTextLength() > 1000) return boundedIntegerToken(getText());
            try {
              return super.getDecimalValue();
            } catch (NumberFormatException unrepresentable) {
              String token = getText();
              for (int i = 0; i < token.length(); i++) {
                char digit = token.charAt(i);
                if (digit == 'e' || digit == 'E') return java.math.BigDecimal.ZERO;
                if (digit != '0' && digit != '.' && digit != '-') throw unrepresentable;
              }
              throw unrepresentable;
            }
          }
        }) {
      parser.disable(JsonParser.Feature.AUTO_CLOSE_SOURCE);
      parser.enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
      if (nextRequired(parser) != JsonToken.START_OBJECT) {
        invalidEnvelope = true;
        parser.skipChildren();
      } else
        while (nextRequired(parser) != JsonToken.END_OBJECT) {
          String field = parser.currentName();
          nextRequired(parser);
          switch (field) {
            case "owner" -> owner = readString(parser);
            case "exportedAt" -> exportedAt = readString(parser);
            case "counts" -> counts = readCounts(parser);
            case "data" -> {
              if (parser.currentToken() != JsonToken.START_OBJECT) {
                invalidEnvelope = true;
                parser.skipChildren();
                break;
              }
              while (nextRequired(parser) != JsonToken.END_OBJECT) {
                String collection = parser.currentName();
                nextRequired(parser);
                if (!COLLECTIONS.contains(collection)
                    || parser.currentToken() != JsonToken.START_ARRAY) {
                  invalidEnvelope = true;
                  parser.skipChildren();
                  continue;
                }
                long count = 0;
                while (nextRequired(parser) != JsonToken.END_ARRAY) {
                  if (++totalRecords > 100000) throw new ImportTooLargeException();
                  var rowParent = parser.getParsingContext();
                  if (parser.currentToken().isStructStart()) rowParent = rowParent.getParent();
                  JsonNode row = null;
                  try {
                    row = mapper.readTree(parser);
                  } catch (NumberFormatException unrepresentableNumber) {
                    invalidEnvelope = true;
                    while (parser.getParsingContext() != rowParent) nextRequired(parser);
                  }
                  if (row != null) consumer.accept(collection, row);
                  count++;
                }
                actualCounts.put(collection, count);
              }
            }
            case "format" -> {
              validFormat =
                  parser.currentToken() == JsonToken.VALUE_STRING
                      && "organizationweb-export".equals(parser.getText());
              parser.skipChildren();
            }
            case "schemaVersion" -> {
              validVersion = Long.valueOf(1).equals(readCount(parser));
            }
            default -> {
              invalidEnvelope = true;
              parser.skipChildren();
            }
          }
        }
      if (parser.nextToken() != null) throw new ImportInvalidFileException();
    } catch (com.fasterxml.jackson.core.JsonProcessingException
        | java.nio.charset.CharacterCodingException invalid) {
      throw new ImportInvalidFileException();
    }
    var actualSha256 = HexFormat.of().formatHex(digest.digest());
    if (expectedSha256 != null && !expectedSha256.equals(actualSha256))
      throw new com.apptolast.organization.application.ImportFileChangedException();
    if (invalidEnvelope || !validFormat || !validVersion || owner == null || exportedAt == null)
      throw new ImportInvalidFileException();
    if (counts == null || !counts.keySet().equals(COLLECTIONS) || !counts.equals(actualCounts))
      throw new ImportInvalidFileException();
    Instant timestamp;
    try {
      timestamp = Instant.parse(exportedAt);
      int year = timestamp.atOffset(java.time.ZoneOffset.UTC).getYear();
      var canonical =
          new java.time.format.DateTimeFormatterBuilder().appendInstant(6).toFormatter();
      if (year < 1 || year > 9999 || !canonical.format(timestamp).equals(exportedAt))
        throw new ImportInvalidFileException();
    } catch (java.time.DateTimeException invalid) {
      throw new ImportInvalidFileException();
    }
    return new Header(
        owner,
        timestamp,
        actualSha256,
        counted.length,
        mapper.convertValue(counts, ImportCounts.class));
  }

  // Jackson has already checked the token grammar. All durable JSON numbers are integers;
  // reduce long equivalent spellings before any arbitrary-precision conversion.
  private static java.math.BigDecimal boundedIntegerToken(String token) {
    int digits = 0, fractional = 0, first = -1, last = -1, exponentAt = token.length();
    boolean afterDot = false;
    var significant = new StringBuilder(19);
    for (int i = token.startsWith("-") ? 1 : 0; i < token.length(); i++) {
      char digit = token.charAt(i);
      if (digit == 'e' || digit == 'E') {
        exponentAt = i;
        break;
      }
      if (digit == '.') {
        afterDot = true;
        continue;
      }
      if (afterDot) fractional++;
      if (digit != '0') {
        if (first < 0) first = digits;
        last = digits;
      }
      if (first >= 0 && digits - first < 19) significant.append(digit);
      digits++;
    }
    if (first < 0) return java.math.BigDecimal.ZERO;
    int precision = last - first + 1;
    if (precision > 19) throw new NumberFormatException("Unbounded numeric representation");
    significant.setLength(precision);
    int exponent = 0;
    boolean negativeExponent = false;
    if (exponentAt < token.length()) {
      int start = exponentAt + 1;
      if (token.charAt(start) == '+' || token.charAt(start) == '-') {
        negativeExponent = token.charAt(start) == '-';
        start++;
      }
      for (int i = start; i < token.length(); i++) {
        exponent = Math.min(33_554_451, exponent * 10 + token.charAt(i) - '0');
        if (exponent == 33_554_451) throw new NumberFormatException("Unbounded numeric exponent");
      }
    }
    if (negativeExponent) exponent = -exponent;
    int scale = fractional - (digits - last - 1) - exponent;
    if (scale > 0 || precision - scale > 19)
      throw new NumberFormatException("Not a bounded integer");
    return new java.math.BigDecimal(
        new java.math.BigInteger((token.startsWith("-") ? "-" : "") + significant), scale);
  }

  private static String readString(JsonParser parser) throws IOException {
    if (parser.currentToken() == JsonToken.VALUE_STRING) return parser.getText();
    parser.skipChildren();
    return null;
  }

  private static java.util.Map<String, Long> readCounts(JsonParser parser) throws IOException {
    if (parser.currentToken() != JsonToken.START_OBJECT) {
      parser.skipChildren();
      return null;
    }
    var counts = new java.util.LinkedHashMap<String, Long>();
    boolean valid = true;
    while (nextRequired(parser) != JsonToken.END_OBJECT) {
      String field = parser.currentName();
      nextRequired(parser);
      Long count = readCount(parser);
      if (count == null || counts.size() >= 14) valid = false;
      else counts.put(field, count);
    }
    return valid ? counts : null;
  }

  private static Long readCount(JsonParser parser) throws IOException {
    if (!parser.currentToken().isNumeric()) {
      parser.skipChildren();
      return null;
    }
    try {
      var value = parser.getDecimalValue();
      if (value.signum() < 0 || value.compareTo(java.math.BigDecimal.valueOf(100000)) > 0)
        return null;
      return value.longValueExact();
    } catch (NumberFormatException | ArithmeticException invalidNumber) {
      return null;
    }
  }

  private static JsonToken nextRequired(JsonParser parser) throws IOException {
    var token = parser.nextToken();
    if (token == null) throw new ImportInvalidFileException();
    return token;
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
