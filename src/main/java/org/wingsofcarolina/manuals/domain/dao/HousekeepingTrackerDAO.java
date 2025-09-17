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
    Datastore ds = Persistence.instance().datastore();
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
  }
}
