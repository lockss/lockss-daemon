#!/usr/bin/python

import sys
import unittest
import time
from getopt import getopt, GetoptError
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
  username = <ui username>
  password = <ui password>
  frameworkDir = /path/to/create/framworks/
  daemonCount = 4
  debuglevel = debug3
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
        # Create framework
        self.framework = Framework(frameworkDir=fwdir, username=username,\
                                   password=password, daemonCount=daemonCount,\
                                   debuglevel=debuglevel)

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
        ## Create some AUs
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
        self.framework.clean()


class SimpleDamageTestCase(SimpleTestCase):
    def runTest(self):
        "Test recovery from random file damage."

        ## To select a random client, uncomment this line.
        # client = self.clients[random.randint(0, len(clients) - 1)]

        ## To use a specific client, uncomment this line.
        client = self.clients[0]
        
        node = client.randomDamage(self.simAu)
        log("Damaged node %s" % node.url)

        # set timeouts to wait up to one hour, pausing 10 in each loop
        timeout = 60 * 60
        sleep = 10
        
        # expect to see a top level content poll
        log("Waiting for top level content poll.")
        self.failUnless(client.waitForTopLevelContentPoll(self.simAu,
                                                          timeout=timeout,
                                                          sleep=sleep),
                        "Never called top level content poll")
        log("Succeesfully called top level content poll.")
        
        # expect to see the top level node marked repairing.
        log("Waiting for lockssau to be marked 'damaged'.")
        self.failUnless(client.waitForDamage(self.simAu, None, timeout=timeout,
                                             sleep=sleep),
                        "Client never marked lockssau 'damaged'.")
        log("Successfully marked lockssau 'damaged'")
        
        # expect to see top level name poll
        log("Waiting for top level name poll.")
        self.failUnless(client.waitForTopLevelNamePoll(self.simAu, timeout=timeout,
                                                       sleep=sleep),
                        "Never called top level name poll")
        log("Successfully called top level name poll")

        # expect to see the specific damaged node marked for repairing.
        log("Waiting for damaged node %s to be marked 'damaged'." % node.url)
        self.failUnless(client.waitForDamage(self.simAu, node, timeout=timeout,
                                             sleep=sleep),
                        "Never marked node %s 'damaged'" % node.url)
        log("Successfully marked node %s 'damaged'" % node.url)

        # TODO: Finer grained tests here.
        
        # expect to see the AU successfully repaired.
        log("Waiting for successful repair of node %s." % node.url)
        self.failUnless(client.waitForContentRepair(self.simAu, node,
                                                    timeout=timeout,
                                                    sleep=sleep),
                        "Node %s not successfully repaired." % node.url)
        log("Node %s successfully repaired." % node.url)

class SimpleDeleteTestCase(SimpleTestCase):
    def runTest(self):
        "Test recovery from a random file deletion."
        
        ## To select a random client, uncomment this line.
        # client = self.clients[random.randint(0, len(clients) - 1)]

        ## To use a specific client, uncomment this line.
        client = self.clients[1]
        
        node = client.randomDelete(self.simAu)
        log("Deleted node %s" % node.url)

        # set timeouts to wait up to one hour, pausing 10 in each loop
        timeout = 60 * 60
        sleep = 10
        
        # expect to see a top level content poll
        log("Waiting for top level content poll.")
        self.failUnless(client.waitForTopLevelContentPoll(self.simAu,
                                                          timeout=timeout,
                                                          sleep=sleep),
                        "Never called top level content poll")
        log("Succeesfully called top level content poll.")
        
        # expect to see the top level node marked repairing.
        log("Waiting for lockssau to be marked 'damaged'.")
        self.failUnless(client.waitForDamage(self.simAu, None,
                                             timeout=timeout,
                                             sleep=sleep),
                        "Client never marked lockssau 'damaged'.")
        log("Successfully marked lockssau 'damaged'")
        
        # expect to see top level name poll
        log("Waiting for top level name poll.")
        self.failUnless(client.waitForTopLevelNamePoll(self.simAu,
                                                       timeout=timeout,
                                                       sleep=sleep),
                        "Never called top level name poll")
        log("Successfully called top level name poll")

        # TODO: finer grained tests.

        # expect to see the AU successfully repaired.
        log("Waiting for successful repair.")
        self.failUnless(client.waitForNameRepair(self.simAu,
                                                 timeout=timeout,
                                                 sleep=sleep),
                        "Au not successfully repaired.")
        log("AU successfully repaired.")

