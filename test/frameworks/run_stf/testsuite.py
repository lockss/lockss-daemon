#!/usr/bin/env python
"""This test suite requires at minimum a top-level work directory in which to
build frameworks.  If desired, optional parameters may also be set to change
the default behavior.  See testsuite.props for details."""

import filecmp
import os
import re
import sys
import time
import unittest
import urllib2

sys.path.append( os.path.normpath( os.path.join( os.path.dirname( sys.argv[ 0 ] ), '../lib' ) ) )
from lockss_util import *

##
## Load configuration.
##
loadConfig('testsuite.props')
if os.path.isfile('testsuite.opt'):
    loadConfig('testsuite.opt')

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
    """Superclass for all STF test cases."""
    def __init__(self, methodName='runTest'):
        unittest.TestCase.__init__(self, methodName)

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

    def getStartUiPort(self):
        return None

    def setUp(self):
        unittest.TestCase.setUp(self)

        ## Log start of test.
        log.info("==========================================================")
        log.info(self.__doc__)
        log.info("----------------------------------------------------------")

        ##
        ## Create a framework for the test.
        ##
        self.framework = Framework(self.getDaemonCount(),
                                   self.getConfigUrls(),
                                   self.getStartUiPort())

        ## global ('static') reference to the current framework, so we
        ## can clean up after a user interruption
        global frameworkList
        frameworkList.append(self.framework)

        ##
        ## List of clients, one for each daemon
        ##
        self.clients = self.framework.clientList

    def tearDown(self):
        # dump threads and look for deadlocks.  This will happen
        # whether or not there was a failure.
        deadlockLogs = self.framework.checkForDeadlock()
        if deadlockLogs:
            log.error("Deadlocks detected!")
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
    """Extension of LockssTestCase that automatically starts the
    framework in the setUp method.  Typically, you should extend this
    class to create a new method unless you want to have more control
    over when the framework starts up."""

    def setUp(self):
        LockssTestCase.setUp(self)

        ##
        ## Start the framework.
        ##
        log.info("Starting framework in %s" % self.framework.frameworkDir)
        self.framework.start()
        assert self.framework.isRunning, 'Framework failed to start.'

        # Block return until all clients are ready to go.
        log.info("Waiting for framework to become ready.")
        self.framework.waitForFrameworkReady()


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

    def getStartUiPort(self):
        return 8081

    def runTest(self):
        tinyui = self.tinyUiClient.getAdminUi()
        html = tinyui.read()
        p = re.compile('This LOCKSS box \(.*\) has not started because it is unable '
                       'to load configuration data', re.MULTILINE | re.DOTALL);
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
    """Test that config URL with unknown host name gets Tiny UI"""
    def getTestUrl(self):
        return "http://unknownhost.lockss.org/"

    def expectedPattern(self):
        return 'UnknownHostException.*unknownhost\.lockss\.org'

class TinyUiMalformedUrlTestCase(TinyUiTests):
    """Test that malformed config URL gets Tiny UI"""
    def getTestUrl(self):
        return "http://x.y:12:13/"

    def expectedPattern(self):
        return 'MalformedURLException'

# The forbidden test relies on the URL returning a 403, with a specially
# crafted body containing hint text.  See HTTPConfigFile.java and
# http://props.lockss.org:8001/daemon/README

class TinyUiForbiddenTestCase(TinyUiTests):
    """Test that a forbidden config fetch gets Tiny UI with the proper hint"""
    def getTestUrl(self):
        return "http://props.lockss.org:8001/daemon/forbidden.xml"

    def expectedPattern(self):
        return '403: Forbidden.*LOCKSS team.*access list'

# XXX should find a guaranteed non-listening port (by binding?)
class TinyUiRefusedTestCase(TinyUiTests):
    """Test that a refused config connect gets Tiny UI"""
    def getTestUrl(self):
        return "http://127.0.0.1:65027/"

    def expectedPattern(self):
        return 'ConnectException:.*Connection refused'

class TinyUiFileNotFoundTestCase(TinyUiTests):
    """Test a config file not found gets Tiny UI"""
    def getTestUrl(self):
        return "/no/such/file/or/directory"

    def expectedPattern(self):
        return 'FileNotFoundException'


