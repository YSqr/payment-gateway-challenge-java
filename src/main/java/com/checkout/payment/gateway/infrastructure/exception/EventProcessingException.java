package com.checkout.payment.gateway.infrastructure.exception;

import lombok.Getter;
import java.util.UUID;

@Getter
public class EventProcessingException extends RuntimeException{
  private final UUID paymentId;

  public EventProcessingException(String message, UUID paymentId) {
    super(message);
    this.paymentId = paymentId;
  }

  public EventProcessingException(String message) {
    super(message);
    this.paymentId = null;
  }

}
