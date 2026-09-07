package com.apptolast.organization.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CustomFieldValuesTest {
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(ints = {-1000000000, 0, 1000000000})
  void s11_exactNumberEndpointsAndZeroAreValid(int number) {
    assertThat(
            new CustomFieldInput(UUID.randomUUID(), BigDecimal.valueOf(number))
                .canonical(CustomFieldType.NUMBER, 0))
        .isEqualTo(number);
  }

  @ParameterizedTest
  @org.junit.jupiter.params.provider.MethodSource("invalidTexts")
  void s12_invalidTextReportsValueErrorWithoutTrimming(String text) {
    assertThatThrownBy(
            () -> new CustomFieldInput(UUID.randomUUID(), text).canonical(CustomFieldType.TEXT, 0))
        .isInstanceOfSatisfying(
            ValidationException.class,
            error -> {
              assertThat(error.errors().getFirst().field()).isEqualTo("values[0].value");
              assertThat(error.errors().getFirst().code()).isEqualTo("INVALID_VALUE");
            });
  }

  static java.util.stream.Stream<String> invalidTexts() {
    return java.util.stream.Stream.of("x".repeat(1001), "\u0000", "\ud800", "\udfff");
  }

  @ParameterizedTest
  @org.junit.jupiter.params.provider.MethodSource("invalidTypes")
  void s12_incompatibleJsonPrimitiveReportsInvalidType(CustomFieldType type, Object value) {
    assertThatThrownBy(() -> new CustomFieldInput(UUID.randomUUID(), value).canonical(type, 1))
        .isInstanceOfSatisfying(
            ValidationException.class,
            error -> {
              assertThat(error.errors().getFirst().field()).isEqualTo("values[1].value");
              assertThat(error.errors().getFirst().code()).isEqualTo("INVALID_TYPE");
            });
  }

  static java.util.stream.Stream<org.junit.jupiter.params.provider.Arguments> invalidTypes() {
    return java.util.stream.Stream.of(
        org.junit.jupiter.params.provider.Arguments.of(CustomFieldType.TEXT, BigDecimal.ONE),
        org.junit.jupiter.params.provider.Arguments.of(CustomFieldType.NUMBER, "1"),
        org.junit.jupiter.params.provider.Arguments.of(CustomFieldType.BOOLEAN, "false"),
        org.junit.jupiter.params.provider.Arguments.of(CustomFieldType.BOOLEAN, BigDecimal.ZERO),
        org.junit.jupiter.params.provider.Arguments.of(CustomFieldType.DATE, true));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"1900-02-29", "0000-01-01", "10000-01-01", "2026-1-01", "2026-01-01T00:00:00Z"})
  void s12_invalidDateReportsItsFieldWithoutNormalization(String date) {
    assertThatThrownBy(
            () -> new CustomFieldInput(UUID.randomUUID(), date).canonical(CustomFieldType.DATE, 2))
        .isInstanceOfSatisfying(
            ValidationException.class,
            error -> {
              assertThat(error.errors().getFirst().field()).isEqualTo("values[2].value");
              assertThat(error.errors().getFirst().code()).isEqualTo("INVALID_VALUE");
            });
  }

  @ParameterizedTest
  @ValueSource(strings = {"0001-01-01", "9999-12-31", "2000-02-29"})
  void s11_dateRemainsTheExactGregorianCalendarDate(String date) {
    assertThat(new CustomFieldInput(UUID.randomUUID(), date).canonical(CustomFieldType.DATE, 0))
        .isEqualTo(date);
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "  \n  ", "<b>Texto</b>"})
  void s11_textPreservesContentAndOnlyEmptyStringBecomesNull(String value) {
    assertThat(new CustomFieldInput(UUID.randomUUID(), value).canonical(CustomFieldType.TEXT, 0))
        .isEqualTo(value.isEmpty() ? null : value);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void s11_booleanValuesKeepTheirExactTruthValue(boolean value) {
    assertThat(new CustomFieldInput(UUID.randomUUID(), value).canonical(CustomFieldType.BOOLEAN, 0))
        .isEqualTo(value);
  }

  @ParameterizedTest
  @org.junit.jupiter.params.provider.EnumSource(CustomFieldType.class)
  void s11_nullRemainsAbsentForEveryType(CustomFieldType type) {
    assertThat(new CustomFieldInput(UUID.randomUUID(), null).canonical(type, 0)).isNull();
  }

  @ParameterizedTest
  @ValueSource(strings = {"-1000000001", "1000000001", "1.5", "1e100"})
  void s12_numberOutsideIntegralRangeReportsItsOriginalArrayIndex(String number) {
    var input = new CustomFieldInput(UUID.randomUUID(), new BigDecimal(number));
    assertThatThrownBy(() -> input.canonical(CustomFieldType.NUMBER, 3))
        .isInstanceOfSatisfying(
            ValidationException.class,
            error -> {
              assertThat(error.errors().getFirst().field()).isEqualTo("values[3].value");
              assertThat(error.errors().getFirst().code()).isEqualTo("INVALID_VALUE");
            });
  }

  @Test
  void s11_exactIntegralJsonNumberIsCanonicalizedToInteger() {
    var input = new CustomFieldInput(UUID.randomUUID(), new BigDecimal("1.0"));
    assertThat(input.canonical(CustomFieldType.NUMBER, 0)).isEqualTo(1).isInstanceOf(Integer.class);
  }
}
