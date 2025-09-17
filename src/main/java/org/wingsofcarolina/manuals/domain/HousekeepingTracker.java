package org.wingsofcarolina.manuals.domain;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import java.util.Date;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.manuals.domain.dao.HousekeepingTrackerDAO;

@Entity("HousekeepingTracker")
public class HousekeepingTracker {

  private static final Logger LOG = LoggerFactory.getLogger(HousekeepingTracker.class);

  private static HousekeepingTrackerDAO dao = new HousekeepingTrackerDAO();

  public static final String SINGLETON_ID = "housekeeping";

  @Id
  private ObjectId id;

  private String trackerId;
  private Date lastRun;

  public HousekeepingTracker() {}

  public HousekeepingTracker(String trackerId) {
    this.trackerId = trackerId;
    this.lastRun = new Date();
  }

  /**
   * Get the singleton housekeeping tracker record
   */
  public static HousekeepingTracker getInstance() {
    HousekeepingTracker tracker = dao.getByTrackerId(SINGLETON_ID);
    if (tracker == null) {
      tracker = new HousekeepingTracker(SINGLETON_ID);
      tracker.save();
    }
    return tracker;
  }

  /**
   * Check if housekeeping is needed (more than 15 minutes since last run)
   */
  public boolean isHousekeepingNeeded() {
    if (lastRun == null) {
      return true;
    }

    long currentTime = System.currentTimeMillis();
    long lastRunTime = lastRun.getTime();
    long fifteenMinutesInMs = 15 * 60 * 1000; // 15 minutes in milliseconds

    return (currentTime - lastRunTime) > fifteenMinutesInMs;
  }

  /**
   * Update the last run time to now
   */
  public void updateLastRun() {
    this.lastRun = new Date();
    this.save();
  }

  // Getters and setters
  public String getTrackerId() {
    return trackerId;
  }

  public void setTrackerId(String trackerId) {
    this.trackerId = trackerId;
  }

  public Date getLastRun() {
    return lastRun;
  }

  public void setLastRun(Date lastRun) {
    this.lastRun = lastRun;
  }

  // Database operations
  public void save() {
    dao.save(this);
  }

  public void delete() {
    dao.delete(this);
  }
}
