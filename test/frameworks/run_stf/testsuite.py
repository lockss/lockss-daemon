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
        log.info("==========================================================")
        log.info(self.__doc__)
        log.info("----------------------------------------------------------")
        
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
        simAu = SimulatedAu('simContent', 0, 0, 3)

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
        client = self.clients[0]

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
        simAu = SimulatedAu('simContent', 0, 0, 3)

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
        client = self.clients[0]

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
        simAu = SimulatedAu('simContent', 0, 0, 3)

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
        client = self.clients[1]

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
        simAu = SimulatedAu('simContent', depth=0, branch=0,
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
        client = self.clients[1]

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
        simAu = SimulatedAu('simContent', depth=0, branch=0,
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
        client = self.clients[1]

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
        simAu = SimulatedAu(root='simContent', depth=depth, branch=branch,
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
        simAu = SimulatedAu(root='simContent', depth=depth, branch=branch,
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
        simAu = SimulatedAu(root='simContent', depth=depth, branch=branch,
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


class TotalLossRecoveryTestCase(LockssTestCase):
    """ Test repairing a cache that has lost all its contents """
    def setUp(self):
        LockssTestCase.setUp(self)

        self.damagedClient = self.clients[1]

        ## Configure one daemon to repair from a cache instead of from
        ## the publisher.  This is the daemon that will have damaged
        ## content.
#        extraConf = {"org.lockss.crawler.repair.repair_from_cache_percent": "100",
#                     "org.lockss.crawler.repair_from_cache_addr": "127.0.0.1"}
#        self.framework.appendLocalConfig(extraConf, self.damagedClient)

        ## Allow default-only parameters to be set.  This is necessary to allow
        ## creating 'publisher down' AUs
        extraConf = {"org.lockss.auconfig.allowEditDefaultOnlyParams": "true"}
        self.framework.appendLocalConfig(extraConf, self.damagedClient)

        ## Start the framework.
        log.info("Starting framework in %s" % self.framework.frameworkDir)
        self.framework.start()
        assert self.framework.isRunning, 'Framework failed to start.'

        ## Block return until all clients are ready to go.
        log.info("Waiting for framework to come ready.")
        for client in self.clients:
            client.waitForDaemonReady()
            
    def runTest(self):
        # Two AUs: One for before the loss, one for after.
        simAu = SimulatedAu('simContent', 0, 0, 0)

        ## Create simulated AUs
        log.info("Creating simulated AUs.")
        for client in self.clients:
            client.createAu(simAu)

        ## Assert that the AUs have been crawled.
        log.info("Waiting for simulated AUs to crawl.")
        for client in self.clients:
            if not (client.waitForSuccessfulCrawl(simAu)):
                self.fail("AUs never completed initial crawl.")
        log.info("AUs completed initial crawl.")
        
        # Select the appropriate client
        client = self.damagedClient

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
        log.info("Won top level content poll.")

        log.info("Backing up cache configuration...")
        client.backupConfiguration()
        log.info("Backed up successfully.")
        
        # All daemons should have recorded their agreeing peers at this
        # point, so stop the client we're going to damage.
        client.daemon.stop()
        log.info("Stopped daemon running on UI port %s" % client.port)

        client.simulateDiskFailure()
        log.info("Deleted entire contents of cache on stopped daemon.")

        #
        # Write out a TitleDB entry for the simulated AU so it will be marked
        # 'publisher down' when it is restored.
        #
        extraConf = {"org.lockss.auconfig.allowEditDefaultOnlyParams": "true",
                     "org.lockss.plugin.registry": "org.lockss.plugin.simulated.SimulatedPlugin",
                     "org.lockss.title.sim1.title": "Simulated Content: simContent",
                     "org.lockss.title.sim1.journalTitle": "Simulated Content",
                     "org.lockss.title.sim1.plugin": "org.lockss.plugin.simulated.SimulatedPlugin",
                     "org.lockss.title.sim1.param.1.key": "root",
                     "org.lockss.title.sim1.param.1.value": "simContent",
                     "org.lockss.title.sim1.param.2.key": "depth",
                     "org.lockss.title.sim1.param.2.value": "0",
                     "org.lockss.title.sim1.param.3.key": "branch",
                     "org.lockss.title.sim1.param.3.value": "0",
                     "org.lockss.title.sim1.param.4.key": "numFiles",
                     "org.lockss.title.sim1.param.4.value": "0",
                     "org.lockss.title.sim1.param.5.key": "pub_down",
                     "org.lockss.title.sim1.param.5.value": "true"}
        
        self.framework.appendLocalConfig(extraConf, client) 

        time.sleep(3) # Give time for things to settle before starting again
        
        client.daemon.start()
        
        # Wait for the client to come up
        assert client.waitForDaemonReady(), "Daemon never became ready"
        log.info("Started daemon running on UI port %s" % client.port)

        assert not client.hasAu(simAu)

        # Now restore the backup file
        log.info("Restoring cache configuration...")
        client.restoreConfiguration(simAu)
        log.info("Restored successfully.")
        
        # These should be equal AU IDs, so both should return true
        assert client.hasAu(simAu)

        # Request a tree walk.
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

        # expect to see the AU successfully repaired
        log.info("Waiting for successful repair of AU.")
        assert client.waitForTopLevelRepair(simAu, timeout=self.timeout),\
               "AU never repaired."
        log.info("AU successfully repaired.")

        # Assert that the repair came from a cache, not from the publisher.
        assert client.isContentRepairedFromCache(simAu)
        
        # End of test.

## Test repairs from a cache.  Create a small AU, damage it, and wait
## for it to be repaird, just like in SimpleDamageTestCase.  Extends
## 'LockssTestCase' instead of 'LockssAutoStartTestCase' so we can
## control adding extra parameters forcing repair from caches to the
## daemon to be damaged before starting up the test framework.
        
class SimpleDamageRepairFromCacheTestCase(LockssTestCase):
    """ Test repairing simple damage from another cache. """
    def setUp(self):
        LockssTestCase.setUp(self)
        self.damagedClient = self.clients[1]

        ## Configure one daemon to repair from a cache instead of from
        ##the publisher.  This is the daemon that will have damaged
        ##content.

        extraConf = {"org.lockss.crawler.repair.repair_from_cache_percent": "100",
                     "org.lockss.crawler.repair_from_cache_addr": "127.0.0.1"}
        self.framework.appendLocalConfig(extraConf, self.damagedClient)
        
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
        simAu = SimulatedAu('simContent', 0, 0, 1)

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

        # Damage the appropriate client, the one that was set up with
        # 'repair_from_cache_percent=1.0'
        client = self.damagedClient

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
        # Expect that the repair came from a cache, not from the publisher.
        assert client.isContentRepairedFromCache(simAu, node)

        log.info("Node %s repaired." % node.url)

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
        self.tinyUiClient.waitForCanConnectToHost(sleep=2)
            
    def getDaemonCount(self):
        return 1
    
    def getConfigUrls(self):
        return (self.getTestUrl(),)

    def runTest(self):
        tinyui = self.tinyUiClient.getAdminUi()
        html = tinyui.read()
        p = re.compile('This LOCKSS cache has not started because it is unable to load configuration data', re.MULTILINE | re.DOTALL);
        self.assertMatch(p, html)
        p = re.compile("Shouldn't happen", re.MULTILINE | re.DOTALL | re.I);
        self.assertNoMatch(p, html)
        exp = self.expectedPattern()
        p = re.compile(exp, re.MULTILINE | re.DOTALL);
        self.assertMatch(p, html)
        log.info('Found "%s"' % p.pattern)

    def assertMatch(self, pat, string):
        msg = 'No match for "%s" in\n%s' % (pat.pattern, string)
        assert pat.search(string), msg

    def assertNoMatch(self, pat, string):
        msg = 'Unexpected match for "%s"' % (pat.pattern)
        assert not pat.search(string), msg


class TinyUiUnknownHostTestCase(TinyUiTests):
    """ Test that config URL with unknown host name gets Tiny UI """
    def getTestUrl(self):
        return "http://unknownhost.lockss.org/"

    def expectedPattern(self):
        return 'UnknownHostException.*unknownhost\.lockss\.org'
        
class TinyUiMalformedUrlTestCase(TinyUiTests):
    """ Test that malformed config URL gets Tiny UI """
    def getTestUrl(self):
        return "http://x.y:12:13/"

    def expectedPattern(self):
        return 'MalformedURLException'
        
class TinyUiForbiddenTestCase(TinyUiTests):
    """ Test that a forbidden config fetch gets Tiny UI """
    def getTestUrl(self):
        return "http://props.lockss.org:8001/forbidden/"

    def expectedPattern(self):
        return '403: Forbidden'
        
# XXX should find a guaranteed non-listening port (by binding?)
class TinyUiRefusedTestCase(TinyUiTests):
    """ Test that a refused config connect gets Tiny UI """
    def getTestUrl(self):
        return "http://127.0.0.1:65027/"

    def expectedPattern(self):
        return 'ConnectException:.*Connection refused'
        
class TinyUiFileNotFoundTestCase(TinyUiTests):
    """ Test a config file not found gets Tiny UI """
    def getTestUrl(self):
        return "/no/such/file/or/directory"

    def expectedPattern(self):
        return 'FileNotFoundException'
        
class SimpleV3PollTestCase(LockssTestCase):
    """ Test a basic V3 Poll. """
    def getDaemonCount(self):
        return 5

    def setUp(self):
        LockssTestCase.setUp(self)
        baseV3Port = 8801

        for i in range(0, len(self.clients)):

            # Generate an initial list of peers, minus ourselves
            # (For now, V3 peers can't participate in polls that they
            # call.  This must be fixed before release.

            peerIds = []
            for port in range(0, len(self.clients)):
                if (port == (i)):
                    continue
                peerIds.append("tcp:[127.0.0.1]:%d" % (baseV3Port + port))

            extraConf = {"org.lockss.auconfig.allowEditDefaultOnlyParams": "true",
                         "org.lockss.comm.enabled": "false",
                         "org.lockss.scomm.enabled": "true",
                         "org.lockss.scomm.maxMessageSize": "1048576",  # 1MB
                         "org.lockss.poll.v3.quorum": "3",
                         "org.lockss.poll.v3.minPollSize": "4",
                         "org.lockss.poll.v3.maxPollSize": "4",
                         "org.lockss.poll.v3.minNominationSize": "1",
                         "org.lockss.poll.v3.maxNominationSize": "1",
                         "org.lockss.poll.v3.minPollDuration": "5m",
                         "org.lockss.poll.v3.maxPollDuration": "6m",
                         "org.lockss.localV3Identity": "tcp:[127.0.0.1]:%d" % (baseV3Port + i),
                         "org.lockss.id.initialV3PeerList": (",".join(peerIds))}

            self.framework.appendLocalConfig(extraConf, self.clients[i])
        
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
        # Reasonably complex AU for testing
        simAu = SimulatedAu('simContent', depth=0, branch=0,
                            numFiles=15, fileTypes=17,
                            binFileSize=1048576, protocolVersion=3)

        ##
        ## Create simulated AUs
        ##
        log.info("Creating V3 simulated AUs.")
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

        client = self.clients[2]
        
	##
	## Damage the AU.
	##
	# node = client.randomDamageSingleNode(simAu)
	# log.info("Damaged node %s" % node)

        # Request a tree walk (deactivate and reactivate AU)
        log.info("Requesting tree walk.")
        client.requestTreeWalk(simAu)

        log.info("Waiting for a V3 poll to be called...")
        client.waitForV3Poller(simAu)

        log.info("Successfully called a V3 poll.")
        
        ## Just pause until we have better tests.

        log.info("Pausing for 20 minutes.  ^C to quit the test.")
        time.sleep(20 * 60)
        

    
###########################################################################
### Functions that build and return test suites.  These can be
### called by name when running this test script.
###########################################################################

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

def totalLossRecoveryTests():
    suite = unittest.TestSuite()
    suite.addTest(TotalLossRecoveryTestCase())
    return suite

def tinyUiTests():
    suite = unittest.TestSuite()
    suite.addTest(TinyUiUnknownHostTestCase())
    suite.addTest(TinyUiMalformedUrlTestCase())
    suite.addTest(TinyUiForbiddenTestCase())
    suite.addTest(TinyUiRefusedTestCase())
    suite.addTest(TinyUiFileNotFoundTestCase())
    return suite

def v3Tests():
    suite = unittest.TestSuite()
    suite.addTest(SimpleV3PollTestCase())
    return suite

##
## Ignore these... mainly used for testing this framework.
##

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

##
## Tests to be run after tagging.
##
def postTagTests():
    suite = unittest.TestSuite()
    suite.addTest(simpleTests())
    suite.addTest(rangedTests())
    suite.addTest(randomTests())
    suite.addTest(tinyUiTests())
    suite.addTest(repairFromCacheTests())
    suite.addTest(totalLossRecoveryTests())
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

