package org.wingsofcarolina.manuals.model;

import org.wingsofcarolina.manuals.domain.Person;

public class User {

  private String name;
  private String email;
  private Boolean admin = false;

  public User(Person person) {
    this.name = person.getName();
    this.email = person.getEmail();
    this.admin = person.isAdmin();
  }

  public User(String name, String email) {
    this.name = name;
    this.email = email;
  }

  public String getName() {
    return name;
  }

  public String getEmail() {
    return email;
  }

  public Boolean getAdmin() {
    // Check database for admin status
    org.wingsofcarolina.manuals.domain.Member member = org.wingsofcarolina.manuals.domain.Member.getByEmail(
      this.email
    );
    return member != null ? member.getAdmin() : false;
  }

  public void setAdmin(Boolean admin) {
    this.admin = admin;
  }

  public static User userFromMock(String details) {
    User mock = null;
    if (!details.equals("none")) {
      String items[] = details.split(":");

      mock = new User(items[0], items[1]);
    }
    return mock;
  }

  @Override
  public String toString() {
    return "User [name=" + name + ", email=" + email + ", admin=" + admin + "]";
  }
}
