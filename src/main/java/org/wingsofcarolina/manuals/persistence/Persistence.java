package org.wingsofcarolina.manuals.persistence;

import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import dev.morphia.Datastore;
import dev.morphia.Morphia;
import dev.morphia.query.Query;
import dev.morphia.query.filters.Filters;
import java.util.List;
import org.bson.Document;
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

      // Entity classes will be mapped automatically when first used in Morphia 2.5+
      // No need for explicit mapping calls

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
    Datastore ds = Persistence.instance().datastore();

    // Use MongoDB driver directly for atomic findOneAndUpdate operation
    MongoCollection<Document> collection = ds.getDatabase().getCollection("IDs");

    Document filter = new Document("_id", key);
    Document update = new Document("$inc", new Document("value", 1));
    FindOneAndUpdateOptions options = new FindOneAndUpdateOptions()
      .returnDocument(ReturnDocument.AFTER)
      .upsert(false);

    Document result = collection.findOneAndUpdate(filter, update, options);

    // If none is found, we need to create one for the given key.
    if (result == null) {
      AutoIncrement autoIncrement = new AutoIncrement(key, minimumValue);
      datastore.save(autoIncrement);
      return autoIncrement.getValue();
    }

    return result.getLong("value");
  }
}
