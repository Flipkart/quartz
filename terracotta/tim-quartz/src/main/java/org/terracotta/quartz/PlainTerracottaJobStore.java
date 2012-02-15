/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.quartz;

import org.quartz.*;
import org.quartz.Trigger.CompletedExecutionInstruction;
import org.quartz.JobKey;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.spi.ClassLoadHelper;
import org.quartz.spi.OperableTrigger;
import org.quartz.spi.SchedulerSignaler;
import org.quartz.spi.TriggerFiredResult;
import org.terracotta.api.Terracotta;
import org.terracotta.cluster.TerracottaClusterInfo;
import org.terracotta.collections.ConcurrentDistributedMap;
import org.terracotta.locking.LockType;
import org.terracotta.locking.strategy.HashcodeLockStrategy;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;

public class PlainTerracottaJobStore<T extends ClusteredJobStore> implements TerracottaJobStoreExtensions {

  private static final long          WEEKLY                                  = 7 * 24 * 60 * 60 * 1000L;
  private Timer                      updateCheckTimer;
  private volatile T                 clusteredJobStore                       = null;
  private Long                       misfireThreshold                        = null;
  private String                     schedName;
  private String                     synchWrite                              = "false";
  private Long                       estimatedTimeToReleaseAndAcquireTrigger = null;
  private String                     schedInstanceId;
  private int                        threadPoolSize;

  public PlainTerracottaJobStore() {
    //
  }

  public void setSynchronousWrite(String synchWrite) {
    this.synchWrite = synchWrite;
  }

  public void setThreadPoolSize(final int size) {
    this.threadPoolSize = size;
  }

  public List<OperableTrigger> acquireNextTriggers(long noLaterThan, int maxCount, long timeWindow)
      throws JobPersistenceException {
    return clusteredJobStore.acquireNextTriggers(noLaterThan, maxCount, timeWindow);
  }

  public List<String> getCalendarNames() throws JobPersistenceException {
    return clusteredJobStore.getCalendarNames();
  }

  public List<String> getJobGroupNames() throws JobPersistenceException {
    return clusteredJobStore.getJobGroupNames();
  }

  public Set<JobKey> getJobKeys(final GroupMatcher<JobKey> matcher) throws JobPersistenceException {
    return clusteredJobStore.getJobKeys(matcher);
  }

  public int getNumberOfCalendars() throws JobPersistenceException {
    return clusteredJobStore.getNumberOfCalendars();
  }

  public int getNumberOfJobs() throws JobPersistenceException {
    return clusteredJobStore.getNumberOfJobs();
  }

  public int getNumberOfTriggers() throws JobPersistenceException {
    return clusteredJobStore.getNumberOfTriggers();
  }

  public Set<String> getPausedTriggerGroups() throws JobPersistenceException {
    return clusteredJobStore.getPausedTriggerGroups();
  }

  public List<String> getTriggerGroupNames() throws JobPersistenceException {
    return clusteredJobStore.getTriggerGroupNames();
  }

  public Set<TriggerKey> getTriggerKeys(final GroupMatcher<TriggerKey> matcher) throws JobPersistenceException {
    return clusteredJobStore.getTriggerKeys(matcher);
  }

  public List<OperableTrigger> getTriggersForJob(final JobKey jobKey) throws JobPersistenceException {
    return clusteredJobStore.getTriggersForJob(jobKey);
  }

  public Trigger.TriggerState getTriggerState(final TriggerKey triggerKey) throws JobPersistenceException {
    return clusteredJobStore.getTriggerState(triggerKey);
  }

  public synchronized void initialize(ClassLoadHelper loadHelper, SchedulerSignaler signaler)
      throws SchedulerConfigException {
    if (clusteredJobStore != null) { throw new IllegalStateException("already initialized"); }

    clusteredJobStore = lookupOrCreateJobStore(schedName, Boolean.valueOf(synchWrite));
    clusteredJobStore.setThreadPoolSize(threadPoolSize);

    // apply deferred misfire threshold if present
    if (misfireThreshold != null) {
      clusteredJobStore.setMisfireThreshold(misfireThreshold);
      misfireThreshold = null;
    }

    if (estimatedTimeToReleaseAndAcquireTrigger != null) {
      clusteredJobStore.setEstimatedTimeToReleaseAndAcquireTrigger(estimatedTimeToReleaseAndAcquireTrigger);
      estimatedTimeToReleaseAndAcquireTrigger = null;
    }
    clusteredJobStore.setInstanceId(schedInstanceId);
    clusteredJobStore.initialize(loadHelper, signaler);

    // update check
    scheduleUpdateCheck();
  }

