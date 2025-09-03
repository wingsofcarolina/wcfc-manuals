package org.wingsofcarolina.manuals.domain;

public enum Role {
  USER(Names.USER),
  ADMIN(Names.ADMIN);

  public class Names {

    public static final String USER = "USER";
    public static final String ADMIN = "ADMIN";
  }

  private final String label;

  private Role(String label) {
    this.label = label;
  }

  public String toString() {
    return this.label;
  }
}
