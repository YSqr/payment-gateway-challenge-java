package com.checkout.payment.gateway.domain.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum PaymentStatus {
  PENDING("Pending"),
  AUTHORIZED("Authorized"),
  DECLINED("Declined"),
  REJECTED("Rejected"),
  UNKNOWN("Unknown");

  private final String name;

  PaymentStatus(String name) {
    this.name = name;
  }

  @JsonValue
  public String getName() {
    return this.name;
  }
}
