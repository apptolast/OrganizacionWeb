package com.apptolast.organization.domain;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AppearanceValuesTest {
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
}
