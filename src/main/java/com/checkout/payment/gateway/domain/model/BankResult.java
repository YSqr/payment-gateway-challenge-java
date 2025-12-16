package com.checkout.payment.gateway.domain.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BankResult {
  PaymentStatus status;
  String authorizationCode;
}
