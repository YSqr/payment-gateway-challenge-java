package com.checkout.payment.gateway.infrastructure.exception;

import lombok.Getter;
import java.util.UUID;

@Getter
public class UpstreamTimeoutException extends RuntimeException {

  private final UUID paymentId;

  public UpstreamTimeoutException(String message, UUID paymentId, Throwable cause) {
    super(message, cause);
    this.paymentId = paymentId;
  }
}
