#!/usr/bin/python

import sys, tempfile, time, unittest, os
from os import path
from lockss_daemon import *

##########################################################################
# Module documentation
##########################################################################
"""
usage: testsuite.py <property-file>

This test suite requires at minimum a top-level work directory to build
frameworks in, a username, a password, and a number of daemons to
create.  A debug level can optionally be set for daemon output (the default
is 'debug').  For example:

  # example properties file
  #
  # Required properties:
  username = <ui username>
  password = <ui password>
  workDir = /path/to/create/work/dir
  daemonCount = 4

  # Optional properties
  # Debug level (default is 'debug')
  debugLevel = debug3

  # projectDir is a top-level lockss-daemon project (if
  # none is provided, walk up the path from current directory
  # trying to find build.xml)
  projectDir = /path/to/lockss-daemon/

  # If true, delete framework directories after tests complete.
  # Default is False, files will not be deleted.
  # (warning: will be deleted whether or not the test succeeded!)
  deleteAfter = false

"""

###########################################################################
## Test cases.  Add test cases here, as well as to the TestSuite object
## created in the main method below.
###########################################################################

##
## Super-class for simple delete/damage repair test cases.
##
class SimpleTestCase(unittest.TestCase):
    """ Parent class for simple damage/delete tests """
    def setUp(self):
        fwdir = getFrameworkDir()
        log("Creating framework in %s" % fwdir)
        self.framework = Framework(frameworkDir=fwdir, username=username,
                                   password=password, daemonCount=daemonCount,
                                   projectDir=projectDir, debugLevel=debugLevel,
                                   startPort=startPort)

        global curFramework
        curFramework = self.framework

        # Get client list from framework
        self.clients = self.framework.clientList

        # very small, for testing simple repairs
        self.simAu = SimulatedAu('localA', 0, 0, 3)

        # Start the daemon.
        log("Starting framework.")
        self.framework.start()
        self.failUnless(self.framework.isRunning, 'Framework failed to start.')

        # Block return until all clients are ready to go.
        log("Waiting for framework to come ready.")
        for client in self.clients:
            client.waitForFrameworkReady()

        ##
        ## Create simulated AUs
        ##
        log("Creating simulated AUs.")
        for client in self.clients:
            client.createAu(self.simAu)

        ##
        ## Assert that the AUs have been crawled.
        ##
        log("Waiting for simulated AUs to crawl.")
        for client in self.clients:
            if not (client.waitForSuccessfulCrawl(self.simAu)):
                self.fail("AUs never completed initial crawl.")
        log("AUs completed initial crawl.")

    def tearDown(self):
        self.framework.stop()
        self.failIf(self.framework.isRunning,
                    'Framework did not stop.')
        if deleteAfter:
            self.framework.clean()


class SimpleDamageTestCase(SimpleTestCase):
    def runTest(self):
        "Test recovery from random file damage."

        ## To select a random client, uncomment this line.
        # client = self.clients[random.randint(0, len(clients) - 1)]

        ## To use a specific client, uncomment this line.
        client = self.clients[0]

        node = client.randomDamage(self.simAu)
        log("Damaged client %s, node %s" % (client, node.url))

        # Request a tree walk (deactivate and reactivate AU)
        client.requestTreeWalk(self.simAu)

        # set timeouts to wait up to one hour, pausing 10 in each loop
        timeout = 60 * 60
        sleep = 10

        # expect to see a top level content poll
        log("Waiting for top level content poll.")
        self.failUnless(client.waitForTopLevelContentPoll(self.simAu,
                                                          timeout=timeout,
                                                          sleep=sleep),
                        "Never called top level content poll")
        log("Called top level content poll.")

        # expect to see the top level node marked repairing.
        log("Waiting for lockssau to be marked 'damaged'.")
        self.failUnless(client.waitForDamage(self.simAu, None,
                                             timeout=timeout,
                                             sleep=sleep),
                        "Client never marked lockssau 'damaged'.")
        log("Marked lockssau 'damaged'.")

        # expect to see top level name poll
        log("Waiting for top level name poll.")
        self.failUnless(client.waitForTopLevelNamePoll(self.simAu,
                                                       timeout=timeout,
                                                       sleep=sleep),
                        "Never called top level name poll")
        log("Called top level name poll")

        # expect to see the specific damaged node marked for repairing.
        log("Waiting for damaged node %s to be marked 'damaged'." % node.url)
        self.failUnless(client.waitForDamage(self.simAu, node, timeout=timeout,
                                             sleep=sleep),
                        "Never marked node %s 'damaged'" % node.url)
        log("Marked node %s 'damaged'" % node.url)

        # TODO: Finer grained tests here?

        # expect to see the AU successfully repaired.
        log("Waiting for successful repair of node %s." % node.url)
        self.failUnless(client.waitForContentRepair(self.simAu, node,
                                                    timeout=timeout,
                                                    sleep=sleep),
                        "Node %s not repaired." % node.url)
        log("Node %s repaired." % node.url)

