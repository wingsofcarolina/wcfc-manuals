package org.wingsofcarolina.manuals.domain.dao;

import dev.morphia.Datastore;
import dev.morphia.query.Query;
import dev.morphia.query.filters.Filters;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.manuals.domain.VerificationCode;
import org.wingsofcarolina.manuals.persistence.Persistence;

public class VerificationCodeDAO extends SuperDAO {

  @SuppressWarnings("unused")
  private static final Logger LOG = LoggerFactory.getLogger(VerificationCodeDAO.class);

  public VerificationCodeDAO() {
    super(VerificationCode.class);
  }

  public VerificationCode getByPersonUUID(String uuid) {
    Datastore ds = Persistence.instance().datastore();
    Query<VerificationCode> query = ds.find(VerificationCode.class);
    List<VerificationCode> users = query
      .filter(Filters.eq("uuid", uuid))
      .iterator()
      .toList();
    if (users.size() > 0) {
      return users.get(0);
    } else {
      return null;
    }
  }

  public VerificationCode getByCode(Integer code) {
    Datastore ds = Persistence.instance().datastore();
    Query<VerificationCode> query = ds.find(VerificationCode.class);
    List<VerificationCode> users = query
      .filter(Filters.eq("code", code))
      .iterator()
      .toList();
    if (users.size() > 0) {
      return users.get(0);
    } else {
      return null;
    }
  }
}
