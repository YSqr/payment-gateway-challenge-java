package com.checkout.payment.gateway.interfaces.payment.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(Include.NON_NULL) // this allows the same DTO for both POST response (without masked number) and GET response (with masked number)
public class PaymentCardInfo {
  private String lastFour;
  private Integer expiryMonth;
  private Integer expiryYear;
  private String maskedNumber;
}
