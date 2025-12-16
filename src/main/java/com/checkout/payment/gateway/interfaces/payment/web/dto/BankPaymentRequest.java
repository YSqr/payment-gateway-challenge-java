package com.checkout.payment.gateway.interfaces.payment.web.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BankPaymentRequest {
  private String cardNumber;
  private String expiryDate;
  private String currency;
  private Long amount;
  private String cvv;
}
