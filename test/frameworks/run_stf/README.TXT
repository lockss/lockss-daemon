             Running The LOCKSS Stochastic Testing Framework
             (or, if you prefer, "Seth's Testing Framework")

------------------------------------------------------------------------
1. Overview

  This directory provides a framework for running functional tests on
  the LOCKSS system.  The tests are invoked using the 'testsuite.py'
  script and passing it the name of a test suite to run.  The tests
  can be run without any customization, but some settings can be
  tweaked through the configuration file described below.

  For each test being run, a new directory is created under the
  working directory to hold the daemon config files, log files, and
  caches.  The structure is basically:

    <workingDir>/testcase-<n>/daemon-<port>/...
                             /daemon-<port + 1>/...
			     /daemon-<port + 2>/...
                             [...]
   
   Where <n> is the number of the test being run.  If, for example,
   you run a testsuite with five tests, this will create testcase-1
   through testcase-5, one for each test.

   Each testcase directory contains a pair of auto-generated config
   files, 'lockss.txt' and 'lockss.opt' (see the next section for
   details about how to override any daemon config param using
   lockss.opt) It also contains daemon directories, one for each
   daemon being started. Each daemon directory has the following
   structure:

       daemon-<port>/test.out
                    /dpid
                    /local.txt

   The daemon log file is 'test.out', and the PID of the daemon
   is 'dpid'.  'local.txt' is an auto-generated config file.

   After each test is run, the daemons are stopped and the log file is
   examined for thread deadlocks.  If no errors have occured while
   running the suite, all the daemon directories are deleted (this can
   be overridden with the deleteAfterSuccess parameter -- see below)
   

------------------------------------------------------------------------
2. Configuration

  Typically, no configuration is necessary, the default values are
  safe.  However, certain parameters can be overridden using the
  config file "testsuite.props", located in this directory.

  Parameter            Default     Meaning
  ---------            -------     ----------------------------------
  daemonCount          4           The number of daemons to start.
  
  daemonLogLevel       debug       The logging level at which to run
                                   the LOCKSS daemons.

  scriptLogLevel       info        The logging level at which to run
                                   the test script -- this really
                                   does not need to be higher than info.

  projectDir           (none)      The path to the top level of a
                                   lockss-daemon project.  Normally the
                                   script will simply look up the path
                                   until it finds build.xml, and then
                                   assume that is the projectDir.

  workDir              './'        The top-level directory in which
                                   to build daemon directories and run
                                   the tests.

  startUiPort          8041        The start of the range of ports to
                                   use for the daemon UI.  Ports
                                   numbered 'startUiPort' to
                                   'startUiPort + (numDaemons - 1)' will
                                   be used.

  startV3Port          8081        The start of the range of ports to
                                   use for V3 LCAP.  Ports
                                   numbered 'startV3Port' to
                                   'startV3Port + (numDaemons - 1)' will
                                   be used.

  timeout              28000       The maximum time to wait for an
                                   event before timing out.  Default
                                   is eight hours, which is usually
                                   enough for even a large test.

  deleteAfterSuccess   False       Set to 'True' if you want the scripts
                                   to clean up their working directory
                                   after a successful run in which no
                                   errors occured.  (In all cases, if
			           errors occur during the test, the
			           daemon directories will be left intact
				   so the logs can be reviewed)

  delayShutdown        False       If set to 'true', when the testcase
                                   ends (whether with success or failure)
				   you will be prompted to continue, and
                                   the daemons will be left running until
                                   the 'Enter' key is pressed.  This
				   should only be set to 'True' if you will
                                   be interactively monitoring the test.
                        

  hostname            'localhost'  The hostname to use when talking to
		                   the servlet UI.

  username            'lockss-u'   The username to use when talking to
                                   the servlet UI.

  password            'lockss-u'   The password to use when talking to
                                   the servlet UI.


  You can also add arbitrary LOCKSS daemon config parameters to
  this file.  Any parameter name starting with the string 'org.lockss.'
  will be written into the per-testcase 'lockss.opt' config file
  for you.  This config file is loaded last by the daemon, so it
  will override any other values previously set.

------------------------------------------------------------------------
3. Invocation

  The tests can be invoked with the command:

  % python testsuite.py <testsuite>

  Where <testsuite> is one of the following:

      all           - Run all of the included tests.

      tinyUiTests   - Run the tiny UI tests.

      simpleV3Tests - Run only the simple damage, simple delete, and
                      simple extra file tests.

      randomV3Tests - Run the randomized stress tests.

      v3Tests       - Run both simpleV3Tests and randomV3Tests


  'simpleV3Tests' currently runs three tests on very small simulated
  AUs.  They damage, delete, or create a node on one of the daemons,
  respectively, and then wait for repair.

  'randomV3Tests' is a set of three tests that creates a randomly sized
  au of depth between 0 and 2, branch factor between 0 and 2, and
  number of files per branch of between 3 and 20.  The first test
  damages a random number of files (between 1 and 5).  The second
  test deletes a random number of files.  The third test creates
  a random number of extra nodes.


------------------------------------------------------------------------
4. Adding test cases

  For a quick introduction to Pyunit, see:
     <http://pyunit.sourceforge.net/pyunit.html>

  The general method for adding tests to 'testsuite.py' is as
  follows:

  a. Create the test case class.  It should have a unique name, extend
     LockssTestCase, and override the 'runTest' method.  The method
     doc for the runTest method should be a one-line summary of what
     the test does -- this will be printed to the script log when the
     test is run.  For example:

       class MyNiftyTestCase(LockssTestCase):
           def runTest(self):
               " One-line test description "
               ...

  b. At the end of the file, create a function that returns a 
     unittest.TestSuite() containing your test case(s).  The name
     of the test suite is invoked as a command-line argument to
     the script, so it should be somewhat descriptive.  For example,

       def niftyTests():
           suite = unittest.TestSuite()
           suite.addTest(MyNiftyTestCase())
           suite.addTest(MyOtherNiftyTestCase())
           return suite

  c. You can now run this test suite with the command:

     % python testsuite.py niftyTests


------------------------------------------------------------------------
5. Cleanup

  If the tests fail, or if deleteAfterSuccess is set to 'False',
  the daemon directories will not be deleted.  A very simple cleanup
  script is included.  Just invoke it with:

     % ./clean.sh
