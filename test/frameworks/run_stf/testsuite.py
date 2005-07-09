#!/usr/bin/python
"""
This test suite requires at minimum a top-level work directory to
build frameworks in.  Optional parameters may also be set, if desired,
to change the default behavior.  See the file for details.
"""
import sys, time, unittest, os, urllib2, re
from lockss_util import *

##
## Load configuration.
##
loadConfig('./testsuite.props')

from lockss_daemon import *

##
## module globals
##
log = Logger()
frameworkList = []
deleteAfterSuccess = config.getBoolean('deleteAfterSuccess', True)

##
## Super class for all LOCKSS daemon test cases.
##

class LockssTestCase(unittest.TestCase):
    """ Superclass for all STF test cases. """
    def __init__(self):
        unittest.TestCase.__init__(self)

        self.delayShutdown = config.get('delayShutdown', False)
        self.timeout = int(config.get('timeout', 60 * 60 * 8))

        ##
        ## assert that the workDir exists and is writable.
        ##
        self.workDir = config.get('workDir', './')
        if not (os.path.isdir(self.workDir) and \
                os.access(self.workDir, os.W_OK)):
            raise LockssError("Work dir %s does not exist or is not writable." \
                              % self.workDir)

    def getConfigUrls(self):
        return None

    def getDaemonCount(self):
        return None
    
    def setUp(self):
        ## Log start of test.
        log.info("====================================================================")
        log.info(self.__doc__)
        log.info("--------------------------------------------------------------------")
        
        ##
        ## Create a framework for the test.
        ##
        self.framework = Framework(self.getDaemonCount(), self.getConfigUrls())

        ## global ('static') reference to the current framework, so we
        ## can clean up after a user interruption
        global frameworkList
        frameworkList.append(self.framework)

        ##
        ## List of clients, one for each daemon
        ##
        self.clients = self.framework.clientList
        
        unittest.TestCase.setUp(self)


    def tearDown(self):
        # dump threads and look for deadlocks.  This will happen
        # whether or not there was a failure.
        deadlockLogs = self.framework.checkForDeadlock()
        if deadlockLogs:
            log.warn("Deadlocks detected!")
            self.fail("Failing due to deadlock detection.  Check the "
                      "following log file(s): %s" % ", ".join(deadlockLogs))
        else:
            log.info("No deadlocks detected.")

        if self.delayShutdown:
            raw_input(">>> Delaying shutdown.  Press any key to continue...")

        self.framework.stop()
        self.failIf(self.framework.isRunning,
                    'Framework did not stop.')

        unittest.TestCase.tearDown(self)
    
class LockssAutoStartTestCase(LockssTestCase):
    """ Extension of LockssTestCase that automatically starts the
    framework in the setUp method.  Typically, you should extend this
    class to create a new method unless you want to have more control
    over when the framework starts up. """

    def setUp(self):
        LockssTestCase.setUp(self)

        ##
        ## Start the framework.
        ##
        log.info("Starting framework in %s" % self.framework.frameworkDir)
        self.framework.start()
        assert self.framework.isRunning, 'Framework failed to start.'

        # Block return until all clients are ready to go.
        log.info("Waiting for framework to come ready.")
        for client in self.clients:
            client.waitForDaemonReady()


##
## Sanity check self-test cases.  Please ignore these.
##

class SucceedingTestTestCase(LockssAutoStartTestCase):
    " Test case that succeeds immediately after daemons start. "
    def runTest(self):
        log.info("Succeeding immediately.")
        return

class FailingTestTestCase(LockssAutoStartTestCase):
    " Test case that fails immediately after daemons start. "    
    def runTest(self):
        log.info("Failing immediately.")
        self.fail("Failed on purpose.")

class ImmediateSucceedingTestTestCase(unittest.TestCase):
    " Test case that succeeds immediately, without starting the daemons. "
    def runTest(self):
	return

class ImmediateFailingTestTestCase(unittest.TestCase):
    " Test case that fails immediately, without starting the daemons. "
    def runTest(self):
        log.info("Failing immediately.")
        self.fail("Failed on purpose.")

###########################################################################
## Test cases.  Add test cases here, as well as to the appropriate
## TestSuite-creating method below.
###########################################################################

