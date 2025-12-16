package com.checkout.payment.gateway.interfaces.payment.web.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import net.bytebuddy.asm.MemberSubstitution.Argument;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class PaymentRequestTest {
  private static Validator validator;

  private static PaymentRequest createValidRequest() {
    PaymentRequest request = new PaymentRequest();
    request.setCardNumber("1234567890123456");
    request.setExpiryMonth(12);
    request.setExpiryYear(2025);
    request.setCurrency("EUR");
    request.setAmount(100L);
    request.setCvv("123");
    return request;
  }

  private static Stream<Arguments> invalidScenarios() {
    return Stream.of(
        // --- Card Number Tests ---
        Arguments.of("Card number is null", (Consumer<PaymentRequest>) r -> r.setCardNumber(null), "cardNumber"),
        Arguments.of("Card number is empty", (Consumer<PaymentRequest>) r -> r.setCardNumber(""), "cardNumber"),
        Arguments.of("Card number too short", (Consumer<PaymentRequest>) r -> r.setCardNumber("123"), "cardNumber"),
        Arguments.of("Card number not numeric", (Consumer<PaymentRequest>) r -> r.setCardNumber("1234abcd5678"), "cardNumber"),

        // --- Amount Tests ---
        Arguments.of("Amount is null", (Consumer<PaymentRequest>) r -> r.setAmount(null), "amount"),
        Arguments.of("Amount is negative", (Consumer<PaymentRequest>) r -> r.setAmount(-100L), "amount"),
        Arguments.of("Amount is zero", (Consumer<PaymentRequest>) r -> r.setAmount(0L), "amount"),

        // --- Currency Tests ---
        Arguments.of("Currency is invalid code", (Consumer<PaymentRequest>) r -> r.setCurrency("US"), "currency"),
        Arguments.of("Currency is lower case", (Consumer<PaymentRequest>) r -> r.setCurrency("LOL"), "currency"),
        Arguments.of("Currency is beyond 3 allowed", (Consumer<PaymentRequest>) r -> r.setCurrency("JPY"), "currency"),

        // --- Expiry Date Logic  ---
        Arguments.of("Expiry date is in the past", (Consumer<PaymentRequest>) r -> {
          r.setExpiryYear(2000);
          r.setExpiryMonth(1);
        }, "validExpiryDate")
    );
  }


  @BeforeAll
  static void setUp() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @Test
  void shouldPassValidationForValidRequest() {
    PaymentRequest request = createValidRequest();
    Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);
    assertTrue(violations.isEmpty(), "Valid request should have no violations");
  }


  @Test
  void shouldMaskSensitiveDataInToString() {
    PaymentRequest request = new PaymentRequest();
    request.setCardNumber("1234567890123456");
    request.setCvv("999");

    String logOutput = request.toString();

    assertFalse(logOutput.contains("1234567890123456"), "Full card number must not be logged");
    assertFalse(logOutput.contains("999"), "CVV must not be logged");
    assertTrue(logOutput.contains("******3456"), "Card number should be masked");
  }


  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidScenarios")
  @DisplayName("Should fail validation for invalid requests")
  void shouldFailValidation(String description, Consumer<PaymentRequest> mutator, String expectedErrorField) {
    PaymentRequest request = createValidRequest();
    mutator.accept(request);
    Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);
    assertThat(violations).isNotEmpty();

    boolean errorFound = violations.stream()
        .anyMatch(v -> v.getPropertyPath().toString().contains(expectedErrorField));

    assertThat(errorFound)
        .withFailMessage("Expected error on field '%s' but got: %s", expectedErrorField, violations)
        .isTrue();
  }
}