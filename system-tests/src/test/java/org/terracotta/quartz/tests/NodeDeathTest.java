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
 package org.terracotta.quartz.tests;

import com.tc.test.config.model.TestConfig;
import com.tc.util.PortChooser;

/**
 * This test ensures that a live node can recover acquired triggers (as opposed to doing recovery just at startup of new
 * nodes)
 */
public class NodeDeathTest extends AbstractStandaloneTest {

  public NodeDeathTest(TestConfig testConfig) {
    super(testConfig, NodeDeathTestClient1.class, NodeDeathTestClient2.class);
    testConfig.getClientConfig().addExtraClientJvmArg("-DlistenPort=" + new PortChooser().chooseRandomPort());
  }
}
