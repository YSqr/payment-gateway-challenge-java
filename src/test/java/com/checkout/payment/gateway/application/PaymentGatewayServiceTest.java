package com.checkout.payment.gateway.application;

import com.checkout.payment.gateway.domain.model.BankResult;
import com.checkout.payment.gateway.domain.model.Payment;
import com.checkout.payment.gateway.domain.model.PaymentStatus;
import com.checkout.payment.gateway.domain.model.PaymentsRepository;
import com.checkout.payment.gateway.domain.service.AcquiringBank;
import com.checkout.payment.gateway.infrastructure.exception.EventProcessingException;
import com.checkout.payment.gateway.interfaces.payment.web.dto.PaymentRequest;
import com.checkout.payment.gateway.interfaces.payment.web.dto.PaymentResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentGatewayServiceTest {
  @Mock
  private PaymentsRepository paymentsRepository;

  @Mock
  private AcquiringBank acquiringBank;

  @InjectMocks
  private PaymentGatewayService paymentGatewayService;

  @Test
  void processPayment_ShouldGenerateIdAndMaskCard_WhenBankAuthorizes() {
    // Given
    PaymentRequest request = new PaymentRequest();
    request.setCardNumber("1234567812345678");
    request.setExpiryMonth(12);
    request.setExpiryYear(2025);
    request.setAmount(100L);
    request.setCurrency("USD");
    request.setCvv("123");

    // Mock bank response
    when(acquiringBank.process(eq(request), any(UUID.class))).thenReturn(
        BankResult.builder().status(PaymentStatus.AUTHORIZED).authorizationCode("abc123").build());

    // When
    PaymentResponse response = paymentGatewayService.processPayment(request);

    // Then
    // 1. Verify Response
    assertThat(response.getId()).isNotNull();
    assertThat(response.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
    assertThat(response.getCard().getMaskedNumber()).isNull();

    // 2. Verify Repository Persistence
    ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
    verify(paymentsRepository).save(paymentCaptor.capture());

    Payment savedPayment = paymentCaptor.getValue();
    assertThat(savedPayment.getId()).isEqualTo(response.getId());
    assertThat(savedPayment.getCardLastFour()).isEqualTo("5678");
    assertThat(savedPayment.getMaskedCardNumber()).isEqualTo("************5678");
    assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
  }

  @Test
  void getPaymentDetails_ShouldReturnMaskedNumber_WhenFound() {
    UUID id = UUID.randomUUID();
    Payment payment = Payment.builder() // 假设 Entity 还有 Builder
        .id(id)
        .status(PaymentStatus.AUTHORIZED)
        .amount(200L)
        .currency("EUR")
        .cardLastFour("4242")
        .maskedCardNumber("************4242")
        .cardExpiryMonth(10)
        .cardExpiryYear(2026)
        .build();

    when(paymentsRepository.get(id)).thenReturn(Optional.of(payment));

    var response = paymentGatewayService.getPaymentById(id);

    assertThat(response.getId()).isEqualTo(id);
    assertThat(response.getCard().getMaskedNumber()).isEqualTo("************4242");
  }

  @Test
  void getPaymentDetails_ShouldThrowException_WhenNotFound() {
    UUID id = UUID.randomUUID();
    when(paymentsRepository.get(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> paymentGatewayService.getPaymentById(id))
        .isInstanceOf(EventProcessingException.class)
        .hasMessageContaining("Invalid ID");
  }
}