class V3TestCase(LockssTestCase):
    def setUp(self):
        LockssTestCase.setUp(self)
        # V3 has a much shorter default timeout, 8 minutes.
        self.timeout = int(config.get('timeout', 60 * 8))
        self.victim = self.clients[0]
        if len(self.clients) > 1:
            self.nonVictim = self.clients[1]

        for i in range(0, len(self.clients)):
            c = self.clients[i]
            isVictim = i == 0
            extraConf = {"org.lockss.auconfig.allowEditDefaultOnlyParams": "true",
                         "org.lockss.localV3Identity": c.getPeerId(),
                         "org.lockss.id.initialV3PeerList": self.getInitialPeerList(),
                         "org.lockss.poll.v3.enableV3Poller": isVictim,
                         "org.lockss.poll.v3.enableV3Voter": "true"
                         }
            extraConf.update(self.getTestLocalConf())
            self.framework.appendLocalConfig(extraConf, self.clients[i])

        ##
        ## Start the framework.
        ##
        log.info("Starting framework in %s" % self.framework.frameworkDir)
        self.framework.start()
        assert self.framework.isRunning, 'Framework failed to start.'

        # Block return until all clients are ready to go.
        log.info("Waiting for framework to come ready.")
        self.framework.waitForFrameworkReady()

    def getDaemonCount(self):
        return 5

    def getInitialPeerList(self):
        peerIds = []
        for c in self.clients:
            peerIds.append(c.getPeerId())
        return ";".join(peerIds)
        
    def getTestLocalConf(self):
        "Override this method to append local per-test configuration"
        return {}
    
    def createAus(self, au):
        log.info("Creating simulated AUs.")
        for c in self.clients:
            c.createAu(au)
        for c in self.clients:
            c.waitAu(au)

    def crawlAus(self, au):
        log.info("Waiting for simulated AUs to crawl.")
        for c in self.clients:
            if not (c.waitForSuccessfulCrawl(au)):
                self.fail("AUs never completed initial crawl.")
        log.info("AUs completed initial crawl.")

    def compareNode(self, node, au, victim, nonVictim):
        url = node.url
        n1 = victim.getAuNode(au, url)
        n2 = nonVictim.getAuNode(au, url)
        f1 = victim.nodeContentFile(n1)
        f2 = nonVictim.nodeContentFile(n2)
        isSame = filecmp.cmp(f1, f2, False)
        return isSame

    def nodeHasContent(self, node, victim):
        file = victim.nodeContentFile(node)
        return path.isfile(file)

class SimpleDamageV3TestCase(V3TestCase):
    """Test a basic V3 Poll."""
    def runTest(self):
        # Reasonably complex AU for testing.
        simAu = SimulatedAu('simContent', depth=0, branch=0,
                            numFiles=15,
                            fileTypes=[FILE_TYPE_TEXT, FILE_TYPE_BIN],
                            binFileSize=1024, protocolVersion=3)

        ##
        ## Create simulated AUs
        ##
        self.createAus(simAu)

        ##
        ## Assert that the AUs have been crawled.
        ##
        self.crawlAus(simAu)

        victim = self.victim

        ##
        ## Damage the AU.
        ##
        node = victim.randomDamageSingleNode(simAu)

        assert not self.compareNode(node, simAu, victim, self.nonVictim), "Failed to damage AU."
        log.info("Damaged node %s on client %s" % (node.url, victim))

        log.info("Waiting for a V3 poll to be called...")
        victim.waitForV3Poller(simAu)

        log.info("Successfully called a V3 poll.")

        ## Just pause until we have better tests.
        log.info("Waiting for V3 repair...")
        # waitForV3Repair takes a list of nodes
        victim.waitForV3Repair(simAu, [node], timeout=self.timeout)
        assert self.compareNode(node, simAu, victim, self.nonVictim), "File wasn't repaired: %s % node.url"
        log.info("AU successfully repaired.")


