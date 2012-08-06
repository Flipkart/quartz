
==============================================================================
This file is intended to help you get started with the Quartz project.

For more information see http://www.quartz-scheduler.org
==============================================================================


What is Quartz?
==============================================================================

Quartz is an open source project aimed at creating a free-for-use Job 
Scheduler, with enterprise features.

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy
of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Additionally, a copy of the license and its accompanying notice file is
included with the distribution.


What is in this package?
==============================================================================

quartz-all-<ver>.jar        all-in-one Quartz library.  Includes the core
                            Quartz components plus all optional packages.  If
                            you use this library, no other quartz-*.jars are
                            necessary.

quartz-<ver>.jar            core Quartz library.

quartz-jboss-<ver>.jar      optional JBoss specific Quartz extensions such as
                            the Quartz startup MBean, QuartzService.

quartz-oracle-<ver>.jar     optional Oracle specific Quartz extensions such as
                            the OracleDelegate.

quartz-terracotta-<ver>.jar optional Terracotta specific Quartz extension to 
                            enable the Terracotta Job Store for Terracotta 
                            based clustering (Located in the Terracotta Kit 
                            http://www.terracotta.org/dl/oss-download-catalog)

quartz-weblogic-<ver>.jar   optional WebLogic specific Quartz extensions such
                            as the WebLogicDelegate.

README.txt                  this file.

LICENSE.txt                 a document declaring the license under which
                            Quartz can be used and distributed.

docs/dbTables               sql scripts for creating Quartz database tables in
                            a variety of different databases.

quartz                      source code for the main quartz module, including
                            the following packages:

    org.quartz              the main package of the Quartz project,
                            containing the 'public' (client-side) API for
                            the scheduler

    org.quartz.core         a package containing the 'private' (server-side)
                            components of Quartz.

    org.quartz.simpl        a package contain simple implementations of
                            Quartz support modules (JobStores, ThreadPools,
                            Loggers, etc.) that have no dependencies on
                            external (third-party) products.

    org.quartz.impl         a package containing implementations of Quartz
                            support modules (JobStores, ThreadPools,
                            Loggers, etc.) that may have dependencies on
                            external (third-party) products - but may be
                            more robust.

    org.quartz.utils        a package containing some utility/helper
                            components used through-out the main Quartz
                            components.

quartz-jboss                source code for the quartz-jboss module

quartz-oracle               source code for the quartz-oracle module

quartz-weblogic             source code for the quartz-weblogic module

quartz-all                  module to compile the quartz-all jar

examples                    a directory containing some code samples on the
                            usage of Quartz.  The first example you should
                            look at is 'example1.bat' or 'example1.sh' -
                            depending if you're a win-dos or unix person.
                            This example uses the code found in the
                            SchedTest.java class, which is also in the
                            examples directory.

lib                         a directory which should contain all of the
                            third-party libraries that are needed in order
                            to use all of the features of Quartz. (Some are
                            not automatically there, but you need to get
                            them and put them there if you use the features 
                            they depend on -- see below).



Where should I start if I am new to Quartz?
==============================================================================

There is an FAQ, tutorial and configuration reference that can be found on the 
main Quartz website at http://quartz-scheduler.org/docs/index.html

Most of the Java source files are fairly well documented with JavaDOC -
consider this your "reference manual".  

Start by looking at org.quartz.Scheduler, org.quartz.Job,
org.quartz.JobDetail and org.quartz.Trigger.

Examine and run the examples found in the "examples" directory.

If you're interested in the "behind the scenes" (server-side) code,
you'll want to look at org.quartz.core.QuartzSchedulerThread, which
will make you interested in org.quartz.spi.JobStore.java,
org.quartz.spi.ThreadPool.java and org.quartz.core.JobRunShell.


What should I do if I encounter a problem?
==============================================================================

Help is available via the Quartz Users forum:

  http://forums.terracotta.org/forums/forums/show/17.page

Please report bugs / issues to JIRA at:

  https://jira.terracotta.org/jira/browse/QTZ


How do I build Quartz?
==============================================================================

Quartz is built using the Maven project management tool.  If you don't 
already have Maven installed, download it from the Apache website 
(http://maven.apache.org) and follow the installation instructions.  You can 
confirm the version of Maven you have installed by typing: mvn --version

To build, simply execute "mvn install" from the top level Quartz project 
directory. This command will build the Quartz JAR files and install them 
into your local repository.  Along the way it will also execute all of the 
Quartz unit and integration tests.  You can disable tests by 
passing -Dmaven.test.skip=true on the mvn command-line.

To create the downloadable distribution, invoke the package phase and the
assembly:assembly plugin goal with prepare-distribution profile enabled, 
i.e.:

    mvn -Pprepare-distribution package assembly:assembly

How can I get started with the Terracotta Job Store?
==============================================================================

The Terracotta Job Store provides an easy way to implement a highly 
available, highly scalable, and durable way to schedule jobs across 
multiple nodes. As with other Terracotta solutions, Quartz clustering 
can be achieved via a Terracotta Express installation as well as via a 
Terracotta Custom Installation. To use the Terracotta Job Store in a 
Custom Installation - i.e. in conjunction with other Terracotta DSO uses 
(such as shared objects/shared roots/TIMs/clustered web sessions) please 
consult the online documentation.

For an Express installation, simply include the quartz-<ver> and 
quartz-terracotta-<ver> jars in your application classpath, and then 
configure your app to use the Terracotta Job Store by setting the 
following in your quartz.properties file (or set these properties 
directly within the application)

    org.quartz.jobStore.class = org.terracotta.quartz.TerracottaJobStore
    org.quartz.jobStore.tcConfigUrl = localhost:9510

This assumes that you are running the Terracotta server on the localhost 
(which can be started using the bin/start-tc-server.[sh|bat] script). If 
not, replace localhost as appropriate. The Terracotta Job Store requires 
Terracotta 3.2.0 or greater.

