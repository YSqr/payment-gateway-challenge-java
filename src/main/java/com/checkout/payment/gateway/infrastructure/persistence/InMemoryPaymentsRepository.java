package com.checkout.payment.gateway.infrastructure.persistence;

import com.checkout.payment.gateway.domain.model.Payment;
import com.checkout.payment.gateway.domain.model.PaymentsRepository;
import com.checkout.payment.gateway.interfaces.payment.web.dto.PaymentResponse;
import org.springframework.stereotype.Repository;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryPaymentsRepository implements PaymentsRepository {

  private final Map<UUID, PaymentResponse> storage = new ConcurrentHashMap<>();

  @Override
  public void save(PaymentResponse payment) {
    storage.put(payment.getId(), payment);
  }

  @Override
  public Optional<PaymentResponse> get(UUID id) {
    return Optional.ofNullable(storage.get(id));
  }
}