class RandomDamageV3TestCase(V3TestCase):
    """Test a V3 Poll with a random size and number of damaged AUs"""
    def runTest(self):
        # Reasonably complex AU for testing.
        simAu = SimulatedAu('simContent', depth=1, branch=1,
                            numFiles=30,
                            fileTypes=[FILE_TYPE_TEXT, FILE_TYPE_BIN],
                            binFileSize=2024, protocolVersion=3)

        ##
        ## Create simulated AUs
        ##
        self.createAus(simAu)

        ##
        ## Assert that the AUs have been crawled.
        ##
        self.crawlAus(simAu)

        victim = self.victim

        ##
        ## Damage the AU.
        ##
        nodeList = victim.randomDamageRandomNodes(simAu, 30, 50)
        log.info("Damaged the following nodes on client %s:\n        %s" %
            (victim, '\n        '.join([str(n) for n in nodeList])))

        log.info("Waiting for a V3 poll to be called...")
        victim.waitForV3Poller(simAu)

        log.info("Successfully called a V3 poll.")

        ## Just pause until we have better tests.
        log.info("Waiting for V3 repair...")
        # waitForV3Repair takes a list of nodes
        victim.waitForV3Repair(simAu, nodeList, timeout=self.timeout)
        for node in nodeList:
            assert self.compareNode(node, simAu, victim, self.nonVictim), "File wasn't repaired: %s % node.url"

        log.info("AU successfully repaired.")


class RepairFromPublisherV3TestCase(V3TestCase):
    """Ensure that repair from pubilsher works correctly in V3."""
    def getTestLocalConf(self):
        # NEVER repair from a cache
        return {"org.lockss.poll.v3.repairFromCachePercent": "0"}
        
    def runTest(self):
        # Reasonably complex AU for testing.
        simAu = SimulatedAu('simContent', depth=1, branch=1,
                            numFiles=10,
                            fileTypes=[FILE_TYPE_TEXT, FILE_TYPE_BIN],
                            binFileSize=1024, protocolVersion=3)

        ##
        ## Create simulated AUs
        ##
        self.createAus(simAu)

        ##
        ## Assert that the AUs have been crawled.
        ##
        self.crawlAus(simAu)

        victim = self.victim

        ##
        ## Damage the AU.
        ##
        nodeList = victim.randomDamageRandomNodes(simAu, 15, 20)
        log.info("Damaged the following nodes on client %s:\n        %s" %
            (victim, '\n        '.join([str(n) for n in nodeList])))

        log.info("Waiting for a V3 poll to be called...")
        victim.waitForV3Poller(simAu)

        log.info("Successfully called a V3 poll.")

        ## Just pause until we have better tests.
        log.info("Waiting for V3 repair...")
        # waitForV3Repair takes a list of nodes
        victim.waitForV3Repair(simAu, nodeList, timeout=self.timeout)

        ## Verify that all repairs came from peers.
        for node in nodeList:
            if not (victim.isNodeRepairedFromPublisherByV3(simAu, node)):
                self.fail("Node %s was not repaired from the publisher!" % node)
            assert self.compareNode(node, simAu, victim, self.nonVictim), "File wasn't repaired: %s % node.url"

        log.info("AU successfully repaired.")


