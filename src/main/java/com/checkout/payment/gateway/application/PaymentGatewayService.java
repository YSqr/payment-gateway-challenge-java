package com.checkout.payment.gateway.application;

import com.checkout.payment.gateway.common.Util;
import com.checkout.payment.gateway.domain.model.BankResult;
import com.checkout.payment.gateway.domain.model.Payment;
import com.checkout.payment.gateway.domain.model.PaymentStatus;
import com.checkout.payment.gateway.domain.model.PaymentsRepository;
import com.checkout.payment.gateway.domain.service.AcquiringBank;
import com.checkout.payment.gateway.infrastructure.exception.EventProcessingException;
import com.checkout.payment.gateway.infrastructure.exception.PaymentNotFoundException;
import com.checkout.payment.gateway.interfaces.payment.web.dto.PaymentCardInfo;
import com.checkout.payment.gateway.interfaces.payment.web.dto.PaymentRequest;
import com.checkout.payment.gateway.interfaces.payment.web.dto.PaymentResponse;
import java.time.Instant;
import java.util.Optional;
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

    return paymentsRepository.get(id)
        .map(payment -> mapToResponse(payment, true))
        .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));
  }

  public PaymentResponse processPayment(PaymentRequest paymentRequest, String idempotencyKey) {
    // idempotency check
    if(idempotencyKey != null) {
      Optional<Payment> existingPaymentOption = paymentsRepository.getByIdempotencyKey(idempotencyKey);
      if(existingPaymentOption.isPresent()) {
        Payment existingPayment = existingPaymentOption.get();
        LOG.info("Idempotency hit for key {}. Current status: {}", idempotencyKey, existingPayment.getStatus());

        // if it is already final status (authorized/declined/rejected), directly return
        if (existingPayment.getStatus() == PaymentStatus.AUTHORIZED ||
            existingPayment.getStatus() == PaymentStatus.DECLINED ||
            existingPayment.getStatus() == PaymentStatus.REJECTED) {
          return mapToResponse(existingPayment, false);
        }

        // TODO: in real production environment, we need to check with bank
        // - query bank's status check API
        // - if bank already process the payment, update database and return the status accordingly
        // - if bank haven't seen the payment, we retry
        // - if bank generate error again, raise EventProcessingError again
        // in this assignment we just return directly
        return mapToResponse(existingPayment, false);
      }
    }

    UUID paymentId = UUID.randomUUID();

    LOG.info("Starting payment processing for payment {}: {}", paymentId, paymentRequest);

    String cardNumber = paymentRequest.getCardNumber();
    String lastFour = cardNumber.substring(cardNumber.length() - 4);
    String maskedNumber = Util.maskCardNumber(cardNumber);

    Payment payment = Payment.builder()
        .id(paymentId)
        .idempotencyKey(idempotencyKey)
        .status(PaymentStatus.PENDING)
        .amount(paymentRequest.getAmount())
        .currency(paymentRequest.getCurrency())
        .cardLastFour(lastFour)
        .cardExpiryMonth(paymentRequest.getExpiryMonth())
        .cardExpiryYear(paymentRequest.getExpiryYear())
        .maskedCardNumber(maskedNumber)
        .createdAt(Instant.now())
        .build();

    paymentsRepository.save(payment); // save before call bank

    try {
      BankResult result = acquiringBank.process(paymentRequest, paymentId);

      payment.setStatus(result.getStatus());
      payment.setAuthorizationCode(result.getAuthorizationCode());
      paymentsRepository.save(payment);

      LOG.info("Payment {} successfully processed with status {}", paymentId, result.getStatus());
      return mapToResponse(payment, false);
    } catch (EventProcessingException e) {
      LOG.error("Error processing payment {}", paymentId, e);
      payment.setStatus(PaymentStatus.UNKNOWN);
      paymentsRepository.save(payment);

      throw e;
    }
  }

  private PaymentResponse mapToResponse(Payment payment, boolean includeMaskedCardNumber) {
    var cardInfoBuilder = PaymentCardInfo.builder()
        .lastFour(payment.getCardLastFour())
        .expiryMonth(payment.getCardExpiryMonth())
        .expiryYear(payment.getCardExpiryYear());

    if (includeMaskedCardNumber) {
      // for GET response only
      cardInfoBuilder.maskedNumber(payment.getMaskedCardNumber());
    }

    return PaymentResponse.builder()
        .id(payment.getId())
        .status(payment.getStatus())
        .card(cardInfoBuilder.build())
        .currency(payment.getCurrency())
        .amount(payment.getAmount())
        .build();
  }


}
