package com.checkout.payment.gateway.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
  private UUID id;
  private PaymentStatus status;
  private Long amount;
  private String currency;

  private String cardLastFour;
  private Integer cardExpiryMonth;
  private Integer cardExpiryYear;

  private String maskedCardNumber;

  private String authorizationCode;

  private Instant createdAt;

  @Override
  public boolean equals(Object o) {
    if(this == o) return true;
    if(o == null || getClass() != o.getClass()) return false;
    Payment payment = (Payment) o;
    return id != null && id.equals(payment.id);
  }
}