class RepairFromPeerV3TestCase(V3TestCase):
    """Ensure that repairing from a V3 peer works correctly."""
    def getTestLocalConf(self):
        # ALWAYS repair from a cache
        ## Enable polling on all peers.
        return {"org.lockss.poll.v3.repairFromCachePercent":"100",
                "org.lockss.poll.v3.enableV3Poller":"true",
                "org.lockss.poll.v3.toplevelPollInterval":"10",
                "org.lockss.poll.minPollAttemptInterval":"10"
                }

    def runTest(self):
        # Reasonably complex AU for testing.
        simAu = SimulatedAu('simContent', depth=1, branch=1,
                            numFiles=10,
                            fileTypes=[FILE_TYPE_TEXT, FILE_TYPE_BIN],
                            binFileSize=1024, protocolVersion=3)
        
        ##
        ## Create simulated AUs
        ##
        self.createAus(simAu)

        ##
        ## Assert that the AUs have been crawled.
        ##
        self.crawlAus(simAu)

        victim = self.victim

        #
        # We need agreement from all the peers before we can continue.
        #

        # expect to see a top level content poll called by all peers.
        log.info("Waiting for a V3 poll by all simulated caches")
        for c in self.clients:
            assert c.waitForV3Poller(simAu), "Never called V3 poll."
            log.info("Client on port %s called V3 poll..." % c.port)

        # expect that each client will have won a top-level v3 poll
        log.info("Waiting for all peers to win their polls")
        for c in self.clients:
            assert c.waitForWonV3Poll(simAu, timeout=self.timeout),\
                   ("Client on port %s never won V3 poll" % c.port)
            log.info("Client on port %s won V3 poll..." % c.port)

        ##
        ## Damage the AU.
        ##
        nodeList = victim.randomDamageRandomNodes(simAu, 15, 20)
        log.info("Damaged the following nodes on client %s:\n        %s" %
            (victim, '\n        '.join([str(n) for n in nodeList])))

        ## XXX - this sees first poll, doesn't wait for second
        log.info("Waiting for a V3 poll to be called...")
        victim.waitForV3Poller(simAu)

        log.info("Successfully called a V3 poll.")

        ## Just pause until we have better tests.
        log.info("Waiting for V3 repair...")
        # waitForV3Repair takes a list of nodes
        victim.waitForV3Repair(simAu, nodeList, timeout=self.timeout)
        
        ## Verify that all repairs came from peers.
        for node in nodeList:
            if not (victim.isNodeRepairedFromPeerByV3(simAu, node)):
                self.fail("Node %s was not repaired from a peer!" % node)
            assert self.compareNode(node, simAu, victim, self.nonVictim), "File wasn't repaired: %s % node.url"

        log.info("AU successfully repaired.")


class SimpleDeleteV3TestCase(V3TestCase):
    """Test repair of a missing file."""
    def runTest(self):
        # Reasonably complex AU for testing.
        simAu = SimulatedAu('simContent', depth=0, branch=0,
                            numFiles=15,
                            fileTypes=[FILE_TYPE_TEXT, FILE_TYPE_BIN],
                            binFileSize=1024, protocolVersion=3)
        ##
        ## Create simulated AUs
        ##
        self.createAus(simAu)

        ##
        ## Assert that the AUs have been crawled.
        ##
        self.crawlAus(simAu)

        victim = self.victim

        ##
        ## Damage the AU.
        ##
        node = victim.randomDelete(simAu)
        log.info("Deleted node %s on client %s" % (node.url, victim))
        
        log.info("Waiting for a V3 poll to be called...")
        victim.waitForV3Poller(simAu)

        log.info("Successfully called a V3 poll.")

        ## Just pause until we have better tests.
        log.info("Waiting for V3 repair...")
        # waitForV3Repair takes a list of nodes
        victim.waitForV3Repair(simAu, [node], timeout=self.timeout)
        assert self.compareNode(node, simAu, victim, self.nonVictim), "File wasn't repaired: %s % node.url"
        log.info("AU successfully repaired.")


class LastFileDeleteV3TestCase(V3TestCase):
    " Ensure that the deletion of the last (alphabetically) file in the AU can be repaired. "
    def runTest(self):
        # Reasonably complex AU for testing.
        simAu = SimulatedAu('simContent', depth=0, branch=0,
                            numFiles=15,
                            fileTypes=[FILE_TYPE_TEXT, FILE_TYPE_BIN],
                            binFileSize=1024, protocolVersion=3)
        ##
        ## Create simulated AUs
        ##
        self.createAus(simAu)

        ##
        ## Assert that the AUs have been crawled.
        ##
        self.crawlAus(simAu)

        victim = self.victim

        ##
        ## Damage the AU.
        ##
        node = victim.getAuNode(simAu, "http://www.example.com/index.html")
        victim.deleteNode(node)
        log.info("Deleted node %s on client %s" % (node.url, victim))

        log.info("Waiting for a V3 poll to be called...")
        victim.waitForV3Poller(simAu)

        log.info("Successfully called a V3 poll.")

        ## Just pause until we have better tests.
        log.info("Waiting for V3 repair...")
        # waitForV3Repair takes a list of nodes
        victim.waitForV3Repair(simAu, [node], timeout=self.timeout)
        assert self.compareNode(node, simAu, victim, self.nonVictim), "File wasn't repaired: %s % node.url"
        log.info("AU successfully repaired.")


