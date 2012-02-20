/* 
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved. 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not 
 * use this file except in compliance with the License. You may obtain a copy 
 * of the License at 
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0 
 *   
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT 
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the 
 * License for the specific language governing permissions and limitations 
 * under the License.
 * 
 */
  package org.quartz;

import static org.quartz.DailyTimeIntervalScheduleBuilder.dailyTimeIntervalSchedule;
import static org.quartz.DateBuilder.dateOf;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TimeOfDay.hourMinuteAndSecondOfDay;
import static org.quartz.TriggerBuilder.newTrigger;

import org.quartz.DateBuilder.IntervalUnit;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.spi.OperableTrigger;

import java.util.Date;
import java.util.List;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Unit test for DailyTimeIntervalScheduleBuilder.
 * 
 * @author Zemian Deng <saltnlight5@gmail.com>
 */
public class DailyTimeIntervalScheduleBuilderTest extends TestCase {

  public void testScheduleActualTrigger() throws Exception {
    Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
    JobDetail job = newJob(MyJob.class).build();
    DailyTimeIntervalTrigger trigger = newTrigger().withIdentity("test")
        .withSchedule(dailyTimeIntervalSchedule().withIntervalInSeconds(3)).build();
    scheduler.scheduleJob(job, trigger); // We are not verify anything other than just run through the scheduler.
    scheduler.shutdown();
  }

  public void testHourlyTrigger() {
    DailyTimeIntervalTrigger trigger = newTrigger().withIdentity("test")
        .withSchedule(dailyTimeIntervalSchedule().withIntervalInHours(1)).build();
    Assert.assertEquals("test", trigger.getKey().getName());
    Assert.assertEquals("DEFAULT", trigger.getKey().getGroup());
    Assert.assertEquals(IntervalUnit.HOUR, trigger.getRepeatIntervalUnit());
    Assert.assertEquals(1, trigger.getRepeatInterval());
    List<Date> fireTimes = TriggerUtils.computeFireTimes((OperableTrigger) trigger, null, 48);
    Assert.assertEquals(48, fireTimes.size());
  }

  public void testMinutelyTriggerWithTimeOfDay() {
    DailyTimeIntervalTrigger trigger = newTrigger()
        .withIdentity("test", "group")
        .withSchedule(dailyTimeIntervalSchedule().withIntervalInMinutes(72)
                          .startingDailyAt(TimeOfDay.hourAndMinuteOfDay(8, 0))
                          .endingDailyAt(TimeOfDay.hourAndMinuteOfDay(17, 0)).onMondayThroughFriday()).build();
    Assert.assertEquals("test", trigger.getKey().getName());
    Assert.assertEquals("group", trigger.getKey().getGroup());
    Assert.assertEquals(true, new Date().getTime() >= trigger.getStartTime().getTime());
    Assert.assertEquals(true, null == trigger.getEndTime());
    Assert.assertEquals(IntervalUnit.MINUTE, trigger.getRepeatIntervalUnit());
    Assert.assertEquals(72, trigger.getRepeatInterval());
    Assert.assertEquals(new TimeOfDay(8, 0), trigger.getStartTimeOfDay());
    Assert.assertEquals(new TimeOfDay(17, 0), trigger.getEndTimeOfDay());
    List<Date> fireTimes = TriggerUtils.computeFireTimes((OperableTrigger) trigger, null, 48);
    Assert.assertEquals(48, fireTimes.size());
  }

  public void testSecondlyTriggerWithStartAndEndTime() {
    Date startTime = DateBuilder.dateOf(0, 0, 0, 1, 1, 2011);
    Date endTime = DateBuilder.dateOf(0, 0, 0, 2, 1, 2011);
    DailyTimeIntervalTrigger trigger = newTrigger()
        .withIdentity("test", "test")
        .withSchedule(dailyTimeIntervalSchedule().withIntervalInSeconds(121)
                          .startingDailyAt(hourMinuteAndSecondOfDay(10, 0, 0))
                          .endingDailyAt(hourMinuteAndSecondOfDay(23, 59, 59)).onSaturdayAndSunday())
        .startAt(startTime).endAt(endTime).build();
    Assert.assertEquals("test", trigger.getKey().getName());
    Assert.assertEquals("test", trigger.getKey().getGroup());
    Assert.assertEquals(true, startTime.getTime() == trigger.getStartTime().getTime());
    Assert.assertEquals(true, endTime.getTime() == trigger.getEndTime().getTime());
    Assert.assertEquals(IntervalUnit.SECOND, trigger.getRepeatIntervalUnit());
    Assert.assertEquals(121, trigger.getRepeatInterval());
    Assert.assertEquals(new TimeOfDay(10, 0, 0), trigger.getStartTimeOfDay());
    Assert.assertEquals(new TimeOfDay(23, 59, 59), trigger.getEndTimeOfDay());
    List<Date> fireTimes = TriggerUtils.computeFireTimes((OperableTrigger) trigger, null, 48);
    Assert.assertEquals(48, fireTimes.size());
  }

