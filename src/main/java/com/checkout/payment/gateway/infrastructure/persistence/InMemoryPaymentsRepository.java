package com.checkout.payment.gateway.infrastructure.persistence;

import com.checkout.payment.gateway.domain.model.Payment;
import com.checkout.payment.gateway.domain.model.PaymentsRepository;
import org.springframework.stereotype.Repository;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Repository
public class InMemoryPaymentsRepository implements PaymentsRepository {

  private final Map<UUID, Payment> storage = new ConcurrentHashMap<>();
  private final Map<String, Payment> idempotencyIndex = new ConcurrentHashMap<>();

  private final ReadWriteLock lock = new ReentrantReadWriteLock();

  @Override
  public Payment save(Payment payment) {
    lock.writeLock().lock();
    try {
      if(payment.getIdempotencyKey() != null && idempotencyIndex.containsKey(payment.getIdempotencyKey())) {
        return idempotencyIndex.get(payment.getIdempotencyKey());
      }
      
      if (payment.getId() == null) {
        payment.setId(UUID.randomUUID());
      }
      storage.put(payment.getId(), payment);

      if (payment.getIdempotencyKey() != null) {
        idempotencyIndex.put(payment.getIdempotencyKey(), payment);
      }
      return payment;
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public Optional<Payment> get(UUID id) {
    lock.readLock().lock();
    try {
      return Optional.ofNullable(storage.get(id));
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public Optional<Payment> getByIdempotencyKey(String key) {
    lock.readLock().lock();
    try {
      return Optional.ofNullable(idempotencyIndex.get(key));
    } finally {
      lock.readLock().unlock();
    }
  }
}
