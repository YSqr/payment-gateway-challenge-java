package com.checkout.payment.gateway.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Currency;
import java.util.Set;
import java.util.stream.Collectors;

public class ISO4217CurrencyValidator implements ConstraintValidator<ISO4217Currency, String> {
  private static final Set<String> AVAILABLE_CURRENCIES = Currency.getAvailableCurrencies()
      .stream()
      .map(Currency::getCurrencyCode)
      .collect(Collectors.toSet());

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if(value == null) {
      return true;
    }

    return AVAILABLE_CURRENCIES.contains(value);
  }
}