class SimpleDeleteTestCase(SimpleTestCase):
    def runTest(self):
        "Test recovery from a random file deletion."

        ## To select a random client, uncomment this line.
        # client = self.clients[random.randint(0, len(clients) - 1)]

        ## To use a specific client, uncomment this line.
        client = self.clients[1]

        node = client.randomDelete(self.simAu)
        log("Deleted client %s, node %s" % (client, node.url))

        # Request a tree walk (deactivate and reactivate AU)
        client.requestTreeWalk(self.simAu)

        # set timeouts to wait up to one hour, pausing 10 in each loop
        timeout = 60 * 60
        sleep = 10

        # expect to see a top level content poll
        log("Waiting for top level content poll.")
        self.failUnless(client.waitForTopLevelContentPoll(self.simAu,
                                                          timeout=timeout,
                                                          sleep=sleep),
                        "Never called top level content poll")
        log("Called top level content poll.")

        # expect to see the top level node marked repairing.
        log("Waiting for lockssau to be marked 'damaged'.")
        self.failUnless(client.waitForDamage(self.simAu, None,
                                             timeout=timeout,
                                             sleep=sleep),
                        "Client never marked lockssau 'damaged'.")
        log("Marked lockssau 'damaged'.")

        # expect to see top level name poll
        log("Waiting for top level name poll.")
        self.failUnless(client.waitForTopLevelNamePoll(self.simAu,
                                                       timeout=timeout,
                                                       sleep=sleep),
                        "Never called top level name poll")
        log("Called top level name poll")

        # TODO: finer grained tests here?

        # expect to see the AU successfully repaired.
        log("Waiting for repair.")
        self.failUnless(client.waitForNameRepair(self.simAu,
                                                 timeout=timeout,
                                                 sleep=sleep),
                        "Au not repaired.")
        log("AU repaired.")

##
## Super-class for simple delete/damage repair test cases.
##
class RangedTestCase(unittest.TestCase):
    def setUp(self):
        fwdir = getFrameworkDir()
        log("Creating framework in %s" % fwdir)
        self.framework = Framework(frameworkDir=fwdir, username=username,
                                   password=password, daemonCount=daemonCount,
                                   projectDir=projectDir, debugLevel=debugLevel,
                                   startPort=startPort)
 
        global curFramework
        curFramework = self.framework

        # Get client list from framework
        self.clients = self.framework.clientList

        # Long names, shallow depth, wide range for testing ranged polls
        self.simAu = SimulatedAu('localA', depth=0, branch=0,
                                 numFiles=45, maxFileName=26)

        # Start the daemon.
        log("Starting framework.")
        self.framework.start()
        self.failUnless(self.framework.isRunning, 'Framework failed to start.')

        # Block return until all clients are ready to go.
        log("Waiting for framework to come ready.")
        for client in self.clients:
            client.waitForFrameworkReady()

        ##
        ## Create simulated AUs
        ##
        log("Creating simulated AUs.")
        for client in self.clients:
            client.createAu(self.simAu)

        ##
        ## Assert that the AUs have been crawled.
        ##
        log("Waiting for simulated AUs to crawl.")
        for client in self.clients:
            if not (client.waitForSuccessfulCrawl(self.simAu)):
                self.fail("AUs never successfully completed initial crawl.")
        log("AUs successfully completed initial crawl.")

    def tearDown(self):
        self.framework.stop()
        self.failIf(self.framework.isRunning,
                    'Framework did not successfully stop.')
        if deleteAfter:
            self.framework.clean()


