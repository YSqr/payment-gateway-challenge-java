package com.checkout.payment.gateway.controller;


import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.checkout.payment.gateway.domain.model.PaymentStatus;
import com.checkout.payment.gateway.domain.model.PaymentsRepository;
import com.checkout.payment.gateway.interfaces.payment.web.dto.PaymentCardInfo;
import com.checkout.payment.gateway.interfaces.payment.web.dto.PaymentResponse;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentGatewayControllerTest {

  @Autowired
  private MockMvc mvc;
  @Autowired
  PaymentsRepository paymentsRepository;

  @Test
  void whenPaymentWithIdExistThenCorrectPaymentIsReturned() throws Exception {
    PaymentResponse payment = new PaymentResponse();
    payment.setId(UUID.randomUUID());
    payment.setAmount(10L);
    payment.setCurrency("USD");
    payment.setStatus(PaymentStatus.AUTHORIZED);

    PaymentCardInfo card = new PaymentCardInfo();
    card.setExpiryMonth(12);
    card.setExpiryYear(2024);
    card.setLastFour("4321");
    payment.setCard(card);

    paymentsRepository.save(payment);

    mvc.perform(MockMvcRequestBuilders.get("/api/v1/payments/" + payment.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(payment.getStatus().getName()))
        .andExpect(jsonPath("$.card.last_four").value(payment.getCard().getLastFour()))
        .andExpect(jsonPath("$.card.expiry_month").value(payment.getCard().getExpiryMonth()))
        .andExpect(jsonPath("$.card.expiry_year").value(payment.getCard().getExpiryYear()))
        .andExpect(jsonPath("$.currency").value(payment.getCurrency()))
        .andExpect(jsonPath("$.amount").value(payment.getAmount()));
  }

  @Test
  void whenPaymentWithIdDoesNotExistThen404IsReturned() throws Exception {
    mvc.perform(MockMvcRequestBuilders.get("/api/v1/payments/" + UUID.randomUUID()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Page not found"));
  }
}
