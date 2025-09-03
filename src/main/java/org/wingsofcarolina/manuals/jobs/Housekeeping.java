package org.wingsofcarolina.manuals.jobs;

import java.util.concurrent.TimeUnit;
import org.knowm.sundial.Job;
import org.knowm.sundial.annotations.SimpleTrigger;
import org.knowm.sundial.exceptions.JobInterruptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.manuals.domain.VerificationCode;

//@CronTrigger(cron = "0/100 * * * * ?")  // Fire every minute, for testing
//@CronTrigger(cron = "0 0 4 * * ?")  // Fire 4am every day
@SimpleTrigger(repeatInterval = 15, timeUnit = TimeUnit.MINUTES)
public class Housekeeping extends Job {

  private static final Logger LOG = LoggerFactory.getLogger(Housekeeping.class);

  @Override
  public void doRun() throws JobInterruptException {
    LOG.debug("Housekeeping triggered .....");

    VerificationCode.cleanCache();

    LOG.debug("Housekeeping completed.");
  }
}
