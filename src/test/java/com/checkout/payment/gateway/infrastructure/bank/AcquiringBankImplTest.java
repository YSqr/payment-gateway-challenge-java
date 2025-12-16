package com.checkout.payment.gateway.infrastructure.bank;

import com.checkout.payment.gateway.domain.model.BankResult;
import com.checkout.payment.gateway.domain.model.PaymentStatus;
import com.checkout.payment.gateway.infrastructure.exception.UpstreamTimeoutException;
import com.checkout.payment.gateway.interfaces.payment.web.dto.PaymentRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RestClientTest(AcquiringBankImpl.class)
@Import(AcquiringBankProperties.class)
class AcquiringBankImplTest {
  @Autowired AcquiringBankImpl bank;

  @Autowired
  private MockRestServiceServer server;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void processShouldSendCorrectJsonFormatAndParseResponse() {
    PaymentRequest request = new PaymentRequest();
      request.setCardNumber("1234567812345678");
      request.setExpiryMonth(4);
      request.setExpiryYear(2025);
      request.setAmount(100L);
      request.setCurrency("GBP");
      request.setCvv("123");

    UUID traceId = UUID.randomUUID();

    String expectedRequestBody = """
                {
                    "card_number": "1234567812345678",
                    "expiry_date": "04/2025", 
                    "currency": "GBP",
                    "amount": 100,
                    "cvv": "123"
                }
                """;

    String simulatorResponse = """
                {
                    "authorized": true,
                    "authorization_code": "abc-123"
                }
                """;

    server.expect(requestTo("/payments"))
        .andExpect(content().json(expectedRequestBody))
        .andRespond(withSuccess(simulatorResponse, MediaType.APPLICATION_JSON));

    BankResult result = bank.process(request, traceId);

    assertThat(result.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
    assertThat(result.getAuthorizationCode()).isEqualTo("abc-123");
  }

  @Test
  @DisplayName("Should return REJECTED when bank API returns 4xx Client Error")
  void process_ShouldReturnDeclined_WhenBankReturns4xxError() {
    // Given
    PaymentRequest request = new PaymentRequest();
    request.setCardNumber("1234567812345678"); // Minimal setup
    request.setExpiryMonth(12);
    request.setExpiryYear(2030);

    UUID traceId = UUID.randomUUID();

    // Mock Server Behavior: Simulate a 400 Bad Request
    server.expect(requestTo("/payments"))
        .andRespond(withBadRequest());

    BankResult result = bank.process(request, traceId);

    assertThat(result.getStatus()).isEqualTo(PaymentStatus.REJECTED);
    assertThat(result.getAuthorizationCode()).isNull();
  }

  @Test
  @DisplayName("Should return DECLINED when bank API returns a malformed or empty response")
  void process_ShouldReturnDeclined_WhenBankReturnsMalformedResponse() {
    // Given
    PaymentRequest request = new PaymentRequest();
    request.setCardNumber("1234567812345678");
    request.setExpiryMonth(12);
    request.setExpiryYear(2030);

    UUID traceId = UUID.randomUUID();

    // Mock Response: Empty JSON object, missing "authorized" field
    String malformedResponse = "{}";

    // Mock Server Behavior
    server.expect(requestTo("/payments"))
        .andRespond(withSuccess(malformedResponse, MediaType.APPLICATION_JSON));

    BankResult result = bank.process(request, traceId);

    // Then
    // Based on our boolean logic (default false or null check), this should be DECLINED
    assertThat(result.getStatus()).isEqualTo(PaymentStatus.DECLINED);
    assertThat(result.getAuthorizationCode()).isNull();
  }

  @Test
  @DisplayName("Should throw EventProcessingException when network connection fails (Timeout/Unreachable)")
  void process_ShouldReturnDeclined_WhenNetworkFails() {
    // Given
    PaymentRequest request = new PaymentRequest();
    request.setCardNumber("1234567812345678");
    request.setExpiryMonth(12);
    request.setExpiryYear(2030);

    UUID traceId = UUID.randomUUID();

    // Mock Server Behavior: Throw a connection exception
    server.expect(requestTo("/payments"))
        .andRespond(response -> {
          throw new ResourceAccessException("Connection timed out");
        });

    assertThrows(UpstreamTimeoutException.class, () -> {
      bank.process(request, traceId);
    });
  }
}