  public void pauseAll() throws JobPersistenceException {
    clusteredJobStore.pauseAll();
  }

  public void pauseJob(final JobKey jobKey) throws JobPersistenceException {
    clusteredJobStore.pauseJob(jobKey);
  }

  public Collection<String> pauseJobs(GroupMatcher<JobKey> matcher) throws JobPersistenceException {
    return clusteredJobStore.pauseJobs(matcher);
  }

  public void pauseTrigger(TriggerKey triggerKey) throws JobPersistenceException {
    clusteredJobStore.pauseTrigger(triggerKey);
  }

  public Collection<String> pauseTriggers(GroupMatcher<TriggerKey> matcher) throws JobPersistenceException {
    return clusteredJobStore.pauseTriggers(matcher);
  }

  public void releaseAcquiredTrigger(final OperableTrigger trigger) throws JobPersistenceException {
    clusteredJobStore.releaseAcquiredTrigger(trigger);
  }

  public List<TriggerFiredResult> triggersFired(final List<OperableTrigger> triggers) throws JobPersistenceException {
    return clusteredJobStore.triggersFired(triggers);
  }

  public boolean removeCalendar(String calName) throws JobPersistenceException {
    return clusteredJobStore.removeCalendar(calName);
  }

  public boolean removeJob(JobKey jobKey) throws JobPersistenceException {
    return clusteredJobStore.removeJob(jobKey);
  }

  public boolean removeTrigger(TriggerKey triggerKey) throws JobPersistenceException {
    return clusteredJobStore.removeTrigger(triggerKey);
  }

  public boolean removeJobs(List<JobKey> jobKeys) throws JobPersistenceException {
      return clusteredJobStore.removeJobs(jobKeys);
  }
    
  public boolean removeTriggers(List<TriggerKey> triggerKeys) throws JobPersistenceException {
      return clusteredJobStore.removeTriggers(triggerKeys);
  }
    
  public void storeJobsAndTriggers(Map<JobDetail, List<Trigger>> triggersAndJobs, boolean replace)
            throws ObjectAlreadyExistsException, JobPersistenceException {
    clusteredJobStore.storeJobsAndTriggers(triggersAndJobs, replace);
  }
    

  public boolean replaceTrigger(TriggerKey triggerKey, OperableTrigger newTrigger)
      throws JobPersistenceException {
    return clusteredJobStore.replaceTrigger(triggerKey, newTrigger);
  }

  public void resumeAll() throws JobPersistenceException {
    clusteredJobStore.resumeAll();
  }

  public void resumeJob(JobKey jobKey) throws JobPersistenceException {
    clusteredJobStore.resumeJob(jobKey);
  }

  public Collection<String> resumeJobs(GroupMatcher<JobKey> matcher) throws JobPersistenceException {
    return clusteredJobStore.resumeJobs(matcher);
  }

  public void resumeTrigger(TriggerKey triggerKey) throws JobPersistenceException {
    clusteredJobStore.resumeTrigger(triggerKey);
  }

  public Collection<String> resumeTriggers(GroupMatcher<TriggerKey> matcher) throws JobPersistenceException {
    return clusteredJobStore.resumeTriggers(matcher);
  }

  public Calendar retrieveCalendar(String calName) throws JobPersistenceException {
    return clusteredJobStore.retrieveCalendar(calName);
  }

  public JobDetail retrieveJob(JobKey jobKey) throws JobPersistenceException {
    return clusteredJobStore.retrieveJob(jobKey);
  }

  public OperableTrigger retrieveTrigger(TriggerKey triggerKey) throws JobPersistenceException {
    return clusteredJobStore.retrieveTrigger(triggerKey);
  }

  public boolean checkExists(final JobKey jobKey) throws JobPersistenceException {
    return clusteredJobStore.checkExists(jobKey);
  }

  public boolean checkExists(final TriggerKey triggerKey) throws JobPersistenceException {
    return clusteredJobStore.checkExists(triggerKey);
  }

  public void clearAllSchedulingData() throws JobPersistenceException {
    clusteredJobStore.clearAllSchedulingData();
  }

  public void schedulerStarted() throws SchedulerException {
    clusteredJobStore.schedulerStarted();
  }

  public void schedulerPaused() {
    clusteredJobStore.schedulerPaused();
  }

  public void schedulerResumed() {
    clusteredJobStore.schedulerResumed();
  }

  public void shutdown() {
    if (clusteredJobStore != null) {
      clusteredJobStore.shutdown();
    }
    if (updateCheckTimer != null) {
      updateCheckTimer.cancel();
    }
  }

