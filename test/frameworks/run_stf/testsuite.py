#!/usr/bin/python
import sys, time, unittest, os
from os import path
from lockss_daemon import *

##########################################################################
# Module documentation
##########################################################################
"""
This test suite requires at minimum a top-level work directory to
build frameworks in.  Optional parameters may also be set, if desired,
to change the default behavior.  Below is an example config file, by
default 'testsuite.props' in the current working directory:

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

  # If true, delete framework directories after a test completes
  # successfully (no errors).  Default is True, files will be deleted
  # after a successful test.
  deleteAfterSuccess = true

  # Override the default 'timeout' value when sitting in wait
  # loops.  Default is 45 minutes.
  timeout = 3600

"""

###########################################################################
## Test cases.  Add test cases here, as well as to the TestSuite object
## created in the main method below.
##
## When you add a test, make sure to add its name to the list "testCases".
###########################################################################

# Global constants
propFile = './testsuite.props'
curFramework = None
config = loadConfig(propFile)

##
## Super class for all LOCKSS daemon test cases.
##
class LockssTestCase(unittest.TestCase):
    def __init__(self):
        unittest.TestCase.__init__(self)

        self.deleteAfterSuccess = config.get('deleteAfterSuccess', False)
        self.timeout = int(config.get('timeout', 60 * 45))


        ##
        ## assert that the workDir exists and is writable.
        ##
        self.workDir = config.get('workDir', './')
        if not (path.isdir(self.workDir) and \
                os.access(self.workDir, os.W_OK)):
            raise LockssError("Work dir %s does not exist or is not writable." \
                              % self.workDir)

    def setUp(self):
        ##
        ## Create a framework for the test.
        ##
        self.framework = Framework(config)

        ## global ('static') reference to the current framework, so we
        ## can clean up after a user interruption
        global curFramework
        curFramework = self.framework

        ##
        ## Start the framework.
        ##
        log("Starting framework in %s" % self.framework.frameworkDir)
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

        unittest.TestCase.setUp(self)


    def tearDown(self):
        self.framework.stop()
        self.failIf(self.framework.isRunning,
                    'Framework did not stop.')
        if self.deleteAfterSuccess and not self.failureException:
            self.framework.clean()

        unittest.TestCase.tearDown(self)


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

        node = client.randomDamageSingleNode(simAu)
        log("Damaged node %s on client %s" % (node.url, client))

        # Request a tree walk (deactivate and reactivate AU)
        client.requestTreeWalk(simAu)

        # expect to see a top level content poll
        log("Waiting for top level content poll.")
        assert client.waitForTopLevelContentPoll(simAu, timeout=self.timeout),\
               "Never called top level content poll"
        log("Called top level content poll.")

        # expect to see the top level node marked repairing.
        log("Waiting for lockssau to be marked 'damaged'.")
        assert client.waitForTopLevelDamage(simAu, timeout=self.timeout),\
               "Client never marked lockssau 'damaged'."
        log("Marked lockssau 'damaged'.")

        # expect to see top level name poll
        log("Waiting for top level name poll.")
        assert client.waitForTopLevelNamePoll(simAu, timeout=self.timeout),\
               "Never called top level name poll"
        log("Called top level name poll")

        # expect to see the specific node marked 'damaged'
        log("Waiting for node %s to be marked 'damaged'." % node.url)
        assert client.waitForDamage(simAu, node, timeout=self.timeout),\
               "Never marked node %s 'damaged'" % node.url
        log("Marked node %s 'damaged'" % node.url)

        # expect to see the AU successfully repaired.
        log("Waiting for successful repair of node %s." % node.url)
        assert client.waitForContentRepair(simAu, node, timeout=self.timeout),\
               "Node %s not repaired." % node.url
        log("Node %s repaired." % node.url)


