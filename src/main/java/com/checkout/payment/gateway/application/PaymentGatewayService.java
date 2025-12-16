package com.checkout.payment.gateway.application;

import com.checkout.payment.gateway.domain.model.BankResult;
import com.checkout.payment.gateway.domain.model.Payment;
import com.checkout.payment.gateway.domain.model.PaymentsRepository;
import com.checkout.payment.gateway.domain.service.AcquiringBank;
import com.checkout.payment.gateway.infrastructure.exception.EventProcessingException;
import com.checkout.payment.gateway.infrastructure.exception.PaymentNotFoundException;
import com.checkout.payment.gateway.interfaces.payment.web.dto.PaymentCardInfo;
import com.checkout.payment.gateway.interfaces.payment.web.dto.PaymentRequest;
import com.checkout.payment.gateway.interfaces.payment.web.dto.PaymentResponse;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PaymentGatewayService {

  private static final Logger LOG = LoggerFactory.getLogger(PaymentGatewayService.class);

  private final PaymentsRepository paymentsRepository;
  private final AcquiringBank acquiringBank;

  public PaymentGatewayService(PaymentsRepository paymentsRepository, AcquiringBank acquiringBank) {
    this.paymentsRepository = paymentsRepository;
    this.acquiringBank = acquiringBank;
  }

  public PaymentResponse getPaymentById(UUID id) {
    LOG.debug("Requesting access to to payment with ID {}", id);

    Payment payment = paymentsRepository.get(id)
        .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));

    PaymentCardInfo card = PaymentCardInfo.builder()
        .lastFour(payment.getCardLastFour())
        .expiryMonth(payment.getCardExpiryMonth())
        .expiryYear(payment.getCardExpiryYear())
        .maskedNumber(payment.getMaskedCardNumber())
        .build();

    return PaymentResponse.builder()
        .id(payment.getId())
        .status(payment.getStatus())
        .amount(payment.getAmount())
        .currency(payment.getCurrency())
        .card(card)
        .build();
  }

  public PaymentResponse processPayment(PaymentRequest paymentRequest) {
    UUID paymentId = UUID.randomUUID();

    LOG.info("Starting payment processing for payment {}: {}", paymentId, paymentRequest);

    String cardNumber = paymentRequest.getCardNumber();
    String lastFour = cardNumber.substring(cardNumber.length() - 4);
    String maskedNumber = maskCardNumber(cardNumber);

    BankResult result = acquiringBank.process(paymentRequest, paymentId);

    Payment payment = Payment.builder()
        .id(paymentId)
        .status(result.getStatus())
        .authorizationCode(result.getAuthorizationCode())
        .amount(paymentRequest.getAmount())
        .currency(paymentRequest.getCurrency())
        .cardLastFour(lastFour)
        .cardExpiryMonth(paymentRequest.getExpiryMonth())
        .cardExpiryYear(paymentRequest.getExpiryYear())
        .maskedCardNumber(maskedNumber)
        .createdAt(Instant.now())
        .build();

    paymentsRepository.save(payment);

    LOG.info("Payment {} successfully processed with status {}", paymentId, result.getStatus());

    PaymentCardInfo card = PaymentCardInfo.builder()
        .lastFour(lastFour)
        .expiryMonth(paymentRequest.getExpiryMonth())
        .expiryYear(paymentRequest.getExpiryYear())
        .maskedNumber(null)
        .build();

    return PaymentResponse.builder()
        .id(paymentId)
        .status(result.getStatus())
        .amount(paymentRequest.getAmount())
        .currency(paymentRequest.getCurrency())
        .card(card)
        .build();
  }

  private String maskCardNumber(String fullNumber) {
    if (fullNumber == null || fullNumber.length() < 4) {
      return "****"; // Fallback safety
    }
    int length = fullNumber.length();
    // E.g. "1234567812345678" -> "************5678"
    return "*".repeat(length - 4) + fullNumber.substring(length - 4);
  }
}
