/*
 * Copyright 2013 Terracotta, Inc..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.quartz.integrations.tests;

import java.util.List;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

/**
 *
 * @author cdennis
 */
public class TrackingJob implements Job {
    public static String SCHEDULED_TIMES_KEY = "TrackingJob.ScheduledTimes";
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            Scheduler scheduler = context.getScheduler();
            List<Long> scheduledFires = (List<Long>)scheduler.getContext().get(SCHEDULED_TIMES_KEY);
            scheduledFires.add(context.getScheduledFireTime().getTime());
        } catch (SchedulerException e) {
            throw new JobExecutionException(e);
        }
    }
}
