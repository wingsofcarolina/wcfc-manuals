package org.wingsofcarolina.manuals.domain.dao;

import dev.morphia.Datastore;
import dev.morphia.query.Query;
import dev.morphia.query.filters.Filters;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.manuals.domain.Admin;
import org.wingsofcarolina.manuals.domain.Member;
import org.wingsofcarolina.manuals.persistence.Persistence;

public class AdminDAO extends SuperDAO {

  @SuppressWarnings("unused")
  private static final Logger LOG = LoggerFactory.getLogger(AdminDAO.class);

  public AdminDAO() {
    super(Admin.class);
  }

  public Admin getByEmail(String email) {
    Datastore ds = Persistence.instance().datastore();
    Query<Admin> query = ds.find(Admin.class);
    List<Admin> users = query.filter(Filters.eq("email", email)).iterator().toList();
    if (users.size() > 0) {
      return users.get(0);
    } else {
      return null;
    }
  }

  public Admin getByUUID(String uuid) {
    Datastore ds = Persistence.instance().datastore();
    Query<Admin> query = ds.find(Admin.class);
    List<Admin> users = query.filter(Filters.eq("uuid", uuid)).iterator().toList();
    if (users.size() > 0) {
      return users.get(0);
    } else {
      return null;
    }
  }
}
