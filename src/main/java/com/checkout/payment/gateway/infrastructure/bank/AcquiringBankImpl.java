package com.checkout.payment.gateway.infrastructure.bank;

import com.checkout.payment.gateway.domain.model.BankResult;
import com.checkout.payment.gateway.domain.model.PaymentStatus;
import com.checkout.payment.gateway.domain.service.AcquiringBank;
import com.checkout.payment.gateway.infrastructure.exception.EventProcessingException;
import com.checkout.payment.gateway.interfaces.payment.web.dto.BankPaymentRequest;
import com.checkout.payment.gateway.interfaces.payment.web.dto.BankPaymentResponse;
import com.checkout.payment.gateway.interfaces.payment.web.dto.PaymentRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import java.util.UUID;

@Slf4j
@Service
public class AcquiringBankImpl implements AcquiringBank {
  private final RestTemplate restTemplate;

  public AcquiringBankImpl(RestTemplateBuilder builder, AcquiringBankProperties properties) {
    log.info("initializing AcquiringBank with properties {}", properties);
    this.restTemplate = builder
        .rootUri(properties.getUrl())
        .setConnectTimeout(properties.getConnTimeout())
        .setReadTimeout(properties.getReadTimeout())
        .build();
  }

  @Override
  public BankResult process(PaymentRequest request, UUID paymentId) {
    try {
      BankPaymentRequest bankPaymentRequest = BankPaymentRequest.builder()
          .cardNumber(request.getCardNumber())
          .currency(request.getCurrency())
          .amount(request.getAmount())
          .cvv(request.getCvv())
          .expiryDate(String.format("%02d/%d", request.getExpiryMonth(), request.getExpiryYear()))
          .build();

      // set payment id for bank to trace or dedup
      HttpHeaders headers = new HttpHeaders();
      headers.set("X-Payment-ID", paymentId.toString());

      HttpEntity<BankPaymentRequest> entity = new HttpEntity<>(bankPaymentRequest, headers);

      log.trace("Sending POST for payment {}", paymentId);
      ResponseEntity<BankPaymentResponse> response = restTemplate.postForEntity(
          "/payments",
          entity,
          BankPaymentResponse.class
      );

      if(response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
        boolean authorized = response.getBody().isAuthorized();
        log.info("Bank response for payment {}: authorized={}", paymentId, authorized);
        return BankResult.builder()
            .status(authorized? PaymentStatus.AUTHORIZED: PaymentStatus.DECLINED)
            .authorizationCode(response.getBody().getAuthorizationCode())
            .build();
      }

      // call bank fail
      log.error("Bank returned {} for payment {}", response.getStatusCode(), paymentId);
      return BankResult.builder().status(PaymentStatus.REJECTED).build();

    } catch (HttpClientErrorException e) {
      log.error("Error calling bank for payment {}, bank returned 4xx", paymentId, e);
      return BankResult.builder().status(PaymentStatus.REJECTED).build();
    } catch (HttpServerErrorException e) {
      // IMPORTANT: undetermined status !! we don't know the payment success or not
      log.error("Bank server error for payment {}, received {}", paymentId, e.getStatusCode(), e);
      throw new EventProcessingException("Bank server error: " + e.getStatusCode());
    } catch (ResourceAccessException e) {
      // timeout, undetermined status !!
      log.error("Bank request timeout for payment {}", paymentId, e);
      throw new EventProcessingException("Bank network issue");
    } catch (Exception e) {
      // unknow error
      log.error("Unknown error calling bank for payment {}", paymentId, e);
      throw new EventProcessingException("Unknow bank error");
    }
  }
}
