#!/usr/bin/python

import sys, tempfile, time, unittest, os
from os import path
from lockss_daemon import *

curFramework = None

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

  # Override the default 'timeout' value when sitting in wait
  # loops.  Default is 45 minutes.
  timeout = 3600

"""

###########################################################################
## Test cases.  Add test cases here, as well as to the TestSuite object
## created in the main method below.
###########################################################################

##
## Super class for all LOCKSS daemon test cases.
##
class LockssTestCase(unittest.TestCase):
    def __init__(self):
        unittest.TestCase.__init__(self)

        self.username = config.get('username')
        self.password = config.get('password')
        self.workDir = config.get('workDir')
        self.daemonCount = config.get('daemonCount')

        if not (self.username or self.password or \
                self.workDir or self.daemonCount):
            raise LockssError("Unable to load required properties.")

        ##
        ## assert that the workDir exists and is writable.
        ##
        if not (path.isdir(self.workDir) and \
                os.access(self.workDir, os.W_OK)):
            raise LockssError("Work dir %s does not exist or is not writable." \
                              % self.workDir)

        ##
        ## Optional properties.
        ##
        self.projectDir = config.get('projectDir')
        self.debugLevel = config.get('debugLevel', 'debug')
        self.deleteAfter = config.get('deleteAfter', False)
        self.startPort = config.get('startPort', 8081)
        self.timeout = config.get('timeout', 60 * 45)
        

    def setUp(self):
        unittest.TestCase.setUp(self)

        ##
        ## Set up for the test.
        ##

        fwdir = self.__getFrameworkDir()
        self.framework = Framework(frameworkDir=fwdir,
                                   username=self.username,
                                   password=self.password,
                                   daemonCount=self.daemonCount,
                                   projectDir=self.projectDir,
                                   debugLevel=self.debugLevel,
                                   startPort=self.startPort)

        ## global ('static') reference to the current framework, so we
        ## can clean up after a user interruption
        global curFramework
        curFramework = self.framework
    
        ##
        ## Start the framework.
        ##
        log("Starting framework in %s" % fwdir)
        self.framework.start()
        assert self.framework.isRunning, 'Framework failed to start.'

        ##
        ## List of clients, one for each daemon in 'numDaemons'
        ##
        self.clients = self.framework.clientList

        # Block return until all clients are ready to go.
        log("Waiting for framework to come ready.")
        for client in self.clients:
            client.waitForDaemonReady()

    def tearDown(self):
        unittest.TestCase.tearDown(self)
    
        self.framework.stop()
        self.failIf(self.framework.isRunning,
                    'Framework did not stop.')
        if self.deleteAfter:
            self.framework.clean()

    def __getFrameworkDir(self):
        """ Construct the name of a directory under the top-level work
        directory in which to create the framework.  This allows each
        functional test to work in its own directory, and for all the test
        results to be left for examination if deleteAfter is false. """
        return tempfile.mkdtemp(prefix='framework-', dir=self.workDir)

##
## Ensure caches can recover from simple file damage.
##
class SimpleDamageTestCase(LockssTestCase):
    def runTest(self):
        "Test recovery from random file damage."

        # Tiny AU for simple testing.
        simAu = SimulatedAu('localA', 0, 0, 3)

        ##
        ## Create simulated AUs
        ##
        log("Creating simulated AUs.")
        for client in self.clients:
            client.createAu(simAu)

        ##
        ## Assert that the AUs have been crawled.
        ##
        log("Waiting for simulated AUs to crawl.")
        for client in self.clients:
            if not (client.waitForSuccessfulCrawl(simAu)):
                self.fail("AUs never completed initial crawl.")
        log("AUs completed initial crawl.")

        ## To select a random client, uncomment this line.
        # client = self.clients[random.randint(0, len(clients) - 1)]

        ## To use a specific client, uncomment this line.
        client = self.clients[0]

        node = client.randomDamage(simAu)
        log("Damaged client %s, node %s" % (client, node.url))

        # Request a tree walk (deactivate and reactivate AU)
        client.requestTreeWalk(simAu)

        # expect to see a top level content poll
        log("Waiting for top level content poll.")
        assert client.waitForTopLevelContentPoll(simAu, timeout=self.timeout),\
               "Never called top level content poll"
        log("Called top level content poll.")
        
        # expect to see the top level node marked repairing.
        log("Waiting for lockssau to be marked 'damaged'.")
        assert client.waitForDamage(simAu, timeout=self.timeout),\
               "Client never marked lockssau 'damaged'."
        log("Marked lockssau 'damaged'.")

        # expect to see top level name poll
        log("Waiting for top level name poll.")
        assert client.waitForTopLevelNamePoll(simAu, timeout=self.timeout),\
               "Never called top level name poll"
        log("Called top level name poll")

        # expect to see the specific damaged node marked for repairing.
        log("Waiting for damaged node %s to be marked 'damaged'." % node.url)
        assert client.waitForDamage(simAu, node, timeout=self.timeout),\
               "Never marked node %s 'damaged'" % node.url
        log("Marked node %s 'damaged'" % node.url)

        # TODO: Finer grained tests here?

        # expect to see the AU successfully repaired.
        log("Waiting for successful repair of node %s." % node.url)
        assert client.waitForContentRepair(simAu, node, timeout=self.timeout),\
               "Node %s not repaired." % node.url
        log("Node %s repaired." % node.url)

##
## Ensure the cache can recover from a simple file deletion.
## This should *not* result in a ranged name poll.
##
class SimpleDeleteTestCase(LockssTestCase):
    def runTest(self):
        "Test recovery from a random file deletion."
        # Tiny AU for simple testing.
        simAu = SimulatedAu('localA', 0, 0, 3)

        ##
        ## Create simulated AUs
        ##
        log("Creating simulated AUs.")
        for client in self.clients:
            client.createAu(simAu)

        ##
        ## Assert that the AUs have been crawled.
        ##
        log("Waiting for simulated AUs to crawl.")
        for client in self.clients:
            if not (client.waitForSuccessfulCrawl(simAu)):
                self.fail("AUs never completed initial crawl.")
        log("AUs completed initial crawl.")

        ## To select a random client, uncomment this line.
        # client = self.clients[random.randint(0, len(clients) - 1)]

        ## To use a specific client, uncomment this line.
        client = self.clients[1]

        node = client.randomDelete(simAu)
        log("Deleted client %s, node %s" % (client, node.url))

        # Request a tree walk (deactivate and reactivate AU)
        client.requestTreeWalk(simAu)

        # expect to see a top level content poll
        log("Waiting for top level content poll.")
        assert client.waitForTopLevelContentPoll(simAu, timeout=self.timeout),\
               "Never called top level content poll"
        log("Called top level content poll.")

        # expect to see the top level node marked repairing.
        log("Waiting for lockssau to be marked 'damaged'.")
        assert client.waitForDamage(simAu, None, timeout=self.timeout),\
               "Client never marked lockssau 'damaged'."
        log("Marked lockssau 'damaged'.")

        # expect to see top level name poll
        log("Waiting for top level name poll.")
        assert client.waitForTopLevelNamePoll(simAu, timeout=self.timeout),\
               "Never called top level name poll"
        log("Called top level name poll")

        # TODO: finer grained tests here?

        # expect to see the AU successfully repaired.
        log("Waiting for repair.")
        assert client.waitForNameRepair(simAu, timeout=self.timeout),\
               "Au not repaired."
        log("AU repaired.")

##
## Ensure that the cache can recover following damage that results
## in a ranged name poll being called.
##
class RangedNamePollDeleteTestCase(LockssTestCase):
    def runTest(self):
        "Test recovery from a file deletion after a ranged name poll"

        # Long names, shallow depth, wide range for testing ranged polls
        simAu = SimulatedAu('localA', depth=0, branch=0,
                            numFiles=45, maxFileName=26)

        ##
        ## Create simulated AUs
        ##
        log("Creating simulated AUs.")
        for client in self.clients:
            client.createAu(simAu)

        ##
        ## Assert that the AUs have been crawled.
        ##
        log("Waiting for simulated AUs to crawl.")
        for client in self.clients:
            if not (client.waitForSuccessfulCrawl(simAu)):
                self.fail("AUs never successfully completed initial crawl.")
        log("AUs successfully completed initial crawl.")

        ## To select a random client, uncomment this line.
        # client = self.clients[random.randint(0, len(clients) - 1)]

        ## To use a specific client, uncomment this line.
        client = self.clients[1]

        # get a file that will be in the second packet
        filename = '045abcdefghijklmnopqrstuvwxyz.txt'
        node = client.getAuNode(simAu,
                                "http://www.example.com/" + filename)
        client.deleteNode(node)
        log("Deleted client %s, node %s" % (client, node.url))

        # Request a tree walk (deactivate and reactivate AU)
        client.requestTreeWalk(simAu)

        # expect to see a top level content poll
        log("Waiting for top level content poll.")
        assert client.waitForTopLevelContentPoll(simAu, timeout=self.timeout),\
        "Never called top level content poll"
        log("Called top level content poll.")

        # expect to see the top level node marked damaged.
        log("Waiting for lockssau to be marked 'damaged'.")
        assert client.waitForDamage(simAu, None, timeout=self.timeout),\
               "Client never marked lockssau 'damaged'."
        log("Marked lockssau 'damaged'.")

        # expect to see top level name poll
        log("Waiting for top level name poll.")
        assert client.waitForTopLevelNamePoll(simAu, timeout=self.timeout),\
               "Never called top level name poll."
        log("Called top level name poll")

        # expect to win a top level name poll (name poll on baseURL
        # should be lost later on in the test)
        log("Waiting to win top level name poll.")
        assert client.waitForWonTopLevelNamePoll(simAu, timeout=self.timeout),\
               "Never won top level name poll"
        log("Won top level name poll.")

        # expect to call a name poll on baes URL.
        node = client.getAuNode(simAu, simAu.baseUrl)
        log("Waiting to call name poll on %s" % node.url)
        assert client.waitForNamePoll(simAu, node, timeout=self.timeout),\
               "Never called name poll on %s" % node.url
        log("Called name poll on %s" % node.url)

        # expect to lose a name poll on baes URL.
        node = client.getAuNode(simAu, simAu.baseUrl)
        log("Waiting to lose name poll on %s" % node.url)
        assert client.waitForLostNamePoll(simAu, node, timeout=self.timeout),\
               "Never lost name poll on %s" % node.url
        log("Lost name poll on %s" % node.url)

        # expect to call a ranged name poll on baes URL.
        node = client.getAuNode(simAu, simAu.baseUrl)
        log("Waiting to call ranged name poll on %s" % node.url)
        assert client.waitForRangedNamePoll(simAu, node, timeout=self.timeout),\
               "Never called ranged name poll on %s" % node.url
        log("Called ranged name poll on %s" % node.url)

        # (Note: It turns that waiting for the ranged name poll to
        # fail is not the right thing to do here -- it may go from
        # 'Active' to 'Repaired' before the timer has a chance to
        # catch it.  So just go straight to expecting 'Repaired'

        # expect to see the AU successfully repaired.
        log("Waiting for successful repair.")
        assert client.waitForRangedNameRepair(simAu, timeout=self.timeout),\
               "AU not repaired by ranged name poll."
        log("AU repaired by ranged name poll.")


###########################################################################
### Main entry point for this test suite.
###########################################################################

def usage():
    print "usage: testsuite <propfile>"
    sys.exit(0)

if __name__ == "__main__":
    if len(sys.argv) < 2:
        usage()

    propFile = sys.argv[1]
    config = loadConfig(propFile)

    try:
        suite = unittest.TestSuite()
        suite.addTest(SimpleDamageTestCase())
        suite.addTest(SimpleDeleteTestCase())
        suite.addTest(RangedNamePollDeleteTestCase())
        runner = unittest.TextTestRunner()
        runner.run(suite)
    except Exception, e:
        print "%s" % e
        # ensure the framework has stopped.
        if curFramework:
            curFramework.stop()
