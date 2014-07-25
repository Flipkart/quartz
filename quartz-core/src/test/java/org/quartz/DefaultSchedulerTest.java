package org.quartz;

import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

import java.util.Properties;

import org.quartz.impl.JobDetailImpl;
import org.quartz.impl.StdSchedulerFactory;

import junit.framework.TestCase;

/**
 * DefaultSchedulerTest
 */
public class DefaultSchedulerTest extends TestCase {

    public void testAddJobNoTrigger() throws Exception {
        StdSchedulerFactory factory = new StdSchedulerFactory("org/quartz/properties/quartzSkipUpdateCheck.properties");
        Scheduler scheduler = factory.getScheduler();
        JobDetailImpl jobDetail = new JobDetailImpl();
        jobDetail.setName("testjob");

        try {
            scheduler.addJob(jobDetail, false);
        } catch (SchedulerException e) {
            assertThat(e.getMessage(), containsString("durable"));
        }

        jobDetail.setDurability(true);
        scheduler.addJob(jobDetail, false);
    }
}