##
## Ensure caches can recover from simple file damage.
##
class SimpleDamageTestCase(LockssAutoStartTestCase):
    "Test recovery from random file damage."
    
    def runTest(self):
        # Tiny AU for simple testing.
        simAu = SimulatedAu('localA', 0, 0, 3)

        ##
        ## Create simulated AUs
        ##
        log.info("Creating simulated AUs.")
        for client in self.clients:
            client.createAu(simAu)

        ##
        ## Assert that the AUs have been crawled.
        ##
        log.info("Waiting for simulated AUs to crawl.")
        for client in self.clients:
            if not (client.waitForSuccessfulCrawl(simAu)):
                self.fail("AUs never completed initial crawl.")
        log.info("AUs completed initial crawl.")

        ## To select a random client, uncomment this line.
        # client = self.clients[random.randint(0, len(clients) - 1)]

        ## To use a specific client, uncomment this line.
        client = self.framework.getClientByPort(8081)

        node = client.randomDamageSingleNode(simAu)
        log.info("Damaged node %s on client %s" % (node.url, client))

        # Request a tree walk (deactivate and reactivate AU)
        log.info("Requesting tree walk.")
        client.requestTreeWalk(simAu)

        # expect to see a top level content poll
        log.info("Waiting for top level content poll.")
        assert client.waitForTopLevelContentPoll(simAu, timeout=self.timeout),\
               "Never called top level content poll"
        log.info("Called top level content poll.")

        # expect to see the top level node marked repairing.
        log.info("Waiting for lockssau to be marked 'damaged'.")
        assert client.waitForTopLevelDamage(simAu, timeout=self.timeout),\
               "Client never marked lockssau 'damaged'."
        log.info("Marked lockssau 'damaged'.")

        # expect to see top level name poll
        log.info("Waiting for top level name poll.")
        assert client.waitForTopLevelNamePoll(simAu, timeout=self.timeout),\
               "Never called top level name poll"
        log.info("Called top level name poll")

        # expect to see the specific node marked 'damaged'
        log.info("Waiting for node %s to be marked 'damaged'." % node.url)
        assert client.waitForDamage(simAu, node, timeout=self.timeout),\
               "Never marked node %s 'damaged'" % node.url
        log.info("Marked node %s 'damaged'" % node.url)

        # expect to see the node successfully repaired.
        log.info("Waiting for successful repair of node %s." % node.url)
        assert client.waitForContentRepair(simAu, node, timeout=self.timeout),\
               "Node %s not repaired." % node.url
        log.info("Node %s repaired." % node.url)

        # expect to see the AU successfully repaired
        log.info("Waiting for successful repair of AU.")
        assert client.waitForTopLevelRepair(simAu, timeout=self.timeout),\
               "AU never repaired."
        log.info("AU successfully repaired.")


##
## Ensure the cache can recover from a simple file deletion.
## (not resulting in a ranged name poll)
##
class SimpleDeleteTestCase(LockssAutoStartTestCase):
    "Test recovery from a random file deletion."
    
    def runTest(self):
        # Tiny AU for simple testing.
        simAu = SimulatedAu('localA', 0, 0, 3)

        ##
        ## Create simulated AUs
        ##
        log.info("Creating simulated AUs.")
        for client in self.clients:
            client.createAu(simAu)

        ##
        ## Assert that the AUs have been crawled.
        ##
        log.info("Waiting for simulated AUs to crawl.")
        for client in self.clients:
            if not (client.waitForSuccessfulCrawl(simAu)):
                self.fail("AUs never completed initial crawl.")
        log.info("AUs completed initial crawl.")

        ## To select a random client, uncomment this line.
        # client = self.clients[random.randint(0, len(clients) - 1)]

        ## To use a specific client, uncomment this line.
        client = framework.getClientByPort(8081)

        node = client.randomDelete(simAu)
        log.info("Deleted node %s on client %s" % (node.url, client))

        # Request a tree walk (deactivate and reactivate AU)
        log.info("Requesting tree walk.")
        client.requestTreeWalk(simAu)

        # expect to see a top level content poll
        log.info("Waiting for top level content poll.")
        assert client.waitForTopLevelContentPoll(simAu, timeout=self.timeout),\
               "Never called top level content poll"
        log.info("Called top level content poll.")

        # expect to see the top level node marked repairing.
        log.info("Waiting for lockssau to be marked 'damaged'.")
        assert client.waitForTopLevelDamage(simAu, timeout=self.timeout),\
               "Client never marked lockssau 'damaged'."
        log.info("Marked lockssau 'damaged'.")

        # expect to see top level name poll
        log.info("Waiting for top level name poll.")
        assert client.waitForTopLevelNamePoll(simAu, timeout=self.timeout),\
               "Never called top level name poll"
        log.info("Called top level name poll")

        # expect to see the AU successfully repaired.
        log.info("Waiting for repair.")
        assert client.waitForNameRepair(simAu, timeout=self.timeout),\
               "Au not repaired."
        log.info("AU repaired.")

