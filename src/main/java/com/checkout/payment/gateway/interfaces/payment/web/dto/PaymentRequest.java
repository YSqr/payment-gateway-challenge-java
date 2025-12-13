package com.checkout.payment.gateway.interfaces.payment.web.dto;

import com.checkout.payment.gateway.common.validation.ISO4217Currency;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import java.time.YearMonth;

@Getter
@Setter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class PaymentRequest {

  @NotBlank(message = "Card number is required")
  @Pattern(regexp = "^[0-9]{14,19}$", message = "Invalid card number format")
  private String cardNumber;

  @NotNull(message = "Expiry month is required")
  @Min(value = 1, message = "Expiry month is between 1 and 12")
  @Max(value = 12, message = "Expiry month is between 1 and 12")
  private Integer expiryMonth;

  @NotNull(message = "Expiry year is required")
  @Min(value = 2025, message = "Expiry year must not be in the past")
  private Integer expiryYear;

  @NotBlank(message = "Currency is required")
  @Pattern(regexp = "^[A-Z]{3}", message = "Currency must be ISO currency code (3 characters upper case letters)")
  @ISO4217Currency(message = "Currency must be a valid ISO 4217 code")
  private String currency;

  @NotNull(message = "Amount is required")
  @Min(value = 1, message = "Amount must be positive")
  private Long amount;

  @NotBlank(message = "CVV is required")
  @Pattern(regexp = "^[0-9]{3,4}", message = "CVV must be 3-4 numeric characters")
  private String cvv;

  @JsonIgnore
  @AssertTrue(message = "Card has expired")
  public boolean isValidExpiryDate() {
    if(expiryYear == null || expiryMonth == null) return true; // leave it to @NotNull check

    YearMonth currentMonth = YearMonth.now();
    YearMonth inputMonth = YearMonth.of(expiryYear, expiryMonth);

    return !inputMonth.isBefore((currentMonth));
  }

  @Override
  public String toString() {
    return "PaymentRequest{" +
        "cardNumber=*******" +  cardNumber.substring(cardNumber.length() - 4) +
        ", expiryMonth=" + expiryMonth +
        ", expiryYear=" + expiryYear +
        ", currency='" + currency + '\'' +
        ", amount=" + amount +
        '}';
  }
}
