package com.checkout.payment.gateway.controller;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.checkout.payment.gateway.application.PaymentGatewayService;
import com.checkout.payment.gateway.domain.model.Payment;
import com.checkout.payment.gateway.domain.model.PaymentStatus;
import com.checkout.payment.gateway.domain.model.PaymentsRepository;
import com.checkout.payment.gateway.interfaces.payment.web.dto.PaymentCardInfo;
import com.checkout.payment.gateway.interfaces.payment.web.dto.PaymentRequest;
import com.checkout.payment.gateway.interfaces.payment.web.dto.PaymentResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentGatewayControllerTest {

  @Autowired
  private MockMvc mvc;
  @Autowired
  PaymentsRepository paymentsRepository;
  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private PaymentGatewayService paymentGatewayService;


  @Test
  void whenPaymentWithIdExistThenCorrectPaymentIsReturned() throws Exception {
    UUID paymentId = UUID.randomUUID();

    Payment payment = Payment.builder()
        .id(paymentId)
        .amount(100L)
        .currency("USD")
        .status(PaymentStatus.AUTHORIZED)
        .cardLastFour("4321")
        .cardExpiryMonth(12)
        .cardExpiryYear(2024)
        .maskedCardNumber("************4321") // GET 必须要有这个
        .createdAt(Instant.now())
        .build();

    paymentsRepository.save(payment);

    mvc.perform(MockMvcRequestBuilders.get("/api/v1/payments/" + payment.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(payment.getStatus().getName()))
        .andExpect(jsonPath("$.card.last_four").value(payment.getCardLastFour()))
        .andExpect(jsonPath("$.card.expiry_month").value(payment.getCardExpiryMonth()))
        .andExpect(jsonPath("$.card.expiry_year").value(payment.getCardExpiryYear()))
        .andExpect(jsonPath("$.currency").value(payment.getCurrency()))
        .andExpect(jsonPath("$.amount").value(payment.getAmount()));
  }

  @Test
  void whenPaymentWithIdDoesNotExistThen404IsReturned() throws Exception {
    mvc.perform(MockMvcRequestBuilders.get("/api/v1/payments/" + UUID.randomUUID()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Payment not found"));
  }

  @Test
  void createPayment_ShouldPassIdempotencyKey_WhenHeaderPresent() throws Exception {
    PaymentRequest request = createValidPaymentRequest();
    UUID paymentId = UUID.randomUUID();
    String idempotencyKey = "key_12345";

    PaymentResponse mockResponse = PaymentResponse.builder()
        .id(paymentId)
        .status(PaymentStatus.PENDING)
        .amount(100L)
        .currency("USD")
        .card(PaymentCardInfo.builder().lastFour("4242").build()) // POST response has no masked number
        .build();

    when(paymentGatewayService.processPayment(any(), eq(idempotencyKey))).thenReturn(mockResponse);
    mvc.perform(post("/api/v1/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .header("Idempotency-Key", "test-header-key"))
        .andExpect(status().isCreated());

    verify(paymentGatewayService).processPayment(any(), eq("test-header-key"));
  }

  private PaymentRequest createValidPaymentRequest() {
    PaymentRequest paymentRequest = new PaymentRequest();
    paymentRequest.setAmount(100L);
    paymentRequest.setCurrency("USD");
    paymentRequest.setCvv("123");
    paymentRequest.setCardNumber("123456789012345");
    paymentRequest.setExpiryMonth(3);
    paymentRequest.setExpiryYear(2030);
    return paymentRequest;
  }
}