##
## Ensure the cache can recover from a simple file deletion.
## (not resulting in a ranged name poll)
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
        log("Deleted node %s on client %s" % (node.url, client))

        # Request a tree walk (deactivate and reactivate AU)
        client.requestTreeWalk(simAu)

        # expect to see a top level content poll
        log("Waiting for top level content poll.")
        assert client.waitForTopLevelContentPoll(simAu, timeout=self.timeout),\
               "Never called top level content poll"
        log("Called top level content poll.")

        # expect to see the top level node marked repairing.
        log("Waiting for lockssau to be marked 'damaged'.")
        assert client.waitForTopLevelDamage(simAu, timeout=self.timeout),\
               "Client never marked lockssau 'damaged'."
        log("Marked lockssau 'damaged'.")

        # expect to see top level name poll
        log("Waiting for top level name poll.")
        assert client.waitForTopLevelNamePoll(simAu, timeout=self.timeout),\
               "Never called top level name poll"
        log("Called top level name poll")

        # expect to see the AU successfully repaired.
        log("Waiting for repair.")
        assert client.waitForNameRepair(simAu, timeout=self.timeout),\
               "Au not repaired."
        log("AU repaired.")

##
## Ensure that the cache can recover following an extra file created
## (not resulting in a ranged name poll)
##
class SimpleExtraFileTestCase(LockssTestCase):
    def runTest(self):
        "Test recovery from an extra node in our cache"

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

        node = client.createNode(simAu, 'extrafile.txt')
        log("Created file %s on client %s" % (node.url, client))

        # Request a tree walk (deactivate and reactivate AU)
        client.requestTreeWalk(simAu)

        # expect to see a top level content poll
        log("Waiting for top level content poll.")
        assert client.waitForTopLevelContentPoll(simAu, timeout=self.timeout),\
               "Never called top level content poll"
        log("Called top level content poll.")

        # expect to see the top level node marked repairing.
        log("Waiting for lockssau to be marked 'damaged'.")
        assert client.waitForTopLevelDamage(simAu, timeout=self.timeout),\
               "Client never marked lockssau 'damaged'."
        log("Marked lockssau 'damaged'.")

        # expect to see top level name poll
        log("Waiting for top level name poll.")
        assert client.waitForTopLevelNamePoll(simAu, timeout=self.timeout),\
               "Never called top level name poll"
        log("Called top level name poll")

        #
        # TODO: Expect to see the node removed from the node list.
        #

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
        log("Deleted node %s on client %s" % (node.url, client))

        # Request a tree walk (deactivate and reactivate AU)
        client.requestTreeWalk(simAu)

        # expect to see a top level content poll
        log("Waiting for top level content poll.")
        assert client.waitForTopLevelContentPoll(simAu, timeout=self.timeout),\
               "Never called top level content poll"
        log("Called top level content poll.")

        # expect to see the top level node marked damaged.
        log("Waiting for lockssau to be marked 'damaged'.")
        assert client.waitForTopLevelDamage(simAu, timeout=self.timeout),\
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


