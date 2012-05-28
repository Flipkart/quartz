package org.quartz.core;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.SchedulerListener;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.listeners.BroadcastSchedulerListener;
import org.quartz.listeners.SchedulerListenerSupport;

/**
 * Test that verifies that schedulerStarting() is called before the schedulerStarted()
 *  
 * 
 * @author adahanne
 *
 */
public class QTZ212_SchedulerListener_Test {

	private static final String SCHEDULER_STARTED = "SCHEDULER_STARTED";
	private static final String SCHEDULER_STARTING = "SCHEDULER_STARTING";
	private static List<String> methodsCalledInSchedulerListener =  new ArrayList<String>();
	
	
	@Test
	public void stdSchedulerCallsStartingBeforeStartedTest() throws SchedulerException {
		SchedulerFactory sf = new StdSchedulerFactory();
		Scheduler sched = sf.getScheduler();
		sched.getListenerManager().addSchedulerListener(new TestSchedulerListener());
		sched.start();
		
		Assert.assertEquals(SCHEDULER_STARTING,methodsCalledInSchedulerListener.get(0));
		Assert.assertEquals(SCHEDULER_STARTED,methodsCalledInSchedulerListener.get(1));
		
		sched.shutdown();
	}
	
	
	@Test
	public void broadcastSchedulerListenerCallsSchedulerStartingOnAllItsListeners() throws SchedulerException {
		
		methodsCalledInSchedulerListener =  new ArrayList<String>();
		SchedulerFactory sf = new StdSchedulerFactory();
		Scheduler sched = sf.getScheduler();
		List<SchedulerListener> listeners=  new ArrayList<SchedulerListener>();
		listeners.add(new TestSchedulerListener());
		
		sched.getListenerManager().addSchedulerListener(new BroadcastSchedulerListener(listeners));
		sched.start();
		
		Assert.assertEquals(SCHEDULER_STARTING,methodsCalledInSchedulerListener.get(0));
		Assert.assertEquals(SCHEDULER_STARTED,methodsCalledInSchedulerListener.get(1));
		
		sched.shutdown();
	}

	public static class TestSchedulerListener extends SchedulerListenerSupport {

		@Override
		public void schedulerStarted() {
			methodsCalledInSchedulerListener.add(SCHEDULER_STARTED);
			System.out.println("schedulerStarted was called");
		}

		@Override
		public void schedulerStarting() {
			methodsCalledInSchedulerListener.add(SCHEDULER_STARTING);
			System.out.println("schedulerStarting was called");
		}

	}

}
