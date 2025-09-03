package org.wingsofcarolina.manuals.domain;

public interface Person {
  public String getName();

  public String getEmail();

  public String getUUID();

  public Boolean isAdmin();

  public Boolean isMember();

  public static Person getPerson(String uuid) {
    Person person = Member.getByUUID(uuid);
    if (person == null) {
      person = Admin.getByUUID(uuid);
    }
    return person;
  }
}