class RandomDeleteV3TestCase(V3TestCase):
    "Test recovery by V3 from randomly deleted nodes in our cache."
    def runTest(self):
        # Reasonably complex AU for testing.
        simAu = SimulatedAu('simContent', depth=1, branch=1,
                            numFiles=15,
                            fileTypes=[FILE_TYPE_TEXT, FILE_TYPE_BIN],
                            binFileSize=1024, protocolVersion=3)
        ##
        ## Create simulated AUs
        ##
        self.createAus(simAu)

        ##
        ## Assert that the AUs have been crawled.
        ##
        self.crawlAus(simAu)

        victim = self.victim

        ##
        ## Damage the AU.
        ##
        nodeList = victim.randomDeleteRandomNodes(simAu, 5, 15)
        for node in nodeList:
            assert not self.nodeHasContent(node, victim), "Failed to delete: %s % node.url"
        log.info("Damaged the following nodes on client %s:\n        %s" %
            (victim, '\n        '.join([str(n) for n in nodeList])))

        log.info("Waiting for a V3 poll to be called...")
        victim.waitForV3Poller(simAu)

        log.info("Successfully called a V3 poll.")

        log.info("Waiting for V3 repair...")
        # waitForV3Repair takes a list of nodes
        victim.waitForV3Repair(simAu, nodeList, timeout=self.timeout)
        for node in nodeList:
            assert self.compareNode(node, simAu, victim, self.nonVictim), "File wasn't repaired: %s % node.url"
        log.info("AU successfully repaired.")


class SimpleExtraFileV3TestCase(V3TestCase):
    "Test recovery by V3 from an extra node in our cache"
    def runTest(self):
        # Reasonably complex AU for testing
        simAu = SimulatedAu('simContent', depth=0, branch=0,
                            fileTypes=[FILE_TYPE_TEXT, FILE_TYPE_BIN],
                            binFileSize=1024,
                            numFiles=20, protocolVersion=3)
        ##
        ## Create simulated AUs
        ##
        self.createAus(simAu)

        ##
        ## Assert that the AUs have been crawled.
        ##
        self.crawlAus(simAu)

        victim = self.victim

        ##
        ## Damage the AU by creating an extra node.
        ##
        node = victim.createNode(simAu, '004extrafile.txt')
        log.info("Created file %s on client %s" % (node.url, victim))

        log.info("Waiting for a V3 poll to be called...")
        victim.waitForV3Poller(simAu)

        log.info("Successfully called a V3 poll.")

        ## Just pause until we have better tests.
        log.info("Waiting for V3 repair...")
        # waitForV3Repair takes a list of nodes
        victim.waitForV3RepairExtraFiles(simAu, timeout=self.timeout)
        assert not self.nodeHasContent(node, victim), "File wasn't deleted: %s % node.url"
        log.info("AU successfully repaired.")


class LastFileExtraV3TestCase(V3TestCase):
    "Test recovery by V3 from an extra last-file node in our cache"
    def runTest(self):
        # Reasonably complex AU for testing
        simAu = SimulatedAu('simContent', depth=0, branch=0,
                            fileTypes=[FILE_TYPE_TEXT, FILE_TYPE_BIN],
                            binFileSize=1024,
                            numFiles=20, protocolVersion=3)
        ##
        ## Create simulated AUs
        ##
        self.createAus(simAu)

        ##
        ## Assert that the AUs have been crawled.
        ##
        self.crawlAus(simAu)

        victim = self.victim

        ##
        ## Damage the AU by creating an extra node that should sort LAST
        ## in the list of CachedUrls..
        ##
        node = victim.createNode(simAu, 'zzzzzzzzzzzzz.txt')
        log.info("Created file %s on client %s" % (node.url, victim))

        log.info("Waiting for a V3 poll to be called...")
        victim.waitForV3Poller(simAu)

        log.info("Successfully called a V3 poll.")

        ## Just pause until we have better tests.
        log.info("Waiting for V3 repair...")
        # waitForV3Repair takes a list of nodes
        victim.waitForV3RepairExtraFiles(simAu, timeout=self.timeout)
        assert not self.nodeHasContent(node, victim), "File wasn't deleted: %s % node.url"
        log.info("AU successfully repaired.")