  public void testRepeatCountTrigger() {
    DailyTimeIntervalTrigger trigger = newTrigger().withIdentity("test")
        .withSchedule(dailyTimeIntervalSchedule().withIntervalInHours(1).withRepeatCount(9)).build();
    Assert.assertEquals("test", trigger.getKey().getName());
    Assert.assertEquals("DEFAULT", trigger.getKey().getGroup());
    Assert.assertEquals(IntervalUnit.HOUR, trigger.getRepeatIntervalUnit());
    Assert.assertEquals(1, trigger.getRepeatInterval());
    List<Date> fireTimes = TriggerUtils.computeFireTimes((OperableTrigger) trigger, null, 48);
    Assert.assertEquals(10, fireTimes.size());
  }

  public void testEndingAtAfterCount() {
    Date startTime = DateBuilder.dateOf(0, 0, 0, 1, 1, 2011);
    DailyTimeIntervalTrigger trigger = newTrigger()
        .withIdentity("test")
        .withSchedule(dailyTimeIntervalSchedule().withIntervalInMinutes(15)
                          .startingDailyAt(TimeOfDay.hourAndMinuteOfDay(8, 0)).endingDailyAfterCount(12))
        .startAt(startTime).build();
    Assert.assertEquals("test", trigger.getKey().getName());
    Assert.assertEquals("DEFAULT", trigger.getKey().getGroup());
    Assert.assertEquals(IntervalUnit.MINUTE, trigger.getRepeatIntervalUnit());
    List<Date> fireTimes = TriggerUtils.computeFireTimes((OperableTrigger) trigger, null, 48);
    Assert.assertEquals(48, fireTimes.size());
    Assert.assertEquals(dateOf(8, 0, 0, 1, 1, 2011), fireTimes.get(0));
    Assert.assertEquals(dateOf(10, 45, 0, 4, 1, 2011), fireTimes.get(47));
    Assert.assertEquals(new TimeOfDay(10, 45), trigger.getEndTimeOfDay());
  }

  public void testEndingAtAfterCountOf1() {
    Date startTime = DateBuilder.dateOf(0, 0, 0, 1, 1, 2011);
    DailyTimeIntervalTrigger trigger = newTrigger()
        .withIdentity("test")
        .withSchedule(dailyTimeIntervalSchedule().withIntervalInMinutes(15)
                          .startingDailyAt(TimeOfDay.hourAndMinuteOfDay(8, 0)).endingDailyAfterCount(1))
        .startAt(startTime).build();
    Assert.assertEquals("test", trigger.getKey().getName());
    Assert.assertEquals("DEFAULT", trigger.getKey().getGroup());
    Assert.assertEquals(IntervalUnit.MINUTE, trigger.getRepeatIntervalUnit());
    List<Date> fireTimes = TriggerUtils.computeFireTimes((OperableTrigger) trigger, null, 48);
    Assert.assertEquals(48, fireTimes.size());
    Assert.assertEquals(dateOf(8, 0, 0, 1, 1, 2011), fireTimes.get(0));
    Assert.assertEquals(dateOf(8, 0, 0, 17, 2, 2011), fireTimes.get(47));
    Assert.assertEquals(new TimeOfDay(8, 0), trigger.getEndTimeOfDay());
  }

  public void testEndingAtAfterCountOf0() {
    try {
      Date startTime = DateBuilder.dateOf(0, 0, 0, 1, 1, 2011);
      newTrigger()
          .withIdentity("test")
          .withSchedule(dailyTimeIntervalSchedule().withIntervalInMinutes(15)
                            .startingDailyAt(TimeOfDay.hourAndMinuteOfDay(8, 0)).endingDailyAfterCount(0))
          .startAt(startTime).build();
      fail("We should not accept endingDailyAfterCount(0)");
    } catch (IllegalArgumentException e) {
      // Expected.
    }

    try {
      Date startTime = DateBuilder.dateOf(0, 0, 0, 1, 1, 2011);
      newTrigger().withIdentity("test")
          .withSchedule(dailyTimeIntervalSchedule().withIntervalInMinutes(15).endingDailyAfterCount(1))
          .startAt(startTime).build();
      fail("We should not accept endingDailyAfterCount(x) without first setting startingDailyAt.");
    } catch (IllegalArgumentException e) {
      // Expected.
    }
  }

  /** An empty job for testing purpose. */
  public static class MyJob implements Job {
    public void execute(JobExecutionContext context) throws JobExecutionException {
      //
    }
  }
}
