/* 
 * Copyright 2005 - 2009 Terracotta, Inc. 
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

package org.quartz.examples.example10;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.quartz.Scheduler;
import org.quartz.SchedulerFactory;
import org.quartz.SchedulerMetaData;
import org.quartz.impl.StdSchedulerFactory;

/**
 * This example will spawn a large number of jobs to run
 * 
 * @author James House, Bill Kratzer
 */
public class PlugInExample {

    public void run() throws Exception {
        Logger log = LoggerFactory.getLogger(PlugInExample.class);

        // First we must get a reference to a scheduler
        SchedulerFactory sf = new StdSchedulerFactory();
        Scheduler sched = sf.getScheduler();

        log.info("------- Initialization Complete -----------");

        log.info("------- (Not Scheduling any Jobs - relying on XML definitions --");

        log.info("------- Starting Scheduler ----------------");

        // start the schedule 
        sched.start();

        log.info("------- Started Scheduler -----------------");

        log.info("------- Waiting five minutes... -----------");

        // wait five minutes to give our jobs a chance to run
        try {
            Thread.sleep(300L * 1000L); 
        } catch (Exception e) {
        }

        // shut down the scheduler
        log.info("------- Shutting Down ---------------------");
        sched.shutdown(true);
        log.info("------- Shutdown Complete -----------------");

        SchedulerMetaData metaData = sched.getMetaData();
        log.info("Executed " + metaData.getNumberOfJobsExecuted() + " jobs.");
    }

    public static void main(String[] args) throws Exception {

        PlugInExample example = new PlugInExample();
        example.run();
    }

}
