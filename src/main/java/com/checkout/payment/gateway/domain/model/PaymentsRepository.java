package com.checkout.payment.gateway.domain.model;

import java.util.Optional;
import java.util.UUID;

public interface PaymentsRepository {
  Payment save(Payment payment);
  Optional<Payment> get(UUID id);
  Optional<Payment> getByIdempotencyKey(String key);
}
