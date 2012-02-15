/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.quartz.tests.container;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.Log4jLoggerFactory;
import org.terracotta.express.ClientFactory;
import org.terracotta.quartz.TerracottaJobStore;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;
import com.tc.test.server.appserver.deployment.AbstractOneServerDeploymentTest;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tc.test.server.appserver.deployment.TempDirectoryUtil;
import com.tc.test.server.appserver.deployment.WebApplicationServer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import junit.framework.Test;

public class BasicContainerTest extends AbstractOneServerDeploymentTest {

  private static final String CONTEXT = "BasicContainerTest";

  public static Test suite() {
    return new BasicContainerTestSetup();
  }

  public void testBasics() throws Exception {
    System.out.println("Running test");
    WebConversation conversation = new WebConversation();

    // do insert on server0
    WebResponse response1 = request(server0, "", conversation);
    assertEquals("OK", response1.getText().trim());
  }

  private WebResponse request(WebApplicationServer server, String params, WebConversation con) throws Exception {
    return server.ping("/" + CONTEXT + "/BasicTestServlet?" + params, con);
  }

  private static class BasicContainerTestSetup extends OneServerTestSetup {

    public BasicContainerTestSetup() {
      super(BasicContainerTest.class, CONTEXT);
    }

    @Override
    protected void configureWar(DeploymentBuilder builder) {
      builder.addDirectoryOrJARContainingClass(ClientFactory.class); // toolkit-runtime
      builder.addDirectoryOrJARContainingClass(StdSchedulerFactory.class); // core quartz
      builder.addDirectoryOrJARContainingClass(TerracottaJobStore.class); // quartz-terracotta
      builder.addDirectoryOrJARContainingClass(LoggerFactory.class); // sl4j-api
      builder.addDirectoryOrJARContainingClass(Log4jLoggerFactory.class); // sl4j-log4j12
      builder.addDirectoryOrJARContainingClass(Logger.class); // log4j

      builder.addFileAsResource(createConfigFile(), "WEB-INF/classes/");

      builder.addServlet("BasicTestServlet", "/BasicTestServlet/*", BasicTestServlet.class, null, false);
    }

    private File createConfigFile() {
      try {
        File configFile = writeConfigFile(TempDirectoryUtil.getTempDirectory(this.getClass()), getServerManager()
            .getServerTcConfig().getDsoPort());
        System.out.println("Wrote temp config file at: " + configFile.getAbsolutePath());
        return configFile;
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    private File writeConfigFile(File tempDirectory, int dsoPort) throws IOException {
      InputStream in = null;
      FileOutputStream out = null;

      try {
        in = getClass().getClassLoader().getResourceAsStream("org/terracotta/quartz/tests/container/quartz.properties");
        File rv = new File(tempDirectory, "basic-quartz.properties");
        out = new FileOutputStream(rv);
        String template = IOUtils.toString(in);
        String config = template.replace("__PORT__", String.valueOf(dsoPort));
        out.write(config.getBytes());
        return rv;
      } finally {
        IOUtils.closeQuietly(in);
        IOUtils.closeQuietly(out);
      }
    }
  }

}