##
## Ensure that the cache can recover following an extra file created
## (not resulting in a ranged name poll)
##
class SimpleExtraFileTestCase(LockssAutoStartTestCase):
    "Test recovery from an extra node in our cache"

    def runTest(self):
        # Tiny AU for simple testing.
        simAu = SimulatedAu('localA', 0, 0, 3)

        ##
        ## Create simulated AUs
        ##
        log.info("Creating simulated AUs.")
        for client in self.clients:
            client.createAu(simAu)

        ##
        ## Assert that the AUs have been crawled.
        ##
        log.info("Waiting for simulated AUs to crawl.")
        for client in self.clients:
            if not (client.waitForSuccessfulCrawl(simAu)):
                self.fail("AUs never completed initial crawl.")
        log.info("AUs completed initial crawl.")

        ## To select a random client, uncomment this line.
        # client = self.clients[random.randint(0, len(clients) - 1)]

        ## To use a specific client, uncomment this line.
        client = self.framework.getClientByPort(8082)

        node = client.createNode(simAu, 'extrafile.txt')
        log.info("Created file %s on client %s" % (node.url, client))

        # Request a tree walk (deactivate and reactivate AU)
        log.info("Requesting tree walk.")
        client.requestTreeWalk(simAu)

        # expect to see a top level content poll
        log.info("Waiting for top level content poll.")
        assert client.waitForTopLevelContentPoll(simAu, timeout=self.timeout),\
               "Never called top level content poll"
        log.info("Called top level content poll.")

        # expect to see the top level node marked repairing.
        log.info("Waiting for lockssau to be marked 'damaged'.")
        assert client.waitForTopLevelDamage(simAu, timeout=self.timeout),\
               "Client never marked lockssau 'damaged'."
        log.info("Marked lockssau 'damaged'.")

        # expect to see top level name poll
        log.info("Waiting for top level name poll.")
        assert client.waitForTopLevelNamePoll(simAu, timeout=self.timeout),\
               "Never called top level name poll"
        log.info("Called top level name poll")

        #
        # TODO: Expect to see the node removed from the node list.
        #

        # expect to see the AU successfully repaired.
        log.info("Waiting for repair.")
        assert client.waitForTopLevelRepair(simAu, timeout=self.timeout),\
               "Au not repaired."
        log.info("AU repaired.")


##
## Ensure that the cache can recover following damage that results
## in a ranged name poll being called.
##
class RangedNamePollDeleteTestCase(LockssAutoStartTestCase):
    "Test recovery from a file deletion after a ranged name poll"

    def runTest(self):
        # Long names, shallow depth, wide range for testing ranged polls
        simAu = SimulatedAu('localA', depth=0, branch=0,
                            numFiles=45, maxFileName=26)

        ##
        ## Create simulated AUs
        ##
        log.info("Creating simulated AUs.")
        for client in self.clients:
            client.createAu(simAu)

        ##
        ## Assert that the AUs have been crawled.
        ##
        log.info("Waiting for simulated AUs to crawl.")
        for client in self.clients:
            if not (client.waitForSuccessfulCrawl(simAu)):
                self.fail("AUs never successfully completed initial crawl.")
        log.info("AUs successfully completed initial crawl.")

        ## To select a random client, uncomment this line.
        # client = self.clients[random.randint(0, len(clients) - 1)]

        ## To use a specific client, uncomment this line.
        client = self.framework.getClientByPort(8082)

        # get a file that will be in the second packet
        filename = '045abcdefghijklmnopqrstuvwxyz.txt'
        node = client.getAuNode(simAu,
                                "http://www.example.com/" + filename)
        client.deleteNode(node)
        log.info("Deleted node %s on client %s" % (node.url, client))

        # Request a tree walk (deactivate and reactivate AU)
        log.info("Requesting tree walk.")
        client.requestTreeWalk(simAu)

        # expect to see a top level content poll
        log.info("Waiting for top level content poll.")
        assert client.waitForTopLevelContentPoll(simAu, timeout=self.timeout),\
               "Never called top level content poll"
        log.info("Called top level content poll.")

        # expect to see the top level node marked damaged.
        log.info("Waiting for lockssau to be marked 'damaged'.")
        assert client.waitForTopLevelDamage(simAu, timeout=self.timeout),\
               "Client never marked lockssau 'damaged'."
        log.info("Marked lockssau 'damaged'.")

        # expect to see top level name poll
        log.info("Waiting for top level name poll.")
        assert client.waitForTopLevelNamePoll(simAu, timeout=self.timeout),\
               "Never called top level name poll."
        log.info("Called top level name poll")

        # expect to win a top level name poll (name poll on baseURL
        # should be lost later on in the test)
        log.info("Waiting to win top level name poll.")
        assert client.waitForWonTopLevelNamePoll(simAu, timeout=self.timeout),\
               "Never won top level name poll"
        log.info("Won top level name poll.")

        # expect to call a name poll on baes URL.
        node = client.getAuNode(simAu, simAu.baseUrl)
        log.info("Waiting to call name poll on %s" % node.url)
        assert client.waitForNamePoll(simAu, node, timeout=self.timeout),\
               "Never called name poll on %s" % node.url
        log.info("Called name poll on %s" % node.url)

        # expect to lose a name poll on baes URL.
        node = client.getAuNode(simAu, simAu.baseUrl)
        log.info("Waiting to lose name poll on %s" % node.url)
        assert client.waitForLostNamePoll(simAu, node, timeout=self.timeout),\
               "Never lost name poll on %s" % node.url
        log.info("Lost name poll on %s" % node.url)

        # expect to call a ranged name poll on baes URL.
        node = client.getAuNode(simAu, simAu.baseUrl)
        log.info("Waiting to call ranged name poll on %s" % node.url)
        assert client.waitForRangedNamePoll(simAu, node, timeout=self.timeout),\
               "Never called ranged name poll on %s" % node.url
        log.info("Called ranged name poll on %s" % node.url)

        # (Note: It turns that waiting for the ranged name poll to
        # fail is not the right thing to do here -- it may go from
        # 'Active' to 'Repaired' before the timer has a chance to
        # catch it.  So just go straight to expecting 'Repaired'

        # expect to see the AU successfully repaired.
        log.info("Waiting for successful repair.")
        assert client.waitForRangedNameRepair(simAu, timeout=self.timeout),\
               "AU not repaired by ranged name poll."
        log.info("AU repaired by ranged name poll.")