  public void storeCalendar(String name, Calendar calendar, boolean replaceExisting, boolean updateTriggers)
      throws ObjectAlreadyExistsException, JobPersistenceException {
    clusteredJobStore.storeCalendar(name, calendar, replaceExisting, updateTriggers);
  }

  public void storeJob(JobDetail newJob, boolean replaceExisting) throws ObjectAlreadyExistsException,
      JobPersistenceException {
    clusteredJobStore.storeJob(newJob, replaceExisting);
  }

  public void storeJobAndTrigger(JobDetail newJob, OperableTrigger newTrigger) throws ObjectAlreadyExistsException,
      JobPersistenceException {
    clusteredJobStore.storeJobAndTrigger(newJob, newTrigger);
  }

  public void storeTrigger(OperableTrigger newTrigger, boolean replaceExisting) throws ObjectAlreadyExistsException,
      JobPersistenceException {
    clusteredJobStore.storeTrigger(newTrigger, replaceExisting);
  }

  public boolean supportsPersistence() {
    return true;
  }

  @Override
  public String toString() {
    return clusteredJobStore.toString();
  }

  public void triggeredJobComplete(OperableTrigger trigger, JobDetail jobDetail, CompletedExecutionInstruction triggerInstCode)
      throws JobPersistenceException {
    clusteredJobStore.triggeredJobComplete(trigger, jobDetail, triggerInstCode);
  }

  public synchronized void setMisfireThreshold(long threshold) {
    ClusteredJobStore cjs = clusteredJobStore;
    if (cjs != null) {
      cjs.setMisfireThreshold(threshold);
    } else {
      misfireThreshold = Long.valueOf(threshold);
    }
  }

  public synchronized void setEstimatedTimeToReleaseAndAcquireTrigger(long estimate) {
    ClusteredJobStore cjs = clusteredJobStore;
    if (cjs != null) {
      cjs.setEstimatedTimeToReleaseAndAcquireTrigger(estimate);
    } else {
      this.estimatedTimeToReleaseAndAcquireTrigger = estimate;
    }
  }

  public void setInstanceId(String schedInstId) {
    this.schedInstanceId = schedInstId;
  }

  public void setInstanceName(String schedName) {
    this.schedName = schedName;
  }

  public String getUUID() {
    return new TerracottaClusterInfo().getUniversallyUniqueClientID();
  }

  private T lookupOrCreateJobStore(String jobStoreName, boolean useSynchWrite) {
    String rootName = "quartz-job-store";
    ConcurrentMap<String, ClusteredJobStore> root;

    Callable<ConcurrentMap<String, ClusteredJobStore>> create = new Callable<ConcurrentMap<String, ClusteredJobStore>>() {

      public ConcurrentMap<String, ClusteredJobStore> call() throws Exception {
        return new ConcurrentDistributedMap<String, ClusteredJobStore>(LockType.WRITE, new HashcodeLockStrategy(), 8);
      }
    };

    root = Terracotta.lookupOrCreateRoot(rootName, create);

    ClusteredJobStore jobStore = root.get(jobStoreName);
    if (jobStore == null) {
      jobStore = createNewJobStoreInstance(useSynchWrite);
      ClusteredJobStore prev = root.putIfAbsent(jobStoreName, jobStore);
      if (prev != null) {
        jobStore = prev;
      }
    }

    return (T) jobStore;
  }

  protected T createNewJobStoreInstance(final boolean useSynchWrite) {
    return (T) new DefaultClusteredJobStore(useSynchWrite);
  }

  private void scheduleUpdateCheck() {
    if (!Boolean.getBoolean("org.terracotta.quartz.skipUpdateCheck")) {
      updateCheckTimer = new Timer("Update Checker", true);
      updateCheckTimer.scheduleAtFixedRate(new UpdateChecker(), 100, WEEKLY);
    }
  }

  public long getEstimatedTimeToReleaseAndAcquireTrigger() {
    return clusteredJobStore.getEstimatedTimeToReleaseAndAcquireTrigger();
  }

  public boolean isClustered() {
    return true;
  }

  protected T getClusteredJobStore() {
    return clusteredJobStore;
  }

  public String getName() {
    return this.getClass().getName();
  }

  public void jobToBeExecuted(final JobExecutionContext context) {
    //
  }

  public void jobExecutionVetoed(final JobExecutionContext context) {
    //
  }

  public void jobWasExecuted(final JobExecutionContext context, final JobExecutionException jobException) {
    //
  }
}
