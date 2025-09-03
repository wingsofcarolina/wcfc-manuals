package org.wingsofcarolina.manuals.common;

public class APIError {

  private String message;

  public APIError() {}

  public APIError(String message) {
    this.message = message;
  }

  public String message() {
    return message;
  }
}
