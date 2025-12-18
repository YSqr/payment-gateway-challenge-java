package com.checkout.payment.gateway.common;

public class Util {
  public static String maskCardNumber(String fullNumber) {
    if (fullNumber == null || fullNumber.length() < 4) {
      return "****"; // Fallback safety
    }
    int length = fullNumber.length();
    // E.g. "1234567812345678" -> "************5678"
    return "*".repeat(length - 4) + fullNumber.substring(length - 4);
  }
}
