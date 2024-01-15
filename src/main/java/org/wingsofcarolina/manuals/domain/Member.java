package org.wingsofcarolina.manuals.domain;

import java.util.List;
import java.util.UUID;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Transient;
import org.wingsofcarolina.manuals.domain.dao.MemberDAO;
import org.wingsofcarolina.manuals.persistence.Persistence;

@Entity("Members")
public class Member implements Person {
	private static MemberDAO dao = new MemberDAO();
	
	@Id
	@JsonIgnore
	private ObjectId dbId;
	
	private Long memberId;
	private Integer id;
	private String uuid;
	private String name;
	private String email;
	private Integer level;
	private Boolean admin = false;
	
	@Transient
	private String token;

	
	public static String ID_KEY = "members";

	public Member() {}
	
	public Member(Integer id, String name, String email, Integer level) {
		this.id = id;
		this.name = name;
		this.email = email;
		this.level = level;
		this.uuid = UUID.randomUUID().toString();
		this.memberId = Persistence.instance().getID(ID_KEY, 1000);
	}
	
	public Boolean isAdmin() {
		return false;
	}
	
	public Boolean isMember() {
		return true;
	}
	
	public Integer getId() {
		return id;
	}

	public Long getMemberId() {
		return memberId;
	}

	public void setMemberId(Long memberId) {
		this.memberId = memberId;
	}

	public String getUUID() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}
	
	public void setId(ObjectId id) {
		this.dbId = id;
	}

	public Integer getLevel() {
		return level;
	}

	public void setLevel(Integer level) {
		this.level = level;
	}

	public Boolean getAdmin() {
		return admin;
	}

	public void setAdmin(Boolean admin) {
		this.admin = admin;
	}

	@Override
	public String toString() {
		return "Member [id=" + id + ", name=" + name + ", email=" + email + "]";
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
	public static List<Member> getAll() {
		return (List<Member>) dao.getAll();
	}

	public static List<Member> getAllForSection(String section) {
		return dao.getAllForSection(section);
	}
	
	public static Member getByID(long id) {
		return (Member) dao.getByID(id);
	}
	
	public static Member getByUUID(String uuid) {
		return dao.getByUUID(uuid);
	}

	public static Member getByEmail(String email) {
		return dao.getByEmail(email);
	}
	
	public void save() {
		dao.save(this);
	}
	
	public void delete() {
		dao.delete(this);
	}
}
