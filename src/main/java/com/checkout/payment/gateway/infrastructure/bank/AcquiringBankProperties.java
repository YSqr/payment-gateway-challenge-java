package com.checkout.payment.gateway.infrastructure.bank;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;
import java.time.Duration;

@Data
@Validated
@Configuration
@ConfigurationProperties(prefix="acquiring-bank")
public class AcquiringBankProperties {

  @NotNull
  private String url;

  private Duration readTimeout = Duration.ofSeconds(10);
  private Duration connTimeout = Duration.ofSeconds(10);
}
