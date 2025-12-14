package com.checkout.payment.gateway.interfaces.payment.web.dto;

import com.checkout.payment.gateway.domain.model.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@JsonTest
class PaymentResponseTest {
  @Autowired
  private JacksonTester<PaymentResponse> jsonResponse; // For POST

  @Autowired
  private JacksonTester<PaymentResponse> jsonDetails; // For GET

  @Test
  @DisplayName("POST Response: Should hide masked_number when it is null")
  void shouldHideMaskedNumberForPostResponse() throws Exception {
    // Given: A card object WITHOUT maskedNumber (Simulating POST creation)
    PaymentCardInfo card = PaymentCardInfo.builder()
        .lastFour("4242")
        .expiryMonth(12)
        .expiryYear(2026)
        .maskedNumber(null) // Explicitly null
        .build();

    PaymentResponse response = PaymentResponse.builder()
        .id(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"))
        .status(PaymentStatus.AUTHORIZED)
        .amount(100L)
        .currency("USD")
        .card(card)
        .build();

    JsonContent<PaymentResponse> result = jsonResponse.write(response);

    // 1. Verify snake_case
    assertThat(result).extractingJsonPathStringValue("$.card.last_four").isEqualTo("4242");

    // 2. Verify masked_number is ABSENT (Hidden)
    assertThat(result).doesNotHaveJsonPathValue("$.card.masked_number");

    // 3. Verify basic fields
    assertThat(result).extractingJsonPathStringValue("$.status").isEqualTo("Authorized");
  }

  @Test
  @DisplayName("GET Response: Should show masked_number when it is populated")
  void shouldShowMaskedNumberForGetResponse() throws Exception {
    // Given: A card object WITH maskedNumber (Simulating GET details)
    PaymentCardInfo card = PaymentCardInfo.builder()
        .lastFour("4242")
        .expiryMonth(12)
        .expiryYear(2026)
        .maskedNumber("************4242") // Populated!
        .build();

    PaymentResponse response = PaymentResponse.builder()
        .id(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"))
        .status(PaymentStatus.DECLINED)
        .amount(2500L)
        .currency("EUR")
        .card(card)
        .build();

    JsonContent<PaymentResponse> result = jsonDetails.write(response);

    // 1. Verify masked_number is PRESENT and mapped correctly
    assertThat(result).hasJsonPathValue("$.card.masked_number");
    assertThat(result).extractingJsonPathStringValue("$.card.masked_number").isEqualTo("************4242");

    // 2. Verify other fields still work
    assertThat(result).extractingJsonPathStringValue("$.card.last_four").isEqualTo("4242");
    assertThat(result).extractingJsonPathStringValue("$.status").isEqualTo("Declined");
  }
}