##
## Ensure that the cache can recover following an extra file being
## added to an AU large enough to trigger a ranged name poll.
##
class RangedNamePollExtraFileTestCase(LockssTestCase):
    def runTest(self):
        "Test recovery from an extra file that triggers a ranged name poll"

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

        # Create a file that doesn't exist
        filename = '046extrafile.txt'
        node = client.createNode(simAu, filename)
        log("Created file %s on client %s" % (node.url, client))

        # Request a tree walk (deactivate and reactivate AU)
        client.requestTreeWalk(simAu)

        # expect to see a top level content poll
        log("Waiting for top level content poll.")
        assert client.waitForTopLevelContentPoll(simAu, timeout=self.timeout),\
               "Never called top level content poll"
        log("Called top level content poll.")

        # expect to see the top level node marked damaged.
        log("Waiting for lockssau to be marked 'damaged'.")
        assert client.waitForTopLevelDamage(simAu, timeout=self.timeout),\
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

        # expect to call a name poll on base URL.
        baseUrlNode = client.getAuNode(simAu, simAu.baseUrl)
        log("Waiting to call name poll on %s" % baseUrlNode.url)
        assert client.waitForNamePoll(simAu, baseUrlNode, timeout=self.timeout),\
               "Never called name poll on %s" % baseUrlNode.url
        log("Called name poll on %s" % node.url)

        # expect to lose a name poll on baes URL.
        log("Waiting to lose name poll on %s" % baseUrlNode.url)
        assert client.waitForLostNamePoll(simAu, baseUrlNode, timeout=self.timeout),\
               "Never lost name poll on %s" % baseUrlNode.url
        log("Lost name poll on %s" % baseUrlNode.url)

        # expect to call a ranged name poll on base URL.
        node = client.getAuNode(simAu, simAu.baseUrl)
        log("Waiting to call ranged name poll on %s" % node.url)
        assert client.waitForRangedNamePoll(simAu, node, timeout=self.timeout),\
               "Never called ranged name poll on %s" % node.url
        log("Called ranged name poll on %s" % node.url)

        #
        # TODO: Expect to see the node removed from the node list.
        #

        # (Note: It turns that waiting for the ranged name poll to
        # fail is not the right thing to do here -- it may go from
        # 'Active' to 'Repaired' before the timer has a chance to
        # catch it.  So just go straight to expecting 'Repaired'

        # expect to see the AU successfully repaired.
        log("Waiting for successful repair.")
        assert client.waitForRangedNameRepair(simAu, timeout=self.timeout),\
               "AU not repaired by ranged name poll."
        log("AU repaired by ranged name poll.")

##
## Create a randomly sized AU (with reasonable limits on maximum depth
## and breadth of the tree structure), and cause damage.  Wait for
## all damaged nodes to be repaired.
##

class RandomizedDamageTestCase(LockssTestCase):
    def runTest(self):
        "Test recovery from random file damage in a randomly sized AU."

        depth = random.randint(0, 3)
        branch = random.randint(0, 3)
        numFiles = random.randint(0, 50)
        maxFileName = 26
        log("Creating simulated AUs: depth = %s; branch = %s; "
            "numFiles = %s; maxFileName = %s" %
            (depth, branch, numFiles, maxFileName))

        ## Pick a client at random
        client = self.clients[random.randint(0, len(self.clients) - 1)]

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
        log("Waiting for simulated AUs to crawl.")
        for c in self.clients:
            if not (c.waitForSuccessfulCrawl(simAu)):
                self.fail("AUs never completed initial crawl.")
        log("AUs completed initial crawl.")

        ## To damage one file, use randomDamageSingleNode(simAu)
        log("Causing random damage...")
        nodeList = client.randomDamageRandomNodes(simAu)

        log("Damaged the following nodes on client %s:\n        %s" %
            (client, '\n        '.join([str(n) for n in nodeList])))

        # Request a tree walk (deactivate and reactivate AU)
        client.requestTreeWalk(simAu)

        # expect to see a top level content poll
        log("Waiting for top level content poll.")
        assert client.waitForTopLevelContentPoll(simAu, timeout=self.timeout),\
               "Never called top level content poll"
        log("Called top level content poll.")

        # expect to see the top level node marked repairing.
        log("Waiting for lockssau to be marked 'damaged'.")
        assert client.waitForTopLevelDamage(simAu, timeout=self.timeout),\
               "Client never marked lockssau 'damaged'."
        log("Marked lockssau 'damaged'.")

        # expect to see top level name poll
        log("Waiting for top level name poll.")
        assert client.waitForTopLevelNamePoll(simAu, timeout=self.timeout),\
               "Never called top level name poll"
        log("Called top level name poll")

        # TODO:
        # Need methods that watch a LIST of nodes, not just one
        # node.  For now, this is good enough, but improve
        # as soon as possible.
        node = nodeList[random.randint(0, len(nodeList) - 1)]

        # expect to see a specific node marked 'damaged'
        log("Waiting for node %s to be marked 'damaged'." % node.url)
        assert client.waitForDamage(simAu, node, timeout=self.timeout),\
               "Never marked node %s 'damaged'" % node.url
        log("Marked node %s 'damaged'" % node.url)

        # expect to see the node successfully repaired.
        log("Waiting for successful repair of node %s." % node.url)
        assert client.waitForContentRepair(simAu, node, timeout=self.timeout),\
               "Node %s not repaired." % node.url
        log("Node %s repaired." % node.url)

        # expect to see the AU successfully repaired.
        log("Waiting for successful repair of AU.")
        assert client.waitForTopLevelRepair(simAu, timeout=self.timeout),\
               "AU never repaired."
        log("AU repaired.")

