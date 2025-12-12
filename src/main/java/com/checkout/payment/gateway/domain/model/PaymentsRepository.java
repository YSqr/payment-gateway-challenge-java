package com.checkout.payment.gateway.domain.model;

import com.checkout.payment.gateway.interfaces.payment.web.dto.PaymentResponse;
import java.util.Optional;
import java.util.UUID;

public interface PaymentsRepository {
  void save(PaymentResponse payment);
  Optional<PaymentResponse> get(UUID id);
}