##
## Ensure that the cache can recover following an extra file being
## added to an AU large enough to trigger a ranged name poll.
##
class RangedNamePollExtraFileTestCase(LockssAutoStartTestCase):
    "Test recovery from an extra file that triggers a ranged name poll"

    def runTest(self):
        # Long names, shallow depth, wide range for testing ranged polls
        simAu = SimulatedAu('localA', depth=0, branch=0,
                            numFiles=45, maxFileName=26)

        ##
        ## Create simulated AUs
        ##
        log.info("Creating simulated AUs.")
        for client in self.clients:
            client.createAu(simAu)

        ##
        ## Assert that the AUs have been crawled.
        ##
        log.info("Waiting for simulated AUs to crawl.")
        for client in self.clients:
            if not (client.waitForSuccessfulCrawl(simAu)):
                self.fail("AUs never successfully completed initial crawl.")
        log.info("AUs successfully completed initial crawl.")

        ## To select a random client, uncomment this line.
        # client = self.clients[random.randint(0, len(clients) - 1)]

        ## To use a specific client, uncomment this line.
        client = self.framework.getClientByPort(8082)

        # Create a file that doesn't exist
        filename = '046extrafile.txt'
        node = client.createNode(simAu, filename)
        log.info("Created file %s on client %s" % (node.url, client))

        # Request a tree walk (deactivate and reactivate AU)
        log.info("Requesting tree walk.")
        client.requestTreeWalk(simAu)

        # expect to see a top level content poll
        log.info("Waiting for top level content poll.")
        assert client.waitForTopLevelContentPoll(simAu, timeout=self.timeout),\
               "Never called top level content poll"
        log.info("Called top level content poll.")

        # expect to see the top level node marked damaged.
        log.info("Waiting for lockssau to be marked 'damaged'.")
        assert client.waitForTopLevelDamage(simAu, timeout=self.timeout),\
               "Client never marked lockssau 'damaged'."
        log.info("Marked lockssau 'damaged'.")

        # expect to see top level name poll
        log.info("Waiting for top level name poll.")
        assert client.waitForTopLevelNamePoll(simAu, timeout=self.timeout),\
               "Never called top level name poll."
        log.info("Called top level name poll")

        # expect to win a top level name poll (name poll on baseURL
        # should be lost later on in the test)
        log.info("Waiting to win top level name poll.")
        assert client.waitForWonTopLevelNamePoll(simAu, timeout=self.timeout),\
               "Never won top level name poll"
        log.info("Won top level name poll.")

        # expect to call a name poll on base URL.
        baseUrlNode = client.getAuNode(simAu, simAu.baseUrl)
        log.info("Waiting to call name poll on %s" % baseUrlNode.url)
        assert client.waitForNamePoll(simAu, baseUrlNode, timeout=self.timeout),\
               "Never called name poll on %s" % baseUrlNode.url
        log.info("Called name poll on %s" % node.url)

        # expect to lose a name poll on baes URL.
        log.info("Waiting to lose name poll on %s" % baseUrlNode.url)
        assert client.waitForLostNamePoll(simAu, baseUrlNode, timeout=self.timeout),\
               "Never lost name poll on %s" % baseUrlNode.url
        log.info("Lost name poll on %s" % baseUrlNode.url)

        # expect to call a ranged name poll on base URL.
        node = client.getAuNode(simAu, simAu.baseUrl)
        log.info("Waiting to call ranged name poll on %s" % node.url)
        assert client.waitForRangedNamePoll(simAu, node, timeout=self.timeout),\
               "Never called ranged name poll on %s" % node.url
        log.info("Called ranged name poll on %s" % node.url)

        #
        # TODO: Expect to see the node removed from the node list.
        #

        # (Note: It turns that waiting for the ranged name poll to
        # fail is not the right thing to do here -- it may go from
        # 'Active' to 'Repaired' before the timer has a chance to
        # catch it.  So just go straight to expecting 'Repaired'

        # expect to see the AU successfully repaired.
        log.info("Waiting for successful repair.")
        assert client.waitForTopLevelRepair(simAu, timeout=self.timeout),\
               "AU never repaired."
        log.info("AU successfully repaired.")

