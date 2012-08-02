package org.quartz.integrations.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.derby.drda.NetworkServerControl;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerFactory;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QTZ179_TriggerLostAfterDbRestart_Test {

    private static final long DURATION_OF_FIRST_SCHEDULING = 9L;
	private static final long DURATION_OF_NETWORK_FAILURE = 10L;
    private static final long DURATION_OF_SECOND_SCHEDULING = 10L;
	private static final  Logger LOG = LoggerFactory.getLogger(QTZ179_TriggerLostAfterDbRestart_Test.class);
    private static final int INTERVAL_IN_SECONDS = 3;
	private static NetworkServerControl derbyServer;
	private static Scheduler sched;
	private static Trigger trigger1_1;
	private static Trigger trigger2_1;
	private static Trigger trigger1_2;
	private static Trigger trigger2_2;

	@BeforeClass
	public static void initialize() throws Exception {
		LOG.info("------- Starting Database ---------------------");
		derbyServer = new NetworkServerControl();
		derbyServer.start(new PrintWriter(System.out));
		int tries = 0;
		while (tries < 5) {
			try {
				Thread.sleep(500);
				derbyServer.ping();
				break;
			} catch (Exception e) {
				tries++;
			}
		}
		if (tries == 5) {
			throw new Exception("Failed to start Derby!");
		}
		LOG.info("------- Database started ---------------------");
		try {
			LOG.info("------- Creating Database tables ---------------------");
			// JdbcQuartzTestUtilities.destroyDatabase();
			JdbcQuartzDerbyUtilities.createDatabase();
			LOG.info("------- Database tables created ---------------------");
		} catch (SQLException e) {
			e.printStackTrace();
			e.getNextException().printStackTrace();
		}
		
        Properties properties = new Properties();
        
        properties.put("org.quartz.scheduler.instanceName","TestScheduler");
        properties.put("org.quartz.scheduler.instanceId","AUTO");
        properties.put("org.quartz.scheduler.skipUpdateCheck","true");
        properties.put("org.quartz.threadPool.class","org.quartz.simpl.SimpleThreadPool");
        properties.put("org.quartz.threadPool.threadCount","12");
        properties.put("org.quartz.threadPool.threadPriority","5");
        properties.put("org.quartz.jobStore.misfireThreshold","10000");
        properties.put("org.quartz.jobStore.class","org.quartz.impl.jdbcjobstore.JobStoreTX");
        properties.put("org.quartz.jobStore.driverDelegateClass","org.quartz.impl.jdbcjobstore.StdJDBCDelegate");
        properties.put("org.quartz.jobStore.useProperties","true");
        properties.put("org.quartz.jobStore.dataSource","myDS");
        properties.put("org.quartz.jobStore.tablePrefix","QRTZ_");
        properties.put("org.quartz.jobStore.isClustered","false");
        properties.put("org.quartz.dataSource.myDS.driver","org.apache.derby.jdbc.ClientDriver");
        properties.put("org.quartz.dataSource.myDS.URL",JdbcQuartzDerbyUtilities.DATABASE_CONNECTION_PREFIX);
        properties.put("org.quartz.dataSource.myDS.user","quartz");
        properties.put("org.quartz.dataSource.myDS.password","quartz");
        properties.put("org.quartz.dataSource.myDS.maxConnections","5");
        // we must get a reference to a scheduler
        SchedulerFactory sf = new StdSchedulerFactory(properties);
		sched = sf.getScheduler();
		LOG.info("------- Initializing ----------------------");


		LOG.info("------- Initialization Complete -----------");

		LOG.info("------- Scheduling Job  -------------------");

		// define the jobs and tie them to our HelloJob class
		JobDetail job1_1 = newJob(HelloJob.class).withIdentity("job1", "group1").build();
		JobDetail job2_1 = newJob(HelloJob.class).withIdentity("job2", "group1").build();
		JobDetail job1_2 = newJob(HelloJob.class).withIdentity("job1", "group2").build();
		JobDetail job2_2 = newJob(HelloJob.class).withIdentity("job2", "group2").build();

		trigger1_1 = newTrigger()
				.withIdentity("job1", "group1")
				.startNow()
				.withSchedule(
						SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(INTERVAL_IN_SECONDS)
								.repeatForever())
				.build();
		trigger2_1 = newTrigger()
				.withIdentity("job2", "group1")
				.startNow()
				.withSchedule(
						SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(INTERVAL_IN_SECONDS)
								.repeatForever())
				.build();
		trigger1_2 = newTrigger()
				.withIdentity("job1", "group2")
				.startNow()
				.withSchedule(
						SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(INTERVAL_IN_SECONDS)
								.repeatForever())
				.build();
		trigger2_2 = newTrigger()
				.withIdentity("job2", "group2")
				.startNow()
				.withSchedule(
						SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(INTERVAL_IN_SECONDS)
								.repeatForever())
				.build();

		Trigger[] triggers = new Trigger[] { trigger1_1, trigger1_2, trigger2_1, trigger2_2 };
		JobDetail[] jobDetails = new JobDetail[] { job1_1, job1_2, job2_1, job2_2 };
		for (int i = 0; i < triggers.length; i++) {
			JobDetail job = jobDetails[i];
			Trigger trigger = triggers[i];
			if (sched.checkExists(job.getKey())) {
				// the job already exists in jdbcjobstore; let's reschedule it
				sched.rescheduleJob(trigger.getKey(), trigger);
			} else {
				sched.scheduleJob(job, trigger);
			}
		}

		// Start up the scheduler (nothing can actually run until the
		// scheduler has been started)
		sched.start();

		LOG.info("------- Scheduler Started -----------------");

		// wait long enough so that the scheduler as an opportunity to
		// run the job!
		try {
			Thread.sleep(DURATION_OF_FIRST_SCHEDULING * 1000L);
		} catch (Exception e) {
		}

		//there should be maximum 1 trigger in acquired state
		if(JdbcQuartzDerbyUtilities.triggersInAcquiredState()>1){
			fail("There should not be more than 1 trigger in ACQUIRED state in the DB.");
		}
		
		// Shutting down and starting up again the database to simulate a
		// network error
		try {
			LOG.info("------- Shutting down database ! -----------------");
			derbyServer.shutdown();
			Thread.sleep(DURATION_OF_NETWORK_FAILURE * 1000L);
			derbyServer.start(null);
			LOG.info("------- Database back online ! -----------------");
			Thread.sleep(DURATION_OF_SECOND_SCHEDULING * 1000L);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
	@Test
	public void checkAll4TriggersStillRunningTest() throws SQLException {
		int triggersInAcquiredState = JdbcQuartzDerbyUtilities.triggersInAcquiredState();
		assertFalse("There should not be more than 1 trigger in ACQUIRED state in the DB, but found "+triggersInAcquiredState,triggersInAcquiredState > 1);
	}
	

	@AfterClass
	public static void shutdownDb() throws Exception {

		// shut down the scheduler
		LOG.info("------- Shutting Down Scheduler---------------------");
		sched.shutdown(true);
		LOG.info("------- Shutdown Complete -----------------");
		try {
			LOG.info("------- Destroying Database ---------------------");
			JdbcQuartzDerbyUtilities.destroyDatabase();
			LOG.info("------- Database destroyed ---------------------");
		} catch (SQLException e) {
			e.printStackTrace();
			e.getNextException().printStackTrace();
			throw new AssertionError(e);
		}

		derbyServer.shutdown();
		LOG.info("------- Database shutdown ---------------------");

	}
}
