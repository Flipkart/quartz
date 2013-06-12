/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.quartz.osgi.tests;

import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;
import org.quartz.Scheduler;
import org.quartz.SchedulerFactory;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.terracotta.test.OsgiUtil;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

/**
 * @author hhuynh
 * 
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class SimpleOsgiQuartzTest implements JobListener {
	public static final CyclicBarrier barrier = new CyclicBarrier(2);

	@Configuration
	public Option[] config() {
		return options(mavenBundle("org.quartz-scheduler", "quartz")
				.versionAsInProject(), wrappedBundle(maven("c3p0", "c3p0")
				.versionAsInProject()), OsgiUtil.commonOptions());
	}

	// note this part of code run in osgi container
	@Test
	public void testQuartz() throws Exception {
		SchedulerFactory sf = new StdSchedulerFactory();
		Scheduler sched = sf.getScheduler();
		sched.getListenerManager().addJobListener(this);
		JobDetail job = JobBuilder.newJob(HelloJob.class)
				.withIdentity("job1", "group1").build();
		org.quartz.Trigger trigger = TriggerBuilder.newTrigger()
				.withIdentity("trigger1", "group1").startNow().build();
		sched.scheduleJob(job, trigger);
		sched.start();
		await();
		sched.shutdown(true);
	}

	private static void await() {
		try {
			barrier.await(10, TimeUnit.SECONDS);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String getName() {
		return "SimpleOsgiQuartzTest";
	}

	@Override
	public void jobToBeExecuted(JobExecutionContext context) {
		//
	}

	@Override
	public void jobExecutionVetoed(JobExecutionContext context) {
		//
	}

	@Override
	public void jobWasExecuted(JobExecutionContext context,
			JobExecutionException jobException) {
		if (context.getJobInstance() instanceof HelloJob) {
			HelloJob job = (HelloJob) context.getJobInstance();
			System.out.println("XXX: count = " + job.getCount());
			await();
		}
	}
}