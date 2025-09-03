package org.wingsofcarolina.manuals.persistence;

import com.mongodb.client.MongoClients;
import com.mongodb.client.result.UpdateResult;
import dev.morphia.Datastore;
// import co.planez.padawan.domain.dao.*;
import dev.morphia.Morphia;
import dev.morphia.query.Query;
import dev.morphia.query.experimental.filters.Filters;
import dev.morphia.query.experimental.updates.UpdateOperators;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.manuals.domain.*;

public class Persistence {

  private static final Logger LOG = LoggerFactory.getLogger(Persistence.class);

  private Datastore datastore;

  private static Persistence instance = null;

  public Persistence initialize(String mongodb) {
    if (instance == null) {
      LOG.info("Connecting to MongoDB with '{}'", mongodb);
      datastore = Morphia.createDatastore(MongoClients.create(mongodb), "manuals");
      datastore.getMapper().mapPackage("dev.morphia.example");
      datastore.ensureIndexes();

      // Make this a singleton
      instance = this;
    }
    return this;
  }

  public static Persistence instance() {
    return instance;
  }

  public Datastore datastore() {
    return datastore;
  }

  public AutoIncrement setID(final String key, final long setvalue) {
    AutoIncrement inc = null;
    Datastore ds = Persistence.instance().datastore();
    Query<AutoIncrement> query = ds.find(AutoIncrement.class);
    List<AutoIncrement> autoIncrement = query
      .filter(Filters.eq("id", key))
      .iterator()
      .toList();

    if (autoIncrement == null || autoIncrement.size() == 0) {
      inc = new AutoIncrement(key, setvalue);
      datastore.save(inc);
    } else {
      if (autoIncrement != null && autoIncrement.get(0) != null) {
        inc = autoIncrement.get(0);
        inc.setValue(setvalue);
        datastore.save(inc);
      }
    }
    return inc;
  }

  public long getID(final String key, final long minimumValue) {
    AutoIncrement autoIncrement = null;
    Datastore ds = Persistence.instance().datastore();
    Query<AutoIncrement> query = ds
      .find(AutoIncrement.class)
      .filter(Filters.eq("key", key));
    UpdateResult results = query.update(UpdateOperators.inc("value", 1)).execute();

    // If none is found, we need to create one for the given key.
    if (results.getModifiedCount() == 0) {
      autoIncrement = new AutoIncrement(key, minimumValue);
      datastore.save(autoIncrement);
    } else {
      autoIncrement = query.iterator().toList().get(0);
    }
    return autoIncrement.getValue();
  }
}