class RandomExtraFileV3TestCase(V3TestCase):
    "Test recovery by V3 from a random number of extra nodes in our cache"
    def runTest(self):
        # Reasonably complex AU for testing
        simAu = SimulatedAu('simContent', depth=1, branch=1,
                            fileTypes=[FILE_TYPE_TEXT, FILE_TYPE_BIN],
                            binFileSize=1024,
                            numFiles=20, protocolVersion=3)

        ##
        ## Create simulated AUs
        ##
        self.createAus(simAu)

        ##
        ## Assert that the AUs have been crawled.
        ##
        self.crawlAus(simAu)

        ## To use a specific client, uncomment this line.
        victim = self.victim

        ##
        ## Damage the AU by creating an extra node.
        ##
        nodeList = victim.randomCreateRandomNodes(simAu, 5, 15)
        log.info("Created the following nodes on client %s:\n        %s" %
                 (victim, '\n        '.join([str(n) for n in nodeList])))

        log.info("Waiting for a V3 poll to be called...")
        victim.waitForV3Poller(simAu)

        log.info("Successfully called a V3 poll.")

        ## Just pause until we have better tests.
        log.info("Waiting for V3 repair...")
        # waitForV3Repair takes a list of nodes
        victim.waitForV3RepairExtraFiles(simAu, timeout=self.timeout)
        for node in nodeList:
            assert not self.nodeHasContent(node, victim), "File wasn't deleted: %s % node.url"
        log.info("AU successfully repaired.")


class VotersDontParticipateV3TestCase(V3TestCase):
    """Test a V3 poll where some peers do not participate."""
    def getInitialPeerList(self):
        """Return the real participant list, plus some that do not really
        exist"""
        return "%s;TCP:[127.0.0.1]:65520;TCP:[127.0.0.1]:65521;TCP:[127.0.0.1]:65522" % V3TestCase.getInitialPeerList(self)
    
    def getTestLocalConf(self):
        return {"org.lockss.poll.v3.quorum": "3",
                "org.lockss.poll.v3.minPollSize": "8",
                "org.lockss.poll.v3.maxPollSize": "8"}

    def runTest(self):
        # Reasonably complex AU for testing
        simAu = SimulatedAu('simContent', depth=1, branch=1,
                            fileTypes=[FILE_TYPE_TEXT, FILE_TYPE_BIN],
                            binFileSize=1024,
                            numFiles=20, protocolVersion=3)

        ##
        ## Create simulated AUs
        ##
        self.createAus(simAu)

        ##
        ## Assert that the AUs have been crawled.
        ##
        self.crawlAus(simAu)

        ## To use a specific client, uncomment this line.
        victim = self.victim

        ##
        ## Damage the AU by creating an extra node.
        ##
        nodeList = victim.randomCreateRandomNodes(simAu, 5, 15)
        log.info("Created the following nodes on client %s:\n        %s" %
                 (victim, '\n        '.join([str(n) for n in nodeList])))

        log.info("Waiting for a V3 poll to be called...")
        victim.waitForV3Poller(simAu)

        log.info("Successfully called a V3 poll.")

        ## Just pause until we have better tests.
        log.info("Waiting for V3 repair...")
        # waitForV3Repair takes a list of nodes
        victim.waitForV3RepairExtraFiles(simAu, timeout=self.timeout)
        for node in nodeList:
            assert not self.nodeHasContent(node, victim), "File wasn't deleted: %s % node.url"
        log.info("AU successfully repaired.")
    