##
## Create a randomly sized AU (with reasonable limits on maximum depth
## and breadth of the tree structure), and cause damage.  Wait for
## all damaged nodes to be repaired.
##

class RandomizedDamageTestCase(LockssAutoStartTestCase):
    "Test recovery from random file damage in a randomly sized AU."

    def runTest(self):
	random.seed(time.time())
        depth = random.randint(0, 2)
        branch = random.randint(0, 2)
        numFiles = random.randint(3, 20)
        maxFileName = 26
        log.info("Creating simulated AUs: depth = %s; branch = %s; "
            "numFiles = %s; maxFileName = %s" %
            (depth, branch, numFiles, maxFileName))

        ##
        ## Create simulated AUs
        ##
        simAu = SimulatedAu(root='localA', depth=depth, branch=branch,
                            numFiles=numFiles, maxFileName=maxFileName)
        for client in self.clients:
            client.createAu(simAu)

        ##
        ## Assert that the AUs have been crawled.
        ##
        log.info("Waiting for simulated AUs to crawl.")
        for client in self.clients:
            if not (client.waitForSuccessfulCrawl(simAu)):
                self.fail("AUs never completed initial crawl.")
        log.info("AUs completed initial crawl.")

        ## Pick a client at random
        client = self.clients[random.randint(0, len(self.clients) - 1)]

        log.info("Causing random damage...")
        nodeList = client.randomDamageRandomNodes(simAu, 1, 5)

        log.info("Damaged the following nodes on client %s:\n        %s" %
            (client, '\n        '.join([str(n) for n in nodeList])))

        # Request a tree walk (deactivate and reactivate AU)
        log.info("Requesting tree walk.")
        client.requestTreeWalk(simAu)

        # expect to see a top level content poll
        log.info("Waiting for top level content poll.")
        assert client.waitForTopLevelContentPoll(simAu, timeout=self.timeout),\
               "Never called top level content poll"
        log.info("Called top level content poll.")

        # expect to see the top level node marked repairing.
        log.info("Waiting for lockssau to be marked 'damaged'.")
        assert client.waitForTopLevelDamage(simAu, timeout=self.timeout),\
               "Client never marked lockssau 'damaged'."
        log.info("Marked lockssau 'damaged'.")

        # expect to see top level name poll
        log.info("Waiting for top level name poll.")
        assert client.waitForTopLevelNamePoll(simAu, timeout=self.timeout),\
               "Never called top level name poll"
        log.info("Called top level name poll")

        # TODO:
        # Need methods that watch a LIST of nodes, not just one
        # node.  For now, this is good enough, but improve
        # as soon as possible.
        node = nodeList[random.randint(0, len(nodeList) - 1)]

        # expect to see a specific node marked 'damaged'
        log.info("Waiting for node %s to be marked 'damaged'." % node.url)
        assert client.waitForDamage(simAu, node, timeout=self.timeout),\
               "Never marked node %s 'damaged'" % node.url
        log.info("Marked node %s 'damaged'" % node.url)

        # expect to see the node successfully repaired.
        log.info("Waiting for successful repair of node %s." % node.url)
        assert client.waitForContentRepair(simAu, node, timeout=self.timeout),\
               "Node %s not repaired." % node.url
        log.info("Node %s repaired." % node.url)

        # expect to see the AU successfully repaired.
        log.info("Waiting for successful repair of AU.")
        assert client.waitForTopLevelRepair(simAu, timeout=self.timeout),\
               "AU never repaired."
        log.info("AU repaired.")

