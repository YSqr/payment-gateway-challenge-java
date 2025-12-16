package com.checkout.payment.gateway.interfaces.payment.web.exception;

import com.checkout.payment.gateway.infrastructure.exception.EventProcessingException;
import com.checkout.payment.gateway.infrastructure.exception.PaymentNotFoundException;
import com.checkout.payment.gateway.interfaces.payment.web.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import java.util.stream.Collectors;

@ControllerAdvice
public class CommonExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(CommonExceptionHandler.class);

  @ExceptionHandler(PaymentNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleNotFound(PaymentNotFoundException ex) {
    return new ResponseEntity<>(new ErrorResponse(ex.getMessage()), HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(EventProcessingException.class)
  public ResponseEntity<ErrorResponse> handleException(EventProcessingException ex) {
    LOG.error("Processing Error", ex);

    // if upstream timeout
    if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("timeout")) {
      return new ResponseEntity<>(
          new ErrorResponse("Upstream provider timed out. Please check status later." + ex.getMessage()),
          HttpStatus.GATEWAY_TIMEOUT
      );
    }

    // other upstream error
    return new ResponseEntity<>(
        new ErrorResponse("Error processing payment with upstream provider. Please check later." + ex.getMessage()),
        HttpStatus.BAD_GATEWAY
    );
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {

    String errorMessage = ex.getBindingResult().getAllErrors().stream()
        .map(error -> {
          String fieldName = ((FieldError) error).getField();
          String message = error.getDefaultMessage();
          return fieldName + ": " + message;
        })
        .collect(Collectors.joining(", "));

    return new ResponseEntity<>(
        new ErrorResponse("Payment Rejected. Validation Failed: " + errorMessage),
        HttpStatus.BAD_REQUEST
    );
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
    LOG.error("Unexpected error", ex);
    return new ResponseEntity<>(new ErrorResponse("Internal Server Error"), HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