class NoQuorumV3TestCase(V3TestCase):
    """Be sure a V3 poll with too few participants ends in No Quorum"""
    def getInitialPeerList(self):
        """Return the real participant list, plus some that do not really
        exist"""
        return "%s;TCP:[127.0.0.1]:65520;TCP:[127.0.0.1]:65521;TCP:[127.0.0.1]:65522" % V3TestCase.getInitialPeerList(self)
    
    def getTestLocalConf(self):
        return {"org.lockss.poll.v3.quorum": "6",
                "org.lockss.poll.v3.minPollSize": "8",
                "org.lockss.poll.v3.maxPollSize": "8"}

    def runTest(self):
        # Reasonably complex AU for testing
        simAu = SimulatedAu('simContent', depth=1, branch=1,
                            fileTypes=[FILE_TYPE_TEXT, FILE_TYPE_BIN],
                            binFileSize=1024,
                            numFiles=20, protocolVersion=3)

        ##
        ## Create simulated AUs
        ##
        self.createAus(simAu)

        ##
        ## Assert that the AUs have been crawled.
        ##
        self.crawlAus(simAu)

        ## To use a specific client, uncomment this line.
        victim = self.victim

        ##
        ## Damage the AU by creating an extra node.
        ##
        nodeList = victim.randomCreateRandomNodes(simAu, 3, 7)
        log.info("Created the following nodes on client %s:\n        %s" %
                 (victim, '\n        '.join([str(n) for n in nodeList])))

        log.info("Waiting for a V3 poll to be called...")
        victim.waitForV3Poller(simAu)

        log.info("Successfully called a V3 poll.")

        ## Just pause until we have better tests.
        log.info("Waiting for V3 poll to report no quorum...")
        victim.waitForV3NoQuorum(simAu)
        log.info("AU successfully reported No Quorum.")
        peerDict = victim.getAuRepairerInfo(simAu)
        log.debug2("peerDict: " + str(peerDict))
        for c in self.clients:
            if c != victim:
                agree = peerDict[c.getPeerId()]
                assert agree['highestAgree'] > 60, "No agreement recorded for %s % c"
    

class TotalLossRecoveryV3TestCase(V3TestCase):
    """Test repairing a cache under V3 that has lost all its contents"""
    def getTestLocalConf(self):
        ## Enable polling on all peers.
        return {"org.lockss.poll.v3.enableV3Poller":"true"}

    def getDaemonCount(self):
        return V3TestCase.getDaemonCount(self) + 1

    def runTest(self):
        
        ## Define a simulated AU
        simAu = SimulatedAu('simContent', depth=0, branch=0,
                            numFiles=30, protocolVersion=3)

        victim = self.victim
        noAuClient = self.clients[len(self.clients) - 1]
        self.clients.remove(noAuClient)
        
        ## Create simulated AUs
        self.createAus(simAu)

        ## Assert that the AUs have been crawled.
        self.crawlAus(simAu)

        ## Deactivate AU on one client
#        noAuClient.deactivateAu(simAu)

        nodeList = victim.getAuNodesWithContent(simAu)

        # expect to see a top level content poll called by all peers.
        log.info("Waiting for a V3 poll by all simulated caches")
        for c in self.clients:
            assert c.waitForV3Poller(simAu), "Never called V3 poll."
            log.info("Client on port %s called V3 poll..." % c.port)

        # expect that each client will have wone a top-level v3 poll
        log.info("Waiting for all peers to win their polls")
        for c in self.clients:
            assert c.waitForWonV3Poll(simAu, timeout=self.timeout),\
                   ("Client on port %s never won V3 poll" % c.port)
            log.info("Client on port %s won V3 poll..." % c.port)

        assert victim.getNoAuPeers(simAu) == [ noAuClient.getPeerId() ], "NoAUPeer not recorded."

        log.info("Backing up cache configuration on victim cache...")
        victim.backupConfiguration()
        log.info("Backed up successfully.")

        # All daemons should have recorded their agreeing peers at this
        # point, so stop the client we're going to damage.
        victim.daemon.stop()
        log.info("Stopped daemon running on UI port %s" % victim.port)

        victim.simulateDiskFailure()
        log.info("Deleted entire contents of cache on stopped daemon.")

        #
        # Write out a TitleDB entry for the simulated AU so it will be marked
        # 'publisher down' when it is restored.
        #
        extraConf = {"org.lockss.auconfig.allowEditDefaultOnlyParams": "true",
#                     "org.lockss.plugin.registry": "org.lockss.plugin.simulated.SimulatedPlugin",
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
                     "org.lockss.title.sim1.param.4.value": "30",
                     "org.lockss.title.sim1.param.pub_down.key": "pub_down",
                     "org.lockss.title.sim1.param.pub_down.value": "true"}