##
## Create a randomly sized AU (with reasonable limits on maximum depth
## and breadth of the tree structure), and randomly remove files.  Wait
## for a successful repair
##

class RandomizedDeleteTestCase(LockssAutoStartTestCase):
    "Test recovery from random file deletion in a randomly sized AU."

    def runTest(self):
	random.seed(time.time())
        depth = random.randint(0, 2)
        branch = random.randint(0, 2)
        numFiles = random.randint(3, 20)
        maxFileName = 26
        log.info("Creating simulated AUs: depth = %s; branch = %s; "
            "numFiles = %s; maxFileName = %s" %
            (depth, branch, numFiles, maxFileName))

        ##
        ## Create simulated AUs
        ##
        simAu = SimulatedAu(root='localA', depth=depth, branch=branch,
                            numFiles=numFiles, maxFileName=maxFileName)
        for client in self.clients:
            client.createAu(simAu)

        ##
        ## Assert that the AUs have been crawled.
        ##
        log.info("Waiting for simulated AUs to crawl.")
        for client in self.clients:
            if not (client.waitForSuccessfulCrawl(simAu)):
                self.fail("AUs never completed initial crawl.")
        log.info("AUs completed initial crawl.")

        ## Pick a client at random
        client = self.clients[random.randint(0, len(self.clients) - 1)]

        log.info("Deleting random files...")
        nodeList = client.randomDeleteRandomNodes(simAu)

        log.info("Deleted the following nodes on client %s:\n        %s" %
            (client, '\n        '.join([str(n) for n in nodeList])))

        # Request a tree walk (deactivate and reactivate AU)
        log.info("Requesting tree walk.")
        client.requestTreeWalk(simAu)

        # expect to see a top level content poll
        log.info("Waiting for top level content poll.")
        assert client.waitForTopLevelContentPoll(simAu, timeout=self.timeout),\
               "Never called top level content poll"
        log.info("Called top level content poll.")

        # expect to see the top level node marked repairing.
        log.info("Waiting for lockssau to be marked 'damaged'.")
        assert client.waitForTopLevelDamage(simAu, timeout=self.timeout),\
               "Client never marked lockssau 'damaged'."
        log.info("Marked lockssau 'damaged'.")

        # expect to see top level name poll
        log.info("Waiting for top level name poll.")
        assert client.waitForTopLevelNamePoll(simAu, timeout=self.timeout),\
               "Never called top level name poll"
        log.info("Called top level name poll")

        # TODO: Definitely want more steps between these two.  Might
        # want to branch on whether it causes a ranged name poll or
        # not, too.

        # expect to see the AU successfully repaired.
        log.info("Waiting for successful repair of AU.")
        assert client.waitForTopLevelRepair(simAu, timeout=self.timeout),\
               "AU never repaired."
        log.info("AU repaired.")

##
## Create a randomly sized AU (with reasonable limits on maximum depth
## and breadth of the tree structure), and add extra files.  Wait for
## all damaged nodes to be repaired.
##
class RandomizedExtraFileTestCase(LockssAutoStartTestCase):
    """ Test recovery from random node creation in a randomly sized AU. """

    def runTest(self):    
	random.seed(time.time())
        depth = random.randint(0, 2)
        branch = random.randint(0, 2)
        numFiles = random.randint(3, 20)
        maxFileName = 26
        log.info("Creating simulated AUs: depth = %s; branch = %s; "
            "numFiles = %s; maxFileName = %s" %
            (depth, branch, numFiles, maxFileName))

        ##
        ## Create simulated AUs
        ##
        simAu = SimulatedAu(root='localA', depth=depth, branch=branch,
                            numFiles=numFiles, maxFileName=maxFileName)
        for client in self.clients:
            client.createAu(simAu)

        ##
        ## Assert that the AUs have been crawled.
        ##
        log.info("Waiting for simulated AUs to crawl.")
        for client in self.clients:
            if not (client.waitForSuccessfulCrawl(simAu)):
                self.fail("AUs never completed initial crawl.")
        log.info("AUs completed initial crawl.")

        ## Pick a client at random
        client = self.clients[random.randint(0, len(self.clients) - 1)]

        log.info("Creating random nodes...")
        nodeList = client.randomCreateRandomNodes(simAu, 1, 5)

        log.info("Created the following nodes on client %s:\n        %s" %
                 (client, '\n        '.join([str(n) for n in nodeList])))

        # Request a tree walk (deactivate and reactivate AU)
        log.info("Requesting tree walk.")
        client.requestTreeWalk(simAu)

        # expect to see a top level content poll
        log.info("Waiting for top level content poll.")
        assert client.waitForTopLevelContentPoll(simAu, timeout=self.timeout),\
               "Never called top level content poll"
        log.info("Called top level content poll.")

        # expect to see the top level node marked repairing.
        log.info("Waiting for lockssau to be marked 'damaged'.")
        assert client.waitForTopLevelDamage(simAu, timeout=self.timeout),\
               "Client never marked lockssau 'damaged'."
        log.info("Marked lockssau 'damaged'.")

        # expect to see top level name poll
        log.info("Waiting for top level name poll.")
        assert client.waitForTopLevelNamePoll(simAu, timeout=self.timeout),\
               "Never called top level name poll"
        log.info("Called top level name poll")

        #
        # TODO: Expect to see the nodes removed from the node list.
        #

        # expect to see the AU successfully repaired.
        log.info("Waiting for repair.")
        assert client.waitForTopLevelRepair(simAu, timeout=self.timeout),\
               "Au not repaired."
        log.info("AU repaired.")


