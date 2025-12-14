package com.checkout.payment.gateway.interfaces.payment.web;

import com.checkout.payment.gateway.application.PaymentGatewayService;
import java.util.UUID;
import com.checkout.payment.gateway.interfaces.payment.web.dto.PaymentResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentGatewayController {

  private final PaymentGatewayService paymentGatewayService;

  public PaymentGatewayController(PaymentGatewayService paymentGatewayService) {
    this.paymentGatewayService = paymentGatewayService;
  }

  @GetMapping("{id}")
  public ResponseEntity<PaymentResponse> getPostPaymentEventById(@PathVariable UUID id) {
    return new ResponseEntity<PaymentResponse>(paymentGatewayService.getPaymentById(id), HttpStatus.OK);
  }


}
