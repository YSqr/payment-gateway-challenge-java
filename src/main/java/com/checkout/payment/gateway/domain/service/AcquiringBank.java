package com.checkout.payment.gateway.domain.service;

import com.checkout.payment.gateway.domain.model.BankResult;
import com.checkout.payment.gateway.interfaces.payment.web.dto.PaymentRequest;
import java.util.UUID;

public interface AcquiringBank {
  BankResult process(PaymentRequest request, UUID paymentId);
}