## Test repairs from a cache.  Create a small AU, damage it, and wait
## for it to be repaird, just like in SimpleDamageTestCase.  Extends
## 'LockssTestCase' instead of 'LockssAutoStartTestCase' so we can
## control adding extra parameters forcing repair from caches to the
## daemon to be damaged before starting up the test framework.
        
class SimpleDamageRepairFromCacheTestCase(LockssTestCase):
    """ Test repairing simple damage from another cache. """
    def setUp(self):
        LockssTestCase.setUp(self)

        ##
        ## Configure the daemon to be damaged (8082) to repair from
        ## a cache instead of from the publisher.
        ##

        extraConf = {"org.lockss.crawler.repair.repair_from_cache_percent": "100",
                     "org.lockss.crawler.repair.repair_from_cache_addr": "127.0.0.1"}
        self.framework.appendLocalConfig(extraConf, 8082)
        
        ##
        ## Start the framework.
        ##
        log.info("Starting framework in %s" % self.framework.frameworkDir)
        self.framework.start()
        assert self.framework.isRunning, 'Framework failed to start.'

        # Block return until all clients are ready to go.
        log.info("Waiting for framework to come ready.")
        for client in self.clients:
            client.waitForDaemonReady()

    def runTest(self):
        # Tiny AU for simple testing.
        simAu = SimulatedAu('localA', 0, 0, 1)

        ##
        ## Create simulated AUs
        ##
        log.info("Creating simulated AUs.")
        for client in self.clients:
            client.createAu(simAu)

        ##
        ## Assert that the AUs have been crawled.
        ##
        log.info("Waiting for simulated AUs to crawl.")
        for client in self.clients:
            if not (client.waitForSuccessfulCrawl(simAu)):
                self.fail("AUs never completed initial crawl.")
        log.info("AUs completed initial crawl.")

        # Damage the client running on port 8082, the one that was set
        # up with 'repair_from_cache_percent=1.0'
        client = self.framework.getClientByPort(8082)

        # Request a tree walk (deactivate and reactivate AU)
        log.info("Requesting tree walk.")
        client.requestTreeWalk(simAu)

        # expect to see a top level content poll called
        log.info("Waiting for top level content poll.")
        assert client.waitForTopLevelContentPoll(simAu, timeout=self.timeout),\
               "Never called top level content poll"
        log.info("Called top level content poll.")

	log.info("Waiting to win top level content poll.")
	assert client.waitForWonTopLevelContentPoll(simAu, timeout=self.timeout),\
	       "Never won top level content poll"

	# Damage a node.
        node = client.randomDamageSingleNode(simAu)
        log.info("Damaged node %s on client %s" % (node.url, client))

        # Request a tree walk (deactivate and reactivate AU)
        log.info("Requesting tree walk.")
        client.requestTreeWalk(simAu)

        # expect to see a top level content poll called
        log.info("Waiting for top level content poll.")
        assert client.waitForTopLevelContentPoll(simAu, timeout=self.timeout),\
               "Never called top level content poll"
        log.info("Called top level content poll.")	

        # expect to see the top level node marked repairing.
        log.info("Waiting for lockssau to be marked 'damaged'.")
        assert client.waitForTopLevelDamage(simAu, timeout=self.timeout),\
               "Client never marked lockssau 'damaged'."
        log.info("Marked lockssau 'damaged'.")

        # expect to see top level name poll
        log.info("Waiting for top level name poll.")
        assert client.waitForTopLevelNamePoll(simAu, timeout=self.timeout),\
               "Never called top level name poll"
        log.info("Called top level name poll")

        # expect to see the specific node marked 'damaged'
        log.info("Waiting for node %s to be marked 'damaged'." % node.url)
        assert client.waitForDamage(simAu, node, timeout=self.timeout),\
               "Never marked node %s 'damaged'" % node.url
        log.info("Marked node %s 'damaged'" % node.url)

        # expect to see the node successfully repaired.
        log.info("Waiting for successful repair of node %s." % node.url)
        assert client.waitForContentRepair(simAu, node, timeout=self.timeout),\
               "Node %s not repaired." % node.url
        log.info("Node %s repaired." % node.url)

        # expect to see the AU successfully repaired
        log.info("Waiting for successful repair of AU.")
        assert client.waitForTopLevelRepair(simAu, timeout=self.timeout),\
               "AU never repaired."
        log.info("AU successfully repaired.")        