class RangedNamePollDeleteTestCase(RangedTestCase):
    def runTest(self):
        "Test recovery from a file deletion after a ranged name poll"

        ## To select a random client, uncomment this line.
        # client = self.clients[random.randint(0, len(clients) - 1)]

        ## To use a specific client, uncomment this line.
        client = self.clients[1]

        # get a file that will be in the second packet
        filename = '045abcdefghijklmnopqrstuvwxyz.txt'
        node = client.getAuNode(self.simAu,
                                "http://www.example.com/" + filename)
        client.deleteNode(node)
        log("Deleted client %s, node %s" % (client, node.url))

        # Request a tree walk (deactivate and reactivate AU)
        client.requestTreeWalk(self.simAu)

        # set timeouts to wait up to one hour, pausing 10 in each loop
        timeout = 60 * 60
        sleep = 10

        # expect to see a top level content poll
        log("Waiting for top level content poll.")
        self.failUnless(client.waitForTopLevelContentPoll(self.simAu,
                                                          timeout=timeout,
                                                          sleep=sleep),
                        "Never called top level content poll")
        log("Called top level content poll.")

        # expect to see the top level node marked damaged.
        log("Waiting for lockssau to be marked 'damaged'.")
        self.failUnless(client.waitForDamage(self.simAu, None,
                                             timeout=timeout,
                                             sleep=sleep),
                        "Client never marked lockssau 'damaged'.")
        log("Marked lockssau 'damaged'.")

        # expect to see top level name poll
        log("Waiting for top level name poll.")
        self.failUnless(client.waitForTopLevelNamePoll(self.simAu,
                                                       timeout=timeout,
                                                       sleep=sleep),
                        "Never called top level name poll")
        log("Called top level name poll")

        # expect to win a top level name poll (name poll on baseURL
        # should be lost later on in the test)
        log("Waiting to win top level name poll.")
        self.failUnless(client.waitForWonTopLevelNamePoll(self.simAu,
                                                          timeout=timeout,
                                                          sleep=sleep),
                        "Never won top level name poll")
        log("Won top level name poll.")

        # expect to call a name poll on baes URL.
        node = client.getAuNode(self.simAu, self.simAu.baseUrl)
        log("Waiting to call name poll on %s" % node.url)
        self.failUnless(client.waitForNamePoll(self.simAu, node, timeout=timeout,
                                               sleep=sleep),
                        "Never called name poll on %s" % node.url)
        log("Called name poll on %s" % node.url)

        # expect to lose a name poll on baes URL.
        node = client.getAuNode(self.simAu, self.simAu.baseUrl)
        log("Waiting to lose name poll on %s" % node.url)
        self.failUnless(client.waitForLostNamePoll(self.simAu, node,
                                                   timeout=timeout,
                                                   sleep=sleep),
                        "Never lost name poll on %s" % node.url)
        log("Lost name poll on %s" % node.url)

        # expect to call a ranged name poll on baes URL.
        node = client.getAuNode(self.simAu, self.simAu.baseUrl)
        log("Waiting to call ranged name poll on %s" % node.url)
        self.failUnless(client.waitForRangedNamePoll(self.simAu, node,
                                                     timeout=timeout,
                                                     sleep=sleep),
                        "Never called ranged name poll on %s" % node.url)
        log("Called ranged name poll on %s" % node.url)

        # (Note: It turns that waiting for the ranged name poll to
        # fail is not the right thing to do here -- it may go from
        # 'Active' to 'Repaired' before the timer has a chance to
        # catch it.  So just go straight to expecting 'Repaired'

        # expect to see the AU successfully repaired.
        log("Waiting for successful repair.")
        self.failUnless(client.waitForRangedNameRepair(self.simAu, timeout=timeout,
                                                       sleep=sleep),
                        "AU not repaired by ranged name poll.")

        log("AU repaired by ranged name poll.")


###########################################################################
## Pre-test set-up.
###########################################################################
def usage():
    print "usage: testsuite <propfile>"
    sys.exit(0)

def getFrameworkDir():
    """ Construct the name of a directory under the top-level work
    directory in which to create the framework.  This allows each
    functional test to work in its own directory, and for all the test
    results to be left for examination if deleteAfter is false. """
    return tempfile.mkdtemp(prefix='framework-', dir=workDir)

def getOpts():
    if not len(sys.argv) == 2:
        usage()

    propFile = sys.argv[1]
    config = loadConfig(propFile)

    global username, password, workDir, daemonCount, projectDir,\
           deleteAfter, debugLevel, startPort

    ##
    ## Ensure all required properties are available.
    ##
    try:
        username = config['username']
        password = config['password']
        workDir = config['workDir']
        daemonCount = config['daemonCount']
    except KeyError, e:
        print "Unable to load required property: %s" % e
        sys.exit(0)

    ##
    ## assert that the workDir exists and is writable.
    ##
    if not (path.isdir(workDir) and os.access(workDir, os.W_OK)):
        print "Work dir %s does not exist or is not writable, can't continue." % workDir
        sys.exit(0)

    ##
    ## Optional properties.
    ##
    if config.has_key('projectDir'):
        projectDir = config['projectDir']
    else:
        projectDir = None

    if config.has_key('debugLevel'):
        debugLevel = config['debugLevel']
    else:
        debugLevel = 'debug'

    if config.has_key('deleteAfter'):
        val = config['deleteAfter']
        if val.lower() == 'true' or val == '1' or val.lower() == 'yes':
            deleteAfter = True
        else:
            deleteAfter = False
    else:
        deleteAfter = False

    if config.has_key('startPort'):
        startPort = int(config['startPort'])
    else:
        startPort = 8081


###
### Main entry point for this test suite.
###
if __name__ == "__main__":
    getOpts()
    try:
        suite = unittest.TestSuite()
        suite.addTest(SimpleDamageTestCase())
        suite.addTest(SimpleDeleteTestCase())
        suite.addTest(RangedNamePollDeleteTestCase())
        runner = unittest.TextTestRunner()
        runner.run(suite)
    except:
        # ensure the framework has stopped.
        curFramework.stop()
