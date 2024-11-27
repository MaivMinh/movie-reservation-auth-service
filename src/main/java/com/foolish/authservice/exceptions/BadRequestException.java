package com.foolish.authservice.exceptions;

import lombok.NoArgsConstructor;

import java.util.Map;

@NoArgsConstructor
public class BadRequestException extends AbstractException{

  public BadRequestException(String message, Map<String, String> details) {
    super(message, details);
  }
}