class TinyUiTests(LockssTestCase):
    def setUp(self):
        LockssTestCase.setUp(self)

        ##
        ## Start the framework.
        ##
        log.info("Starting framework in %s" % self.framework.frameworkDir)
        self.framework.start()
        assert self.framework.isRunning, 'Framework failed to start.'

        # Block return until all clients are ready to go.
        log.info("Waiting for framework to come ready.")
        self.tinyUiClient = self.clients[0]
        time.sleep(2)
        self.tinyUiClient.waitForCanConnectToHost()
            
    def getDaemonCount(self):
        return 1
    
class TinyUiMalformedUrlTestCase(TinyUiTests):
    """ Test a malformed config URL gets the Tiny UI """
    def getConfigUrls(self):
        return ["foobar:"]

    def runTest(self):
        tinyui = self.tinyUiClient.getAdminUi()
        html = tinyui.read()
        p = re.compile('.*This LOCKSS cache has not started because it is unable to load configuration data.*', re.MULTILINE | re.DOTALL);
        assert(p.match(html))
        

    
###########################################################################
### Functions that build and return test suites.  These can be
### called by name when running this test script.
###########################################################################

def all():
    suite = unittest.TestSuite()
    suite.addTest(simpleTests())
    suite.addTest(rangedTests())
    suite.addTest(randomTests())
    return suite

def postTagTests():
    suite = unittest.TestSuite()
    suite.addTest(SimpleDamageTestCase())
    suite.addTest(SimpleDeleteTestCase())
    suite.addTest(SimpleExtraFileTestCase())
    suite.addTest(RangedNamePollDeleteTestCase())
    suite.addTest(RangedNamePollExtraFileTestCase())
    return suite

def simpleTests():
    suite = unittest.TestSuite()
    suite.addTest(SimpleDamageTestCase())
    suite.addTest(SimpleDeleteTestCase())
    suite.addTest(SimpleExtraFileTestCase())
    return suite

def rangedTests():
    suite = unittest.TestSuite()
    suite.addTest(RangedNamePollDeleteTestCase())
    suite.addTest(RangedNamePollExtraFileTestCase())
    return suite

def randomTests():
    suite = unittest.TestSuite()
    suite.addTest(RandomizedDamageTestCase())
    suite.addTest(RandomizedDeleteTestCase())
    suite.addTest(RandomizedExtraFileTestCase())
    return suite

def repairFromCacheTests():
    suite = unittest.TestSuite()
    suite.addTest(SimpleDamageRepairFromCacheTestCase())
    return suite

def succeedingTests():
    suite = unittest.TestSuite()
    suite.addTest(SucceedingTestTestCase())
    return suite

def failingTests():
    suite = unittest.TestSuite()
    suite.addTest(FailingTestTestCase())
    return suite

def immediateSucceedingTests():
    suite = unittest.TestSuite()
    suite.addTest(ImmediateSucceedingTestTestCase())
    return suite

def immediateFailingTests():
    suite = unittest.TestSuite()
    suite.addTest(ImmediateFailingTestTestCase())
    return suite

def tinyUiTests():
    suite = unittest.TestSuite()
    suite.addTest(TinyUiMalformedUrlTestCase())
    return suite

###########################################################################
### Main entry point for this test suite.
###########################################################################

if __name__ == "__main__":
    try:
        unittest.main()
    except SystemExit, e:
        # unittest.main() is very unfortunate here.  It does a
        # sys.exit (which raises SystemExit), instead of letting you
        # clean up after it in the try: block. The SystemExit
        # exception has one attribute, 'code', which is either True if
        # an error occured while running the tests, or False if the
        # tests ran successfully.
        for fw in frameworkList:
            if fw.isRunning: fw.stop()

        if e.code:
            sys.exit(1)
        else:
            if deleteAfterSuccess:
                for fw in frameworkList:
                    fw.clean()

    except KeyboardInterrupt:
        for fw in frameworkList:
            if fw.isRunning: fw.stop()
            
    except Exception, e:
        # Unhandled exception occured.
        log.error("%s" % e)
        sys.exit(1)
        
