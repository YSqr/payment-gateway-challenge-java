package com.checkout.payment.gateway.e2e;

import com.checkout.payment.gateway.domain.model.PaymentStatus;
import com.checkout.payment.gateway.interfaces.payment.web.dto.ErrorResponse;
import com.checkout.payment.gateway.interfaces.payment.web.dto.PaymentRequest;
import com.checkout.payment.gateway.interfaces.payment.web.dto.PaymentResponse;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = "acquiring-bank.url=http://localhost:8080"
)
@Tag("e2e")
public class TestWithSimulator {
  private static final String SERVICE_NAME = "bank_simulator";
  private static final int SERVICE_PORT = 8080;
  @LocalServerPort
  private int port;

  @Autowired
  private TestRestTemplate restTemplate;

  private PaymentRequest createValidRequest() {
    PaymentRequest request = new PaymentRequest();
    request.setExpiryMonth(12);
    request.setExpiryYear(2025);
    request.setAmount(100L);
    request.setCurrency("USD");
    request.setCvv("123");
    return request;
  }

  private ResponseEntity<PaymentResponse> postPayment(PaymentRequest request) {
    return restTemplate.postForEntity(
        "http://localhost:" + port + "/api/v1/payments",
        request,
        PaymentResponse.class
    );
  }

  private ResponseEntity<PaymentResponse> getPayment(UUID id) {
    return restTemplate.getForEntity(
        "http://localhost:" + port + "/api/v1/payments/" + id,
        PaymentResponse.class
    );
  }


  // Card number ends on an odd number (1, 3, 5, 7, 9) -> Authorized
  @Test
  void shouldReturnAuthorized_WhenCardEndsWithOddNumber() {
    PaymentRequest request = createValidRequest();

    request.setCardNumber("1234567812345671"); // Ends with 1 (Odd)
    // create payment
    ResponseEntity<PaymentResponse> response = postPayment(request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
    assertThat(response.getBody().getId()).isNotNull();
    assertThat(response.getBody().getCard().getMaskedNumber()).isNull();

    // retrieve payment
    UUID paymentId = response.getBody().getId();
    ResponseEntity<PaymentResponse> getResponse = getPayment(paymentId);

    assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(getResponse.getBody().getId()).isEqualTo(paymentId);
    assertThat(getResponse.getBody().getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
    assertThat(getResponse.getBody().getCard().getMaskedNumber()).isEqualTo("************5671");
  }

  @Test
  void shouldReturnDeclined_WhenCardEndsWithEvenNumber() {
    PaymentRequest request = createValidRequest();
    request.setCardNumber("1234567812345672"); // Ends with 2 (Even)

    ResponseEntity<PaymentResponse> response = postPayment(request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody().getStatus()).isEqualTo(PaymentStatus.DECLINED);

    UUID paymentId = response.getBody().getId();
    ResponseEntity<PaymentResponse> getResponse = getPayment(paymentId);

    assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(getResponse.getBody().getId()).isEqualTo(paymentId);
    assertThat(getResponse.getBody().getStatus()).isEqualTo(PaymentStatus.DECLINED);
    assertThat(getResponse.getBody().getCard().getMaskedNumber()).isEqualTo("************5672");
  }

  // Bank return 503 Service Unavailable
  @Test
  void shouldReturnBadGatewayWhenBankUnavailable() {
    PaymentRequest request = createValidRequest();
    request.setCardNumber("1234567812345670"); // Ends with 0

    ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
        "http://localhost:" + port + "/api/v1/payments",
        request,
        ErrorResponse.class
    );

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
    ErrorResponse errorBody = response.getBody();
    assertThat(errorBody).isNotNull();
    assertThat(errorBody.getMessage()).contains("Bank server error");

  }

  // Bad Request
  @Test
  void shouldReturn400_WhenRequestIsInvalid() {
    PaymentRequest request = new PaymentRequest();

    ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
        "http://localhost:" + port + "/api/v1/payments",
        request,
        ErrorResponse.class
    );

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    ErrorResponse errorBody = response.getBody();
    assertThat(errorBody).isNotNull();
    assertThat(errorBody.getMessage()).contains("Validation Failed").contains("Rejected");
  }
}
