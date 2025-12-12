package com.checkout.payment.gateway.application;

import com.checkout.payment.gateway.domain.model.PaymentsRepository;
import com.checkout.payment.gateway.infrastructure.exception.EventProcessingException;
import com.checkout.payment.gateway.interfaces.payment.web.dto.PaymentRequest;
import com.checkout.payment.gateway.interfaces.payment.web.dto.PaymentResponse;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PaymentGatewayService {

  private static final Logger LOG = LoggerFactory.getLogger(PaymentGatewayService.class);

  private final PaymentsRepository paymentsRepository;

  public PaymentGatewayService(PaymentsRepository paymentsRepository) {
    this.paymentsRepository = paymentsRepository;
  }

  public PaymentResponse getPaymentById(UUID id) {
    LOG.debug("Requesting access to to payment with ID {}", id);
    return paymentsRepository.get(id).orElseThrow(() -> new EventProcessingException("Invalid ID"));
  }

  public UUID processPayment(PaymentRequest paymentRequest) {
    return UUID.randomUUID();
  }
}