##
## Super-class for simple delete/damage repair test cases.
##
class RangedTestCase(unittest.TestCase):
    def setUp(self):
        self.framework = Framework(frameworkDir=fwdir, username=username,\
                                   password=password, daemonCount=daemonCount,\
                                   debuglevel=debuglevel)

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
        ## Create some AUs
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

        # set timeouts to wait up to one hour, pausing 10 in each loop
        timeout = 60 * 60
        sleep = 10
        
        # expect to see a top level content poll
        log("Waiting for top level content poll.")
        self.failUnless(client.waitForTopLevelContentPoll(self.simAu,
                                                          timeout=timeout,
                                                          sleep=sleep),
                        "Never called top level content poll")
        log("Succeesfully called top level content poll.")

        # expect to see the top level node marked damaged.
        log("Waiting for lockssau to be marked 'damaged'.")
        self.failUnless(client.waitForDamage(self.simAu, None,
                                             timeout=timeout,
                                             sleep=sleep),
                        "Client never marked lockssau 'damaged'.")
        log("Successfully marked lockssau 'damaged'.")

        # expect to see top level name poll
        log("Waiting for top level name poll.")
        self.failUnless(client.waitForTopLevelNamePoll(self.simAu,
                                                       timeout=timeout,
                                                       sleep=sleep),
                        "Never called top level name poll")
        log("Successfully called top level name poll")

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
        
        # expect to lose a ranged name poll on baes URL.
        node = client.getAuNode(self.simAu, self.simAu.baseUrl)
        log("Waiting to lose name poll on %s" % node.url)
        self.failUnless(client.waitForLostRangedNamePoll(self.simAu, node,
                                                         timeout=timeout,
                                                         sleep=sleep),
                        "Never lost ranged name poll on %s" % node.url)
        log("Lost ranged name poll on %s" % node.url)

        # expect to see the AU successfully repaired.
        log("Waiting for successful repair.")
        self.failUnless(client.waitForRangedNameRepair(self.simAu, timeout=timeout,
                                                       sleep=sleep),
                        "AU not successfully repaired by ranged name poll.")

        log("AU successfully repaired by ranged name poll.")


###########################################################################
## Pre-test set-up.
###########################################################################
def usage():
    print "usage: testsuite <propfile>"
    sys.exit(0)

def getOpts():
    if not len(sys.argv) == 2:
        usage()
        
    propFile = sys.argv[1]
    config = loadConfig(propFile)

    ##
    ## Ensure all required properties are available.
    ##
    global username, password, fwdir, daemonCount, debuglevel

    try:
        username = config['username']
        password = config['password']
        fwdir = config['frameworkDir']
        daemonCount = config['daemonCount']
        debuglevel = config['debuglevel']
    except KeyError, e:
        print "Unable to load required property: %s" % e
        sys.exit(0)

###
### Main entry point for this test suite.
###
if __name__ == "__main__":
    getOpts()

    try:
        suite = unittest.TestSuite()
        suite.addTest(SimpleDamageTestCase())
        suite.addTest(SimpleDeleteTestCase())
        ## commented out until FuncSimulatedContent is fixed and
        ## org.lockss.plugin.simulated.SimulatedContentGenerator is
        ## checked in:
        # suite.addTest(RangedNamePollDeleteTestCase())
        runner = unittest.TextTestRunner()
        runner.run(suite)
    except:
        # ensure the framework has stopped.
        curFramework.stop()

