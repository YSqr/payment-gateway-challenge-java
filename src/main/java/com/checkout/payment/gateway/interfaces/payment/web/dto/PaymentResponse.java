package com.checkout.payment.gateway.interfaces.payment.web.dto;

import com.checkout.payment.gateway.domain.model.PaymentStatus;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class PaymentResponse {
  private UUID id;
  private PaymentStatus status;
  private String currency;
  private Long amount;
  private PaymentCardInfo card;
}
