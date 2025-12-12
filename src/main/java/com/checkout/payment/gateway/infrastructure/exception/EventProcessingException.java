package com.checkout.payment.gateway.infrastructure.exception;

public class EventProcessingException extends RuntimeException{
  public EventProcessingException(String message) {
    super(message);
  }
}
