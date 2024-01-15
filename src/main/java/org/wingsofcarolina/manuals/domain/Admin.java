package org.wingsofcarolina.manuals.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Transient;
import org.wingsofcarolina.manuals.domain.dao.AdminDAO;
import org.wingsofcarolina.manuals.persistence.Persistence;

@Entity("Admins")
public class Admin implements Person {
	private static AdminDAO dao = new AdminDAO();
	
	@Id
	@JsonIgnore
	private ObjectId id;
	
	private long userId;
	private String email;
	private String name;
	private String uuid;
	@JsonIgnore
	private String password;
	@Transient
	private String token;
	private List<Role> roles;
	
	public static String ID_KEY = "Admins";

	public Admin() {}
	
	public Admin(String email, String fullName, List<Role> roles) {
		this.email = email;
		this.name = fullName;
		this.roles = roles;
		this.uuid = UUID.randomUUID().toString();
		this.userId = Persistence.instance().getID(ID_KEY, 1000);
	}
	
	public Admin(String email, Admin admin) {
		this.email = email;
		this.name = admin.getName();
		this.roles = admin.getRoles();
		this.uuid = admin.getUUID();
		this.userId = Persistence.instance().getID(ID_KEY, 1000);
	}

	public long getId() {
		return userId;
	}
	
	public String getEmail() {
		return email;
	}

	public String getUUID() {
		return uuid;
	}
	
	public String getName() {
		return name;
	}

	public void setFullName(String fullName) {
		this.name = fullName;
	}

	public Boolean isAdmin() {
		if (roles != null) {
			return roles.contains(Role.ADMIN);
		} else {
			return false;
		}
	}
	
	public Boolean isMember() {
		return false;
	}
	
	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public List<Role> getRoles() {
		return roles;
	}
	
	public List<Role> addRole(Role role) {
		roles.add(role);
		return roles;
	}

	@Override
	public String toString() {
		return "User [email=" + email + ", name=" + name + ", isAdmin=" + isAdmin() + "]";
	}

	/*
	 * Database Management Functionality
	 */
	public static long count() {
		return dao.count();
	}
	
	public static void drop() {
		dao.drop();
	}
	
	@SuppressWarnings("unchecked")
	public static List<Admin> getAll() {
		return (List<Admin>) dao.getAll();
	}

	public static Admin getByID(long id) {
		return (Admin) dao.getByID(id);
	}

	public static Admin getByUUID(String uuid) {
		return dao.getByUUID(uuid);
	}

	public static Admin getByEmail(String email) {
		return dao.getByEmail(email);
	}
	
	public void save() {
		dao.save(this);
	}
	
	public void delete() {
		dao.delete(this);
	}

}