#                     "org.lockss.title.sim1.param.protocol.key": "protocol_version",
#                     "org.lockss.title.sim1.param.protocol.value": "3"}

        self.framework.appendLocalConfig(extraConf, victim)

        time.sleep(5) # Give time for things to settle before starting again

        victim.daemon.start()

        # Wait for the client to come up
        assert victim.waitForDaemonReady(), "Daemon never became ready"
        log.info("Started daemon running on UI port %s" % victim.port)

        assert not victim.hasAu(simAu)

        # Now restore the backup file
        log.info("Restoring cache configuration...")
        victim.restoreConfiguration(simAu)
        log.info("Restored successfully.")

        # These should be equal AU IDs, so both should return true
        assert victim.hasAu(simAu)
        assert victim.isPublisherDown(simAu)
        
        # expect to see a V3 poll called
        log.info("Waiting for a V3 poll.")
        assert victim.waitForV3Poller(simAu),\
            "Never called V3 poll."
        log.info("Called V3 poll.")

        # expect to see the AU successfully repaired
        log.info("Waiting for successful V3 repair of AU.")
        assert victim.waitForCompleteV3Repair(simAu, timeout=self.timeout),\
               "AU never repaired by V3."
        for node in nodeList:
            assert self.compareNode(node, simAu, victim, self.nonVictim), "File wasn't repaired: %s % node.url"
        log.info("AU successfully repaired by V3.")

        # End of test.


###########################################################################
### Functions that build and return test suites.  These can be
### called by name when running this test script.
###########################################################################

def tinyUiTests():
    suite = unittest.TestSuite()
    suite.addTest(TinyUiUnknownHostTestCase())
    suite.addTest(TinyUiMalformedUrlTestCase())
    suite.addTest(TinyUiForbiddenTestCase())
    suite.addTest(TinyUiRefusedTestCase())
    suite.addTest(TinyUiFileNotFoundTestCase())
    return suite

def simpleV3Tests():
    suite = unittest.TestSuite()
    suite.addTest(SimpleDamageV3TestCase())
    suite.addTest(SimpleDeleteV3TestCase())
    suite.addTest(SimpleExtraFileV3TestCase())
    suite.addTest(LastFileDeleteV3TestCase())
    suite.addTest(LastFileExtraV3TestCase())
    suite.addTest(VotersDontParticipateV3TestCase())
    suite.addTest(NoQuorumV3TestCase())
    suite.addTest(TotalLossRecoveryV3TestCase())
    # suite.addTest(RepairFromPublisherV3TestCase())
    suite.addTest(RepairFromPeerV3TestCase())
    return suite

def randomV3Tests():
    suite = unittest.TestSuite()
    suite.addTest(RandomDamageV3TestCase())
    suite.addTest(RandomDeleteV3TestCase())
    suite.addTest(RandomExtraFileV3TestCase())
    return suite

def v3Tests():
    suite = unittest.TestSuite()
    suite.addTest(simpleV3Tests())
    suite.addTest(randomV3Tests())
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
    suite.addTest(tinyUiTests())
    suite.addTest(v3Tests())
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
            if fw.isRunning:
                fw.stop()

        if e.code:
            sys.exit(1)
        else:
            if deleteAfterSuccess:
                for fw in frameworkList:
                    fw.clean()

    except KeyboardInterrupt:
        for fw in frameworkList:
            if fw.isRunning:
                fw.stop()
        raise
    except Exception, e:
        # Unhandled exception occured.
        log.error("%s" % e)
        raise
