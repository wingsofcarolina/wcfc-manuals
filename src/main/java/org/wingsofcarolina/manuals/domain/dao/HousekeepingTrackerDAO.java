package org.wingsofcarolina.manuals.domain.dao;

import dev.morphia.Datastore;
import dev.morphia.query.Query;
import dev.morphia.query.filters.Filters;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.manuals.domain.HousekeepingTracker;
import org.wingsofcarolina.manuals.persistence.Persistence;

public class HousekeepingTrackerDAO extends SuperDAO {

  @SuppressWarnings("unused")
  private static final Logger LOG = LoggerFactory.getLogger(HousekeepingTrackerDAO.class);

  public HousekeepingTrackerDAO() {
    super(HousekeepingTracker.class);
  }

  /**
   * Get housekeeping tracker by tracker ID
   */
  public HousekeepingTracker getByTrackerId(String trackerId) {
    try {
      Datastore ds = Persistence.instance().datastore();

      // Ensure the collection exists by checking if it exists first
      ensureCollectionExists(ds, "HousekeepingTracker");

      Query<HousekeepingTracker> query = ds.find(HousekeepingTracker.class);
      List<HousekeepingTracker> trackers = query
        .filter(Filters.eq("trackerId", trackerId))
        .iterator()
        .toList();
      if (trackers.size() > 0) {
        return trackers.get(0);
      } else {
        return null;
      }
    } catch (Exception e) {
      // Log the error but don't fail - return null so a new tracker will be created
      LOG.warn(
        "Error querying HousekeepingTracker collection, will create new tracker: {}",
        e.getMessage()
      );
      return null;
    }
  }

  /**
   * Ensure the collection exists, creating it if it doesn't
   */
  private void ensureCollectionExists(Datastore ds, String collectionName) {
    try {
      // Check if collection exists by listing collections
      boolean collectionExists = false;
      for (String name : ds.getDatabase().listCollectionNames()) {
        if (name.equals(collectionName)) {
          collectionExists = true;
          break;
        }
      }

      // Create collection if it doesn't exist
      if (!collectionExists) {
        LOG.info("Creating MongoDB collection: {}", collectionName);
        ds.getDatabase().createCollection(collectionName);
      }
    } catch (Exception e) {
      // Log but don't fail - the collection might exist or be created automatically
      LOG.debug(
        "Could not ensure collection exists (this is usually fine): {}",
        e.getMessage()
      );
    }
  }
}
