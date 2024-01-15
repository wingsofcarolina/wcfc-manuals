package org.wingsofcarolina.manuals.model;

import org.wingsofcarolina.manuals.domain.Person;

public class User {
	private String name;
	private String email;
	private String access_token;
	private Boolean admin = false;
	
	public User(Person person) {
		this.name = person.getName();
		this.email = person.getEmail();
		this.admin = person.isAdmin();
	}
	
	public User(String name, String email, String access_token) {
		this.name = name;
		this.email = email;
		this.access_token = access_token;
	}

	public User(String name, String email) {
		this(name, email, null);
	}

	public String getName() {
		return name;
	}

	public String getEmail() {
		return email;
	}
	
	public Boolean getAdmin() {
		if (email.contentEquals("dfrye@planez.co")) {
			return true;
		} else if (email.contentEquals("dwight@openweave.org")) {
			return true;
		} else if (email.contentEquals("kwcycler@gmail.com")) {
			return true;
		}
		return admin;
	}

	public void setAdmin(Boolean admin) {
		this.admin = admin;
	}
	
	public String getAccess_token() {
		return access_token;
	}

	public static User userFromMock(String details) {
		User mock = null;
		if ( ! details.equals("none") ) {
			String items[] = details.split(":");
			
			mock = new User(items[0], items[1], null);
		}
		return mock;
	}

	@Override
	public String toString() {
		return "User [name=" + name + ", email=" + email + ", admin=" + admin + "]";
	}
}
