package org.wingsofcarolina.manuals.domain.dao;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.manuals.domain.Member;
import org.wingsofcarolina.manuals.persistence.Persistence;

import dev.morphia.Datastore;
import dev.morphia.query.Query;
import dev.morphia.query.experimental.filters.*;

public class MemberDAO extends SuperDAO {
	@SuppressWarnings("unused")
	private static final Logger LOG = LoggerFactory.getLogger(MemberDAO.class);
	
	public MemberDAO() {
		super(Member.class);
	}

	public Member getByEmail(String email) {
		Datastore ds = Persistence.instance().datastore();
		Query<Member> query = ds.find(Member.class);
		List<Member> users = query.filter(Filters.eq("email", email)).iterator().toList();
		if (users.size() > 0) {
			return users.get(0);
		} else {
			return null;
		}
	}

	public Member getByUUID(String uuid) {
		Datastore ds = Persistence.instance().datastore();
		Query<Member> query = ds.find(Member.class);
		List<Member> users = query.filter(Filters.eq("uuid", uuid)).iterator().toList();
		if (users.size() > 0) {
			return users.get(0);
		} else {
			return null;
		}
	}

	public List<Member> getAllForSection(String section) {
		Datastore ds = Persistence.instance().datastore();
		Query<Member> query = ds.find(Member.class);
		List<Member> users = query.filter(Filters.eq("section", section)).iterator().toList();
		return users;
	}
}
