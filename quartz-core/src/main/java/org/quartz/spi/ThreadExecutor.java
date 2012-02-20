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
  package org.quartz.spi;

/**
 * Allows different strategies for scheduling threads. The {@link #initialize()}
 * method is required to be called before the first call to
 * {@link #execute(Thread)}. The Thread containing the work to be performed is
 * passed to execute and the work is scheduled by the underlying implementation.
 *
 * @author matt.accola
 * @version $Revision$ $Date$
 */
public interface ThreadExecutor {

    /**
     * Submit a task for execution
     *
     * @param thread the thread to execute
     */
    void execute(Thread thread);

    /**
     * Initialize any state prior to calling {@link #execute(Thread)}
     */
    void initialize();
}