##
## Create a randomly sized AU (with reasonable limits on maximum depth
## and breadth of the tree structure), and randomly remove files.  Wait
## for a successful repair
##

class RandomizedDeleteTestCase(LockssTestCase):
    def runTest(self):
        "Test recovery from random file deletion in a randomly sized AU."

        depth = random.randint(0, 3)
        branch = random.randint(0, 3)
        numFiles = random.randint(0, 50)
        maxFileName = 26
        log("Creating simulated AUs: depth = %s; branch = %s; "
            "numFiles = %s; maxFileName = %s" %
            (depth, branch, numFiles, maxFileName))

        ## Pick a client at random
        client = self.clients[random.randint(0, len(self.clients) - 1)]

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
        log("Waiting for simulated AUs to crawl.")
        for c in self.clients:
            if not (c.waitForSuccessfulCrawl(simAu)):
                self.fail("AUs never completed initial crawl.")
        log("AUs completed initial crawl.")

        log("Deleting random files...")
        nodeList = client.randomDeleteRandomNodes(simAu)

        log("Deleted the following nodes on client %s:\n        %s" %
            (client, '\n        '.join([str(n) for n in nodeList])))

        # Request a tree walk (deactivate and reactivate AU)
        client.requestTreeWalk(simAu)

        # expect to see a top level content poll
        log("Waiting for top level content poll.")
        assert client.waitForTopLevelContentPoll(simAu, timeout=self.timeout),\
               "Never called top level content poll"
        log("Called top level content poll.")

        # expect to see the top level node marked repairing.
        log("Waiting for lockssau to be marked 'damaged'.")
        assert client.waitForTopLevelDamage(simAu, timeout=self.timeout),\
               "Client never marked lockssau 'damaged'."
        log("Marked lockssau 'damaged'.")

        # expect to see top level name poll
        log("Waiting for top level name poll.")
        assert client.waitForTopLevelNamePoll(simAu, timeout=self.timeout),\
               "Never called top level name poll"
        log("Called top level name poll")

        # TODO: Definitely want more steps between these two.  Might
        # want to branch on whether it causes a ranged name poll or
        # not, too.

        # expect to see the AU successfully repaired.
        log("Waiting for repair.")
        assert client.waitForTopLevelRepair(simAu, timeout=self.timeout),\
               "AU never repaired."
        log("AU repaired.")

##
## Create a randomly sized AU (with reasonable limits on maximum depth
## and breadth of the tree structure), and add extra files.  Wait for
## all damaged nodes to be repaired.
##
class RandomizedExtraFileTestCase(LockssTestCase):
    def runTest(self):
        """ Not yet implemented... """
        pass


###########################################################################
### Main entry point for this test suite.
###########################################################################

def all():
    suite = unittest.TestSuite()
    suite.add(simpleTests())
    suite.add(rangedTests())
    suite.add(randomTests())
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

if __name__ == "__main__":
    try:
        unittest.main()
    except Exception, e:
        print "%s" % e
        if curFramework and curFramework.isRunning:
            curFramework.stop()
