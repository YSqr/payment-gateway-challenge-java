package com.checkout.payment.gateway.infrastructure.persistence;

import com.checkout.payment.gateway.domain.model.Payment;
import com.checkout.payment.gateway.domain.model.PaymentsRepository;
import org.springframework.stereotype.Repository;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryPaymentsRepository implements PaymentsRepository {

  private final Map<UUID, Payment> storage = new ConcurrentHashMap<>();

  @Override
  public Payment save(Payment payment) {
    if(payment.getId() == null) {
      payment.setId(UUID.randomUUID());
    }
    storage.put(payment.getId(), payment);
    return payment;
  }

  @Override
  public Optional<Payment> get(UUID id) {
    return Optional.ofNullable(storage.get(id));
  }
}
