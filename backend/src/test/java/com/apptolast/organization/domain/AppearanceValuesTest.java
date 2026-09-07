package com.apptolast.organization.domain;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AppearanceValuesTest {
  @Test
  void s11_acceptsAValidAccentWhoseContrastNeedsTheLuminanceOffset() {
    // Independent WCAG vector: minimum contrast on the six light surfaces is 5.1689120807.
    var values = new AppearanceValues("DARK", "#0033FF", "#B7E4C7");
    assertThat(values.accentLight()).isEqualTo("#0033FF");
  }

  @Test
  void s11_acceptsAValidAccentWithANonzeroLowLinearChannel() {
    // Independent WCAG vector: minimum contrast on the six light surfaces is 7.8564656109.
    var values = new AppearanceValues("LIGHT", "#0002D0", "#B7E4C7");
    assertThat(values.accentLight()).isEqualTo("#0002D0");
  }

  @Test
  void s10_rejectsThemeBeforeBothInvalidAccents() {
    assertThatThrownBy(() -> new AppearanceValues("light", "#fff", "rgb(1,2,3)"))
        .isInstanceOfSatisfying(
            ValidationException.class,
            failure -> {
              assertThat(failure.errors()).extracting(FieldError::field).containsExactly("theme");
              assertThat(failure.errors())
                  .extracting(FieldError::code)
                  .containsExactly("INVALID_VALUE");
            });
  }

  @Test
  void s10_rejectsTheLightColorFormatBeforeDark() {
    assertThatThrownBy(() -> new AppearanceValues("LIGHT", "#123", "rgb(1,2,3)"))
        .isInstanceOfSatisfying(
            ValidationException.class,
            failure -> {
              assertThat(failure.errors())
                  .extracting(FieldError::field)
                  .containsExactly("accentLight");
              assertThat(failure.errors())
                  .extracting(FieldError::code)
                  .containsExactly("INVALID_VALUE");
            });
  }

  @Test
  void s11_checksLightContrastEvenWhenDarkThemeIsSelected() {
    assertThatThrownBy(() -> new AppearanceValues("DARK", "#FFFFFF", "#00FFFF"))
        .isInstanceOfSatisfying(
            ValidationException.class,
            failure -> {
              assertThat(failure.errors())
                  .extracting(FieldError::field)
                  .containsExactly("accentLight");
              assertThat(failure.errors())
                  .extracting(FieldError::code)
                  .containsExactly("INSUFFICIENT_CONTRAST");
            });
  }

  @Test
  void s10_rejectsDarkColorFormatAfterAValidLightColor() {
    assertThatThrownBy(() -> new AppearanceValues("LIGHT", "#0000FF", "rgb(1,2,3)"))
        .isInstanceOfSatisfying(
            ValidationException.class,
            failure -> {
              assertThat(failure.errors())
                  .extracting(FieldError::field)
                  .containsExactly("accentDark");
              assertThat(failure.errors())
                  .extracting(FieldError::code)
                  .containsExactly("INVALID_VALUE");
            });
  }

  @Test
  void s11_checksDarkContrastEvenWhenLightThemeIsSelected() {
    assertThatThrownBy(() -> new AppearanceValues("LIGHT", "#0000FF", "#000000"))
        .isInstanceOfSatisfying(
            ValidationException.class,
            failure -> {
              assertThat(failure.errors())
                  .extracting(FieldError::field)
                  .containsExactly("accentDark");
              assertThat(failure.errors())
                  .extracting(FieldError::code)
                  .containsExactly("INSUFFICIENT_CONTRAST");
            });
  }

  @Test
  void s2_canonicalizesBothAcceptedColors() {
    var values = new AppearanceValues("SYSTEM", "#244c3c", "#b7e4c7");
    assertThat(values.accentLight()).isEqualTo("#244C3C");
    assertThat(values.accentDark()).isEqualTo("#B7E4C7");
  }

  @Test
  void s11_doesNotRoundTheNearThresholdContrastUpToFourPointFive() {
    // Independent contract vector: worst surface #D0DFC9 gives 4.499799974...
    assertThatThrownBy(() -> new AppearanceValues("LIGHT", "#645F61", "#00FFFF"))
        .isInstanceOfSatisfying(
            ValidationException.class,
            error ->
                assertThat(error.errors())
                    .extracting(FieldError::code)
                    .containsExactly("INSUFFICIENT_CONTRAST"));
  }
}
